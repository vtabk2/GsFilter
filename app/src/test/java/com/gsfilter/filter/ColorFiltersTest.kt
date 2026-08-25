package com.gsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorFiltersTest {

    @Test
    fun `mono filter keeps alpha and returns equal color channels`() {
        val filtered = ColorFilters.apply(
            argb = 0x80402010.toInt(),
            recipe = FilterRecipe(isMonochrome = true),
            adjustments = Adjustments(),
        )

        assertEquals(0x80, (filtered ushr 24) and 0xff)
        assertEquals(red(filtered), green(filtered))
        assertEquals(green(filtered), blue(filtered))
    }

    @Test
    fun `brightness adjustment clamps channels`() {
        val filtered = ColorFilters.apply(
            argb = 0xfffff0e0.toInt(),
            recipe = FilterRecipe(),
            adjustments = Adjustments(brightness = 100),
        )

        assertEquals(255, red(filtered))
        assertEquals(255, green(filtered))
        assertEquals(255, blue(filtered))
    }

    private fun red(argb: Int): Int = (argb ushr 16) and 0xff

    private fun green(argb: Int): Int = (argb ushr 8) and 0xff

    private fun blue(argb: Int): Int = argb and 0xff
}
