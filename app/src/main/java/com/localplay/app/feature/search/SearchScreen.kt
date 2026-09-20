package com.localplay.app.feature.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.TrackRow

@Composable
fun SearchScreen(libraryViewModel: LibraryViewModel = viewModel(), playerViewModel: PlayerViewModel = viewModel()) {
    val allTracks   by libraryViewModel.allTracks.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, allTracks) {
        if (query.isBlank()) emptyList()
        else allTracks.filter { t ->
            val q = query.lowercase()
            t.title.lowercase().contains(q) || t.artist.lowercase().contains(q) || t.album.lowercase().contains(q)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(value = query, onValueChange = { query = it },
            placeholder = { Text("Songs, artists, albums") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp))
        when {
            query.isBlank() -> Text("Search your library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            filtered.isEmpty() -> Text("No results for \"$query\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = filtered, key = { it.id }) { track ->
                    TrackRow(track = track, isNowPlaying = track.id == playerState.currentTrack?.id,
                        onClick = { playerViewModel.playQueue(filtered, filtered.indexOf(track)) })
                }
            }
        }
    }
}
