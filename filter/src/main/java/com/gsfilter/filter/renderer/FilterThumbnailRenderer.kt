package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterEffect
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams
import java.util.concurrent.CancellationException

object FilterThumbnailRenderer {

    fun render(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int = THUMBNAIL_MAX_SIZE,
        maxHeight: Int = THUMBNAIL_MAX_SIZE,
        isCancelled: () -> Boolean = { false },
    ): Bitmap {
        throwIfCancelled(isCancelled)
        val thumbnailRecipe = thumbnailRecipe(recipe)
        return if (shouldUseFullSourceTexture(thumbnailRecipe)) {
            renderWithFullSourceTexture(
                source,
                thumbnailRecipe,
                adjustments,
                maxWidth,
                maxHeight,
                isCancelled,
            )
        } else {
            renderScaledFirst(source, thumbnailRecipe, adjustments, maxWidth, maxHeight, isCancelled)
        }
    }

    private fun renderWithFullSourceTexture(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments,
        maxWidth: Int,
        maxHeight: Int,
        isCancelled: () -> Boolean,
    ): Bitmap {
        val renderSource = FilterBitmapRenderer.scaledSource(
            source = source,
            maxWidth = maxWidth * ART_SOURCE_SCALE,
            maxHeight = maxHeight * ART_SOURCE_SCALE,
        )
        return try {
            FilterGpuBitmapRenderer.getBitmap(
                source = renderSource,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                scaleSource = false,
                texelScale = texelScaleFor(recipe),
                isCancelled = isCancelled,
            )
        } catch (error: RuntimeException) {
            if (isCancelled()) {
                throw error
            }
            renderScaledFirst(source, recipe, adjustments, maxWidth, maxHeight, isCancelled)
        } finally {
            FilterBitmapRenderer.recycleIfTemporary(renderSource, source)
        }
    }

    private fun renderScaledFirst(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments,
        maxWidth: Int,
        maxHeight: Int,
        isCancelled: () -> Boolean,
    ): Bitmap =
        try {
            throwIfCancelled(isCancelled)
            FilterGpuBitmapRenderer.getBitmap(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                texelScale = texelScaleFor(recipe),
                isCancelled = isCancelled,
            )
        } catch (error: RuntimeException) {
            if (isCancelled()) {
                throw error
            }
            throwIfCancelled(isCancelled)
            FilterBitmapRenderer.getBitmap(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
        }

    private fun throwIfCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) {
            throw CancellationException("Thumbnail render cancelled")
        }
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

    internal const val THUMBNAIL_MAX_SIZE = 256
    internal const val ART_THUMBNAIL_MAX_SIZE = 256

    private fun Int.scale(percent: Int): Int = (this * percent / PERCENT_MAX).coerceIn(EFFECT_MIN, EFFECT_MAX)

    private fun Int.shift(delta: Int): Int = (this + delta).coerceIn(EFFECT_MIN, EFFECT_MAX)

    private const val PERCENT_MAX = 100
    private const val EFFECT_MIN = 0
    private const val EFFECT_MAX = 100
    private const val ART_SOURCE_SCALE = 2
    private const val DEFAULT_TEXEL_SCALE = 1f
    private const val ART_TEXEL_SCALE = 0.5f
}
