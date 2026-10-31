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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.components.errorText
import io.github.suqi8.coui.kmp.basic.Surface
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "潮",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "潮汐引擎",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                )
                Text(
                    text = "登录后管理游戏账号与自动任务",
                    fontSize = 14.sp,
                    color = scheme.onBackgroundVariant,
                )
            }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    TideErrorBanner(message = message)
                }

                TidePrimaryButton(
                    text = "登录",
                    enabled = !loading,
                    loading = loading,
                    onClick = { submit() },
                )
            }

            Text(
                text = "服务地址：${ServiceLocator.apiBaseUrl}",
                fontSize = 11.sp,
                color = scheme.onBackgroundVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
