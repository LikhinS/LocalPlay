package com.localplay.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Format badge — shows ONE badge at a time in priority order:
 *
 *   Mixing  >  Dolby Atmos  >  Hi-Res Lossless  >  Lossless  >  (nothing)
 *
 * When "Mixing" is active it replaces whatever format badge was showing,
 * using a 300ms crossfade so it never pops in. When Mixing ends, the
 * original format badge fades back in the same way.
 *
 * Apple Music shows exactly one badge at a time in Now Playing —
 * we match that behaviour here.
 */
@Composable
fun FormatBadgeRow(
    isLossless: Boolean,
    isHiRes: Boolean,
    isAtmos: Boolean,
    isMixing: Boolean,
    modifier: Modifier = Modifier
) {
    // Determine which single label to show — Mixing wins when active
    val label: String? = when {
        isMixing   -> "Mixing"
        isAtmos    -> "Dolby Atmos"
        isHiRes    -> "Hi-Res Lossless"
        isLossless -> "Lossless"
        else       -> null
    }

    // AnimatedContent crossfades between different label strings.
    // null → nothing shown; any string → the badge pill.
    // 300 ms fade matches Apple Music's badge transition speed.
    AnimatedContent(
        targetState   = label,
        transitionSpec = {
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
        },
        modifier = modifier,
        label    = "badge_crossfade"
    ) { targetLabel ->
        if (targetLabel != null) {
            if (targetLabel == "Mixing") {
                MixingBadgePill()
            } else {
                BadgePill(targetLabel)
            }
        }
        // null state renders nothing — AnimatedContent fades it out cleanly
    }
}

@Composable
fun BadgePill(label: String, modifier: Modifier = Modifier) {
    Text(
        text     = label,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

/**
 * "Mixing" pill with a gentle alpha pulse (0.55 → 1.0 → 0.55, 900 ms).
 * Single Float animation — cheapest possible on Mali-G52.
 */
@Composable
private fun MixingBadgePill() {
    val transition = rememberInfiniteTransition(label = "mixing_pulse")
    val alpha by transition.animateFloat(
        initialValue  = 0.55f,
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
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}
