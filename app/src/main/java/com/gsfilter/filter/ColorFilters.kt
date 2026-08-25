package com.gsfilter.filter

import kotlin.math.roundToInt

object ColorFilters {

    fun apply(
        argb: Int,
        recipe: FilterRecipe,
        adjustments: Adjustments,
    ): Int {
        val alpha = (argb ushr 24) and CHANNEL_MASK
        var red = (argb ushr 16) and CHANNEL_MASK
        var green = (argb ushr 8) and CHANNEL_MASK
        var blue = argb and CHANNEL_MASK

        if (recipe.isMonochrome) {
            val gray = luminance(red, green, blue)
            red = gray
            green = gray
            blue = gray
        }

        red += recipe.redShift
        green += recipe.greenShift
        blue += recipe.blueShift

        red = applyContrast(red + adjustments.brightness, adjustments.contrast)
        green = applyContrast(green + adjustments.brightness, adjustments.contrast)
        blue = applyContrast(blue + adjustments.brightness, adjustments.contrast)

        val gray = luminance(red, green, blue)
        red = mix(gray, red, adjustments.saturation)
        green = mix(gray, green, adjustments.saturation)
        blue = mix(gray, blue, adjustments.saturation)

        return (alpha shl 24) or
            (red.coerceToChannel() shl 16) or
            (green.coerceToChannel() shl 8) or
            blue.coerceToChannel()
    }

    private fun applyContrast(channel: Int, contrast: Int): Int =
        (((channel.coerceToChannel() - CHANNEL_CENTER) * (contrast / 100f)) + CHANNEL_CENTER)
            .roundToInt()
            .coerceToChannel()

    private fun luminance(red: Int, green: Int, blue: Int): Int =
        (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT).roundToInt()

    private fun mix(from: Int, to: Int, percent: Int): Int =
        (from + (to - from) * (percent / 100f)).roundToInt().coerceToChannel()

    private fun Int.coerceToChannel(): Int = coerceIn(0, CHANNEL_MASK)

    private const val CHANNEL_MASK = 255
    private const val CHANNEL_CENTER = 128
    private const val RED_WEIGHT = 0.299f
    private const val GREEN_WEIGHT = 0.587f
    private const val BLUE_WEIGHT = 0.114f
}
