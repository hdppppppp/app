package com.tideengine.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Surface
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import com.tideengine.mobile.api.ApiException
import io.github.suqi8.coui.kmp.theme.COUITheme

@Composable
fun TideCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
fun TidePrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(
            text = if (loading) "处理中…" else text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun TideGhostButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = COUITheme.colorScheme
    TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            color = if (danger) scheme.errorContainer else scheme.secondaryContainer,
            textColor = if (danger) scheme.onErrorContainer else scheme.onSecondaryContainer,
            disabledColor = scheme.surfaceContainerHigh,
            disabledTextColor = scheme.disabledOnSurface,
        ),
        textStyle = COUITheme.textStyles.button.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        ),
    )
}

@Composable
fun TideTag(
    text: String,
    color: Color = COUITheme.colorScheme.onPrimaryContainer,
    background: Color = COUITheme.colorScheme.primaryContainer,
) {
    Surface(
        color = background,
        contentColor = color,
        shape = RoundedCornerShape(6.dp),
        shadowElevation = 0.dp,
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun TideDivider() {
    HorizontalDivider(color = COUITheme.colorScheme.dividerLine)
}

@Composable
fun TideSectionTitle(text: String, trailing: @Composable (RowScope.() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = COUITheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke(this)
    }
}

@Composable
fun TideEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun TideErrorBanner(message: String, onRetry: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(COUITheme.colorScheme.errorContainer, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = COUITheme.colorScheme.onErrorContainer,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        if (onRetry != null) {
            Spacer(Modifier.width(8.dp))
            TideGhostButton(text = "重试", onClick = onRetry)
        }
    }
}

@Composable
fun TideNoticeBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(COUITheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = message,
            color = COUITheme.colorScheme.onPrimaryContainer,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun TideLoadingRow(text: String = "加载中…") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun TideStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
            fontSize = 12.sp,
            modifier = Modifier.width(84.dp),
        )
        Text(
            text = value,
            color = COUITheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun TideFieldLabel(text: String) {
    Text(
        text = text,
        color = COUITheme.colorScheme.onSurfaceVariantSummary,
        fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun TideVerticalGap(height: Int = 8) {
    Spacer(Modifier.height(height.dp))
}

fun errorText(throwable: Throwable): String = when (throwable) {
    is ApiException -> if (throwable.isNetworkError) {
        "无法连接服务器，请检查网络或 API 地址"
    } else {
        throwable.message
    }

    else -> throwable.message?.takeIf { it.isNotBlank() } ?: "操作失败，请稍后重试"
}

/** 服务端返回 ISO 8601；截取到分钟，避免引入时区依赖。 */
fun formatIsoDate(value: String?): String {
    if (value.isNullOrBlank()) return "—"
    val normalized = value.replace('T', ' ')
    return if (normalized.length >= 16) normalized.substring(0, 16) else normalized
}
