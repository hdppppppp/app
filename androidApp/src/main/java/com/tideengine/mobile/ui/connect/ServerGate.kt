package com.tideengine.mobile.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.connect.ServerConnectState
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.TidePrimaryButton
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * 服务端连接门禁：探测完成（Ready）之前拦截主界面。
 *
 * 必须拦住的理由：ServiceLocator 的地址在探测命中前是构建期兜底值，放行主界面
 * 会让 /api/auth/me 与热更新检查打到错误地址，401 清理逻辑会把用户误踢回登录页。
 * 探测失败时永远提供「手动输入地址」—— 探测只是优化，不能把用户锁死在门外。
 */
@Composable
fun ServerGate(
    state: ServerConnectState,
    onRetry: () -> Unit,
    onManualConnect: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    when (state) {
        is ServerConnectState.Ready -> content()
        is ServerConnectState.Discovering -> ConnectPage(
            state = state,
            onRetry = null,
            onManualConnect = onManualConnect,
        )
        is ServerConnectState.Failed -> ConnectPage(
            state = state,
            onRetry = onRetry,
            onManualConnect = onManualConnect,
        )
    }
}

@Composable
private fun ConnectPage(
    state: ServerConnectState,
    onRetry: (() -> Unit)?,
    onManualConnect: (String) -> Unit,
) {
    val scheme = COUITheme.colorScheme
    var manualVisible by rememberSaveable { mutableStateOf(false) }
    var manualInput by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "潮汐引擎",
                color = scheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            when (state) {
                is ServerConnectState.Discovering -> {
                    TideLoadingRow(text = state.step)
                }
                is ServerConnectState.Failed -> {
                    TideErrorBanner(message = state.message, onRetry = null)
                }
                else -> Unit
            }
            if (onRetry != null) {
                TidePrimaryButton(text = "重新探测", onClick = onRetry)
            }
            if (!manualVisible) {
                TideGhostButton(
                    text = if (onRetry == null) "手动指定服务地址" else "手动输入服务地址",
                    onClick = { manualVisible = true },
                )
            } else {
                ManualAddressForm(
                    value = manualInput,
                    enabled = true,
                    onValueChange = { manualInput = it },
                    onConnect = { onManualConnect(manualInput) },
                )
            }
        }
    }
}

@Composable
private fun ManualAddressForm(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "输入服务端地址，例如 192.168.31.17:3100",
            color = scheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = "服务地址",
            backgroundMode = TextFieldMode.Rectangle,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
        )
        TidePrimaryButton(
            text = "连接",
            onClick = onConnect,
            enabled = enabled && value.isNotBlank(),
        )
    }
}
