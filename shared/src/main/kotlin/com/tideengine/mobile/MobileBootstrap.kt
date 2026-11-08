package com.tideengine.mobile

import android.content.Context
import com.tideengine.mobile.auth.AndroidTokenStore
import com.tideengine.mobile.auth.TokenStorage

/**
 * androidApp 在 Application.onCreate 调用一次：
 * 安装加密 token 存储并初始化网络容器。
 */
object MobileBootstrap {
    fun install(context: Context, apiBaseUrl: String) {
        TokenStorage.install(AndroidTokenStore(context))
        ServiceLocator.initialize(apiBaseUrl)
    }
}
