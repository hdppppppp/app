package com.tideengine.mobile.data

import com.tideengine.mobile.api.ApiClient
import com.tideengine.mobile.api.dto.AuthOptions
import com.tideengine.mobile.api.dto.AuthResponse
import com.tideengine.mobile.api.dto.ChangePasswordRequest
import com.tideengine.mobile.api.dto.LoginRequest
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.api.dto.SuccessResponse
import com.tideengine.mobile.api.dto.UpdateAvatarRequest
import com.tideengine.mobile.auth.TokenStorage

class AuthRepository(private val api: ApiClient) {
    suspend fun options(): AuthOptions = api.get("/api/auth/options")

    suspend fun login(identifier: String, password: String, rememberMe: Boolean = false): AuthResponse {
        val request = LoginRequest(identifier = identifier, password = password, rememberMe = rememberMe)
        val response: AuthResponse = api.postJson("/api/auth/login", request)
        TokenStorage.save(response.accessToken)
        return response
    }

    suspend fun me(): PublicUser = api.get("/api/auth/me")

    /** 与 Web 端一致：本地裁剪压缩好的 256×256 JPEG data URL。 */
    suspend fun updateAvatar(imageDataUrl: String): PublicUser =
        api.putJson("/api/auth/avatar", UpdateAvatarRequest(image = imageDataUrl))

    suspend fun resetAvatar(): PublicUser = api.delete("/api/auth/avatar")

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.patchJson<ChangePasswordRequest, SuccessResponse>(
            "/api/auth/password",
            ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword),
        )
    }

    /** 服务端没有注销接口；与 Web 端一致，仅清理本地 token。 */
    fun logout() {
        TokenStorage.clear()
    }

    fun hasToken(): Boolean = !TokenStorage.load().isNullOrBlank()
}
