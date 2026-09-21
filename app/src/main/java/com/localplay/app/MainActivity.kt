package com.localplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.localplay.app.feature.settings.SettingsScreen
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
    HOME    ("Home",     Icons.Filled.Home),
    SONGS   ("Songs",   Icons.Filled.MusicNote),
    ALBUMS  ("Albums",  Icons.Filled.Album),
    ARTISTS ("Artists", Icons.Filled.Person),
    SEARCH  ("Search",  Icons.Filled.Search),
    SETTINGS("Settings",Icons.Filled.Settings)
}

@Composable
fun LocalPlayApp() {
    LocalPlayTheme {
        val perm = rememberAudioPermissionState()
        if (!perm.value) {
            PermissionScreen(onPermissionGranted = { perm.value = true })
            return@LocalPlayTheme
        }

        val lib: LibraryViewModel   = viewModel()
        val player: PlayerViewModel = viewModel()

        LaunchedEffect(Unit) { lib.scanLibrary() }

        var tab     by rememberSaveable { mutableStateOf(Tab.HOME) }
        var showNP  by remember { mutableStateOf(false) }
        val ps      by player.playerState.collectAsState()

        if (showNP) {
            NowPlayingScreen(player, onDismiss = { showNP = false })
            return@LocalPlayTheme
        }

        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MiniPlayer(
                        state          = ps,
                        onExpand       = { showNP = true },
                        onPlayPause    = { player.playOrPause() },
                        onSkipNext     = { player.skipToNext() },
                        onSkipPrevious = { player.skipToPrevious() }
                    )
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick  = { tab = t },
                                icon     = { Icon(t.icon, t.label) },
                                label    = { Text(t.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    Tab.HOME     -> HomeScreen(lib, player)
                    Tab.SONGS    -> SongsScreen(lib, player)
                    Tab.ALBUMS   -> AlbumsScreen(lib, player)
                    Tab.ARTISTS  -> ArtistsScreen(lib, player)
                    Tab.SEARCH   -> SearchScreen(lib, player)
                    Tab.SETTINGS -> SettingsScreen(player)
                }
            }
        }
    }
}
