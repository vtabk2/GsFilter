package com.gsfilter.filter

enum class AdjustControl(
    val labelRes: Int,
    val iconRes: Int,
    val minValue: Int,
    val maxValue: Int,
) {
    Brightness(R.string.gs_brightness, R.drawable.ic_gs_adjust_brightness, -100, 100),
    Exposure(R.string.gs_exposure, R.drawable.ic_gs_adjust_exposure, -100, 100),
    Contrast(R.string.gs_contrast, R.drawable.ic_gs_adjust_contrast, -100, 100),
    Highlights(R.string.gs_highlights, R.drawable.ic_gs_adjust_highlights, -100, 100),
    Shadows(R.string.gs_shadows, R.drawable.ic_gs_adjust_shadows, -100, 100),
    Saturation(R.string.gs_saturation, R.drawable.ic_gs_adjust_saturation, -100, 100),
    Vibrance(R.string.gs_vibrance, R.drawable.ic_gs_adjust_vibrance, -100, 100),
    Temperature(R.string.gs_temperature, R.drawable.ic_gs_adjust_temperature, -100, 100),
    Tint(R.string.gs_tint, R.drawable.ic_gs_adjust_tint, -100, 100),
    Sharpness(R.string.gs_sharpness, R.drawable.ic_gs_adjust_sharpness, 0, 100),
    Clarity(R.string.gs_clarity, R.drawable.ic_gs_adjust_clarity, -100, 100),
    Fade(R.string.gs_fade, R.drawable.ic_gs_adjust_fade, 0, 100),
    Vignette(R.string.gs_vignette, R.drawable.ic_gs_adjust_vignette, 0, 100),
    Grain(R.string.gs_grain, R.drawable.ic_gs_adjust_grain, 0, 100);

    val progressMax: Int = maxValue - minValue

    fun valueFrom(progress: Int): Int =
        (progress + minValue).coerceIn(minValue, maxValue)

    fun progressFrom(value: Int): Int =
        value.coerceIn(minValue, maxValue) - minValue

    fun valueIn(adjustments: Adjustments): Int =
        when (this) {
            Brightness -> adjustments.brightness
            Exposure -> adjustments.exposure
            Contrast -> adjustments.contrast
            Highlights -> adjustments.highlights
            Shadows -> adjustments.shadows
            Saturation -> adjustments.saturation
            Vibrance -> adjustments.vibrance
            Temperature -> adjustments.temperature
            Tint -> adjustments.tint
            Sharpness -> adjustments.sharpness
            Clarity -> adjustments.clarity
            Fade -> adjustments.fade
            Vignette -> adjustments.vignette
            Grain -> adjustments.grain
        }

    fun update(adjustments: Adjustments, value: Int): Adjustments {
        val nextValue = value.coerceIn(minValue, maxValue)
        return when (this) {
            Brightness -> adjustments.copy(brightness = nextValue)
            Exposure -> adjustments.copy(exposure = nextValue)
            Contrast -> adjustments.copy(contrast = nextValue)
            Highlights -> adjustments.copy(highlights = nextValue)
            Shadows -> adjustments.copy(shadows = nextValue)
            Saturation -> adjustments.copy(saturation = nextValue)
            Vibrance -> adjustments.copy(vibrance = nextValue)
            Temperature -> adjustments.copy(temperature = nextValue)
            Tint -> adjustments.copy(tint = nextValue)
            Sharpness -> adjustments.copy(sharpness = nextValue)
            Clarity -> adjustments.copy(clarity = nextValue)
            Fade -> adjustments.copy(fade = nextValue)
            Vignette -> adjustments.copy(vignette = nextValue)
            Grain -> adjustments.copy(grain = nextValue)
        }
    }
}
