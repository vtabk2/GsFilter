package com.gsfilter

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterOption
import com.gsfilter.filter.FilterPack
import com.gsfilter.filter.FilterRecipe

data class FilterUiState(
    val sourceBitmap: Bitmap? = null,
    val filterThumbnailKey: String? = null,
    val catalog: FilterPack = FilterCatalog.pack,
    val selectedCategory: FilterCategory = FilterCatalog.defaultCategory,
    val selectedFilter: FilterOption = FilterCatalog.default,
    val filterIntensities: Map<String, Int> = emptyMap(),
    val adjustments: Adjustments = Adjustments(),
    val isLoading: Boolean = false,
    val error: FilterError? = null,
) {
    val selectedFilterIntensity: Int
        get() = filterIntensities[selectedFilter.id] ?: selectedFilter.recipe.intensity

    val selectedRecipe: FilterRecipe
        get() {
            val intensity = selectedFilterIntensity
            return if (intensity != selectedFilter.recipe.intensity) {
                selectedFilter.recipe.copy(intensity = intensity)
            } else {
                selectedFilter.recipe
            }
        }
}

enum class FilterError {
    AssetLoadFailed,
}
