package com.localplay.app.feature.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.AlbumArt

@Composable
fun AlbumsScreen(
    lib: LibraryViewModel   = viewModel(),
    player: PlayerViewModel = viewModel()
) {
    val tracks by lib.allTracks.collectAsState()
    val albums = remember(tracks) {
        tracks.groupBy { it.album }.entries.sortedBy { it.key }
    }

    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        contentPadding        = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        modifier              = Modifier.fillMaxSize()
    ) {
        items(items = albums, key = { it.key }) { (name, ts) ->
            AlbumCell(name, ts, onClick = {
                player.playQueue(ts.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })), 0)
            })
        }
    }
}

@Composable
private fun AlbumCell(
    albumName: String,
    tracks: List<TrackEntity>,
    onClick: () -> Unit
) {
    val first = tracks.first()
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        AlbumArt(
            albumId    = first.albumId,
            sizeDp     = 160.dp,
            cornerDp   = 8.dp,
            showShadow = true,
            modifier   = Modifier.fillMaxWidth().aspectRatio(1f)
        )
        Spacer(Modifier.height(6.dp))
        Text(albumName, style = MaterialTheme.typography.bodyLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${first.albumArtist.ifBlank { first.artist }} · ${tracks.size} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}
