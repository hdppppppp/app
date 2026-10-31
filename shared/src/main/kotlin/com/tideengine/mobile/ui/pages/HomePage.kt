package com.tideengine.mobile.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.tideengine.mobile.api.dto.PublicUser
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.NavigationBar
import io.github.suqi8.coui.kmp.basic.NavigationBarItem
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Contacts
import io.github.suqi8.coui.kmp.icon.extended.Notes
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.icon.extended.Tasks
import io.github.suqi8.coui.kmp.theme.COUITheme

private enum class HomeTab(val label: String, val icon: ImageVector) {
    Tasks("任务", COUIIcons.Tasks),
    Accounts("账号", COUIIcons.Contacts),
    Logs("日志", COUIIcons.Notes),
    Profile("我的", COUIIcons.Settings),
}

@Composable
fun HomePage(
    user: PublicUser,
    onSessionExpired: () -> Unit,
    onOpenManager: (String) -> Unit,
) {
    val scheme = COUITheme.colorScheme
    var tab by remember { mutableStateOf(HomeTab.Tasks) }
    var refreshKey by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = scheme.background,
        topBar = {
            SmallTopAppBar(
                title = tab.label,
                subtitle = "潮汐引擎 · ${user.displayName}",
                color = scheme.background,
                titleColor = scheme.onBackground,
                subtitleColor = scheme.onBackgroundVariant,
                dividerColor = scheme.dividerLine,
                showDivider = true,
                actions = {
                    if (tab != HomeTab.Profile) {
                        TextButton(
                            text = "刷新",
                            onClick = { refreshKey += 1 },
                            colors = ButtonDefaults.textButtonColorsBorderless(
                                textColor = scheme.primary,
                            ),
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(color = scheme.background, showDivider = true) {
                HomeTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .imePadding(),
        ) {
            when (tab) {
                HomeTab.Tasks -> TasksTab(
                    refreshKey = refreshKey,
                    onRefresh = { refreshKey += 1 },
                    onSessionExpired = onSessionExpired,
                )

                HomeTab.Accounts -> AccountsTab(
                    refreshKey = refreshKey,
                    onRefresh = { refreshKey += 1 },
                    onSessionExpired = onSessionExpired,
                    onOpenManager = onOpenManager,
                )

                HomeTab.Logs -> LogsTab(
                    refreshKey = refreshKey,
                    onRefresh = { refreshKey += 1 },
                    onSessionExpired = onSessionExpired,
                )

                HomeTab.Profile -> ProfileTab(
                    user = user,
                    onSessionExpired = onSessionExpired,
                    onLoggedOut = onSessionExpired,
                )
            }
        }
    }
}
