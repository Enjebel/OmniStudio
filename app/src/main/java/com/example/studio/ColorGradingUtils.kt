package com.example.studio

import android.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.ColorFilterPreset
import android.graphics.ColorMatrix as AndroidColorMatrix

/**
 * Translates CSS-style color grading parameters (brightness, contrast, saturate,
 * sepia, warmth) into hardware-accelerated 4x5 ColorMatrix transforms shared by
 * the Jetpack Compose Studio Canvas and the Android Canvas / MP4 ExportEngine.
 */
object ColorGradingUtils {

    fun buildColorMatrixArray(
        preset: ColorFilterPreset,
        intensity: Float = 1.0f
    ): FloatArray {
        val t = intensity.coerceIn(0f, 1f)
        if (preset == ColorFilterPreset.NONE || t <= 0.001f) {
            return AndroidColorMatrix().array
        }

        val brightness = 1f + (preset.brightness - 1f) * t
        val contrast = 1f + (preset.contrast - 1f) * t
        val saturation = 1f + (preset.saturation - 1f) * t
        val sepia = (preset.sepia * t).coerceIn(0f, 1f)
        val warmth = preset.warmth * t

        // 1. Saturation matrix
        val result = AndroidColorMatrix().apply {
            setSaturation(saturation)
        }

        // 2. W3C CSS sepia(s) matrix
        if (sepia > 0.001f) {
            val sepiaMatrix = AndroidColorMatrix(
                floatArrayOf(
                    1f - 0.607f * sepia, 0.769f * sepia, 0.189f * sepia, 0f, 0f,
                    0.349f * sepia, 1f - 0.314f * sepia, 0.168f * sepia, 0f, 0f,
                    0.272f * sepia, 0.534f * sepia, 1f - 0.869f * sepia, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(sepiaMatrix)
        }

        // 3. CSS contrast(c) + brightness(b) + warmth color balance
        val contrastTranslate = 127.5f * (1f - contrast) * brightness
        val scale = contrast * brightness
        val redShift = warmth * 28f
        val blueShift = -warmth * 22f

        val toneMatrix = AndroidColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, contrastTranslate + redShift,
                0f, scale, 0f, 0f, contrastTranslate,
                0f, 0f, scale, 0f, contrastTranslate + blueShift,
                0f, 0f, 0f, 1f, 0f
            )
        )
        result.postConcat(toneMatrix)

        return result.array
    }

    fun toComposeColorFilter(
        preset: ColorFilterPreset,
        intensity: Float = 1.0f
    ): ColorFilter? {
        if (preset == ColorFilterPreset.NONE || intensity <= 0.001f) return null
        return ColorFilter.colorMatrix(ColorMatrix(buildColorMatrixArray(preset, intensity)))
    }

    fun toAndroidColorFilter(
        preset: ColorFilterPreset,
        intensity: Float = 1.0f
    ): ColorMatrixColorFilter? {
        if (preset == ColorFilterPreset.NONE || intensity <= 0.001f) return null
        return ColorMatrixColorFilter(AndroidColorMatrix(buildColorMatrixArray(preset, intensity)))
    }
}
