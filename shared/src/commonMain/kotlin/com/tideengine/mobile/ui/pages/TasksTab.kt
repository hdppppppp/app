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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.TaskDto
import com.tideengine.mobile.api.dto.TaskExecutionStatus
import com.tideengine.mobile.api.dto.TaskScheduleType
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideEmptyState
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.TideNoticeBanner
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.errorText
import com.tideengine.mobile.ui.components.formatIsoDate
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun TasksTab(
    refreshKey: Int,
    onRefresh: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    var tasks by remember { mutableStateOf<List<TaskDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun handleFailure(throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        if (throwable is ApiException && throwable.isUnauthorized) {
            onSessionExpired()
        } else {
            error = errorText(throwable)
        }
    }

    LaunchedEffect(refreshKey) {
        loading = true
        error = null
        notice = null
        try {
            tasks = ServiceLocator.tasks.list(pageSize = 50).items
        } catch (throwable: Throwable) {
            handleFailure(throwable)
        } finally {
            loading = false
        }
    }

    fun toggle(task: TaskDto) {
        busyId = task.id
        notice = null
        scope.launch {
            try {
                ServiceLocator.tasks.setEnabled(task.id, !task.enabled)
                tasks = tasks.map { if (it.id == task.id) it.copy(enabled = !task.enabled) else it }
            } catch (throwable: Throwable) {
                handleFailure(throwable)
            } finally {
                busyId = null
            }
        }
    }

    fun run(task: TaskDto) {
        busyId = task.id
        notice = null
        scope.launch {
            try {
                val execution = ServiceLocator.tasks.run(task.id)
                notice = when (execution?.status) {
                    TaskExecutionStatus.Succeeded -> "任务「${task.name}」执行成功"
                    TaskExecutionStatus.Failed -> "任务「${task.name}」执行失败：${execution?.error ?: "无错误详情"}"
                    TaskExecutionStatus.Skipped -> "任务「${task.name}」已跳过"
                    else -> "已提交执行请求"
                }
            } catch (throwable: Throwable) {
                handleFailure(throwable)
            } finally {
                busyId = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) item { TideLoadingRow(text = "正在加载任务…") }
        error?.let { message ->
            item { TideErrorBanner(message = message, onRetry = onRefresh) }
        }
        notice?.let { message ->
            item { TideNoticeBanner(message = message) }
        }
        if (!loading && tasks.isEmpty() && error == null) {
            item { TideEmptyState(text = "暂无任务，可在 Web 端创建后刷新") }
        }
        items(items = tasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                busy = busyId == task.id,
                onToggle = { toggle(task) },
                onRun = { run(task) },
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskDto,
    busy: Boolean,
    onToggle: () -> Unit,
    onRun: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    TideCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = task.name,
                color = scheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TideTag(
                text = if (task.enabled) "已启用" else "已停用",
                color = if (task.enabled) scheme.onPrimaryContainer else scheme.onSurfaceVariantSummary,
                background = if (task.enabled) scheme.primaryContainer else scheme.surfaceContainerHigh,
            )
        }

        Text(
            text = "${task.handlerType} · ${task.scheduleType.label()}",
            color = scheme.onSurfaceSecondary,
            fontSize = 12.sp,
        )
        Text(
            text = "下次执行 ${formatIsoDate(task.nextRunAt)} · 上次执行 ${formatIsoDate(task.lastRunAt)}",
            color = scheme.onSurfaceVariantSummary,
            fontSize = 11.sp,
        )
        if (task.description.isNotBlank()) {
            Text(
                text = task.description,
                color = scheme.onSurfaceVariantSummary,
                fontSize = 12.sp,
                maxLines = 2,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TideGhostButton(
                text = if (task.enabled) "停用" else "启用",
                enabled = !busy,
                onClick = onToggle,
            )
            TideGhostButton(
                text = if (busy) "执行中…" else "立即执行",
                enabled = !busy && task.enabled,
                onClick = onRun,
            )
        }
    }
}

private fun TaskScheduleType.label(): String = when (this) {
    TaskScheduleType.Once -> "仅一次"
    TaskScheduleType.Daily -> "每天"
    TaskScheduleType.Weekly -> "每周"
    TaskScheduleType.Monthly -> "每月"
    TaskScheduleType.Interval -> "固定间隔"
}
