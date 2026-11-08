package com.tideengine.mobile.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.OperationLogDto
import com.tideengine.mobile.api.dto.OperationLogStatus
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideEmptyState
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.errorText
import com.tideengine.mobile.ui.components.formatIsoDate
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CancellationException

@Composable
fun LogsTab(
    refreshKey: Int,
    onRefresh: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    var logs by remember { mutableStateOf<List<OperationLogDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        try {
            logs = ServiceLocator.logs.list(pageSize = 30).items
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (throwable is ApiException && throwable.isUnauthorized) {
                onSessionExpired()
            } else {
                error = errorText(throwable)
            }
        } finally {
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) item { TideLoadingRow(text = "正在加载日志…") }
        error?.let { message ->
            item { TideErrorBanner(message = message, onRetry = onRefresh) }
        }
        if (!loading && logs.isEmpty() && error == null) {
            item { TideEmptyState(text = "暂无操作日志") }
        }
        items(items = logs, key = { it.id }) { log ->
            LogRow(log)
        }
    }
}

@Composable
private fun LogRow(log: OperationLogDto) {
    val scheme = COUITheme.colorScheme
    TideCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = log.taskName.ifBlank { log.handlerType },
                color = scheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TideTag(
                text = log.status.label(),
                color = log.status.contentColor(),
                background = log.status.containerColor(),
            )
        }

        Text(
            text = "${log.action} · ${log.nickname ?: "—"} · ${formatIsoDate(log.createdAt)}",
            color = scheme.onSurfaceVariantSummary,
            fontSize = 11.sp,
        )

        if (!log.error.isNullOrBlank()) {
            Text(text = log.error, color = scheme.error, fontSize = 12.sp)
        }

        if (log.durationMs != null) {
            Text(
                text = "耗时 ${log.durationMs} ms · 触发方式 ${log.triggeredBy}",
                color = scheme.onSurfaceVariantSummary,
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

@Composable
private fun OperationLogStatus.contentColor(): Color {
    val scheme = COUITheme.colorScheme
    return when (this) {
        OperationLogStatus.Succeeded -> scheme.onPrimaryContainer
        OperationLogStatus.Failed -> scheme.onErrorContainer
        OperationLogStatus.Skipped -> scheme.onSurfaceVariantSummary
    }
}

@Composable
private fun OperationLogStatus.containerColor(): Color {
    val scheme = COUITheme.colorScheme
    return when (this) {
        OperationLogStatus.Succeeded -> scheme.primaryContainer
        OperationLogStatus.Failed -> scheme.errorContainer
        OperationLogStatus.Skipped -> scheme.surfaceContainerHigh
    }
}
