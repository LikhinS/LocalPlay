package com.localplay.app.feature.permissions
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
val audioPermission: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
@Composable fun rememberAudioPermissionState(): MutableState<Boolean> {
    val context = LocalContext.current
    return remember { mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED) }
}
@Composable fun PermissionScreen(onPermissionGranted: () -> Unit) {
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) onPermissionGranted() else denied=true }
    Surface(modifier=Modifier.fillMaxSize()) {
        Column(modifier=Modifier.fillMaxSize().padding(32.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
            Text("LocalPlay needs access to your music", style=MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text("Reads only local audio files. No internet permission is used.", style=MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick={launcher.launch(audioPermission)}) { Text("Grant access") }
            if (denied) { Spacer(Modifier.height(16.dp)); Text("Grant later via Settings > Apps > LocalPlay > Permissions.", style=MaterialTheme.typography.bodySmall) }
        }
    }
}
