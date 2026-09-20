package com.localplay.app.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localplay.app.core.database.entity.TrackEntity
@Composable fun TrackRow(track:TrackEntity, isNowPlaying:Boolean=false, showAlbumArt:Boolean=true, onClick:()->Unit, modifier:Modifier=Modifier) {
    val color = if(isNowPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Column(modifier=modifier.fillMaxWidth()) {
        Row(modifier=Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=16.dp,vertical=10.dp), verticalAlignment=Alignment.CenterVertically) {
            if(showAlbumArt) { AlbumArt(trackId=track.id,sizeDp=48.dp,cornerDp=6.dp); Spacer(Modifier.width(12.dp)) }
            Column(modifier=Modifier.weight(1f)) {
                Text(track.title,style=MaterialTheme.typography.bodyLarge,color=color,maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(buildString{append(track.artist);if(track.album.isNotBlank()){append(" — ");append(track.album)}},
                    style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(formatDuration(track.durationMs),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier=Modifier.padding(start=if(showAlbumArt) 76.dp else 16.dp),
            color=MaterialTheme.colorScheme.outline.copy(alpha=0.4f),thickness=0.5.dp)
    }
}
fun formatDuration(ms:Long):String { val s=ms/1000; return "%d:%02d".format(s/60,s%60) }
