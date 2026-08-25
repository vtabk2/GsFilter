package com.gsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class ShaderFilterParamsTest {

    @Test
    fun `maps recipe and adjustments to shader values`() {
        val params = ShaderFilterParams.from(
            recipe = FilterRecipe(
                isMonochrome = true,
                redShift = 10,
                greenShift = -5,
                blueShift = 20,
            ),
            adjustments = Adjustments(
                brightness = 25,
                contrast = 120,
                saturation = 80,
            ),
        )

        assertEquals(1f, params.isMonochrome, DELTA)
        assertEquals(10f / 255f, params.redShift, DELTA)
        assertEquals(-5f / 255f, params.greenShift, DELTA)
        assertEquals(20f / 255f, params.blueShift, DELTA)
        assertEquals(25f / 255f, params.brightness, DELTA)
        assertEquals(1.2f, params.contrast, DELTA)
        assertEquals(0.8f, params.saturation, DELTA)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
