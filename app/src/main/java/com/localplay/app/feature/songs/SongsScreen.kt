package com.localplay.app.feature.songs
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.TrackRow
@Composable fun SongsScreen(lib:LibraryViewModel=viewModel(),player:PlayerViewModel=viewModel()) {
    val tracks by lib.allTracks.collectAsState()
    val ps by player.playerState.collectAsState()
    LazyColumn(modifier=Modifier.fillMaxSize()) {
        items(items=tracks,key={it.id}){ t -> TrackRow(t,t.id==ps.currentTrack?.id,onClick={player.playQueue(tracks,tracks.indexOf(t))}) }
    }
}
