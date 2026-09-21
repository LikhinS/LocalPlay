package com.localplay.app.ui.components

import android.content.ContentUris
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

private val ALBUMART_BASE = Uri.parse("content://media/external/audio/albumart")

/**
 * Correct album art URI strategy:
 *
 *   content://media/external/audio/albumart/<albumId>
 *
 * This is the canonical MediaStore URI for embedded album art, supported
 * on API 1 through 35. It requires ALBUM_ID (not TRACK _ID), which we
 * now store in TrackEntity.albumId after the scanner fix.
 *
 * Passing albumId = 0 or null shows the placeholder — used for tracks
 * where MediaStore returned no album association.
 */
@Composable
fun AlbumArt(
    albumId: Long?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp,
    cornerDp: Dp = 6.dp,
    showShadow: Boolean = false
) {
    val context = LocalContext.current
    val shape   = RoundedCornerShape(cornerDp)
    val base    = modifier
        .size(sizeDp)
        .then(if (showShadow) Modifier.shadow(4.dp, shape) else Modifier)
        .clip(shape)

    if (albumId != null && albumId > 0L) {
        val artUri = ContentUris.withAppendedId(ALBUMART_BASE, albumId)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artUri)
                .size(
                    (sizeDp.value * 2).toInt(),
                    (sizeDp.value * 2).toInt()
                )
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = base
        )
    } else {
        ArtPlaceholder(base)
    }
}

@Composable
private fun ArtPlaceholder(modifier: Modifier) {
    Box(
        modifier         = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = Icons.Filled.MusicNote,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
