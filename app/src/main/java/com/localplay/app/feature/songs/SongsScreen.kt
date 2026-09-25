package com.localplay.app.feature.songs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.feature.library.SortOrder
import com.localplay.app.ui.components.TrackRow

@Composable
fun SongsScreen(lib: LibraryViewModel = viewModel(), player: PlayerViewModel = viewModel()) {
    val tracks by lib.allTracks.collectAsState()
    val ps by player.playerState.collectAsState()
    val sortOrder by lib.sortOrder.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sort bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${tracks.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                TextButton(onClick = { showSortMenu = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Filled.Sort, "Sort", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(when (sortOrder) {
                        SortOrder.TITLE -> "Title"
                        SortOrder.ARTIST -> "Artist"
                        SortOrder.ALBUM -> "Album"
                        SortOrder.DATE_ADDED -> "Date Added"
                    }, style = MaterialTheme.typography.labelSmall)
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(when (order) {
                                SortOrder.TITLE -> "Title"
                                SortOrder.ARTIST -> "Artist"
                                SortOrder.ALBUM -> "Album"
                                SortOrder.DATE_ADDED -> "Date Added"
                            }) },
                            onClick = { lib.setSortOrder(order); showSortMenu = false }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = tracks, key = { it.id }) { t ->
                TrackRow(t, t.id == ps.currentTrack?.id, onClick = { player.playQueue(tracks, tracks.indexOf(t)) })
            }
        }
    }
}
