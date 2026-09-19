package com.localplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.albums.AlbumsScreen
import com.localplay.app.feature.artists.ArtistsScreen
import com.localplay.app.feature.home.HomeScreen
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.feature.nowplaying.NowPlayingScreen
import com.localplay.app.feature.permissions.PermissionScreen
import com.localplay.app.feature.permissions.rememberAudioPermissionState
import com.localplay.app.feature.search.SearchScreen
import com.localplay.app.feature.songs.SongsScreen
import com.localplay.app.ui.components.MiniPlayer
import com.localplay.app.ui.theme.LocalPlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LocalPlayApp() }
    }
}

private enum class Tab(val label: String, val iconRes: Int) {
    HOME    ("Home",    android.R.drawable.ic_menu_today),
    SONGS   ("Songs",  android.R.drawable.ic_menu_music_folder),
    ALBUMS  ("Albums", android.R.drawable.ic_menu_gallery),
    ARTISTS ("Artists",android.R.drawable.ic_menu_myplaces),
    SEARCH  ("Search", android.R.drawable.ic_menu_search)
}

@Composable
fun LocalPlayApp() {
    LocalPlayTheme {
        val permissionGranted = rememberAudioPermissionState()

        if (!permissionGranted.value) {
            PermissionScreen(onPermissionGranted = { permissionGranted.value = true })
            return@LocalPlayTheme
        }

        // Shared ViewModels — one instance each for the whole app
        val libraryViewModel: LibraryViewModel = viewModel()
        val playerViewModel: PlayerViewModel   = viewModel()

        // Kick off library scan once permission is confirmed
        androidx.compose.runtime.LaunchedEffect(Unit) {
            libraryViewModel.scanLibrary()
        }

        var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME) }
        var showNowPlaying by remember { mutableStateOf(false) }

        val playerState by playerViewModel.playerState.collectAsState()

        if (showNowPlaying) {
            NowPlayingScreen(
                playerViewModel = playerViewModel,
                onDismiss       = { showNowPlaying = false }
            )
            return@LocalPlayTheme
        }

        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini player sits directly above the nav bar
                    MiniPlayer(
                        state           = playerState,
                        onExpand        = { showNowPlaying = true },
                        onPlayPause     = { playerViewModel.playOrPause() },
                        onSkipNext      = { playerViewModel.skipToNext() },
                        onSkipPrevious  = { playerViewModel.skipToPrevious() }
                    )
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick  = { selectedTab = tab },
                                icon     = {
                                    Icon(
                                        painter = painterResource(tab.iconRes),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    Tab.HOME    -> HomeScreen(libraryViewModel, playerViewModel)
                    Tab.SONGS   -> SongsScreen(libraryViewModel, playerViewModel)
                    Tab.ALBUMS  -> AlbumsScreen(libraryViewModel, playerViewModel)
                    Tab.ARTISTS -> ArtistsScreen(libraryViewModel, playerViewModel)
                    Tab.SEARCH  -> SearchScreen(libraryViewModel, playerViewModel)
                }
            }
        }
    }
}
