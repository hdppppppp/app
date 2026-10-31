package com.tideengine.mobile

import com.tideengine.mobile.api.ApiClient
import com.tideengine.mobile.auth.TokenStorage
import com.tideengine.mobile.data.AuthRepository
import com.tideengine.mobile.data.GameAccountRepository
import com.tideengine.mobile.data.OperationLogRepository
import com.tideengine.mobile.data.TaskRepository

/**
 * 轻量依赖容器。Kuikly 页面不持有 Context，网络与鉴权依赖在此集中初始化，
 * 由 androidApp 的 Application 在启动时调用 [initialize]。
 */
object ServiceLocator {
    private var initialized = false

    lateinit var api: ApiClient
        private set
    lateinit var auth: AuthRepository
        private set
    lateinit var gameAccounts: GameAccountRepository
        private set
    lateinit var tasks: TaskRepository
        private set
    lateinit var logs: OperationLogRepository
        private set

    var apiBaseUrl: String = ""
        private set

    fun initialize(baseUrl: String) {
        if (initialized) return
        apiBaseUrl = baseUrl.trimEnd('/')
        api = ApiClient(apiBaseUrl, { TokenStorage.load() }, { TokenStorage.clear() })
        auth = AuthRepository(api)
        gameAccounts = GameAccountRepository(api)
        tasks = TaskRepository(api)
        logs = OperationLogRepository(api)
        initialized = true
    }

    val isInitialized: Boolean
        get() = initialized
}
