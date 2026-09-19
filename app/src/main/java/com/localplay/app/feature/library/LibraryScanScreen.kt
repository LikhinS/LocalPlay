package com.localplay.app.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.PlayerViewModel

/**
 * Phase 2/3 debug screen — scan + tap-to-play.
 * Replaced by the full Apple Music-style UI in Phase 4.
 */
@Composable
fun LibraryScanScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel   = viewModel()
) {
    val scanState  by libraryViewModel.scanState.collectAsState()
    val allTracks  by libraryViewModel.allTracks.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()

    LaunchedEffect(Unit) { libraryViewModel.scanLibrary() }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Minimal now-playing status bar at the top
            playerState.currentTrack?.let { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { playerViewModel.playOrPause() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = if (playerState.isPlaying) "⏸" else "▶",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
                HorizontalDivider()
            }

            when (val state = scanState) {
                is LibraryViewModel.ScanState.Idle -> CenteredMessage("Preparing scan…")
                is LibraryViewModel.ScanState.Scanning -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scanning your library…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is LibraryViewModel.ScanState.Error  -> CenteredMessage("Scan error: ${state.message}")
                is LibraryViewModel.ScanState.Done   -> {
                    TrackList(
                        trackCount = state.trackCount,
                        tracks     = allTracks,
                        nowPlayingId = playerState.currentTrack?.id,
                        onTrackClick = { track ->
                            playerViewModel.playQueue(allTracks, allTracks.indexOf(track))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackList(
    trackCount: Int,
    tracks: List<TrackEntity>,
    nowPlayingId: Long?,
    onTrackClick: (TrackEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Library  •  $trackCount tracks",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(items = tracks, key = { it.id }) { track ->
                TrackRow(
                    track         = track,
                    isNowPlaying  = track.id == nowPlayingId,
                    onClick       = { onTrackClick(track) }
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: TrackEntity,
    isNowPlaying: Boolean,
    onClick: () -> Unit
) {
    val highlightColor = if (isNowPlaying)
        MaterialTheme.colorScheme.primary
    else
        Color.Unspecified

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text  = track.title,
            style = MaterialTheme.typography.bodyMedium,
            color = highlightColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = buildString {
                append(track.artist)
                if (track.isLossless) append(" · Lossless")
                if (track.isAtmos)    append(" · Atmos")
            },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
