package com.tideengine.mobile.discovery

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.semaphore.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * 局域网服务发现：App 启动时自动探测服务端地址。
 *
 * 协议与服务端（主仓 `server/src/release/lan-discovery.protocol.ts`）对齐，改动
 * 魔数、应答字段或端点路径必须两边同步：
 *
 * 1. UDP 广播：向全网广播 + 各网卡的定向广播发送魔数包，服务端向来源单播回
 *    `{protocol, app, name, port, version}`；候选地址用**应答包的来源 IP** +
 *    应答里的 `port` 拼 `http://<ip>:<port>` —— 多网卡环境由回程路由保证来源
 *    IP 就是客户端可达的那个接口地址，因此应答刻意不带 baseUrl。
 * 2. HTTP 子网扫描兜底：广播被 AP 隔离等拦截时，对本机所在网段（/24）的
 *    3100 端口并行 TCP 探测，再请求 `GET /api/v1/app/discover` 确认。
 * 3. verify：对候选地址请求 discover 端点，`data.app == "tide-engine"` 才算
 *    命中，避免连到同端口的其他服务。
 *
 * 本文件只依赖 java.net 与 Ktor，不触碰 Android API（与 shared 模块的其余
 * 部分一致）；广播接收所需的 MulticastLock 由宿主（androidApp）持有。
 */
object LanDiscovery {

    /** 探测请求魔数：与服务端 LAN_DISCOVERY_MAGIC 逐字节一致，勿单边修改。 */
    const val MAGIC = "TIDE-LAN-DISCOVERY-V1"

    /** 服务端 UDP 监听端口（LAN_DISCOVERY_PORT 默认值），广播探测包的目标端口。 */
    const val UDP_PORT = 31001

    /** 业务 API 的 HTTP 端口（服务端 PORT 默认值），子网扫描的目标端口。 */
    const val DEFAULT_HTTP_PORT = 3100

    /** 应答协议版本：只认 protocol=1，服务端将来升版时旧客户端静默丢弃。 */
    private const val PROTOCOL_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * discover HTTP 端点应答的信封（`{code, message, data}`）；与 UDP 应答的
     * 扁平结构不同，两者必须分开建模。字段全部可空，解析失败按无效应答丢弃。
     */
    @Serializable
    private data class DiscoverEnvelope(
        val code: Int? = null,
        val data: DiscoverInfo? = null,
    )

    /** HTTP 端点 data 内容。 */
    @Serializable
    private data class DiscoverInfo(
        val app: String? = null,
        val name: String? = null,
        val port: Int? = null,
        val version: String? = null,
    )

    /**
     * UDP 应答的扁平结构：服务端直接单播
     * `{protocol, app, name, port, version}`，没有信封层。
     */
    @Serializable
    private data class DiscoverReply(
        val protocol: Int? = null,
        val app: String? = null,
        val name: String? = null,
        val port: Int? = null,
        val version: String? = null,
    )

    /** 一次成功的探测结果：baseUrl 可直接交给 ServiceLocator，name 用于展示。 */
    data class DiscoveredServer(val baseUrl: String, val name: String?, val version: String?)

    /**
     * 验证一个候选地址是否为潮汐引擎服务端。
     *
     * 命中返回摘要；网络失败、非 2xx、非本服务（app 不符）一律返回 null，
     * 不抛异常 —— 调用方按「下一个候选」继续，不必逐个捕获。每个超时档位
     * 建独立客户端：verify 调用频次极低（每次启动最多几次），不复用连接池。
     */
    suspend fun verify(baseUrl: String, timeoutMs: Int = DEFAULT_TIMEOUT_MS): DiscoveredServer? =
        withContext(Dispatchers.IO) {
            val root = baseUrl.trimEnd('/')
            if (root.isEmpty()) return@withContext null
            val client = HttpClient {
                expectSuccess = false
                install(HttpTimeout) {
                    requestTimeoutMillis = timeoutMs.toLong()
                    connectTimeoutMillis = timeoutMs.toLong()
                    socketTimeoutMillis = timeoutMs.toLong()
                }
            }
            try {
                val text = client.get("$root/api/v1/app/discover").bodyAsText()
                val envelope = runCatching { json.decodeFromString(DiscoverEnvelope.serializer(), text) }
                    .getOrNull() ?: return@withContext null
                val info = envelope.data ?: return@withContext null
                if (envelope.code != 0 || info.app != "tide-engine") return@withContext null
                DiscoveredServer(
                    baseUrl = root,
                    name = info.name?.takeIf { it.isNotBlank() },
                    version = info.version?.takeIf { it.isNotBlank() },
                )
            } catch (_: Throwable) {
                null
            } finally {
                runCatching { client.close() }
            }
        }

    /**
     * UDP 广播探测：返回候选 baseUrl 列表（按应答先后排序）。
     *
     * 只收集窗口内的应答，不在这里做 verify —— 多个服务端同时应答时由调用方
     * 逐个 verify 并取第一个命中的。广播发送失败（部分网卡不支持定向广播）
     * 静默跳过：只要有一个目标送达即可。
     */
    suspend fun probeUdp(
        timeoutMs: Long = UDP_WINDOW_MS,
        port: Int = UDP_PORT,
    ): List<String> = withContext(Dispatchers.IO) {
        val found = LinkedHashMap<String, String?>()
        // 先 unbound 创建，配好 soTimeout 再 bind，避免 bind 到 receive 之间的
        // 早期应答被默认超时卡住
        val socket = DatagramSocket(null).apply {
            broadcast = true
            soTimeout = RECV_POLL_MS
            bind(InetSocketAddress(0))
        }
        try {
            val payload = MAGIC.toByteArray(Charsets.UTF_8)
            for (target in broadcastAddresses()) {
                runCatching {
                    socket.send(DatagramPacket(payload, payload.size, InetSocketAddress(target, port)))
                }
            }
            val deadline = System.currentTimeMillis() + timeoutMs
            val buffer = ByteArray(MAX_REPLY_BYTES)
            while (System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buffer, buffer.size)
                val received = try {
                    socket.receive(packet)
                    true
                } catch (_: SocketTimeoutException) {
                    false
                }
                if (!received) continue
                val reply = parseReply(
                    String(packet.data, packet.offset, packet.length, Charsets.UTF_8),
                ) ?: continue
                val host = packet.address.hostAddress ?: continue
                val replyPort = reply.port ?: DEFAULT_HTTP_PORT
                if (replyPort !in 1..65535) continue
                // 同一服务端可能经全网广播与定向广播各应答一次，按 baseUrl 去重
                found.putIfAbsent("http://$host:$replyPort", reply.name)
            }
        } finally {
            socket.close()
        }
        found.keys.toList()
    }

    /**
     * HTTP 子网扫描兜底：返回本机所在网段内目标端口可达的主机 baseUrl 列表。
     *
     * 只做 TCP 探测不校验内容，候选的 verify 由调用方完成。网段按 /24 划分，
     * 与家用路由的常见形态一致；并行度由 [SCAN_PARALLELISM] 限制，避免在
     * 低端机上瞬时打开数百个 socket。
     */
    suspend fun scanHttpPort(
        port: Int = DEFAULT_HTTP_PORT,
        connectTimeoutMs: Int = SCAN_CONNECT_TIMEOUT_MS,
    ): List<String> = withContext(Dispatchers.IO) {
        val subnets = localSubnets()
        if (subnets.isEmpty()) return@withContext emptyList()
        val hosts = subnets.flatMap { subnet -> HOST_RANGE.map { "$subnet.$it" } }.distinct()
        coroutineScope {
            val semaphore = Semaphore(SCAN_PARALLELISM)
            hosts.map { host ->
                async {
                    semaphore.withPermit {
                        val socket = Socket()
                        try {
                            socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
                            "http://$host:$port"
                        } catch (_: Throwable) {
                            null
                        } finally {
                            runCatching { socket.close() }
                        }
                    }
                }
            }.mapNotNull { it.await() }
        }
    }

    /**
     * 手动输入地址的规范化：补默认协议、去尾斜杠；不合法返回 null。
     *
     * 局域网部署形态是 http，也允许 https（自建反代场景）；纯 IP 自动补成
     * `http://<ip>`。host 段之后的路径、查询串一律拒绝 —— 服务端根地址不应
     * 带路径，路径由请求层统一拼接。
     */
    fun normalizeBaseUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val root = withScheme.trimEnd('/')
        val afterScheme = root.substringAfter("://")
        if (afterScheme.isEmpty()) return null
        if (afterScheme.contains('/') || afterScheme.contains('?') || afterScheme.contains('#')) {
            return null
        }
        return root
    }

    /** 解析 UDP 应答 JSON（扁平结构）；协议版本不符或缺 app 标识的应答直接丢弃。 */
    private fun parseReply(text: String): DiscoverReply? {
        val reply = runCatching { json.decodeFromString(DiscoverReply.serializer(), text) }
            .getOrNull() ?: return null
        if (reply.protocol != PROTOCOL_VERSION) return null
        return reply.takeIf { it.app == "tide-engine" }
    }

    /**
     * 广播目标：全网广播 + 各网卡所在子网的定向广播。
     *
     * 255.255.255.255 在多数家用路由可达；定向广播覆盖多网卡（有线 + Wi-Fi）
     * 与个别丢弃全网广播的 AP。字节按无符号拼装，避免 Byte 负值进字符串。
     */
    private fun broadcastAddresses(): List<String> {
        val result = mutableListOf("255.255.255.255")
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
        for (nif in interfaces) {
            if (!nif.isUp || nif.isLoopback || nif.isVirtual) continue
            for (address in nif.inetAddresses) {
                val inet = address as? Inet4Address ?: continue
                if (inet.isLoopbackAddress || inet.isLinkLocalAddress || inet.isAnyLocalAddress) continue
                val bytes = inet.address
                result.add(
                    "${bytes[0].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}.${bytes[2].toInt() and 0xFF}.255",
                )
            }
        }
        return result.distinct()
    }

    /** 本机所在 /24 网段的前三段集合，用于扫描兜底；无站点局部地址时为空。 */
    private fun localSubnets(): Set<String> {
        val result = mutableSetOf<String>()
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return result
        for (nif in interfaces) {
            if (!nif.isUp || nif.isLoopback || nif.isVirtual) continue
            for (address in nif.inetAddresses) {
                val inet = address as? Inet4Address ?: continue
                if (inet.isLoopbackAddress || inet.isLinkLocalAddress || inet.isAnyLocalAddress) continue
                val bytes = inet.address
                result.add(
                    "${bytes[0].toInt() and 0xFF}.${bytes[1].toInt() and 0xFF}.${bytes[2].toInt() and 0xFF}",
                )
            }
        }
        return result
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 2_000
        const val UDP_WINDOW_MS = 1_500L
        const val RECV_POLL_MS = 80
        const val MAX_REPLY_BYTES = 1_024
        const val SCAN_CONNECT_TIMEOUT_MS = 300
        const val SCAN_PARALLELISM = 64
        const val HOST_RANGE = 1..254
    }
}
