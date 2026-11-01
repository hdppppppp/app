package com.tideengine.mobile.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.api.dto.UserRole
import com.tideengine.mobile.api.dto.UserStatus
import com.tideengine.mobile.ui.components.AvatarImage
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideDivider
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideNoticeBanner
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.components.TideStatRow
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.createAvatarDataUrl
import com.tideengine.mobile.ui.components.errorText
import com.tideengine.mobile.ui.components.formatIsoDate
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
) {
    val scheme = COUITheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

    var avatarBusy by remember { mutableStateOf(false) }
    var confirmResetAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    var avatarNotice by remember { mutableStateOf<String?>(null) }

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

    suspend fun uploadAvatar(uri: Uri) {
        avatarBusy = true
        avatarError = null
        avatarNotice = null
        confirmResetAvatar = false
        try {
            val avatar = createAvatarDataUrl(context, uri)
            val updated = ServiceLocator.auth.updateAvatar(avatar)
            avatarNotice = "头像已更新"
            onUserUpdated(updated)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (throwable is ApiException && throwable.isUnauthorized) {
                onSessionExpired()
            } else {
                avatarError = errorText(throwable)
            }
        } finally {
            avatarBusy = false
        }
    }

    val pickAvatarImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            scope.launch { uploadAvatar(uri) }
        }
    }

    fun pickAvatar() {
        if (avatarBusy) return
        pickAvatarImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun resetAvatar() {
        if (avatarBusy) return
        confirmResetAvatar = false
        avatarError = null
        avatarNotice = null
        avatarBusy = true
        scope.launch {
            try {
                val updated = ServiceLocator.auth.resetAvatar()
                avatarNotice = "已恢复默认头像"
                onUserUpdated(updated)
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
                if (throwable is ApiException && throwable.isUnauthorized) {
                    onSessionExpired()
                } else {
                    avatarError = errorText(throwable)
                }
            } finally {
                avatarBusy = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
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
                            color = scheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "@${user.username}",
                            color = scheme.onSurfaceVariantSummary,
                            fontSize = 12.sp,
                        )
                    }

                    TideTag(text = user.role.label())
                }

                TideStatRow(label = "邮箱", value = user.email)
                TideStatRow(label = "角色", value = user.role.label())
                TideStatRow(label = "状态", value = user.status.label())
                TideStatRow(label = "上次登录", value = formatIsoDate(user.lastLoginAt))

                TideDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TideGhostButton(
                        text = if (user.avatarUrl.isNullOrEmpty()) "上传头像" else "更换头像",
                        enabled = !avatarBusy,
                        onClick = { pickAvatar() },
                    )
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        TideGhostButton(
                            text = "恢复默认",
                            enabled = !avatarBusy,
                            danger = true,
                            onClick = { confirmResetAvatar = !confirmResetAvatar },
                        )
                    }
                }
                if (confirmResetAvatar) {
                    Text(
                        text = "确认恢复默认头像？",
                        color = scheme.error,
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TideGhostButton(text = "取消", onClick = { confirmResetAvatar = false })
                        TideGhostButton(
                            text = "确认恢复",
                            danger = true,
                            enabled = !avatarBusy,
                            onClick = { resetAvatar() },
                        )
                    }
                }
                Text(
                    text = "支持 JPG、PNG 或 WebP，保存前会自动裁切为正方形并压缩。",
                    color = scheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                )
                avatarError?.let { TideErrorBanner(message = it) }
                avatarNotice?.let { TideNoticeBanner(message = it) }
            }
        }

        item {
            TideCard {
                Text(
                    text = "修改密码",
                    color = scheme.onSurface,
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
}

private fun UserRole.label(): String = when (this) {
    UserRole.Admin -> "管理员"
    UserRole.Editor -> "编辑者"
    UserRole.Viewer -> "只读成员"
}

private fun UserStatus.label(): String = when (this) {
    UserStatus.Active -> "正常"
    UserStatus.Disabled -> "已禁用"
}
