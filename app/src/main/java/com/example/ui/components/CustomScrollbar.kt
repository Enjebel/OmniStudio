package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OmniTheme

/**
 * Subtle spring press-scale interaction modifier with Material 3 ripple feedback.
 */
fun Modifier.pressScaleClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Subtle rounded vertical scrollbar indicator for ScrollState containers.
 */
fun Modifier.verticalScrollbar(
    state: ScrollState,
    width: Dp = 3.5.dp,
    alwaysVisible: Boolean = true
): Modifier = composed {
    val palette = OmniTheme.colors
    val targetAlpha = if (alwaysVisible || state.isScrollInProgress) {
        if (state.maxValue > 0) 0.72f else 0f
    } else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        label = "scrollbar_alpha"
    )

    drawWithContent {
        drawContent()
        val maxScroll = state.maxValue.toFloat()
        if (maxScroll > 0f && alpha > 0.01f) {
            val viewHeight = size.height
            val totalContentHeight = viewHeight + maxScroll
            val thumbHeight = ((viewHeight / totalContentHeight) * viewHeight).coerceIn(36f, viewHeight * 0.7f)
            val scrollableTrack = (viewHeight - thumbHeight).coerceAtLeast(1f)
            val thumbOffsetY = (state.value / maxScroll) * scrollableTrack
            val barWidthPx = width.toPx()
            val xOffset = size.width - barWidthPx - 2.dp.toPx()

            drawRoundRect(
                color = palette.border.copy(alpha = 0.4f * alpha),
                topLeft = Offset(xOffset, 4f),
                size = Size(barWidthPx, (viewHeight - 8f).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            drawRoundRect(
                color = palette.textMuted.copy(alpha = alpha),
                topLeft = Offset(xOffset, thumbOffsetY),
                size = Size(barWidthPx, thumbHeight),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )
        }
    }
}

/**
 * Subtle rounded vertical scrollbar indicator for LazyColumn / LazyListState containers.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 3.5.dp,
    alwaysVisible: Boolean = true
): Modifier = composed {
    val palette = OmniTheme.colors
    val layoutInfo = state.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo

    val hasOverflow = totalItems > visibleItems.size ||
        state.canScrollForward ||
        state.canScrollBackward

    val alpha by animateFloatAsState(
        targetValue = if (hasOverflow && (alwaysVisible || state.isScrollInProgress)) 0.72f else 0f,
        label = "lazy_scrollbar_alpha"
    )

    drawWithContent {
        drawContent()
        if (hasOverflow && visibleItems.isNotEmpty() && totalItems > 0 && alpha > 0.01f) {
            val viewHeight = size.height
            val avgItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size.coerceAtLeast(1)
            val estimatedTotalHeight = (avgItemSize * totalItems).coerceAtLeast(viewHeight + 1f)
            val thumbHeight = ((viewHeight / estimatedTotalHeight) * viewHeight).coerceIn(40f, viewHeight * 0.65f)
            val firstIdx = state.firstVisibleItemIndex
            val firstOffset = state.firstVisibleItemScrollOffset
            val scrollProgress = ((firstIdx * avgItemSize + firstOffset) / (estimatedTotalHeight - viewHeight).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val thumbY = scrollProgress * (viewHeight - thumbHeight).coerceAtLeast(0f)

            val barWidthPx = width.toPx()
            val xOffset = size.width - barWidthPx - 2.dp.toPx()

            drawRoundRect(
                color = palette.border.copy(alpha = 0.4f * alpha),
                topLeft = Offset(xOffset, 4f),
                size = Size(barWidthPx, (viewHeight - 8f).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )

            drawRoundRect(
                color = palette.textMuted.copy(alpha = alpha),
                topLeft = Offset(xOffset, thumbY),
                size = Size(barWidthPx, thumbHeight),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )
        }
    }
}
