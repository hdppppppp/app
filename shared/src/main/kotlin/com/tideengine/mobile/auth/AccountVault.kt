package com.tideengine.mobile.auth

import kotlinx.serialization.Serializable

/**
 * 已登录账号的本地快照：切换账号时无需重输密码（token 加密落盘）。
 * [token] 在内存中为明文，Android 实现落盘前逐账号加密。
 */
@Serializable
data class SavedAccount(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val token: String,
)

/**
 * 多账号保险库抽象：记住多个已登录账号（各自加密 token + 展示信息）与
 * 当前激活账号。与 [TokenStorage] 同为可安装抽象，Android 侧实现用
 * Keystore 加密，shared 不触碰平台 API。
 *
 * 语义约定：
 * - upsert 按 userId 覆盖（同名重复登录不产生重复条目）。
 * - 「退出登录」由调用方决定是否移除条目（本版移除，重新登录即重新入列）。
 * - 切换到 token 已失效的账号由调用方处理（me() 401 → 移除并回登录页）。
 */
interface AccountVault {
    fun loadAccounts(): List<SavedAccount>

    fun upsert(account: SavedAccount)

    fun remove(userId: String)

    fun activeUserId(): String?

    fun setActive(userId: String)
}

object AccountVaultStorage {
    private var vault: AccountVault? = null

    fun install(v: AccountVault) {
        vault = v
    }

    fun loadAccounts(): List<SavedAccount> = vault?.loadAccounts() ?: emptyList()

    fun upsert(account: SavedAccount) {
        vault?.upsert(account)
    }

    fun remove(userId: String) {
        vault?.remove(userId)
    }

    fun activeUserId(): String? = vault?.activeUserId()

    fun setActive(userId: String) {
        vault?.setActive(userId)
    }

    /**
     * 旧版单 token 存量的迁移：保险库里没有该用户条目且本地还有旧 token 时
     * 建档（升级到多账号版本后的首次 me() 触发一次）。
     */
    fun migrateLegacy(token: String?, user: SavedAccount) {
        if (token.isNullOrBlank()) return
        if (loadAccounts().any { it.userId == user.userId }) return
        upsert(user.copy(token = token))
        setActive(user.userId)
    }
}
