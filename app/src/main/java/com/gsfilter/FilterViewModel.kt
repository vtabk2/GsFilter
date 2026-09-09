package com.gsfilter

import android.app.Application
import android.graphics.Bitmap
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
    private var imageAssets: List<String> = emptyList()
    private var imageAssetIndex = 0

    init {
        loadSample()
    }

    fun nextImage() {
        if (_state.value.isLoading || imageAssets.size < 2) {
            return
        }
        imageAssetIndex = (imageAssetIndex + 1) % imageAssets.size
        val assetPath = imageAssets[imageAssetIndex]
        viewModelScope.launch {
            loadAsset(assetPath)
        }
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
                underEye = selectedFilter.recipe.underEye,
                teethWhitening = selectedFilter.recipe.teethWhitening,
                eyeShadow = selectedFilter.recipe.eyeShadow,
                eyeliner = selectedFilter.recipe.eyeliner,
                eyebrow = selectedFilter.recipe.eyebrow,
                faceSlimming = selectedFilter.recipe.faceSlimming,
                eyeEnlargement = selectedFilter.recipe.eyeEnlargement,
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
                underEye = filter.recipe.underEye,
                teethWhitening = filter.recipe.teethWhitening,
                eyeShadow = filter.recipe.eyeShadow,
                eyeliner = filter.recipe.eyeliner,
                eyebrow = filter.recipe.eyebrow,
                faceSlimming = filter.recipe.faceSlimming,
                eyeEnlargement = filter.recipe.eyeEnlargement,
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

    fun setUnderEye(value: Int) {
        _state.update { it.copy(underEye = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setTeethWhitening(value: Int) {
        _state.update { it.copy(teethWhitening = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setEyeShadow(value: Int) {
        _state.update { it.copy(eyeShadow = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setEyeliner(value: Int) {
        _state.update { it.copy(eyeliner = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setEyebrow(value: Int) {
        _state.update { it.copy(eyebrow = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setFaceSlimming(value: Int) {
        _state.update { it.copy(faceSlimming = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun setEyeEnlargement(value: Int) {
        _state.update { it.copy(eyeEnlargement = value.coerceIn(BEAUTY_MIN, BEAUTY_MAX)) }
    }

    fun resetBeauty() {
        _state.update {
            it.copy(
                skinSmoothing = it.selectedFilter.recipe.skinSmoothing,
                skinWhitening = it.selectedFilter.recipe.skinWhitening,
                blush = it.selectedFilter.recipe.blush,
                lipstick = it.selectedFilter.recipe.lipstick,
                underEye = it.selectedFilter.recipe.underEye,
                teethWhitening = it.selectedFilter.recipe.teethWhitening,
                eyeShadow = it.selectedFilter.recipe.eyeShadow,
                eyeliner = it.selectedFilter.recipe.eyeliner,
                eyebrow = it.selectedFilter.recipe.eyebrow,
                faceSlimming = it.selectedFilter.recipe.faceSlimming,
                eyeEnlargement = it.selectedFilter.recipe.eyeEnlargement,
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
            if (recipe == FilterRecipe() && state.adjustments == Adjustments()) {
                return@withContext source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
            }

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
                imageAssets = withContext(Dispatchers.IO) {
                    imageAssetPaths(getApplication<Application>().assets.list("") ?: emptyArray())
                }
                if (imageAssets.isEmpty()) {
                    throw IOException("No image assets found")
                }
                imageAssetIndex = imageAssets.indexOf(SAMPLE_ASSET).takeIf { it >= 0 } ?: 0
                loadAsset(imageAssets[imageAssetIndex])
            } catch (_: IOException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        imageAssetCount = imageAssets.size,
                        error = FilterError.AssetLoadFailed,
                    )
                }
            }
        }
    }

    private suspend fun loadAsset(assetPath: String) {
        _state.update {
            it.copy(
                isLoading = true,
                error = null,
                makeupFeatures = null,
                imageAssetCount = imageAssets.size,
            )
        }
        try {
            val bitmap = withContext(Dispatchers.IO) { decodeAssetBitmap(assetPath) }
            _state.update {
                it.copy(
                    sourceBitmap = bitmap,
                    filterThumbnailKey = FilterSourceKey.asset(assetPath),
                    isLoading = false,
                    error = null,
                    makeupFeatures = null,
                    imageAssetCount = imageAssets.size,
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
            _state.update { it.copy(isLoading = false, error = FilterError.AssetLoadFailed) }
        }
    }

    override fun onCleared() {
        faceMakeupDetector.close()
        super.onCleared()
    }

    private fun updateAdjustments(update: (Adjustments) -> Adjustments) {
        _state.update { it.copy(adjustments = update(it.adjustments)) }
    }

    private fun decodeAssetBitmap(assetPath: String): Bitmap {
        val application = getApplication<Application>()
        return LoadUtils.getBitmapFromAsset(
            context = application,
            assetPath = assetPath,
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

internal fun imageAssetPaths(paths: Array<String>): List<String> =
    paths
        .filter { path -> path.substringAfterLast('.', "").lowercase() in SUPPORTED_IMAGE_EXTENSIONS }
        .sortedWith(
            compareBy<String> { if (it == "sample.jpg") 0 else 1 }
                .thenBy { it.lowercase() },
        )

private val SUPPORTED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
