package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterEffect
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams

object FilterThumbnailRenderer {

    fun render(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int = THUMBNAIL_MAX_SIZE,
        maxHeight: Int = THUMBNAIL_MAX_SIZE,
    ): Bitmap {
        val thumbnailRecipe = thumbnailRecipe(recipe)
        return if (shouldUseFullSourceTexture(thumbnailRecipe)) {
            renderWithFullSourceTexture(source, thumbnailRecipe, adjustments, maxWidth, maxHeight)
        } else {
            renderScaledFirst(source, thumbnailRecipe, adjustments, maxWidth, maxHeight)
        }
    }

    private fun renderWithFullSourceTexture(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments,
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap =
        try {
            FilterGpuBitmapRenderer.getBitmap(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                scaleSource = false,
                texelScale = texelScaleFor(recipe),
            )
        } catch (_: RuntimeException) {
            renderScaledFirst(source, recipe, adjustments, maxWidth, maxHeight)
        }

    private fun renderScaledFirst(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments,
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap =
        try {
            FilterGpuBitmapRenderer.getBitmap(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                texelScale = texelScaleFor(recipe),
            )
        } catch (_: RuntimeException) {
            FilterBitmapRenderer.getBitmap(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
        }

    internal fun shouldUseFullSourceTexture(recipe: FilterRecipe): Boolean =
        recipe.effect != FilterEffect.Color

    internal fun maxSizeFor(recipe: FilterRecipe): Int =
        if (recipe.effect == FilterEffect.Color) THUMBNAIL_MAX_SIZE else ART_THUMBNAIL_MAX_SIZE

    internal fun texelScaleFor(recipe: FilterRecipe): Float =
        if (recipe.effect == FilterEffect.Color) DEFAULT_TEXEL_SCALE else ART_TEXEL_SCALE

    internal fun thumbnailRecipe(recipe: FilterRecipe): FilterRecipe =
        when (recipe.effect) {
            FilterEffect.Color -> recipe
            FilterEffect.Sketch -> recipe.copy(
                effectStrength = recipe.effectStrength.scale(80),
                effectThreshold = recipe.effectThreshold.shift(16),
            )
            FilterEffect.Ink -> recipe.copy(
                effectStrength = recipe.effectStrength.scale(75),
                effectThreshold = recipe.effectThreshold.shift(26),
            )
            FilterEffect.Pencil -> recipe.copy(
                effectStrength = recipe.effectStrength.scale(82),
                effectThreshold = recipe.effectThreshold.shift(14),
                adjustments = recipe.adjustments.copy(grain = recipe.adjustments.grain.scale(50)),
            )
            FilterEffect.ColorPencil -> recipe.copy(
                effectStrength = recipe.effectStrength.scale(90),
                effectThreshold = recipe.effectThreshold.shift(10),
            )
            FilterEffect.Charcoal -> recipe.copy(
                effectStrength = recipe.effectStrength.scale(75),
                effectThreshold = recipe.effectThreshold.shift(18),
                adjustments = recipe.adjustments.copy(grain = recipe.adjustments.grain.scale(50)),
            )
            FilterEffect.CrossHatch -> recipe.copy(
                effectStrength = recipe.effectStrength.scale(80),
                effectThreshold = recipe.effectThreshold.shift(18),
            )
        }

    internal fun filterPixel(
        pixels: IntArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
    ): Int = FilterBitmapRenderer.filterPixel(pixels, x, y, width, height, params)

    internal const val THUMBNAIL_MAX_SIZE = 128
    internal const val ART_THUMBNAIL_MAX_SIZE = 256

    private fun Int.scale(percent: Int): Int = (this * percent / PERCENT_MAX).coerceIn(EFFECT_MIN, EFFECT_MAX)

    private fun Int.shift(delta: Int): Int = (this + delta).coerceIn(EFFECT_MIN, EFFECT_MAX)

    private const val PERCENT_MAX = 100
    private const val EFFECT_MIN = 0
    private const val EFFECT_MAX = 100
    private const val DEFAULT_TEXEL_SCALE = 1f
    private const val ART_TEXEL_SCALE = 0.5f
}
