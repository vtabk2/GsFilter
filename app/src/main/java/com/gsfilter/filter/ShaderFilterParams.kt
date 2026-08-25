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
                brightness = adjustments.brightness / COLOR_CHANNEL_MAX,
                exposure = adjustments.exposure / PERCENT_MAX,
                contrast = 1f + adjustments.contrast / PERCENT_MAX,
                highlights = adjustments.highlights / PERCENT_MAX,
                shadows = adjustments.shadows / PERCENT_MAX,
                saturation = 1f + adjustments.saturation / PERCENT_MAX,
                vibrance = adjustments.vibrance / PERCENT_MAX,
                temperature = adjustments.temperature / PERCENT_MAX,
                tint = adjustments.tint / PERCENT_MAX,
                sharpness = adjustments.sharpness / PERCENT_MAX,
                clarity = adjustments.clarity / PERCENT_MAX,
                fade = adjustments.fade / PERCENT_MAX,
                vignette = adjustments.vignette / PERCENT_MAX,
                grain = adjustments.grain / PERCENT_MAX,
            )

        private const val COLOR_CHANNEL_MAX = 255f
        private const val PERCENT_MAX = 100f
    }
}
