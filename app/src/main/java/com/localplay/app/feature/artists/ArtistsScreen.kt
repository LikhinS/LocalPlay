package com.localplay.app.feature.artists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.AlbumArt

@Composable
fun ArtistsScreen(libraryViewModel: LibraryViewModel = viewModel(), playerViewModel: PlayerViewModel = viewModel()) {
    val tracks by libraryViewModel.allTracks.collectAsState()
    val artists = remember(tracks) { tracks.groupBy { it.artist }.entries.sortedBy { it.key } }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = artists, key = { it.key }) { (artistName, artistTracks) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth()
                    .clickable { playerViewModel.playQueue(artistTracks.sortedBy { it.title }, 0) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    AlbumArt(trackId = artistTracks.first().id, sizeDp = 48.dp, cornerDp = 24.dp,
                        modifier = Modifier.clip(CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artistName, style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${artistTracks.size} songs · ${artistTracks.map { it.album }.distinct().size} albums",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(start = 76.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }
    }
}
