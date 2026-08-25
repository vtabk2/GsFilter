package com.gsfilter

import android.os.Bundle
import android.graphics.Bitmap
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gsfilter.databinding.ActivityMainBinding
import com.gsfilter.filter.Adjustments
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FilterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val categoryButtons = mutableMapOf<String, Button>()
    private val filterButtons = mutableMapOf<String, Button>()
    private var renderedCategoryId: String? = null
    private var renderedBitmap: Bitmap? = null
    private var isRenderingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindCategoryControls()
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

    private fun bindCategoryControls() {
        FilterCatalog.categories.forEachIndexed { index, category ->
            val button = createControlButton(
                index = index,
                text = getString(category.nameRes),
                onClick = { viewModel.selectCategory(category) },
            )
            categoryButtons[category.id] = button
            binding.categoryContainer.addView(button)
        }
    }

    private fun renderFilterControls(categoryId: String) {
        if (renderedCategoryId == categoryId) {
            return
        }

        renderedCategoryId = categoryId
        filterButtons.clear()
        binding.filterContainer.removeAllViews()
        FilterCatalog.filtersForCategory(categoryId).forEachIndexed { index, filter ->
            val button = createControlButton(
                index = index,
                text = getString(filter.nameRes),
                onClick = { viewModel.selectFilter(filter) },
            )
            filterButtons[filter.id] = button
            binding.filterContainer.addView(button)
        }
    }

    private fun createControlButton(
        index: Int,
        text: String,
        onClick: () -> Unit,
    ): Button {
        val spacing = resources.getDimensionPixelSize(R.dimen.item_spacing)
        val minHeight = resources.getDimensionPixelSize(R.dimen.button_min_height)
        return Button(this).apply {
            this.text = text
            minimumHeight = minHeight
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                if (index > 0) {
                    marginStart = spacing
                }
            }
        }
    }

    private fun bindAdjustControls() {
        bindSeekBar(binding.brightnessSeek) { viewModel.setBrightness(it - ADJUST_CENTER) }
        bindSeekBar(binding.contrastSeek) { viewModel.setContrast(it) }
        bindSeekBar(binding.saturationSeek) { viewModel.setSaturation(it) }
        binding.buttonResetAdjust.setOnClickListener { viewModel.resetAdjustments() }
    }

    private fun bindSeekBar(seekBar: SeekBar, onProgressChanged: (Int) -> Unit) {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !isRenderingState) {
                    onProgressChanged(progress)
                }
            }

            override fun onStartTrackingTouch(view: SeekBar) = Unit

            override fun onStopTrackingTouch(view: SeekBar) = Unit
        })
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun render(state: FilterUiState) {
        isRenderingState = true
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
        renderFilterControls(state.selectedCategory.id)
        categoryButtons.forEach { (id, button) ->
            button.isEnabled = id != state.selectedCategory.id
        }
        filterButtons.forEach { (id, button) ->
            button.isEnabled = id != state.selectedFilter.id
        }
        renderAdjustments(state.adjustments)
        isRenderingState = false
    }

    private fun renderAdjustments(adjustments: Adjustments) {
        binding.brightnessSeek.progress = adjustments.brightness + ADJUST_CENTER
        binding.contrastSeek.progress = adjustments.contrast
        binding.saturationSeek.progress = adjustments.saturation
        val separator = getString(R.string.label_separator)
        val percent = getString(R.string.percent_suffix)
        binding.brightnessValue.text =
            getString(R.string.brightness) + separator + adjustments.brightness
        binding.contrastValue.text =
            getString(R.string.contrast) + separator + adjustments.contrast + percent
        binding.saturationValue.text =
            getString(R.string.saturation) + separator + adjustments.saturation + percent
    }

    private fun FilterError.toMessage(): String =
        when (this) {
            FilterError.AssetLoadFailed -> getString(R.string.asset_load_failed)
        }

    private companion object {
        const val ADJUST_CENTER = 100
    }
}
