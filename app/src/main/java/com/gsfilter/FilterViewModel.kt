package com.gsfilter

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.AdjustControl
import com.gsfilter.filter.renderer.FilterBitmapRenderer
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterEffect
import com.gsfilter.filter.renderer.FilterGpuBitmapRenderer
import com.gsfilter.filter.FilterOption
import com.gsfilter.filter.FilterPack
import com.gsfilter.filter.FilterSourceKey
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun setFilterEffectStrength(value: Int) {
        _state.update { state ->
            if (state.selectedFilter.recipe.effect == FilterEffect.Color) {
                return@update state
            }

            val strength = value.coerceIn(EFFECT_STRENGTH_MIN, EFFECT_STRENGTH_MAX)
            val currentStrength =
                state.filterEffectStrengths[state.selectedFilter.id] ?: state.selectedFilter.recipe.effectStrength
            if (currentStrength == strength) {
                state
            } else {
                state.copy(
                    filterEffectStrengths =
                        if (strength == state.selectedFilter.recipe.effectStrength) {
                            state.filterEffectStrengths - state.selectedFilter.id
                        } else {
                            state.filterEffectStrengths + (state.selectedFilter.id to strength)
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
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        getApplication<Application>().assets.open(SAMPLE_ASSET).use { input ->
            return BitmapFactory.decodeStream(input, null, options)
                ?: throw IOException("Cannot decode asset")
        }
    }

    private companion object {
        const val SAMPLE_ASSET = "sample.jpg"
        const val EFFECT_STRENGTH_MIN = 0
        const val EFFECT_STRENGTH_MAX = 100
    }
}
