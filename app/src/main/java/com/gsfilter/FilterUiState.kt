package com.gsfilter

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterOption
import com.gsfilter.filter.FilterPack
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.MakeupFeatures

data class FilterUiState(
    val sourceBitmap: Bitmap? = null,
    val filterThumbnailKey: String? = null,
    val catalog: FilterPack = FilterCatalog.pack,
    val selectedCategory: FilterCategory = FilterCatalog.defaultCategory,
    val selectedFilter: FilterOption = FilterCatalog.default,
    val filterIntensities: Map<String, Int> = emptyMap(),
    val skinSmoothing: Int = FilterCatalog.default.recipe.skinSmoothing,
    val skinWhitening: Int = FilterCatalog.default.recipe.skinWhitening,
    val blush: Int = FilterCatalog.default.recipe.blush,
    val lipstick: Int = FilterCatalog.default.recipe.lipstick,
    val makeupFeatures: MakeupFeatures? = null,
    val adjustments: Adjustments = Adjustments(),
    val isLoading: Boolean = false,
    val error: FilterError? = null,
) {
    val selectedFilterIntensity: Int
        get() = filterIntensities[selectedFilter.id] ?: selectedFilter.recipe.intensity

    val selectedRecipe: FilterRecipe
        get() {
            val recipe = selectedFilter.recipe
            val intensity = selectedFilterIntensity
            return if (
                intensity != recipe.intensity ||
                skinSmoothing != recipe.skinSmoothing ||
                skinWhitening != recipe.skinWhitening ||
                blush != recipe.blush ||
                lipstick != recipe.lipstick
            ) {
                recipe.copy(
                    intensity = intensity,
                    skinSmoothing = skinSmoothing,
                    skinWhitening = skinWhitening,
                    blush = blush,
                    lipstick = lipstick,
                )
            } else {
                recipe
            }
        }
}

enum class FilterError {
    AssetLoadFailed,
}
