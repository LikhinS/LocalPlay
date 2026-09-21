package com.localplay.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(playerViewModel: PlayerViewModel = viewModel()) {

    val config by playerViewModel.crossfadeConfig.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "Settings",
            style    = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ── Crossfade section ────────────────────────────────────────────
        Text(
            "PLAYBACK",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier          = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Crossfade", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (config.isEnabled)
                        "Tracks overlap by ${config.crossfadeDurationMs / 1000}s"
                    else
                        "Off — tracks play back to back",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked         = config.isEnabled,
                onCheckedChange = { on ->
                    playerViewModel.setCrossfadeDuration(if (on) 3_000L else 0L)
                }
            )
        }

        if (config.isEnabled) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Duration",
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${config.crossfadeDurationMs / 1000}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value         = config.crossfadeDurationMs / 1_000f,
                onValueChange = { secs ->
                    playerViewModel.setCrossfadeDuration((secs * 1_000).roundToLong())
                },
                valueRange = 1f..12f,
                steps      = 10,          // 1s increments
                modifier   = Modifier.fillMaxWidth()
            )
            Text(
                "The \"Mixing\" label appears about 1.5 seconds before tracks overlap.",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        HorizontalDivider(
            modifier  = Modifier.padding(vertical = 16.dp),
            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            thickness = 0.5.dp
        )

        // ── Info section ─────────────────────────────────────────────────
        Text(
            "ABOUT",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "LocalPlay — local files only. No internet permission.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Version 0.6.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
