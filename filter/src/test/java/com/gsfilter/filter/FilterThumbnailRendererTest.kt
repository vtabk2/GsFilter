package com.gsfilter.filter

import com.gsfilter.filter.renderer.FilterBitmapRenderer
import com.gsfilter.filter.renderer.FilterThumbnailRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterThumbnailRendererTest {

    @Test
    fun `default recipe keeps thumbnail pixel unchanged`() {
        val color = 0xcc336699.toInt()
        val output = FilterThumbnailRenderer.filterPixel(
            pixels = intArrayOf(color),
            x = 0,
            y = 0,
            width = 1,
            height = 1,
            params = ShaderFilterParams.from(FilterRecipe(), Adjustments()),
        )

        assertEquals(color, output)
    }

    @Test
    fun `monochrome recipe balances color channels`() {
        val output = FilterThumbnailRenderer.filterPixel(
            pixels = intArrayOf(0xcc336699.toInt()),
            x = 0,
            y = 0,
            width = 1,
            height = 1,
            params = ShaderFilterParams.from(FilterRecipe(isMonochrome = true), Adjustments()),
        )

        assertEquals(0xcc, output ushr 24)
        assertEquals(channel(output, RED_SHIFT), channel(output, GREEN_SHIFT))
        assertEquals(channel(output, GREEN_SHIFT), channel(output, BLUE_SHIFT))
    }

    @Test
    fun `filter recipe changes thumbnail pixel`() {
        val color = 0xff336699.toInt()
        val output = FilterThumbnailRenderer.filterPixel(
            pixels = intArrayOf(color),
            x = 0,
            y = 0,
            width = 1,
            height = 1,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(redShift = 20, blueShift = -20),
                adjustments = Adjustments(),
            ),
        )

        assertNotEquals(color, output)
    }

    @Test
    fun `thumbnail render size is capped`() {
        assertEquals(
            FilterBitmapRenderer.RenderSize(width = 128, height = 64),
            FilterBitmapRenderer.targetSize(
                width = 4000,
                height = 2000,
                maxWidth = FilterThumbnailRenderer.THUMBNAIL_MAX_SIZE,
                maxHeight = FilterThumbnailRenderer.THUMBNAIL_MAX_SIZE,
            ),
        )
    }

    @Test
    fun `art thumbnails allow a larger render cap`() {
        assertEquals(
            FilterThumbnailRenderer.THUMBNAIL_MAX_SIZE,
            FilterThumbnailRenderer.maxSizeFor(FilterRecipe()),
        )
        assertEquals(
            FilterThumbnailRenderer.ART_THUMBNAIL_MAX_SIZE,
            FilterThumbnailRenderer.maxSizeFor(FilterRecipe(effect = FilterEffect.Ink)),
        )
    }

    @Test
    fun `art thumbnails use a smaller texel scale`() {
        assertEquals(1f, FilterThumbnailRenderer.texelScaleFor(FilterRecipe()), FLOAT_DELTA)
        assertEquals(
            0.6f,
            FilterThumbnailRenderer.texelScaleFor(FilterRecipe(effect = FilterEffect.Ink)),
            FLOAT_DELTA,
        )
    }

    @Test
    fun `art effects keep full source texture for thumbnail rendering`() {
        assertFalse(FilterThumbnailRenderer.shouldUseFullSourceTexture(FilterRecipe()))
        assertTrue(
            FilterThumbnailRenderer.shouldUseFullSourceTexture(
                FilterRecipe(effect = FilterEffect.Sketch),
            ),
        )
    }

    @Test
    fun `thumbnail recipe leaves color filters unchanged`() {
        val recipe = FilterRecipe(redShift = 10, blueShift = -10)

        assertEquals(recipe, FilterThumbnailRenderer.thumbnailRecipe(recipe))
    }

    @Test
    fun `thumbnail recipe softens ink details`() {
        val recipe = FilterRecipe(
            effect = FilterEffect.Ink,
            effectStrength = 100,
            effectThreshold = 20,
        )
        val thumbnailRecipe = FilterThumbnailRenderer.thumbnailRecipe(recipe)

        assertEquals(75, thumbnailRecipe.effectStrength)
        assertEquals(46, thumbnailRecipe.effectThreshold)
    }

    private fun channel(color: Int, shift: Int): Int = (color shr shift) and CHANNEL_MASK

    private companion object {
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val BLUE_SHIFT = 0
        const val CHANNEL_MASK = 255
        const val FLOAT_DELTA = 0.0001f
    }
}
