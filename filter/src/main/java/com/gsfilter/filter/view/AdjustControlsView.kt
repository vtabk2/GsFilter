package com.gsfilter.filter.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.core.gscore.view.RippleImageView
import com.gsfilter.filter.AdjustControl
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.R

internal class AdjustControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val style = AdjustControlsStyle(context, attrs)
    private val resetButton: RippleImageView?
    private val seekBar: SeekBar?
    private val valueText: TextView?
    private val controlsContainer: LinearLayout?
    private val resetAllButton: Button?
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
        LayoutInflater.from(context).inflate(R.layout.gs_view_adjust_controls, this, true)
        resetButton = findViewById(R.id.gs_adjust_reset_button)
        seekBar = findViewById(R.id.gs_adjust_seek_bar)
        valueText = findViewById(R.id.gs_adjust_value)
        controlsContainer = findViewById(R.id.gs_adjust_controls_container)
        resetAllButton = findViewById(R.id.gs_adjust_reset_all_button)
        bindSeekRow()
        bindControls()
        bindResetAllButton()
        renderAdjustments()
    }

    fun setAdjustments(adjustments: Adjustments) {
        if (this.adjustments == adjustments) {
            return
        }
        this.adjustments = adjustments
        renderAdjustments()
    }

    private fun bindSeekRow() {
        resetButton?.iconRippleRes = style.resetIconRes
        style.resetIconPadding?.let { resetButton?.paddingRipple = it }
        resetButton?.setOnClickListener {
            onAdjustmentChanged?.invoke(selectedControl, selectedControl.valueIn(Adjustments()))
        }
        seekBar?.progressBackgroundTintList = ColorStateList.valueOf(style.trackColor)
        seekBar?.progressTintList = ColorStateList.valueOf(style.selectedColor)
        seekBar?.thumbTintList = ColorStateList.valueOf(style.textColor)
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !isRendering) {
                    onAdjustmentChanged?.invoke(selectedControl, selectedControl.valueFrom(progress))
                }
            }

            override fun onStartTrackingTouch(view: SeekBar) = Unit

            override fun onStopTrackingTouch(view: SeekBar) = Unit
        })
        valueText?.setTextColor(style.textColor)
    }

    private fun bindControls() {
        val container = controlsContainer ?: return
        AdjustControl.entries.forEachIndexed { index, control ->
            container.addView(createAdjustControlItem(index, control, container))
        }
    }

    private fun bindResetAllButton() {
        resetAllButton?.text = style.resetAllText
        resetAllButton?.setOnClickListener { onResetAllClick?.invoke() }
    }

    private fun createAdjustControlItem(index: Int, control: AdjustControl, parent: LinearLayout): View {
        val item = LayoutInflater.from(context).inflate(
            R.layout.gs_item_adjust_control,
            parent,
            false,
        )
        val label: TextView? = item.findViewById(R.id.gs_adjust_item_label)
        val icon: ImageView? = item.findViewById(R.id.gs_adjust_item_icon)
        val dot: View? = item.findViewById(R.id.gs_adjust_changed_dot)

        label?.let { labels[control] = it }
        icon?.let { icons[control] = it }
        dot?.let { dots[control] = it }

        return item.apply {
            setOnClickListener {
                selectedControl = control
                renderAdjustments()
            }
            if (index > 0) {
                (layoutParams as? LayoutParams)?.marginStart = itemSpacing()
            }
            dot?.backgroundTintList = ColorStateList.valueOf(style.selectedColor)
            icon?.setImageResource(control.iconRes)
            label?.setText(control.labelRes)
        }
    }

    private fun renderAdjustments() {
        isRendering = true
        val activeValue = selectedControl.valueIn(adjustments)
        seekBar?.max = selectedControl.progressMax
        seekBar?.progress = selectedControl.progressFrom(activeValue)
        valueText?.text = activeValue.toString()

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
