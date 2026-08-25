package com.gsfilter.filter

data class FilterOption(
    val id: String,
    val categoryIds: Set<String>,
    val nameRes: Int,
    val recipe: FilterRecipe,
)
