package com.tideengine.mobile.ui.pages


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.auth.SavedAccount
import com.tideengine.mobile.ui.components.AvatarImage
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideDivider
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideNoticeBanner
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.components.TideStatRow
import com.tideengine.mobile.ui.components.errorText
import io.github.suqi8.coui.kmp.basic.Surface
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ProfileTab(
    user: PublicUser,
    onUserUpdated: (PublicUser) -> Unit,
    onSessionExpired: () -> Unit,
    onLoggedOut: () -> Unit,
    otherAccounts: List<SavedAccount> = emptyList(),
    switching: Boolean = false,
    onAddAccount: () -> Unit = {},
    onSwitchAccount: (SavedAccount) -> Unit = {},
    bottomPadding: Dp = 0.dp,
    editingProfile: Boolean = false,
    onEditingProfileChange: (Boolean) -> Unit = {},
) {
    val scheme = COUITheme.colorScheme
    val scope = rememberCoroutineScope()

    var currentPassword by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var newPassword by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }

    fun changePassword() {
        if (currentPassword.text.isBlank() || newPassword.text.length < 8) {
            error = "请填写当前密码，新密码至少 8 位"
            return
        }
        saving = true
        error = null
        notice = null
        scope.launch {
            try {
                ServiceLocator.auth.changePassword(currentPassword.text, newPassword.text)
                currentPassword = TextFieldValue("")
                newPassword = TextFieldValue("")
                notice = "密码已更新，下次登录请使用新密码"
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (throwable is ApiException && throwable.isUnauthorized) {
                    onSessionExpired()
                } else {
                    error = errorText(throwable)
                }
            } finally {
                saving = false
            }
        }
    }

    fun logout() {
        ServiceLocator.auth.logout()
        confirmLogout = false
        onLoggedOut()
    }

    // 子页以侧滑进入 / 退出（AnimatedVisibility），系统返回手势经 BackHandler
    // 关闭子页而不是退出应用
    BackHandler(enabled = editingProfile) { onEditingProfileChange(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp + bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TideCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 头像：圆角方形（ColorOS 卡片风格），与编辑资料页一致
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = scheme.primary,
                            contentColor = scheme.onPrimary,
                            shadowElevation = 0.dp,
                        ) {
                            AvatarImage(
                                url = user.avatarUrl,
                                fallback = {
                                    Text(
                                        text = user.displayName.take(1).uppercase(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName,
                                color = scheme.labelPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "@${user.username}",
                                color = scheme.labelSecondaryVariant,
                                fontSize = 12.sp,
                            )
                        }

                        // 编辑资料入口（头像 / 昵称在子页内修改）
                        TideGhostButton(
                            text = "编辑资料",
                            onClick = { onEditingProfileChange(true) },
                        )
                    }

                    TideStatRow(label = "邮箱", value = user.email)
                }
            }

        item {
            TideCard {
                Text(
                    text = "账号切换",
                    color = scheme.labelPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (otherAccounts.isEmpty()) {
                    Text(
                        text = "暂无其他已记住账号。点「添加账号」登录另一个账号，之后可在这里一键切换。",
                        color = scheme.labelSecondaryVariant,
                        fontSize = 12.sp,
                    )
                } else {
                    otherAccounts.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = scheme.primary,
                                contentColor = scheme.onPrimary,
                            ) {
                                AvatarImage(
                                    url = entry.avatarUrl,
                                    fallback = {
                                        Text(
                                            text = entry.displayName.take(1).uppercase(),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    },
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.displayName,
                                    color = scheme.labelPrimary,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = "@${entry.username}",
                                    color = scheme.labelSecondaryVariant,
                                    fontSize = 11.sp,
                                )
                            }
                            TideGhostButton(
                                text = if (switching) "切换中" else "切换",
                                enabled = !switching,
                                onClick = { onSwitchAccount(entry) },
                            )
                        }
                    }
                }
                TideDivider()
                TideGhostButton(
                    text = "添加账号",
                    enabled = !switching,
                    onClick = onAddAccount,
                )
            }
        }

        item {
            TideCard {
                Text(
                    text = "修改密码",
                    color = scheme.labelPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                TextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "当前密码",
                    backgroundMode = TextFieldMode.Rectangle,
                    enabled = !saving,
                    showPasswordToggle = true,
                    singleLine = true,
                )
                TextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "新密码（至少 8 位）",
                    backgroundMode = TextFieldMode.Rectangle,
                    enabled = !saving,
                    showPasswordToggle = true,
                    singleLine = true,
                )
                error?.let { TideErrorBanner(message = it) }
                notice?.let { TideNoticeBanner(message = it) }
                TidePrimaryButton(
                    text = "保存新密码",
                    loading = saving,
                    enabled = !saving,
                    onClick = { changePassword() },
                )
            }
        }

        item {
            TideCard {
                TideStatRow(label = "服务地址", value = ServiceLocator.apiBaseUrl)
                TideStatRow(label = "多开管理器", value = "原生 WebView 容器")
                TideNoticeBanner(message = "Bin 仅保存在本机应用私有目录，不会上传到服务端。")
                TideDivider()
                if (confirmLogout) {
                    Text(
                        text = "确认退出当前账号？",
                        color = scheme.error,
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TideGhostButton(text = "取消", onClick = { confirmLogout = false })
                        TideGhostButton(text = "确认退出", danger = true, onClick = { logout() })
                    }
                } else {
                    TideGhostButton(
                        text = "退出登录",
                        danger = true,
                        onClick = { confirmLogout = true },
                    )
                }
            }
        }
    }

        // 编辑资料子页：侧滑进入 / 退出，覆盖在「我的」内容之上
        AnimatedVisibility(
            visible = editingProfile,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(tween(220)),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(tween(180)),
        ) {
            EditProfilePage(
                user = user,
                onUserUpdated = onUserUpdated,
                onSessionExpired = onSessionExpired,
                onBack = { onEditingProfileChange(false) },
            )
        }
    }
}
