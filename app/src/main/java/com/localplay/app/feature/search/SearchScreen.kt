package com.localplay.app.feature.search
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.TrackRow
@Composable fun SearchScreen(lib:LibraryViewModel=viewModel(),player:PlayerViewModel=viewModel()) {
    val all by lib.allTracks.collectAsState()
    val ps by player.playerState.collectAsState()
    var q by remember{ mutableStateOf("") }
    val filtered = remember(q,all){ if(q.isBlank()) emptyList() else all.filter{ val lq=q.lowercase(); it.title.lowercase().contains(lq)||it.artist.lowercase().contains(lq)||it.album.lowercase().contains(lq) } }
    Column(modifier=Modifier.fillMaxSize()) {
        OutlinedTextField(value=q,onValueChange={q=it},placeholder={Text("Songs, artists, albums")},singleLine=true,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp))
        when {
            q.isBlank() -> Text("Search your library",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))
            filtered.isEmpty() -> Text("No results for \"$q\"",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(horizontal=16.dp,vertical=8.dp))
            else -> LazyColumn(modifier=Modifier.fillMaxSize()){
                items(items=filtered,key={it.id}){ t -> TrackRow(t,t.id==ps.currentTrack?.id,onClick={player.playQueue(filtered,filtered.indexOf(t))}) }
            }
        }
    }
}
