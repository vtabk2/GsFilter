package com.gsfilter

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterOption

data class FilterUiState(
    val sourceBitmap: Bitmap? = null,
    val resultBitmap: Bitmap? = null,
    val selectedFilter: FilterOption = FilterCatalog.default,
    val adjustments: Adjustments = Adjustments(),
    val isLoading: Boolean = false,
    val error: FilterError? = null,
)

enum class FilterError {
    AssetLoadFailed,
}
