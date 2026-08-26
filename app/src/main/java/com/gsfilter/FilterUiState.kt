package com.gsfilter

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterEffect
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
    val filterEffectStrengths: Map<String, Int> = emptyMap(),
    val adjustments: Adjustments = Adjustments(),
    val isLoading: Boolean = false,
    val error: FilterError? = null,
) {
    val selectedRecipe: FilterRecipe
        get() {
            val strength = filterEffectStrengths[selectedFilter.id]
            return if (strength != null && selectedFilter.recipe.effect != FilterEffect.Color) {
                selectedFilter.recipe.copy(effectStrength = strength)
            } else {
                selectedFilter.recipe
            }
        }
}

enum class FilterError {
    AssetLoadFailed,
}
