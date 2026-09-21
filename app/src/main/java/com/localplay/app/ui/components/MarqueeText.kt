package com.localplay.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow

/**
 * Marquee-scrolling text that only animates when the text overflows.
 *
 * When text fits: renders exactly like a normal single-line Text.
 * When text overflows: scrolls left continuously with a pause at
 * each end (like Apple Music's Now Playing title scroll).
 *
 * Implementation is a pure offset animation — no Canvas, no custom
 * draw, no shader. Just a graphicsLayer translationX on a Text
 * composable. Cost on Exynos 850: one Float per frame while scrolling,
 * zero when text fits.
 *
 * scrollPauseDurationMs: how long text rests at each end before reversing.
 * scrollSpeedDpPerSec:   approximate pixels per second (scaled to density).
 */
@Composable
fun MarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    scrollPauseDurationMs: Int = 1_200,
    scrollSpeedDpPerSec: Float = 40f
) {
    var containerWidth by remember { mutableStateOf(0) }
    var textWidth      by remember { mutableStateOf(0) }

    val overflow = textWidth > containerWidth && containerWidth > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width }
    ) {
        if (overflow) {
            val travelPx = (textWidth - containerWidth).toFloat()
            // Duration to scroll travelPx at scrollSpeedDpPerSec
            // We use px directly since we measure in px
            val scrollDuration = ((travelPx / scrollSpeedDpPerSec) * 1_000f).toInt()
                .coerceAtLeast(1_000)
            val totalDuration  = scrollPauseDurationMs + scrollDuration + scrollPauseDurationMs

            val transition = rememberInfiniteTransition(label = "marquee")
            val offset by transition.animateFloat(
                initialValue  = 0f,
                targetValue   = -travelPx,
                animationSpec = infiniteRepeatable(
                    animation  = tween(
                        durationMillis = totalDuration,
                        easing         = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "marquee_offset"
            )

            Text(
                text       = text,
                style      = style,
                maxLines   = 1,
                softWrap   = false,
                overflow   = TextOverflow.Visible,
                modifier   = Modifier
                    .wrapContentSize()
                    .onSizeChanged { textWidth = it.width }
                    .graphicsLayer { translationX = offset }
            )
        } else {
            Text(
                text     = text,
                style    = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { textWidth = it.width }
            )
        }
    }
}
