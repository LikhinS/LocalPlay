package com.localplay.app.feature.nowplaying

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.core.player.model.RepeatMode
import com.localplay.app.ui.components.AlbumArt
import com.localplay.app.ui.components.FormatBadgeRow
import com.localplay.app.ui.components.formatDuration

@Composable
fun NowPlayingScreen(playerViewModel: PlayerViewModel = viewModel(), onDismiss: () -> Unit) {
    val state by playerViewModel.playerState.collectAsState()
    val track = state.currentTrack

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))

        // Dismiss chevron
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Dismiss",
                modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Album art
        AlbumArt(trackId = track?.id, sizeDp = 300.dp, cornerDp = 12.dp, showShadow = true,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f))

        Spacer(Modifier.height(28.dp))

        // Title
        Text(track?.title ?: "Not playing", style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        Text(track?.artist ?: "", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
            maxLines = 1, modifier = Modifier.fillMaxWidth())

        // Format badges
        if (track != null) {
            Spacer(Modifier.height(8.dp))
            FormatBadgeRow(isLossless = track.isLossless,
                isHiRes = track.sampleRateHz >= 48_000 && track.bitDepth >= 24,
                isAtmos = track.isAtmos, isMixing = state.isCrossfading)
        }

        Spacer(Modifier.height(24.dp))

        // Scrubber
        if (state.durationMs > 0) {
            Slider(value = state.progress,
                onValueChange = { playerViewModel.seekTo((it * state.durationMs).toLong()) },
                modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(state.positionMs), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Transport controls
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {

            // Shuffle
            IconButton(onClick = { playerViewModel.setShuffleEnabled(!state.shuffleEnabled) }) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle",
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Previous
            IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous",
                    modifier = Modifier.size(36.dp))
            }
            // Play/Pause — larger
            IconButton(onClick = { playerViewModel.playOrPause() },
                modifier = Modifier.size(64.dp)) {
                Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(52.dp))
            }
            // Next
            IconButton(onClick = { playerViewModel.skipToNext() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next",
                    modifier = Modifier.size(36.dp))
            }
            // Repeat
            IconButton(onClick = {
                playerViewModel.setRepeatMode(when (state.repeatMode) {
                    RepeatMode.OFF -> RepeatMode.ALL
                    RepeatMode.ALL -> RepeatMode.ONE
                    RepeatMode.ONE -> RepeatMode.OFF
                })
            }) {
                Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
