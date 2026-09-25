package com.localplay.app.feature.nowplaying

import androidx.compose.animation.core.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.core.player.model.RepeatMode
import com.localplay.app.ui.components.*
import com.localplay.app.ui.gradient.ArtworkGradientBackground

@Composable
fun NowPlayingScreen(playerViewModel: PlayerViewModel = viewModel(), onDismiss: () -> Unit) {
    val state by playerViewModel.playerState.collectAsState()
    val track = state.currentTrack
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 120f
    val dragFraction = (dragOffsetY / dismissThreshold).coerceIn(0f, 1f)
    val artScale by animateFloatAsState(1f - dragFraction * 0.08f, tween(0), label = "scale")
    val screenAlpha by animateFloatAsState(1f - dragFraction * 0.4f, tween(0), label = "alpha")
    var showQueue by remember { mutableStateOf(false) }

    ArtworkGradientBackground(albumId = track?.albumId) {
        Box(modifier = Modifier.fillMaxSize().alpha(screenAlpha).pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragEnd = { if (dragOffsetY > dismissThreshold) onDismiss(); dragOffsetY = 0f },
                onDragCancel = { dragOffsetY = 0f },
                onVerticalDrag = { _, delta -> if (delta > 0) dragOffsetY = (dragOffsetY + delta).coerceAtLeast(0f) }
            )
        }) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.Start) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Box(Modifier.width(36.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.KeyboardArrowDown, "Dismiss", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface) }
                    Text("Now Playing", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    IconButton(onClick = { showQueue = true }) { Icon(Icons.Filled.QueueMusic, "Queue", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface) }
                }
                Spacer(Modifier.height(20.dp))
                AlbumArt(albumId = track?.albumId, sizeDp = 300.dp, cornerDp = 12.dp, showShadow = true,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).scale(artScale))
                Spacer(Modifier.height(24.dp))
                MarqueeText(text = track?.title ?: "Not playing",
                    style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface))
                Spacer(Modifier.height(4.dp))
                MarqueeText(text = track?.artist ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)))
                if (track != null) {
                    Spacer(Modifier.height(8.dp))
                    FormatBadgeRow(isLossless = track.isLossless, isHiRes = track.sampleRateHz >= 48_000 && track.bitDepth >= 24, isAtmos = track.isAtmos, isMixing = state.isCrossfading)
                }
                Spacer(Modifier.height(20.dp))
                if (state.durationMs > 0) {
                    Slider(value = state.progress, onValueChange = { playerViewModel.seekTo((it * state.durationMs).toLong()) }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDuration(state.positionMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(formatDuration(state.durationMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { playerViewModel.setShuffleEnabled(!state.shuffleEnabled) }) {
                        Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = { playerViewModel.skipToPrevious() }) { Icon(Icons.Filled.SkipPrevious, "Previous", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface) }
                    FilledIconButton(onClick = { playerViewModel.playOrPause() }, modifier = Modifier.size(64.dp), shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.onSurface, contentColor = MaterialTheme.colorScheme.surface)) {
                        Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (state.isPlaying) "Pause" else "Play", modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = { playerViewModel.skipToNext() }) { Icon(Icons.Filled.SkipNext, "Next", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = { playerViewModel.setRepeatMode(when (state.repeatMode) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF }) }) {
                        Icon(if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Repeat",
                            tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                VolumeSlider()
                Spacer(Modifier.height(24.dp))
            }
            if (showQueue) QueueSheet(playerViewModel = playerViewModel, onDismiss = { showQueue = false })
        }
    }
}
