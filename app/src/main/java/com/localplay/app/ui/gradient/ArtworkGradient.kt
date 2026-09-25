package com.localplay.app.ui.gradient

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArtworkGradientBackground(
    albumId: Long?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var paletteColors by remember { mutableStateOf(defaultColors()) }

    LaunchedEffect(albumId) {
        if (albumId == null || albumId <= 0L) { paletteColors = defaultColors(); return@LaunchedEffect }
        extractPaletteColors(context, albumId)?.let { paletteColors = it }
    }

    val c0 by animateColorAsState(paletteColors[0], tween(400, easing = FastOutSlowInEasing), label = "c0")
    val c1 by animateColorAsState(paletteColors[1], tween(400, easing = FastOutSlowInEasing), label = "c1")
    val c2 by animateColorAsState(paletteColors[2], tween(400, easing = FastOutSlowInEasing), label = "c2")
    val base by animateColorAsState(paletteColors[3], tween(400, easing = FastOutSlowInEasing), label = "cb")

    val transition = rememberInfiniteTransition(label = "grad_rot")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )

    Box(
        modifier = modifier.fillMaxSize().drawBehind {
            drawRect(color = base)
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val cx = size.width / 2f; val cy = size.height / 2f; val r = size.width * 0.65f
            val x1 = cx + r * cos(rad); val y1 = cy + r * sin(rad) * 0.6f
            drawCircle(Brush.radialGradient(listOf(c0.copy(alpha = 0.75f), Color.Transparent), Offset(x1, y1), size.width * 0.7f), size.width * 0.7f, Offset(x1, y1))
            val x2 = cx - r * cos(rad) * 0.8f; val y2 = cy - r * sin(rad) * 0.5f + size.height * 0.3f
            drawCircle(Brush.radialGradient(listOf(c1.copy(alpha = 0.65f), Color.Transparent), Offset(x2, y2), size.width * 0.65f), size.width * 0.65f, Offset(x2, y2))
            val x3 = cx + r * 0.4f * cos(rad * 0.5f + 1f); val y3 = cy * 1.6f + r * 0.3f * sin(rad * 0.5f)
            drawCircle(Brush.radialGradient(listOf(c2.copy(alpha = 0.45f), Color.Transparent), Offset(x3, y3), size.width * 0.5f), size.width * 0.5f, Offset(x3, y3))
        }
    ) { content() }
}

private fun defaultColors() = listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460), Color(0xFF070710))

private suspend fun extractPaletteColors(context: Context, albumId: Long): List<Color>? = withContext(Dispatchers.IO) {
    try {
        val uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
        val loader = ImageLoader(context)
        val result = loader.execute(ImageRequest.Builder(context).data(uri).size(24, 24).allowHardware(false).build())
        val bitmap = (result as? SuccessResult)?.drawable?.let { (it as? android.graphics.drawable.BitmapDrawable)?.bitmap } ?: return@withContext null
        val palette = Palette.from(bitmap).generate()
        val vibrant = palette.vibrantSwatch; val darkVibrant = palette.darkVibrantSwatch
        val muted = palette.mutedSwatch; val darkMuted = palette.darkMutedSwatch
        val dominant = palette.dominantSwatch
        val primary = (vibrant ?: dominant ?: muted)?.rgb?.let { Color(it) } ?: Color(0xFF1A1A2E)
        val secondary = (darkVibrant ?: muted ?: darkMuted)?.rgb?.let { Color(it) } ?: Color(0xFF16213E)
        val accent = (muted ?: darkMuted ?: vibrant)?.rgb?.let { Color(it) } ?: Color(0xFF0F3460)
        val base = (darkMuted ?: darkVibrant)?.rgb?.let { Color(it) }?.let {
            Color(it.red * 0.55f, it.green * 0.55f, it.blue * 0.55f, it.alpha)
        } ?: Color(0xFF070710)
        listOf(primary, secondary, accent, base)
    } catch (e: Exception) { null }
}
