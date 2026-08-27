package com.gsfilter.filter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterCatalogTest {

    @Test
    fun `every category has at least one filter`() {
        FilterCatalog.categories.forEach { category ->
            assertFalse(FilterCatalog.filtersForCategory(category.id).isEmpty())
        }
    }

    @Test
    fun `every category has multiple starter filters`() {
        FilterCatalog.categories.forEach { category ->
            assertTrue(FilterCatalog.filtersForCategory(category.id).size > 1)
        }
    }

    @Test
    fun `categories are ordered for common photo browsing`() {
        assertEquals(
            listOf(
                "popular",
                "portrait",
                "natural",
                "food",
                "landscape",
                "night",
                "film",
                "cinematic",
                "vintage",
                "black_white",
                "warm",
                "cool",
                "aesthetic",
                "creative",
                "art",
            ),
            FilterCatalog.categories.map { it.id },
        )
    }

    @Test
    fun `category filter lists prioritize strongest choices`() {
        mapOf(
            "popular" to listOf("lut_clean_portrait", "lut_daylight_fresh", "selfie_clear", "fresh"),
            "portrait" to listOf("lut_clean_portrait", "lut_soft_skin", "selfie_clear", "lut_golden_portrait"),
            "natural" to listOf("lut_daylight_fresh", "fresh", "clear", "clean_light"),
            "food" to listOf("lut_food_pop", "tasty", "crispy", "fresh_plate"),
            "landscape" to listOf("lut_green_film", "clear_day", "golden_hour", "sunlit_forest"),
            "night" to listOf("lut_night_mood", "midnight_city", "city", "night"),
            "film" to listOf("portra", "fuji", "kodak", "gold"),
            "cinematic" to listOf("lut_teal_cinema", "teal_orange", "cinema", "blockbuster"),
            "black_white" to listOf("mono", "soft_mono", "pearl_mono", "noir"),
            "art" to listOf("pencil", "soft_sketch", "color_pencil", "fine_line"),
        ).forEach { (categoryId, expectedIds) ->
            assertEquals(
                expectedIds,
                FilterCatalog.filtersForCategory(categoryId).take(expectedIds.size).map { it.id },
            )
        }
    }

    @Test
    fun `filter ids are unique`() {
        val ids = FilterCatalog.options.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `filter category ids all exist in catalog`() {
        val categoryIds = FilterCatalog.categories.map { it.id }.toSet()
        val unknownIds = FilterCatalog.options
            .flatMap { it.categoryIds }
            .filterNot { it in categoryIds }

        assertTrue(unknownIds.isEmpty())
    }

    @Test
    fun `category filter lists exclude original action`() {
        FilterCatalog.categories.forEach { category ->
            assertFalse(
                FilterCatalog.filtersForCategory(category.id)
                    .any { it.id == FilterCatalog.default.id },
            )
        }
    }

    @Test
    fun `category lookup returns category containing filter`() {
        val filter = FilterCatalog.options.first { it.id != FilterCatalog.default.id }
        val category = requireNotNull(FilterCatalog.categoryForFilter(filter))

        assertTrue(category.id in filter.categoryIds)
    }

    @Test
    fun `category lookup returns null for original action`() {
        assertNull(FilterCatalog.categoryForFilter(FilterCatalog.default))
    }

    @Test
    fun `popular category reuses filters from other categories`() {
        val popularFilters = FilterCatalog.filtersForCategory(FilterCatalog.defaultCategory.id)

        assertTrue(popularFilters.size > 1)
        assertTrue(popularFilters.any { it.categoryIds.size > 1 })
    }

    @Test
    fun `filters can carry recipe adjustment presets`() {
        assertTrue(
            FilterCatalog.options.any { filter ->
                filter.id != "original" && filter.recipe.adjustments != Adjustments()
            },
        )
    }

    @Test
    fun `catalog includes starter LUT filters`() {
        val lutFilters = FilterCatalog.options.filter { it.recipe.lut != FilterLut.None }

        assertEquals(
            setOf(
                "lut_clean_portrait",
                "lut_soft_skin",
                "lut_golden_portrait",
                "lut_daylight_fresh",
                "lut_food_pop",
                "lut_green_film",
                "lut_teal_cinema",
                "lut_night_mood",
                "lut_vintage_fade",
                "lut_editorial_matte",
            ),
            lutFilters.map { it.id }.toSet(),
        )
    }

    @Test
    fun `catalog includes sketch-style filters`() {
        val artFilterIds = FilterCatalog.filtersForCategory("art").map { it.id }.toSet()

        assertEquals(
            setOf("pencil", "soft_sketch", "color_pencil", "fine_line", "ink", "charcoal", "cross_hatch"),
            artFilterIds,
        )
    }

    @Test
    fun `sketch-style filters live in art category only`() {
        FilterCatalog.options
            .filter { it.recipe.effect != FilterEffect.Color }
            .forEach { assertEquals(setOf("art"), it.categoryIds) }
    }

    @Test
    fun `black white category contains monochrome filters only`() {
        FilterCatalog.filtersForCategory("black_white")
            .forEach { filter -> assertTrue(filter.recipe.isMonochrome) }
    }
}
