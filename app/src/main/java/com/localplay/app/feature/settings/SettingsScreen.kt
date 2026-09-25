package com.localplay.app.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.player.PlayerViewModel
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(playerViewModel: PlayerViewModel = viewModel()) {
    val config by playerViewModel.crossfadeConfig.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 24.dp))
        Text("PLAYBACK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Crossfade", style = MaterialTheme.typography.bodyLarge)
                Text(if (config.isEnabled) "Tracks overlap by ${config.crossfadeDurationMs / 1000}s" else "Off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = config.isEnabled, onCheckedChange = { playerViewModel.setCrossfadeDuration(if (it) 3_000L else 0L) })
        }
        if (config.isEnabled) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Duration", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${config.crossfadeDurationMs / 1000}s", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = config.crossfadeDurationMs / 1_000f, onValueChange = { playerViewModel.setCrossfadeDuration((it * 1_000).roundToLong()) }, valueRange = 1f..12f, steps = 10, modifier = Modifier.fillMaxWidth())
            Text("\"Mixing\" appears ~1.5s before tracks overlap.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.5.dp)
        Text("ABOUT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Text("LocalPlay — local files only. No internet permission.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text("Version 0.8.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
