package com.tideengine.mobile.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 多账号保险库的 Android 实现：账号条目（含加密 token）序列化进私有
 * SharedPreferences，token 与 AndroidTokenStore 用同一把 Keystore AES-256-GCM
 * 密钥逐账号加密，不落明文。展示字段（用户名 / 昵称 / 头像地址）为非敏感信息明文存储。
 */
class AndroidAccountVault(context: Context) : AccountVault {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /** 落盘用的条目：token 字段存 Keystore 加密后的 base64。 */
    @Serializable
    private data class StoredAccount(
        val userId: String,
        val username: String,
        val displayName: String,
        val avatarUrl: String? = null,
        val encryptedToken: String,
    )

    override fun loadAccounts(): List<SavedAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoredAccount.serializer()), raw)
                .map { stored ->
                    SavedAccount(
                        userId = stored.userId,
                        username = stored.username,
                        displayName = stored.displayName,
                        avatarUrl = stored.avatarUrl,
                        token = decrypt(stored.encryptedToken),
                    )
                }
        }.getOrDefault(emptyList())
    }

    override fun saveAccounts(accounts: List<SavedAccount>) {
        val stored = accounts.map { account ->
            StoredAccount(
                userId = account.userId,
                username = account.username,
                displayName = account.displayName,
                avatarUrl = account.avatarUrl,
                encryptedToken = encrypt(account.token),
            )
        }
        prefs.edit()
            .putString(KEY_ACCOUNTS, json.encodeToString(ListSerializer(StoredAccount.serializer()), stored))
            .apply()
    }

    override fun upsert(account: SavedAccount) {
        saveAccounts(loadAccounts().filterNot { it.userId == account.userId } + account)
    }

    override fun remove(userId: String) {
        saveAccounts(loadAccounts().filterNot { it.userId == userId })
    }

    override fun activeUserId(): String? = prefs.getString(KEY_ACTIVE, null)

    override fun setActive(userId: String) {
        prefs.edit().putString(KEY_ACTIVE, userId).apply()
    }

    private fun encrypt(token: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(payloadBase64: String): String {
        val payload = Base64.decode(payloadBase64, Base64.NO_WRAP)
        if (payload.size <= IV_SIZE) return ""
        val iv = payload.copyOfRange(0, IV_SIZE)
        val cipherText = payload.copyOfRange(IV_SIZE, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "tide_engine_token_key"
        const val PREFS_NAME = "tide_accounts"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_ACTIVE = "active_user_id"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
