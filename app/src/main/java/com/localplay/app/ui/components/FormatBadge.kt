package com.localplay.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * Format badge row — Lossless, Hi-Res Lossless, Dolby Atmos, Mixing.
 *
 * The "Mixing" pill animates with a gentle alpha pulse (0.5 → 1.0 → 0.5)
 * matching Apple Music's subtle shimmer on the label. This is a cheap
 * alpha animation only — no shader, no blur — so it costs nothing on
 * Mali-G52.
 */
@Composable
fun FormatBadgeRow(
    isLossless: Boolean,
    isHiRes: Boolean,
    isAtmos: Boolean,
    isMixing: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLossless && !isAtmos && !isMixing) return

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isLossless) {
            BadgePill(if (isHiRes) "Hi-Res Lossless" else "Lossless")
        }
        if (isAtmos) {
            if (isLossless) Spacer(Modifier.width(6.dp))
            BadgePill("Dolby Atmos")
        }
        if (isMixing) {
            if (isLossless || isAtmos) Spacer(Modifier.width(6.dp))
            MixingBadgePill()
        }
    }
}

@Composable
fun BadgePill(label: String, modifier: Modifier = Modifier) {
    Text(
        text     = label,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

/**
 * "Mixing" pill with a gentle alpha pulse.
 * Uses rememberInfiniteTransition — a single float animating between
 * 0.5 and 1.0 over 900 ms. This is the cheapest possible animation on
 * Exynos 850: one Float updated per frame, one alpha applied to a Text.
 */
@Composable
private fun MixingBadgePill() {
    val transition = rememberInfiniteTransition(label = "mixing_pulse")
    val alpha by transition.animateFloat(
        initialValue  = 0.5f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mixing_alpha"
    )

    Text(
        text     = "Mixing",
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .alpha(alpha)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = alpha), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}
