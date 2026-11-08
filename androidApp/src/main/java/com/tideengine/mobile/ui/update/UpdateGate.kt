package com.tideengine.mobile.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taotao.music.update.UpdateStage
import com.taotao.music.update.UpdateStatus
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TidePrimaryButton
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay

/**
 * 热更新门禁：强制更新拦截页 + 可选更新对话框逻辑。
 *
 * 位置约定（上游 HOT_UPDATE.md 4.3）：门禁必须插在登录之前 —— 最需要强制更新的场景
 * 恰恰是「上一个版本把登录搞坏了」，坏版本可能连登录都是坏的。
 *
 * - [UpdateStatus.blocking]（强制更新且不在 IDLE/CHECKING/UP_TO_DATE）时整页拦截，
 *   不渲染 [content]；失败也不能放行，只能重试或直接跳安装。
 * - 非强制更新在主界面顶部以横幅提示（可关闭），划掉后本机记住版本号不再自动弹。
 * - 补丁下载与安装是后台静默动作（UpdateManager 内部处理），不占门禁 UI。
 */
@Composable
fun UpdateGate(
    status: UpdateStatus,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (status.blocking) {
        ForceUpdatePage(
            status = status,
            onInstall = onInstall,
            onDownload = onDownload,
            onRetry = onRetry,
        )
        return
    }
    content()
    if (status.stage == UpdateStage.AVAILABLE && !status.forced) {
        OptionalUpdateBanner(
            status = status,
            onDownload = onDownload,
            onInstall = onInstall,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ForceUpdatePage(
    status: UpdateStatus,
    onInstall: () -> Unit,
    onDownload: () -> Unit,
    onRetry: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "发现新版本",
                color = scheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            status.release?.let { release ->
                Text(
                    text = "${release.versionName}（${release.versionCode}）",
                    color = scheme.onSurfaceVariantSummary,
                    fontSize = 15.sp,
                )
                if (release.releaseNote.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surfaceContainerHigh)
                            .padding(14.dp),
                    ) {
                        Text(
                            text = release.releaseNote,
                            color = scheme.onSurface,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
            Text(
                text = "当前版本过旧，必须升级后才能继续使用",
                color = scheme.error,
                fontSize = 13.sp,
            )
            when (status.stage) {
                UpdateStage.DOWNLOADING -> {
                    Text(
                        text = "正在下载更新包 ${status.progressPercent}%",
                        color = scheme.onSurfaceVariantSummary,
                        fontSize = 13.sp,
                    )
                }
                UpdateStage.READY -> {
                    TidePrimaryButton(text = "立即安装", onClick = onInstall)
                    TideGhostButton(text = "重新下载", onClick = onDownload)
                }
                UpdateStage.FAILED -> {
                    Text(
                        text = status.error ?: "操作失败，请重试",
                        color = scheme.error,
                        fontSize = 12.sp,
                    )
                    TidePrimaryButton(text = "重试", onClick = onRetry)
                }
                else -> {
                    TidePrimaryButton(
                        text = if (status.release == null) "重新检查" else "下载更新",
                        onClick = {
                            if (status.release == null) onRetry() else onDownload()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionalUpdateBanner(
    status: UpdateStatus,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    // 下载中横幅保持常驻；READY 后短暂展示入口，避免长期遮挡主界面。
    var showReady by remember { mutableStateOf(true) }
    LaunchedEffect(status.stage) {
        if (status.stage == UpdateStage.READY) {
            delay(10_000)
            showReady = false
        }
    }
    val visible = when (status.stage) {
        UpdateStage.DOWNLOADING -> true
        UpdateStage.READY -> showReady
        else -> false
    }
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.surfaceContainerHigh)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val release = status.release
            Text(
                text = when (status.stage) {
                    UpdateStage.DOWNLOADING -> "正在下载更新 ${status.progressPercent}%"
                    else -> "新版本${release?.versionName?.let { " $it" } ?: ""}已就绪"
                },
                color = scheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (status.stage == UpdateStage.READY) {
                Text(
                    text = "安装包已下载并通过校验，点击立即安装",
                    color = scheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
            }
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (status.stage == UpdateStage.DOWNLOADING) {
                    TideGhostButton(text = "隐藏", onClick = onDismiss)
                } else {
                    TidePrimaryButton(text = "立即安装", onClick = onInstall)
                    TideGhostButton(text = "稍后", onClick = onDismiss)
                }
            }
        }
    }
}
