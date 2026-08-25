package com.gsfilter

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
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
    private val adjustLabels = mutableMapOf<AdjustControl, TextView>()
    private val adjustSeekBars = mutableMapOf<AdjustControl, SeekBar>()
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
        AdjustControl.entries.forEach { control ->
            val label = TextView(this).apply {
                setTextColor(getColor(R.color.text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.item_spacing)
                }
            }
            val seekBar = SeekBar(this).apply {
                max = control.progressMax
                progress = control.progressFrom(control.valueIn(Adjustments()))
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser && !isRenderingState) {
                            viewModel.setAdjustment(control, control.valueFrom(progress))
                        }
                    }

                    override fun onStartTrackingTouch(view: SeekBar) = Unit

                    override fun onStopTrackingTouch(view: SeekBar) = Unit
                })
            }

            adjustLabels[control] = label
            adjustSeekBars[control] = seekBar
            binding.adjustContainer.addView(label)
            binding.adjustContainer.addView(seekBar)
        }
        binding.buttonResetAdjust.setOnClickListener { viewModel.resetAdjustments() }
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
        val separator = getString(R.string.label_separator)
        AdjustControl.entries.forEach { control ->
            val value = control.valueIn(adjustments)
            adjustSeekBars[control]?.progress = control.progressFrom(value)
            adjustLabels[control]?.text = getString(control.labelRes) + separator + value
        }
    }

    private fun FilterError.toMessage(): String =
        when (this) {
            FilterError.AssetLoadFailed -> getString(R.string.asset_load_failed)
        }

}
