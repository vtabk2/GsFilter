package com.gsfilter

import com.gsfilter.filter.FilterOption
import com.gsfilter.filter.FilterRecipe

object FilterCatalog {

    val options: List<FilterOption> = listOf(
        FilterOption(
            id = "original",
            nameRes = R.string.action_original,
            recipe = FilterRecipe(),
        ),
        FilterOption(
            id = "mono",
            nameRes = R.string.filter_mono,
            recipe = FilterRecipe(isMonochrome = true),
        ),
        FilterOption(
            id = "warm",
            nameRes = R.string.filter_warm,
            recipe = FilterRecipe(redShift = 18, greenShift = 6, blueShift = -10),
        ),
        FilterOption(
            id = "cool",
            nameRes = R.string.filter_cool,
            recipe = FilterRecipe(redShift = -8, greenShift = 4, blueShift = 18),
        ),
    )

    val default: FilterOption = options.first()
}
