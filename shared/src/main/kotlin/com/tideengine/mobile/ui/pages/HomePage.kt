package com.tideengine.mobile.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.auth.SavedAccount
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.blur.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
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
    onUserUpdated: (PublicUser) -> Unit = {},
    otherAccounts: List<SavedAccount> = emptyList(),
    switching: Boolean = false,
    onAddAccount: () -> Unit = {},
    onSwitchAccount: (SavedAccount) -> Unit = {},
) {
    val scheme = COUITheme.colorScheme
    var tab by remember { mutableStateOf(HomeTab.Tasks) }
    var refreshKey by remember { mutableIntStateOf(0) }
    // 编辑资料子页打开时隐藏悬浮导航条（组件自带收起动画）
    var editingProfile by remember { mutableStateOf(false) }

    // 液态玻璃：Scaffold 内容整体录入 LayerBackdrop，悬浮导航条以 textureBlur
    // 取景「自己背后的内容」——滚动列表会从胶囊底下穿过。RuntimeShader 仅
    // API 33+ 可用，低版本设备回退组件默认实心底（不透明，不透内容）。
    val backdrop = rememberLayerBackdrop()
    val glass = isRuntimeShaderSupported()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barFootprint = 104.dp + navBottomInset

    Scaffold(
        containerColor = scheme.backgroundWithCard,
        topBar = {
            SmallTopAppBar(
                title = tab.label,
                subtitle = "潮汐引擎 · ${user.displayName}",
                color = scheme.backgroundWithCard,
                titleColor = scheme.labelPrimary,
                subtitleColor = scheme.labelSecondary,
                dividerColor = scheme.divider,
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
            // COUI 17 悬浮胶囊导航栏（组件自带底栏材质，随选中项形变）。
            // 组件默认的选中 / 未选中只差一层透明度（E6 vs B8 黑），肉眼几乎
            // 分不清当前页 —— 选中项显式用主题色加强指示。
            // 编辑资料子页打开时整条收起（组件 visible 自带下滑动画），让位给子页
            FloatingNavigationBar(visible = !(tab == HomeTab.Profile && editingProfile)) {
                HomeTab.entries.forEach { item ->
                    FloatingNavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = item.icon,
                        label = item.label,
                        colors = FloatingNavigationBarDefaults.itemColors(
                            selectedColor = scheme.primary,
                        ),
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        // 内容层铺满全屏（不避让底部导航条），列表自身用 barFootprint 留出
        // 滚动末端的呼吸位——这样导航条的玻璃才能「透出」滚动中的内容
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.backgroundWithCard)
                    .layerBackdrop(backdrop)
                    .padding(top = scaffoldPadding.calculateTopPadding())
                    .imePadding(),
            ) {
                when (tab) {
                    HomeTab.Tasks -> TasksTab(
                        refreshKey = refreshKey,
                        onRefresh = { refreshKey += 1 },
                        onSessionExpired = onSessionExpired,
                        bottomPadding = barFootprint,
                    )

                    HomeTab.Accounts -> AccountsTab(
                        refreshKey = refreshKey,
                        onRefresh = { refreshKey += 1 },
                        onSessionExpired = onSessionExpired,
                        onOpenManager = onOpenManager,
                        bottomPadding = barFootprint,
                    )

                    HomeTab.Logs -> LogsTab(
                        refreshKey = refreshKey,
                        onRefresh = { refreshKey += 1 },
                        onSessionExpired = onSessionExpired,
                        bottomPadding = barFootprint,
                    )

                HomeTab.Profile -> ProfileTab(
                    user = user,
                    onUserUpdated = onUserUpdated,
                    onSessionExpired = onSessionExpired,
                    onLoggedOut = onSessionExpired,
                    otherAccounts = otherAccounts,
                    switching = switching,
                    onAddAccount = onAddAccount,
                    onSwitchAccount = onSwitchAccount,
                    bottomPadding = if (editingProfile) 0.dp else barFootprint,
                    editingProfile = editingProfile,
                    onEditingProfileChange = { editingProfile = it },
                )
                }
            }
            // 悬浮胶囊导航：不占布局位，浮在内容之上；液态玻璃透出底下的内容
            FloatingNavigationBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                backgroundModifier = if (glass) {
                    Modifier.textureBlur(backdrop = backdrop, shape = RoundedCornerShape(100))
                } else {
                    Modifier
                },
                color = if (glass) Color.Transparent else FloatingNavigationBarDefaults.color(),
            ) {
                HomeTab.entries.forEach { item ->
                    FloatingNavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = item.icon,
                        label = item.label,
                        colors = FloatingNavigationBarDefaults.itemColors(
                            selectedColor = scheme.primary,
                        ),
                    )
                }
            }
        }
    }
}
