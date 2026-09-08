package com.gsfilter.filter

import com.gsfilter.filter.renderer.FilterBitmapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

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
    fun `lipstick changes only the detected lip region`() {
        val original = 0xff996633.toInt()
        val output = FilterBitmapRenderer.renderPixels(
            pixels = intArrayOf(original),
            width = 1,
            height = 1,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(lipstick = 100),
                adjustments = Adjustments(),
                makeupFeatures = MakeupFeatures(
                    leftCheekX = 0f,
                    leftCheekY = 0f,
                    rightCheekX = 0f,
                    rightCheekY = 0f,
                    cheekRadiusX = 0f,
                    cheekRadiusY = 0f,
                    lipCenterX = 0.5f,
                    lipCenterY = 0.5f,
                    lipRadiusX = 1f,
                    lipRadiusY = 1f,
                ),
            ),
        )

        assertNotEquals(original, output.single())
    }

    @Test
    fun `lip contour keeps lipstick inside the angled mouth shape`() {
        val original = 0xff996633.toInt()
        val output = FilterBitmapRenderer.renderPixels(
            pixels = IntArray(5 * 5) { original },
            width = 5,
            height = 5,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(lipstick = 100),
                adjustments = Adjustments(),
                makeupFeatures = MakeupFeatures(
                    leftCheekX = 0f,
                    leftCheekY = 0f,
                    rightCheekX = 0f,
                    rightCheekY = 0f,
                    cheekRadiusX = 0f,
                    cheekRadiusY = 0f,
                    lipCenterX = 0.5f,
                    lipCenterY = 0.5f,
                    lipRadiusX = 0.2f,
                    lipRadiusY = 0.1f,
                    lipContour = listOf(
                        NormalizedPoint(0.5f, 0.25f),
                        NormalizedPoint(0.75f, 0.5f),
                        NormalizedPoint(0.5f, 0.75f),
                        NormalizedPoint(0.25f, 0.5f),
                    ),
                ),
            ),
        )

        assertNotEquals(original, output[12])
        assertEquals(original, output[0])
    }

    @Test
    fun `rotated lip region follows face roll`() {
        val original = 0xff996633.toInt()
        val features = MakeupFeatures(
            leftCheekX = 0f,
            leftCheekY = 0f,
            rightCheekX = 0f,
            rightCheekY = 0f,
            cheekRadiusX = 0f,
            cheekRadiusY = 0f,
            lipCenterX = 0.5f,
            lipCenterY = 0.5f,
            lipRadiusX = 0.35f,
            lipRadiusY = 0.1f,
            rotationRadians = (PI / 4).toFloat(),
        )
        val output = FilterBitmapRenderer.renderPixels(
            pixels = IntArray(25) { original },
            width = 5,
            height = 5,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(lipstick = 100),
                adjustments = Adjustments(),
                makeupFeatures = features,
            ),
        )

        assertNotEquals(original, output[18])
    }

    @Test
    fun `hidden cheek strength prevents makeup on the hidden side`() {
        val original = 0xff996633.toInt()
        val output = FilterBitmapRenderer.renderPixels(
            pixels = IntArray(4) { original },
            width = 4,
            height = 1,
            params = ShaderFilterParams.from(
                recipe = FilterRecipe(blush = 100),
                adjustments = Adjustments(),
                makeupFeatures = MakeupFeatures(
                    leftCheekX = 0.125f,
                    leftCheekY = 0.5f,
                    rightCheekX = 0.875f,
                    rightCheekY = 0.5f,
                    cheekRadiusX = 0.15f,
                    cheekRadiusY = 0.5f,
                    leftCheekStrength = 0f,
                    rightCheekStrength = 1f,
                    lipCenterX = 0f,
                    lipCenterY = 0f,
                    lipRadiusX = 0f,
                    lipRadiusY = 0f,
                ),
            ),
        )

        assertEquals(original, output[0])
        assertNotEquals(original, output[3])
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
