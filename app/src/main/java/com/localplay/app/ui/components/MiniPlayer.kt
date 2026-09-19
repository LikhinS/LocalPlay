package com.localplay.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localplay.app.core.player.model.PlayerState

/**
 * Persistent mini player bar that sits above the bottom navigation bar.
 *
 * Gestures:
 *  - Tap anywhere → [onExpand] (opens Now Playing full-screen)
 *  - Swipe right → skip to next
 *  - Swipe left  → skip to previous
 *  - Play/pause button taps [onPlayPause]
 *
 * Rendered as a plain surface + thin progress bar — no blur or glass.
 * The gradient animation arrives in Phase 7 and wraps around this widget,
 * not inside it, so this composable stays cheap.
 */
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
        visible = state.currentTrack != null,
        enter   = slideInVertically { it },
        exit    = slideOutVertically { it },
        modifier = modifier
    ) {
        val track = state.currentTrack ?: return@AnimatedVisibility
        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onExpand)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                dragAccumulator > 80f  -> onSkipPrevious()
                                dragAccumulator < -80f -> onSkipNext()
                            }
                            dragAccumulator = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            dragAccumulator += dragAmount
                        }
                    )
                }
        ) {
            // Thin progress bar across the very top of the mini player
            LinearProgressIndicator(
                progress    = { state.progress },
                modifier    = Modifier.fillMaxWidth().height(2.dp),
                color       = MaterialTheme.colorScheme.primary,
                trackColor  = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art
                AlbumArt(
                    trackId  = track.id,
                    sizeDp   = 40.dp,
                    cornerDp = 4.dp
                )

                Spacer(Modifier.width(10.dp))

                // Track info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = track.title,
                        style    = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text     = track.artist,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause
                IconButton(onClick = onPlayPause) {
                    Icon(
                        painter = painterResource(
                            if (state.isPlaying) android.R.drawable.ic_media_pause
                            else android.R.drawable.ic_media_play
                        ),
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Skip next
                IconButton(onClick = onSkipNext) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_media_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
