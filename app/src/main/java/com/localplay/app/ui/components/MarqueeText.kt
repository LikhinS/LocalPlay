package com.localplay.app.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
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
 * Marquee text that scrolls right-to-left, one direction only:
 *
 *   1. Text sits at x=0 for [pauseAtStartMs]
 *   2. Scrolls left until fully out of view (linear, [scrollSpeedPxPerSec])
 *   3. Snaps invisibly back to x=0
 *   4. Pauses for [pauseAtStartMs] again, then repeats
 *
 * When text fits in the container: static, single-line, ellipsis.
 * Applied to both title and artist in Now Playing and MiniPlayer.
 *
 * SubcomposeLayout measures text width before the first frame so the
 * overflow decision is correct immediately — no onSizeChanged lag.
 *
 * Animation cost on Exynos 850: one Float per frame while scrolling,
 * zero cost while paused, zero cost when text fits.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    pauseAtStartMs: Int = 1_500,        // pause at origin before and after scroll
    scrollSpeedPxPerSec: Float = 55f    // px/sec — comfortable read speed
) {
    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) { constraints ->

        // Measure intrinsic text width (unconstrained)
        val textPlaceable = subcompose("measure") {
            Text(text = text, style = style, maxLines = 1, softWrap = false)
        }.first().measure(Constraints())

        val containerWidth = constraints.maxWidth
        val textWidth      = textPlaceable.width
        val overflows      = textWidth > containerWidth

        val content = subcompose("content") {
            if (overflows) {
                ScrollingText(
                    text               = text,
                    style              = style,
                    travelPx           = textWidth.toFloat(),   // scroll until FULLY offscreen
                    pauseAtStartMs     = pauseAtStartMs,
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
            if (overflows) Constraints() else constraints
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
    pauseAtStartMs: Int,
    scrollSpeedPxPerSec: Float
) {
    // How long the scroll itself takes at the given speed
    val scrollMs = ((travelPx / scrollSpeedPxPerSec) * 1_000f)
        .toInt().coerceAtLeast(800)

    // Snap back is instant (1 ms) then pause again
    val snapMs   = 1
    val cycleMs  = pauseAtStartMs + scrollMs + snapMs + pauseAtStartMs

    // keyframes: sit at 0 → scroll to -travelPx → snap back to 0 → sit at 0
    val transition = rememberInfiniteTransition(label = "marquee")
    val offset by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 0f,   // keyframes override start/end; target = same so it loops
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = cycleMs

                // sit at start
                0f at 0                              with LinearEasing
                0f at pauseAtStartMs                 with LinearEasing

                // scroll left to fully off-screen
                -travelPx at (pauseAtStartMs + scrollMs) with LinearEasing

                // snap back instantly
                0f at (pauseAtStartMs + scrollMs + snapMs) with LinearEasing

                // sit at start again until cycle end (implicit: 0f at cycleMs)
            },
            repeatMode = RepeatMode.Restart   // always left-to-right, never bounces
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
