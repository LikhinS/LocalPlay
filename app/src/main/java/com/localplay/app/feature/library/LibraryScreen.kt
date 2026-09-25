package com.localplay.app.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.ui.components.AlbumArt

enum class LibraryTab { PLAYLISTS, ALBUMS, ARTISTS }

@Composable
fun LibraryScreen(lib: LibraryViewModel = viewModel(), player: PlayerViewModel = viewModel()) {
    val tracks by lib.allTracks.collectAsState()
    var activeTab by remember { mutableStateOf(LibraryTab.ALBUMS) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab row
        TabRow(selectedTabIndex = activeTab.ordinal) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        when (activeTab) {
            LibraryTab.PLAYLISTS -> PlaylistsTab()
            LibraryTab.ALBUMS    -> AlbumsTab(tracks, player)
            LibraryTab.ARTISTS   -> ArtistsTab(tracks, player)
        }
    }
}

@Composable
private fun PlaylistsTab() {
    // Playlists are a Phase 9 feature — scaffold shown here
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.QueueMusic, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("No playlists yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {}) { Text("New Playlist") }
        }
    }
}

@Composable
private fun AlbumsTab(tracks: List<TrackEntity>, player: PlayerViewModel) {
    val albums = remember(tracks) { tracks.groupBy { it.album }.entries.sortedBy { it.key } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = albums, key = { it.key }) { (name, ts) ->
            Column(modifier = Modifier.fillMaxWidth().clickable {
                player.playQueue(ts.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })), 0)
            }) {
                AlbumArt(albumId = ts.first().albumId, sizeDp = 160.dp, cornerDp = 8.dp, showShadow = true, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(6.dp))
                Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${ts.first().albumArtist.ifBlank { ts.first().artist }} · ${ts.size} songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ArtistsTab(tracks: List<TrackEntity>, player: PlayerViewModel) {
    val artists = remember(tracks) { tracks.groupBy { it.artist }.entries.sortedBy { it.key } }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = artists, key = { it.key }) { (name, ts) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().clickable { player.playQueue(ts.sortedBy { it.title }, 0) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(albumId = ts.first().albumId, sizeDp = 48.dp, cornerDp = 24.dp, modifier = Modifier.clip(CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${ts.size} songs · ${ts.map { it.album }.distinct().size} albums", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 76.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }
    }
}
