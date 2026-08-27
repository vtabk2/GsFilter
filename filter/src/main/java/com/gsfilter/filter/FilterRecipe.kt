package com.gsfilter.filter

data class FilterRecipe(
    val effect: FilterEffect = FilterEffect.Color,
    val effectStrength: Int = 100,
    val effectThreshold: Int = 50,
    val effectTone: Int = 20,
    val lut: FilterLut = FilterLut.None,
    val lutStrength: Int = 100,
    val intensity: Int = 100,
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
    Ink(jsonName = "ink", shaderValue = 2f),
    Pencil(jsonName = "pencil", shaderValue = 3f),
    ColorPencil(jsonName = "color_pencil", shaderValue = 4f),
    Charcoal(jsonName = "charcoal", shaderValue = 5f),
    CrossHatch(jsonName = "cross_hatch", shaderValue = 6f);

    companion object {
        fun fromJsonName(name: String): FilterEffect =
            entries.firstOrNull { it.jsonName.equals(name, ignoreCase = true) } ?: Color
    }
}
