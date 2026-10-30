package com.tideengine.mobile.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Switch
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
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
import com.tideengine.mobile.ui.components.TideSectionTitle
import com.tideengine.mobile.ui.components.TideStatRow
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.formatIsoDate
import com.tideengine.mobile.ui.theme.TideColors
import kotlinx.coroutines.launch

@Composable
fun AccountsTab(
    contentPadding: PaddingValues,
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

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                accounts = ServiceLocator.gameAccounts.list()
                switches = ServiceLocator.gameAccounts.listSwitches()
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "加载游戏账号失败，请稍后重试"
            } finally {
                loading = false
            }
        }
    }

    fun setPrimary(account: GameAccountDto) {
        busyKey = account.id
        scope.launch {
            try {
                ServiceLocator.gameAccounts.setPrimary(account.id)
                accounts = ServiceLocator.gameAccounts.list()
                notice = "已将「${account.nickname}」设为默认账号"
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "设置默认账号失败"
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
                pendingRemove = null
                load()
                notice = "已解绑「${account.nickname}」"
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "解绑失败，请稍后重试"
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
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "切换任务失败，请稍后重试"
            } finally {
                busyKey = null
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    val switchesByAccount = switches.groupBy { it.gameAccountId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TideSectionTitle(text = "游戏账号") {
                TideGhostButton(text = "刷新", onClick = { load() })
            }
        }
        if (loading) item { TideLoadingRow(text = "正在加载游戏账号…") }
        error?.let { message ->
            item { TideErrorBanner(message = message, onRetry = { load() }) }
        }
        notice?.let { message ->
            item { TideNoticeBanner(message = message) }
        }
        if (!loading && accounts.isEmpty() && error == null) {
            item { TideEmptyState(text = "还没有绑定游戏账号，请先在 Web 端完成登录绑定") }
        }
        items(accounts.size) { index ->
            val account = accounts[index]
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
    TideCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = account.nickname,
                color = TideColors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (account.isPrimary) {
                TideTag(
                    text = "默认",
                    color = TideColors.AccentStrong,
                    background = TideColors.AccentSoft,
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
                    color = TideColors.Danger,
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
                color = TideColors.TextSoft,
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.label, color = TideColors.Text, fontSize = 13.sp)
            Text(
                text = if (item.enabled) {
                    "下次执行 ${formatIsoDate(item.nextRunAt)}"
                } else {
                    item.description.ifBlank { "未启用" }
                },
                color = TideColors.TextDim,
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
