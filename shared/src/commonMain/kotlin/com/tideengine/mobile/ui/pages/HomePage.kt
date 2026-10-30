package com.tideengine.mobile.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.ui.AppSession
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.theme.TideColors
import kotlinx.coroutines.launch

private val TABS = listOf("任务", "账号", "日志", "我的")

@Page("home")
class HomePage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            HomeScreen(
                onOpenManager = { accountId ->
                    val params = JSONObject().apply { put("accountId", accountId) }
                    router().openPage("gameManager", params)
                },
                onRequireLogin = {
                    router().openPage("login")
                    router().closePage()
                },
            )
        }
    }

    private fun router(): RouterModule = acquireModule<RouterModule>(RouterModule.MODULE_NAME)
}

@Composable
private fun HomeScreen(
    onOpenManager: (String) -> Unit,
    onRequireLogin: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun bootstrap() {
        if (!ServiceLocator.isInitialized) {
            error = "客户端尚未初始化，请重启应用"
            loading = false
            return
        }
        if (!ServiceLocator.auth.hasToken()) {
            onRequireLogin()
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                AppSession.user = ServiceLocator.auth.me()
            } catch (e: ApiException) {
                if (e.isUnauthorized) {
                    onRequireLogin()
                } else {
                    error = e.message
                }
            } catch (e: Throwable) {
                error = "加载账户信息失败，请稍后重试"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { bootstrap() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TideColors.Bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "潮汐引擎",
                    color = TideColors.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = AppSession.user?.displayName ?: "正在连接…",
                    color = TideColors.TextDim,
                    fontSize = 11.sp,
                )
            }
            if (error != null) {
                TideGhostButton(text = "重试", onClick = { bootstrap() })
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (loading) {
                TideLoadingRow(text = "正在加载账户信息…")
            } else if (error != null) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TideErrorBanner(message = error!!, onRetry = { bootstrap() })
                }
            } else {
                val contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 16.dp,
                )
                when (tab) {
                    0 -> TasksTab(contentPadding)
                    1 -> AccountsTab(contentPadding = contentPadding, onOpenManager = onOpenManager)
                    2 -> LogsTab(contentPadding)
                    else -> ProfileTab(contentPadding = contentPadding, onLoggedOut = onRequireLogin)
                }
            }
        }

        Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(TideColors.Border))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TideColors.Surface)
                .padding(vertical = 6.dp),
        ) {
            TABS.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { tab = index },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (tab == index) TideColors.Accent else TideColors.TextDim,
                        fontSize = 13.sp,
                        fontWeight = if (tab == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
