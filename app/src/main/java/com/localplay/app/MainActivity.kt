package com.localplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME    ("Home",    Icons.Filled.Home),
    SONGS   ("Songs",  Icons.Filled.MusicNote),
    ALBUMS  ("Albums", Icons.Filled.Album),
    ARTISTS ("Artists",Icons.Filled.Person),
    SEARCH  ("Search", Icons.Filled.Search)
}

@Composable
fun LocalPlayApp() {
    LocalPlayTheme {
        val permissionGranted = rememberAudioPermissionState()
        if (!permissionGranted.value) {
            PermissionScreen(onPermissionGranted = { permissionGranted.value = true })
            return@LocalPlayTheme
        }

        val libraryViewModel: LibraryViewModel = viewModel()
        val playerViewModel: PlayerViewModel   = viewModel()

        LaunchedEffect(Unit) { libraryViewModel.scanLibrary() }

        var selectedTab    by rememberSaveable { mutableStateOf(Tab.HOME) }
        var showNowPlaying by remember        { mutableStateOf(false) }
        val playerState    by playerViewModel.playerState.collectAsState()

        if (showNowPlaying) {
            NowPlayingScreen(playerViewModel = playerViewModel,
                onDismiss = { showNowPlaying = false })
            return@LocalPlayTheme
        }

        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MiniPlayer(
                        state          = playerState,
                        onExpand       = { showNowPlaying = true },
                        onPlayPause    = { playerViewModel.playOrPause() },
                        onSkipNext     = { playerViewModel.skipToNext() },
                        onSkipPrevious = { playerViewModel.skipToPrevious() }
                    )
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick  = { selectedTab = tab },
                                icon     = { Icon(tab.icon, contentDescription = tab.label) },
                                label    = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
