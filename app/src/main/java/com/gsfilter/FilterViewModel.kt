package com.gsfilter

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.BitmapFilterRenderer
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterOption
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilterViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(FilterUiState())
    val state: StateFlow<FilterUiState> = _state.asStateFlow()

    private var sourceBitmap: Bitmap? = null
    private var renderJob: Job? = null

    init {
        loadSample()
    }

    fun selectFilter(filter: FilterOption) {
        _state.update { it.copy(selectedFilter = filter) }
        renderCurrent()
    }

    fun selectCategory(category: FilterCategory) {
        val nextFilter = FilterCatalog.filtersForCategory(category.id).firstOrNull()
            ?: _state.value.selectedFilter
        _state.update {
            it.copy(
                selectedCategory = category,
                selectedFilter = nextFilter,
            )
        }
        renderCurrent()
    }

    fun setBrightness(value: Int) {
        updateAdjustments { it.copy(brightness = value) }
    }

    fun setContrast(value: Int) {
        updateAdjustments { it.copy(contrast = value) }
    }

    fun setSaturation(value: Int) {
        updateAdjustments { it.copy(saturation = value) }
    }

    fun resetAdjustments() {
        _state.update { it.copy(adjustments = Adjustments()) }
        renderCurrent()
    }

    private fun loadSample() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val bitmap = withContext(Dispatchers.IO) { decodeSampleBitmap() }
                sourceBitmap = bitmap
                val snapshot = _state.value
                val rendered = withContext(Dispatchers.Default) {
                    BitmapFilterRenderer.render(
                        source = bitmap,
                        filter = snapshot.selectedFilter,
                        adjustments = snapshot.adjustments,
                    )
                }
                _state.update {
                    it.copy(
                        sourceBitmap = bitmap,
                        resultBitmap = rendered,
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
        renderCurrent()
    }

    private fun renderCurrent() {
        val source = sourceBitmap ?: return
        val snapshot = _state.value
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val rendered = withContext(Dispatchers.Default) {
                BitmapFilterRenderer.render(
                    source = source,
                    filter = snapshot.selectedFilter,
                    adjustments = snapshot.adjustments,
                )
            }
            _state.update {
                it.copy(
                    resultBitmap = rendered,
                    isLoading = false,
                    error = null,
                )
            }
        }
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
