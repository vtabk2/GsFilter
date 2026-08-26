package com.gsfilter

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gsfilter.databinding.ActivityMainBinding
import com.gsfilter.filter.FilterControlsView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FilterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private var selectedControlTab = FilterControlsView.ControlTab.Filter
    private var renderedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindFilterControls()
        bindAdjustControls()
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
        binding.filterControls.onCloseClick = { finish() }
        binding.filterControls.onControlTabSelected = ::selectControlTab
        binding.filterControls.onCategorySelected = viewModel::selectCategory
        binding.filterControls.onFilterSelected = viewModel::selectFilter
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

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: FilterUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.errorText.isVisible = state.error != null
        binding.errorText.text = state.error?.toMessage().orEmpty()
        binding.imageOriginal.setImageBitmap(state.sourceBitmap)
        if (renderedBitmap !== state.sourceBitmap) {
            renderedBitmap = state.sourceBitmap
            binding.filterPreview.setSourceBitmap(state.sourceBitmap)
        }
        binding.filterPreview.setFilterState(
            recipe = state.selectedFilter.recipe,
            adjustments = state.adjustments,
        )
        binding.filterControls.setState(
            selectedCategory = state.selectedCategory,
            selectedFilter = state.selectedFilter,
            thumbnailBitmap = state.filterThumbnailBitmap,
            thumbnailKey = state.filterThumbnailKey,
        )
        binding.filterControls.setAdjustments(state.adjustments)
    }

    private fun FilterError.toMessage(): String =
        when (this) {
            FilterError.AssetLoadFailed -> getString(R.string.asset_load_failed)
        }
}
