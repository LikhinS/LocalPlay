package com.localplay.app.ui.components

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
 * Marquee text — continuous left-to-right ticker belt:
 *
 *   1. Text sits at x=0 (left edge) for [pauseAtStartMs]
 *   2. Scrolls LEFT continuously until the text has fully exited
 *      the right side of the container (travelled containerWidth + textWidth px)
 *   3. Instantly reappears entering from the right edge (snap)
 *   4. Continues scrolling left until text reaches x=0 again
 *   5. Pauses, then repeats
 *
 * This gives the "ticker/conveyor belt" feel where text continuously
 * flows left and wraps around from the right — same as Apple Music,
 * Spotify, and most modern music players.
 *
 * SubcomposeLayout measures text width before first frame — no lag.
 * Static single-line ellipsis when text fits.
 * Cost on Exynos 850: one Float update per frame while scrolling, zero otherwise.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    pauseAtStartMs: Int = 1_500,
    scrollSpeedPxPerSec: Float = 55f
) {
    SubcomposeLayout(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
    ) { constraints ->

        // Measure intrinsic text width unconstrained
        val measured = subcompose("measure") {
            Text(text = text, style = style, maxLines = 1, softWrap = false)
        }.first().measure(Constraints())

        val containerWidth = constraints.maxWidth
        val textWidth      = measured.width
        val overflows      = textWidth > containerWidth

        val content = subcompose("content") {
            if (overflows) {
                TickerText(
                    text               = text,
                    style              = style,
                    containerWidth     = containerWidth.toFloat(),
                    textWidth          = textWidth.toFloat(),
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
private fun TickerText(
    text: String,
    style: TextStyle,
    containerWidth: Float,
    textWidth: Float,
    pauseAtStartMs: Int,
    scrollSpeedPxPerSec: Float
) {
    // Full travel distance:
    // Text starts at x=0, scrolls left until it fully exits the container.
    // That means it travels (containerWidth + textWidth) px total before
    // it wraps. But we only need it to travel (containerWidth + textWidth)
    // to go fully off the left edge, then we snap it back entering from
    // the right at x = containerWidth, and scroll to x = 0.
    //
    // Simpler equivalent: treat the full cycle as one continuous leftward
    // scroll of (containerWidth + textWidth + gap) px, where gap is the
    // visual spacing between the end of one pass and the start of the next.
    // We use containerWidth as the gap so the text is fully hidden before
    // it reappears.
    //
    // keyframes:
    //   0ms            : x = 0          (start, visible at left)
    //   pauseAtStartMs : x = 0          (still paused)
    //   pauseAtStartMs + scrollMs : x = -(containerWidth + textWidth)
    //                                    (fully off left edge)
    //   +1ms           : x = containerWidth  (snap: entering from right edge)
    //   +resumeMs      : x = 0          (scrolled back to start position)
    //   cycle end      : pause handled by gap between resumeMs and cyclMs

    val exitDistance   = containerWidth + textWidth   // px to exit left edge
    val returnDistance = containerWidth               // px from right edge back to 0

    val scrollOutMs  = ((exitDistance   / scrollSpeedPxPerSec) * 1_000f).toInt().coerceAtLeast(400)
    val scrollBackMs = ((returnDistance / scrollSpeedPxPerSec) * 1_000f).toInt().coerceAtLeast(200)
    val snapMs       = 1

    val cycleMs = pauseAtStartMs + scrollOutMs + snapMs + scrollBackMs

    val transition = rememberInfiniteTransition(label = "marquee")
    val offset by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = cycleMs

                // pause at origin
                0f at 0                with LinearEasing
                0f at pauseAtStartMs   with LinearEasing

                // scroll left until fully off-screen (left edge)
                -exitDistance at (pauseAtStartMs + scrollOutMs) with LinearEasing

                // instant snap: reappear entering from right edge
                containerWidth at (pauseAtStartMs + scrollOutMs + snapMs) with LinearEasing

                // scroll left back to origin at the same speed
                0f at cycleMs with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ticker_offset"
    )

    Text(
        text     = text,
        style    = style,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.graphicsLayer { translationX = offset }
    )
}
