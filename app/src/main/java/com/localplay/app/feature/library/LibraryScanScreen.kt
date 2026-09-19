package com.localplay.app.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplay.app.core.database.entity.TrackEntity

/**
 * Phase 2 debug screen — shows scan state and a raw track list.
 * This will be replaced by the real Apple Music-style library UI in Phase 4.
 * Keeping it simple here on purpose: no images, no fancy layout —
 * just proof that the scanner and Room are working correctly.
 */
@Composable
fun LibraryScanScreen(
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val scanState by libraryViewModel.scanState.collectAsState()
    val allTracks by libraryViewModel.allTracks.collectAsState()

    // Kick off a scan as soon as this screen is first composed.
    // LaunchedEffect with Unit key means it runs exactly once per composition.
    LaunchedEffect(Unit) {
        libraryViewModel.scanLibrary()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = scanState) {
            is LibraryViewModel.ScanState.Idle -> {
                CenteredMessage("Preparing scan…")
            }

            is LibraryViewModel.ScanState.Scanning -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning your library…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            is LibraryViewModel.ScanState.Error -> {
                CenteredMessage("Scan error: ${state.message}")
            }

            is LibraryViewModel.ScanState.Done -> {
                TrackDebugList(
                    trackCount = state.trackCount,
                    tracks = allTracks
                )
            }
        }
    }
}

@Composable
private fun TrackDebugList(
    trackCount: Int,
    tracks: List<TrackEntity>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Found $trackCount tracks  ✓",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = tracks,
                // Stable key = no unnecessary recomposition on scroll
                key = { it.id }
            ) { track ->
                TrackDebugRow(track)
            }
        }
    }
}

@Composable
private fun TrackDebugRow(track: TrackEntity) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
        Text(
            text = buildString {
                append(track.artist)
                append(" · ")
                append(track.album)
                if (track.isLossless) append(" · Lossless")
                if (track.isAtmos)    append(" · Atmos/Surround")
            },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
