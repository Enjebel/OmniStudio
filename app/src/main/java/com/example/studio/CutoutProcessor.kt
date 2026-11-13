package com.example.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.annotation.DrawableRes

enum class CutoutToolMode(val label: String) {
    AI_AUTO_CUTOUT("AI Magic Cutout"),
    ERASE_BRUSH("Erase Brush"),
    RESTORE_BRUSH("Restore Brush"),
    EDGE_REFINE("Edge Refine")
}

class CutoutProcessor {

    var activeTool: CutoutToolMode = CutoutToolMode.AI_AUTO_CUTOUT
    var brushSize: Float = 35f
    var brushSoftness: Float = 0.5f
    var brushOpacity: Float = 1.0f

    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private val historyStack = mutableListOf<Bitmap>()

    fun loadFromDrawable(context: Context, @DrawableRes drawableRes: Int): Bitmap? {
        val drawable = context.getDrawable(drawableRes) ?: return null
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, 512, 512)
        drawable.draw(canvas)

        originalBitmap = bmp
        workingBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true)
        historyStack.clear()
        saveState()
        return workingBitmap
    }

    private fun saveState() {
        workingBitmap?.let {
            if (historyStack.size >= 10) historyStack.removeAt(0)
            historyStack.add(it.copy(Bitmap.Config.ARGB_8888, true))
        }
    }

    fun canUndo(): Boolean = historyStack.size > 1

    fun undo(): Bitmap? {
        if (historyStack.size > 1) {
            historyStack.removeAt(historyStack.size - 1)
            workingBitmap = historyStack.last().copy(Bitmap.Config.ARGB_8888, true)
        }
        return workingBitmap
    }

    fun reset(): Bitmap? {
        originalBitmap?.let {
            workingBitmap = it.copy(Bitmap.Config.ARGB_8888, true)
            historyStack.clear()
            saveState()
        }
        return workingBitmap
    }

    /**
     * AI Auto-Cutout: Smart segmentation that samples corner background color
     * and clears background pixels within tolerance, smoothing edges.
     */
    fun performAiAutoCutout(): Bitmap? {
        val bmp = workingBitmap ?: return null
        saveState()

        val width = bmp.width
        val height = bmp.height
        val pixels = IntArray(width * height)
        bmp.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample corner background color
        val cornerColor = pixels[0]
        val bgRed = Color.red(cornerColor)
        val bgGreen = Color.green(cornerColor)
        val bgBlue = Color.blue(cornerColor)

        val tolerance = 38

        for (i in pixels.indices) {
            val c = pixels[i]
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)

            val diff = kotlin.math.sqrt(
                ((r - bgRed) * (r - bgRed) + (g - bgGreen) * (g - bgGreen) + (b - bgBlue) * (b - bgBlue)).toDouble()
            )

            if (diff < tolerance) {
                // Clear pixel to transparent
                pixels[i] = Color.TRANSPARENT
            } else if (diff < tolerance + 15) {
                // Soft edge blend
                val alpha = (((diff - tolerance) / 15.0) * 255).toInt().coerceIn(0, 255)
                pixels[i] = Color.argb(alpha, r, g, b)
            }
        }

        bmp.setPixels(pixels, 0, width, 0, 0, width, height)
        return bmp
    }

    /**
     * Apply brush stroke (Erase / Restore) at normalized coordinate (normX, normY)
     */
    fun applyBrushStroke(normX: Float, normY: Float) {
        val bmp = workingBitmap ?: return
        val canvas = Canvas(bmp)
        val x = normX * bmp.width
        val y = normY * bmp.height

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (activeTool) {
            CutoutToolMode.ERASE_BRUSH -> {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                canvas.drawCircle(x, y, brushSize, paint)
            }
            CutoutToolMode.RESTORE_BRUSH -> {
                originalBitmap?.let { orig ->
                    // Sample original pixel region and draw back
                    paint.alpha = (brushOpacity * 255).toInt()
                    val srcRect = android.graphics.Rect(
                        (x - brushSize).toInt().coerceAtLeast(0),
                        (y - brushSize).toInt().coerceAtLeast(0),
                        (x + brushSize).toInt().coerceAtMost(bmp.width),
                        (y + brushSize).toInt().coerceAtMost(bmp.height)
                    )
                    canvas.drawBitmap(orig, srcRect, srcRect, paint)
                }
            }
            CutoutToolMode.EDGE_REFINE -> {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                paint.alpha = (0.25f * 255).toInt()
                canvas.drawCircle(x, y, brushSize * 0.6f, paint)
            }
            CutoutToolMode.AI_AUTO_CUTOUT -> {}
        }
    }

    fun finishStroke() {
        saveState()
    }

    fun getCurrentBitmap(): Bitmap? = workingBitmap
}
