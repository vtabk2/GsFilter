package com.gsfilter.filter

data class FilterRecipe(
    val effect: FilterEffect = FilterEffect.Color,
    val isMonochrome: Boolean = false,
    val redShift: Int = 0,
    val greenShift: Int = 0,
    val blueShift: Int = 0,
    val adjustments: Adjustments = Adjustments(),
)

enum class FilterEffect(
    val jsonName: String,
    internal val shaderValue: Float,
) {
    Color(jsonName = "color", shaderValue = 0f),
    Sketch(jsonName = "sketch", shaderValue = 1f),
    Ink(jsonName = "ink", shaderValue = 2f);

    companion object {
        fun fromJsonName(name: String): FilterEffect =
            entries.firstOrNull { it.jsonName.equals(name, ignoreCase = true) } ?: Color
    }
}
