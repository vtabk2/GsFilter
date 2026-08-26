package com.gsfilter

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.AdjustControl
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterOption
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale

class FilterViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(FilterUiState())
    val state: StateFlow<FilterUiState> = _state.asStateFlow()

    init {
        loadSample()
    }

    fun selectFilter(filter: FilterOption) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun selectCategory(category: FilterCategory) {
        _state.update { state ->
            val selectedFilterCategory = FilterCatalog.categoryForFilter(state.selectedFilter)
            val nextCategory =
                if (
                    state.selectedCategory.id == category.id &&
                    category.id !in state.selectedFilter.categoryIds &&
                    selectedFilterCategory != null
                ) {
                    selectedFilterCategory
                } else {
                    category
                }

            state.copy(selectedCategory = nextCategory)
        }
    }

    fun setAdjustment(control: AdjustControl, value: Int) {
        updateAdjustments { control.update(it, value) }
    }

    fun resetAdjustments() {
        _state.update { it.copy(adjustments = Adjustments()) }
    }

    private fun loadSample() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val (bitmap, thumbnail) = withContext(Dispatchers.IO) {
                    val bitmap = decodeSampleBitmap()
                    bitmap to bitmap.scaledToMaxEdge(FILTER_THUMBNAIL_MAX_EDGE)
                }
                _state.update {
                    it.copy(
                        sourceBitmap = bitmap,
                        filterThumbnailBitmap = thumbnail,
                        filterThumbnailKey = SAMPLE_ASSET,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (_: IOException) {
                _state.update {
                    it.copy(isLoading = false, error = FilterError.AssetLoadFailed)
                }
            }
        }
    }

    private fun updateAdjustments(update: (Adjustments) -> Adjustments) {
        _state.update { it.copy(adjustments = update(it.adjustments)) }
    }

    private fun decodeSampleBitmap(): Bitmap {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        getApplication<Application>().assets.open(SAMPLE_ASSET).use { input ->
            return BitmapFactory.decodeStream(input, null, options)
                ?: throw IOException("Cannot decode asset")
        }
    }

    private fun Bitmap.scaledToMaxEdge(maxEdge: Int): Bitmap {
        val longestEdge = maxOf(width, height)
        if (longestEdge <= maxEdge) {
            return this
        }

        val scale = maxEdge.toFloat() / longestEdge
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return this.scale(targetWidth, targetHeight)
    }

    private companion object {
        const val SAMPLE_ASSET = "sample.jpg"
        const val FILTER_THUMBNAIL_MAX_EDGE = 256
    }
}
