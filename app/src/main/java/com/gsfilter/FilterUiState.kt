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
    val imageAssetCount: Int = 0,
    val catalog: FilterPack = FilterCatalog.pack,
    val selectedCategory: FilterCategory = FilterCatalog.defaultCategory,
    val selectedFilter: FilterOption = FilterCatalog.default,
    val filterIntensities: Map<String, Int> = emptyMap(),
    val skinSmoothing: Int = FilterCatalog.default.recipe.skinSmoothing,
    val skinWhitening: Int = FilterCatalog.default.recipe.skinWhitening,
    val blush: Int = FilterCatalog.default.recipe.blush,
    val lipstick: Int = FilterCatalog.default.recipe.lipstick,
    val underEye: Int = FilterCatalog.default.recipe.underEye,
    val teethWhitening: Int = FilterCatalog.default.recipe.teethWhitening,
    val eyeShadow: Int = FilterCatalog.default.recipe.eyeShadow,
    val eyeliner: Int = FilterCatalog.default.recipe.eyeliner,
    val eyebrow: Int = FilterCatalog.default.recipe.eyebrow,
    val faceSlimming: Int = FilterCatalog.default.recipe.faceSlimming,
    val eyeEnlargement: Int = FilterCatalog.default.recipe.eyeEnlargement,
    val makeupFeatures: MakeupFeatures? = null,
    val adjustments: Adjustments = Adjustments.DEFAULT,
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
                lipstick != recipe.lipstick ||
                underEye != recipe.underEye ||
                teethWhitening != recipe.teethWhitening ||
                eyeShadow != recipe.eyeShadow ||
                eyeliner != recipe.eyeliner ||
                eyebrow != recipe.eyebrow ||
                faceSlimming != recipe.faceSlimming ||
                eyeEnlargement != recipe.eyeEnlargement
            ) {
                recipe.copy(
                    intensity = intensity,
                    skinSmoothing = skinSmoothing,
                    skinWhitening = skinWhitening,
                    blush = blush,
                    lipstick = lipstick,
                    underEye = underEye,
                    teethWhitening = teethWhitening,
                    eyeShadow = eyeShadow,
                    eyeliner = eyeliner,
                    eyebrow = eyebrow,
                    faceSlimming = faceSlimming,
                    eyeEnlargement = eyeEnlargement,
                )
            } else {
                recipe
            }
        }
    }

internal data class SavedFilterState(
    val selectedCategoryId: String,
    val selectedFilterId: String,
    val selectedRecipe: FilterRecipe,
    val filterIntensities: Map<String, Int>,
    val adjustments: Adjustments,
)

internal fun FilterUiState.restoreFilterState(saved: SavedFilterState?): FilterUiState {
    val savedFilter = saved?.selectedFilterId?.let(catalog::filterById)
    val filter = savedFilter ?: catalog.defaultFilter
    val recipe = if (savedFilter != null) saved?.selectedRecipe ?: filter.recipe else filter.recipe
    val category =
        saved?.selectedCategoryId?.let(catalog::categoryById)
            ?: (if (savedFilter != null) catalog.categoryForFilter(filter) else catalog.defaultCategory)
            ?: catalog.defaultCategory
    val focusedCategory =
        if (savedFilter != null && category.id !in filter.categoryIds) {
            catalog.categoryForFilter(filter) ?: category
        } else {
            category
        }

    return copy(
        selectedCategory = focusedCategory,
        selectedFilter = filter,
        filterIntensities = saved?.filterIntensities ?: emptyMap(),
        skinSmoothing = recipe.skinSmoothing,
        skinWhitening = recipe.skinWhitening,
        blush = recipe.blush,
        lipstick = recipe.lipstick,
        underEye = recipe.underEye,
        teethWhitening = recipe.teethWhitening,
        eyeShadow = recipe.eyeShadow,
        eyeliner = recipe.eyeliner,
        eyebrow = recipe.eyebrow,
        faceSlimming = recipe.faceSlimming,
        eyeEnlargement = recipe.eyeEnlargement,
        adjustments = saved?.adjustments ?: Adjustments.DEFAULT,
    )
}

enum class FilterError {
    AssetLoadFailed,
}
