package com.example.studio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.AspectRatioOption
import com.example.model.BackgroundPreset
import com.example.model.CanvasLayer
import com.example.model.LayerType
import com.example.model.LightingSettings
import com.example.model.isWatermarkFreeTier
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.cos
import kotlin.math.sin

object ExportEngine {

    enum class ExportFormat(val extension: String, val mimeType: String) {
        PNG("png", "image/png"),
        JPG("jpg", "image/jpeg"),
        WEBP("webp", "image/webp")
    }

    fun renderCanvasToBitmap(
        context: Context,
        layers: List<CanvasLayer>,
        bgPreset: BackgroundPreset,
        lighting: LightingSettings,
        aspectRatio: AspectRatioOption,
        subscriptionTier: String = "FREE",
        targetWidth: Int = aspectRatio.widthPx,
        targetHeight: Int = aspectRatio.heightPx
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background Preset
        drawBackground(canvas, bgPreset, targetWidth, targetHeight)

        // 2. Draw Floor Reflection if any product layer
        val sortedLayers = layers.filter { it.isVisible }.sortedBy { it.zIndex }
        val productLayer = sortedLayers.firstOrNull { it.type == LayerType.PRODUCT }

        productLayer?.let { prod ->
            drawReflection(context, canvas, prod, lighting, targetWidth, targetHeight)
        }

        // 3. Draw sorted layers
        for (layer in sortedLayers) {
            drawLayer(context, canvas, layer, lighting, targetWidth, targetHeight)
        }

        // 4. Composite Tiered Watermark for non-Pro/Premium users
        if (!isWatermarkFreeTier(subscriptionTier)) {
            drawWatermark(canvas, targetWidth, targetHeight)
        }

        return bitmap
    }

    private fun drawBackground(canvas: Canvas, preset: BackgroundPreset, width: Int, height: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (preset) {
            BackgroundPreset.LUXURY_MARBLE -> {
                val shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.parseColor("#0B0C10"), Color.parseColor("#1B1F2A"), Color.parseColor("#0D0E15")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Marble veins
                val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#22D4AF37")
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(0f, height * 0.2f, width.toFloat(), height * 0.45f, veinPaint)
                canvas.drawLine(0f, height * 0.65f, width.toFloat(), height * 0.8f, veinPaint)
            }
            BackgroundPreset.WHITE_STUDIO -> {
                val shader = LinearGradient(
                    width * 0.5f, 0f, width * 0.5f, height.toFloat(),
                    intArrayOf(Color.parseColor("#2E3348"), Color.parseColor("#171A26"), Color.parseColor("#0F111A")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            BackgroundPreset.CYBERPUNK_NEON -> {
                val shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.parseColor("#050510"), Color.parseColor("#1E0638"), Color.parseColor("#021E2F")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Neon grid
                val gridPaint = Paint().apply {
                    color = Color.parseColor("#2600F0FF")
                    strokeWidth = 2f
                }
                for (y in 0 until height step (height / 20)) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
                }
            }
            BackgroundPreset.FASHION_RUNWAY -> {
                val shader = LinearGradient(
                    width * 0.5f, 0f, width * 0.5f, height.toFloat(),
                    intArrayOf(Color.parseColor("#2B0038"), Color.parseColor("#0F0214"), Color.parseColor("#000000")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            BackgroundPreset.GOLDEN_LUXURY -> {
                val shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.parseColor("#241A04"), Color.parseColor("#120E03"), Color.parseColor("#000000")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            BackgroundPreset.TECH_GRID -> {
                paint.color = Color.parseColor("#080D1A")
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                val gridPaint = Paint().apply {
                    color = Color.parseColor("#1A00F0FF")
                    strokeWidth = 1.5f
                }
                for (x in 0 until width step (width / 16)) {
                    canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
                }
                for (y in 0 until height step (height / 24)) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
                }
            }
            BackgroundPreset.LUXURY_BLACK_GOLD -> {
                paint.color = Color.parseColor("#08080C")
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                val goldBorder = Paint().apply {
                    color = Color.parseColor("#44D4AF37")
                    strokeWidth = 6f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(30f, 30f, width - 30f, height - 30f, goldBorder)
            }
            else -> {
                paint.color = Color.parseColor("#0D0E15")
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
    }

    private fun drawReflection(
        context: Context,
        canvas: Canvas,
        prodLayer: CanvasLayer,
        lighting: LightingSettings,
        targetWidth: Int,
        targetHeight: Int
    ) {
        try {
            val drawable = context.getDrawable(prodLayer.productDrawableRes) ?: return
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val dCanvas = Canvas(bitmap)
            drawable.setBounds(0, 0, 512, 512)
            drawable.draw(dCanvas)

            val centerX = prodLayer.x * targetWidth
            val centerY = prodLayer.y * targetHeight
            val size = (targetWidth * 0.65f * prodLayer.scale)

            canvas.save()
            // Invert vertically for reflection
            canvas.scale(1f, -1f, centerX, centerY + size * 0.45f)

            val reflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (lighting.reflectionOpacity * 140).toInt().coerceIn(0, 255)
            }

            val destRect = RectF(
                centerX - size / 2,
                centerY + size * 0.45f - size / 2,
                centerX + size / 2,
                centerY + size * 0.45f + size / 2
            )
            canvas.drawBitmap(bitmap, null, destRect, reflectionPaint)
            canvas.restore()
        } catch (_: Exception) {}
    }

    private fun drawLayer(
        context: Context,
        canvas: Canvas,
        layer: CanvasLayer,
        lighting: LightingSettings,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val centerX = layer.x * targetWidth
        val centerY = layer.y * targetHeight

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(layer.rotation)
        canvas.scale(layer.scale, layer.scale)

        when (layer.type) {
            LayerType.PRODUCT -> {
                try {
                    val size = (targetWidth * 0.65f).toInt()
                    val half = size / 2

                    // Drop shadow
                    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        alpha = (lighting.shadowOpacity * 200).toInt()
                    }
                    val angleRad = Math.toRadians(lighting.shadowAngle.toDouble())
                    val dx = (lighting.shadowDistance * cos(angleRad)).toFloat()
                    val dy = (lighting.shadowDistance * sin(angleRad)).toFloat()
                    canvas.drawOval(
                        RectF(-half * 0.8f + dx, half * 0.7f + dy, half * 0.8f + dx, half * 0.95f + dy),
                        shadowPaint
                    )

                    // Draw image: AI bitmap, custom image from gallery, or bundled drawable
                    val customBmp = layer.aiBitmap ?: if (!layer.customImageUri.isNullOrBlank()) {
                        try {
                            val uri = Uri.parse(layer.customImageUri)
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                android.graphics.BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) { null }
                    } else null

                    if (customBmp != null) {
                        canvas.drawBitmap(customBmp, null, RectF(-half.toFloat(), -half.toFloat(), half.toFloat(), half.toFloat()), null)
                    } else {
                        val drawable = context.getDrawable(layer.productDrawableRes)
                        drawable?.let { d ->
                            d.setBounds(-half, -half, half, half)
                            d.draw(canvas)
                        }
                    }
                } catch (_: Exception) {}
            }
            LayerType.TEXT -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.textColor.toInt()
                    textSize = layer.fontSizeSp * (targetWidth / 400f)
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    letterSpacing = layer.letterSpacingSp / 10f
                }

                if (layer.isShadow) {
                    textPaint.setShadowLayer(16f, 0f, 6f, Color.BLACK)
                }

                if (layer.isGradientText) {
                    val bounds = Rect()
                    textPaint.getTextBounds(layer.text, 0, layer.text.length, bounds)
                    val shader = LinearGradient(
                        bounds.left.toFloat(), 0f, bounds.right.toFloat(), 0f,
                        layer.gradientColorStart.toInt(), layer.gradientColorEnd.toInt(),
                        Shader.TileMode.CLAMP
                    )
                    textPaint.shader = shader
                }

                canvas.drawText(layer.text, 0f, 0f, textPaint)
            }
            LayerType.BADGE -> {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.badgeAccentColor.toInt()
                    style = Paint.Style.FILL
                }
                val w = targetWidth * 0.35f
                val h = 48f * (targetWidth / 400f)
                val rect = RectF(-w / 2, -h / 2, w / 2, h / 2)
                canvas.drawRoundRect(rect, h / 2, h / 2, badgePaint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 20f * (targetWidth / 400f)
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                canvas.drawText(layer.badgeLabel, 0f, h * 0.22f, textPaint)
            }
            else -> {}
        }

        canvas.restore()
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, format: ExportFormat, quality: Int): Uri? {
        val filename = "OmniStudio_${System.currentTimeMillis()}.${format.extension}"
        var outputStream: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, format.mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OmniStudio")
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    outputStream = resolver.openOutputStream(uri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/OmniStudio"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val imageFile = File(file, filename)
                outputStream = FileOutputStream(imageFile)
                imageUri = Uri.fromFile(imageFile)
            }

            outputStream?.use { out ->
                val compressFormat = when (format) {
                    ExportFormat.PNG -> Bitmap.CompressFormat.PNG
                    ExportFormat.JPG -> Bitmap.CompressFormat.JPEG
                    ExportFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }
                }
                bitmap.compress(compressFormat, quality.coerceIn(50, 100), out)
            }
        } catch (_: Exception) {
            imageUri = null
        }

        return imageUri
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, caption: String = "Created with OmniStudio AI") {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "omnistudio_export_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, "image/png")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share OmniStudio Creation"))
            }
        } catch (_: Exception) {}
    }

    private fun drawWatermark(canvas: Canvas, width: Int, height: Int) {
        val pad = width * 0.035f
        val text = "OmniStudio AI Free | ENJEBEL TECH"
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 220
            textSize = (width * 0.026f).coerceIn(20f, 44f)
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E605070D")
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5500F0FF")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val textBounds = Rect()
        paint.getTextBounds(text, 0, text.length, textBounds)
        val rectW = textBounds.width() + pad * 1.8f
        val rectH = textBounds.height() + pad * 1.2f
        val right = width - pad
        val bottom = height - pad
        val left = right - rectW
        val top = bottom - rectH

        val rectF = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rectF, 12f, 12f, bgPaint)
        canvas.drawRoundRect(rectF, 12f, 12f, strokePaint)

        // Draw neon indicator dot
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
        }
        canvas.drawCircle(left + pad * 0.7f, top + rectH / 2f, 7f, dotPaint)

        canvas.drawText(text, right - pad * 0.5f, top + rectH / 2f + textBounds.height() / 2.5f, paint)
    }
}
