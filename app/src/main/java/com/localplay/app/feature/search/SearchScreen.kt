package com.localplay.app.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.TrackRow

@Composable
fun SearchScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel   = viewModel()
) {
    val allTracks   by libraryViewModel.allTracks.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    var query by remember { mutableStateOf("") }

    // Instant local filter — no debounce needed since filtering
    // a local List<TrackEntity> on the main thread is O(n) and fast
    // even for libraries of several thousand tracks.
    val filtered: List<TrackEntity> = remember(query, allTracks) {
        if (query.isBlank()) emptyList()
        else allTracks.filter { track ->
            val q = query.lowercase()
            track.title.lowercase().contains(q)  ||
            track.artist.lowercase().contains(q) ||
            track.album.lowercase().contains(q)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value         = query,
            onValueChange = { query = it },
            placeholder   = { Text("Songs, artists, albums") },
            singleLine    = true,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        when {
            query.isBlank() -> {
                Text(
                    text     = "Search your library",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            filtered.isEmpty() -> {
                Text(
                    text     = "No results for \"$query\"",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = filtered, key = { it.id }) { track ->
                        TrackRow(
                            track        = track,
                            isNowPlaying = track.id == playerState.currentTrack?.id,
                            onClick      = {
                                playerViewModel.playQueue(filtered, filtered.indexOf(track))
                            }
                        )
                    }
                }
            }
        }
    }
}
