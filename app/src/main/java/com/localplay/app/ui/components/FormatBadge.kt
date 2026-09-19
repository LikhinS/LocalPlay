package com.localplay.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Inline badge row shown under the track title in Now Playing.
 * Mirrors Apple Music's "Lossless", "Hi-Res Lossless", and "Dolby Atmos"
 * pill labels — flat outlined pills, no background fill, cheap to render.
 */
@Composable
fun FormatBadgeRow(
    isLossless: Boolean,
    isHiRes: Boolean,     // sampleRate >= 48000 && bitDepth >= 24
    isAtmos: Boolean,
    isMixing: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLossless && !isAtmos && !isMixing) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isLossless) {
            BadgePill(label = if (isHiRes) "Hi-Res Lossless" else "Lossless")
        }
        if (isAtmos) {
            if (isLossless) Spacer(Modifier.width(6.dp))
            BadgePill(label = "Dolby Atmos")
        }
        if (isMixing) {
            if (isLossless || isAtmos) Spacer(Modifier.width(6.dp))
            BadgePill(label = "Mixing", animated = true)
        }
    }
}

@Composable
fun BadgePill(
    label: String,
    animated: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}
