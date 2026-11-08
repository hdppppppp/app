package com.tideengine.mobile.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * 对应 Web 端 client.ts：baseUrl 指向服务端根地址，路径统一以 /api 开头。
 * token 由 TokenStorage 提供，401 时清空并通知外层回到登录页。
 *
 * 请求体先由 kotlinx.serialization 编码为字符串再发送，避免 Ktor 在
 * `setBody(Any)` 时无法从运行时类型推断 serializer。
 */
class ApiClient(
    baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: () -> Unit,
) {
    private val root = baseUrl.trimEnd('/')

    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        isLenient = true
        encodeDefaults = true
    }

    @PublishedApi
    internal val http = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend inline fun <reified T> get(
        path: String,
        query: Map<String, String?> = emptyMap(),
    ): T = send(HttpMethod.Get, path, query, null)

    suspend inline fun <reified T> post(path: String): T =
        send(HttpMethod.Post, path, emptyMap(), null)

    suspend inline fun <reified B : Any, reified T> postJson(path: String, body: B): T =
        send(HttpMethod.Post, path, emptyMap(), json.encodeToString(body))

    suspend inline fun <reified B : Any, reified T> patchJson(path: String, body: B): T =
        send(HttpMethod.Patch, path, emptyMap(), json.encodeToString(body))

    suspend inline fun <reified B : Any, reified T> putJson(path: String, body: B): T =
        send(HttpMethod.Put, path, emptyMap(), json.encodeToString(body))

    suspend inline fun <reified T> delete(path: String): T =
        send(HttpMethod.Delete, path, emptyMap(), null)

    /** 头像等静态资源以二进制下发，不走 JSON 反序列化；失败返回 null 由调用方回退。 */
    suspend fun fetchBytes(path: String): ByteArray? {
        val response = execute(HttpMethod.Get, path, emptyMap(), null)
        if (response.status.value !in 200..299) return null
        return runCatching { response.body<ByteArray>() }.getOrNull()
    }

    @PublishedApi
    internal suspend inline fun <reified T> send(
        method: HttpMethod,
        path: String,
        query: Map<String, String?>,
        rawJsonBody: String?,
    ): T {
        val response = execute(method, path, query, rawJsonBody)
        if (response.status.value in 200..299) {
            if (T::class == Unit::class) {
                @Suppress("UNCHECKED_CAST")
                return Unit as T
            }
            return response.body()
        }
        throw errorFrom(response)
    }

    @PublishedApi
    internal suspend fun execute(
        method: HttpMethod,
        path: String,
        query: Map<String, String?>,
        rawJsonBody: String?,
    ): HttpResponse {
        val url = buildString {
            append(root)
            append(if (path.startsWith('/')) path else "/$path")
            val pairs = query.filterValues { it != null }
            if (pairs.isNotEmpty()) {
                append('?')
                append(
                    pairs.entries.joinToString("&") { (key, value) ->
                        "${encode(key)}=${encode(value!!)}"
                    },
                )
            }
        }

        val response = try {
            http.request(url) {
                this.method = method
                tokenProvider()?.takeIf { it.isNotBlank() }?.let {
                    header("Authorization", "Bearer $it")
                }
                if (rawJsonBody != null) {
                    setBody(TextContent(rawJsonBody, ContentType.Application.Json))
                }
            }
        } catch (error: Throwable) {
            throw ApiException(
                status = 0,
                code = "NETWORK_ERROR",
                message = "网络连接失败，请检查网络或服务地址",
                payload = error.message,
            )
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            onUnauthorized()
        }
        return response
    }

    @PublishedApi
    internal suspend fun errorFrom(response: HttpResponse): ApiException {
        val text = runCatching { response.bodyAsText() }.getOrDefault("")
        return ApiException(
            status = response.status.value,
            code = errorCode(response.status),
            message = errorMessage(text),
            payload = text,
        )
    }

    @PublishedApi
    internal fun errorCode(status: HttpStatusCode): String = when (status.value) {
        400 -> "BAD_REQUEST"
        401 -> "UNAUTHORIZED"
        403 -> "FORBIDDEN"
        404 -> "NOT_FOUND"
        409 -> "CONFLICT"
        429 -> "TOO_MANY_REQUESTS"
        503 -> "SERVICE_UNAVAILABLE"
        else -> "INTERNAL_SERVER_ERROR"
    }

    @PublishedApi
    internal fun errorMessage(text: String): String {
        val fallback = "请求失败，请稍后重试"
        val element = runCatching { json.parseToJsonElement(text) }.getOrNull()
        val body = element as? JsonObject ?: return fallback

        when (val message = body["message"]) {
            is JsonPrimitive -> message.contentOrNull?.takeIf { it.isNotBlank() }?.let { return it }
            is JsonArray -> {
                val joined = message.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                    .filter { it.isNotBlank() }
                    .joinToString("；")
                if (joined.isNotBlank()) return joined
            }
            else -> Unit
        }

        val issues = runCatching {
            body["data"]?.jsonObject?.get("issues") as? JsonArray
        }.getOrNull()
        if (issues != null) {
            val joined = issues.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .filter { it.isNotBlank() }
                .joinToString("；")
            if (joined.isNotBlank()) return joined
        }

        return fallback
    }

    @PublishedApi
    internal fun encode(value: String): String = buildString {
        value.forEach { char ->
            when {
                char.isLetterOrDigit() || char in "-._~" -> append(char)
                char == ' ' -> append("%20")
                else -> {
                    val bytes = char.toString().encodeToByteArray()
                    bytes.forEach { byte ->
                        val unsigned = byte.toInt() and 0xFF
                        append('%')
                        append(HEX[unsigned shr 4])
                        append(HEX[unsigned and 0x0F])
                    }
                }
            }
        }
    }

    private companion object {
        const val HEX = "0123456789ABCDEF"
    }
}
