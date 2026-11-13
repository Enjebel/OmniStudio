package com.example.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.BgRemovalMode
import com.example.model.BgReplacementType
import com.example.model.ChromaKeyColor
import kotlin.math.sqrt

data class LocalVideoMetadata(
    val title: String,
    val durationMs: Long,
    val widthPx: Int,
    val heightPx: Int
)

data class SegmentedFrameResult(
    val foregroundCutout: Bitmap,
    val compositedPreview: Bitmap
)

object VideoSegmentationEngine {

    fun inspectLocalVideo(context: Context, uri: Uri): LocalVideoMetadata {
        val title = resolveDisplayName(context, uri)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 5000L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1080
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1920
            LocalVideoMetadata(
                title = title,
                durationMs = duration.coerceAtLeast(500L),
                widthPx = width,
                heightPx = height
            )
        } catch (_: Exception) {
            LocalVideoMetadata(title = title, durationMs = 5000L, widthPx = 1080, heightPx = 1920)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun extractVideoFrame(context: Context, uri: Uri, timeMs: Long): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val raw = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
                ?: return null
            scaleForRealtimeProcessing(raw, 480)
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun decodeBackdropBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return null
                scaleForRealtimeProcessing(raw, 480)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Real-time pixel-level video frame background removal + compositing.
     * Supports:
     * 1) AI_SEGMENTATION: Multi-perimeter background color estimation + spatial foreground prior
     * 2) CHROMA_KEY: Screen color distance matte + chroma spill suppression
     */
    fun processFrame(
        sourceFrame: Bitmap,
        mode: BgRemovalMode,
        chromaKeyColor: ChromaKeyColor,
        sensitivity: Float,
        edgeFeather: Float,
        replacementType: BgReplacementType,
        solidColorArgb: Long,
        customBackdrop: Bitmap?
    ): SegmentedFrameResult {
        val w = sourceFrame.width
        val h = sourceFrame.height
        val srcPixels = IntArray(w * h)
        sourceFrame.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val fgPixels = IntArray(w * h)

        val tolerance = when (mode) {
            BgRemovalMode.AI_SEGMENTATION -> 30.0 + (sensitivity.coerceIn(0f, 1f) * 95.0)
            BgRemovalMode.CHROMA_KEY -> 35.0 + (sensitivity.coerceIn(0f, 1f) * 125.0)
        }
        val featherBand = 10.0 + (edgeFeather.coerceIn(0f, 1f) * 52.0)

        // Estimate key reference colors
        val refColors: List<Triple<Int, Int, Int>> = when (mode) {
            BgRemovalMode.CHROMA_KEY -> listOf(chromaKeyColor.rgbTriple)
            BgRemovalMode.AI_SEGMENTATION -> samplePerimeterColors(srcPixels, w, h)
        }

        val cx = w / 2.0f
        val cy = h / 2.0f

        for (y in 0 until h) {
            val dyNorm = ((y - cy) / (h * 0.45f))
            for (x in 0 until w) {
                val idx = y * w + x
                val px = srcPixels[idx]
                var r = Color.red(px)
                var g = Color.green(px)
                var b = Color.blue(px)

                var minDist = Double.MAX_VALUE
                for ((kr, kg, kb) in refColors) {
                    val dr = r - kr
                    val dg = g - kg
                    val db = b - kb
                    val dist = sqrt((dr * dr + dg * dg + db * db).toDouble())
                    if (dist < minDist) minDist = dist
                }

                // In AI Segmentation, give center subject region gentle foreground protection
                val effectiveDist = if (mode == BgRemovalMode.AI_SEGMENTATION) {
                    val dxNorm = ((x - cx) / (w * 0.36f))
                    val radialFromCenter = sqrt((dxNorm * dxNorm + dyNorm * dyNorm).toDouble())
                    if (radialFromCenter < 0.72) minDist + (0.72 - radialFromCenter) * 32.0 else minDist
                } else {
                    // Spill suppression on green/blue edges
                    if (chromaKeyColor == ChromaKeyColor.GREEN && g > r && g > b && minDist < tolerance + featherBand * 1.4) {
                        g = ((r + b) / 2).coerceIn(0, 255)
                    } else if (chromaKeyColor == ChromaKeyColor.BLUE && b > r && b > g && minDist < tolerance + featherBand * 1.4) {
                        b = ((r + g) / 2).coerceIn(0, 255)
                    }
                    minDist
                }

                fgPixels[idx] = when {
                    effectiveDist <= tolerance -> Color.TRANSPARENT
                    effectiveDist < tolerance + featherBand -> {
                        val alpha = (((effectiveDist - tolerance) / featherBand) * 255.0).toInt().coerceIn(0, 255)
                        Color.argb(alpha, r, g, b)
                    }
                    else -> Color.argb(255, r, g, b)
                }
            }
        }

        val foregroundCutout = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
            setPixels(fgPixels, 0, w, 0, 0, w, h)
        }

        val previewBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(previewBitmap)

        when (replacementType) {
            BgReplacementType.TRANSPARENT -> drawCheckerboard(canvas, w, h)
            BgReplacementType.SOLID_COLOR -> canvas.drawColor(solidColorArgb.toInt())
            BgReplacementType.BLUR_ORIGINAL -> {
                val blurred = fastBlurBackdrop(sourceFrame)
                canvas.drawBitmap(blurred, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.drawColor(Color.argb(45, 5, 8, 16))
            }
            BgReplacementType.CUSTOM_IMAGE -> {
                if (customBackdrop != null) {
                    canvas.drawBitmap(customBackdrop, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
                } else {
                    drawCheckerboard(canvas, w, h)
                }
            }
        }

        canvas.drawBitmap(foregroundCutout, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        return SegmentedFrameResult(
            foregroundCutout = foregroundCutout,
            compositedPreview = previewBitmap
        )
    }

    private fun samplePerimeterColors(pixels: IntArray, w: Int, h: Int): List<Triple<Int, Int, Int>> {
        if (w <= 2 || h <= 2) return listOf(Triple(0, 0, 0))
        val coords = listOf(
            2 to 2,
            (w / 2) to 2,
            (w - 3) to 2,
            2 to (h / 2),
            (w - 3) to (h / 2),
            2 to (h - 3),
            (w - 3) to (h - 3)
        )
        return coords.map { (x, y) ->
            val px = pixels[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)]
            Triple(Color.red(px), Color.green(px), Color.blue(px))
        }
    }

    private fun fastBlurBackdrop(source: Bitmap): Bitmap {
        val tinyW = (source.width / 10).coerceIn(16, 64)
        val tinyH = (source.height / 10).coerceIn(16, 64)
        val tiny = Bitmap.createScaledBitmap(source, tinyW, tinyH, true)
        return Bitmap.createScaledBitmap(tiny, source.width, source.height, true)
    }

    private fun drawCheckerboard(canvas: Canvas, width: Int, height: Int) {
        val cell = 24
        val light = Paint().apply { color = Color.parseColor("#2A3244") }
        val dark = Paint().apply { color = Color.parseColor("#181E2B") }
        var row = 0
        var y = 0
        while (y < height) {
            var col = 0
            var x = 0
            while (x < width) {
                val paint = if ((row + col) % 2 == 0) light else dark
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + cell).coerceAtMost(width).toFloat(),
                    (y + cell).coerceAtMost(height).toFloat(),
                    paint
                )
                x += cell
                col++
            }
            y += cell
            row++
        }
    }

    private fun scaleForRealtimeProcessing(src: Bitmap, maxDim: Int): Bitmap {
        if (src.width <= maxDim && src.height <= maxDim) {
            return if (src.config == Bitmap.Config.ARGB_8888) src else src.copy(Bitmap.Config.ARGB_8888, false)
        }
        val ratio = minOf(maxDim.toFloat() / src.width, maxDim.toFloat() / src.height)
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.takeIf { it.isNotBlank() }
                } else null
            } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Local Clip.mp4"
        } catch (_: Exception) {
            "Local Video.mp4"
        }
    }
}
