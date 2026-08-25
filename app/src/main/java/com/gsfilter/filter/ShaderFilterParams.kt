package com.gsfilter.filter

data class ShaderFilterParams(
    val isMonochrome: Float,
    val redShift: Float,
    val greenShift: Float,
    val blueShift: Float,
    val brightness: Float,
    val exposure: Float,
    val contrast: Float,
    val highlights: Float,
    val shadows: Float,
    val saturation: Float,
    val vibrance: Float,
    val temperature: Float,
    val tint: Float,
    val sharpness: Float,
    val clarity: Float,
    val fade: Float,
    val vignette: Float,
    val grain: Float,
) {
    companion object {
        fun from(recipe: FilterRecipe, adjustments: Adjustments): ShaderFilterParams =
            ShaderFilterParams(
                isMonochrome = if (recipe.isMonochrome) 1f else 0f,
                redShift = recipe.redShift / COLOR_CHANNEL_MAX,
                greenShift = recipe.greenShift / COLOR_CHANNEL_MAX,
                blueShift = recipe.blueShift / COLOR_CHANNEL_MAX,
                brightness = signed(adjustments.brightness, BRIGHTNESS_MAX),
                exposure = signed(adjustments.exposure, SAFE_PERCENT_MAX),
                contrast = 1f + signed(adjustments.contrast, SAFE_PERCENT_MAX),
                highlights = signed(adjustments.highlights, SAFE_PERCENT_MAX),
                shadows = signed(adjustments.shadows, SAFE_PERCENT_MAX),
                saturation = 1f + signed(adjustments.saturation, SAFE_PERCENT_MAX),
                vibrance = signed(adjustments.vibrance, SAFE_PERCENT_MAX),
                temperature = signed(adjustments.temperature, SAFE_PERCENT_MAX),
                tint = signed(adjustments.tint, SAFE_PERCENT_MAX),
                sharpness = amount(adjustments.sharpness, SAFE_PERCENT_MAX),
                clarity = signed(adjustments.clarity, SAFE_PERCENT_MAX),
                fade = amount(adjustments.fade, PERCENT_MAX),
                vignette = amount(adjustments.vignette, PERCENT_MAX),
                grain = amount(adjustments.grain, PERCENT_MAX),
            )

        private fun signed(value: Int, divisor: Float): Float =
            value.coerceIn(SIGNED_MIN, SIGNED_MAX) / divisor

        private fun amount(value: Int, divisor: Float): Float =
            value.coerceIn(AMOUNT_MIN, AMOUNT_MAX) / divisor

        private const val COLOR_CHANNEL_MAX = 255f
        private const val BRIGHTNESS_MAX = 400f
        private const val PERCENT_MAX = 100f
        private const val SAFE_PERCENT_MAX = 200f
        private const val SIGNED_MIN = -100
        private const val SIGNED_MAX = 100
        private const val AMOUNT_MIN = 0
        private const val AMOUNT_MAX = 100
    }
}
