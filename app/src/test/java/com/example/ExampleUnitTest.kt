package com.example

import com.example.model.AdStudioRepository
import com.example.model.AiCategory
import com.example.model.AspectRatioOption
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
}
