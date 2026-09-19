package com.localplay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localplay.app.feature.library.LibraryScanScreen
import com.localplay.app.feature.permissions.PermissionScreen
import com.localplay.app.feature.permissions.rememberAudioPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalPlayApp()
        }
    }
}

@Composable
fun LocalPlayApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val permissionGranted = rememberAudioPermissionState()

            if (permissionGranted.value) {
                // Phase 2: real scanner + Room debug list.
                // Replaced by full nav/tab UI in Phase 4.
                LibraryScanScreen()
            } else {
                PermissionScreen(
                    onPermissionGranted = { permissionGranted.value = true }
                )
            }
        }
    }
}
