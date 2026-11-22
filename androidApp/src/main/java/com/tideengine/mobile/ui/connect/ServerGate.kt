package com.tideengine.mobile.ui.connect

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.connect.ServerConnectState
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.TideLogo
import com.tideengine.mobile.ui.components.TidePrimaryButton
import io.github.suqi8.coui.kmp.basic.InputView
import io.github.suqi8.coui.kmp.basic.InputViewDefaults
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
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
    restrictedMode: Boolean,
    onRestrictedModeChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onManualConnect: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    when (state) {
        is ServerConnectState.Ready -> content()
        is ServerConnectState.Discovering -> ConnectPage(
            state = state,
            restrictedMode = restrictedMode,
            onRestrictedModeChange = onRestrictedModeChange,
            onRetry = null,
            onManualConnect = onManualConnect,
        )
        is ServerConnectState.Failed -> ConnectPage(
            state = state,
            restrictedMode = restrictedMode,
            onRestrictedModeChange = onRestrictedModeChange,
            onRetry = onRetry,
            onManualConnect = onManualConnect,
        )
    }
}

@Composable
private fun ConnectPage(
    state: ServerConnectState,
    restrictedMode: Boolean,
    onRestrictedModeChange: (Boolean) -> Unit,
    onRetry: (() -> Unit)?,
    onManualConnect: (String) -> Unit,
) {
    val scheme = COUITheme.colorScheme
    var manualVisible by rememberSaveable { mutableStateOf(false) }
    var manualInput by rememberSaveable { mutableStateOf("") }

    // 与登录页同一套构图：垂直居中 + 居中品牌头部 + 分层控件
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.backgroundWithCard)
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
            TideLogo(size = 64, fontSize = 26)
            Spacer(Modifier.height(18.dp))
            Text(
                text = "潮汐引擎",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.labelPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "正在寻找局域网内的服务端",
                fontSize = 14.sp,
                color = scheme.labelSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            when (state) {
                is ServerConnectState.Discovering -> TideLoadingRow(text = state.step)
                is ServerConnectState.Failed -> TideErrorBanner(
                    message = state.message,
                    onRetry = null,
                )
                else -> Unit
            }

            if (onRetry != null) {
                Spacer(Modifier.height(4.dp))
                TidePrimaryButton(text = "重新探测", onClick = onRetry)
            }

            Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(16.dp))
            RestrictedModeToggle(restricted = restrictedMode, onChange = onRestrictedModeChange)
        }
    }
}

/**
 * 公共网络模式开关：开启后跳过 UDP 广播与子网扫描，只信任记忆地址、构建期
 * 默认值与手动输入 —— 不可信 Wi-Fi 下恶意设备可以伪装探测应答把登录凭据
 * 引到假服务端，这是局域网发现机制的固有短板，开关是显式的用户逃生通道。
 * 切换会立即保存并按新模式重新探测。
 */
@Composable
private fun RestrictedModeToggle(
    restricted: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "公共网络模式",
                color = scheme.labelPrimary,
                fontSize = 14.sp,
            )
            Text(
                text = "不发广播、不扫描网段，仅记忆地址或手动输入。公共 Wi-Fi 下建议开启。",
                color = scheme.labelSecondaryVariant,
                fontSize = 11.sp,
            )
        }
        Switch(checked = restricted, onCheckedChange = onChange)
    }
}

@Composable
private fun ManualAddressForm(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        InputView(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            title = "服务地址",
            label = "例如 192.168.31.17:3100",
            enabled = enabled,
            // 清除按钮按条件出现会撑高输入行（登录页账号框同因），一并关闭；
            // 卡片底色同登录页：默认 cardBackground 白底上隐形
            showClearButton = false,
            colors = InputViewDefaults.inputViewColors(
                cardColor = COUITheme.colorScheme.cardBackground,
            ),
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
