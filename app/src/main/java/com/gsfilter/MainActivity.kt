package com.gsfilter

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gsfilter.databinding.ActivityMainBinding
import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.view.FilterControlsView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val viewModel: FilterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private var selectedControlTab = FilterControlsView.ControlTab.Filter
    private var renderedBitmap: Bitmap? = null
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindFilterControls()
        bindBeautyControls()
        bindAdjustControls()
        bindFilterPackToggle()
        binding.nextImageButton.setOnClickListener { viewModel.nextImage() }
        collectState()
    }

    override fun onResume() {
        super.onResume()
        binding.filterPreview.onResume()
    }

    override fun onPause() {
        binding.filterPreview.onPause()
        super.onPause()
    }

    private fun bindFilterControls() {
        binding.filterControls.onCloseClick = ::saveFilteredImage
        binding.filterControls.onControlTabSelected = ::selectControlTab
        binding.filterControls.onCategorySelected = viewModel::selectCategory
        binding.filterControls.onFilterSelected = viewModel::selectFilter
        binding.filterControls.onFilterIntensityChanged = viewModel::setFilterIntensity
        binding.filterControls.onCatalogLoaded = viewModel::setCatalog
        binding.filterControls.onCatalogLoadFailed = {
            Toast.makeText(this, R.string.filter_pack_load_failed, Toast.LENGTH_SHORT).show()
            binding.filterPackSwitch.isChecked = false
        }
        renderControlTabs()
    }

    private fun selectControlTab(tab: FilterControlsView.ControlTab) {
        selectedControlTab = tab
        renderControlTabs()
    }

    private fun renderControlTabs() {
        binding.filterControls.setSelectedTab(selectedControlTab)
    }

    private fun bindAdjustControls() {
        binding.filterControls.onAdjustmentChanged = viewModel::setAdjustment
        binding.filterControls.onResetAllAdjustClick = viewModel::resetAdjustments
    }

    private fun bindBeautyControls() {
        binding.filterControls.onBeautyChanged = { control, value ->
            when (control) {
                FilterControlsView.BeautyControl.Smoothing -> viewModel.setSkinSmoothing(value)
                FilterControlsView.BeautyControl.Whitening -> viewModel.setSkinWhitening(value)
                FilterControlsView.BeautyControl.Blush -> viewModel.setBlush(value)
                FilterControlsView.BeautyControl.Lipstick -> viewModel.setLipstick(value)
                FilterControlsView.BeautyControl.UnderEye -> viewModel.setUnderEye(value)
                FilterControlsView.BeautyControl.TeethWhitening -> viewModel.setTeethWhitening(value)
                FilterControlsView.BeautyControl.EyeShadow -> viewModel.setEyeShadow(value)
                FilterControlsView.BeautyControl.Eyeliner -> viewModel.setEyeliner(value)
            }
        }
        binding.filterControls.onResetBeautyClick = viewModel::resetBeauty
    }

    private fun bindFilterPackToggle() {
        binding.filterPackSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.filterControls.loadCatalogFromAssets(TEST_FILTER_PACK_ASSET)
            } else {
                binding.filterControls.setCatalog(FilterCatalog.pack)
                viewModel.setCatalog(FilterCatalog.pack)
            }
        }
        if (binding.filterPackSwitch.isChecked) {
            binding.filterControls.loadCatalogFromAssets(TEST_FILTER_PACK_ASSET)
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: FilterUiState) {
        binding.progressBar.isVisible = state.isLoading || isSaving
        binding.nextImageButton.isEnabled = state.imageAssetCount > 1 && !state.isLoading && !isSaving
        binding.errorText.isVisible = state.error != null
        binding.errorText.text = state.error?.toMessage().orEmpty()
        binding.imageOriginal.setImageBitmap(state.sourceBitmap)
        if (renderedBitmap !== state.sourceBitmap) {
            renderedBitmap = state.sourceBitmap
            binding.filterPreview.setSourceBitmap(state.sourceBitmap)
        }
        binding.filterPreview.setFilterState(
            recipe = state.selectedRecipe,
            adjustments = state.adjustments,
            makeupFeatures = state.makeupFeatures,
        )
        binding.filterControls.setState(
            selectedCategory = state.selectedCategory,
            selectedFilter = state.selectedFilter,
            thumbnailBitmap = state.sourceBitmap,
            thumbnailKey = state.filterThumbnailKey,
            selectedRecipe = state.selectedRecipe,
        )
        binding.filterControls.setAdjustments(state.adjustments)
    }

    private fun saveFilteredImage() {
        if (isSaving) {
            return
        }

        lifecycleScope.launch {
            setSaving(true)
            val file = try {
                val bitmap = viewModel.renderFilteredBitmap()
                if (bitmap == null) {
                    null
                } else {
                    try {
                        withContext(Dispatchers.IO) { saveToAppStorage(bitmap) }
                    } finally {
                        bitmap.recycle()
                    }
                }
            } catch (error: RuntimeException) {
                if (error is CancellationException) {
                    throw error
                }
                null
            } catch (_: IOException) {
                null
            } finally {
                setSaving(false)
            }

            Toast.makeText(
                this@MainActivity,
                if (file == null) R.string.image_save_failed else R.string.image_saved_to_app_storage,
                Toast.LENGTH_SHORT,
            ).show()
//            if (file != null) {
//                finish()
//            }
        }
    }

    private fun setSaving(saving: Boolean) {
        isSaving = saving
        binding.progressBar.isVisible = viewModel.state.value.isLoading || saving
    }

    private fun saveToAppStorage(bitmap: Bitmap): File {
        Log.d("TAG5", "saveToAppStorage: bitmap.width = " + bitmap.width)
        Log.d("TAG5", "saveToAppStorage: bitmap.height = " + bitmap.height)
        val directory = File(filesDir, FILTERED_IMAGES_DIR)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Cannot create filtered images directory.")
        }

        val file = File.createTempFile(FILTERED_IMAGE_PREFIX, FILTERED_IMAGE_SUFFIX, directory)
        var saved = false
        try {
            file.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    throw IOException("Cannot encode filtered image.")
                }
            }
            saved = true
            return file
        } finally {
            if (!saved) {
                file.delete()
            }
        }
    }

    private fun FilterError.toMessage(): String =
        when (this) {
            FilterError.AssetLoadFailed -> getString(R.string.asset_load_failed)
        }

    private companion object {
        const val TEST_FILTER_PACK_ASSET = "filter_pack.json"
        const val FILTERED_IMAGES_DIR = "filtered"
        const val FILTERED_IMAGE_PREFIX = "filtered_"
        const val FILTERED_IMAGE_SUFFIX = ".jpg"
        const val JPEG_QUALITY = 95
    }
}
