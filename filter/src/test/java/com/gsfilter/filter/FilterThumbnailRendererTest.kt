package com.gsfilter.filter

import com.gsfilter.filter.renderer.FilterThumbnailRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

    private fun channel(color: Int, shift: Int): Int = (color shr shift) and CHANNEL_MASK

    private companion object {
        const val RED_SHIFT = 16
        const val GREEN_SHIFT = 8
        const val BLUE_SHIFT = 0
        const val CHANNEL_MASK = 255
    }
}
