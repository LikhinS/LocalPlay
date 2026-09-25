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

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    pauseAtStartMs: Int = 1_500,
    scrollSpeedPxPerSec: Float = 55f
) {
    SubcomposeLayout(modifier = modifier.fillMaxWidth().clipToBounds()) { constraints ->
        val measured = subcompose("measure") {
            Text(text = text, style = style, maxLines = 1, softWrap = false)
        }.first().measure(Constraints())

        val containerWidth = constraints.maxWidth
        val textWidth = measured.width
        val overflows = textWidth > containerWidth

        val content = subcompose("content") {
            if (overflows) {
                TickerText(text, style, containerWidth.toFloat(), textWidth.toFloat(), pauseAtStartMs, scrollSpeedPxPerSec)
            } else {
                Text(text = text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth())
            }
        }

        val placeable = content.first().measure(if (overflows) Constraints() else constraints)
        layout(containerWidth, placeable.height) { placeable.placeRelative(0, 0) }
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
    val exitDistance = containerWidth + textWidth
    val returnDistance = containerWidth
    val scrollOutMs = ((exitDistance / scrollSpeedPxPerSec) * 1_000f).toInt().coerceAtLeast(400)
    val scrollBackMs = ((returnDistance / scrollSpeedPxPerSec) * 1_000f).toInt().coerceAtLeast(200)
    val cycleMs = pauseAtStartMs + scrollOutMs + 1 + scrollBackMs

    val transition = rememberInfiniteTransition(label = "marquee")
    val offset by transition.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = cycleMs
                0f at 0 with LinearEasing
                0f at pauseAtStartMs with LinearEasing
                -exitDistance at (pauseAtStartMs + scrollOutMs) with LinearEasing
                containerWidth at (pauseAtStartMs + scrollOutMs + 1) with LinearEasing
                0f at cycleMs with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "ticker"
    )

    Text(text = text, style = style, maxLines = 1, softWrap = false,
        modifier = Modifier.graphicsLayer { translationX = offset })
}
