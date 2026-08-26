package com.gsfilter.filter

object FilterCatalog {

    val categories: List<FilterCategory> = listOf(
        FilterCategory(id = POPULAR, nameRes = R.string.gs_category_popular),
        FilterCategory(id = NATURAL, nameRes = R.string.gs_category_natural),
        FilterCategory(id = FILM, nameRes = R.string.gs_category_film),
        FilterCategory(id = VINTAGE, nameRes = R.string.gs_category_vintage),
        FilterCategory(id = WARM, nameRes = R.string.gs_category_warm),
        FilterCategory(id = COOL, nameRes = R.string.gs_category_cool),
        FilterCategory(id = PORTRAIT, nameRes = R.string.gs_category_portrait),
        FilterCategory(id = FOOD, nameRes = R.string.gs_category_food),
        FilterCategory(id = CINEMATIC, nameRes = R.string.gs_category_cinematic),
        FilterCategory(id = BLACK_WHITE, nameRes = R.string.gs_category_black_white),
    )

    val options: List<FilterOption> = listOf(
        FilterOption(
            id = ORIGINAL,
            categoryIds = emptySet(),
            nameRes = R.string.gs_action_original,
            recipe = FilterRecipe(),
        ),
        FilterOption(
            id = "fresh",
            categoryIds = setOf(POPULAR, NATURAL),
            nameRes = R.string.gs_filter_fresh,
            recipe = FilterRecipe(
                redShift = 4,
                greenShift = 6,
                blueShift = 4,
                adjustments = Adjustments(
                    exposure = 6,
                    contrast = 4,
                    saturation = 8,
                    vibrance = 12,
                    clarity = 4,
                ),
            ),
        ),
        FilterOption(
            id = "portra",
            categoryIds = setOf(FILM, PORTRAIT),
            nameRes = R.string.gs_filter_portra,
            recipe = FilterRecipe(
                redShift = 10,
                greenShift = 3,
                blueShift = -4,
                adjustments = Adjustments(
                    brightness = 4,
                    contrast = -6,
                    highlights = -12,
                    shadows = 8,
                    saturation = -6,
                    temperature = 6,
                    tint = 4,
                    sharpness = 8,
                ),
            ),
        ),
        FilterOption(
            id = "retro",
            categoryIds = setOf(VINTAGE),
            nameRes = R.string.gs_filter_retro,
            recipe = FilterRecipe(
                redShift = 12,
                greenShift = -4,
                blueShift = -12,
                adjustments = Adjustments(
                    contrast = -12,
                    saturation = -18,
                    temperature = 12,
                    fade = 20,
                    vignette = 10,
                    grain = 14,
                ),
            ),
        ),
        FilterOption(
            id = "warm",
            categoryIds = setOf(POPULAR, WARM),
            nameRes = R.string.gs_filter_warm,
            recipe = FilterRecipe(
                redShift = 18,
                greenShift = 6,
                blueShift = -10,
                adjustments = Adjustments(
                    exposure = 4,
                    contrast = 6,
                    saturation = 8,
                    temperature = 18,
                ),
            ),
        ),
        FilterOption(
            id = "cool",
            categoryIds = setOf(COOL),
            nameRes = R.string.gs_filter_cool,
            recipe = FilterRecipe(
                redShift = -8,
                greenShift = 4,
                blueShift = 18,
                adjustments = Adjustments(
                    contrast = 4,
                    saturation = -4,
                    temperature = -18,
                    tint = -6,
                    clarity = 8,
                ),
            ),
        ),
        FilterOption(
            id = "skin",
            categoryIds = setOf(PORTRAIT),
            nameRes = R.string.gs_filter_skin,
            recipe = FilterRecipe(
                redShift = 12,
                greenShift = 2,
                blueShift = -6,
                adjustments = Adjustments(
                    brightness = 4,
                    contrast = -8,
                    highlights = -10,
                    shadows = 6,
                    saturation = -4,
                    temperature = 8,
                    tint = 5,
                    sharpness = 4,
                ),
            ),
        ),
        FilterOption(
            id = "tasty",
            categoryIds = setOf(POPULAR, FOOD),
            nameRes = R.string.gs_filter_tasty,
            recipe = FilterRecipe(
                redShift = 14,
                greenShift = 8,
                blueShift = -4,
                adjustments = Adjustments(
                    contrast = 8,
                    saturation = 16,
                    vibrance = 14,
                    temperature = 8,
                    clarity = 8,
                ),
            ),
        ),
        FilterOption(
            id = "cinema",
            categoryIds = setOf(CINEMATIC),
            nameRes = R.string.gs_filter_cinema,
            recipe = FilterRecipe(
                redShift = -8,
                greenShift = 8,
                blueShift = 10,
                adjustments = Adjustments(
                    exposure = -4,
                    contrast = 18,
                    highlights = -18,
                    shadows = -8,
                    saturation = -8,
                    clarity = 10,
                    vignette = 18,
                ),
            ),
        ),
        FilterOption(
            id = "mono",
            categoryIds = setOf(POPULAR, BLACK_WHITE),
            nameRes = R.string.gs_filter_mono,
            recipe = FilterRecipe(
                isMonochrome = true,
                adjustments = Adjustments(
                    contrast = 14,
                    clarity = 8,
                    grain = 6,
                ),
            ),
        ),
        FilterOption(
            id = "clear",
            categoryIds = setOf(POPULAR, NATURAL),
            nameRes = R.string.gs_filter_clear,
            recipe = FilterRecipe(
                redShift = 2,
                greenShift = 4,
                blueShift = 3,
                adjustments = Adjustments(
                    exposure = 4,
                    contrast = 6,
                    highlights = -8,
                    shadows = 6,
                    vibrance = 8,
                    clarity = 6,
                ),
            ),
        ),
        FilterOption(
            id = "airy",
            categoryIds = setOf(NATURAL),
            nameRes = R.string.gs_filter_airy,
            recipe = FilterRecipe(
                redShift = 4,
                greenShift = 4,
                blueShift = 6,
                adjustments = Adjustments(
                    brightness = 6,
                    exposure = 6,
                    contrast = -10,
                    highlights = -10,
                    saturation = -6,
                    fade = 8,
                ),
            ),
        ),
        FilterOption(
            id = "fuji",
            categoryIds = setOf(FILM),
            nameRes = R.string.gs_filter_fuji,
            recipe = FilterRecipe(
                redShift = 2,
                greenShift = 8,
                blueShift = 6,
                adjustments = Adjustments(
                    contrast = 10,
                    highlights = -14,
                    shadows = 6,
                    saturation = 8,
                    temperature = -4,
                    grain = 8,
                ),
            ),
        ),
        FilterOption(
            id = "gold",
            categoryIds = setOf(POPULAR, FILM, WARM),
            nameRes = R.string.gs_filter_gold,
            recipe = FilterRecipe(
                redShift = 16,
                greenShift = 8,
                blueShift = -8,
                adjustments = Adjustments(
                    contrast = 8,
                    highlights = -8,
                    saturation = 10,
                    temperature = 16,
                    fade = 6,
                    grain = 6,
                ),
            ),
        ),
        FilterOption(
            id = "classic",
            categoryIds = setOf(VINTAGE),
            nameRes = R.string.gs_filter_classic,
            recipe = FilterRecipe(
                redShift = 10,
                greenShift = 0,
                blueShift = -10,
                adjustments = Adjustments(
                    contrast = -8,
                    saturation = -14,
                    temperature = 8,
                    tint = 4,
                    fade = 16,
                    vignette = 12,
                    grain = 10,
                ),
            ),
        ),
        FilterOption(
            id = "honey",
            categoryIds = setOf(WARM),
            nameRes = R.string.gs_filter_honey,
            recipe = FilterRecipe(
                redShift = 20,
                greenShift = 8,
                blueShift = -12,
                adjustments = Adjustments(
                    exposure = 6,
                    contrast = 4,
                    saturation = 8,
                    temperature = 22,
                    highlights = -6,
                ),
            ),
        ),
        FilterOption(
            id = "arctic",
            categoryIds = setOf(COOL),
            nameRes = R.string.gs_filter_arctic,
            recipe = FilterRecipe(
                redShift = -12,
                greenShift = 2,
                blueShift = 22,
                adjustments = Adjustments(
                    exposure = 2,
                    contrast = 8,
                    saturation = -8,
                    temperature = -24,
                    clarity = 10,
                ),
            ),
        ),
        FilterOption(
            id = "peach",
            categoryIds = setOf(PORTRAIT),
            nameRes = R.string.gs_filter_peach,
            recipe = FilterRecipe(
                redShift = 14,
                greenShift = 4,
                blueShift = -8,
                adjustments = Adjustments(
                    brightness = 4,
                    contrast = -10,
                    highlights = -8,
                    saturation = -4,
                    temperature = 10,
                    tint = 8,
                    sharpness = 4,
                ),
            ),
        ),
        FilterOption(
            id = "crispy",
            categoryIds = setOf(FOOD),
            nameRes = R.string.gs_filter_crispy,
            recipe = FilterRecipe(
                redShift = 10,
                greenShift = 10,
                blueShift = -4,
                adjustments = Adjustments(
                    contrast = 14,
                    saturation = 18,
                    vibrance = 18,
                    clarity = 14,
                    sharpness = 10,
                ),
            ),
        ),
        FilterOption(
            id = "teal_orange",
            categoryIds = setOf(POPULAR, CINEMATIC),
            nameRes = R.string.gs_filter_teal_orange,
            recipe = FilterRecipe(
                redShift = 12,
                greenShift = 4,
                blueShift = 8,
                adjustments = Adjustments(
                    exposure = -4,
                    contrast = 22,
                    highlights = -16,
                    shadows = -10,
                    saturation = -6,
                    temperature = 8,
                    tint = -8,
                    clarity = 12,
                    vignette = 20,
                ),
            ),
        ),
        FilterOption(
            id = "noir",
            categoryIds = setOf(CINEMATIC, BLACK_WHITE),
            nameRes = R.string.gs_filter_noir,
            recipe = FilterRecipe(
                isMonochrome = true,
                adjustments = Adjustments(
                    exposure = -6,
                    contrast = 26,
                    highlights = -20,
                    shadows = -12,
                    clarity = 16,
                    vignette = 24,
                    grain = 12,
                ),
            ),
        ),
        FilterOption(
            id = "matte",
            categoryIds = setOf(BLACK_WHITE),
            nameRes = R.string.gs_filter_matte,
            recipe = FilterRecipe(
                isMonochrome = true,
                adjustments = Adjustments(
                    contrast = -8,
                    highlights = -8,
                    shadows = 10,
                    fade = 18,
                    grain = 8,
                ),
            ),
        ),
        FilterOption(
            id = "cream",
            categoryIds = setOf(POPULAR, PORTRAIT),
            nameRes = R.string.gs_filter_cream,
            recipe = FilterRecipe(
                redShift = 12,
                greenShift = 6,
                blueShift = -6,
                adjustments = Adjustments(
                    brightness = 6,
                    contrast = -12,
                    highlights = -8,
                    shadows = 8,
                    saturation = -6,
                    temperature = 8,
                    tint = 4,
                    fade = 6,
                ),
            ),
        ),
        FilterOption(
            id = "glow",
            categoryIds = setOf(POPULAR, NATURAL, PORTRAIT),
            nameRes = R.string.gs_filter_glow,
            recipe = FilterRecipe(
                redShift = 8,
                greenShift = 6,
                blueShift = 2,
                adjustments = Adjustments(
                    brightness = 8,
                    exposure = 6,
                    contrast = -8,
                    highlights = -12,
                    shadows = 6,
                    vibrance = 10,
                    clarity = -6,
                ),
            ),
        ),
        FilterOption(
            id = "kodak",
            categoryIds = setOf(FILM, VINTAGE),
            nameRes = R.string.gs_filter_kodak,
            recipe = FilterRecipe(
                redShift = 14,
                greenShift = 6,
                blueShift = -6,
                adjustments = Adjustments(
                    contrast = 12,
                    highlights = -12,
                    shadows = 4,
                    saturation = 10,
                    temperature = 6,
                    fade = 8,
                    grain = 10,
                ),
            ),
        ),
        FilterOption(
            id = "sunset",
            categoryIds = setOf(POPULAR, WARM),
            nameRes = R.string.gs_filter_sunset,
            recipe = FilterRecipe(
                redShift = 24,
                greenShift = 6,
                blueShift = -18,
                adjustments = Adjustments(
                    exposure = -2,
                    contrast = 12,
                    highlights = -16,
                    saturation = 12,
                    vibrance = 10,
                    temperature = 24,
                    vignette = 10,
                ),
            ),
        ),
        FilterOption(
            id = "mist",
            categoryIds = setOf(COOL, NATURAL),
            nameRes = R.string.gs_filter_mist,
            recipe = FilterRecipe(
                redShift = -4,
                greenShift = 4,
                blueShift = 12,
                adjustments = Adjustments(
                    brightness = 4,
                    contrast = -14,
                    highlights = -12,
                    shadows = 10,
                    saturation = -12,
                    temperature = -10,
                    fade = 12,
                ),
            ),
        ),
        FilterOption(
            id = "cafe",
            categoryIds = setOf(FOOD, WARM),
            nameRes = R.string.gs_filter_cafe,
            recipe = FilterRecipe(
                redShift = 16,
                greenShift = 8,
                blueShift = -10,
                adjustments = Adjustments(
                    contrast = 6,
                    saturation = 10,
                    vibrance = 8,
                    temperature = 16,
                    tint = 4,
                    fade = 8,
                ),
            ),
        ),
        FilterOption(
            id = "drama",
            categoryIds = setOf(CINEMATIC),
            nameRes = R.string.gs_filter_drama,
            recipe = FilterRecipe(
                redShift = -4,
                greenShift = 4,
                blueShift = 8,
                adjustments = Adjustments(
                    exposure = -8,
                    contrast = 28,
                    highlights = -22,
                    shadows = -16,
                    saturation = -12,
                    clarity = 18,
                    vignette = 26,
                    grain = 6,
                ),
            ),
        ),
        FilterOption(
            id = "epic",
            categoryIds = setOf(CINEMATIC),
            nameRes = R.string.gs_filter_epic,
            recipe = FilterRecipe(
                redShift = 6,
                greenShift = 2,
                blueShift = 8,
                adjustments = Adjustments(
                    exposure = -4,
                    contrast = 24,
                    highlights = -20,
                    shadows = -8,
                    saturation = -6,
                    clarity = 18,
                    vignette = 20,
                ),
            ),
        ),
        FilterOption(
            id = "blockbuster",
            categoryIds = setOf(POPULAR, CINEMATIC),
            nameRes = R.string.gs_filter_blockbuster,
            recipe = FilterRecipe(
                redShift = 16,
                greenShift = 2,
                blueShift = 10,
                adjustments = Adjustments(
                    exposure = -6,
                    contrast = 30,
                    highlights = -24,
                    shadows = -14,
                    saturation = -10,
                    temperature = 10,
                    tint = -10,
                    clarity = 20,
                    vignette = 24,
                    grain = 4,
                ),
            ),
        ),
        FilterOption(
            id = "arthouse",
            categoryIds = setOf(CINEMATIC, VINTAGE),
            nameRes = R.string.gs_filter_arthouse,
            recipe = FilterRecipe(
                redShift = 4,
                greenShift = -2,
                blueShift = 6,
                adjustments = Adjustments(
                    brightness = -2,
                    contrast = -6,
                    highlights = -14,
                    shadows = 12,
                    saturation = -18,
                    tint = 6,
                    fade = 14,
                    vignette = 16,
                    grain = 12,
                ),
            ),
        ),
        FilterOption(
            id = "silver",
            categoryIds = setOf(BLACK_WHITE),
            nameRes = R.string.gs_filter_silver,
            recipe = FilterRecipe(
                isMonochrome = true,
                adjustments = Adjustments(
                    brightness = 4,
                    contrast = 10,
                    highlights = -10,
                    shadows = 8,
                    clarity = 10,
                    fade = 6,
                ),
            ),
        ),
    )

    val defaultCategory: FilterCategory = categories.first()
    val default: FilterOption = options.first { it.id == ORIGINAL }
    val pack: FilterPack = FilterPack(
        categories = categories,
        options = options,
        defaultCategory = defaultCategory,
        defaultFilter = default,
    )

    fun filtersForCategory(categoryId: String): List<FilterOption> =
        pack.filtersForCategory(categoryId)

    fun categoryForFilter(filter: FilterOption): FilterCategory? =
        pack.categoryForFilter(filter)

    private const val ORIGINAL = "original"
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

