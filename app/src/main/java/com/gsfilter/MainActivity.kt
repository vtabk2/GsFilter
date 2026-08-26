package com.gsfilter

import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.gsfilter.filter.AdjustControl
import com.gsfilter.filter.FilterCatalog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FilterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val categoryChips = mutableMapOf<String, TextView>()
    private val filterCards = mutableMapOf<String, View>()
    private val adjustLabels = mutableMapOf<AdjustControl, TextView>()
    private val adjustIcons = mutableMapOf<AdjustControl, ImageView>()
    private val adjustDots = mutableMapOf<AdjustControl, View>()
    private var renderedCategoryId: String? = null
    private var renderedFilterThumbnailBitmap: Bitmap? = null
    private var scrolledSelectedCategoryId: String? = null
    private var scrolledSelectedFilterId: String? = null
    private var selectedAdjustControl = AdjustControl.Brightness
    private var selectedControlTab = ControlTab.Filters
    private var lastAdjustments = Adjustments()
    private var renderedBitmap: Bitmap? = null
    private var isRenderingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindControlTabs()
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

    private fun bindControlTabs() {
        binding.tabFilter.setOnClickListener { selectControlTab(ControlTab.Filters) }
        binding.tabAdjust.setOnClickListener { selectControlTab(ControlTab.Adjust) }
        renderControlTabs()
    }

    private fun selectControlTab(tab: ControlTab) {
        selectedControlTab = tab
        renderControlTabs()
    }

    private fun renderControlTabs() {
        val isFilterSelected = selectedControlTab == ControlTab.Filters
        binding.filterPanel.isVisible = isFilterSelected
        binding.adjustPanel.isVisible = !isFilterSelected
        renderControlTab(binding.tabFilter, isFilterSelected)
        renderControlTab(binding.tabAdjust, !isFilterSelected)
    }

    private fun renderControlTab(tab: TextView, isSelected: Boolean) {
        tab.isSelected = isSelected
        tab.setBackgroundResource(
            if (isSelected) R.drawable.bg_filter_chip_selected else R.drawable.bg_filter_chip,
        )
        tab.setTextColor(getColor(if (isSelected) android.R.color.white else R.color.text_secondary))
    }

    private fun bindCategoryControls() {
        binding.buttonOriginalFilter.setOnClickListener {
            viewModel.selectFilter(FilterCatalog.default)
        }
        FilterCatalog.categories.forEachIndexed { index, category ->
            val chip = createCategoryChip(
                index = index,
                text = getString(category.nameRes),
                onClick = { viewModel.selectCategory(category) },
            )
            categoryChips[category.id] = chip
            binding.categoryContainer.addView(chip)
        }
    }

    private fun renderFilterControls(categoryId: String, sourceBitmap: Bitmap?) {
        if (renderedCategoryId == categoryId && renderedFilterThumbnailBitmap === sourceBitmap) {
            return
        }

        renderedCategoryId = categoryId
        renderedFilterThumbnailBitmap = sourceBitmap
        filterCards.clear()
        binding.filterContainer.removeAllViews()
        FilterCatalog.filtersForCategory(categoryId).forEachIndexed { index, filter ->
            val card = createFilterCard(
                index = index,
                text = getString(filter.nameRes),
                thumbnail = sourceBitmap,
                onClick = { viewModel.selectFilter(filter) },
            )
            filterCards[filter.id] = card
            binding.filterContainer.addView(card)
        }
    }

    private fun createCategoryChip(
        index: Int,
        text: String,
        onClick: () -> Unit,
    ): TextView {
        val spacing = resources.getDimensionPixelSize(R.dimen.item_spacing)
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.filter_chip_horizontal_padding)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.filter_chip_vertical_padding)
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            minHeight = resources.getDimensionPixelSize(R.dimen.filter_chip_min_height)
            setBackgroundResource(R.drawable.bg_filter_chip)
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            setTextColor(getColor(R.color.text_secondary))
            textSize = 14f
            isClickable = true
            isFocusable = true
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

    private fun createFilterCard(
        index: Int,
        text: String,
        thumbnail: Bitmap?,
        onClick: () -> Unit,
    ): View {
        val spacing = resources.getDimensionPixelSize(R.dimen.item_spacing)
        val width = resources.getDimensionPixelSize(R.dimen.filter_thumbnail_width)
        val height = resources.getDimensionPixelSize(R.dimen.filter_thumbnail_height)
        val inset = resources.getDimensionPixelSize(R.dimen.filter_thumbnail_inset)
        val labelHeight = resources.getDimensionPixelSize(R.dimen.filter_thumbnail_label_height)
        val labelPadding = resources.getDimensionPixelSize(R.dimen.filter_thumbnail_label_padding)
        return FrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_filter_card)
            contentDescription = text
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(width, height).apply {
                if (index > 0) {
                    marginStart = spacing
                }
            }
            addView(ImageView(this@MainActivity).apply {
                setImageBitmap(thumbnail)
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ).apply {
                    setMargins(inset, inset, inset, inset)
                }
            })
            addView(TextView(this@MainActivity).apply {
                this.text = text
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                setBackgroundResource(R.drawable.bg_filter_label)
                setPadding(labelPadding, 0, labelPadding, 0)
                setTextColor(getColor(android.R.color.white))
                textSize = 12f
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    labelHeight,
                    Gravity.BOTTOM,
                ).apply {
                    setMargins(inset, 0, inset, inset)
                }
            })
        }
    }

    private fun bindAdjustControls() {
        binding.buttonResetAdjust.setOnClickListener {
            viewModel.setAdjustment(
                selectedAdjustControl,
                selectedAdjustControl.valueIn(Adjustments()),
            )
        }
        binding.buttonResetAllAdjust.setOnClickListener { viewModel.resetAdjustments() }
        binding.adjustSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !isRenderingState) {
                    viewModel.setAdjustment(selectedAdjustControl, selectedAdjustControl.valueFrom(progress))
                }
            }

            override fun onStartTrackingTouch(view: SeekBar) = Unit

            override fun onStopTrackingTouch(view: SeekBar) = Unit
        })
        AdjustControl.entries.forEachIndexed { index, control ->
            binding.adjustContainer.addView(createAdjustControlItem(index, control))
        }
        renderAdjustments(lastAdjustments)
    }

    private fun createAdjustControlItem(index: Int, control: AdjustControl): View {
        val spacing = resources.getDimensionPixelSize(R.dimen.item_spacing)
        val itemWidth = resources.getDimensionPixelSize(R.dimen.adjust_item_width)
        val itemMinHeight = resources.getDimensionPixelSize(R.dimen.adjust_item_min_height)
        val dotSize = resources.getDimensionPixelSize(R.dimen.adjust_dot_size)
        val itemGap = resources.getDimensionPixelSize(R.dimen.adjust_item_gap)
        val iconSize = resources.getDimensionPixelSize(R.dimen.adjust_icon_size)
        val label = TextView(this)
        val icon = ImageView(this)
        val dot = View(this)

        adjustLabels[control] = label
        adjustIcons[control] = icon
        adjustDots[control] = dot

        return LinearLayout(this).apply {
            contentDescription = getString(control.labelRes)
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            minimumHeight = itemMinHeight
            orientation = LinearLayout.VERTICAL
            setOnClickListener {
                selectedAdjustControl = control
                renderAdjustments(lastAdjustments)
            }
            layoutParams = LinearLayout.LayoutParams(
                itemWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                if (index > 0) {
                    marginStart = spacing
                }
            }

            addView(dot.apply {
                setBackgroundResource(R.drawable.bg_adjust_changed_dot)
                visibility = View.INVISIBLE
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize)
            })
            addView(icon.apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setImageResource(control.iconRes)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    topMargin = itemGap
                }
            })
            addView(label.apply {
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                text = getString(control.labelRes)
                textSize = 10f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    topMargin = itemGap
                }
            })
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
        renderFilterControls(state.selectedCategory.id, state.sourceBitmap)
        renderOriginalAction(state)
        categoryChips.forEach { (id, chip) ->
            val isSelected = id == state.selectedCategory.id
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_filter_chip_selected else R.drawable.bg_filter_chip,
            )
            chip.setTextColor(
                getColor(if (isSelected) android.R.color.white else R.color.text_secondary),
            )
        }
        filterCards.forEach { (id, card) ->
            card.setBackgroundResource(
                if (id == state.selectedFilter.id) {
                    R.drawable.bg_filter_card_selected
                } else {
                    R.drawable.bg_filter_card
                },
            )
        }
        scrollSelectedFilterIntoView(state)
        renderAdjustments(state.adjustments)
        isRenderingState = false
    }

    private fun renderOriginalAction(state: FilterUiState) {
        val isSelected = state.selectedFilter.id == FilterCatalog.default.id
        binding.buttonOriginalFilter.setBackgroundResource(
            if (isSelected) R.drawable.bg_filter_chip_selected else R.drawable.bg_filter_chip,
        )
        binding.buttonOriginalFilter.setColorFilter(
            getColor(if (isSelected) android.R.color.white else R.color.text_secondary),
        )
    }

    private fun scrollSelectedFilterIntoView(state: FilterUiState) {
        val selectedCard = filterCards[state.selectedFilter.id] ?: return
        if (
            scrolledSelectedCategoryId == state.selectedCategory.id &&
            scrolledSelectedFilterId == state.selectedFilter.id
        ) {
            return
        }

        scrolledSelectedCategoryId = state.selectedCategory.id
        scrolledSelectedFilterId = state.selectedFilter.id
        binding.filterScrollView.post {
            binding.filterScrollView.smoothScrollTo(selectedCard.left, 0)
        }
    }

    private fun renderAdjustments(adjustments: Adjustments) {
        lastAdjustments = adjustments
        val activeValue = selectedAdjustControl.valueIn(adjustments)
        binding.adjustSeekBar.max = selectedAdjustControl.progressMax
        binding.adjustSeekBar.progress = selectedAdjustControl.progressFrom(activeValue)
        binding.adjustValueText.text = activeValue.toString()

        val defaults = Adjustments()
        AdjustControl.entries.forEach { control ->
            val isSelected = control == selectedAdjustControl
            val textColor = getColor(if (isSelected) R.color.filter_selected else R.color.adjust_text_secondary)
            adjustDots[control]?.visibility =
                if (control.valueIn(adjustments) != control.valueIn(defaults)) View.VISIBLE else View.INVISIBLE
            adjustIcons[control]?.setColorFilter(textColor)
            adjustLabels[control]?.setTextColor(textColor)
        }
    }

    private fun FilterError.toMessage(): String =
        when (this) {
            FilterError.AssetLoadFailed -> getString(R.string.asset_load_failed)
        }

    private enum class ControlTab {
        Filters,
        Adjust,
    }

}
