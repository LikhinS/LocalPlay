package com.localplay.app.feature.home
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
@Composable fun HomeScreen(lib:LibraryViewModel=viewModel(), player:PlayerViewModel=viewModel()) {
    val recentlyAdded by lib.recentlyAdded.collectAsState()
    val recentlyPlayed by lib.recentlyPlayed.collectAsState()
    val all by lib.allTracks.collectAsState()
    LazyColumn(modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=16.dp)) {
        if(recentlyPlayed.isNotEmpty()) {
            item(key="rp_h"){ Text("Recently Played",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(start=16.dp,top=24.dp,bottom=8.dp)) }
            item(key="rp_r"){ TrackRail(recentlyPlayed.take(10)){ player.playQueue(all,all.indexOf(it)) } }
        }
        if(recentlyAdded.isNotEmpty()) {
            item(key="ra_h"){ Text("Recently Added",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(start=16.dp,top=24.dp,bottom=8.dp)) }
            item(key="ra_r"){ TrackRail(recentlyAdded.take(10)){ player.playQueue(recentlyAdded,recentlyAdded.indexOf(it)) } }
        }
    }
}
@Composable private fun TrackRail(tracks:List<TrackEntity>, onTap:(TrackEntity)->Unit) {
    LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        items(items=tracks,key={it.id}){ t ->
            Column(modifier=Modifier.width(130.dp).clickable{onTap(t)}) {
                AlbumArt(trackId=t.id,sizeDp=130.dp,cornerDp=8.dp,showShadow=true,modifier=Modifier.fillMaxWidth().aspectRatio(1f))
                Spacer(Modifier.height(6.dp))
                Text(t.title,style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(t.artist,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
        }
    }
}
