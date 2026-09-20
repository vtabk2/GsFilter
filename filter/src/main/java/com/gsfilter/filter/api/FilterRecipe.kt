package com.gsfilter.filter.api

import com.gsfilter.filter.effects.FilterEffect
import com.gsfilter.filter.effects.FilterLut

data class FilterRecipe(
    val effect: FilterEffect = FilterEffect.Color,
    val effectStrength: Int = 100,
    val effectThreshold: Int = 50,
    val effectTone: Int = 20,
    val lut: FilterLut = FilterLut.None,
    val lutStrength: Int = 100,
    val intensity: Int = 100,
    val skinSmoothing: Int = 0,
    val skinWhitening: Int = 0,
    val blush: Int = 0,
    val lipstick: Int = 0,
    val underEye: Int = 0,
    val teethWhitening: Int = 0,
    val eyeShadow: Int = 0,
    val eyeliner: Int = 0,
    val eyebrow: Int = 0,
    val faceSlimming: Int = 0,
    val eyeEnlargement: Int = 0,
    val isMonochrome: Boolean = false,
    val redShift: Int = 0,
    val greenShift: Int = 0,
    val blueShift: Int = 0,
    val adjustments: Adjustments = Adjustments.DEFAULT,
) {
    companion object {
        val DEFAULT = FilterRecipe()
    }
}
