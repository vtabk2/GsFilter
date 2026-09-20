package com.gsfilter

import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterPack

internal object AppFilterCatalog {

    private const val SKETCH_CATEGORY = "sketch"
    private const val PAINTING_ART_CATEGORY = "painting_art"

    val sketchFilterIds: List<String> = listOf(
        "pencil",
        "soft_pencil",
        "hard_pencil",
        "graphite",
        "charcoal",
        "ink",
        "black_white_sketch",
        "color_pencil",
        "cross_hatch",
        "fine_line",
        "blueprint",
        "chalk",
    )

    val paintingArtFilterIds: List<String> = listOf(
        "oil_painting",
        "watercolor",
        "canvas_painting",
        "pastel",
        "comic",
        "pop_art",
        "vintage_painting",
        "impression_style",
    )

    val filterIds: List<String> = sketchFilterIds + paintingArtFilterIds

    private val categories = listOf(
        FilterCategory(
            id = SKETCH_CATEGORY,
            nameRes = R.string.category_sketch,
        ),
        FilterCategory(
            id = PAINTING_ART_CATEGORY,
            nameRes = R.string.category_painting_art,
        ),
    )

    val pack: FilterPack = FilterPack(
        categories = categories,
        options = listOf(FilterCatalog.default) +
            optionsFor(sketchFilterIds, SKETCH_CATEGORY) +
            optionsFor(paintingArtFilterIds, PAINTING_ART_CATEGORY),
        defaultCategory = categories.first(),
        defaultFilter = FilterCatalog.default,
    )

    private fun optionsFor(filterIds: List<String>, categoryId: String) =
        filterIds.map { id ->
            FilterCatalog.options.first { it.id == id }.copy(categoryIds = setOf(categoryId))
        }
}
