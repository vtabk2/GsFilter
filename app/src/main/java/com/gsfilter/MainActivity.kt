package com.gsfilter

import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
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
import com.gsfilter.filter.AdjustControl
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterControlsView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: FilterViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private val adjustLabels = mutableMapOf<AdjustControl, TextView>()
    private val adjustIcons = mutableMapOf<AdjustControl, ImageView>()
    private val adjustDots = mutableMapOf<AdjustControl, View>()
    private var selectedAdjustControl = AdjustControl.Brightness
    private var selectedControlTab = FilterControlsView.ControlTab.Filter
    private var lastAdjustments = Adjustments()
    private var renderedBitmap: Bitmap? = null
    private var isRenderingState = false

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
        binding.adjustPanel.isVisible = selectedControlTab == FilterControlsView.ControlTab.Adjust
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
        binding.filterControls.setState(
            selectedCategory = state.selectedCategory,
            selectedFilter = state.selectedFilter,
            thumbnailBitmap = state.filterThumbnailBitmap,
            thumbnailKey = state.filterThumbnailKey,
        )
        renderAdjustments(state.adjustments)
        isRenderingState = false
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
}
