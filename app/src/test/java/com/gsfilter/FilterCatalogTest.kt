package com.gsfilter

import com.gsfilter.filter.Adjustments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `filter ids are unique`() {
        val ids = FilterCatalog.options.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
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
}
