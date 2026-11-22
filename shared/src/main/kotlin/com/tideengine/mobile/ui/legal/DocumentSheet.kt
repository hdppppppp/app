package com.tideengine.mobile.ui.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tideengine.mobile.ui.components.TidePrimaryButton
import io.github.suqi8.coui.kmp.basic.Surface
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * 协议文档浮层：App 内置展示《用户协议》/《隐私政策》，不跳网页。
 *
 * 自绘实现（COUI 无现成 Dialog 组件）：半透明遮罩 + 居中圆角卡片，内部
 * 可滚动；点遮罩或「关闭」收起。卡片拦截点击（clickable 空实现），避免
 * 触到遮罩误关闭。
 */
@Composable
fun DocumentSheet(doc: AgreementDoc, onClose: () -> Unit) {
    val scheme = COUITheme.colorScheme
    val cardInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose,
            )
            .systemBarsPadding()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .fillMaxHeight(0.9f)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                ),
            shape = RoundedCornerShape(16.dp),
            color = scheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = doc.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "版本 ${doc.version} · 生效日期 ${doc.effectiveDate}",
                    fontSize = 11.sp,
                    color = scheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = doc.intro,
                    fontSize = 13.sp,
                    color = scheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
                doc.sections.forEach { section ->
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = section.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        section.blocks.forEach { block ->
                            when (block) {
                                is AgreementBlock.Paragraph -> Text(
                                    text = block.text,
                                    fontSize = 13.sp,
                                    color = scheme.onSurfaceVariantSummary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                is AgreementBlock.Bullets -> Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    block.items.forEach { item ->
                                        Text(
                                            text = "· $item",
                                            fontSize = 13.sp,
                                            color = scheme.onSurfaceVariantSummary,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                TidePrimaryButton(text = "关闭", onClick = onClose)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
