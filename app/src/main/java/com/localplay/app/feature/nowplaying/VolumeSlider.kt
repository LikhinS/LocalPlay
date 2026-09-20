package com.localplay.app.feature.nowplaying

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Volume slider that reads from and writes to the system's STREAM_MUSIC volume.
 *
 * Exynos 850 note: we read the current volume once on composition and on
 * each user drag, never polling in a loop. There's no background listener
 * here — the slider simply re-reads on next composition if the user changes
 * volume via hardware buttons while on this screen.
 */
@Composable
fun VolumeSlider(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    var volume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Filled.VolumeDown,
            contentDescription = "Volume low",
            modifier           = Modifier.size(20.dp),
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Slider(
            value         = volume,
            onValueChange = { newVol ->
                volume = newVol
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    newVol.toInt(),
                    0   // no UI flag — we draw our own slider
                )
            },
            valueRange = 0f..maxVolume,
            modifier   = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector        = Icons.Filled.VolumeUp,
            contentDescription = "Volume high",
            modifier           = Modifier.size(20.dp),
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
