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
import com.localplay.app.feature.permissions.*
import com.localplay.app.feature.search.SearchScreen
import com.localplay.app.feature.songs.SongsScreen
import com.localplay.app.ui.components.MiniPlayer
import com.localplay.app.ui.theme.LocalPlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { LocalPlayApp() } }
}

private enum class Tab(val label:String, val icon:ImageVector) {
    HOME("Home",Icons.Filled.Home), SONGS("Songs",Icons.Filled.MusicNote),
    ALBUMS("Albums",Icons.Filled.Album), ARTISTS("Artists",Icons.Filled.Person),
    SEARCH("Search",Icons.Filled.Search)
}

@Composable fun LocalPlayApp() {
    LocalPlayTheme {
        val perm = rememberAudioPermissionState()
        if (!perm.value) { PermissionScreen(onPermissionGranted={perm.value=true}); return@LocalPlayTheme }

        val lib: LibraryViewModel = viewModel()
        val player: PlayerViewModel = viewModel()
        LaunchedEffect(Unit) { lib.scanLibrary() }

        var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
        var showNP by remember { mutableStateOf(false) }
        val ps by player.playerState.collectAsState()

        if (showNP) { NowPlayingScreen(player, onDismiss={showNP=false}); return@LocalPlayTheme }

        Scaffold(bottomBar={
            Column(modifier=Modifier.fillMaxWidth()) {
                MiniPlayer(ps, onExpand={showNP=true}, onPlayPause={player.playOrPause()},
                    onSkipNext={player.skipToNext()}, onSkipPrevious={player.skipToPrevious()})
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(selected=tab==t, onClick={tab=t},
                            icon={Icon(t.icon,t.label)}, label={Text(t.label)})
                    }
                }
            }
        }) { padding ->
            Box(modifier=Modifier.fillMaxSize().padding(padding)) {
                when(tab) {
                    Tab.HOME    -> HomeScreen(lib,player)
                    Tab.SONGS   -> SongsScreen(lib,player)
                    Tab.ALBUMS  -> AlbumsScreen(lib,player)
                    Tab.ARTISTS -> ArtistsScreen(lib,player)
                    Tab.SEARCH  -> SearchScreen(lib,player)
                }
            }
        }
    }
}
