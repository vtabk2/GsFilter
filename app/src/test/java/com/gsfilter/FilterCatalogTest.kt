package com.gsfilter

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
    fun `popular category reuses filters from other categories`() {
        val popularFilters = FilterCatalog.filtersForCategory(FilterCatalog.defaultCategory.id)

        assertTrue(popularFilters.size > 1)
        assertTrue(popularFilters.any { it.categoryIds.size > 1 })
    }
}
