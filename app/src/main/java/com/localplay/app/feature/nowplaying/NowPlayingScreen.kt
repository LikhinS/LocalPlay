package com.localplay.app.feature.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.core.player.model.RepeatMode
import com.localplay.app.ui.components.AlbumArt
import com.localplay.app.ui.components.FormatBadgeRow
import com.localplay.app.ui.components.MarqueeText
import com.localplay.app.ui.components.formatDuration

@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val state by playerViewModel.playerState.collectAsState()
    val track = state.currentTrack

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 120f
    val dragFraction = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)

    val artScale    by animateFloatAsState(1f - dragFraction * 0.08f, tween(0), label = "scale")
    val screenAlpha by animateFloatAsState(1f - dragFraction * 0.4f,  tween(0), label = "alpha")

    var showQueue by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd    = { if (dragOffsetY > dismissThreshold) onDismiss(); dragOffsetY = 0f },
                    onDragCancel = { dragOffsetY = 0f },
                    onVerticalDrag = { _, delta ->
                        if (delta > 0) dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                    }
                )
            }
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.Start      // everything left-aligned
        ) {
            Spacer(Modifier.height(12.dp))

            // Drag handle — centred by its own Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .width(36.dp).height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }

            Spacer(Modifier.height(12.dp))

            // Header row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Dismiss", modifier = Modifier.size(28.dp))
                }
                Text(
                    "Now Playing",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Filled.QueueMusic, "Queue", modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Album art
            AlbumArt(
                albumId    = track?.albumId,
                sizeDp     = 300.dp,
                cornerDp   = 12.dp,
                showShadow = true,
                modifier   = Modifier.fillMaxWidth().aspectRatio(1f).scale(artScale)
            )

            Spacer(Modifier.height(24.dp))

            // Title — left-aligned, marquee if too long
            MarqueeText(
                text  = track?.title ?: "Not playing",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(4.dp))

            // Artist — left-aligned, primary colour, marquee if too long
            MarqueeText(
                text  = track?.artist ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary
                )
            )

            // Format badge (one at a time — AnimatedContent crossfade)
            if (track != null) {
                Spacer(Modifier.height(8.dp))
                FormatBadgeRow(
                    isLossless = track.isLossless,
                    isHiRes    = track.sampleRateHz >= 48_000 && track.bitDepth >= 24,
                    isAtmos    = track.isAtmos,
                    isMixing   = state.isCrossfading
                )
            }

            Spacer(Modifier.height(20.dp))

            // Scrubber
            if (state.durationMs > 0) {
                Slider(
                    value         = state.progress,
                    onValueChange = { playerViewModel.seekTo((it * state.durationMs).toLong()) },
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatDuration(state.positionMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDuration(state.durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Transport controls
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.setShuffleEnabled(!state.shuffleEnabled) }) {
                    Icon(Icons.Filled.Shuffle, "Shuffle",
                        tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, "Previous", modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick  = { playerViewModel.playOrPause() },
                    modifier = Modifier.size(64.dp),
                    shape    = CircleShape,
                    colors   = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor   = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { playerViewModel.skipToNext() }) {
                    Icon(Icons.Filled.SkipNext, "Next", modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = {
                    playerViewModel.setRepeatMode(when (state.repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    })
                }) {
                    Icon(
                        if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne
                        else Icons.Filled.Repeat,
                        "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            VolumeSlider()
            Spacer(Modifier.height(24.dp))
        }

        if (showQueue) {
            QueueSheet(playerViewModel = playerViewModel, onDismiss = { showQueue = false })
        }
    }
}
