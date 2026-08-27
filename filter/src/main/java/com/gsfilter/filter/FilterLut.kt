package com.gsfilter.filter

enum class FilterLut(val jsonName: String) {
    None("none"),
    CleanPortrait("clean_portrait"),
    SoftSkin("soft_skin"),
    GoldenPortrait("golden_portrait"),
    DaylightFresh("daylight_fresh"),
    FoodPop("food_pop"),
    GreenFilm("green_film"),
    TealCinema("teal_cinema"),
    NightMood("night_mood"),
    VintageFade("vintage_fade"),
    EditorialMatte("editorial_matte");

    internal fun apply(red: Float, green: Float, blue: Float, out: FloatArray) {
        when (this) {
            None -> out.set(red, green, blue)
            CleanPortrait -> grade(
                red,
                green,
                blue,
                contrast = 1.08f,
                saturation = 0.93f,
                shiftRed = 0.045f,
                shiftGreen = 0.025f,
                shiftBlue = -0.025f,
                fade = 0.04f,
                highlightRed = 0.040f,
                highlightGreen = 0.018f,
                out = out,
            )
            SoftSkin -> grade(
                red,
                green,
                blue,
                contrast = 0.82f,
                saturation = 0.78f,
                shiftRed = 0.060f,
                shiftGreen = 0.020f,
                shiftBlue = -0.025f,
                fade = 0.10f,
                highlightRed = 0.045f,
                highlightGreen = 0.018f,
                out = out,
            )
            GoldenPortrait -> grade(
                red,
                green,
                blue,
                contrast = 1.12f,
                saturation = 1.10f,
                shiftRed = 0.075f,
                shiftGreen = 0.030f,
                shiftBlue = -0.060f,
                shadowRed = 0.035f,
                shadowBlue = -0.025f,
                highlightRed = 0.055f,
                highlightGreen = 0.025f,
                highlightBlue = -0.035f,
                out = out,
            )
            DaylightFresh -> grade(
                red,
                green,
                blue,
                contrast = 1.10f,
                saturation = 1.12f,
                shiftRed = -0.025f,
                shiftGreen = 0.035f,
                shiftBlue = 0.055f,
                shadowBlue = 0.015f,
                highlightBlue = 0.035f,
                out = out,
            )
            FoodPop -> grade(
                red,
                green,
                blue,
                contrast = 1.06f,
                saturation = 1.06f,
                shiftRed = 0.055f,
                shiftGreen = 0.030f,
                shiftBlue = -0.030f,
                shadowRed = 0.010f,
                shadowGreen = 0.006f,
                highlightRed = 0.018f,
                highlightGreen = 0.004f,
                highlightBlue = -0.010f,
                out = out,
            )
            GreenFilm -> grade(
                red,
                green,
                blue,
                contrast = 0.98f,
                saturation = 0.82f,
                shiftRed = -0.055f,
                shiftGreen = 0.080f,
                shiftBlue = -0.015f,
                fade = 0.14f,
                shadowGreen = 0.055f,
                shadowBlue = 0.035f,
                highlightRed = -0.025f,
                highlightGreen = 0.030f,
                out = out,
            )
            TealCinema -> grade(
                red,
                green,
                blue,
                contrast = 1.28f,
                saturation = 0.85f,
                shiftRed = -0.040f,
                shiftGreen = 0.020f,
                shiftBlue = 0.060f,
                fade = 0.04f,
                shadowRed = -0.090f,
                shadowGreen = 0.035f,
                shadowBlue = 0.120f,
                highlightRed = 0.100f,
                highlightGreen = 0.035f,
                highlightBlue = -0.060f,
                out = out,
            )
            NightMood -> grade(
                red,
                green,
                blue,
                contrast = 1.34f,
                saturation = 0.78f,
                shiftRed = -0.055f,
                shiftBlue = 0.080f,
                shadowRed = -0.080f,
                shadowBlue = 0.130f,
                highlightRed = 0.040f,
                highlightBlue = -0.040f,
                out = out,
            )
            VintageFade -> grade(
                red,
                green,
                blue,
                contrast = 0.88f,
                saturation = 0.72f,
                shiftRed = 0.080f,
                shiftGreen = 0.040f,
                shiftBlue = -0.070f,
                fade = 0.20f,
                shadowRed = 0.050f,
                shadowGreen = 0.025f,
                highlightRed = 0.050f,
                highlightGreen = 0.035f,
                highlightBlue = -0.030f,
                out = out,
            )
            EditorialMatte -> grade(
                red,
                green,
                blue,
                contrast = 0.90f,
                saturation = 0.65f,
                shiftRed = -0.025f,
                shiftGreen = 0.020f,
                shiftBlue = 0.050f,
                fade = 0.18f,
                shadowRed = -0.040f,
                shadowGreen = 0.030f,
                shadowBlue = 0.080f,
                highlightRed = 0.035f,
                highlightGreen = 0.025f,
                highlightBlue = -0.020f,
                out = out,
            )
        }
    }

    companion object {
        fun fromJsonName(name: String): FilterLut =
            entries.firstOrNull { it.jsonName.equals(name, ignoreCase = true) } ?: None

        private fun grade(
            red: Float,
            green: Float,
            blue: Float,
            contrast: Float,
            saturation: Float,
            shiftRed: Float = 0f,
            shiftGreen: Float = 0f,
            shiftBlue: Float = 0f,
            fade: Float = 0f,
            shadowRed: Float = 0f,
            shadowGreen: Float = 0f,
            shadowBlue: Float = 0f,
            highlightRed: Float = 0f,
            highlightGreen: Float = 0f,
            highlightBlue: Float = 0f,
            out: FloatArray,
        ) {
            val gray = gray(red, green, blue)
            val shadowMask = 1f - smoothstep(0f, 0.58f, gray)
            val highlightMask = smoothstep(0.42f, 1f, gray)
            var nextRed = red + shiftRed + (shadowMask * shadowRed) + (highlightMask * highlightRed)
            var nextGreen = green + shiftGreen + (shadowMask * shadowGreen) + (highlightMask * highlightGreen)
            var nextBlue = blue + shiftBlue + (shadowMask * shadowBlue) + (highlightMask * highlightBlue)

            nextRed = ((nextRed - 0.5f) * contrast) + 0.5f
            nextGreen = ((nextGreen - 0.5f) * contrast) + 0.5f
            nextBlue = ((nextBlue - 0.5f) * contrast) + 0.5f

            val nextGray = gray(nextRed, nextGreen, nextBlue)
            nextRed = mix(nextGray, nextRed, saturation)
            nextGreen = mix(nextGray, nextGreen, saturation)
            nextBlue = mix(nextGray, nextBlue, saturation)

            out.set(
                mix(nextRed, 0.5f, fade).coerceIn(0f, 1f),
                mix(nextGreen, 0.5f, fade).coerceIn(0f, 1f),
                mix(nextBlue, 0.5f, fade).coerceIn(0f, 1f),
            )
        }

        private fun FloatArray.set(red: Float, green: Float, blue: Float) {
            this[0] = red
            this[1] = green
            this[2] = blue
        }

        private fun gray(red: Float, green: Float, blue: Float): Float =
            (red * 0.299f) + (green * 0.587f) + (blue * 0.114f)

        private fun mix(start: Float, end: Float, amount: Float): Float =
            start * (1f - amount) + end * amount

        private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
            val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
            return t * t * (3f - (2f * t))
        }
    }
}
