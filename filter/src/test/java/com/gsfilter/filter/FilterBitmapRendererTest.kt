package com.gsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterBitmapRendererTest {

    @Test
    fun `default params keep pixels unchanged`() {
        val color = 0xcc336699.toInt()
        val output = FilterBitmapRenderer.renderPixels(
            pixels = intArrayOf(color),
            width = 1,
            height = 1,
            params = ShaderFilterParams.from(FilterRecipe(), Adjustments()),
        )

        assertEquals(color, output.single())
    }

    @Test
    fun `user adjustments affect rendered pixels`() {
        val color = 0xff336699.toInt()
        val output = FilterBitmapRenderer.renderPixels(
            pixels = intArrayOf(color),
            width = 1,
            height = 1,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(),
                adjustments = Adjustments(brightness = 40, contrast = 30, saturation = 20),
            ),
        )

        assertNotEquals(color, output.single())
    }

    @Test
    fun `render pixels rejects mismatched dimensions`() {
        val result = runCatching {
            FilterBitmapRenderer.renderPixels(
                pixels = intArrayOf(0xff336699.toInt()),
                width = 2,
                height = 1,
                params = ShaderFilterParams.from(FilterRecipe(), Adjustments()),
            )
        }

        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `target size keeps aspect ratio and does not upscale`() {
        assertEquals(
            FilterBitmapRenderer.RenderSize(width = 1000, height = 500),
            FilterBitmapRenderer.targetSize(width = 4000, height = 2000, maxWidth = 1000, maxHeight = 1000),
        )
        assertEquals(
            FilterBitmapRenderer.RenderSize(width = 500, height = 1000),
            FilterBitmapRenderer.targetSize(width = 2000, height = 4000, maxWidth = 1000, maxHeight = 1000),
        )
        assertEquals(
            FilterBitmapRenderer.RenderSize(width = 800, height = 600),
            FilterBitmapRenderer.targetSize(width = 800, height = 600, maxWidth = 1200, maxHeight = 1200),
        )
    }
}
