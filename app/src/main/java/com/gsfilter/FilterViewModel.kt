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
                val bitmap = withContext(Dispatchers.IO) { decodeSampleBitmap() }
                _state.update {
                    it.copy(
                        sourceBitmap = bitmap,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (error: IOException) {
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
    }
}
