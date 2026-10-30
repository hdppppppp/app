package com.tideengine.mobile.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.OperationLogDto
import com.tideengine.mobile.api.dto.OperationLogStatus
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideEmptyState
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.TideSectionTitle
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.formatIsoDate
import com.tideengine.mobile.ui.theme.TideColors
import kotlinx.coroutines.launch

@Composable
fun LogsTab(contentPadding: PaddingValues) {
    var logs by remember { mutableStateOf<List<OperationLogDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                logs = ServiceLocator.logs.list(pageSize = 30).items
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "加载操作日志失败，请稍后重试"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TideSectionTitle(text = "操作日志") {
                TideGhostButton(text = "刷新", onClick = { load() })
            }
        }
        if (loading) item { TideLoadingRow(text = "正在加载日志…") }
        error?.let { message ->
            item { TideErrorBanner(message = message, onRetry = { load() }) }
        }
        if (!loading && logs.isEmpty() && error == null) {
            item { TideEmptyState(text = "暂无操作日志") }
        }
        items(logs.size) { index ->
            LogRow(logs[index])
        }
    }
}

@Composable
private fun LogRow(log: OperationLogDto) {
    TideCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = log.taskName.ifBlank { log.handlerType },
                color = TideColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TideTag(
                text = log.status.label(),
                color = log.status.color(),
                background = log.status.background(),
            )
        }

        Text(
            text = "${log.action} · ${log.nickname ?: "—"} · ${formatIsoDate(log.createdAt)}",
            color = TideColors.TextDim,
            fontSize = 11.sp,
        )

        if (!log.error.isNullOrBlank()) {
            Text(text = log.error, color = TideColors.Danger, fontSize = 12.sp)
        }

        if (log.durationMs != null) {
            Text(
                text = "耗时 ${log.durationMs} ms · 触发方式 ${log.triggeredBy}",
                color = TideColors.TextDim,
                fontSize = 11.sp,
            )
        }
    }
}

private fun OperationLogStatus.label(): String = when (this) {
    OperationLogStatus.Succeeded -> "成功"
    OperationLogStatus.Failed -> "失败"
    OperationLogStatus.Skipped -> "跳过"
}

private fun OperationLogStatus.color(): Color = when (this) {
    OperationLogStatus.Succeeded -> TideColors.Success
    OperationLogStatus.Failed -> TideColors.Danger
    OperationLogStatus.Skipped -> TideColors.Warning
}

private fun OperationLogStatus.background(): Color = when (this) {
    OperationLogStatus.Succeeded -> TideColors.SuccessSoft
    OperationLogStatus.Failed -> TideColors.DangerSoft
    OperationLogStatus.Skipped -> TideColors.WarningSoft
}
