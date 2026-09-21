package com.localplay.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.localplay.app.core.player.model.PlayerState

@Composable
fun MiniPlayer(
    state: PlayerState,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible  = state.currentTrack != null,
        enter    = slideInVertically { it },
        exit     = slideOutVertically { it },
        modifier = modifier
    ) {
        val track = state.currentTrack ?: return@AnimatedVisibility
        var drag by remember { mutableFloatStateOf(0f) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onExpand)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                drag > 80f  -> onSkipPrevious()
                                drag < -80f -> onSkipNext()
                            }
                            drag = 0f
                        },
                        onHorizontalDrag = { _, d -> drag += d }
                    )
                }
        ) {
            LinearProgressIndicator(
                progress   = { state.progress },
                modifier   = Modifier.fillMaxWidth().height(2.dp),
                color      = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(albumId = track.albumId, sizeDp = 40.dp, cornerDp = 4.dp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    MarqueeText(
                        text  = track.title,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    MarqueeText(
                        text  = track.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onSkipNext) {
                    Icon(Icons.Filled.SkipNext, "Next", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
