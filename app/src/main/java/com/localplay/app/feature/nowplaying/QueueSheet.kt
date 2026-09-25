package com.localplay.app.feature.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.ui.components.AlbumArt
import com.localplay.app.ui.components.formatDuration

@Composable
fun QueueSheet(playerViewModel: PlayerViewModel = viewModel(), onDismiss: () -> Unit) {
    val state by playerViewModel.playerState.collectAsState()
    Box(modifier = Modifier.fillMaxSize().background(Color(0x80000000)).clickable(onClick = onDismiss)) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f).align(Alignment.BottomCenter)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface).clickable(enabled = false) {}) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Up Next", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.5.dp)
            if (state.queue.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Queue is empty", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(state.queue, key = { i, t -> "${t.id}_$i" }) { index, track ->
                        val isPlaying = index == state.queueIndex
                        val color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth()
                                .clickable { playerViewModel.repository.playQueue(state.queue, index) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                if (isPlaying) {
                                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), Alignment.Center) {
                                        Icon(Icons.Filled.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    }
                                } else { AlbumArt(albumId = track.albumId, sizeDp = 48.dp, cornerDp = 6.dp) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(formatDuration(track.durationMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
