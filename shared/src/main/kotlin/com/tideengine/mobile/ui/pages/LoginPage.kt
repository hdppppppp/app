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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.ui.components.TideLogo
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.components.errorText
import com.tideengine.mobile.ui.legal.PRIVACY_POLICY
import com.tideengine.mobile.ui.legal.USER_AGREEMENT
import com.tideengine.mobile.ui.legal.DocumentSheet
import io.github.suqi8.coui.kmp.basic.Checkbox
import io.github.suqi8.coui.kmp.basic.InputView
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

@Composable
fun LoginPage(onAuthenticated: (PublicUser) -> Unit) {
    val scheme = COUITheme.colorScheme
    val scope = rememberCoroutineScope()

    // InputView 只接收纯 String，状态随之简化（不再需要 TextFieldValue.Saver）
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // 协议文档在 App 内以浮层展示，不跳网页（WebView 冷启动代价高、体验差）
    var legalDoc by remember { mutableStateOf<com.tideengine.mobile.ui.legal.AgreementDoc?>(null) }

    fun submit() {
        val account = identifier.trim()
        if (account.isBlank() || password.isBlank()) {
            error = "请输入账号和密码"
            return
        }
        if (loading) return
        loading = true
        error = null
        scope.launch {
            try {
                val response = ServiceLocator.auth.login(account, password, rememberMe)
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

            // 输入区：COUI InputView（ColorOS 设置同款「卡片单输入」）。账号框
            // 不开 showClearButton —— 清除按钮按焦点与内容条件出现，出现时撑高
            // 输入行导致视觉抖动；密码可见切换是常驻元素，高度恒定可保留
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InputView(
                    value = identifier,
                    onValueChange = { identifier = it },
                    modifier = Modifier.fillMaxWidth(),
                    title = "账号",
                    label = "邮箱或用户名",
                    enabled = !loading,
                    errorMessage = error.orEmpty(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )

                InputView(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    title = "密码",
                    label = "请输入密码",
                    enabled = !loading,
                    showPasswordToggle = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                )
            }

            // 记住登录：Checkbox 在左、文案在右，整组居中
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    state = if (rememberMe) ToggleableState.On else ToggleableState.Off,
                    onClick = { rememberMe = !rememberMe },
                    enabled = !loading,
                )
                Text(
                    text = "记住登录",
                    fontSize = 14.sp,
                    color = scheme.onSurface,
                )
            }

            Spacer(Modifier.height(16.dp))

            TidePrimaryButton(
                text = "登录",
                enabled = !loading,
                loading = loading,
                onClick = { submit() },
            )

            Spacer(Modifier.height(20.dp))

            // 协议声明：与 Web 端登录页一致；链接在 App 内浮层打开，不走浏览器。
            // 链接颜色用外层 withStyle 提供（TextLinkStyles 构造参数各版本不一致，
            // 不直接依赖它）
            Text(
                text = buildAnnotatedString {
                    append("登录即表示您同意")
                    withStyle(SpanStyle(color = scheme.primary)) {
                        withLink(
                            LinkAnnotation.Clickable("user") { legalDoc = USER_AGREEMENT },
                        ) { append("《用户协议》") }
                    }
                    append("与")
                    withStyle(SpanStyle(color = scheme.primary)) {
                        withLink(
                            LinkAnnotation.Clickable("privacy") { legalDoc = PRIVACY_POLICY },
                        ) { append("《隐私政策》") }
                    }
                },
                fontSize = 12.sp,
                color = scheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(10.dp))

            // 服务地址脚注：只展示主机与端口，去掉协议头，居中弱化
            Text(
                text = "服务地址 ${ServiceLocator.apiBaseUrl.substringAfter("://")}",
                fontSize = 11.sp,
                color = scheme.onBackgroundVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        legalDoc?.let { doc ->
            DocumentSheet(doc = doc, onClose = { legalDoc = null })
        }
    }
}
