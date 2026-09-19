package com.localplay.app.ui.components

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Album art thumbnail for list rows and grids.
 *
 * Exynos 850 notes:
 * - [sizeDp] drives Coil's target size — we never ask Coil to decode
 *   a full-res image for a 48dp row icon. This keeps memory pressure low
 *   on 4 GB RAM and avoids large bitmap allocations during fast scroll.
 * - crossfade(true) is kept because it's a simple alpha blend (cheap on
 *   Mali-G52) and prevents jarring pop-in during scroll.
 * - We use MediaStore URIs (content://) rather than file paths so the
 *   system can serve embedded artwork without us reading raw bytes.
 */
@Composable
fun AlbumArt(
    trackId: Long?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp,
    cornerDp: Dp = 6.dp,
    showShadow: Boolean = false
) {
    val artUri = trackId?.let { albumArtUri(it) }

    val shape = RoundedCornerShape(cornerDp)
    val baseModifier = modifier
        .size(sizeDp)
        .clip(shape)
        .then(if (showShadow) Modifier.shadow(4.dp, shape) else Modifier)

    if (artUri != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artUri)
                .size(
                    (sizeDp.value * 2).toInt(),   // 2× for hi-dpi, no more
                    (sizeDp.value * 2).toInt()
                )
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = baseModifier,
            error = painterResource(android.R.drawable.ic_media_play),
            placeholder = null
        )
    } else {
        ArtPlaceholder(modifier = baseModifier)
    }
}

@Composable
private fun ArtPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(android.R.drawable.ic_media_play),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** MediaStore URI for embedded album art — works on API 26+ without file permission */
fun albumArtUri(trackId: Long): Uri {
    val baseUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    return ContentUris.withAppendedId(baseUri, trackId)
}
