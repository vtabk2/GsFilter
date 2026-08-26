package com.gsfilter.filter

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.core.gscore.view.RippleImageView
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
    private var thumbnailBitmap: Bitmap? = null
    private var thumbnailKey: String? = null
    private var thumbnailGenerationId = 0

    var onCloseClick: (() -> Unit)? = null
    var onControlTabSelected: ((ControlTab) -> Unit)? = null
    var onCategorySelected: ((FilterCategory) -> Unit)? = null
    var onFilterSelected: ((FilterOption) -> Unit)? = null
    var onCatalogLoaded: ((FilterPack) -> Unit)? = null
    var onCatalogLoadFailed: ((Throwable) -> Unit)? = null

    private val categoryChips = mutableMapOf<String, TextView>()
    private val tabFilter = createTab(R.string.gs_section_filter, ControlTab.Filter, true)
    private val tabAdjust = createTab(R.string.gs_section_adjust, ControlTab.Adjust, false)
    private val buttonOriginalFilter = createOriginalButton()
    private val categoryContainer = LinearLayout(context).apply {
        isBaselineAligned = false
        orientation = HORIZONTAL
    }
    private val filterAdapter = FilterAdapter(::selectFilter)
    private val filterRecyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = filterAdapter
        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER
    }
    private val filterContent = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    init {
        orientation = VERTICAL
        addView(createHeader())
        addView(filterContent)
        bindFilterContent()
        setCatalog(catalog)
        setSelectedTab(ControlTab.Filter)
        style.catalogAssetPath?.let(::loadCatalogFromAssets)
    }

    fun setSelectedTab(tab: ControlTab) {
        val isFilterSelected = tab == ControlTab.Filter
        renderTab(tabFilter, isFilterSelected)
        renderTab(tabAdjust, !isFilterSelected)
        filterContent.visibility = if (isFilterSelected) VISIBLE else GONE
    }

    fun setCatalog(catalog: FilterPack) {
        this.catalog = catalog
        selectedCategory = catalog.categoryById(selectedCategory.id) ?: catalog.defaultCategory
        selectedFilter = catalog.filterById(selectedFilter.id) ?: catalog.defaultFilter
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
        val nextCategory = catalog.categoryById(selectedCategory.id) ?: catalog.defaultCategory
        val nextFilter = catalog.filterById(selectedFilter.id) ?: selectedFilter
        val nextThumbnailGenerationId = thumbnailBitmap?.generationId ?: 0
        if (
            this.selectedCategory == nextCategory &&
            this.selectedFilter == nextFilter &&
            this.thumbnailBitmap === thumbnailBitmap &&
            this.thumbnailKey == thumbnailKey &&
            this.thumbnailGenerationId == nextThumbnailGenerationId
        ) {
            return
        }

        this.selectedCategory = nextCategory
        this.selectedFilter = nextFilter
        this.thumbnailBitmap = thumbnailBitmap
        this.thumbnailKey = thumbnailKey
        this.thumbnailGenerationId = nextThumbnailGenerationId
        renderState()
    }

    override fun onDetachedFromWindow() {
        catalogLoadVersion++
        super.onDetachedFromWindow()
    }

    private fun renderState() {
        renderOriginalAction()
        renderCategories(selectedCategory)
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
                filterRecyclerView.scrollToPosition(selectedIndex)
            }
        }
    }

    private fun createHeader(): View =
        LinearLayout(context).apply {
            isBaselineAligned = false
            gravity = Gravity.CENTER_VERTICAL
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            addView(tabFilter, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(
                tabAdjust,
                LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = itemSpacing()
                    marginEnd = itemSpacing()
                },
            )
            addView(RippleImageView(context).apply {
                contentDescription = context.getString(R.string.gs_action_close)
                iconRippleRes = style.closeIconRes
                style.iconPadding?.let { paddingRipple = it }
                setOnClickListener { onCloseClick?.invoke() }
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
            })
        }

    private fun bindFilterContent() {
        buttonOriginalFilter.setOnClickListener { selectFilter(catalog.defaultFilter) }
        filterContent.addView(
            LinearLayout(context).apply {
                isBaselineAligned = false
                gravity = Gravity.CENTER_VERTICAL
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = itemSpacing()
                }
                addView(buttonOriginalFilter)
                addView(HorizontalScrollView(context).apply {
                    isHorizontalScrollBarEnabled = false
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        marginStart = itemSpacing()
                    }
                    addView(categoryContainer)
                })
            },
        )
        filterContent.addView(
            filterRecyclerView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = itemSpacing()
            },
        )
    }

    private fun renderCategoryChips() {
        categoryChips.clear()
        categoryContainer.removeAllViews()
        catalog.categories.forEachIndexed { index, category ->
            val chip = createCategoryChip(
                index = index,
                text = category.displayName(context).toString(),
                onClick = { selectCategory(category) },
            )
            categoryChips[category.id] = chip
            categoryContainer.addView(chip)
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
        renderState()
        onFilterSelected?.invoke(filter)
    }

    private fun createTab(textRes: Int, tab: ControlTab, isSelected: Boolean): LinearLayout {
        val label = TextView(context).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            minHeight = chipMinHeight()
            setPadding(chipHorizontalPadding(), chipVerticalPadding(), chipHorizontalPadding(), chipVerticalPadding())
            setText(textRes)
            textSize = 14f
        }
        val indicator = View(context).apply {
            background = style.tabIndicatorColor.toDrawable()
            visibility = if (style.showTabIndicator && isSelected) VISIBLE else INVISIBLE
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, style.tabIndicatorHeight)
        }
        return LinearLayout(context).apply {
            orientation = VERTICAL
            isClickable = true
            isFocusable = true
            setOnClickListener {
                setSelectedTab(tab)
                onControlTabSelected?.invoke(tab)
            }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                if (!isSelected) {
                    marginStart = itemSpacing()
                }
            }
            addView(label)
            addView(indicator)
            tag = TabParts(label, indicator)
            renderTab(this, isSelected)
        }
    }

    private fun renderTab(tab: LinearLayout, isSelected: Boolean) {
        tab.isSelected = isSelected
        if (style.useTabBackground) {
            tab.setBackgroundResource(if (isSelected) style.selectedTabBackgroundRes else style.tabBackgroundRes)
        } else {
            tab.background = null
        }
        val parts = tab.tag as TabParts
        parts.label.setTextColor(if (isSelected) style.selectedTabTextColor else style.tabTextColor)
        parts.indicator.visibility = if (style.showTabIndicator && isSelected) VISIBLE else INVISIBLE
    }

    private fun createOriginalButton(): RippleImageView =
        RippleImageView(context).apply {
            contentDescription = context.getString(R.string.gs_action_original)
            iconRippleRes = style.noneIconRes
            style.iconPadding?.let { paddingRipple = it }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
        }

    private fun createCategoryChip(index: Int, text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            minHeight = chipMinHeight()
            setBackgroundResource(style.chipBackgroundRes)
            setPadding(chipHorizontalPadding(), chipVerticalPadding(), chipHorizontalPadding(), chipVerticalPadding())
            setTextColor(style.textColor)
            textSize = 14f
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                if (index > 0) {
                    marginStart = itemSpacing()
                }
            }
        }

    private fun renderOriginalAction() {
        buttonOriginalFilter.isSelected = false
    }

    private fun renderCategories(selectedCategory: FilterCategory) {
        categoryChips.forEach { (id, chip) ->
            val isSelected = id == selectedCategory.id
            chip.setBackgroundResource(if (isSelected) style.selectedChipBackgroundRes else style.chipBackgroundRes)
            chip.setTextColor(if (isSelected) style.selectedTextColor else style.textColor)
        }
    }

    private fun itemSpacing(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing)

    private fun chipMinHeight(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_chip_min_height)

    private fun chipHorizontalPadding(): Int =
        resources.getDimensionPixelSize(R.dimen.gs_filter_chip_horizontal_padding)

    private fun chipVerticalPadding(): Int =
        resources.getDimensionPixelSize(R.dimen.gs_filter_chip_vertical_padding)

    private companion object {
        val CATALOG_EXECUTOR = Executors.newSingleThreadExecutor()
    }

    enum class ControlTab {
        Filter,
        Adjust,
    }

    private data class TabParts(
        val label: TextView,
        val indicator: View,
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
            FilterHolder(parent, onFilterSelected)

        override fun onBindViewHolder(holder: FilterHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun onViewRecycled(holder: FilterHolder) {
            holder.clear()
        }

        class FilterHolder(
            parent: ViewGroup,
            private val onFilterSelected: (FilterOption) -> Unit,
        ) : RecyclerView.ViewHolder(FrameLayout(parent.context)) {

            private val image = ImageView(parent.context)
            private val label = TextView(parent.context)
            private lateinit var style: FilterControlsStyle

            init {
                val context = parent.context
                val width = context.resources.getDimensionPixelSize(R.dimen.gs_filter_thumbnail_width)
                val height = context.resources.getDimensionPixelSize(R.dimen.gs_filter_thumbnail_height)
                val inset = context.resources.getDimensionPixelSize(R.dimen.gs_filter_thumbnail_inset)
                val labelHeight = context.resources.getDimensionPixelSize(R.dimen.gs_filter_thumbnail_label_height)
                val labelPadding = context.resources.getDimensionPixelSize(R.dimen.gs_filter_thumbnail_label_padding)
                val spacing = context.resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing)

                itemView.layoutParams = RecyclerView.LayoutParams(width, height).apply {
                    marginEnd = spacing
                }
                itemView.isClickable = true
                itemView.isFocusable = true

                val root = itemView as FrameLayout
                image.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                image.scaleType = ImageView.ScaleType.CENTER_CROP
                root.addView(
                    image,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ).apply {
                        setMargins(inset, inset, inset, inset)
                    },
                )
                root.addView(
                    label.apply {
                        ellipsize = TextUtils.TruncateAt.END
                        gravity = Gravity.CENTER
                        includeFontPadding = false
                        maxLines = 1
                        setPadding(labelPadding, 0, labelPadding, 0)
                        textSize = 12f
                    },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        labelHeight,
                        Gravity.BOTTOM,
                    ).apply {
                        setMargins(inset, 0, inset, inset)
                    },
                )
            }

            fun bind(item: FilterItem) {
                style = item.style
                itemView.setBackgroundResource(
                    if (item.isSelected) style.selectedCardBackgroundRes else style.cardBackgroundRes,
                )
                itemView.setOnClickListener { onFilterSelected(item.filter) }
                label.setBackgroundResource(style.labelBackgroundRes)
                label.setTextColor(style.labelTextColor)
                label.text = item.filter.displayName(itemView.context)

                val source = item.thumbnailBitmap
                val sourceKey = item.thumbnailKey
                if (source == null || sourceKey == null) {
                    Glide.with(image).clear(image)
                    image.setImageBitmap(source)
                    return
                }

                Glide.with(image)
                    .load(FilterThumbnailModel(sourceKey, source, item.filter))
                    .placeholder(source.toDrawable(itemView.resources))
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .dontAnimate()
                    .into(image)
            }

            fun clear() {
                Glide.with(image).clear(image)
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
