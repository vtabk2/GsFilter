package com.gsfilter.filter.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.core.gscore.view.RippleImageView
import com.gsfilter.filter.AdjustControl
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterCatalog
import com.gsfilter.filter.FilterCategory
import com.gsfilter.filter.FilterOption
import com.gsfilter.filter.FilterPack
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.R
import com.gsfilter.filter.ext.displayName
import com.gsfilter.filter.glide.FilterThumbnailModel
import kotlin.math.ceil

class FilterControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val style = FilterControlsStyle(context, attrs)
    private var catalog = FilterCatalog.pack
    private var selectedCategory = catalog.defaultCategory
    private var selectedFilter = catalog.defaultFilter
    private var selectedRecipe = selectedFilter.recipe
    private var adjustments = Adjustments.DEFAULT
    private var thumbnailBitmap: Bitmap? = null
    private var thumbnailKey: String? = null
    private var thumbnailGenerationId = 0
    private var showHeader = style.showHeader
    private var isOriginalPreviewPressed = false
    private var isRenderingFilterIntensity = false
    private var isRenderingBeauty = false
    private var selectedTab = ControlTab.Filter
    private var filterIntensityRowOriginalIndex = -1
    private var beautySeekRowOriginalIndex = -1
    private var adjustSeekRowOriginalIndex = -1
    private var pendingFilterIntensity: Int? = null
    private var filterIntensityDispatchPosted = false
    private val filterIntensityDispatchRunnable = Runnable {
        dispatchPendingFilterIntensity()
    }

    var onCloseClick: (() -> Unit)? = null
    var onOriginalClick: (() -> Unit)? = null
    var onControlTabSelected: ((ControlTab) -> Unit)? = null
    var onCategorySelected: ((FilterCategory) -> Unit)? = null
    var onFilterSelected: ((FilterOption) -> Unit)? = null
    var onFilterIntensityChanged: ((Int) -> Unit)? = null
    var onOriginalFilterPressedChanged: ((Boolean) -> Unit)? = null
    var onBeautyChanged: ((BeautyControl, Int) -> Unit)? = null
    var onResetBeautyClick: (() -> Unit)? = null
    var onAdjustmentChanged: ((AdjustControl, Int) -> Unit)? = null
    var onResetAllAdjustClick: (() -> Unit)? = null

    private val categoryChips = mutableMapOf<String, TextView>()
    private val filterAdapter = FilterAdapter(::selectFilter)
    private val tabFilter: LinearLayout?
    private val tabBeauty: LinearLayout?
    private val tabAdjust: LinearLayout?
    private val header: View?
    private val buttonClose: RippleImageView?
    private val buttonOriginalFilter: RippleImageView?
    private val categoryContainer: LinearLayout?
    private val categoryScroll: HorizontalScrollView?
    private val filterRecyclerView: RecyclerView?
    private val filterIntensityLabel: TextView?
    private val filterIntensitySeekBar: SeekBar?
    private val filterIntensityValue: TextView?
    private val filterIntensityRow: View?
    private val compactControls: LinearLayout?
    private val compactReset: RippleImageView?
    private val compactOriginal: RippleImageView?
    private val filterContent: LinearLayout?
    private val beautyContainer: LinearLayout?
    private val beautySeekRow: View?
    private val beautyControlsContainer: LinearLayout?
    private val beautyResetButton: RippleImageView?
    private val beautySeekBar: SeekBar?
    private val beautyValueText: TextView?
    private val beautyResetAll: TextView?
    private val adjustContainer: FrameLayout?
    private val adjustContent: AdjustControlsView
    private val adjustSeekRow: View?
    private val beautyLabels = mutableMapOf<BeautyControl, TextView>()
    private val beautyIcons = mutableMapOf<BeautyControl, ImageView>()
    private val beautyDots = mutableMapOf<BeautyControl, View>()
    private var selectedBeautyControl = BeautyControl.Smoothing

    init {
        orientation = VERTICAL
        if (background == null) setBackgroundResource(R.color.gs_panel_background)
        style.backgroundRes?.let { setBackgroundResource(it) }
        LayoutInflater.from(context).inflate(R.layout.gs_view_filter_controls, this, true)

        header = findViewById(R.id.gs_filter_header)
        header?.isClickable = false
        header?.isFocusable = false
        style.headerBackgroundRes?.let { header?.setBackgroundResource(it) }
        tabFilter = findViewById(R.id.gs_filter_tab_filter)
        tabBeauty = findViewById(R.id.gs_filter_tab_beauty)
        tabBeauty?.visibility = if (style.showBeauty) VISIBLE else GONE
        tabAdjust = findViewById(R.id.gs_filter_tab_adjust)
        buttonClose = findViewById(R.id.gs_filter_close_button)
        buttonOriginalFilter = findViewById(R.id.gs_filter_original)
        categoryContainer = findViewById(R.id.gs_filter_category_container)
        categoryScroll = findViewById(R.id.gs_filter_category_scroll)
        filterRecyclerView = findViewById(R.id.gs_filter_recycler)
        filterIntensityLabel = findViewById(R.id.gs_filter_intensity_label)
        filterIntensitySeekBar = findViewById(R.id.gs_filter_intensity_seek_bar)
        filterIntensityValue = findViewById(R.id.gs_filter_intensity_value)
        filterIntensityRow = findViewById(R.id.gs_filter_intensity_row)
        compactControls = findViewById(R.id.gs_filter_compact_controls)
        compactReset = findViewById(R.id.gs_filter_compact_reset)
        compactOriginal = findViewById(R.id.gs_filter_compact_original)
        style.compactBackgroundRes?.let { compactControls?.setBackgroundResource(it) }
        compactControls?.isClickable = false
        compactControls?.isFocusable = false
        filterContent = findViewById(R.id.gs_filter_content)
        beautyContainer = findViewById(R.id.gs_beauty_container)
        beautySeekRow = findViewById(R.id.gs_beauty_seek_row)
        beautyControlsContainer = findViewById(R.id.gs_beauty_controls_container)
        beautyResetButton = findViewById(R.id.gs_beauty_reset)
        beautySeekBar = findViewById(R.id.gs_beauty_seek_bar)
        beautyValueText = findViewById(R.id.gs_beauty_value)
        beautyResetAll = findViewById(R.id.gs_beauty_reset_all)
        adjustContainer = findViewById(R.id.gs_adjust_container)
        adjustContent = AdjustControlsView(context, attrs)
        adjustContainer?.addView(
            adjustContent,
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )
        adjustSeekRow = adjustContent.seekRow
        setShowHeader(showHeader)

        bindHeader()
        bindFilterContent()
        bindBeautyContent()
        bindAdjustContent()
        setCatalog(catalog)
        setSelectedTab(ControlTab.Filter)
    }

    fun setShowHeader(show: Boolean) {
        if (show && isOriginalPreviewPressed) {
            releaseOriginalPreview()
        }
        showHeader = show
        findViewById<View>(R.id.gs_filter_header)?.visibility = if (show) VISIBLE else GONE
        renderCategoryRowSpacing()
        renderCompactControls()
    }

    private fun renderCategoryRowSpacing() {
        val categoryRow = findViewById<View>(R.id.gs_filter_category_row) ?: return
        val params = categoryRow.layoutParams as? LinearLayout.LayoutParams ?: return
        params.topMargin = resources.getDimensionPixelSize(
            if (showHeader) {
                R.dimen.gs_filter_category_top_spacing
            } else {
                R.dimen.gs_filter_compact_category_top_spacing
            },
        )
        categoryRow.layoutParams = params
    }

    fun setSelectedTab(tab: ControlTab) {
        val effectiveTab = if (!style.showBeauty && tab == ControlTab.Beauty) ControlTab.Filter else tab
        selectedTab = effectiveTab
        renderTab(tabFilter, effectiveTab == ControlTab.Filter)
        renderTab(tabBeauty, effectiveTab == ControlTab.Beauty)
        renderTab(tabAdjust, effectiveTab == ControlTab.Adjust)
        filterContent?.visibility = if (effectiveTab == ControlTab.Filter) VISIBLE else GONE
        beautyContainer?.visibility = if (effectiveTab == ControlTab.Beauty) VISIBLE else GONE
        adjustContainer?.visibility = if (effectiveTab == ControlTab.Adjust) VISIBLE else GONE
        renderCompactControls()
    }

    fun setCatalog(catalog: FilterPack) {
        this.catalog = catalog
        selectedCategory = visibleCategoryById(selectedCategory.id) ?: visibleCategories().first()
        selectedFilter = catalog.filterById(selectedFilter.id) ?: catalog.defaultFilter
        selectedRecipe = selectedFilter.recipe
        renderCategoryChips()
        renderState()
    }

    fun setState(
        selectedCategory: FilterCategory,
        selectedFilter: FilterOption,
        thumbnailBitmap: Bitmap?,
        thumbnailKey: String?,
    ) {
        setState(
            selectedCategory = selectedCategory,
            selectedFilter = selectedFilter,
            thumbnailBitmap = thumbnailBitmap,
            thumbnailKey = thumbnailKey,
            selectedRecipe = selectedFilter.recipe,
        )
    }

    fun setState(
        selectedCategory: FilterCategory,
        selectedFilter: FilterOption,
        thumbnailBitmap: Bitmap?,
        thumbnailKey: String?,
        selectedRecipe: FilterRecipe,
    ) {
        val nextCategory = visibleCategoryById(selectedCategory.id) ?: visibleCategories().first()
        val nextFilter = catalog.filterById(selectedFilter.id) ?: selectedFilter
        val nextThumbnailGenerationId = thumbnailBitmap?.generationId ?: 0
        val thumbnailChanged =
            this.thumbnailBitmap !== thumbnailBitmap ||
                this.thumbnailKey != thumbnailKey ||
                this.thumbnailGenerationId != nextThumbnailGenerationId
        val shouldRenderFilters =
            this.selectedCategory != nextCategory ||
                this.selectedFilter != nextFilter ||
                thumbnailChanged
        val shouldRenderFilterIntensity =
            this.selectedFilter != nextFilter || this.selectedRecipe != selectedRecipe
        val selectionChanged = this.selectedCategory != nextCategory || this.selectedFilter != nextFilter
        if (
            !shouldRenderFilters &&
            !shouldRenderFilterIntensity
        ) {
            return
        }

        this.selectedCategory = nextCategory
        this.selectedFilter = nextFilter
        this.selectedRecipe = selectedRecipe
        this.thumbnailBitmap = thumbnailBitmap
        this.thumbnailKey = thumbnailKey
        this.thumbnailGenerationId = nextThumbnailGenerationId
        if (shouldRenderFilters) {
            if (selectionChanged) {
                renderOriginalAction()
                renderCategories(this.selectedCategory)
            }
            if (shouldRenderFilterIntensity) {
                renderFilterIntensity()
                renderBeauty()
            }
            renderFilterItems(scrollToSelected = selectionChanged || thumbnailChanged)
        } else {
            renderFilterIntensity()
            renderBeauty()
        }
    }

    fun setAdjustments(adjustments: Adjustments) {
        if (this.adjustments == adjustments) {
            return
        }
        this.adjustments = adjustments
        adjustContent.setAdjustments(adjustments)
        renderCompactControls()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(filterIntensityDispatchRunnable)
        pendingFilterIntensity = null
        filterIntensityDispatchPosted = false
        super.onDetachedFromWindow()
    }

    private fun renderState() {
        renderOriginalAction()
        renderCategories(selectedCategory)
        renderFilterIntensity()
        renderBeauty()
        renderFilterItems(scrollToSelected = true)
    }

    private fun renderFilterItems(scrollToSelected: Boolean) {
        val items = catalog.filtersForCategory(selectedCategory.id).map { filter ->
            FilterItem(
                filter = filter,
                isSelected = filter.id == this.selectedFilter.id,
                thumbnailBitmap = thumbnailBitmap,
                thumbnailKey = thumbnailKey,
                thumbnailGenerationId = thumbnailGenerationId,
                style = style,
            )
        }
        filterAdapter.submitList(items) {
            if (!scrollToSelected) {
                return@submitList
            }
            filterRecyclerView?.post {
                val selectedIndex = filterAdapter.currentList.indexOfFirst {
                    it.filter.id == this.selectedFilter.id
                }
                if (selectedIndex >= 0) {
                    filterRecyclerView?.scrollToPosition(selectedIndex)
                }
            }
        }
    }

    private fun bindHeader() {
        val filterLabel: TextView? = findViewById(R.id.gs_filter_tab_filter_label)
        val filterIndicator: View? = findViewById(R.id.gs_filter_tab_filter_indicator)
        val beautyLabel: TextView? = findViewById(R.id.gs_filter_tab_beauty_label)
        val beautyIndicator: View? = findViewById(R.id.gs_filter_tab_beauty_indicator)
        val adjustLabel: TextView? = findViewById(R.id.gs_filter_tab_adjust_label)
        val adjustIndicator: View? = findViewById(R.id.gs_filter_tab_adjust_indicator)
        val filterParts = TabParts(
            label = filterLabel,
            indicator = filterIndicator,
            compactGravity = Gravity.END,
        )
        val beautyParts = TabParts(
            label = beautyLabel,
            indicator = beautyIndicator,
            compactGravity = Gravity.CENTER,
        )
        val adjustParts = TabParts(
            label = adjustLabel,
            indicator = adjustIndicator,
            compactGravity = Gravity.START,
        )
        tabFilter?.tag = filterParts
        tabBeauty?.tag = beautyParts
        tabAdjust?.tag = adjustParts
        bindTabLayout(filterParts, beautyParts, adjustParts)
        listOf(filterParts, beautyParts, adjustParts).forEach { parts ->
            val indicator = parts.indicator ?: return@forEach
            indicator.background = style.tabIndicatorColor.toDrawable()
            indicator.layoutParams?.let { params ->
                params.height = style.tabIndicatorHeight
                params.width = tabIndicatorWidth(parts.label)
                (params as? LinearLayout.LayoutParams)?.let { layoutParams ->
                    layoutParams.gravity = if (style.compactTabs) parts.compactGravity else Gravity.CENTER_HORIZONTAL
                    if (style.compactTabs) {
                        layoutParams.marginStart = if (parts.compactGravity == Gravity.START) {
                            parts.label?.paddingStart ?: 0
                        } else {
                            0
                        }
                        layoutParams.marginEnd = if (parts.compactGravity == Gravity.END) {
                            parts.label?.paddingEnd ?: 0
                        } else {
                            0
                        }
                    }
                }
                indicator.layoutParams = params
            }
        }
        tabFilter?.setOnClickListener {
            setSelectedTab(ControlTab.Filter)
            onControlTabSelected?.invoke(ControlTab.Filter)
        }
        tabBeauty?.setOnClickListener {
            setSelectedTab(ControlTab.Beauty)
            onControlTabSelected?.invoke(ControlTab.Beauty)
        }
        tabAdjust?.setOnClickListener {
            setSelectedTab(ControlTab.Adjust)
            onControlTabSelected?.invoke(ControlTab.Adjust)
        }
        buttonClose?.iconRippleRes = style.closeIconRes
        style.iconPadding?.let { buttonClose?.paddingRipple = it }
        buttonClose?.setOnClickListener { onCloseClick?.invoke() }
    }

    private fun bindTabLayout(filterParts: TabParts, beautyParts: TabParts, adjustParts: TabParts) {
        listOf(tabBeauty, tabAdjust).forEach { tab ->
            (tab?.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                params.marginStart = style.tabSpacing
                tab.layoutParams = params
            }
        }
        if (!style.compactTabs) {
            return
        }
        compactTab(tabFilter, filterParts)
        compactTab(tabBeauty, beautyParts)
        compactTab(tabAdjust, adjustParts)
    }

    private fun compactTab(tab: LinearLayout?, parts: TabParts) {
        tab?.gravity = parts.compactGravity
        parts.label?.layoutParams?.let { params ->
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT
            parts.label.layoutParams = params
        }
    }

    private fun bindFilterContent() {
        buttonOriginalFilter?.iconRippleRes = style.noneIconRes
        style.iconPadding?.let { buttonOriginalFilter?.paddingRipple = it }
        buttonOriginalFilter?.contentDescription = context.getString(R.string.gs_action_show_original)
        buttonOriginalFilter?.setOnClickListener { selectFilter(catalog.defaultFilter) }
        compactReset?.iconRippleRes = R.drawable.selector_ic_gs_adjust_reset
        style.iconPadding?.let { compactReset?.paddingRipple = it }
        compactReset?.contentDescription = context.getString(R.string.gs_action_reset_filter_intensity)
        compactReset?.setOnClickListener {
            onFilterIntensityChanged?.invoke(selectedFilter.recipe.intensity)
        }
        compactOriginal?.iconRippleRes = R.drawable.ic_gs_filter_original
        style.iconPadding?.let { compactOriginal?.paddingRipple = it }
        compactOriginal?.contentDescription = context.getString(R.string.gs_action_preview_original)
        compactOriginal?.setOnClickListener { onOriginalClick?.invoke() }
        compactOriginal?.setOnLongClickListener {
            if (showHeader) {
                false
            } else {
                if (!isOriginalPreviewPressed) {
                    isOriginalPreviewPressed = true
                    onOriginalFilterPressedChanged?.invoke(true)
                }
                true
            }
        }
        compactOriginal?.setOnTouchListener { _, event ->
            if (
                !showHeader &&
                (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL)
            ) {
                releaseOriginalPreview()
            }
            false
        }
        filterRecyclerView?.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        filterRecyclerView?.adapter = filterAdapter
        filterRecyclerView?.setHasFixedSize(true)
        filterRecyclerView?.itemAnimator = null
        filterIntensityLabel?.setTextColor(style.intensityTextColor)
        filterIntensityValue?.setTextColor(style.intensityTextColor)
        filterIntensitySeekBar?.progressBackgroundTintList = ColorStateList.valueOf(style.intensityTrackColor)
        filterIntensitySeekBar?.progressTintList = ColorStateList.valueOf(style.intensityProgressColor)
        filterIntensitySeekBar?.thumbTintList = ColorStateList.valueOf(style.intensityProgressColor)
        filterIntensitySeekBar?.max = FILTER_INTENSITY_MAX
        filterIntensitySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !isRenderingFilterIntensity) {
                    dispatchFilterIntensity(progress)
                }
            }

            override fun onStartTrackingTouch(view: SeekBar) = Unit

            override fun onStopTrackingTouch(view: SeekBar) {
                dispatchFilterIntensity(view.progress, flush = true)
            }
        })
        bindSeekBarTouch(filterIntensitySeekBar)
    }

    private fun dispatchFilterIntensity(progress: Int, flush: Boolean = false) {
        pendingFilterIntensity = progress
        if (flush) {
            removeCallbacks(filterIntensityDispatchRunnable)
            filterIntensityDispatchPosted = false
            dispatchPendingFilterIntensity()
        } else if (!filterIntensityDispatchPosted) {
            filterIntensityDispatchPosted = true
            postOnAnimation(filterIntensityDispatchRunnable)
        }
    }

    private fun dispatchPendingFilterIntensity() {
        filterIntensityDispatchPosted = false
        val intensity = pendingFilterIntensity ?: return
        pendingFilterIntensity = null
        onFilterIntensityChanged?.invoke(intensity)
    }

    private fun bindSeekBarTouch(seekBar: SeekBar?) {
        seekBar?.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun renderCompactControls() {
        val compact = compactControls ?: return
        val canAdjustIntensity = style.showIntensity && selectedFilter.recipe != FilterRecipe.DEFAULT
        val useCompactControls = !showHeader
        val activeRow = if (useCompactControls) {
            when (selectedTab) {
                ControlTab.Filter -> filterIntensityRow
                ControlTab.Beauty -> beautySeekRow
                ControlTab.Adjust -> adjustSeekRow
            }
        } else {
            null
        }

        compact.visibility = if (useCompactControls) VISIBLE else GONE
        compactOriginal?.visibility = if (useCompactControls && hasOriginalPreviewChanges()) {
            VISIBLE
        } else {
            INVISIBLE
        }

        filterIntensityRowOriginalIndex = moveSeekRow(
            row = filterIntensityRow,
            originalParent = filterContent,
            originalIndex = filterIntensityRowOriginalIndex,
            compactParent = compact,
            shouldBeCompact = activeRow === filterIntensityRow,
        )
        beautySeekRowOriginalIndex = moveSeekRow(
            row = beautySeekRow,
            originalParent = beautyContainer,
            originalIndex = beautySeekRowOriginalIndex,
            compactParent = compact,
            shouldBeCompact = activeRow === beautySeekRow,
        )
        adjustSeekRowOriginalIndex = moveSeekRow(
            row = adjustSeekRow,
            originalParent = adjustContent,
            originalIndex = adjustSeekRowOriginalIndex,
            compactParent = compact,
            shouldBeCompact = activeRow === adjustSeekRow,
        )

        renderCompactSeekRow(filterIntensityRow, filterIntensitySeekBar, activeRow === filterIntensityRow)
        renderCompactSeekRow(beautySeekRow, beautySeekBar, activeRow === beautySeekRow)
        renderCompactSeekRow(adjustSeekRow, adjustContent.seekBarView, activeRow === adjustSeekRow)
        filterIntensityLabel?.visibility = if (activeRow === filterIntensityRow) GONE else VISIBLE
        compactReset?.visibility = if (activeRow === filterIntensityRow && canAdjustIntensity) VISIBLE else GONE
    }

    private fun moveSeekRow(
        row: View?,
        originalParent: ViewGroup?,
        originalIndex: Int,
        compactParent: ViewGroup,
        shouldBeCompact: Boolean,
    ): Int {
        if (row == null || originalParent == null) {
            return originalIndex
        }
        if (shouldBeCompact) {
            if (row.parent !== compactParent) {
                val savedIndex = if (originalIndex >= 0) originalIndex else originalParent.indexOfChild(row)
                (row.parent as? ViewGroup)?.removeView(row)
                val originalButtonIndex = compactOriginal?.let { compactParent.indexOfChild(it) }
                    ?: compactParent.childCount
                compactParent.addView(row, originalButtonIndex)
                return if (originalIndex >= 0) originalIndex else savedIndex
            }
        } else if (row.parent !== originalParent) {
            (row.parent as? ViewGroup)?.removeView(row)
            originalParent.addView(row, originalIndex.coerceIn(0, originalParent.childCount))
        }
        return originalIndex
    }

    private fun renderCompactSeekRow(row: View?, seekBar: SeekBar?, isCompact: Boolean) {
        row ?: return
        val rowParams = row.layoutParams as? LinearLayout.LayoutParams ?: return
        val width = if (isCompact) 0 else LayoutParams.MATCH_PARENT
        val weight = if (isCompact) 1f else 0f
        val topMargin = if (isCompact) {
            0
        } else {
            resources.getDimensionPixelSize(R.dimen.gs_filter_category_top_spacing)
        }
        if (rowParams.width != width || rowParams.weight != weight || rowParams.topMargin != topMargin) {
            rowParams.width = width
            rowParams.weight = weight
            rowParams.topMargin = topMargin
            row.layoutParams = rowParams
        }
        seekBar?.layoutParams?.let { seekBarParams ->
            val height = if (isCompact) {
                resources.getDimensionPixelSize(R.dimen.gs_filter_compact_seekbar_touch_height)
            } else {
                LayoutParams.WRAP_CONTENT
            }
            if (seekBarParams.height != height) {
                seekBarParams.height = height
                seekBar.layoutParams = seekBarParams
            }
        }
    }

    private fun hasOriginalPreviewChanges(): Boolean =
        selectedRecipe != FilterRecipe.DEFAULT || adjustments != Adjustments.DEFAULT

    private fun releaseOriginalPreview() {
        if (!isOriginalPreviewPressed) {
            return
        }
        isOriginalPreviewPressed = false
        onOriginalFilterPressedChanged?.invoke(false)
    }

    private fun bindBeautyContent() {
        beautyControlsContainer?.let { container ->
            BeautyControl.entries.forEachIndexed { index, control ->
                container.addView(createBeautyControlItem(index, control, container))
            }
        }
        beautyResetButton?.iconRippleRes = R.drawable.selector_ic_gs_adjust_reset
        style.iconPadding?.let { beautyResetButton?.paddingRipple = it }
        beautyResetButton?.setOnClickListener {
            onBeautyChanged?.invoke(
                selectedBeautyControl,
                beautyValue(selectedBeautyControl, selectedFilter.recipe),
            )
        }
        beautySeekBar?.max = BEAUTY_MAX
        beautySeekBar?.progressBackgroundTintList = ColorStateList.valueOf(style.intensityTrackColor)
        beautySeekBar?.progressTintList = ColorStateList.valueOf(style.intensityProgressColor)
        beautySeekBar?.thumbTintList = ColorStateList.valueOf(style.intensityProgressColor)
        beautySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !isRenderingBeauty) {
                    onBeautyChanged?.invoke(selectedBeautyControl, progress)
                }
            }

            override fun onStartTrackingTouch(view: SeekBar) = Unit

            override fun onStopTrackingTouch(view: SeekBar) = Unit
        })
        bindSeekBarTouch(beautySeekBar)
        beautyValueText?.setTextColor(style.intensityTextColor)
        beautyResetAll?.text = context.getString(R.string.gs_action_reset_beauty)
        beautyResetAll?.setTextColor(style.intensityTextColor)
        beautyResetAll?.setOnClickListener { onResetBeautyClick?.invoke() }
    }

    private fun createBeautyControlItem(index: Int, control: BeautyControl, parent: LinearLayout): View {
        val item = LayoutInflater.from(context).inflate(
            R.layout.gs_item_beauty_control,
            parent,
            false,
        )
        val label: TextView = item.findViewById(R.id.gs_beauty_item_label)
        val icon: ImageView = item.findViewById(R.id.gs_beauty_item_icon)
        val dot: View = item.findViewById(R.id.gs_beauty_changed_dot)
        beautyLabels[control] = label
        beautyIcons[control] = icon
        beautyDots[control] = dot
        return item.apply {
            setOnClickListener {
                selectedBeautyControl = control
                renderBeauty()
            }
            if (index > 0) {
                (layoutParams as? LayoutParams)?.marginStart = itemSpacing()
            }
            dot.backgroundTintList = ColorStateList.valueOf(style.intensityProgressColor)
            icon.setImageResource(control.iconRes)
            label.setText(control.labelRes)
        }
    }

    private fun bindAdjustContent() {
        bindSeekBarTouch(adjustContent.seekBarView)
        adjustContent.onAdjustmentChanged = { control, value ->
            onAdjustmentChanged?.invoke(control, value)
        }
        adjustContent.onResetAllClick = {
            onResetAllAdjustClick?.invoke()
        }
    }

    private fun renderCategoryChips() {
        val container = categoryContainer ?: return
        categoryChips.clear()
        container.removeAllViews()
        visibleCategories().forEachIndexed { index, category ->
            val chip = createCategoryChip(
                index = index,
                text = category.displayName(context).toString(),
                onClick = { selectCategory(category) },
            ) ?: return@forEachIndexed
            categoryChips[category.id] = chip
            container.addView(chip)
        }
    }

    private fun selectCategory(category: FilterCategory) {
        val selectedFilterCategory = visibleCategories().firstOrNull { it.id in selectedFilter.categoryIds }
        val nextCategory =
            if (
                selectedCategory.id == category.id &&
                category.id !in selectedFilter.categoryIds &&
                selectedFilterCategory != null
            ) {
                selectedFilterCategory
            } else {
                category
            }
        if (selectedCategory.id == nextCategory.id) {
            return
        }

        selectedCategory = nextCategory
        renderState()
        onCategorySelected?.invoke(selectedCategory)
    }

    private fun visibleCategories(): List<FilterCategory> {
        if (style.showPopular) {
            return catalog.categories
        }
        return catalog.categories.filterNot { it.id == POPULAR_CATEGORY_ID }.ifEmpty { catalog.categories }
    }

    private fun visibleCategoryById(id: String): FilterCategory? =
        visibleCategories().firstOrNull { it.id == id }

    private fun selectFilter(filter: FilterOption) {
        selectedFilter = filter
        selectedRecipe = filter.recipe
        renderState()
        onFilterSelected?.invoke(filter)
    }

    private fun renderFilterIntensity() {
        val canAdjustIntensity = style.showIntensity && selectedFilter.recipe != FilterRecipe.DEFAULT
        filterIntensityRow?.visibility = when {
            canAdjustIntensity -> VISIBLE
            else -> INVISIBLE
        }
        compactReset?.visibility = if (!showHeader && canAdjustIntensity) VISIBLE else GONE
        if (!showHeader) {
            renderCompactControls()
        }
        if (!canAdjustIntensity) {
            return
        }

        val intensity = selectedRecipe.intensity.coerceIn(0, FILTER_INTENSITY_MAX)
        isRenderingFilterIntensity = true
        filterIntensitySeekBar?.progress = intensity
        filterIntensityValue?.text = intensity.toString()
        isRenderingFilterIntensity = false
    }

    private fun renderBeauty() {
        val activeValue = beautyValue(selectedBeautyControl, selectedRecipe).coerceIn(0, BEAUTY_MAX)
        val defaultValue = beautyValue(selectedBeautyControl, selectedFilter.recipe).coerceIn(0, BEAUTY_MAX)
        isRenderingBeauty = true
        beautySeekBar?.max = BEAUTY_MAX
        beautySeekBar?.progress = activeValue
        beautyValueText?.text = activeValue.toString()
        beautyResetButton?.isEnabled = activeValue != defaultValue

        var hasChangedValue = false
        BeautyControl.entries.forEach { control ->
            val isSelected = control == selectedBeautyControl
            val hasChanged = beautyValue(control, selectedRecipe) != beautyValue(control, selectedFilter.recipe)
            hasChangedValue = hasChangedValue || hasChanged
            val textColor = if (isSelected) style.intensityProgressColor else style.intensityTextColor
            beautyDots[control]?.visibility = if (hasChanged) VISIBLE else INVISIBLE
            beautyIcons[control]?.setColorFilter(textColor)
            beautyLabels[control]?.setTextColor(textColor)
        }
        beautyResetAll?.isEnabled = hasChangedValue
        isRenderingBeauty = false
    }

    private fun beautyValue(control: BeautyControl, recipe: FilterRecipe): Int =
        when (control) {
            BeautyControl.Smoothing -> recipe.skinSmoothing
            BeautyControl.Whitening -> recipe.skinWhitening
            BeautyControl.Blush -> recipe.blush
            BeautyControl.Lipstick -> recipe.lipstick
            BeautyControl.UnderEye -> recipe.underEye
            BeautyControl.TeethWhitening -> recipe.teethWhitening
            BeautyControl.EyeShadow -> recipe.eyeShadow
            BeautyControl.Eyeliner -> recipe.eyeliner
            BeautyControl.Eyebrow -> recipe.eyebrow
            BeautyControl.FaceSlimming -> recipe.faceSlimming
            BeautyControl.EyeEnlargement -> recipe.eyeEnlargement
        }

    private fun renderTab(tab: LinearLayout?, isSelected: Boolean) {
        tab?.isSelected = isSelected
        if (style.useTabBackground) {
            tab?.setBackgroundResource(if (isSelected) style.selectedTabBackgroundRes else style.tabBackgroundRes)
        } else {
            tab?.background = null
        }
        val parts = tab?.tag as? TabParts
        parts?.label?.setTextColor(if (isSelected) style.selectedTabTextColor else style.tabTextColor)
        parts?.indicator?.visibility = if (style.showTabIndicator && isSelected) VISIBLE else INVISIBLE
    }

    private fun tabIndicatorWidth(label: TextView?): Int =
        when (style.tabIndicatorWidthMode) {
            TAB_INDICATOR_WIDTH_MIN -> style.tabIndicatorMinWidth
            TAB_INDICATOR_WIDTH_TEXT -> label?.textWidth() ?: style.tabIndicatorMinWidth
            else -> ViewGroup.LayoutParams.MATCH_PARENT
        }

    private fun TextView.textWidth(): Int = ceil(paint.measureText(text.toString()).toDouble()).toInt()

    private fun createCategoryChip(index: Int, text: String, onClick: () -> Unit): TextView? {
        val container = categoryContainer ?: return null
        val chip = LayoutInflater.from(context).inflate(
            R.layout.gs_item_filter_category,
            container,
            false,
        ) as? TextView ?: return null
        return chip.apply {
            this.text = text
            setBackgroundResource(style.chipBackgroundRes)
            setTextColor(style.textColor)
            setOnClickListener { onClick() }
            if (index > 0) {
                (layoutParams as? LayoutParams)?.marginStart = itemSpacing()
            }
        }
    }

    private fun renderOriginalAction() {
        buttonOriginalFilter?.isSelected = false
    }

    private fun renderCategories(selectedCategory: FilterCategory) {
        categoryChips.forEach { (id, chip) ->
            val isSelected = id == selectedCategory.id
            chip.setBackgroundResource(if (isSelected) style.selectedChipBackgroundRes else style.chipBackgroundRes)
            chip.setTextColor(if (isSelected) style.selectedTextColor else style.textColor)
        }
        val selectedChip = categoryChips[selectedCategory.id] ?: return
        categoryScroll?.post {
            selectedChip.requestRectangleOnScreen(
                Rect(0, 0, selectedChip.width, selectedChip.height),
                true,
            )
        }
    }

    private fun itemSpacing(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing)

    private companion object {
        const val FILTER_INTENSITY_MAX = 100
        const val BEAUTY_MAX = 100
        const val TAB_INDICATOR_WIDTH_FULL = 0
        const val TAB_INDICATOR_WIDTH_MIN = 1
        const val TAB_INDICATOR_WIDTH_TEXT = 2
        const val POPULAR_CATEGORY_ID = "popular"
    }

    enum class ControlTab {
        Filter,
        Beauty,
        Adjust,
    }

    enum class BeautyControl(
        val labelRes: Int,
        val iconRes: Int,
    ) {
        Smoothing(R.string.gs_beauty_smoothing, R.drawable.ic_gs_beauty_smoothing),
        Whitening(R.string.gs_beauty_whitening, R.drawable.ic_gs_beauty_whitening),
        Blush(R.string.gs_beauty_blush, R.drawable.ic_gs_beauty_blush),
        Lipstick(R.string.gs_beauty_lipstick, R.drawable.ic_gs_beauty_lipstick),
        UnderEye(R.string.gs_beauty_under_eye, R.drawable.ic_gs_beauty_under_eye),
        TeethWhitening(R.string.gs_beauty_teeth_whitening, R.drawable.ic_gs_beauty_teeth),
        EyeShadow(R.string.gs_beauty_eye_shadow, R.drawable.ic_gs_beauty_eye_shadow),
        Eyeliner(R.string.gs_beauty_eyeliner, R.drawable.ic_gs_beauty_eyeliner),
        Eyebrow(R.string.gs_beauty_eyebrow, R.drawable.ic_gs_beauty_eyebrow),
        FaceSlimming(R.string.gs_beauty_face_slimming, R.drawable.ic_gs_beauty_face_slimming),
        EyeEnlargement(R.string.gs_beauty_eye_enlargement, R.drawable.ic_gs_beauty_eye_enlargement),
    }

    private data class TabParts(
        val label: TextView?,
        val indicator: View?,
        val compactGravity: Int,
    )

    private data class FilterControlsStyle(
        val textColor: Int,
        val selectedTextColor: Int,
        val tabTextColor: Int,
        val selectedTabTextColor: Int,
        val chipBackgroundRes: Int,
        val selectedChipBackgroundRes: Int,
        val tabBackgroundRes: Int,
        val selectedTabBackgroundRes: Int,
        val useTabBackground: Boolean,
        val cardBackgroundRes: Int,
        val selectedCardBackgroundRes: Int,
        val cardForegroundRes: Int,
        val selectedCardForegroundRes: Int,
        val labelBackgroundRes: Int,
        val backgroundRes: Int?,
        val headerBackgroundRes: Int?,
        val compactBackgroundRes: Int?,
        val labelTextColor: Int,
        val closeIconRes: Int,
        val noneIconRes: Int,
        val iconPadding: Float?,
        val showTabIndicator: Boolean,
        val tabIndicatorColor: Int,
        val tabIndicatorHeight: Int,
        val tabIndicatorWidthMode: Int,
        val tabIndicatorMinWidth: Int,
        val compactTabs: Boolean,
        val tabSpacing: Int,
        val showHeader: Boolean,
        val showBeauty: Boolean,
        val showIntensity: Boolean,
        val showPopular: Boolean,
        val intensityTextColor: Int,
        val intensityProgressColor: Int,
        val intensityTrackColor: Int,
    ) {
        constructor(context: Context, attrs: AttributeSet?) : this(
            context = context,
            array = context.obtainStyledAttributes(attrs, R.styleable.FilterControlsView),
        )

        private constructor(context: Context, array: TypedArray) : this(
            textColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterTextColor,
                context.getColor(R.color.gs_text_secondary),
            ),
            selectedTextColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterSelectedTextColor,
                Color.WHITE,
            ),
            tabTextColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterTabTextColor,
                array.getColor(
                    R.styleable.FilterControlsView_gsFilterTextColor,
                    context.getColor(R.color.gs_text_secondary),
                ),
            ),
            selectedTabTextColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterSelectedTabTextColor,
                if (array.getBoolean(R.styleable.FilterControlsView_gsFilterUseTabBackground, true)) {
                    array.getColor(R.styleable.FilterControlsView_gsFilterSelectedTextColor, Color.WHITE)
                } else {
                    array.getColor(
                        R.styleable.FilterControlsView_gsFilterSelectedColor,
                        context.getColor(R.color.gs_filter_selected),
                    )
                },
            ),
            chipBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterChipBackground,
                R.drawable.gs_bg_filter_chip,
            ),
            selectedChipBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterSelectedChipBackground,
                R.drawable.gs_bg_filter_chip_selected,
            ),
            tabBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterTabBackground,
                array.getResourceId(
                    R.styleable.FilterControlsView_gsFilterChipBackground,
                    R.drawable.gs_bg_filter_chip,
                ),
            ),
            selectedTabBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterSelectedTabBackground,
                array.getResourceId(
                    R.styleable.FilterControlsView_gsFilterSelectedChipBackground,
                    R.drawable.gs_bg_filter_chip_selected,
                ),
            ),
            useTabBackground = array.getBoolean(R.styleable.FilterControlsView_gsFilterUseTabBackground, true),
            cardBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterCardBackground,
                R.drawable.gs_bg_filter_card,
            ),
            selectedCardBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterSelectedCardBackground,
                R.drawable.gs_bg_filter_card_selected,
            ),
            cardForegroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterCardForeground,
                R.drawable.gs_fg_filter_card,
            ),
            selectedCardForegroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterSelectedCardForeground,
                R.drawable.gs_fg_filter_card_selected,
            ),
            labelBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterLabelBackground,
                R.drawable.gs_bg_filter_label,
            ),
            backgroundRes = if (array.hasValue(R.styleable.FilterControlsView_gsFilterBackground)) {
                array.getResourceId(R.styleable.FilterControlsView_gsFilterBackground, 0)
            } else {
                null
            },
            headerBackgroundRes = if (array.hasValue(R.styleable.FilterControlsView_gsFilterHeaderBackground)) {
                array.getResourceId(R.styleable.FilterControlsView_gsFilterHeaderBackground, 0)
            } else {
                null
            },
            compactBackgroundRes = if (array.hasValue(R.styleable.FilterControlsView_gsFilterCompactBackground)) {
                array.getResourceId(R.styleable.FilterControlsView_gsFilterCompactBackground, 0)
            } else {
                null
            },
            labelTextColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterLabelTextColor,
                Color.WHITE,
            ),
            closeIconRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterCloseIcon,
                R.drawable.ic_gs_tick,
            ),
            noneIconRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterNoneIcon,
                R.drawable.ic_gs_filter_none,
            ),
            iconPadding = if (array.hasValue(R.styleable.FilterControlsView_gsFilterIconPadding)) {
                array.getDimension(R.styleable.FilterControlsView_gsFilterIconPadding, 0f)
            } else {
                null
            },
            showTabIndicator = array.getBoolean(R.styleable.FilterControlsView_gsFilterShowTabIndicator, false),
            tabIndicatorColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterTabIndicatorColor,
                array.getColor(
                    R.styleable.FilterControlsView_gsFilterSelectedColor,
                    context.getColor(R.color.gs_filter_selected),
                ),
            ),
            tabIndicatorHeight = array.getDimensionPixelSize(
                R.styleable.FilterControlsView_gsFilterTabIndicatorHeight,
                context.resources.getDimensionPixelSize(R.dimen.gs_filter_tab_indicator_height),
            ),
            tabIndicatorWidthMode = array.getInt(
                R.styleable.FilterControlsView_gsFilterTabIndicatorWidthMode,
                TAB_INDICATOR_WIDTH_FULL,
            ),
            tabIndicatorMinWidth = array.getDimensionPixelSize(
                R.styleable.FilterControlsView_gsFilterTabIndicatorMinWidth,
                context.resources.getDimensionPixelSize(R.dimen.gs_filter_chip_min_width),
            ),
            compactTabs = array.getBoolean(R.styleable.FilterControlsView_gsFilterCompactTabs, false),
            tabSpacing = array.getDimensionPixelSize(
                R.styleable.FilterControlsView_gsFilterTabSpacing,
                context.resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing),
            ),
            showHeader = array.getBoolean(R.styleable.FilterControlsView_gsFilterShowHeader, true),
            showBeauty = array.getBoolean(R.styleable.FilterControlsView_gsFilterShowBeauty, true),
            showIntensity = array.getBoolean(R.styleable.FilterControlsView_gsFilterShowIntensity, true),
            showPopular = array.getBoolean(R.styleable.FilterControlsView_gsFilterShowPopular, true),
            intensityTextColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterIntensityTextColor,
                array.getColor(
                    R.styleable.FilterControlsView_gsFilterTextColor,
                    context.getColor(R.color.gs_text_secondary),
                ),
            ),
            intensityProgressColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterIntensityProgressColor,
                array.getColor(
                    R.styleable.FilterControlsView_gsFilterSelectedColor,
                    context.getColor(R.color.gs_filter_selected),
                ),
            ),
            intensityTrackColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterIntensityTrackColor,
                context.getColor(R.color.gs_adjust_track_background),
            ),
        ) {
            array.recycle()
        }
    }

    private data class FilterItem(
        val filter: FilterOption,
        val isSelected: Boolean,
        val thumbnailBitmap: Bitmap?,
        val thumbnailKey: String?,
        val thumbnailGenerationId: Int,
        val style: FilterControlsStyle,
    )

    private class FilterAdapter(
        private val onFilterSelected: (FilterOption) -> Unit,
    ) : ListAdapter<FilterItem, FilterAdapter.FilterHolder>(DIFF) {

        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long = getItem(position).filter.id.hashCode().toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterHolder =
            FilterHolder(
                itemView = LayoutInflater.from(parent.context).inflate(
                    R.layout.gs_item_filter_option,
                    parent,
                    false,
                ),
                onFilterSelected = onFilterSelected,
            )

        override fun onBindViewHolder(holder: FilterHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onBindViewHolder(holder: FilterHolder, position: Int, payloads: MutableList<Any>) {
            when {
                payloads.contains(PAYLOAD_SELECTION) && payloads.contains(PAYLOAD_THUMBNAIL) -> {
                    holder.bind(getItem(position))
                }
                payloads.contains(PAYLOAD_SELECTION) -> holder.bindSelection(getItem(position))
                payloads.contains(PAYLOAD_THUMBNAIL) -> holder.bindThumbnail(getItem(position))
                else -> holder.bind(getItem(position))
            }
        }

        override fun onViewRecycled(holder: FilterHolder) {
            holder.clear()
        }

        class FilterHolder(
            itemView: View,
            private val onFilterSelected: (FilterOption) -> Unit,
        ) : RecyclerView.ViewHolder(itemView) {

            private val image: ImageView? = itemView.findViewById(R.id.gs_filter_option_image)
            private val label: TextView? = itemView.findViewById(R.id.gs_filter_option_label)

            fun bind(item: FilterItem) {
                val style = item.style
                bindSelection(item)
                itemView.setOnClickListener { onFilterSelected(item.filter) }
                label?.setBackgroundResource(style.labelBackgroundRes)
                label?.setTextColor(style.labelTextColor)
                label?.text = item.filter.displayName(itemView.context)
                bindThumbnail(item)
            }

            fun bindThumbnail(item: FilterItem) {
                val source = item.thumbnailBitmap
                val sourceKey = item.thumbnailKey
                val imageView = image ?: return
                if (source == null || sourceKey == null) {
                    Glide.with(imageView).clear(imageView)
                    imageView.setImageBitmap(source)
                    return
                }

                Glide.with(imageView)
                    .load(FilterThumbnailModel(sourceKey, source, item.filter))
                    .placeholder(source.toDrawable(itemView.resources))
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .dontAnimate()
                    .into(imageView)
            }

            fun bindSelection(item: FilterItem) {
                val style = item.style
                itemView.setBackgroundResource(
                    if (item.isSelected) style.selectedCardBackgroundRes else style.cardBackgroundRes,
                )
                itemView.foreground = itemView.context.getDrawable(
                    if (item.isSelected) style.selectedCardForegroundRes else style.cardForegroundRes,
                )
                itemView.isSelected = item.isSelected
            }

            fun clear() {
                image?.let { Glide.with(it).clear(it) }
            }
        }

        private companion object {
            const val PAYLOAD_SELECTION = "selection"
            const val PAYLOAD_THUMBNAIL = "thumbnail"

            val DIFF = object : DiffUtil.ItemCallback<FilterItem>() {
                override fun areItemsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean =
                    oldItem.filter.id == newItem.filter.id

                override fun areContentsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean =
                    oldItem == newItem

                override fun getChangePayload(oldItem: FilterItem, newItem: FilterItem): Any? =
                    if (oldItem.filter.id != newItem.filter.id || oldItem.style != newItem.style) {
                        null
                    } else {
                        val selectionChanged = oldItem.isSelected != newItem.isSelected
                        val thumbnailChanged =
                            oldItem.thumbnailBitmap !== newItem.thumbnailBitmap ||
                                oldItem.thumbnailKey != newItem.thumbnailKey ||
                                oldItem.thumbnailGenerationId != newItem.thumbnailGenerationId
                        when {
                            selectionChanged && !thumbnailChanged -> PAYLOAD_SELECTION
                            thumbnailChanged && !selectionChanged -> PAYLOAD_THUMBNAIL
                            else -> null
                        }
                    }
            }
        }
    }
}
