package com.gsfilter

import android.app.Application
import android.graphics.Bitmap
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
import com.gsfilter.utils.LoadUtils
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
    private val faceMakeupDetector = FaceMakeupDetector()

    init {
        loadSample()
    }

    fun setCatalog(catalog: FilterPack) {
        _state.update { state ->
            val selectedFilter = catalog.filterById(state.selectedFilter.id) ?: catalog.defaultFilter
            state.copy(
                catalog = catalog,
                selectedCategory = catalog.categoryById(state.selectedCategory.id) ?: catalog.defaultCategory,
                selectedFilter = selectedFilter,
                skinSmoothing = selectedFilter.recipe.skinSmoothing,
                skinWhitening = selectedFilter.recipe.skinWhitening,
                blush = selectedFilter.recipe.blush,
                lipstick = selectedFilter.recipe.lipstick,
            )
        }
    }

    fun selectFilter(filter: FilterOption) {
        _state.update {
            it.copy(
                selectedFilter = filter,
                skinSmoothing = filter.recipe.skinSmoothing,
                skinWhitening = filter.recipe.skinWhitening,
                blush = filter.recipe.blush,
                lipstick = filter.recipe.lipstick,
            )
        }
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

    fun setSkinSmoothing(value: Int) {
        _state.update { it.copy(skinSmoothing = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setSkinWhitening(value: Int) {
        _state.update { it.copy(skinWhitening = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setBlush(value: Int) {
        _state.update { it.copy(blush = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setLipstick(value: Int) {
        _state.update { it.copy(lipstick = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun resetBeauty() {
        _state.update {
            it.copy(
                skinSmoothing = it.selectedFilter.recipe.skinSmoothing,
                skinWhitening = it.selectedFilter.recipe.skinWhitening,
                blush = it.selectedFilter.recipe.blush,
                lipstick = it.selectedFilter.recipe.lipstick,
            )
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
                    makeupFeatures = state.makeupFeatures,
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
                    makeupFeatures = state.makeupFeatures,
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
                faceMakeupDetector.detect(bitmap) { makeupFeatures ->
                    _state.update { state ->
                        if (state.sourceBitmap === bitmap) {
                            state.copy(makeupFeatures = makeupFeatures)
                        } else {
                            state
                        }
                    }
                }
            } catch (_: IOException) {
                _state.update {
                    it.copy(isLoading = false, error = FilterError.AssetLoadFailed)
                }
            }
        }
    }

    override fun onCleared() {
        faceMakeupDetector.close()
        super.onCleared()
    }

    private fun updateAdjustments(update: (Adjustments) -> Adjustments) {
        _state.update { it.copy(adjustments = update(it.adjustments)) }
    }

    private fun decodeSampleBitmap(): Bitmap {
        val application = getApplication<Application>()
        return LoadUtils.getBitmapFromAsset(
            context = application,
            assetPath = SAMPLE_ASSET,
            threshold = SAMPLE_BITMAP_MAX_EDGE,
        )
    }

    private companion object {
        const val SAMPLE_ASSET = "sample.jpg"
        const val SAMPLE_BITMAP_MAX_EDGE = 4096
        const val FILTER_INTENSITY_MIN = 0
        const val FILTER_INTENSITY_MAX = 100
        const val BEAUTY_MIN = 0
        const val BEAUTY_MAX = 100
    }
}
