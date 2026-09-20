package com.localplay.app.feature.songs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.TrackRow

@Composable
fun SongsScreen(libraryViewModel: LibraryViewModel = viewModel(), playerViewModel: PlayerViewModel = viewModel()) {
    val tracks      by libraryViewModel.allTracks.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = tracks, key = { it.id }) { track ->
            TrackRow(track = track, isNowPlaying = track.id == playerState.currentTrack?.id,
                onClick = { playerViewModel.playQueue(tracks, tracks.indexOf(track)) })
        }
    }
}
