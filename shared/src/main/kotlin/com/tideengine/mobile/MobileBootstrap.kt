package com.tideengine.mobile

import android.content.Context
import com.tideengine.mobile.auth.AccountVaultStorage
import com.tideengine.mobile.auth.AndroidAccountVault
import com.tideengine.mobile.auth.AndroidTokenStore
import com.tideengine.mobile.auth.TokenStorage

/**
 * androidApp 在 Application.onCreate 调用一次：
 * 安装加密 token 存储、多账号保险库，并初始化网络容器。
 */
object MobileBootstrap {
    fun install(context: Context, apiBaseUrl: String, userAgent: String) {
        TokenStorage.install(AndroidTokenStore(context))
        AccountVaultStorage.install(AndroidAccountVault(context))
        ServiceLocator.initialize(apiBaseUrl, userAgent)
    }
}
