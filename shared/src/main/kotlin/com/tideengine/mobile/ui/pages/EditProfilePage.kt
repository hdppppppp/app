package com.tideengine.mobile.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.ui.components.AvatarImage
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideNoticeBanner
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.components.TideStatRow
import com.tideengine.mobile.ui.components.createAvatarDataUrl
import com.tideengine.mobile.ui.components.errorText
import io.github.suqi8.coui.kmp.basic.Surface
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextField
import io.github.suqi8.coui.kmp.basic.TextFieldMode
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 编辑资料页：头像（选择 / 恢复默认）与昵称修改，从「我的 → 编辑资料」进入。
 *
 * 头像为圆角方形（与「我的」页一致）；保存走 PATCH /api/auth/profile，仅更新
 * 显示名称。页面为「我的」Tab 内的子页，返回即回到「我的」。
 */
@Composable
fun EditProfilePage(
    user: PublicUser,
    onUserUpdated: (PublicUser) -> Unit,
    onSessionExpired: () -> Unit,
    onBack: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var displayName by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(user.displayName))
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    var avatarBusy by remember { mutableStateOf(false) }
    var confirmResetAvatar by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    var avatarNotice by remember { mutableStateOf<String?>(null) }

    fun handleFailure(throwable: Throwable, target: (String) -> Unit) {
        if (throwable is CancellationException) throw throwable
        if (throwable is ApiException && throwable.isUnauthorized) {
            onSessionExpired()
        } else {
            target(errorText(throwable))
        }
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
            handleFailure(throwable) { avatarError = it }
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
                handleFailure(throwable) { avatarError = it }
            } finally {
                avatarBusy = false
            }
        }
    }

    fun saveProfile() {
        val name = displayName.text.trim()
        if (name.isEmpty() || name == user.displayName || saving) return
        saving = true
        error = null
        notice = null
        scope.launch {
            try {
                val updated = ServiceLocator.auth.updateProfile(name)
                notice = "资料已更新"
                onUserUpdated(updated)
            } catch (throwable: Throwable) {
                handleFailure(throwable) { error = it }
            } finally {
                saving = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.backgroundElevated)
            .systemBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TideGhostButton(text = "返回", onClick = onBack)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "编辑资料",
                    color = scheme.labelPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(56.dp))
            }
        }

        item {
            // 头像卡：圆角方形头像（点击换图）+ 恢复默认入口，手工卡片以居中布局
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.cardBackground)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(enabled = !avatarBusy) {
                                pickAvatarImage.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                    ) {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = scheme.primary,
                            contentColor = scheme.onPrimary,
                        ) {
                            AvatarImage(
                                url = user.avatarUrl,
                                fallback = {
                                    Text(
                                        text = user.displayName.take(1).uppercase(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "点击头像更换 · 支持 JPG/PNG/WebP",
                        color = scheme.labelSecondaryVariant,
                        fontSize = 12.sp,
                    )
                    if (!user.avatarUrl.isNullOrEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        TideGhostButton(
                            text = if (avatarBusy) "处理中…" else "恢复默认头像",
                            enabled = !avatarBusy,
                            onClick = { confirmResetAvatar = !confirmResetAvatar },
                        )
                    }
                    if (confirmResetAvatar) {
                        Spacer(Modifier.height(4.dp))
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
                }
            }
        }

        item {
            TideCard {
                TextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "昵称",
                    backgroundMode = TextFieldMode.Rectangle,
                    enabled = !saving,
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                TidePrimaryButton(
                    text = "保存昵称",
                    loading = saving,
                    enabled = !saving && displayName.text.trim().isNotBlank() &&
                        displayName.text.trim() != user.displayName,
                    onClick = { saveProfile() },
                )
            }
        }

        item {
            TideCard {
                TideStatRow(label = "用户名", value = "@${user.username}")
                TideStatRow(label = "邮箱", value = user.email)
            }
        }

        avatarError?.let {
            item { TideErrorBanner(message = it) }
        }
        avatarNotice?.let {
            item { TideNoticeBanner(message = it) }
        }
        error?.let {
            item { TideErrorBanner(message = it) }
        }
        notice?.let {
            item { TideNoticeBanner(message = it) }
        }
    }
}
