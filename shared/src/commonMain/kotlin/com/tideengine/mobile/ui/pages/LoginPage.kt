package com.tideengine.mobile.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.input.PasswordVisualTransformation
import com.tencent.kuikly.compose.ui.text.input.TextFieldValue
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.ui.AppSession
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.theme.TideColors
import kotlinx.coroutines.launch

@Page("login")
class LoginPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            LoginScreen(
                onAuthenticated = {
                    router().openPage("home")
                    router().closePage()
                },
            )
        }
    }

    private fun router(): RouterModule = acquireModule<RouterModule>(RouterModule.MODULE_NAME)
}

@Composable
private fun LoginScreen(onAuthenticated: () -> Unit) {
    var identifier by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (identifier.text.isBlank() || password.text.isBlank()) {
            error = "请输入账号和密码"
            return
        }
        if (loading) return
        loading = true
        error = null
        scope.launch {
            try {
                if (!ServiceLocator.isInitialized) {
                    error = "客户端尚未初始化，请重启应用"
                    return@launch
                }
                val response = ServiceLocator.auth.login(identifier.text.trim(), password.text)
                AppSession.user = response.user
                onAuthenticated()
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "登录失败，请稍后重试"
            } finally {
                loading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "潮汐引擎",
                    color = TideColors.Text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "登录后管理游戏账号、自动任务与操作日志",
                    color = TideColors.TextDim,
                    fontSize = 13.sp,
                )
            }
        }

        item {
            TideCard {
                Text(text = "账号登录", color = TideColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

                TextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("用户名或邮箱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                error?.let { message -> TideErrorBanner(message = message) }

                TidePrimaryButton(
                    text = "登录",
                    loading = loading,
                    onClick = { submit() },
                )

                Text(
                    text = "账号由管理员创建，或通过 Web 端注册后使用",
                    color = TideColors.TextDim,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
