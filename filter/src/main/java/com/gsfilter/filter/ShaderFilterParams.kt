package com.gsfilter.filter

data class ShaderFilterParams(
    val effect: FilterEffect,
    val effectStrength: Float,
    val effectThreshold: Float,
    val effectTone: Float,
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
            combineAdjustments(recipe.adjustments, adjustments).let { combined ->
                ShaderFilterParams(
                    effect = recipe.effect,
                    effectStrength = amount(recipe.effectStrength, PERCENT_MAX),
                    effectThreshold = amount(recipe.effectThreshold, PERCENT_MAX),
                    effectTone = amount(recipe.effectTone, PERCENT_MAX),
                    isMonochrome = if (recipe.isMonochrome) 1f else 0f,
                    redShift = recipe.redShift / COLOR_CHANNEL_MAX,
                    greenShift = recipe.greenShift / COLOR_CHANNEL_MAX,
                    blueShift = recipe.blueShift / COLOR_CHANNEL_MAX,
                    brightness = signed(combined.brightness, BRIGHTNESS_MAX),
                    exposure = signed(combined.exposure, SAFE_PERCENT_MAX),
                    contrast = 1f + signed(combined.contrast, SAFE_PERCENT_MAX),
                    highlights = signed(combined.highlights, SAFE_PERCENT_MAX),
                    shadows = signed(combined.shadows, SAFE_PERCENT_MAX),
                    saturation = 1f + signed(combined.saturation, SAFE_PERCENT_MAX),
                    vibrance = signed(combined.vibrance, SAFE_PERCENT_MAX),
                    temperature = signed(combined.temperature, SAFE_PERCENT_MAX),
                    tint = signed(combined.tint, SAFE_PERCENT_MAX),
                    sharpness = amount(combined.sharpness, SAFE_PERCENT_MAX),
                    clarity = signed(combined.clarity, SAFE_PERCENT_MAX),
                    fade = amount(combined.fade, PERCENT_MAX),
                    vignette = amount(combined.vignette, PERCENT_MAX),
                    grain = amount(combined.grain, PERCENT_MAX),
                )
            }

        private fun combineAdjustments(preset: Adjustments, user: Adjustments): Adjustments =
            Adjustments(
                brightness = preset.brightness + user.brightness,
                exposure = preset.exposure + user.exposure,
                contrast = preset.contrast + user.contrast,
                highlights = preset.highlights + user.highlights,
                shadows = preset.shadows + user.shadows,
                saturation = preset.saturation + user.saturation,
                vibrance = preset.vibrance + user.vibrance,
                temperature = preset.temperature + user.temperature,
                tint = preset.tint + user.tint,
                sharpness = preset.sharpness + user.sharpness,
                clarity = preset.clarity + user.clarity,
                fade = preset.fade + user.fade,
                vignette = preset.vignette + user.vignette,
                grain = preset.grain + user.grain,
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
