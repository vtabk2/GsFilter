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
            "popular" to listOf("selfie_clear", "fresh", "clear", "clean_light"),
            "portrait" to listOf("selfie_clear", "soft_portrait", "skin", "portra", "studio_skin"),
            "natural" to listOf("fresh", "clear", "clean_light", "soft_day"),
            "food" to listOf("tasty", "crispy", "fresh_plate", "warm_plate"),
            "landscape" to listOf("clear_day", "golden_hour", "sunlit_forest", "forest"),
            "night" to listOf("midnight_city", "city", "night", "blue_hour"),
            "film" to listOf("portra", "fuji", "kodak", "gold"),
            "cinematic" to listOf("teal_orange", "cinema", "blockbuster", "deep_teal"),
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
}
