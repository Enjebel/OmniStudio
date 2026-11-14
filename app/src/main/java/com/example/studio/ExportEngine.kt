package com.example.studio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.AspectRatioOption
import com.example.model.BackgroundPreset
import com.example.model.CanvasLayer
import com.example.model.LayerType
import com.example.model.LightingSettings
import com.example.model.MotionTemplate
import com.example.model.TimelineClip
import com.example.model.TimelineTrackType
import com.example.model.TransitionEffect
import com.example.model.isWatermarkFreeTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object ExportEngine {

    private const val TAG = "ExportEngine"

    enum class ExportFormat(val extension: String, val mimeType: String) {
        PNG("png", "image/png"),
        JPG("jpg", "image/jpeg"),
        WEBP("webp", "image/webp")
    }

    data class MotionFrameTransform(
        val scale: Float = 1.0f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val rotationDeg: Float = 0f,
        val alpha: Float = 1.0f
    )

    data class TransitionFrameTransform(
        val effect: TransitionEffect = TransitionEffect.NONE,
        val progress: Float = 1f,
        val scale: Float = 1.0f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val rotationDeg: Float = 0f,
        val alpha: Float = 1.0f,
        val flashAlpha: Float = 0f
    )

    data class VideoExportSpec(
        val widthPx: Int,
        val heightPx: Int,
        val fps: Int = 30,
        val durationSec: Float = 5.0f,
        val bitrateBps: Int = 4_500_000
    )

    fun alignVideoDimensions(
        aspectRatio: AspectRatioOption,
        maxShortEdgePx: Int = 720
    ): Pair<Int, Int> {
        val clampedShort = maxShortEdgePx.coerceIn(256, 1080)
        val rawW: Float
        val rawH: Float
        if (aspectRatio.widthPx <= aspectRatio.heightPx) {
            rawW = clampedShort.toFloat()
            rawH = clampedShort.toFloat() / aspectRatio.ratio
        } else {
            rawH = clampedShort.toFloat()
            rawW = clampedShort.toFloat() * aspectRatio.ratio
        }
        val alignedW = ((rawW.roundToInt() + 8) / 16 * 16).coerceAtLeast(128)
        val alignedH = ((rawH.roundToInt() + 8) / 16 * 16).coerceAtLeast(128)
        return alignedW to alignedH
    }

    fun computeMotionFrameTransform(
        motionTemplate: MotionTemplate,
        timeSec: Float,
        motionSpeed: Float,
        motionIntensity: Float,
        targetWidth: Int
    ): MotionFrameTransform {
        val time = timeSec * motionSpeed
        val unitScale = (targetWidth / 1080f).coerceAtLeast(0.4f)
        return when (motionTemplate) {
            MotionTemplate.DYNAMIC_ZOOM_PUNCH -> {
                val punchCycle = time % 1.5f
                val isPunch = punchCycle < 0.25f
                MotionFrameTransform(
                    scale = if (isPunch) 1.0f + (0.20f * motionIntensity) else 1.0f + 0.03f * sin(time * 2.5).toFloat(),
                    offsetX = if (isPunch) (sin(time * 40.0) * 14f * motionIntensity * unitScale).toFloat() else 0f,
                    offsetY = if (isPunch) (cos(time * 40.0) * 14f * motionIntensity * unitScale).toFloat() else 0f
                )
            }
            MotionTemplate.ORBIT_3D -> MotionFrameTransform(
                scale = 1.0f + 0.04f * cos(time * 1.5).toFloat(),
                offsetX = (cos(time * 2.0) * 28f * motionIntensity * unitScale).toFloat(),
                offsetY = (sin(time * 2.0) * 18f * motionIntensity * unitScale).toFloat(),
                rotationDeg = (sin(time * 1.5) * 6f * motionIntensity).toFloat()
            )
            MotionTemplate.FLOATING_LUXURY -> MotionFrameTransform(
                offsetY = (sin(time * 1.8) * 24f * motionIntensity * unitScale).toFloat(),
                rotationDeg = (cos(time * 1.2) * 3f * motionIntensity).toFloat()
            )
            MotionTemplate.FLASH_SALE_GLITCH -> {
                val isFlash = ((time * 12f).toInt() % 4 == 0)
                if (isFlash) {
                    MotionFrameTransform(
                        scale = 1.0f + (0.08f * motionIntensity),
                        offsetX = (sin(time * 73.0) * 18f * motionIntensity * unitScale).toFloat(),
                        offsetY = (cos(time * 59.0) * 10f * motionIntensity * unitScale).toFloat()
                    )
                } else {
                    MotionFrameTransform()
                }
            }
            MotionTemplate.CINEMATIC_REVEAL -> {
                val progress = (time / 2.0f).coerceIn(0.15f, 1f)
                MotionFrameTransform(
                    scale = 0.9f + (0.1f * progress),
                    alpha = progress
                )
            }
        }
    }

    fun computeClipTransitionTransform(
        clips: List<TimelineClip>,
        timeSec: Float,
        targetWidth: Float,
        targetHeight: Float,
        trackFilter: Set<TimelineTrackType> = setOf(TimelineTrackType.PRODUCT, TimelineTrackType.TEXT)
    ): TransitionFrameTransform {
        val activeWithTransition = clips
            .filter {
                it.trackType in trackFilter &&
                    it.transitionIn != TransitionEffect.NONE &&
                    it.transitionDurationSec > 0.05f &&
                    timeSec >= it.startTimeSec &&
                    timeSec <= (it.startTimeSec + it.transitionDurationSec)
            }
            .maxByOrNull { it.startTimeSec }
            ?: return TransitionFrameTransform()

        val rawProgress = ((timeSec - activeWithTransition.startTimeSec) / activeWithTransition.transitionDurationSec)
            .coerceIn(0f, 1f)
        // Smooth cubic ease-out for enterprise CapCut-grade motion
        val inv = 1f - rawProgress
        val eased = 1f - (inv * inv * inv)

        return when (activeWithTransition.transitionIn) {
            TransitionEffect.NONE -> TransitionFrameTransform()
            TransitionEffect.FADE -> TransitionFrameTransform(
                effect = TransitionEffect.FADE,
                progress = rawProgress,
                alpha = eased.coerceIn(0.05f, 1f)
            )
            TransitionEffect.SLIDE_LEFT -> TransitionFrameTransform(
                effect = TransitionEffect.SLIDE_LEFT,
                progress = rawProgress,
                offsetX = (1f - eased) * targetWidth * 0.65f,
                alpha = (0.35f + 0.65f * eased).coerceIn(0f, 1f)
            )
            TransitionEffect.SLIDE_UP -> TransitionFrameTransform(
                effect = TransitionEffect.SLIDE_UP,
                progress = rawProgress,
                offsetY = (1f - eased) * targetHeight * 0.5f,
                alpha = (0.35f + 0.65f * eased).coerceIn(0f, 1f)
            )
            TransitionEffect.ZOOM_IN -> TransitionFrameTransform(
                effect = TransitionEffect.ZOOM_IN,
                progress = rawProgress,
                scale = 0.55f + (0.45f * eased),
                alpha = (0.25f + 0.75f * eased).coerceIn(0f, 1f)
            )
            TransitionEffect.WHIP_PAN -> TransitionFrameTransform(
                effect = TransitionEffect.WHIP_PAN,
                progress = rawProgress,
                offsetX = -(1f - eased) * targetWidth * 0.75f,
                rotationDeg = (1f - eased) * -10f,
                scale = 1f + (1f - eased) * 0.14f,
                alpha = (0.4f + 0.6f * eased).coerceIn(0f, 1f)
            )
            TransitionEffect.FLASH_CUT -> TransitionFrameTransform(
                effect = TransitionEffect.FLASH_CUT,
                progress = rawProgress,
                scale = 1.0f + (1f - eased) * 0.12f,
                flashAlpha = ((1f - rawProgress) * 0.78f).coerceIn(0f, 0.78f)
            )
        }
    }

    private fun preloadLayerBitmaps(context: Context, layers: List<CanvasLayer>): Map<String, Bitmap> {
        val map = mutableMapOf<String, Bitmap>()
        for (layer in layers) {
            if (layer.type != LayerType.PRODUCT) continue
            val bmp = layer.aiBitmap ?: if (!layer.customImageUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(layer.customImageUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            } ?: try {
                val drawable = context.getDrawable(layer.productDrawableRes)
                if (drawable != null) {
                    Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).also { out ->
                        val c = Canvas(out)
                        drawable.setBounds(0, 0, 512, 512)
                        drawable.draw(c)
                    }
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }

            if (bmp != null) {
                map[layer.id] = bmp
            }
        }
        return map
    }

    fun renderCanvasToBitmap(
        context: Context,
        layers: List<CanvasLayer>,
        bgPreset: BackgroundPreset,
        lighting: LightingSettings,
        aspectRatio: AspectRatioOption,
        subscriptionTier: String = "FREE",
        motionTemplate: MotionTemplate = MotionTemplate.DYNAMIC_ZOOM_PUNCH,
        motionSpeed: Float = 1.0f,
        motionIntensity: Float = 1.0f,
        timeSec: Float = 0f,
        targetWidth: Int = aspectRatio.widthPx,
        targetHeight: Int = aspectRatio.heightPx
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cachedBitmaps = preloadLayerBitmaps(context, layers)

        renderFrameToCanvas(
            canvas = canvas,
            layers = layers,
            cachedProductBitmaps = cachedBitmaps,
            bgPreset = bgPreset,
            lighting = lighting,
            subscriptionTier = subscriptionTier,
            motionTemplate = motionTemplate,
            motionSpeed = motionSpeed,
            motionIntensity = motionIntensity,
            timeSec = timeSec,
            targetWidth = targetWidth,
            targetHeight = targetHeight
        )

        return bitmap
    }

    fun renderFrameToCanvas(
        canvas: Canvas,
        layers: List<CanvasLayer>,
        cachedProductBitmaps: Map<String, Bitmap>,
        bgPreset: BackgroundPreset,
        lighting: LightingSettings,
        subscriptionTier: String,
        motionTemplate: MotionTemplate,
        motionSpeed: Float,
        motionIntensity: Float,
        timeSec: Float,
        targetWidth: Int,
        targetHeight: Int,
        timelineClips: List<TimelineClip> = emptyList()
    ) {
        // 1. Draw Background Preset with subtle animated studio sweep
        drawBackground(canvas, bgPreset, targetWidth, targetHeight, timeSec)

        // 2. Compute frame motion + timeline transition transform
        val baseMotion = computeMotionFrameTransform(
            motionTemplate = motionTemplate,
            timeSec = timeSec,
            motionSpeed = motionSpeed,
            motionIntensity = motionIntensity,
            targetWidth = targetWidth
        )
        val transition = computeClipTransitionTransform(
            clips = timelineClips,
            timeSec = timeSec,
            targetWidth = targetWidth.toFloat(),
            targetHeight = targetHeight.toFloat()
        )
        val motion = MotionFrameTransform(
            scale = baseMotion.scale * transition.scale,
            offsetX = baseMotion.offsetX + transition.offsetX,
            offsetY = baseMotion.offsetY + transition.offsetY,
            rotationDeg = baseMotion.rotationDeg + transition.rotationDeg,
            alpha = (baseMotion.alpha * transition.alpha).coerceIn(0f, 1f)
        )

        val sortedLayers = layers.filter { it.isVisible }.sortedBy { it.zIndex }
        val productLayer = sortedLayers.firstOrNull { it.type == LayerType.PRODUCT }

        // 3. Draw Floor Reflection following product motion
        productLayer?.let { prod ->
            cachedProductBitmaps[prod.id]?.let { prodBmp ->
                drawReflection(
                    canvas = canvas,
                    bitmap = prodBmp,
                    prodLayer = prod,
                    motion = motion,
                    lighting = lighting,
                    targetWidth = targetWidth,
                    targetHeight = targetHeight
                )
            }
        }

        // 4. Draw sorted layers
        for (layer in sortedLayers) {
            drawLayer(
                canvas = canvas,
                layer = layer,
                cachedBitmap = cachedProductBitmaps[layer.id],
                motion = motion,
                lighting = lighting,
                timeSec = timeSec,
                targetWidth = targetWidth,
                targetHeight = targetHeight
            )
        }

        // 4b. Flash transition overlay if active
        if (transition.flashAlpha > 0.01f) {
            val flashPaint = Paint().apply {
                color = Color.WHITE
                alpha = (transition.flashAlpha * 255f).toInt().coerceIn(0, 255)
            }
            canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), flashPaint)
        }

        // 5. Composite Tiered Watermark for non-Pro/Premium users
        if (!isWatermarkFreeTier(subscriptionTier)) {
            drawWatermark(canvas, targetWidth, targetHeight)
        }
    }

    suspend fun exportCanvasToMp4File(
        context: Context,
        layers: List<CanvasLayer>,
        bgPreset: BackgroundPreset,
        lighting: LightingSettings,
        aspectRatio: AspectRatioOption,
        motionTemplate: MotionTemplate,
        motionSpeed: Float,
        motionIntensity: Float,
        subscriptionTier: String = "FREE",
        spec: VideoExportSpec? = null,
        timelineClips: List<TimelineClip> = emptyList(),
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.Default) {
        val defaultDims = alignVideoDimensions(
            aspectRatio = aspectRatio,
            maxShortEdgePx = if (isWatermarkFreeTier(subscriptionTier)) 1080 else 720
        )
        val activeSpec = spec ?: VideoExportSpec(
            widthPx = defaultDims.first,
            heightPx = defaultDims.second,
            fps = 30,
            durationSec = 5.0f,
            bitrateBps = if (defaultDims.first >= 1080 || defaultDims.second >= 1080) 7_000_000 else 4_500_000
        )

        val outDir = File(context.cacheDir, "videos").apply { mkdirs() }
        val outputFile = File(outDir, "omnistudio_ad_${System.currentTimeMillis()}.mp4")
        val cachedProductBitmaps = preloadLayerBitmaps(context, layers)

        try {
            encodeFramesToMp4(
                outputFile = outputFile,
                spec = activeSpec,
                layers = layers,
                cachedProductBitmaps = cachedProductBitmaps,
                bgPreset = bgPreset,
                lighting = lighting,
                subscriptionTier = subscriptionTier,
                motionTemplate = motionTemplate,
                motionSpeed = motionSpeed,
                motionIntensity = motionIntensity,
                timelineClips = timelineClips,
                onProgress = onProgress
            )
            Result.success(outputFile)
        } catch (primaryErr: Exception) {
            Log.w(TAG, "Primary MP4 encode failed (${primaryErr.message}), attempting 720p/30fps compatibility fallback")
            outputFile.delete()
            val fallbackDims = alignVideoDimensions(aspectRatio, maxShortEdgePx = 720)
            val fallbackSpec = VideoExportSpec(
                widthPx = fallbackDims.first,
                heightPx = fallbackDims.second,
                fps = 24,
                durationSec = activeSpec.durationSec.coerceAtMost(8f),
                bitrateBps = 2_800_000
            )
            try {
                encodeFramesToMp4(
                    outputFile = outputFile,
                    spec = fallbackSpec,
                    layers = layers,
                    cachedProductBitmaps = cachedProductBitmaps,
                    bgPreset = bgPreset,
                    lighting = lighting,
                    subscriptionTier = subscriptionTier,
                    motionTemplate = motionTemplate,
                    motionSpeed = motionSpeed,
                    motionIntensity = motionIntensity,
                    timelineClips = timelineClips,
                    onProgress = onProgress
                )
                Result.success(outputFile)
            } catch (fallbackErr: Exception) {
                Log.e(TAG, "MP4 export failed: ${fallbackErr.message}", fallbackErr)
                outputFile.delete()
                Result.failure(
                    IllegalStateException("Unable to encode H.264 MP4 video: ${fallbackErr.localizedMessage ?: "Encoder error"}")
                )
            }
        }
    }

    private fun encodeFramesToMp4(
        outputFile: File,
        spec: VideoExportSpec,
        layers: List<CanvasLayer>,
        cachedProductBitmaps: Map<String, Bitmap>,
        bgPreset: BackgroundPreset,
        lighting: LightingSettings,
        subscriptionTier: String,
        motionTemplate: MotionTemplate,
        motionSpeed: Float,
        motionIntensity: Float,
        timelineClips: List<TimelineClip> = emptyList(),
        onProgress: (Float) -> Unit
    ) {
        val width = ((spec.widthPx + 8) / 16 * 16).coerceAtLeast(128)
        val height = ((spec.heightPx + 8) / 16 * 16).coerceAtLeast(128)
        val fps = spec.fps.coerceIn(15, 60)
        val totalFrames = (spec.durationSec * fps).roundToInt().coerceAtLeast(fps)

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, spec.bitrateBps)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null
        var muxerStarted = false
        var trackIndex = -1
        var emittedFrameCount = 0L

        try {
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            fun drain(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, if (endOfStream) 10_000L else 0L)
                    when {
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) break
                        }
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) throw IllegalStateException("MediaCodec output format changed twice")
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        outIndex >= 0 -> {
                            val encodedData = encoder.getOutputBuffer(outIndex)
                                ?: throw IllegalStateException("Null encoder output buffer at $outIndex")
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                // Assign exact deterministic frame PTS so video duration & web playback speed match fps
                                bufferInfo.presentationTimeUs = (emittedFrameCount * 1_000_000L) / fps
                                emittedFrameCount += 1
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                break
                            }
                        }
                    }
                }
            }

            for (frameIdx in 0 until totalFrames) {
                drain(endOfStream = false)
                val timeSec = frameIdx.toFloat() / fps.toFloat()
                val surfaceCanvas = inputSurface.lockCanvas(null)
                try {
                    renderFrameToCanvas(
                        canvas = surfaceCanvas,
                        layers = layers,
                        cachedProductBitmaps = cachedProductBitmaps,
                        bgPreset = bgPreset,
                        lighting = lighting,
                        subscriptionTier = subscriptionTier,
                        motionTemplate = motionTemplate,
                        motionSpeed = motionSpeed,
                        motionIntensity = motionIntensity,
                        timeSec = timeSec,
                        targetWidth = width,
                        targetHeight = height,
                        timelineClips = timelineClips
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(surfaceCanvas)
                }
                if (frameIdx % 3 == 0 || frameIdx == totalFrames - 1) {
                    onProgress((frameIdx + 1).toFloat() / totalFrames.toFloat())
                }
            }

            drain(endOfStream = true)
            onProgress(1f)
        } finally {
            runCatching {
                if (muxerStarted) muxer?.stop()
            }
            runCatching { muxer?.release() }
            runCatching {
                encoder?.stop()
            }
            runCatching { encoder?.release() }
            runCatching { inputSurface?.release() }
        }
    }

    fun saveVideoToGallery(context: Context, videoFile: File): Uri? {
        if (!videoFile.exists() || videoFile.length() == 0L) return null
        val filename = "OmniStudio_${System.currentTimeMillis()}.mp4"
        var videoUri: Uri? = null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/OmniStudio")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                videoUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(videoFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    val updateValues = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    resolver.update(uri, updateValues, null, null)
                }
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/OmniStudio"
                val dir = File(moviesDir)
                if (!dir.exists()) dir.mkdirs()
                val outFile = File(dir, filename)
                FileInputStream(videoFile).use { input ->
                    FileOutputStream(outFile).use { out ->
                        input.copyTo(out)
                    }
                }
                videoUri = Uri.fromFile(outFile)
            }
            videoUri
        } catch (e: Exception) {
            Log.e(TAG, "saveVideoToGallery failed: ${e.message}", e)
            null
        }
    }

    fun shareVideo(
        context: Context,
        videoFile: File,
        caption: String = "Created with OmniStudio AI by ENJEBEL TECH"
    ): Boolean {
        return try {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", videoFile)
            if (contentUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share OmniStudio MP4 Ad"))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "shareVideo failed: ${e.message}", e)
            false
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        preset: BackgroundPreset,
        width: Int,
        height: Int,
        timeSec: Float = 0f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sweepOffset = sin(timeSec * 1.4).toFloat() * (width * 0.12f)

        when (preset) {
            BackgroundPreset.LUXURY_MARBLE -> {
                val shader = LinearGradient(
                    0f + sweepOffset, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.parseColor("#0B0C10"), Color.parseColor("#1B1F2A"), Color.parseColor("#0D0E15")),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

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
                    width * 0.5f + sweepOffset, 0f, width * 0.5f, height.toFloat(),
                    intArrayOf(Color.parseColor("#2E3348"), Color.parseColor("#171A26"), Color.parseColor("#0F111A")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            BackgroundPreset.CYBERPUNK_NEON -> {
                val shader = LinearGradient(
                    0f + sweepOffset, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(Color.parseColor("#050510"), Color.parseColor("#1E0638"), Color.parseColor("#021E2F")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                val gridPaint = Paint().apply {
                    color = Color.parseColor("#2600F0FF")
                    strokeWidth = 2f
                }
                val stepY = (height / 20).coerceAtLeast(16)
                val scrollY = ((timeSec * 40f) % stepY).toInt()
                for (y in scrollY until height step stepY) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
                }
            }
            BackgroundPreset.FASHION_RUNWAY -> {
                val shader = LinearGradient(
                    width * 0.5f + sweepOffset, 0f, width * 0.5f, height.toFloat(),
                    intArrayOf(Color.parseColor("#2B0038"), Color.parseColor("#0F0214"), Color.parseColor("#000000")),
                    null,
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
            BackgroundPreset.GOLDEN_LUXURY -> {
                val shader = LinearGradient(
                    0f + sweepOffset, 0f, width.toFloat(), height.toFloat(),
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
                val stepX = (width / 16).coerceAtLeast(16)
                val stepY = (height / 24).coerceAtLeast(16)
                for (x in 0 until width step stepX) {
                    canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
                }
                for (y in 0 until height step stepY) {
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
        canvas: Canvas,
        bitmap: Bitmap,
        prodLayer: CanvasLayer,
        motion: MotionFrameTransform,
        lighting: LightingSettings,
        targetWidth: Int,
        targetHeight: Int
    ) {
        try {
            val centerX = prodLayer.x * targetWidth + motion.offsetX
            val centerY = prodLayer.y * targetHeight + motion.offsetY
            val size = (targetWidth * 0.65f * prodLayer.scale * motion.scale)

            canvas.save()
            canvas.scale(1f, -1f, centerX, centerY + size * 0.45f)

            val reflectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (lighting.reflectionOpacity * 140 * motion.alpha).toInt().coerceIn(0, 255)
                colorFilter = ColorGradingUtils.toAndroidColorFilter(prodLayer.colorFilter, prodLayer.filterIntensity)
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
        canvas: Canvas,
        layer: CanvasLayer,
        cachedBitmap: Bitmap?,
        motion: MotionFrameTransform,
        lighting: LightingSettings,
        timeSec: Float,
        targetWidth: Int,
        targetHeight: Int
    ) {
        val isProduct = layer.type == LayerType.PRODUCT
        val centerX = layer.x * targetWidth + if (isProduct) motion.offsetX else 0f
        val centerY = layer.y * targetHeight + if (isProduct) motion.offsetY else 0f
        val effectiveScale = if (isProduct) layer.scale * motion.scale else layer.scale
        val effectiveRotation = if (isProduct) layer.rotation + motion.rotationDeg else layer.rotation
        val effectiveAlpha = ((layer.opacity * if (isProduct) motion.alpha else 1f) * 255f).toInt().coerceIn(0, 255)

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(effectiveRotation)
        canvas.scale(effectiveScale, effectiveScale)

        when (layer.type) {
            LayerType.PRODUCT -> {
                try {
                    val size = (targetWidth * 0.65f).toInt()
                    val half = size / 2

                    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        alpha = (lighting.shadowOpacity * 200 * motion.alpha).toInt().coerceIn(0, 255)
                    }
                    val angleRad = Math.toRadians(lighting.shadowAngle.toDouble())
                    val dx = (lighting.shadowDistance * cos(angleRad)).toFloat()
                    val dy = (lighting.shadowDistance * sin(angleRad)).toFloat()
                    canvas.drawOval(
                        RectF(-half * 0.8f + dx, half * 0.7f + dy, half * 0.8f + dx, half * 0.95f + dy),
                        shadowPaint
                    )

                    if (cachedBitmap != null) {
                        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            alpha = effectiveAlpha
                            colorFilter = ColorGradingUtils.toAndroidColorFilter(layer.colorFilter, layer.filterIntensity)
                        }
                        canvas.drawBitmap(
                            cachedBitmap,
                            null,
                            RectF(-half.toFloat(), -half.toFloat(), half.toFloat(), half.toFloat()),
                            bmpPaint
                        )
                    }
                } catch (_: Exception) {}
            }
            LayerType.TEXT -> {
                val pulse = 1.0f + 0.02f * sin(timeSec * 3.0).toFloat()
                canvas.scale(pulse, pulse)
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layer.textColor.toInt()
                    alpha = effectiveAlpha
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
                    alpha = effectiveAlpha
                    style = Paint.Style.FILL
                }
                val w = targetWidth * 0.35f
                val h = 48f * (targetWidth / 400f)
                val rect = RectF(-w / 2, -h / 2, w / 2, h / 2)
                canvas.drawRoundRect(rect, h / 2, h / 2, badgePaint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = effectiveAlpha
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
                        @Suppress("DEPRECATION")
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
            textSize = (width * 0.026f).coerceIn(18f, 44f)
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

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F0FF")
        }
        canvas.drawCircle(left + pad * 0.7f, top + rectH / 2f, 7f, dotPaint)

        canvas.drawText(text, right - pad * 0.5f, top + rectH / 2f + textBounds.height() / 2.5f, paint)
    }
}
