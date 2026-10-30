package com.tideengine.mobile.auth

/**
 * Token 存储抽象。Android 侧实现使用 SharedPreferences + Android Keystore，
 * 避免 Kuikly 页面直接依赖 Context。
 */
interface TokenStore {
    fun load(): String?

    fun save(token: String)

    fun clear()
}

object TokenStorage {
    private var store: TokenStore? = null

    fun install(tokenStore: TokenStore) {
        store = tokenStore
    }

    fun load(): String? = store?.load()

    fun save(token: String) {
        store?.save(token)
    }

    fun clear() {
        store?.clear()
    }
}
