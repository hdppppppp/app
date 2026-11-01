package com.tideengine.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ServiceLocator
import com.tideengine.mobile.api.ApiException
import com.tideengine.mobile.api.dto.PublicUser
import com.tideengine.mobile.ui.components.TideErrorBanner
import com.tideengine.mobile.ui.components.TideGhostButton
import com.tideengine.mobile.ui.components.TideLoadingRow
import com.tideengine.mobile.ui.components.errorText
import com.tideengine.mobile.ui.pages.HomePage
import com.tideengine.mobile.ui.pages.LoginPage
import io.github.suqi8.coui.kmp.basic.Surface
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ColorSchemeMode
import io.github.suqi8.coui.kmp.theme.ThemeController
import kotlinx.coroutines.CancellationException

@Composable
fun TideApp(onOpenManager: (String) -> Unit) {
    COUITheme(
        controller = remember {
            ThemeController(
                colorSchemeMode = ColorSchemeMode.MonetSystem,
                keyColor = Color(0xFFC95735),
            )
        },
    ) {
        TideAppContent(onOpenManager = onOpenManager)
    }
}

@Composable
private fun TideAppContent(onOpenManager: (String) -> Unit) {
    var user by remember { mutableStateOf<PublicUser?>(null) }
    var checking by remember { mutableStateOf(true) }
    var bootError by remember { mutableStateOf<String?>(null) }
    var bootKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(bootKey) {
        checking = true
        bootError = null
        try {
            if (!ServiceLocator.isInitialized) {
                bootError = "客户端尚未初始化，请重启应用"
                return@LaunchedEffect
            }
            if (!ServiceLocator.auth.hasToken()) {
                user = null
                return@LaunchedEffect
            }
            user = ServiceLocator.auth.me()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            if (throwable is ApiException && throwable.isUnauthorized) {
                ServiceLocator.auth.logout()
                user = null
            } else {
                bootError = errorText(throwable)
            }
        } finally {
            checking = false
        }
    }

    when {
        checking -> SplashScreen()
        bootError != null -> BootErrorScreen(
            message = bootError!!,
            onRetry = { bootKey += 1 },
            onSignOut = {
                if (ServiceLocator.isInitialized) {
                    ServiceLocator.auth.logout()
                    user = null
                    bootError = null
                    checking = false
                } else {
                    bootError = "客户端尚未初始化，请重启应用"
                }
            },
        )

        user == null -> LoginPage(onAuthenticated = { user = it })
        else -> HomePage(
            user = user!!,
            onSessionExpired = {
                ServiceLocator.auth.logout()
                user = null
                checking = false
            },
            onOpenManager = onOpenManager,
            onUserUpdated = { user = it },
        )
    }
}

@Composable
private fun SplashScreen() {
    val scheme = COUITheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "潮", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "潮汐引擎",
                color = scheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            TideLoadingRow(text = "正在恢复登录状态…")
        }
    }
}

@Composable
private fun BootErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TideErrorBanner(message = message, onRetry = onRetry)
            TideGhostButton(text = "清除登录状态", onClick = onSignOut)
        }
    }
}
