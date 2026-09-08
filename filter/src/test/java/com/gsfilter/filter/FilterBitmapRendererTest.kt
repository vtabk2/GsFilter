package com.gsfilter.filter

import com.gsfilter.filter.renderer.FilterBitmapRenderer
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
    fun `beauty whitening affects skin tones but leaves neutral pixels unchanged`() {
        val skin = 0xffbf8c66.toInt()
        val neutral = 0xff808080.toInt()
        val output = FilterBitmapRenderer.renderPixels(
            pixels = intArrayOf(skin, neutral),
            width = 2,
            height = 1,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(skinWhitening = 100),
                adjustments = Adjustments(),
            ),
        )

        assertNotEquals(skin, output[0])
        assertEquals(neutral, output[1])
    }

    @Test
    fun `sketch effect draws dark edge lines`() {
        val white = 0xffffffff.toInt()
        val black = 0xff000000.toInt()
        val output = FilterBitmapRenderer.renderPixels(
            pixels = intArrayOf(
                white,
                white,
                black,
                white,
                white,
                black,
                white,
                white,
                black,
            ),
            width = 3,
            height = 3,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(effect = FilterEffect.Sketch),
                adjustments = Adjustments(),
            ),
        )

        assertTrue((output[4] and 0xff) < 128)
    }

    @Test
    fun `art effects change edge pixels`() {
        val white = 0xffffffff.toInt()
        val black = 0xff000000.toInt()
        val pixels = intArrayOf(
            white,
            white,
            black,
            white,
            white,
            black,
            white,
            white,
            black,
        )

        listOf(
            FilterEffect.Ink,
            FilterEffect.Pencil,
            FilterEffect.ColorPencil,
            FilterEffect.Charcoal,
            FilterEffect.CrossHatch,
        ).forEach { effect ->
            val output = FilterBitmapRenderer.renderPixels(
                pixels = pixels,
                width = 3,
                height = 3,
                params = ShaderFilterParams.from(
                    recipe = FilterRecipe(effect = effect),
                    adjustments = Adjustments(),
                ),
            )

            assertNotEquals(white, output[4])
        }
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
