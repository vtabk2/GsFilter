package com.gsfilter

import com.gsfilter.filter.FilterOption
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.FilterCategory

object FilterCatalog {

    val categories: List<FilterCategory> = listOf(
        FilterCategory(id = POPULAR, nameRes = R.string.category_popular),
        FilterCategory(id = NATURAL, nameRes = R.string.category_natural),
        FilterCategory(id = FILM, nameRes = R.string.category_film),
        FilterCategory(id = VINTAGE, nameRes = R.string.category_vintage),
        FilterCategory(id = WARM, nameRes = R.string.category_warm),
        FilterCategory(id = COOL, nameRes = R.string.category_cool),
        FilterCategory(id = PORTRAIT, nameRes = R.string.category_portrait),
        FilterCategory(id = FOOD, nameRes = R.string.category_food),
        FilterCategory(id = CINEMATIC, nameRes = R.string.category_cinematic),
        FilterCategory(id = BLACK_WHITE, nameRes = R.string.category_black_white),
    )

    val options: List<FilterOption> = listOf(
        FilterOption(
            id = "original",
            categoryIds = setOf(POPULAR, NATURAL),
            nameRes = R.string.action_original,
            recipe = FilterRecipe(),
        ),
        FilterOption(
            id = "fresh",
            categoryIds = setOf(POPULAR, NATURAL),
            nameRes = R.string.filter_fresh,
            recipe = FilterRecipe(redShift = 4, greenShift = 6, blueShift = 4),
        ),
        FilterOption(
            id = "portra",
            categoryIds = setOf(FILM, PORTRAIT),
            nameRes = R.string.filter_portra,
            recipe = FilterRecipe(redShift = 10, greenShift = 3, blueShift = -4),
        ),
        FilterOption(
            id = "retro",
            categoryIds = setOf(VINTAGE),
            nameRes = R.string.filter_retro,
            recipe = FilterRecipe(redShift = 12, greenShift = -4, blueShift = -12),
        ),
        FilterOption(
            id = "warm",
            categoryIds = setOf(POPULAR, WARM),
            nameRes = R.string.filter_warm,
            recipe = FilterRecipe(redShift = 18, greenShift = 6, blueShift = -10),
        ),
        FilterOption(
            id = "cool",
            categoryIds = setOf(COOL),
            nameRes = R.string.filter_cool,
            recipe = FilterRecipe(redShift = -8, greenShift = 4, blueShift = 18),
        ),
        FilterOption(
            id = "skin",
            categoryIds = setOf(PORTRAIT),
            nameRes = R.string.filter_skin,
            recipe = FilterRecipe(redShift = 12, greenShift = 2, blueShift = -6),
        ),
        FilterOption(
            id = "tasty",
            categoryIds = setOf(POPULAR, FOOD),
            nameRes = R.string.filter_tasty,
            recipe = FilterRecipe(redShift = 14, greenShift = 8, blueShift = -4),
        ),
        FilterOption(
            id = "cinema",
            categoryIds = setOf(CINEMATIC),
            nameRes = R.string.filter_cinema,
            recipe = FilterRecipe(redShift = -8, greenShift = 8, blueShift = 10),
        ),
        FilterOption(
            id = "mono",
            categoryIds = setOf(POPULAR, BLACK_WHITE),
            nameRes = R.string.filter_mono,
            recipe = FilterRecipe(isMonochrome = true),
        ),
    )

    val defaultCategory: FilterCategory = categories.first()
    val default: FilterOption = filtersForCategory(defaultCategory.id).first()

    fun filtersForCategory(categoryId: String): List<FilterOption> =
        options.filter { categoryId in it.categoryIds }

    private const val POPULAR = "popular"
    private const val NATURAL = "natural"
    private const val FILM = "film"
    private const val VINTAGE = "vintage"
    private const val WARM = "warm"
    private const val COOL = "cool"
    private const val PORTRAIT = "portrait"
    private const val FOOD = "food"
    private const val CINEMATIC = "cinematic"
    private const val BLACK_WHITE = "black_white"
}
