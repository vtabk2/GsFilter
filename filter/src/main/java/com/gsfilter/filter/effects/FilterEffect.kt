package com.gsfilter.filter.effects

enum class FilterEffect(
    val jsonName: String,
    internal val shaderValue: Float,
) {
    Color(jsonName = "color", shaderValue = 0f),
    Sketch(jsonName = "sketch", shaderValue = 1f),
    Ink(jsonName = "ink", shaderValue = 2f),
    Pencil(jsonName = "pencil", shaderValue = 3f),
    ColorPencil(jsonName = "color_pencil", shaderValue = 4f),
    Charcoal(jsonName = "charcoal", shaderValue = 5f);

    companion object {
        fun fromJsonName(name: String): FilterEffect =
            entries.firstOrNull { it.jsonName.equals(name, ignoreCase = true) } ?: Color
    }
}
