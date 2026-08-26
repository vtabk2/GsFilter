package com.gsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class ShaderFilterParamsTest {

    @Test
    fun `maps recipe and adjustments to shader values`() {
        val params = ShaderFilterParams.from(
            recipe = FilterRecipe(
                effect = FilterEffect.Ink,
                effectStrength = 80,
                effectThreshold = 35,
                effectTone = 60,
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

        assertEquals(FilterEffect.Ink, params.effect)
        assertEquals(0.8f, params.effectStrength, DELTA)
        assertEquals(0.35f, params.effectThreshold, DELTA)
        assertEquals(0.6f, params.effectTone, DELTA)
        assertEquals(1f, params.isMonochrome, DELTA)
        assertEquals(10f / 255f, params.redShift, DELTA)
        assertEquals(-5f / 255f, params.greenShift, DELTA)
        assertEquals(20f / 255f, params.blueShift, DELTA)
        assertEquals(0.0625f, params.brightness, DELTA)
        assertEquals(-0.1f, params.exposure, DELTA)
        assertEquals(1.1f, params.contrast, DELTA)
        assertEquals(0.15f, params.highlights, DELTA)
        assertEquals(-0.2f, params.shadows, DELTA)
        assertEquals(0.9f, params.saturation, DELTA)
        assertEquals(0.25f, params.vibrance, DELTA)
        assertEquals(-0.3f, params.temperature, DELTA)
        assertEquals(0.35f, params.tint, DELTA)
        assertEquals(0.4f, params.sharpness, DELTA)
        assertEquals(-0.45f, params.clarity, DELTA)
        assertEquals(0.1f, params.fade, DELTA)
        assertEquals(0.4f, params.vignette, DELTA)
        assertEquals(1f, params.grain, DELTA)
    }

    @Test
    fun `keeps adjust extremes inside usable preview ranges`() {
        val minParams = ShaderFilterParams.from(
            recipe = FilterRecipe(),
            adjustments = Adjustments(
                brightness = -100,
                exposure = -100,
                contrast = -100,
                highlights = -100,
                shadows = -100,
                saturation = -100,
                vibrance = -100,
                temperature = -100,
                tint = -100,
                sharpness = 0,
                clarity = -100,
                fade = 0,
                vignette = 0,
                grain = 0,
            ),
        )
        val maxParams = ShaderFilterParams.from(
            recipe = FilterRecipe(),
            adjustments = Adjustments(
                brightness = 100,
                exposure = 100,
                contrast = 100,
                highlights = 100,
                shadows = 100,
                saturation = 100,
                vibrance = 100,
                temperature = 100,
                tint = 100,
                sharpness = 100,
                clarity = 100,
                fade = 100,
                vignette = 100,
                grain = 100,
            ),
        )

        assertEquals(-0.25f, minParams.brightness, DELTA)
        assertEquals(0.25f, maxParams.brightness, DELTA)
        assertEquals(-0.5f, minParams.exposure, DELTA)
        assertEquals(0.5f, maxParams.exposure, DELTA)
        assertEquals(0.5f, minParams.contrast, DELTA)
        assertEquals(1.5f, maxParams.contrast, DELTA)
        assertEquals(-0.5f, minParams.highlights, DELTA)
        assertEquals(0.5f, maxParams.highlights, DELTA)
        assertEquals(-0.5f, minParams.shadows, DELTA)
        assertEquals(0.5f, maxParams.shadows, DELTA)
        assertEquals(0.5f, minParams.saturation, DELTA)
        assertEquals(1.5f, maxParams.saturation, DELTA)
        assertEquals(-0.5f, minParams.vibrance, DELTA)
        assertEquals(0.5f, maxParams.vibrance, DELTA)
        assertEquals(-0.5f, minParams.temperature, DELTA)
        assertEquals(0.5f, maxParams.temperature, DELTA)
        assertEquals(-0.5f, minParams.tint, DELTA)
        assertEquals(0.5f, maxParams.tint, DELTA)
        assertEquals(0f, minParams.sharpness, DELTA)
        assertEquals(0.5f, maxParams.sharpness, DELTA)
        assertEquals(-0.5f, minParams.clarity, DELTA)
        assertEquals(0.5f, maxParams.clarity, DELTA)
        assertEquals(0f, minParams.fade, DELTA)
        assertEquals(1f, maxParams.fade, DELTA)
        assertEquals(0f, minParams.vignette, DELTA)
        assertEquals(1f, maxParams.vignette, DELTA)
        assertEquals(0f, minParams.grain, DELTA)
        assertEquals(1f, maxParams.grain, DELTA)
    }

    @Test
    fun `adds recipe preset adjustments before user adjustments`() {
        val params = ShaderFilterParams.from(
            recipe = FilterRecipe(
                adjustments = Adjustments(
                    contrast = 20,
                    saturation = -10,
                    fade = 15,
                ),
            ),
            adjustments = Adjustments(
                contrast = 10,
                saturation = 20,
                fade = 5,
            ),
        )

        assertEquals(1.15f, params.contrast, DELTA)
        assertEquals(1.05f, params.saturation, DELTA)
        assertEquals(0.2f, params.fade, DELTA)
    }

    private companion object {
        const val DELTA = 0.0001f
    }
}
