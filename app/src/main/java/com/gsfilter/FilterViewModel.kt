package com.gsfilter

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gsfilter.filter.AdjustControl
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterOption
import com.gsfilter.filter.FilterPack
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.FilterSourceKey
import com.gsfilter.filter.renderer.FilterBitmapRenderer
import com.gsfilter.filter.renderer.FilterGpuBitmapRenderer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class FilterViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(FilterUiState())
    val state: StateFlow<FilterUiState> = _state.asStateFlow()

    init {
        loadSample()
    }

    fun setCatalog(catalog: FilterPack) {
        _state.update { state ->
            state.copy(
                catalog = catalog,
                selectedCategory = catalog.categoryById(state.selectedCategory.id) ?: catalog.defaultCategory,
                selectedFilter = catalog.filterById(state.selectedFilter.id) ?: catalog.defaultFilter,
            )
        }
    }

    fun selectFilter(filter: FilterOption) {
        _state.update { it.copy(selectedFilter = filter) }
    }

    fun selectCategory(category: FilterCategory) {
        _state.update { state ->
            val selectedFilterCategory = state.catalog.categoryForFilter(state.selectedFilter)
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

    fun setFilterIntensity(value: Int) {
        _state.update { state ->
            if (state.selectedFilter.recipe == FilterRecipe()) {
                return@update state
            }

            val intensity = value.coerceIn(FILTER_INTENSITY_MIN, FILTER_INTENSITY_MAX)
            if (state.selectedFilterIntensity == intensity) {
                state
            } else {
                state.copy(
                    filterIntensities =
                        if (intensity == state.selectedFilter.recipe.intensity) {
                            state.filterIntensities - state.selectedFilter.id
                        } else {
                            state.filterIntensities + (state.selectedFilter.id to intensity)
                        },
                )
            }
        }
    }

    suspend fun renderFilteredBitmap(
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        useGpu: Boolean = true,
    ): Bitmap? {
        val state = _state.value
        val source = state.sourceBitmap ?: return null
        val recipe = state.selectedRecipe
        return withContext(Dispatchers.Default) {
            fun renderCpu() =
                FilterBitmapRenderer.getBitmap(
                    source = source,
                    recipe = recipe,
                    adjustments = state.adjustments,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                )

            if (!useGpu) {
                return@withContext renderCpu()
            }

            try {
                FilterGpuBitmapRenderer.getBitmap(
                    source = source,
                    recipe = recipe,
                    adjustments = state.adjustments,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                )
            } catch (error: RuntimeException) {
                if (error is CancellationException) {
                    throw error
                }
                renderCpu()
            }
        }
    }

    private fun loadSample() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val bitmap = withContext(Dispatchers.IO) { decodeSampleBitmap() }
                _state.update {
                    Log.d("TAG5", "loadSample: width = " + bitmap.width)
                    Log.d("TAG5", "loadSample: height = " + bitmap.height)
                    it.copy(
                        sourceBitmap = bitmap,
                        filterThumbnailKey = FilterSourceKey.asset(SAMPLE_ASSET),
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
        val application = getApplication<Application>()
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        application.assets.open(SAMPLE_ASSET).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Cannot decode asset bounds")
        }
        Log.d("TAG5", "decodeSampleBitmap: outWidth = " + bounds.outWidth)
        Log.d("TAG5", "decodeSampleBitmap: outHeight = " + bounds.outHeight)
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleBitmapInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                maxEdge = SAMPLE_BITMAP_MAX_EDGE,
            )
        }
        application.assets.open(SAMPLE_ASSET).use { input ->
            return BitmapFactory.decodeStream(input, null, options)
                ?: throw IOException("Cannot decode asset")
        }
    }

    private companion object {
        const val SAMPLE_ASSET = "sample.jpg"
        const val SAMPLE_BITMAP_MAX_EDGE = 4096
        const val FILTER_INTENSITY_MIN = 0
        const val FILTER_INTENSITY_MAX = 100
    }
}

internal fun sampleBitmapInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
    require(width > 0 && height > 0) { "Source size must be positive." }
    require(maxEdge > 0) { "maxEdge must be positive." }

    var sampleSize = 1
    while (maxOf(width, height) / sampleSize > maxEdge) {
        sampleSize *= 2
    }
    return sampleSize
}
