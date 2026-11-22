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

    /**
     * 局域网探测 / 手动配置后切换服务端地址：ApiClient 切根即可让所有仓库
     * 生效，不重建依赖图。必须在探测结果验证通过后调用；启动探测完成前
     * 页面被 ServerGate 拦截，不会出现「一半请求打到旧地址」的窗口。
     */
    fun switchBaseUrl(url: String) {
        check(initialized) { "ServiceLocator 尚未初始化" }
        apiBaseUrl = url.trimEnd('/')
        api.useBaseUrl(apiBaseUrl)
    }

    val isInitialized: Boolean
        get() = initialized
}
