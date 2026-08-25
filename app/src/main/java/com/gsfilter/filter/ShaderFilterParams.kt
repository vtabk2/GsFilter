package com.gsfilter.filter

data class ShaderFilterParams(
    val isMonochrome: Float,
    val redShift: Float,
    val greenShift: Float,
    val blueShift: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
) {
    companion object {
        fun from(recipe: FilterRecipe, adjustments: Adjustments): ShaderFilterParams =
            ShaderFilterParams(
                isMonochrome = if (recipe.isMonochrome) 1f else 0f,
                redShift = recipe.redShift / COLOR_CHANNEL_MAX,
                greenShift = recipe.greenShift / COLOR_CHANNEL_MAX,
                blueShift = recipe.blueShift / COLOR_CHANNEL_MAX,
                brightness = adjustments.brightness / COLOR_CHANNEL_MAX,
                contrast = adjustments.contrast / PERCENT_MAX,
                saturation = adjustments.saturation / PERCENT_MAX,
            )

        private const val COLOR_CHANNEL_MAX = 255f
        private const val PERCENT_MAX = 100f
    }
}
