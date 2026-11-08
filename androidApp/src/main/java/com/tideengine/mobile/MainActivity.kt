package com.tideengine.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tideengine.mobile.bridge.GameManagerActivity
import com.tideengine.mobile.ui.TideApp
import com.tideengine.mobile.ui.update.UpdateGate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as TideApplication
        val hotUpdate = application.hotUpdate
        setContent {
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
