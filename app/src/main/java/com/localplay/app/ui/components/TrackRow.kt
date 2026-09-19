package com.localplay.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localplay.app.core.database.entity.TrackEntity

/**
 * Standard track list row matching Apple Music's layout:
 * [album art 48dp] [title / artist · album] [duration]
 *
 * Kept deliberately shallow (one Row, one Column) to minimise layout
 * passes during fast scroll on Exynos 850. No nested lazy layouts.
 */
@Composable
fun TrackRow(
    track: TrackEntity,
    isNowPlaying: Boolean = false,
    showAlbumArt: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isNowPlaying)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAlbumArt) {
                AlbumArt(
                    trackId = track.id,
                    sizeDp  = 48.dp,
                    cornerDp = 6.dp
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = track.title,
                    style    = MaterialTheme.typography.bodyLarge,
                    color    = primaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(track.artist)
                        if (track.album.isNotBlank()) {
                            append(" — ")
                            append(track.album)
                        }
                    },
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Duration (mm:ss)
            Text(
                text  = formatDuration(track.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier    = Modifier.padding(start = if (showAlbumArt) 76.dp else 16.dp),
            color       = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            thickness   = 0.5.dp
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
