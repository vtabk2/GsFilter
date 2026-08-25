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
                exposure = -20,
                contrast = 20,
                highlights = 30,
                shadows = -40,
                saturation = -20,
                vibrance = 50,
                temperature = -60,
                tint = 70,
                sharpness = 80,
                clarity = -90,
                fade = 10,
                vignette = 40,
                grain = 100,
            ),
        )

        assertEquals(1f, params.isMonochrome, DELTA)
        assertEquals(10f / 255f, params.redShift, DELTA)
        assertEquals(-5f / 255f, params.greenShift, DELTA)
        assertEquals(20f / 255f, params.blueShift, DELTA)
        assertEquals(25f / 255f, params.brightness, DELTA)
        assertEquals(-0.2f, params.exposure, DELTA)
        assertEquals(1.2f, params.contrast, DELTA)
        assertEquals(0.3f, params.highlights, DELTA)
        assertEquals(-0.4f, params.shadows, DELTA)
        assertEquals(0.8f, params.saturation, DELTA)
        assertEquals(0.5f, params.vibrance, DELTA)
        assertEquals(-0.6f, params.temperature, DELTA)
        assertEquals(0.7f, params.tint, DELTA)
        assertEquals(0.8f, params.sharpness, DELTA)
        assertEquals(-0.9f, params.clarity, DELTA)
        assertEquals(0.1f, params.fade, DELTA)
        assertEquals(0.4f, params.vignette, DELTA)
        assertEquals(1f, params.grain, DELTA)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
