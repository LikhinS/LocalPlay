package com.localplay.app.ui.gradient

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated gradient background derived from the current track's album art.
 *
 * Design for Exynos 850 / Mali-G52:
 *
 * 1. Palette extraction runs once per albumId change on Dispatchers.IO,
 *    on a 24×24 px downsampled bitmap — essentially free.
 * 2. The gradient itself is a precomputed Brush built from the swatches.
 *    It is only rebuilt when the track changes — not per frame.
 * 3. Per-frame animation is a single Float (angle) from
 *    rememberInfiniteTransition that rotates the two radial gradient
 *    anchor points slowly. The only GPU work per frame is one
 *    drawBehind call with two radial gradients — no blur, no shader,
 *    no offscreen compositing.
 * 4. Color cross-fade between tracks uses animateColorAsState (4 colors,
 *    400ms) — cheap alpha blending, not a re-extraction.
 * 5. Animation freezes automatically when the app is backgrounded because
 *    rememberInfiniteTransition pauses when the composable leaves the
 *    composition.
 */
@Composable
fun ArtworkGradientBackground(
    albumId: Long?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Extracted palette colors — updated once per track
    var paletteColors by remember { mutableStateOf(defaultGradientColors()) }

    // Extract palette whenever albumId changes
    LaunchedEffect(albumId) {
        if (albumId == null || albumId <= 0L) {
            paletteColors = defaultGradientColors()
            return@LaunchedEffect
        }
        val extracted = extractPaletteColors(context, albumId)
        if (extracted != null) paletteColors = extracted
    }

    // Animate each of the 3 gradient colors independently so they
    // cross-fade smoothly when the track changes (400ms)
    val color0 by animateColorAsState(
        targetValue   = paletteColors[0],
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label         = "grad0"
    )
    val color1 by animateColorAsState(
        targetValue   = paletteColors[1],
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label         = "grad1"
    )
    val color2 by animateColorAsState(
        targetValue   = paletteColors[2],
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label         = "grad2"
    )
    val darkBase by animateColorAsState(
        targetValue   = paletteColors[3],
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label         = "gradBase"
    )

    // Single slowly-rotating angle — the only per-frame update
    val transition = rememberInfiniteTransition(label = "gradient_rotation")
    val angle by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grad_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Fill with dark base first
                drawRect(color = darkBase)

                val angleRad = Math.toRadians(angle.toDouble()).toFloat()
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r  = size.width * 0.65f

                // Blob 1 — top-left quadrant, rotates clockwise
                val x1 = cx + r * cos(angleRad)
                val y1 = cy + r * sin(angleRad) * 0.6f

                drawCircle(
                    brush  = Brush.radialGradient(
                        colors  = listOf(color0.copy(alpha = 0.75f), Color.Transparent),
                        center  = Offset(x1, y1),
                        radius  = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(x1, y1),
                    alpha  = 1f
                )

                // Blob 2 — bottom-right quadrant, rotates counter-clockwise
                val x2 = cx - r * cos(angleRad) * 0.8f
                val y2 = cy - r * sin(angleRad) * 0.5f + size.height * 0.3f

                drawCircle(
                    brush  = Brush.radialGradient(
                        colors  = listOf(color1.copy(alpha = 0.65f), Color.Transparent),
                        center  = Offset(x2, y2),
                        radius  = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(x2, y2),
                    alpha  = 1f
                )

                // Blob 3 — accent, slower drift
                val x3 = cx + r * 0.4f * cos(angleRad * 0.5f + 1f)
                val y3 = cy * 1.6f + r * 0.3f * sin(angleRad * 0.5f)

                drawCircle(
                    brush  = Brush.radialGradient(
                        colors  = listOf(color2.copy(alpha = 0.45f), Color.Transparent),
                        center  = Offset(x3, y3),
                        radius  = size.width * 0.5f
                    ),
                    radius = size.width * 0.5f,
                    center = Offset(x3, y3),
                    alpha  = 1f
                )
            }
    ) {
        content()
    }
}

// ── Palette extraction ────────────────────────────────────────────────────

private data class GradientColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val base: Color
)

private fun defaultGradientColors() = listOf(
    Color(0xFF1A1A2E),
    Color(0xFF16213E),
    Color(0xFF0F3460),
    Color(0xFF0A0A0F)
)

/**
 * Loads the album art at 24×24 px, runs Palette, and returns 4 colors:
 * [primary, secondary, accent, dark base].
 *
 * Runs on IO dispatcher — never blocks the main thread.
 * Returns null if extraction fails — caller keeps previous colors.
 */
private suspend fun extractPaletteColors(
    context: Context,
    albumId: Long
): List<Color>? = withContext(Dispatchers.IO) {
    try {
        val uri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )

        // Use Coil to load a tiny thumbnail — it handles caching so
        // repeated calls for the same albumId cost nothing
        val loader  = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(24, 24)   // tiny — we only need representative colors
            .allowHardware(false)   // Palette requires software bitmap
            .build()

        val result = loader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable
            ?.let { (it as? android.graphics.drawable.BitmapDrawable)?.bitmap }
            ?: return@withContext null

        val palette = Palette.from(bitmap).generate()

        // Pick the best available swatches in priority order
        val vibrant     = palette.vibrantSwatch
        val darkVibrant = palette.darkVibrantSwatch
        val muted       = palette.mutedSwatch
        val darkMuted   = palette.darkMutedSwatch
        val dominant    = palette.dominantSwatch

        // Primary: vibrant > dominant > muted
        val primary = (vibrant ?: dominant ?: muted)
            ?.rgb?.let { Color(it) } ?: Color(0xFF1A1A2E)

        // Secondary: darkVibrant > muted > darkMuted
        val secondary = (darkVibrant ?: muted ?: darkMuted)
            ?.rgb?.let { Color(it) } ?: Color(0xFF16213E)

        // Accent: muted > darkMuted > vibrant
        val accent = (muted ?: darkMuted ?: vibrant)
            ?.rgb?.let { Color(it) } ?: Color(0xFF0F3460)

        // Dark base: always very dark version of the dominant color
        val base = (darkMuted ?: darkVibrant)
            ?.rgb?.let { Color(it) }?.darken(0.55f) ?: Color(0xFF070710)

        listOf(primary, secondary, accent, base)
    } catch (e: Exception) {
        null
    }
}

/** Darken a color by [factor] (0 = black, 1 = original). */
private fun Color.darken(factor: Float): Color = Color(
    red   = red   * factor,
    green = green * factor,
    blue  = blue  * factor,
    alpha = alpha
)
