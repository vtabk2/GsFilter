package com.gsfilter.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gsfilter.filter.glide.FilterThumbnailModel

class FilterControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    var onCloseClick: (() -> Unit)? = null
    var onControlTabSelected: ((ControlTab) -> Unit)? = null
    var onCategorySelected: ((FilterCategory) -> Unit)? = null
    var onFilterSelected: ((FilterOption) -> Unit)? = null

    private val categoryChips = mutableMapOf<String, TextView>()
    private val tabFilter = createTab(R.string.gs_section_filter, true)
    private val tabAdjust = createTab(R.string.gs_section_adjust, false)
    private val buttonOriginalFilter = createOriginalButton()
    private val categoryContainer = LinearLayout(context).apply {
        isBaselineAligned = false
        orientation = HORIZONTAL
    }
    private val filterAdapter = FilterAdapter { onFilterSelected?.invoke(it) }
    private val filterRecyclerView = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = filterAdapter
        overScrollMode = OVER_SCROLL_NEVER
    }
    private val filterContent = LinearLayout(context).apply {
        orientation = VERTICAL
    }

    init {
        orientation = VERTICAL
        addView(createHeader())
        addView(filterContent)
        bindCategories()
        setSelectedTab(ControlTab.Filter)
    }

    fun setSelectedTab(tab: ControlTab) {
        val isFilterSelected = tab == ControlTab.Filter
        renderTab(tabFilter, isFilterSelected)
        renderTab(tabAdjust, !isFilterSelected)
        filterContent.visibility = if (isFilterSelected) VISIBLE else GONE
    }

    fun setState(
        selectedCategory: FilterCategory,
        selectedFilter: FilterOption,
        thumbnailBitmap: Bitmap?,
        thumbnailKey: String?,
    ) {
        renderOriginalAction(selectedFilter)
        renderCategories(selectedCategory)
        val items = FilterCatalog.filtersForCategory(selectedCategory.id).map { filter ->
            FilterItem(
                filter = filter,
                isSelected = filter.id == selectedFilter.id,
                thumbnailBitmap = thumbnailBitmap,
                thumbnailKey = thumbnailKey,
                thumbnailGenerationId = thumbnailBitmap?.generationId ?: 0,
            )
        }
        filterAdapter.submitList(items) {
            val selectedIndex = items.indexOfFirst { it.filter.id == selectedFilter.id }
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

            addView(tabFilter)
            addView(tabAdjust)
            addView(View(context), LayoutParams(0, 1, 1f))
            addView(ImageButton(context).apply {
                background = null
                contentDescription = context.getString(R.string.gs_action_close)
                setImageResource(R.drawable.ic_gs_close)
                setOnClickListener { onCloseClick?.invoke() }
                layoutParams = LayoutParams(chipMinHeight(), chipMinHeight())
            })
        }

    private fun bindCategories() {
        buttonOriginalFilter.setOnClickListener { onFilterSelected?.invoke(FilterCatalog.default) }
        FilterCatalog.categories.forEachIndexed { index, category ->
            val chip = createCategoryChip(
                index = index,
                text = context.getString(category.nameRes),
                onClick = { onCategorySelected?.invoke(category) },
            )
            categoryChips[category.id] = chip
            categoryContainer.addView(chip)
        }

        filterContent.addView(
            LinearLayout(context).apply {
                isBaselineAligned = false
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = itemSpacing()
                }
                addView(buttonOriginalFilter)
                addView(HorizontalScrollView(context).apply {
                    isHorizontalScrollBarEnabled = false
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
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

    private fun createTab(textRes: Int, isSelected: Boolean): TextView =
        TextView(context).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            minHeight = chipMinHeight()
            setPadding(chipHorizontalPadding(), chipVerticalPadding(), chipHorizontalPadding(), chipVerticalPadding())
            setText(textRes)
            textSize = 14f
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val tab = if (textRes == R.string.gs_section_filter) ControlTab.Filter else ControlTab.Adjust
                setSelectedTab(tab)
                onControlTabSelected?.invoke(tab)
            }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                if (!isSelected) {
                    marginStart = itemSpacing()
                }
            }
            renderTab(this, isSelected)
        }

    private fun renderTab(tab: TextView, isSelected: Boolean) {
        tab.isSelected = isSelected
        tab.setBackgroundResource(if (isSelected) R.drawable.gs_bg_filter_chip_selected else R.drawable.gs_bg_filter_chip)
        tab.setTextColor(context.getColor(if (isSelected) android.R.color.white else R.color.gs_text_secondary))
    }

    private fun createOriginalButton(): ImageButton =
        ImageButton(context).apply {
            contentDescription = context.getString(R.string.gs_action_original)
            setBackgroundResource(R.drawable.gs_bg_filter_chip)
            setImageResource(R.drawable.ic_gs_filter_none)
            setPadding(iconPadding(), iconPadding(), iconPadding(), iconPadding())
            scaleType = ImageView.ScaleType.CENTER
            layoutParams = LayoutParams(chipMinHeight(), chipMinHeight())
        }

    private fun createCategoryChip(index: Int, text: String, onClick: () -> Unit): TextView =
        TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            includeFontPadding = false
            maxLines = 1
            minHeight = chipMinHeight()
            setBackgroundResource(R.drawable.gs_bg_filter_chip)
            setPadding(chipHorizontalPadding(), chipVerticalPadding(), chipHorizontalPadding(), chipVerticalPadding())
            setTextColor(context.getColor(R.color.gs_text_secondary))
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

    private fun renderOriginalAction(selectedFilter: FilterOption) {
        val isSelected = selectedFilter.id == FilterCatalog.default.id
        buttonOriginalFilter.setBackgroundResource(
            if (isSelected) R.drawable.gs_bg_filter_chip_selected else R.drawable.gs_bg_filter_chip,
        )
        buttonOriginalFilter.setColorFilter(
            context.getColor(if (isSelected) android.R.color.white else R.color.gs_text_secondary),
        )
    }

    private fun renderCategories(selectedCategory: FilterCategory) {
        categoryChips.forEach { (id, chip) ->
            val isSelected = id == selectedCategory.id
            chip.setBackgroundResource(if (isSelected) R.drawable.gs_bg_filter_chip_selected else R.drawable.gs_bg_filter_chip)
            chip.setTextColor(context.getColor(if (isSelected) android.R.color.white else R.color.gs_text_secondary))
        }
    }

    private fun itemSpacing(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing)

    private fun chipMinHeight(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_chip_min_height)

    private fun chipHorizontalPadding(): Int =
        resources.getDimensionPixelSize(R.dimen.gs_filter_chip_horizontal_padding)

    private fun chipVerticalPadding(): Int =
        resources.getDimensionPixelSize(R.dimen.gs_filter_chip_vertical_padding)

    private fun iconPadding(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_none_icon_padding)

    enum class ControlTab {
        Filter,
        Adjust,
    }

    private data class FilterItem(
        val filter: FilterOption,
        val isSelected: Boolean,
        val thumbnailBitmap: Bitmap?,
        val thumbnailKey: String?,
        val thumbnailGenerationId: Int,
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
                        setBackgroundResource(R.drawable.gs_bg_filter_label)
                        setPadding(labelPadding, 0, labelPadding, 0)
                        setTextColor(Color.WHITE)
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
                itemView.setBackgroundResource(
                    if (item.isSelected) R.drawable.gs_bg_filter_card_selected else R.drawable.gs_bg_filter_card,
                )
                itemView.setOnClickListener { onFilterSelected(item.filter) }
                label.setText(item.filter.nameRes)

                val source = item.thumbnailBitmap
                val sourceKey = item.thumbnailKey
                if (source == null || sourceKey == null) {
                    Glide.with(image).clear(image)
                    image.setImageBitmap(source)
                    return
                }

                Glide.with(image)
                    .load(FilterThumbnailModel(sourceKey, source, item.filter))
                    .placeholder(BitmapDrawable(itemView.resources, source))
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
