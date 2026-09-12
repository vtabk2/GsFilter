package com.gsfilter

import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterUiStateTest {

    @Test
    fun `restores saved filter recipe and adjustments`() {
        val catalog = FilterCatalog.pack
        val filter = catalog.options.first {
            it.id != catalog.defaultFilter.id && it.recipe != FilterRecipe()
        }
        val recipe = filter.recipe.copy(
            intensity = 62,
            skinSmoothing = 78,
            blush = 31,
        )
        val adjustments = Adjustments(brightness = 12, grain = 7)
        val saved = SavedFilterState(
            selectedCategoryId = filter.categoryIds.first(),
            selectedFilterId = filter.id,
            selectedRecipe = recipe,
            filterIntensities = mapOf(filter.id to recipe.intensity),
            adjustments = adjustments,
        )

        val restored = FilterUiState(catalog = catalog).restoreFilterState(saved)

        assertEquals(filter, restored.selectedFilter)
        assertEquals(recipe, restored.selectedRecipe)
        assertEquals(adjustments, restored.adjustments)
    }

    @Test
    fun `new image starts with catalog defaults when no saved state exists`() {
        val catalog = FilterCatalog.pack

        val restored = FilterUiState(
            catalog = catalog,
            selectedFilter = catalog.options.last(),
            adjustments = Adjustments(brightness = 20),
        ).restoreFilterState(null)

        assertEquals(catalog.defaultCategory, restored.selectedCategory)
        assertEquals(catalog.defaultFilter, restored.selectedFilter)
        assertEquals(FilterRecipe(), restored.selectedRecipe)
        assertEquals(Adjustments(), restored.adjustments)
    }

    @Test
    fun `restored filter focuses its category when saved category does not contain it`() {
        val catalog = FilterCatalog.pack
        val filter = catalog.options.first { it.id == "fuji" }
        val saved = SavedFilterState(
            selectedCategoryId = "natural",
            selectedFilterId = filter.id,
            selectedRecipe = filter.recipe,
            filterIntensities = emptyMap(),
            adjustments = Adjustments.DEFAULT,
        )

        val restored = FilterUiState(catalog = catalog).restoreFilterState(saved)

        assertEquals("film", restored.selectedCategory.id)
        assertTrue(restored.selectedCategory.id in restored.selectedFilter.categoryIds)
    }
}
