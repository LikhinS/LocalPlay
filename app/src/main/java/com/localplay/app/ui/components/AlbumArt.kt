package com.localplay.app.ui.components
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
@Composable fun AlbumArt(trackId: Long?, modifier: Modifier=Modifier, sizeDp: Dp=48.dp, cornerDp: Dp=6.dp, showShadow: Boolean=false) {
    val shape = RoundedCornerShape(cornerDp)
    val base = modifier.size(sizeDp).then(if(showShadow) Modifier.shadow(4.dp,shape) else Modifier).clip(shape)
    if (trackId != null) {
        AsyncImage(model=ImageRequest.Builder(LocalContext.current).data(albumArtUri(trackId))
            .size((sizeDp.value*2).toInt(),(sizeDp.value*2).toInt()).crossfade(true).build(),
            contentDescription=null, contentScale=ContentScale.Crop, modifier=base)
    } else {
        Box(base.background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
            Icon(Icons.Filled.MusicNote, contentDescription=null, tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
fun albumArtUri(trackId: Long): Uri {
    val base = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    return ContentUris.withAppendedId(base, trackId)
}
