package com.pranshulgg.watchmaster.core.ui.components.media

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Extracts a stable, poster/backdrop-derived colour without adding another image-palette
 * dependency. The image is reduced to a small bitmap and quantized so a large flat area
 * contributes more strongly than tiny details.
 */
@Composable
fun rememberBackdropDominantColor(
    imageUrl: String?,
    fallback: Color
): Color {
    val context = LocalContext.current
    var color by remember(imageUrl, fallback) { mutableStateOf(fallback) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            color = fallback
            return@LaunchedEffect
        }

        val extracted = withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val drawable = ImageLoader(context).execute(request).drawable ?: return@runCatching null
                dominantColor(drawable.toBitmap())
            }.getOrNull()
        }

        color = extracted ?: fallback
    }

    return color
}

private fun dominantColor(bitmap: Bitmap): Color {
    val small = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
    val bins = HashMap<Int, Double>()

    for (y in 0 until small.height) {
        for (x in 0 until small.width) {
            val pixel = small.getPixel(x, y)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val maxChannel = max(r, max(g, b)) / 255f
            val minChannel = min(r, min(g, b)) / 255f
            val saturation = if (maxChannel == 0f) 0f else (maxChannel - minChannel) / maxChannel
            val luminance = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f

            // Ignore near-black/near-white pixels; they are usually borders, text, or glare.
            if (luminance < 0.06f || luminance > 0.97f) continue

            val qr = r shr 4
            val qg = g shr 4
            val qb = b shr 4
            val key = (qr shl 8) or (qg shl 4) or qb
            val weight = 1.0 + saturation * 1.5
            bins[key] = (bins[key] ?: 0.0) + weight
        }
    }

    val best = bins.maxByOrNull { it.value }?.key ?: return Color(0xFF202020)
    val r = ((best shr 8) and 0xF) * 17
    val g = ((best shr 4) and 0xF) * 17
    val b = (best and 0xF) * 17

    // Keep the detail background rich without becoming painfully bright.
    val maxValue = max(r, max(g, b))
    val scale = if (maxValue > 180) 180f / maxValue else 1f
    return Color(
        red = (r * scale / 255f).coerceIn(0f, 1f),
        green = (g * scale / 255f).coerceIn(0f, 1f),
        blue = (b * scale / 255f).coerceIn(0f, 1f),
        alpha = 1f
    )
}
