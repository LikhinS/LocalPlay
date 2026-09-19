package com.localplay.app.feature.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.core.player.model.RepeatMode
import com.localplay.app.ui.components.AlbumArt
import com.localplay.app.ui.components.FormatBadgeRow
import com.localplay.app.ui.components.formatDuration

/**
 * Full-screen Now Playing screen — Phase 4 static layout.
 * Phase 7 wraps this in the animated gradient background.
 * Phase 8 adds the lyrics button functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val state by playerViewModel.playerState.collectAsState()
    val track = state.currentTrack

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // Chevron / dismiss handle
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.Start)) {
            Icon(
                painter = painterResource(android.R.drawable.arrow_down_float),
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))

        // Large album art
        AlbumArt(
            trackId    = track?.id,
            sizeDp     = 300.dp,
            cornerDp   = 12.dp,
            showShadow = true,
            modifier   = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )

        Spacer(Modifier.height(28.dp))

        // Title + artist
        Text(
            text      = track?.title ?: "Not playing",
            style     = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            modifier  = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = track?.artist ?: "",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines  = 1,
            modifier  = Modifier.fillMaxWidth()
        )

        // Format badges
        if (track != null) {
            Spacer(Modifier.height(8.dp))
            FormatBadgeRow(
                isLossless = track.isLossless,
                isHiRes    = track.sampleRateHz >= 48_000 && track.bitDepth >= 24,
                isAtmos    = track.isAtmos,
                isMixing   = state.isCrossfading
            )
        }

        Spacer(Modifier.height(24.dp))

        // Scrubber
        if (state.durationMs > 0) {
            Slider(
                value         = state.progress,
                onValueChange = { fraction ->
                    playerViewModel.seekTo((fraction * state.durationMs).toLong())
                },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = formatDuration(state.positionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = formatDuration(state.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(onClick = {
                playerViewModel.setShuffleEnabled(!state.shuffleEnabled)
            }) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_sort_by_size),
                    contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Previous
            IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_media_previous),
                    contentDescription = "Previous",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Play / Pause (large)
            IconButton(
                onClick  = { playerViewModel.playOrPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (state.isPlaying) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    ),
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Next
            IconButton(onClick = { playerViewModel.skipToNext() }) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_media_next),
                    contentDescription = "Next",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Repeat
            IconButton(onClick = {
                val next = when (state.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                }
                playerViewModel.setRepeatMode(next)
            }) {
                Icon(
                    painter = painterResource(
                        when (state.repeatMode) {
                            RepeatMode.ONE -> android.R.drawable.ic_menu_rotate
                            else           -> android.R.drawable.ic_menu_revert
                        }
                    ),
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != RepeatMode.OFF)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
