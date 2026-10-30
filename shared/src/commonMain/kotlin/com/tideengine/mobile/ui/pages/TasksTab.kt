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
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.TaskDto
import com.tideengine.mobile.api.dto.TaskScheduleType
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideEmptyState
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.TideNoticeBanner
import com.tideengine.mobile.ui.components.TideSectionTitle
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.formatIsoDate
import com.tideengine.mobile.ui.theme.TideColors
import kotlinx.coroutines.launch

@Composable
fun TasksTab(contentPadding: PaddingValues) {
    var tasks by remember { mutableStateOf<List<TaskDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                tasks = ServiceLocator.tasks.list(pageSize = 50).items
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "加载任务失败，请稍后重试"
            } finally {
                loading = false
            }
        }
    }

    fun toggle(task: TaskDto) {
        busyId = task.id
        notice = null
        scope.launch {
            try {
                ServiceLocator.tasks.setEnabled(task.id, !task.enabled)
                tasks = tasks.map { if (it.id == task.id) it.copy(enabled = !task.enabled) else it }
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "操作失败，请稍后重试"
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
                notice = when (execution?.status?.wireName) {
                    "succeeded" -> "任务「${task.name}」执行成功"
                    "failed" -> "任务「${task.name}」执行失败：${execution.error ?: "无错误详情"}"
                    "skipped" -> "任务「${task.name}」已跳过"
                    else -> "已提交执行请求"
                }
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "触发执行失败，请稍后重试"
            } finally {
                busyId = null
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
            TideSectionTitle(text = "自动任务") {
                TideGhostButton(text = "刷新", onClick = { load() })
            }
        }
        if (loading) item { TideLoadingRow(text = "正在加载任务…") }
        error?.let { message ->
            item { TideErrorBanner(message = message, onRetry = { load() }) }
        }
        notice?.let { message ->
            item { TideNoticeBanner(message = message) }
        }
        if (!loading && tasks.isEmpty() && error == null) {
            item { TideEmptyState(text = "暂无任务，可在 Web 端创建后下拉刷新") }
        }
        items(tasks.size) { index ->
            val task = tasks[index]
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
    TideCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = task.name,
                color = TideColors.Text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TideTag(
                text = if (task.enabled) "已启用" else "已停用",
                color = if (task.enabled) TideColors.Success else TideColors.TextDim,
                background = if (task.enabled) TideColors.SuccessSoft else TideColors.Surface3,
            )
        }

        Text(
            text = "${task.handlerType} · ${task.scheduleType.label()}",
            color = TideColors.TextSoft,
            fontSize = 12.sp,
        )
        Text(
            text = "下次执行 ${formatIsoDate(task.nextRunAt)} · 上次执行 ${formatIsoDate(task.lastRunAt)}",
            color = TideColors.TextDim,
            fontSize = 11.sp,
        )
        TaskDescription(task.description)

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
                text = "立即执行",
                enabled = !busy && task.enabled,
                onClick = onRun,
            )
        }
    }
}

@Composable
private fun TaskDescription(description: String) {
    if (description.isBlank()) return
    Text(text = description, color = TideColors.TextDim, fontSize = 12.sp, maxLines = 2)
}

private fun TaskScheduleType.label(): String = when (this) {
    TaskScheduleType.Once -> "仅一次"
    TaskScheduleType.Daily -> "每天"
    TaskScheduleType.Weekly -> "每周"
    TaskScheduleType.Monthly -> "每月"
    TaskScheduleType.Interval -> "固定间隔"
}

private val com.tideengine.mobile.api.dto.TaskExecutionStatus.wireName: String
    get() = when (this) {
        com.tideengine.mobile.api.dto.TaskExecutionStatus.Running -> "running"
        com.tideengine.mobile.api.dto.TaskExecutionStatus.Succeeded -> "succeeded"
        com.tideengine.mobile.api.dto.TaskExecutionStatus.Failed -> "failed"
        com.tideengine.mobile.api.dto.TaskExecutionStatus.Skipped -> "skipped"
    }
