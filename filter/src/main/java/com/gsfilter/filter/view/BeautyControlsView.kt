package com.gsfilter.filter.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.core.gscore.view.RippleImageView
import com.gsfilter.filter.R
import com.gsfilter.filter.api.FilterRecipe
import com.gsfilter.filter.view.FilterControlsView.BeautyControl

internal class BeautyControlsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val style = BeautyControlsStyle(context, attrs)
    private val controlsContainer: LinearLayout?
    private val resetButton: RippleImageView?
    val seekRow: View
    val seekBar: SeekBar
    private val valueText: TextView?
    private val resetAll: TextView?
    private val labels = mutableMapOf<BeautyControl, TextView>()
    private val icons = mutableMapOf<BeautyControl, ImageView>()
    private val dots = mutableMapOf<BeautyControl, View>()
    private var selectedControl = BeautyControl.Smoothing
    private var selectedRecipe = FilterRecipe.DEFAULT
    private var defaultRecipe = FilterRecipe.DEFAULT
    private var isRendering = false

    var onBeautyChanged: ((BeautyControl, Int) -> Unit)? = null
    var onResetAllClick: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.gs_view_beauty_controls, this, true)
        controlsContainer = findViewById(R.id.gs_beauty_controls_container)
        seekRow = requireNotNull(findViewById(R.id.gs_beauty_seek_row))
        resetButton = findViewById(R.id.gs_beauty_reset)
        seekBar = requireNotNull(findViewById(R.id.gs_beauty_seek_bar))
        valueText = findViewById(R.id.gs_beauty_value)
        resetAll = findViewById(R.id.gs_beauty_reset_all)
        bindControls()
        bindSeekRow()
        bindResetAll()
        renderBeauty()
    }

    fun setState(selectedRecipe: FilterRecipe, defaultRecipe: FilterRecipe) {
        this.selectedRecipe = selectedRecipe
        this.defaultRecipe = defaultRecipe
        renderBeauty()
    }

    private fun bindControls() {
        controlsContainer?.let { container ->
            BeautyControl.entries.forEachIndexed { index, control ->
                container.addView(createControlItem(index, control, container))
            }
        }
    }

    private fun bindSeekRow() {
        resetButton?.iconRippleRes = R.drawable.selector_ic_gs_adjust_reset
        style.iconPadding?.let { resetButton?.paddingRipple = it }
        resetButton?.setOnClickListener {
            onBeautyChanged?.invoke(selectedControl, beautyValue(selectedControl, defaultRecipe))
        }
        seekBar.max = BEAUTY_MAX
        seekBar.progressBackgroundTintList = ColorStateList.valueOf(style.trackColor)
        seekBar.progressTintList = ColorStateList.valueOf(style.progressColor)
        seekBar.thumbTintList = ColorStateList.valueOf(style.progressColor)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !isRendering) {
                    onBeautyChanged?.invoke(selectedControl, progress)
                }
            }

            override fun onStartTrackingTouch(view: SeekBar) = Unit

            override fun onStopTrackingTouch(view: SeekBar) = Unit
        })
        seekBar.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> view.parent?.requestDisallowInterceptTouchEvent(true)

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
        valueText?.setTextColor(style.valueTextColor)
    }

    private fun bindResetAll() {
        resetAll?.text = context.getString(R.string.gs_action_reset_beauty)
        resetAll?.setTextColor(resetAllTextColors())
        resetAll?.setOnClickListener { onResetAllClick?.invoke() }
    }

    private fun createControlItem(index: Int, control: BeautyControl, parent: LinearLayout): View {
        val item = LayoutInflater.from(context).inflate(
            R.layout.gs_item_beauty_control,
            parent,
            false,
        )
        val label: TextView = item.findViewById(R.id.gs_beauty_item_label)
        val icon: ImageView = item.findViewById(R.id.gs_beauty_item_icon)
        val dot: View = item.findViewById(R.id.gs_beauty_changed_dot)
        labels[control] = label
        icons[control] = icon
        dots[control] = dot

        return item.apply {
            setOnClickListener {
                selectedControl = control
                renderBeauty()
            }
            if (index > 0) {
                (layoutParams as? LayoutParams)?.marginStart = itemSpacing()
            }
            dot.backgroundTintList = ColorStateList.valueOf(style.progressColor)
            icon.setImageResource(control.iconRes)
            label.setText(control.labelRes)
        }
    }

    private fun renderBeauty() {
        val activeValue = beautyValue(selectedControl, selectedRecipe).coerceIn(0, BEAUTY_MAX)
        val defaultValue = beautyValue(selectedControl, defaultRecipe).coerceIn(0, BEAUTY_MAX)
        isRendering = true
        seekBar.progress = activeValue
        valueText?.text = activeValue.toString()
        resetButton?.isEnabled = activeValue != defaultValue

        var hasChangedValue = false
        BeautyControl.entries.forEach { control ->
            val isSelected = control == selectedControl
            val hasChanged = beautyValue(control, selectedRecipe) != beautyValue(control, defaultRecipe)
            hasChangedValue = hasChangedValue || hasChanged
            val textColor = if (isSelected) style.progressColor else style.textColor
            dots[control]?.visibility = if (hasChanged) VISIBLE else INVISIBLE
            icons[control]?.setColorFilter(textColor)
            labels[control]?.setTextColor(textColor)
        }
        resetAll?.isEnabled = hasChangedValue
        isRendering = false
    }

    private fun resetAllTextColors(): ColorStateList =
        ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(),
            ),
            intArrayOf(style.textColor, style.progressColor),
        )

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

    private fun itemSpacing(): Int = resources.getDimensionPixelSize(R.dimen.gs_filter_item_spacing)

    private data class BeautyControlsStyle(
        val textColor: Int,
        val valueTextColor: Int,
        val progressColor: Int,
        val trackColor: Int,
        val iconPadding: Float?,
    ) {
        constructor(context: Context, attrs: AttributeSet?) : this(
            context = context,
            array = context.obtainStyledAttributes(attrs, R.styleable.FilterControlsView),
        )

        private constructor(context: Context, array: TypedArray) : this(
            textColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterBeautyTextColor,
                context.getColor(R.color.gs_adjust_text_secondary),
            ),
            valueTextColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterIntensityTextColor,
                array.getColor(
                    R.styleable.FilterControlsView_gsFilterTextColor,
                    context.getColor(R.color.gs_text_secondary),
                ),
            ),
            progressColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterIntensityProgressColor,
                array.getColor(
                    R.styleable.FilterControlsView_gsFilterSelectedColor,
                    context.getColor(R.color.gs_filter_selected),
                ),
            ),
            trackColor = array.getColor(
                R.styleable.FilterControlsView_gsFilterIntensityTrackColor,
                context.getColor(R.color.gs_adjust_track_background),
            ),
            iconPadding = if (array.hasValue(R.styleable.FilterControlsView_gsFilterIconPadding)) {
                array.getDimension(R.styleable.FilterControlsView_gsFilterIconPadding, 0f)
            } else {
                null
            },
        ) {
            array.recycle()
        }
    }

    private companion object {
        const val BEAUTY_MAX = 100
    }
}
