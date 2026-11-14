package com.example

import com.example.model.AdStudioRepository
import com.example.model.AiCategory
import com.example.model.AspectRatioOption
import com.example.model.ColorFilterPreset
import com.example.model.MotionTemplate
import com.example.model.TimelineClip
import com.example.model.TimelineTrackType
import com.example.model.TransitionEffect
import com.example.studio.ExportEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testDemoProductsCatalog() {
        val products = AdStudioRepository.demoProducts
        assertEquals(6, products.size)
        assertTrue(products.any { it.name.contains("Chronos") })
        assertTrue(products.any { it.name.contains("AeroSprint") })
        assertTrue(products.any { it.name.contains("L'Élixir") })
    }

    @Test
    fun testTemplatesIntegrity() {
        val templates = AdStudioRepository.templates
        assertEquals(6, templates.size)
        assertTrue(templates.any { it.defaultRatio == AspectRatioOption.RATIO_9_16 })
        assertTrue(templates.any { it.defaultRatio == AspectRatioOption.RATIO_1_1 })
        assertTrue(templates.any { it.defaultRatio == AspectRatioOption.RATIO_16_9 })
    }

    @Test
    fun testAiCopyGeneration() {
        for (category in AiCategory.entries) {
            val copy = AdStudioRepository.generateCopyForProduct("Test Product", category)
            assertEquals(5, copy.hooks.size)
            assertEquals(5, copy.headlines.size)
            assertEquals(5, copy.offers.size)
            assertEquals(5, copy.scripts.size)
            assertEquals(5, copy.ctas.size)
            assertTrue(copy.scripts[0].contains("Test Product"))
        }
    }

    @Test
    fun testExportFormats() {
        assertEquals("png", ExportEngine.ExportFormat.PNG.extension)
        assertEquals("jpg", ExportEngine.ExportFormat.JPG.extension)
        assertEquals("webp", ExportEngine.ExportFormat.WEBP.extension)
    }

    @Test
    fun testVideoDimensionAlignmentAndMotionTransforms() {
        for (ratio in AspectRatioOption.entries) {
            val (w720, h720) = ExportEngine.alignVideoDimensions(ratio, 720)
            assertEquals("Width must be divisible by 16 for H.264", 0, w720 % 16)
            assertEquals("Height must be divisible by 16 for H.264", 0, h720 % 16)
            assertTrue(w720 >= 720 && h720 >= 720)

            val (w1080, h1080) = ExportEngine.alignVideoDimensions(ratio, 1080)
            assertEquals(0, w1080 % 16)
            assertEquals(0, h1080 % 16)
            assertTrue(w1080 >= 1072 && h1080 >= 1072)
        }

        val punch = ExportEngine.computeMotionFrameTransform(
            motionTemplate = MotionTemplate.DYNAMIC_ZOOM_PUNCH,
            timeSec = 0.1f,
            motionSpeed = 1.0f,
            motionIntensity = 1.0f,
            targetWidth = 1080
        )
        assertTrue(punch.scale > 1.15f)

        val revealStart = ExportEngine.computeMotionFrameTransform(
            motionTemplate = MotionTemplate.CINEMATIC_REVEAL,
            timeSec = 0.3f,
            motionSpeed = 1.0f,
            motionIntensity = 1.0f,
            targetWidth = 720
        )
        val revealEnd = ExportEngine.computeMotionFrameTransform(
            motionTemplate = MotionTemplate.CINEMATIC_REVEAL,
            timeSec = 2.0f,
            motionSpeed = 1.0f,
            motionIntensity = 1.0f,
            targetWidth = 720
        )
        assertTrue(revealEnd.alpha > revealStart.alpha)
        assertEquals(1.0f, revealEnd.scale, 0.001f)
    }

    @Test
    fun testTransitionTransforms() {
        val fadeClip = TimelineClip(
            id = "clip_fade",
            trackType = TimelineTrackType.PRODUCT,
            name = "Fade Clip",
            startTimeSec = 2.0f,
            durationSec = 4.0f,
            transitionIn = TransitionEffect.FADE,
            transitionDurationSec = 0.8f
        )
        val slideClip = TimelineClip(
            id = "clip_slide",
            trackType = TimelineTrackType.PRODUCT,
            name = "Slide Clip",
            startTimeSec = 6.0f,
            durationSec = 4.0f,
            transitionIn = TransitionEffect.SLIDE_LEFT,
            transitionDurationSec = 0.5f
        )
        val zoomClip = TimelineClip(
            id = "clip_zoom",
            trackType = TimelineTrackType.TEXT,
            name = "Zoom Clip",
            startTimeSec = 10.0f,
            durationSec = 3.0f,
            transitionIn = TransitionEffect.ZOOM_IN,
            transitionDurationSec = 0.6f
        )
        val clips = listOf(fadeClip, slideClip, zoomClip)

        // Outside transition windows returns neutral transform
        val outside = ExportEngine.computeClipTransitionTransform(clips, timeSec = 0.5f, targetWidth = 1080f, targetHeight = 1920f)
        assertEquals(TransitionEffect.NONE, outside.effect)
        assertEquals(1.0f, outside.alpha, 0.001f)
        assertEquals(0f, outside.offsetX, 0.001f)

        // Inside Fade window increases alpha monotonically
        val fadeStart = ExportEngine.computeClipTransitionTransform(clips, timeSec = 2.05f, targetWidth = 1080f, targetHeight = 1920f)
        val fadeMid = ExportEngine.computeClipTransitionTransform(clips, timeSec = 2.4f, targetWidth = 1080f, targetHeight = 1920f)
        assertEquals(TransitionEffect.FADE, fadeStart.effect)
        assertTrue(fadeMid.alpha > fadeStart.alpha)

        // Inside Slide Left window slides from positive X toward 0
        val slideStart = ExportEngine.computeClipTransitionTransform(clips, timeSec = 6.05f, targetWidth = 1080f, targetHeight = 1920f)
        val slideNearEnd = ExportEngine.computeClipTransitionTransform(clips, timeSec = 6.45f, targetWidth = 1080f, targetHeight = 1920f)
        assertEquals(TransitionEffect.SLIDE_LEFT, slideStart.effect)
        assertTrue(slideStart.offsetX > slideNearEnd.offsetX)
        assertTrue(slideStart.offsetX > 100f)

        // Inside Zoom In window scales from <1.0 toward 1.0
        val zoomStart = ExportEngine.computeClipTransitionTransform(clips, timeSec = 10.05f, targetWidth = 1080f, targetHeight = 1920f)
        val zoomNearEnd = ExportEngine.computeClipTransitionTransform(clips, timeSec = 10.55f, targetWidth = 1080f, targetHeight = 1920f)
        assertEquals(TransitionEffect.ZOOM_IN, zoomStart.effect)
        assertTrue(zoomStart.scale < zoomNearEnd.scale)
    }

    @Test
    fun testColorFilterPresetsHaveCssSpecs() {
        assertTrue(ColorFilterPreset.entries.any { it == ColorFilterPreset.CINEMATIC })
        assertTrue(ColorFilterPreset.entries.any { it == ColorFilterPreset.VIBRANT })
        assertTrue(ColorFilterPreset.entries.any { it == ColorFilterPreset.VINTAGE })
        for (preset in ColorFilterPreset.entries) {
            assertTrue(preset.title.isNotBlank())
            assertTrue(preset.cssFilterSpec.startsWith("filter:"))
        }
        assertEquals("filter: none;", ColorFilterPreset.NONE.cssFilterSpec)
        assertTrue(ColorFilterPreset.VINTAGE.cssFilterSpec.contains("sepia("))
        assertTrue(ColorFilterPreset.VIBRANT.cssFilterSpec.contains("saturate("))
    }
}
