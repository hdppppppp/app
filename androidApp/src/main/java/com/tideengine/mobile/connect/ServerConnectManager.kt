package com.tideengine.mobile.connect

import android.content.Context
import android.net.wifi.WifiManager
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.discovery.LanDiscovery
import com.tideengine.mobile.discovery.ServerAddressStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** 连接状态：界面据此渲染探测中 / 失败（含手动输入）/ 就绪三种形态。 */
sealed class ServerConnectState {
    /** 探测中；[step] 是当前阶段文案，直接展示给用户。 */
    data class Discovering(val step: String) : ServerConnectState()

    /** 地址已就绪并写入 ServiceLocator，可以进入热更新门禁与主界面。 */
    data class Ready(val baseUrl: String, val serverName: String?) : ServerConnectState()

    /** 探测失败：局域网内没有找到服务端，需要用户重试或手动输入。 */
    data class Failed(val message: String) : ServerConnectState()
}

/**
 * 发现模式持久化契约：公共网络模式下不发广播、不扫描网段。
 * 与 [ServerAddressStore] 一样由宿主注入 SharedPreferences 实现。
 */
interface DiscoveryModeStore {
    fun restricted(): Boolean

    fun setRestricted(value: Boolean)
}

/**
 * 服务端地址解析编排：记忆地址校验 → UDP 广播 → 子网扫描 → 构建期默认值。
 *
 * 时序红线：`/api/auth/me`、热更新检查等业务请求必须等 [ServerConnectState.Ready]
 * 之后才能发出 —— ServiceLocator.initialize 用构建期默认值兜底，若地址探测
 * 尚未完成就放行业务请求，token 会打到错误地址上，401 清理逻辑会把用户踢回
 * 登录页。界面侧由 ServerGate 在 Ready 之前拦截所有页面。
 *
 * 探测命中后立即 switchBaseUrl + 落盘，下次启动优先走记忆地址快连；
 * onReady 只在每次成功连接时回调一次，宿主用它触发首次热更新检查
 * （UpdateManager 内部互斥，多次回调不会并发检查）。
 *
 * 信任边界：探测发现的是局域网里的声明，`app == "tide-engine"` 只是协议
 * 约定不是身份认证 —— 不可信网络（公共 Wi-Fi）下恶意设备可以伪装成服务端。
 * 「公共网络模式」跳过广播与扫描两步自动发现，只信记忆地址与手动输入；
 * 候选验证数量另有硬上限（见 VERIFY_CAP_*），假应答洪泛最多拖慢一个有界时长。
 */
class ServerConnectManager(
    private val defaultBaseUrl: String,
    private val store: ServerAddressStore,
    private val modeStore: DiscoveryModeStore,
    private val networkLock: DiscoveryNetworkLock,
    private val onReady: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow<ServerConnectState>(
        ServerConnectState.Discovering("正在准备连接…"),
    )
    val state: StateFlow<ServerConnectState> = mutableState

    // 公共网络模式：StateFlow 是运行时唯一事实源，落盘是持久化副本；
    // 用流是为了 UI 开关能正确重组（普通参数读一次不会随切换刷新）
    private val mutableRestricted = MutableStateFlow(modeStore.restricted())
    val discoveryRestricted: StateFlow<Boolean> = mutableRestricted

    private var started = false

    /** 冷启动探测入口；重复调用无副作用（MainActivity 重建等场景）。 */
    fun start() {
        if (started) return
        started = true
        retry()
    }

    /** 从失败页或用户主动重试：按完整顺序重新探测。 */
    fun retry() {
        scope.launch { resolve() }
    }

    /**
     * 切换公共网络模式并立即生效：保存后若尚未连接成功，按新模式重新探测。
     * 已连接（Ready）时只保存，下次启动生效——不打断当前会话。
     */
    fun setDiscoveryRestricted(value: Boolean) {
        mutableRestricted.value = value
        modeStore.setRestricted(value)
        if (mutableState.value !is ServerConnectState.Ready) retry()
    }

    /** 手动输入地址：规范化 + 验证后采用；失败给出可直接阅读的错误文案。 */
    fun connectManually(input: String) {
        val url = LanDiscovery.normalizeBaseUrl(input)
        if (url == null) {
            mutableState.value = ServerConnectState.Failed("地址格式不正确，请输入 IP:端口 或完整 http(s) 地址")
            return
        }
        mutableState.value = ServerConnectState.Discovering("正在连接 $url …")
        scope.launch {
            val server = LanDiscovery.verify(url, timeoutMs = MANUAL_VERIFY_TIMEOUT_MS)
            if (server == null) {
                mutableState.value = ServerConnectState.Failed(
                    "无法连接 $url：那里没有运行潮汐引擎服务端，请确认地址与端口",
                )
                return@launch
            }
            adopt(url, server)
        }
    }

    private suspend fun resolve() {
        mutableState.value = ServerConnectState.Discovering("正在检查上次使用的服务端…")
        try {
            val restricted = mutableRestricted.value

            // ① 记忆地址快连：短超时验证，通过则完全跳过广播与扫描
            val saved = store.load()
            if (saved != null) {
                val server = LanDiscovery.verify(saved)
                if (server != null) {
                    adopt(saved, server)
                    return
                }
            }

            // 公共网络模式：跳过广播与扫描——只信任记忆地址、构建期默认值与
            // 手动输入，避免把登录凭据发到局域网里伪装的假服务端。
            if (!restricted) {
                networkLock.acquire()
                try {
                    // ② UDP 广播：服务端单播回源，毫秒级；候选数有硬上限
                    mutableState.value = ServerConnectState.Discovering("正在探测局域网内的服务端…")
                    val udpCandidates = LanDiscovery.probeUdp().take(VERIFY_CAP_UDP)
                    for (candidate in udpCandidates) {
                        val server = LanDiscovery.verify(candidate)
                        if (server != null) {
                            adopt(candidate, server)
                            return
                        }
                    }

                    // ③ 子网扫描兜底：广播被 AP 隔离等拦截时的最后一条自动路径
                    mutableState.value = ServerConnectState.Discovering("广播无应答，正在扫描同网段…")
                    val scanCandidates = LanDiscovery.scanHttpPort().take(VERIFY_CAP_SCAN)
                    for (candidate in scanCandidates) {
                        val server = LanDiscovery.verify(candidate)
                        if (server != null) {
                            adopt(candidate, server)
                            return
                        }
                    }
                } finally {
                    networkLock.release()
                }
            }

            // ④ 构建期默认地址兜底：打包时指定的地址仍然可用的场景
            mutableState.value = ServerConnectState.Discovering("正在尝试默认地址…")
            val fallbackServer = LanDiscovery.verify(defaultBaseUrl)
            if (fallbackServer != null) {
                adopt(defaultBaseUrl, fallbackServer)
                return
            }

            val failureMessage = if (restricted) {
                "公共网络模式下未找到记忆的服务端。请确认服务端地址，或手动输入服务地址；回到可信网络后可关闭该模式恢复自动探测。"
            } else {
                "没有在局域网内找到潮汐引擎服务端。请确认手机与服务器连的是同一个 Wi-Fi、服务端已启动，或直接手动输入服务地址。"
            }
            mutableState.value = ServerConnectState.Failed(failureMessage)
        } catch (error: Throwable) {
            // 探测路径全部容错（verify 返回 null），能走到这里的多是线程/存储异常
            mutableState.value = ServerConnectState.Failed(
                "服务端探测失败：${error.message ?: "未知错误"}",
            )
        }
    }

    /** 采用地址：切根 + 落盘 + 置 Ready，并通知宿主发起首次热更新检查。 */
    private fun adopt(url: String, server: LanDiscovery.DiscoveredServer) {
        if (ServiceLocator.isInitialized) {
            ServiceLocator.switchBaseUrl(url)
        }
        store.save(url)
        mutableState.value = ServerConnectState.Ready(url, server.name)
        onReady()
    }

    private companion object {
        const val MANUAL_VERIFY_TIMEOUT_MS = 4_000
        // 候选验证上限：假应答洪泛时，验证阶段最多耗时 = 上限 × verify 超时，
        // 启动期 DoS 被限制在几十秒的有界时长内（正常家庭网络只有 1 个候选）
        const val VERIFY_CAP_UDP = 5
        const val VERIFY_CAP_SCAN = 8
    }
}

/**
 * 组播接收锁的窄接口：Wi-Fi 在省电模式下会过滤广播 / 组播包，探测前获取、
 * 结束后释放。shared 的 LanDiscovery 不触碰 Android API，锁由宿主持有。
 */
interface DiscoveryNetworkLock {
    fun acquire()

    fun release()
}

/** 基于 WifiManager.MulticastLock 的实现；系统服务缺失时静默降级为无锁。 */
class MulticastNetworkLock(context: Context) : DiscoveryNetworkLock {
    private val lock: WifiManager.MulticastLock? =
        (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.createMulticastLock("tide-lan-discovery")
            ?.apply { setReferenceCounted(false) }

    override fun acquire() {
        runCatching { lock?.acquire() }
    }

    override fun release() {
        runCatching { if (lock?.isHeld == true) lock.release() }
    }
}

/** SharedPreferences 版地址与模式存储；与 token、热更新配置同样按应用私有目录落盘。 */
class SharedPreferencesServerAddressStore(context: Context) :
    ServerAddressStore, DiscoveryModeStore {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): String? = prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }

    override fun save(baseUrl: String) {
        prefs.edit().putString(KEY_BASE_URL, baseUrl).apply()
    }

    override fun restricted(): Boolean = prefs.getBoolean(KEY_RESTRICTED, false)

    override fun setRestricted(value: Boolean) {
        prefs.edit().putBoolean(KEY_RESTRICTED, value).apply()
    }

    private companion object {
        const val PREFS_NAME = "tide_server_address"
        const val KEY_BASE_URL = "base_url"
        const val KEY_RESTRICTED = "discovery_restricted"
    }
}
