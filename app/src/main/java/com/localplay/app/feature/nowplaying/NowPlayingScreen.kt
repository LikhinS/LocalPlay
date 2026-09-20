package com.localplay.app.feature.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
 * Full-screen Now Playing screen — Phase 5.
 *
 * New in this phase vs Phase 4:
 *  - Swipe-down-to-dismiss gesture with animated art scale + alpha
 *  - Volume slider wired to AudioManager
 *  - Queue bottom sheet ("Up Next")
 *  - Proper dismiss handle / drag indicator at the top
 *
 * The gradient background wraps around this composable in Phase 7 —
 * nothing here needs to change for that; the background is a separate
 * layer behind this Column, not inside it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerViewModel: PlayerViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val state by playerViewModel.playerState.collectAsState()
    val track = state.currentTrack

    // ── Swipe-to-dismiss gesture ─────────────────────────────────────────
    // When the user drags down more than 120dp we call onDismiss().
    // The art scales down and the whole screen fades as they drag,
    // giving tactile feedback without any blur or heavy animation.
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 120f
    val dragFraction = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)

    val artScale by animateFloatAsState(
        targetValue = 1f - (dragFraction * 0.08f),   // shrinks to 92% at threshold
        animationSpec = tween(durationMillis = 0),    // instant follow during drag
        label = "artScale"
    )
    val screenAlpha by animateFloatAsState(
        targetValue = 1f - (dragFraction * 0.4f),     // fades to 60% at threshold
        animationSpec = tween(durationMillis = 0),
        label = "screenAlpha"
    )

    // Queue sheet state
    var showQueue by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffsetY > dismissThreshold) onDismiss()
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f },
                    onVerticalDrag = { _, delta ->
                        if (delta > 0) dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // Drag handle — visual affordance that the screen is swipeable
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(Modifier.height(12.dp))

            // Top row: chevron dismiss | title "Now Playing" | queue button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Dismiss",
                        modifier = Modifier.size(28.dp))
                }
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = "Queue",
                        modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Album art — scales down as the user drags
            AlbumArt(
                trackId    = track?.id,
                sizeDp     = 300.dp,
                cornerDp   = 12.dp,
                showShadow = true,
                modifier   = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .scale(artScale)
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

            Spacer(Modifier.height(20.dp))

            // Scrubber
            if (state.durationMs > 0) {
                Slider(
                    value         = state.progress,
                    onValueChange = { playerViewModel.seekTo((it * state.durationMs).toLong()) },
                    modifier      = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerViewModel.setShuffleEnabled(!state.shuffleEnabled) }) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle",
                        tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { playerViewModel.skipToPrevious() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                // Large play/pause — filled circle background like Apple Music
                FilledIconButton(
                    onClick   = { playerViewModel.playOrPause() },
                    modifier  = Modifier.size(64.dp),
                    shape     = CircleShape,
                    colors    = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor   = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { playerViewModel.skipToNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = {
                    playerViewModel.setRepeatMode(when (state.repeatMode) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    })
                }) {
                    Icon(
                        imageVector = if (state.repeatMode == RepeatMode.ONE)
                            Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Volume slider
            VolumeSlider()

            Spacer(Modifier.height(24.dp))
        }

        // Queue bottom sheet
        if (showQueue) {
            QueueSheet(
                playerViewModel = playerViewModel,
                onDismiss       = { showQueue = false }
            )
        }
    }
}
