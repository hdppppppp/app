package com.tideengine.mobile.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.tideengine.mobile.api.dto.GameAccountDto
import com.tideengine.mobile.api.dto.TaskSwitchDto
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideDivider
import com.tideengine.mobile.ui.components.TideEmptyState
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.TideNoticeBanner
import com.tideengine.mobile.ui.components.TideStatRow
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.errorText
import com.tideengine.mobile.ui.components.formatIsoDate
import io.github.suqi8.coui.kmp.basic.Switch
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AccountsTab(
    refreshKey: Int,
    onRefresh: () -> Unit,
    onSessionExpired: () -> Unit,
    onOpenManager: (String) -> Unit,
) {
    var accounts by remember { mutableStateOf<List<GameAccountDto>>(emptyList()) }
    var switches by remember { mutableStateOf<List<TaskSwitchDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyKey by remember { mutableStateOf<String?>(null) }
    var pendingRemove by remember { mutableStateOf<String?>(null) }
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
            accounts = ServiceLocator.gameAccounts.list()
            switches = ServiceLocator.gameAccounts.listSwitches()
        } catch (throwable: Throwable) {
            handleFailure(throwable)
        } finally {
            loading = false
        }
    }

    fun setPrimary(account: GameAccountDto) {
        busyKey = account.id
        scope.launch {
            try {
                ServiceLocator.gameAccounts.setPrimary(account.id)
                accounts = ServiceLocator.gameAccounts.list()
                notice = "已将「${account.nickname}」设为默认账号"
            } catch (throwable: Throwable) {
                handleFailure(throwable)
            } finally {
                busyKey = null
            }
        }
    }

    fun remove(account: GameAccountDto) {
        busyKey = account.id
        scope.launch {
            try {
                ServiceLocator.gameAccounts.remove(account.id)
                accounts = accounts.filterNot { it.id == account.id }
                switches = switches.filterNot { it.gameAccountId == account.id }
                pendingRemove = null
                notice = "已解绑「${account.nickname}」"
            } catch (throwable: Throwable) {
                handleFailure(throwable)
            } finally {
                busyKey = null
            }
        }
    }

    fun toggleSwitch(account: GameAccountDto, item: TaskSwitchDto) {
        val key = "${account.id}:${item.handlerType}"
        busyKey = key
        scope.launch {
            try {
                val updated = if (item.enabled) {
                    ServiceLocator.gameAccounts.disableSwitch(account.id, item.handlerType)
                } else {
                    ServiceLocator.gameAccounts.enableSwitch(account.id, item.handlerType)
                }
                switches = switches.map {
                    if (it.gameAccountId == account.id && it.handlerType == item.handlerType) updated else it
                }
            } catch (throwable: Throwable) {
                handleFailure(throwable)
            } finally {
                busyKey = null
            }
        }
    }

    val switchesByAccount = switches.groupBy { it.gameAccountId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (loading) item { TideLoadingRow(text = "正在加载游戏账号…") }
        error?.let { message ->
            item { TideErrorBanner(message = message, onRetry = onRefresh) }
        }
        notice?.let { message ->
            item { TideNoticeBanner(message = message) }
        }
        if (!loading && accounts.isEmpty() && error == null) {
            item { TideEmptyState(text = "还没有绑定游戏账号，请先在 Web 端完成登录绑定") }
        }
        items(items = accounts, key = { it.id }) { account ->
            AccountCard(
                account = account,
                switches = switchesByAccount[account.id].orEmpty(),
                busyKey = busyKey,
                pendingRemove = pendingRemove == account.id,
                onSetPrimary = { setPrimary(account) },
                onAskRemove = { pendingRemove = account.id },
                onCancelRemove = { pendingRemove = null },
                onConfirmRemove = { remove(account) },
                onToggleSwitch = { toggleSwitch(account, it) },
                onOpenManager = { onOpenManager(account.id) },
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: GameAccountDto,
    switches: List<TaskSwitchDto>,
    busyKey: String?,
    pendingRemove: Boolean,
    onSetPrimary: () -> Unit,
    onAskRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onToggleSwitch: (TaskSwitchDto) -> Unit,
    onOpenManager: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    TideCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = account.nickname,
                color = scheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (account.isPrimary) {
                TideTag(
                    text = "默认",
                    color = scheme.onPrimaryContainer,
                    background = scheme.primaryContainer,
                )
            }
        }

        TideStatRow(label = "区服", value = account.serverName)
        TideStatRow(label = "角色 ID", value = account.roleId)
        TideStatRow(label = "绑定时间", value = formatIsoDate(account.boundAt))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!account.isPrimary) {
                TideGhostButton(
                    text = "设为默认",
                    enabled = busyKey == null,
                    onClick = onSetPrimary,
                )
            }
            TideGhostButton(text = "打开多开器", onClick = onOpenManager)
            TideGhostButton(
                text = if (pendingRemove) "确认解绑" else "解绑",
                danger = true,
                enabled = busyKey != account.id,
                onClick = if (pendingRemove) onConfirmRemove else onAskRemove,
            )
        }

        if (pendingRemove) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "解绑会删除该账号的 Bin 与凭据，且不可恢复。",
                    color = scheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
                TideGhostButton(text = "取消", onClick = onCancelRemove)
            }
        }

        if (switches.isNotEmpty()) {
            TideDivider()
            Text(
                text = "账号任务开关",
                color = scheme.onSurfaceSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            switches.forEach { item ->
                SwitchRow(
                    item = item,
                    busy = busyKey == "${account.id}:${item.handlerType}",
                    onToggle = { onToggleSwitch(item) },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    item: TaskSwitchDto,
    busy: Boolean,
    onToggle: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.label, color = scheme.onSurface, fontSize = 13.sp)
            Text(
                text = if (item.enabled) {
                    "下次执行 ${formatIsoDate(item.nextRunAt)}"
                } else {
                    item.description.ifBlank { "未启用" }
                },
                color = scheme.onSurfaceVariantSummary,
                fontSize = 11.sp,
            )
        }
        Switch(
            checked = item.enabled,
            onCheckedChange = { onToggle() },
            enabled = !busy,
        )
    }
}
