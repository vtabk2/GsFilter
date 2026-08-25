package com.gsfilter

import com.gsfilter.filter.Adjustments

enum class AdjustControl(
    val labelRes: Int,
    val minValue: Int,
    val maxValue: Int,
) {
    Brightness(R.string.brightness, -100, 100),
    Exposure(R.string.exposure, -100, 100),
    Contrast(R.string.contrast, -100, 100),
    Highlights(R.string.highlights, -100, 100),
    Shadows(R.string.shadows, -100, 100),
    Saturation(R.string.saturation, -100, 100),
    Vibrance(R.string.vibrance, -100, 100),
    Temperature(R.string.temperature, -100, 100),
    Tint(R.string.tint, -100, 100),
    Sharpness(R.string.sharpness, 0, 100),
    Clarity(R.string.clarity, -100, 100),
    Fade(R.string.fade, 0, 100),
    Vignette(R.string.vignette, 0, 100),
    Grain(R.string.grain, 0, 100);

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
