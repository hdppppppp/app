package com.tideengine.mobile.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.UserRole
import com.tideengine.mobile.api.dto.UserStatus
import com.tideengine.mobile.ui.AppSession
import com.tideengine.mobile.ui.components.TideCard
import com.tideengine.mobile.ui.components.TideDivider
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideNoticeBanner
import com.tideengine.mobile.ui.components.TidePrimaryButton
import com.tideengine.mobile.ui.components.TideSectionTitle
import com.tideengine.mobile.ui.components.TideStatRow
import com.tideengine.mobile.ui.components.TideTag
import com.tideengine.mobile.ui.components.formatIsoDate
import com.tideengine.mobile.ui.theme.TideColors
import kotlinx.coroutines.launch

@Composable
fun ProfileTab(
    contentPadding: PaddingValues,
    onLoggedOut: () -> Unit,
) {
    val user = AppSession.user
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun changePassword() {
        if (currentPassword.isBlank() || newPassword.length < 8) {
            error = "请填写当前密码，新密码至少 8 位"
            return
        }
        saving = true
        error = null
        notice = null
        scope.launch {
            try {
                ServiceLocator.auth.changePassword(currentPassword, newPassword)
                currentPassword = ""
                newPassword = ""
                notice = "密码已更新，下次登录请使用新密码"
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Throwable) {
                error = "修改密码失败，请稍后重试"
            } finally {
                saving = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { TideSectionTitle(text = "我的") }

        item {
            TideCard {
                Text(
                    text = user?.displayName ?: "未登录",
                    color = TideColors.Text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = user?.username ?: "—",
                    color = TideColors.TextDim,
                    fontSize = 12.sp,
                )
                if (user != null) {
                    TideStatRow(label = "邮箱", value = user.email)
                    TideStatRow(label = "角色", value = user.role.label())
                    TideStatRow(label = "状态", value = user.status.label())
                    TideStatRow(label = "上次登录", value = formatIsoDate(user.lastLoginAt))
                }
            }
        }

        item {
            TideCard {
                Text(text = "修改密码", color = TideColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                TextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("当前密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("新密码（至少 8 位）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { TideErrorBanner(message = it) }
                notice?.let { TideNoticeBanner(message = it) }
                TidePrimaryButton(
                    text = "保存新密码",
                    loading = saving,
                    onClick = { changePassword() },
                )
            }
        }

        item {
            TideCard {
                TideStatRow(label = "服务地址", value = ServiceLocator.apiBaseUrl)
                TideStatRow(label = "多开管理器", value = "原生 WebView 容器")
                TideTag(
                    text = "Bin 仅保存在本机应用私有目录",
                    color = TideColors.Info,
                    background = TideColors.InfoSoft,
                )
                TideDivider()
                TideGhostButton(
                    text = "退出登录",
                    danger = true,
                    onClick = {
                        ServiceLocator.auth.logout()
                        AppSession.clear()
                        onLoggedOut()
                    },
                )
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
