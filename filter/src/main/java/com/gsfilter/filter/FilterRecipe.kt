package com.gsfilter.filter

data class FilterRecipe(
    val isMonochrome: Boolean = false,
    val redShift: Int = 0,
    val greenShift: Int = 0,
    val blueShift: Int = 0,
    val adjustments: Adjustments = Adjustments(),
)
