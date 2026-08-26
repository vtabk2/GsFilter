package com.gsfilter

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterOption

data class FilterUiState(
    val sourceBitmap: Bitmap? = null,
    val filterThumbnailBitmap: Bitmap? = null,
    val filterThumbnailKey: String? = null,
    val selectedCategory: FilterCategory = FilterCatalog.defaultCategory,
    val selectedFilter: FilterOption = FilterCatalog.default,
    val adjustments: Adjustments = Adjustments(),
    val isLoading: Boolean = false,
    val error: FilterError? = null,
)

enum class FilterError {
    AssetLoadFailed,
}
