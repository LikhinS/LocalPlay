package com.localplay.app.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.AlbumArt

@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel   = viewModel()
) {
    val recentlyAdded  by libraryViewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsState()
    val allTracks      by libraryViewModel.allTracks.collectAsState()

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (recentlyPlayed.isNotEmpty()) {
            item(key = "recently_played_header") {
                SectionHeader("Recently Played")
            }
            item(key = "recently_played_row") {
                HorizontalTrackRail(
                    tracks    = recentlyPlayed.take(10),
                    onTap     = { track ->
                        playerViewModel.playQueue(allTracks, allTracks.indexOf(track))
                    }
                )
            }
            item(key = "spacer1") { Spacer(Modifier.height(24.dp)) }
        }

        if (recentlyAdded.isNotEmpty()) {
            item(key = "recently_added_header") {
                SectionHeader("Recently Added")
            }
            item(key = "recently_added_row") {
                HorizontalTrackRail(
                    tracks = recentlyAdded.take(10),
                    onTap  = { track ->
                        playerViewModel.playQueue(
                            recentlyAdded, recentlyAdded.indexOf(track)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun HorizontalTrackRail(
    tracks: List<TrackEntity>,
    onTap: (TrackEntity) -> Unit
) {
    LazyRow(
        contentPadding      = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = tracks, key = { it.id }) { track ->
            Column(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { onTap(track) }
            ) {
                AlbumArt(
                    trackId    = track.id,
                    sizeDp     = 130.dp,
                    cornerDp   = 8.dp,
                    showShadow = true,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = track.title,
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text     = track.artist,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
