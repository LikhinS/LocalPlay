package com.localplay.app.feature.artists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun ArtistsScreen(
    lib: LibraryViewModel   = viewModel(),
    player: PlayerViewModel = viewModel()
) {
    val tracks  by lib.allTracks.collectAsState()
    val artists = remember(tracks) {
        tracks.groupBy { it.artist }.entries.sortedBy { it.key }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = artists, key = { it.key }) { (name, ts) ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { player.playQueue(ts.sortedBy { it.title }, 0) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArt(
                        albumId  = ts.first().albumId,
                        sizeDp   = 48.dp,
                        cornerDp = 24.dp,
                        modifier = Modifier.clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${ts.size} songs · ${ts.map { it.album }.distinct().size} albums",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(
                    modifier  = Modifier.padding(start = 76.dp),
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}
