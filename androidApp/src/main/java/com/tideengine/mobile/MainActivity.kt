package com.tideengine.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tideengine.mobile.bridge.GameManagerActivity
import com.tideengine.mobile.ui.TideApp
import com.tideengine.mobile.ui.connect.ServerGate
import com.tideengine.mobile.ui.update.UpdateGate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as TideApplication
        val hotUpdate = application.hotUpdate
        setContent {
            // 服务端连接门禁：探测完成前拦截主界面，避免业务请求打到构建期
            // 兜底地址（探测命中的地址才会写入 ServiceLocator）。
            val connectState by application.serverConnect.state.collectAsState()
            val restricted by application.serverConnect.discoveryRestricted.collectAsState()
            ServerGate(
                state = connectState,
                restrictedMode = restricted,
                onRestrictedModeChange = { application.serverConnect.setDiscoveryRestricted(it) },
                onRetry = { application.serverConnect.retry() },
                onManualConnect = { application.serverConnect.connectManually(it) },
            ) {
                // 状态流与 Compose status 属性同源；这里用 StateFlow 订阅，
                // 由 UpdateGate 决定是否用强制更新页拦截主界面。
                val status by hotUpdate.updateManager.statusFlow.collectAsState()
                UpdateGate(
                    status = status,
                    onCheck = { hotUpdate.launchCheck(manual = true) },
                    onDownload = { hotUpdate.launchDownload() },
                    onInstall = { hotUpdate.updateManager.install() },
                    onDismiss = { hotUpdate.updateManager.dismiss() },
                    onRetry = { hotUpdate.launchRetry() },
                ) {
                    TideApp(
                        onOpenManager = { accountId ->
                            GameManagerActivity.start(this, accountId)
                        },
                    )
                }
            }
        }
    }
}
