package com.tideengine.mobile.data

import com.tideengine.mobile.api.ApiClient
import com.tideengine.mobile.api.dto.AuthOptions
import com.tideengine.mobile.api.dto.AuthResponse
import com.tideengine.mobile.api.dto.ChangePasswordRequest
import com.tideengine.mobile.api.dto.LoginRequest
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.api.dto.SuccessResponse
import com.tideengine.mobile.api.dto.UpdateAvatarRequest
import com.tideengine.mobile.api.dto.UpdateProfileRequest
import com.tideengine.mobile.auth.AccountVaultStorage
import com.tideengine.mobile.auth.SavedAccount
import com.tideengine.mobile.auth.TokenStorage

class AuthRepository(private val api: ApiClient) {
    suspend fun options(): AuthOptions = api.get("/api/auth/options")

    suspend fun login(identifier: String, password: String, rememberMe: Boolean = false): AuthResponse {
        val request = LoginRequest(identifier = identifier, password = password, rememberMe = rememberMe)
        val response: AuthResponse = api.postJson("/api/auth/login", request)
        TokenStorage.save(response.accessToken)
        rememberAccount(response.user, response.accessToken)
        return response
    }

    suspend fun me(): PublicUser {
        val user: PublicUser = api.get("/api/auth/me")
        // 多账号版本的存量迁移：保险库里没有当前用户条目时，把旧单 token 存储建档
        AccountVaultStorage.migrateLegacy(TokenStorage.load(), user.toSavedAccount(TokenStorage.load().orEmpty()))
        return user
    }

    /** 与 Web 端一致：本地裁剪压缩好的 256×256 JPEG data URL。 */
    suspend fun updateAvatar(imageDataUrl: String): PublicUser {
        val updated = api.putJson<UpdateAvatarRequest, PublicUser>(
            "/api/auth/avatar",
            UpdateAvatarRequest(image = imageDataUrl),
        )
        syncVaultUser(updated)
        return updated
    }

    suspend fun resetAvatar(): PublicUser {
        val updated = api.delete<PublicUser>("/api/auth/avatar")
        syncVaultUser(updated)
        return updated
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.patchJson<ChangePasswordRequest, SuccessResponse>(
            "/api/auth/password",
            ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword),
        )
    }

    /** 与 Web 端一致：仅更新显示名称（邮箱当前不支持自助修改）。 */
    suspend fun updateProfile(displayName: String): PublicUser {
        val updated = api.patchJson<UpdateProfileRequest, PublicUser>(
            "/api/auth/profile",
            UpdateProfileRequest(displayName = displayName),
        )
        syncVaultUser(updated)
        return updated
    }

    /**
     * 退出登录：清理当前账号的保险库条目与激活 token；其他已记住账号不受影响，
     * 之后仍可在「我的 → 切换账号」里快速切换（重新登录一次即可恢复其 token）。
     */
    fun logout() {
        AccountVaultStorage.activeUserId()?.let { AccountVaultStorage.remove(it) }
        TokenStorage.clear()
    }

    fun hasToken(): Boolean = !TokenStorage.load().isNullOrBlank()

    /** 排除当前账号外的其他已记住账号（切换列表用）。 */
    fun otherAccounts(currentUserId: String): List<SavedAccount> =
        AccountVaultStorage.loadAccounts().filter { it.userId != currentUserId }

    /**
     * 切换到已记住的账号：换激活 token 后拉取 me 确认登录态仍有效。
     * token 失效（401）时抛出异常，由调用方移除该条目并回到登录页；
     * 当前账号的 token 已在保险库中，不会被丢失。
     */
    suspend fun switchAccount(userId: String): PublicUser {
        val entry = AccountVaultStorage.loadAccounts().find { it.userId == userId }
            ?: throw IllegalStateException("该账号不在已记住列表中")
        TokenStorage.save(entry.token)
        AccountVaultStorage.setActive(userId)
        return me()
    }

    /** 切换目标 token 失效（401）时恢复当前账号的 token，避免把用户完全登出。 */
    fun restoreActiveToken(userId: String) {
        AccountVaultStorage.loadAccounts()
            .find { it.userId == userId }
            ?.let { TokenStorage.save(it.token) }
    }

    /** 从保险库移除指定账号（切换目标 token 失效时调用）。 */
    fun removeSavedAccount(userId: String) {
        AccountVaultStorage.remove(userId)
    }

    /** 登录成功后把账号写入保险库并设为激活（重复登录按 userId 覆盖）。 */
    private fun rememberAccount(user: PublicUser, token: String) {
        AccountVaultStorage.upsert(user.toSavedAccount(token))
        AccountVaultStorage.setActive(user.id)
    }

    /** 头像更新后同步保险库里的展示字段，切换列表里看到的头像不滞后。 */
    private fun syncVaultUser(user: PublicUser) {
        val updated = AccountVaultStorage.loadAccounts()
            .map { if (it.userId == user.id) it.copy(avatarUrl = user.avatarUrl) else it }
        AccountVaultStorage.saveAccounts(updated)
    }

    private fun PublicUser.toSavedAccount(token: String): SavedAccount = SavedAccount(
        userId = id,
        username = username,
        displayName = displayName,
        avatarUrl = avatarUrl,
        token = token,
    )
}
