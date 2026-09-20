package com.localplay.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.AlbumArt

@Composable
fun HomeScreen(libraryViewModel: LibraryViewModel = viewModel(), playerViewModel: PlayerViewModel = viewModel()) {
    val recentlyAdded  by libraryViewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsState()
    val allTracks      by libraryViewModel.allTracks.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (recentlyPlayed.isNotEmpty()) {
            item(key = "rp_header") {
                Text("Recently Played", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
            }
            item(key = "rp_rail") {
                TrackRail(recentlyPlayed.take(10)) { t ->
                    playerViewModel.playQueue(allTracks, allTracks.indexOf(t))
                }
            }
            item(key = "rp_spacer") { Spacer(Modifier.height(24.dp)) }
        }
        if (recentlyAdded.isNotEmpty()) {
            item(key = "ra_header") {
                Text("Recently Added", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
            }
            item(key = "ra_rail") {
                TrackRail(recentlyAdded.take(10)) { t ->
                    playerViewModel.playQueue(recentlyAdded, recentlyAdded.indexOf(t))
                }
            }
        }
    }
}

@Composable
private fun TrackRail(tracks: List<TrackEntity>, onTap: (TrackEntity) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items = tracks, key = { it.id }) { track ->
            Column(modifier = Modifier.width(130.dp).clickable { onTap(track) }) {
                AlbumArt(trackId = track.id, sizeDp = 130.dp, cornerDp = 8.dp, showShadow = true,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(6.dp))
                Text(track.title, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
