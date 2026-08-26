package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object FilterBitmapRenderer {

    fun getBitmap(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int? = null,
        maxHeight: Int? = null,
    ): Bitmap {
        val renderSource = scaledSource(source, maxWidth, maxHeight)
        val width = renderSource.width
        val height = renderSource.height
        val pixels = IntArray(width * height)

        return try {
            renderSource.getPixels(pixels, 0, width, 0, 0, width, height)
            val output = renderPixels(
                pixels = pixels,
                width = width,
                height = height,
                params = ShaderFilterParams.Companion.from(recipe, adjustments),
            )
            Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
        } finally {
            recycleIfTemporary(renderSource, source)
        }
    }

    internal fun targetSize(
        width: Int,
        height: Int,
        maxWidth: Int?,
        maxHeight: Int?,
    ): RenderSize {
        require(width > 0 && height > 0) { "Source size must be positive." }
        require(maxWidth == null || maxWidth > 0) { "maxWidth must be positive." }
        require(maxHeight == null || maxHeight > 0) { "maxHeight must be positive." }

        val widthScale = maxWidth?.let { it.toFloat() / width } ?: 1f
        val heightScale = maxHeight?.let { it.toFloat() / height } ?: 1f
        val scale = min(1f, min(widthScale, heightScale))
        return RenderSize(
            width = (width * scale).roundToInt().coerceAtLeast(1),
            height = (height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    internal fun scaledSource(source: Bitmap, maxWidth: Int?, maxHeight: Int?): Bitmap {
        val size = targetSize(source.width, source.height, maxWidth, maxHeight)
        if (size.width == source.width && size.height == source.height) {
            return source
        }
        return source.scale(size.width, size.height)
    }

    internal fun recycleIfTemporary(renderSource: Bitmap, originalSource: Bitmap) {
        if (renderSource !== originalSource) {
            renderSource.recycle()
        }
    }

    internal fun renderPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
    ): IntArray {
        require(pixels.size == width * height) { "Pixel array size must match width * height." }

        val output = IntArray(pixels.size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                output[index] = filterPixel(pixels, x, y, width, height, params)
            }
        }
        return output
    }

    internal fun filterPixel(
        pixels: IntArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
    ): Int {
        val color = pixels[y * width + x]
        val left = pixels[y * width + (x - 1).coerceAtLeast(0)]
        val right = pixels[y * width + (x + 1).coerceAtMost(width - 1)]
        val up = pixels[(y - 1).coerceAtLeast(0) * width + x]
        val down = pixels[(y + 1).coerceAtMost(height - 1) * width + x]

        var red = red(color)
        var green = green(color)
        var blue = blue(color)
        val sharpAmount = (params.sharpness * 0.65f) + (params.clarity * 0.35f)

        red += (red - average(red(left), red(right), red(up), red(down))) * sharpAmount
        green += (green - average(green(left), green(right), green(up), green(down))) * sharpAmount
        blue += (blue - average(blue(left), blue(right), blue(up), blue(down))) * sharpAmount

        red += params.redShift
        green += params.greenShift
        blue += params.blueShift

        var gray = gray(red, green, blue)
        red = mix(red, gray, params.isMonochrome)
        green = mix(green, gray, params.isMonochrome)
        blue = mix(blue, gray, params.isMonochrome)

        red += params.brightness
        green += params.brightness
        blue += params.brightness

        val exposure = 2.0.pow(params.exposure.toDouble()).toFloat()
        red *= exposure
        green *= exposure
        blue *= exposure

        gray = gray(red, green, blue)
        val shadowMask = 1f - smoothstep(0f, 0.6f, gray)
        val highlightMask = smoothstep(0.4f, 1f, gray)
        red += shadowMask * params.shadows * 0.35f
        green += shadowMask * params.shadows * 0.35f
        blue += shadowMask * params.shadows * 0.35f
        red += highlightMask * params.highlights * 0.35f
        green += highlightMask * params.highlights * 0.35f
        blue += highlightMask * params.highlights * 0.35f

        red = ((red - 0.5f) * params.contrast) + 0.5f
        green = ((green - 0.5f) * params.contrast) + 0.5f
        blue = ((blue - 0.5f) * params.contrast) + 0.5f

        red += (params.temperature * 0.12f) + (params.tint * 0.06f)
        green -= params.tint * 0.08f
        blue += (-params.temperature * 0.12f) + (params.tint * 0.06f)

        gray = gray(red, green, blue)
        red = mix(gray, red, params.saturation)
        green = mix(gray, green, params.saturation)
        blue = mix(gray, blue, params.saturation)

        val maxChannel = max(max(red, green), blue)
        val channelAverage = (red + green + blue) / 3f
        val vibranceMask = 1f - clamp(maxChannel - channelAverage, 0f, 1f)
        red = mix(gray, red, 1f + (params.vibrance * vibranceMask))
        green = mix(gray, green, 1f + (params.vibrance * vibranceMask))
        blue = mix(gray, blue, 1f + (params.vibrance * vibranceMask))

        val fade = clamp(params.fade * 0.35f, 0f, 0.35f)
        red = mix(red, 0.5f, fade)
        green = mix(green, 0.5f, fade)
        blue = mix(blue, 0.5f, fade)

        val textureX = (x + 0.5f) / width
        val textureY = (y + 0.5f) / height
        val edgeDistance = sqrt(((textureX - 0.5f) * (textureX - 0.5f)) + ((textureY - 0.5f) * (textureY - 0.5f)))
        val edgeMask = smoothstep(0.35f, 0.75f, edgeDistance)
        val vignette = 1f - (params.vignette * 0.7f * edgeMask)
        red *= vignette
        green *= vignette
        blue *= vignette

        val grain = (random(textureX * 1024f, textureY * 768f) - 0.5f) * params.grain * 0.16f
        red += grain
        green += grain
        blue += grain

        return argb(alpha(color), red, green, blue)
    }

    private fun alpha(color: Int): Int = color ushr 24

    private fun red(color: Int): Float = ((color shr 16) and CHANNEL_MASK) / CHANNEL_MAX

    private fun green(color: Int): Float = ((color shr 8) and CHANNEL_MASK) / CHANNEL_MAX

    private fun blue(color: Int): Float = (color and CHANNEL_MASK) / CHANNEL_MAX

    private fun average(a: Float, b: Float, c: Float, d: Float): Float = (a + b + c + d) * 0.25f

    private fun gray(red: Float, green: Float, blue: Float): Float = (red * 0.299f) + (green * 0.587f) + (blue * 0.114f)

    private fun mix(start: Float, end: Float, amount: Float): Float = start * (1f - amount) + end * amount

    private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        val t = clamp((value - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - (2f * t))
    }

    private fun clamp(value: Float, minValue: Float, maxValue: Float): Float = min(max(value, minValue), maxValue)

    private fun random(x: Float, y: Float): Float {
        val value = sin((x * 12.9898) + (y * 78.233)) * 43758.5453
        return (value - floor(value)).toFloat()
    }

    private fun argb(alpha: Int, red: Float, green: Float, blue: Float): Int =
        (alpha shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)

    private fun channel(value: Float): Int = (clamp(value, 0f, 1f) * CHANNEL_MASK).roundToInt().coerceIn(0, CHANNEL_MASK)

    internal data class RenderSize(
        val width: Int,
        val height: Int,
    )

    private const val CHANNEL_MASK = 255
    private const val CHANNEL_MAX = 255f
}