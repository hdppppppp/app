package com.tideengine.mobile.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideLogo
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.components.errorText
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

@Composable
fun LoginPage(onAuthenticated: (PublicUser) -> Unit) {
    val scheme = COUITheme.colorScheme
    val scope = rememberCoroutineScope()

    var identifier by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var password by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val account = identifier.text.trim()
        if (account.isBlank() || password.text.isBlank()) {
            error = "请输入账号和密码"
            return
        }
        if (loading) return
        loading = true
        error = null
        scope.launch {
            try {
                val response = ServiceLocator.auth.login(account, password.text, rememberMe)
                onAuthenticated(response.user)
            } catch (throwable: Throwable) {
                error = errorText(throwable)
            } finally {
                loading = false
            }
        }
    }

    // 垂直居中的整体构图：内容不满一屏时视觉重心在页面中部，超出时可滚动。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 头部：logo + 标题 + 副标题，全部居中
            TideLogo()
            Spacer(Modifier.height(18.dp))
            Text(
                text = "潮汐引擎",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "登录后管理游戏账号与自动任务",
                fontSize = 14.sp,
                color = scheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            // 输入区单独成卡；开关与按钮移出卡片，层次更清晰
            TideCard {
                TextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "邮箱或用户名",
                    backgroundMode = TextFieldMode.Rectangle,
                    enabled = !loading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "密码",
                    backgroundMode = TextFieldMode.Rectangle,
                    enabled = !loading,
                    showPasswordToggle = true,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "记住登录",
                    fontSize = 14.sp,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    enabled = !loading,
                )
            }

            error?.let { message ->
                Spacer(Modifier.height(10.dp))
                TideErrorBanner(message = message)
            }

            Spacer(Modifier.height(16.dp))

            TidePrimaryButton(
                text = "登录",
                enabled = !loading,
                loading = loading,
                onClick = { submit() },
            )

            Spacer(Modifier.height(24.dp))

            // 服务地址脚注：居中小字，与表单拉开距离，弱化成辅助信息
            Text(
                text = "服务地址 ${ServiceLocator.apiBaseUrl}",
                fontSize = 11.sp,
                color = scheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
