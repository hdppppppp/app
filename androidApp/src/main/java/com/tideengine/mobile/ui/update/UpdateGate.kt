package com.tideengine.mobile.ui.update

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import io.github.suqi8.coui.kmp.basic.LinearProgressIndicator
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/** 门禁与横幅的诊断日志 tag，与 UpdateManager 的检查日志同 tag 便于串联。 */
private const val LOG_TAG = "TideUpdate"

/** 可选更新横幅覆盖的阶段：发现新版本、下载中（横幅内嵌进度条）、已就绪、失败。 */
private val OPTIONAL_BANNER_STAGES = setOf(
    UpdateStage.AVAILABLE,
    UpdateStage.DOWNLOADING,
    UpdateStage.READY,
    UpdateStage.FAILED,
)

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
    // 门禁组合的诊断日志：AVAILABLE 横幅不出现时，先确认组合层是否收到状态
    Log.d(LOG_TAG, "gate compose: stage=${status.stage} forced=${status.forced} release=${status.release?.versionCode}")
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
    // 可选更新的反馈覆盖整个生命周期：AVAILABLE（发现新版本）→ DOWNLOADING
    // （横幅内嵌确定性进度条）→ READY（立即安装）→ FAILED（失败原因 + 重试），
    // 全程停留在底部横幅里——下载快慢都可见，不再用一闪而过的模态对话框
    // （真机反馈：模态对话框在快网下闪过，用户完全看不到下载进度）
    if (!status.forced && status.stage in OPTIONAL_BANNER_STAGES) {
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
            .background(scheme.backgroundWithCard)
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
                color = scheme.labelPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            status.release?.let { release ->
                Text(
                    text = "${release.versionName}（${release.versionCode}）",
                    color = scheme.labelSecondaryVariant,
                    fontSize = 15.sp,
                )
                if (release.releaseNote.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.cardBackground)
                            .padding(14.dp),
                    ) {
                        Text(
                            text = release.releaseNote,
                            color = scheme.labelPrimary,
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
                        color = scheme.labelSecondaryVariant,
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
    // 横幅在发现新版本 / 下载中 / 已就绪 / 失败四个阶段持续可见，直到用户点
    // 「稍后」或完成安装——下载进度内嵌在横幅里，快慢都能看到
    val visible = when (status.stage) {
        UpdateStage.AVAILABLE, UpdateStage.DOWNLOADING, UpdateStage.FAILED, UpdateStage.READY -> true
        else -> false
    }
    if (!visible) return
    val release = status.release
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 居中卡片：不占底部导航条区域，也不遮挡顶部栏；跟随系统深浅色
            .systemBarsPadding()
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.cardBackground)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = when (status.stage) {
                    UpdateStage.AVAILABLE -> "发现新版本${release?.versionName?.let { " $it" } ?: ""}"
                    UpdateStage.DOWNLOADING -> "正在下载更新 ${status.progressPercent}%"
                    UpdateStage.READY -> "新版本${release?.versionName?.let { " $it" } ?: ""}已就绪"
                    UpdateStage.FAILED -> "下载失败"
                    else -> "潮汐引擎更新"
                },
                color = scheme.labelPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (status.stage == UpdateStage.DOWNLOADING) {
                // 确定性进度条：百分比随下载实时推进
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = status.progressPercent / 100f,
                )
            }
            when (status.stage) {
                UpdateStage.AVAILABLE -> release?.releaseNote?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = scheme.labelSecondaryVariant,
                        fontSize = 12.sp,
                    )
                }
                UpdateStage.READY -> Text(
                    text = "安装包已下载并通过校验，点击立即安装",
                    color = scheme.labelSecondaryVariant,
                    fontSize = 12.sp,
                )
                UpdateStage.FAILED -> Text(
                    text = status.error ?: "下载失败，请重试",
                    color = scheme.error,
                    fontSize = 12.sp,
                )
                else -> Unit
            }
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (status.stage) {
                    UpdateStage.AVAILABLE -> {
                        TidePrimaryButton(text = "下载更新", onClick = onDownload)
                        TideGhostButton(text = "稍后", onClick = onDismiss)
                    }
                    UpdateStage.DOWNLOADING -> TideGhostButton(text = "隐藏", onClick = onDismiss)
                    UpdateStage.FAILED -> {
                        TidePrimaryButton(text = "重试下载", onClick = onDownload)
                        TideGhostButton(text = "稍后", onClick = onDismiss)
                    }
                    else -> {
                        TidePrimaryButton(text = "立即安装", onClick = onInstall)
                        TideGhostButton(text = "稍后", onClick = onDismiss)
                    }
                }
            }
        }
    }
}
