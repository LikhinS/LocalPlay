package com.localplay.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints

/**
 * Marquee text — scrolls only when the text is wider than the container.
 *
 * Previous implementation used onSizeChanged which fires AFTER composition,
 * meaning both widths read as 0 on the first frame and the overflow check
 * never triggered. This version uses SubcomposeLayout which measures the
 * intrinsic text width DURING layout, before the first frame is drawn,
 * so the decision to scroll is correct from frame one.
 *
 * When text fits: static single-line Text with ellipsis.
 * When text overflows: infinite left-right scroll with pause at each end.
 *
 * Animation cost on Exynos 850: one Float per frame while scrolling,
 * zero cost while paused at ends, zero cost when text fits.
 */
@Composable
fun MarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    pauseMs: Int = 1_200,       // pause duration at each end in ms
    scrollSpeedPxPerSec: Float = 60f
) {
    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) { constraints ->

        // Step 1: measure the text at unlimited width to get its natural width
        val textPlaceable = subcompose("measure") {
            Text(
                text     = text,
                style    = style,
                maxLines = 1,
                softWrap = false
            )
        }.first().measure(Constraints())   // unconstrained = natural text width

        val containerWidth = constraints.maxWidth
        val textWidth      = textPlaceable.width
        val overflows      = textWidth > containerWidth

        // Step 2: lay out the actual content
        val content = subcompose("content") {
            if (overflows) {
                ScrollingText(
                    text             = text,
                    style            = style,
                    travelPx         = (textWidth - containerWidth).toFloat(),
                    pauseMs          = pauseMs,
                    scrollSpeedPxPerSec = scrollSpeedPxPerSec
                )
            } else {
                Text(
                    text     = text,
                    style    = style,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val placeable = content.first().measure(
            if (overflows) Constraints()   // let scrolling text be its natural width
            else constraints
        )

        layout(containerWidth, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
}

@Composable
private fun ScrollingText(
    text: String,
    style: TextStyle,
    travelPx: Float,
    pauseMs: Int,
    scrollSpeedPxPerSec: Float
) {
    val scrollDurationMs = ((travelPx / scrollSpeedPxPerSec) * 1_000f)
        .toInt().coerceAtLeast(800)

    // Total cycle: pause → scroll → pause → scroll back (Reverse handles return)
    val cycleDurationMs = pauseMs + scrollDurationMs + pauseMs

    val transition = rememberInfiniteTransition(label = "marquee")
    val offset by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = -travelPx,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = cycleDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "marquee_offset"
    )

    Text(
        text     = text,
        style    = style,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.graphicsLayer { translationX = offset }
    )
}
