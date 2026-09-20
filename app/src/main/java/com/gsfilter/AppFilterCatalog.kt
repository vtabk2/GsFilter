package com.gsfilter

import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterPack

internal object AppFilterCatalog {

    val filterIds: List<String> = listOf(
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
        "oil_painting",
        "watercolor",
        "canvas_painting",
        "pastel",
        "comic",
        "pop_art",
        "vintage_painting",
        "impression_style",
    )

    val pack: FilterPack = FilterCatalog.packFor(filterIds)
}
