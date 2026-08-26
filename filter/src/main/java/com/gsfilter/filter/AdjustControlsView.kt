package com.gsfilter.filter

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.core.gscore.view.RippleImageView

internal class AdjustControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val style = AdjustControlsStyle(context, attrs)
    private val resetButton = RippleImageView(context)
    private val seekBar = SeekBar(context)
    private val valueText = TextView(context)
    private val controlsContainer = LinearLayout(context)
    private val resetAllButton = Button(context)
    private val labels = mutableMapOf<AdjustControl, TextView>()
    private val icons = mutableMapOf<AdjustControl, ImageView>()
    private val dots = mutableMapOf<AdjustControl, View>()
    private var selectedControl = AdjustControl.Brightness
    private var adjustments = Adjustments()
    private var isRendering = false

    var onAdjustmentChanged: ((AdjustControl, Int) -> Unit)? = null
    var onResetAllClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        val padding = resources.getDimensionPixelSize(R.dimen.gs_adjust_panel_padding)
        setPadding(padding, padding, padding, padding)
        addView(createSeekRow())
        addView(createControlsScroll())
        addView(createResetAllButton())
        renderAdjustments()
    }

    fun setAdjustments(adjustments: Adjustments) {
        if (this.adjustments == adjustments) {
            return
        }
        this.adjustments = adjustments
        renderAdjustments()
    }

    private fun createSeekRow(): View =
        LinearLayout(context).apply {
            isBaselineAligned = false
            gravity = Gravity.CENTER_VERTICAL
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            addView(resetButton.apply {
                contentDescription = context.getString(R.string.gs_action_reset_adjust)
                iconRippleRes = style.resetIconRes
                style.resetIconPadding?.let { paddingRipple = it }
                setOnClickListener {
                    onAdjustmentChanged?.invoke(selectedControl, selectedControl.valueIn(Adjustments()))
                }
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            })
            addView(seekBar.apply {
                progressBackgroundTintList = ColorStateList.valueOf(style.trackColor)
                progressTintList = ColorStateList.valueOf(style.selectedColor)
                thumbTintList = ColorStateList.valueOf(style.textColor)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser && !isRendering) {
                            onAdjustmentChanged?.invoke(selectedControl, selectedControl.valueFrom(progress))
                        }
                    }

                    override fun onStartTrackingTouch(view: SeekBar) = Unit

                    override fun onStopTrackingTouch(view: SeekBar) = Unit
                })
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = itemSpacing()
                    marginEnd = itemSpacing()
                }
            })
            addView(valueText.apply {
                gravity = Gravity.END
                setTextColor(style.textColor)
                textSize = 12f
                layoutParams = LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.gs_adjust_value_width),
                    LayoutParams.WRAP_CONTENT,
                )
            })
        }

    private fun createControlsScroll(): View =
        HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = itemSpacing()
            }
            addView(controlsContainer.apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                AdjustControl.entries.forEachIndexed { index, control ->
                    addView(createAdjustControlItem(index, control))
                }
            })
        }

    private fun createResetAllButton(): View =
        resetAllButton.apply {
            minHeight = resources.getDimensionPixelSize(R.dimen.gs_adjust_button_min_height)
            text = style.resetAllText
            setOnClickListener { onResetAllClick?.invoke() }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = itemSpacing()
            }
        }

    private fun createAdjustControlItem(index: Int, control: AdjustControl): View {
        val itemGap = resources.getDimensionPixelSize(R.dimen.gs_adjust_item_gap)
        val label = TextView(context)
        val icon = ImageView(context)
        val dot = View(context)

        labels[control] = label
        icons[control] = icon
        dots[control] = dot

        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            minimumHeight = resources.getDimensionPixelSize(R.dimen.gs_adjust_item_min_height)
            orientation = VERTICAL
            setOnClickListener {
                selectedControl = control
                renderAdjustments()
            }
            layoutParams = LayoutParams(
                resources.getDimensionPixelSize(R.dimen.gs_adjust_item_width),
                LayoutParams.WRAP_CONTENT,
            ).apply {
                if (index > 0) {
                    marginStart = itemSpacing()
                }
            }

            addView(dot.apply {
                setBackgroundResource(R.drawable.gs_bg_adjust_changed_dot)
                visibility = INVISIBLE
                layoutParams = LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.gs_adjust_dot_size),
                    resources.getDimensionPixelSize(R.dimen.gs_adjust_dot_size),
                )
            })
            addView(icon.apply {
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                setImageResource(control.iconRes)
                layoutParams = LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.gs_adjust_icon_size),
                    resources.getDimensionPixelSize(R.dimen.gs_adjust_icon_size),
                ).apply {
                    topMargin = itemGap
                }
            })
            addView(label.apply {
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                setText(control.labelRes)
                textSize = 10f
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = itemGap
                }
            })
        }
    }

    private fun renderAdjustments() {
        isRendering = true
        val activeValue = selectedControl.valueIn(adjustments)
        seekBar.max = selectedControl.progressMax
        seekBar.progress = selectedControl.progressFrom(activeValue)
        valueText.text = activeValue.toString()

        val defaults = Adjustments()
        AdjustControl.entries.forEach { control ->
            val isSelected = control == selectedControl
            val textColor = if (isSelected) style.selectedColor else style.secondaryTextColor
            dots[control]?.visibility =
                if (control.valueIn(adjustments) != control.valueIn(defaults)) VISIBLE else INVISIBLE
            icons[control]?.setColorFilter(textColor)
            labels[control]?.setTextColor(textColor)
        }
        isRendering = false
    }

    private fun itemSpacing(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing)

    private data class AdjustControlsStyle(
        val textColor: Int,
        val secondaryTextColor: Int,
        val selectedColor: Int,
        val trackColor: Int,
        val resetIconRes: Int,
        val resetIconPadding: Float?,
        val resetAllText: String,
    ) {
        constructor(context: Context, attrs: AttributeSet?) : this(
            context = context,
            array = context.obtainStyledAttributes(attrs, R.styleable.AdjustControlsView),
        )

        private constructor(context: Context, array: TypedArray) : this(
            textColor = array.getColor(
                R.styleable.AdjustControlsView_gsAdjustTextColor,
                context.getColor(R.color.gs_adjust_text_primary),
            ),
            secondaryTextColor = array.getColor(
                R.styleable.AdjustControlsView_gsAdjustSecondaryTextColor,
                context.getColor(R.color.gs_adjust_text_secondary),
            ),
            selectedColor = array.getColor(
                R.styleable.AdjustControlsView_gsAdjustSelectedColor,
                context.getColor(R.color.gs_filter_selected),
            ),
            trackColor = array.getColor(
                R.styleable.AdjustControlsView_gsAdjustTrackColor,
                context.getColor(R.color.gs_adjust_track_background),
            ),
            resetIconRes = array.getResourceId(
                R.styleable.AdjustControlsView_gsAdjustResetIcon,
                R.drawable.ic_gs_adjust_reset,
            ),
            resetIconPadding = if (array.hasValue(R.styleable.AdjustControlsView_gsAdjustResetIconPadding)) {
                array.getDimension(R.styleable.AdjustControlsView_gsAdjustResetIconPadding, 0f)
            } else {
                null
            },
            resetAllText = array.getString(R.styleable.AdjustControlsView_gsAdjustResetAllText)
                ?: context.getString(R.string.gs_action_reset_all_adjust),
        ) {
            array.recycle()
        }
    }
}
