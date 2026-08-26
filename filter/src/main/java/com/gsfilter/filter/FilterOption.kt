package com.gsfilter.filter

import androidx.annotation.StringRes

data class FilterOption(
    val id: String,
    val categoryIds: Set<String>,
    @param:StringRes val nameRes: Int = 0,
    val recipe: FilterRecipe,
    val name: String? = null,
)
