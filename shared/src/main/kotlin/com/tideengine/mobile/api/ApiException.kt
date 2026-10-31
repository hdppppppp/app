package com.tideengine.mobile.api

/**
 * 与 Web 端 ApiError 对齐：status 保留 HTTP 状态码，message 为服务端中文文案。
 * 网络层失败时 status 为 0，便于 UI 区分「服务不可达」与业务错误。
 */
class ApiException(
    val status: Int,
    val code: String?,
    override val message: String,
    val payload: String? = null,
) : Exception(message) {
    val isUnauthorized: Boolean
        get() = status == 401

    val isNetworkError: Boolean
        get() = status == 0
}
