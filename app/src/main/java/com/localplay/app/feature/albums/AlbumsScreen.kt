package com.localplay.app.feature.albums
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity
import com.localplay.app.core.player.PlayerViewModel
import com.localplay.app.feature.library.LibraryViewModel
import com.localplay.app.ui.components.AlbumArt
@Composable fun AlbumsScreen(lib:LibraryViewModel=viewModel(),player:PlayerViewModel=viewModel()) {
    val tracks by lib.allTracks.collectAsState()
    val albums = remember(tracks){ tracks.groupBy{it.album}.entries.sortedBy{it.key} }
    LazyVerticalGrid(columns=GridCells.Fixed(2),contentPadding=PaddingValues(12.dp),
        horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(16.dp),modifier=Modifier.fillMaxSize()) {
        items(items=albums,key={it.key}){ (name,ts) ->
            Column(modifier=Modifier.fillMaxWidth().clickable{
                player.playQueue(ts.sortedWith(compareBy({it.discNumber},{it.trackNumber})),0)}) {
                AlbumArt(ts.first().id,sizeDp=160.dp,cornerDp=8.dp,showShadow=true,modifier=Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(6.dp))
                Text(name,style=MaterialTheme.typography.bodyLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text("${ts.first().albumArtist.ifBlank{ts.first().artist}} · ${ts.size} songs",
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
