package com.gsfilter.filter.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.gsfilter.filter.FilterPackJson
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.R
import com.gsfilter.filter.ext.displayName
import com.gsfilter.filter.glide.FilterThumbnailModel
import java.io.IOException
import java.util.concurrent.Executors

class FilterControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val style = FilterControlsStyle(context, attrs)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var catalogLoadVersion = 0
    private var catalog = FilterCatalog.pack
    private var selectedCategory = catalog.defaultCategory
    private var selectedFilter = catalog.defaultFilter
    private var selectedRecipe = selectedFilter.recipe
    private var thumbnailBitmap: Bitmap? = null
    private var thumbnailKey: String? = null
    private var thumbnailGenerationId = 0
    private var lastPreloadKey: String? = null
    private var isRenderingFilterIntensity = false

    var onCloseClick: (() -> Unit)? = null
    var onControlTabSelected: ((ControlTab) -> Unit)? = null
    var onCategorySelected: ((FilterCategory) -> Unit)? = null
    var onFilterSelected: ((FilterOption) -> Unit)? = null
    var onFilterIntensityChanged: ((Int) -> Unit)? = null
    var onAdjustmentChanged: ((AdjustControl, Int) -> Unit)? = null
    var onResetAllAdjustClick: (() -> Unit)? = null
    var onCatalogLoaded: ((FilterPack) -> Unit)? = null
    var onCatalogLoadFailed: ((Throwable) -> Unit)? = null

    private val categoryChips = mutableMapOf<String, TextView>()
    private val filterAdapter = FilterAdapter(::selectFilter)
    private val tabFilter: LinearLayout?
    private val tabAdjust: LinearLayout?
    private val buttonClose: RippleImageView?
    private val buttonOriginalFilter: RippleImageView?
    private val categoryContainer: LinearLayout?
    private val filterRecyclerView: RecyclerView?
    private val filterIntensityLabel: TextView?
    private val filterIntensitySeekBar: SeekBar?
    private val filterIntensityValue: TextView?
    private val filterIntensityRow: View?
    private val filterContent: LinearLayout?
    private val adjustContainer: FrameLayout?
    private val adjustContent: AdjustControlsView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.gs_view_filter_controls, this, true)

        tabFilter = findViewById(R.id.gs_filter_tab_filter)
        tabAdjust = findViewById(R.id.gs_filter_tab_adjust)
        buttonClose = findViewById(R.id.gs_filter_close_button)
        buttonOriginalFilter = findViewById(R.id.gs_filter_original_button)
        categoryContainer = findViewById(R.id.gs_filter_category_container)
        filterRecyclerView = findViewById(R.id.gs_filter_recycler)
        filterIntensityLabel = findViewById(R.id.gs_filter_intensity_label)
        filterIntensitySeekBar = findViewById(R.id.gs_filter_intensity_seek_bar)
        filterIntensityValue = findViewById(R.id.gs_filter_intensity_value)
        filterIntensityRow = findViewById(R.id.gs_filter_intensity_row)
        filterContent = findViewById(R.id.gs_filter_content)
        adjustContainer = findViewById(R.id.gs_adjust_container)
        adjustContent = AdjustControlsView(context, attrs)
        adjustContainer?.addView(
            adjustContent,
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )

        bindHeader()
        bindFilterContent()
        bindAdjustContent()
        setCatalog(catalog)
        setSelectedTab(ControlTab.Filter)
        style.catalogAssetPath?.let(::loadCatalogFromAssets)
    }

    fun setSelectedTab(tab: ControlTab) {
        val isFilterSelected = tab == ControlTab.Filter
        renderTab(tabFilter, isFilterSelected)
        renderTab(tabAdjust, !isFilterSelected)
        filterContent?.visibility = if (isFilterSelected) VISIBLE else GONE
        adjustContainer?.visibility = if (isFilterSelected) GONE else VISIBLE
    }

    fun setCatalog(catalog: FilterPack) {
        catalogLoadVersion++
        this.catalog = catalog
        selectedCategory = catalog.categoryById(selectedCategory.id) ?: catalog.defaultCategory
        selectedFilter = catalog.filterById(selectedFilter.id) ?: catalog.defaultFilter
        selectedRecipe = selectedFilter.recipe
        renderCategoryChips()
        renderState()
    }

    fun loadCatalogFromAssets(assetPath: String) {
        val loadVersion = ++catalogLoadVersion
        CATALOG_EXECUTOR.execute {
            val result = runCatching {
                context.applicationContext.assets.open(assetPath).use { input ->
                    FilterPackJson.parse(input.bufferedReader().readText())
                }
            }.recoverCatching { error ->
                throw IOException("Cannot load filter catalog asset: $assetPath", error)
            }

            mainHandler.post {
                if (loadVersion != catalogLoadVersion) {
                    return@post
                }
                result
                    .onSuccess { loadedCatalog ->
                        setCatalog(loadedCatalog)
                        onCatalogLoaded?.invoke(loadedCatalog)
                    }
                    .onFailure { error -> onCatalogLoadFailed?.invoke(error) }
            }
        }
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
        val nextCategory = catalog.categoryById(selectedCategory.id) ?: catalog.defaultCategory
        val nextFilter = catalog.filterById(selectedFilter.id) ?: selectedFilter
        val nextThumbnailGenerationId = thumbnailBitmap?.generationId ?: 0
        val shouldRenderFilters =
            this.selectedCategory != nextCategory ||
                this.selectedFilter != nextFilter ||
                this.thumbnailBitmap !== thumbnailBitmap ||
                this.thumbnailKey != thumbnailKey ||
                this.thumbnailGenerationId != nextThumbnailGenerationId
        val shouldRenderFilterIntensity =
            this.selectedFilter != nextFilter || this.selectedRecipe != selectedRecipe
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
            renderState()
        } else {
            renderFilterIntensity()
        }
    }

    fun setAdjustments(adjustments: Adjustments) {
        adjustContent.setAdjustments(adjustments)
    }

    override fun onDetachedFromWindow() {
        catalogLoadVersion++
        super.onDetachedFromWindow()
    }

    private fun renderState() {
        renderOriginalAction()
        renderCategories(selectedCategory)
        renderFilterIntensity()
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
            val selectedIndex = items.indexOfFirst { it.filter.id == this.selectedFilter.id }
            if (selectedIndex >= 0) {
                filterRecyclerView?.scrollToPosition(selectedIndex)
            }
        }
        preloadFilterThumbnails(items)
    }

    private fun preloadFilterThumbnails(items: List<FilterItem>) {
        val models = items.mapNotNull { item ->
            val source = item.thumbnailBitmap ?: return@mapNotNull null
            val sourceKey = item.thumbnailKey ?: return@mapNotNull null
            FilterThumbnailModel(sourceKey, source, item.filter)
        }
        if (models.isEmpty()) {
            lastPreloadKey = null
            return
        }

        val preloadKey = models.joinToString(separator = "|") { it.cacheKey }
        if (preloadKey == lastPreloadKey) {
            return
        }
        lastPreloadKey = preloadKey

        val width = resources.getDimensionPixelSize(R.dimen.gs_filter_thumbnail_width)
        val height = resources.getDimensionPixelSize(R.dimen.gs_filter_thumbnail_height)
        val requestManager = Glide.with(this)
        models.forEach { model ->
            requestManager
                .load(model)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .preload(width, height)
        }
    }

    private fun bindHeader() {
        val filterLabel: TextView? = findViewById(R.id.gs_filter_tab_filter_label)
        val filterIndicator: View? = findViewById(R.id.gs_filter_tab_filter_indicator)
        val adjustLabel: TextView? = findViewById(R.id.gs_filter_tab_adjust_label)
        val adjustIndicator: View? = findViewById(R.id.gs_filter_tab_adjust_indicator)
        val filterParts = TabParts(
            label = filterLabel,
            indicator = filterIndicator,
        )
        val adjustParts = TabParts(
            label = adjustLabel,
            indicator = adjustIndicator,
        )
        tabFilter?.tag = filterParts
        tabAdjust?.tag = adjustParts
        listOfNotNull(filterParts.indicator, adjustParts.indicator).forEach { indicator ->
            indicator.background = style.tabIndicatorColor.toDrawable()
            indicator.layoutParams?.let { params ->
                params.height = style.tabIndicatorHeight
                indicator.layoutParams = params
            }
        }
        tabFilter?.setOnClickListener {
            setSelectedTab(ControlTab.Filter)
            onControlTabSelected?.invoke(ControlTab.Filter)
        }
        tabAdjust?.setOnClickListener {
            setSelectedTab(ControlTab.Adjust)
            onControlTabSelected?.invoke(ControlTab.Adjust)
        }
        buttonClose?.iconRippleRes = style.closeIconRes
        style.iconPadding?.let { buttonClose?.paddingRipple = it }
        buttonClose?.setOnClickListener { onCloseClick?.invoke() }
    }

    private fun bindFilterContent() {
        buttonOriginalFilter?.iconRippleRes = style.noneIconRes
        style.iconPadding?.let { buttonOriginalFilter?.paddingRipple = it }
        buttonOriginalFilter?.setOnClickListener { selectFilter(catalog.defaultFilter) }
        filterRecyclerView?.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        filterRecyclerView?.adapter = filterAdapter
        filterRecyclerView?.itemAnimator = null
        filterIntensityLabel?.setTextColor(style.intensityTextColor)
        filterIntensityValue?.setTextColor(style.intensityTextColor)
        filterIntensitySeekBar?.progressBackgroundTintList = ColorStateList.valueOf(style.intensityTrackColor)
        filterIntensitySeekBar?.progressTintList = ColorStateList.valueOf(style.intensityProgressColor)
        filterIntensitySeekBar?.thumbTintList = ColorStateList.valueOf(style.intensityTextColor)
        filterIntensitySeekBar?.max = FILTER_INTENSITY_MAX
        filterIntensitySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !isRenderingFilterIntensity) {
                    onFilterIntensityChanged?.invoke(progress)
                }
            }

            override fun onStartTrackingTouch(view: SeekBar) = Unit

            override fun onStopTrackingTouch(view: SeekBar) = Unit
        })
    }

    private fun bindAdjustContent() {
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
        catalog.categories.forEachIndexed { index, category ->
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
        val selectedFilterCategory = catalog.categoryForFilter(selectedFilter)
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

    private fun selectFilter(filter: FilterOption) {
        selectedFilter = filter
        selectedRecipe = filter.recipe
        renderState()
        onFilterSelected?.invoke(filter)
    }

    private fun renderFilterIntensity() {
        val canAdjustIntensity = style.showIntensity && selectedFilter.recipe != FilterRecipe()
        filterIntensityRow?.visibility = if (canAdjustIntensity) VISIBLE else GONE
        if (!canAdjustIntensity) {
            return
        }

        val intensity = selectedRecipe.intensity.coerceIn(0, FILTER_INTENSITY_MAX)
        isRenderingFilterIntensity = true
        filterIntensitySeekBar?.progress = intensity
        filterIntensityValue?.text = intensity.toString()
        isRenderingFilterIntensity = false
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
    }

    private fun itemSpacing(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing)

    private companion object {
        val CATALOG_EXECUTOR = Executors.newSingleThreadExecutor()
        const val FILTER_INTENSITY_MAX = 100
    }

    enum class ControlTab {
        Filter,
        Adjust,
    }

    private data class TabParts(
        val label: TextView?,
        val indicator: View?,
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
        val labelBackgroundRes: Int,
        val labelTextColor: Int,
        val closeIconRes: Int,
        val noneIconRes: Int,
        val iconPadding: Float?,
        val showTabIndicator: Boolean,
        val tabIndicatorColor: Int,
        val tabIndicatorHeight: Int,
        val showIntensity: Boolean,
        val intensityTextColor: Int,
        val intensityProgressColor: Int,
        val intensityTrackColor: Int,
        val catalogAssetPath: String?,
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
            labelBackgroundRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterLabelBackground,
                R.drawable.gs_bg_filter_label,
            ),
            labelTextColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterLabelTextColor,
                Color.WHITE,
            ),
            closeIconRes = array.getResourceId(
                R.styleable.FilterControlsView_gsFilterCloseIcon,
                R.drawable.ic_gs_close,
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
            showIntensity = array.getBoolean(R.styleable.FilterControlsView_gsFilterShowIntensity, true),
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
            catalogAssetPath = array.getString(R.styleable.FilterControlsView_gsFilterCatalogAsset),
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
                itemView.setBackgroundResource(
                    if (item.isSelected) style.selectedCardBackgroundRes else style.cardBackgroundRes,
                )
                itemView.setOnClickListener { onFilterSelected(item.filter) }
                label?.setBackgroundResource(style.labelBackgroundRes)
                label?.setTextColor(style.labelTextColor)
                label?.text = item.filter.displayName(itemView.context)

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

            fun clear() {
                image?.let { Glide.with(it).clear(it) }
            }
        }

        private companion object {
            val DIFF = object : DiffUtil.ItemCallback<FilterItem>() {
                override fun areItemsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean =
                    oldItem.filter.id == newItem.filter.id

                override fun areContentsTheSame(oldItem: FilterItem, newItem: FilterItem): Boolean =
                    oldItem == newItem
            }
        }
    }
}
