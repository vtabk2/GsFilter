package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterEffect
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException

object FilterThumbnailRenderer {

    // ponytail: one-slot cache bounds memory; expand only with profiling.
    private val artSourceCacheLock = Any()
    private var cachedArtSource: Bitmap? = null
    private var cachedArtSourceOriginal: WeakReference<Bitmap>? = null
    private var cachedArtSourceKey: String? = null
    private var cachedArtSourceWidth = 0
    private var cachedArtSourceHeight = 0
    private var cachedArtSourceGenerationId = 0
    private var cachedArtSourceMaxWidth = 0
    private var cachedArtSourceMaxHeight = 0
    private val thumbnailSourceCacheLock = Any()
    private var cachedThumbnailSource: Bitmap? = null
    private var cachedThumbnailSourceOriginal: WeakReference<Bitmap>? = null
    private var cachedThumbnailSourceKey: String? = null
    private var cachedThumbnailSourceWidth = 0
    private var cachedThumbnailSourceHeight = 0
    private var cachedThumbnailSourceGenerationId = 0
    private var cachedThumbnailSourceMaxWidth = 0
    private var cachedThumbnailSourceMaxHeight = 0

    fun render(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments.DEFAULT,
        maxWidth: Int = THUMBNAIL_MAX_SIZE,
        maxHeight: Int = THUMBNAIL_MAX_SIZE,
        isCancelled: () -> Boolean = { false },
        sourceKey: String? = null,
    ): Bitmap {
        throwIfCancelled(isCancelled)
        val thumbnailRecipe = thumbnailRecipe(recipe)
        val hasDefaultAdjustments = adjustments === Adjustments.DEFAULT || adjustments == Adjustments.DEFAULT
        val isDefaultRecipe = thumbnailRecipe === FilterRecipe.DEFAULT || thumbnailRecipe == FilterRecipe.DEFAULT
        if (hasDefaultAdjustments && (isDefaultRecipe || thumbnailRecipe.intensity == 0)) {
            return copyThumbnail(source, maxWidth, maxHeight)
        }
        return if (shouldUseFullSourceTexture(thumbnailRecipe)) {
            renderWithFullSourceTexture(
                source,
                thumbnailRecipe,
                adjustments,
                maxWidth,
                maxHeight,
                sourceKey,
                isCancelled,
            )
        } else {
            renderScaledFirst(
                source,
                thumbnailRecipe,
                adjustments,
                maxWidth,
                maxHeight,
                sourceKey,
                isCancelled,
            )
        }
    }

    private fun renderWithFullSourceTexture(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments,
        maxWidth: Int,
        maxHeight: Int,
        sourceKey: String?,
        isCancelled: () -> Boolean,
    ): Bitmap = synchronized(artSourceCacheLock) {
        val renderSource = scaledArtSource(source, maxWidth, maxHeight, sourceKey)
        try {
            throwIfCancelled(isCancelled)
            FilterGpuBitmapRenderer.getBitmap(
                source = renderSource,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                scaleSource = false,
                texelScale = texelScaleFor(recipe, renderSource.width, renderSource.height, maxWidth, maxHeight),
                isCancelled = isCancelled,
            )
        } catch (error: RuntimeException) {
            if (isCancelled()) {
                throw error
            }
            renderScaledFirst(source, recipe, adjustments, maxWidth, maxHeight, sourceKey, isCancelled)
        }
    }

    private fun scaledArtSource(
        source: Bitmap,
        maxWidth: Int,
        maxHeight: Int,
        sourceKey: String?,
    ): Bitmap {
        val generationId = source.generationId
        val cachedSource = cachedArtSource
        val sourceMatches = if (sourceKey != null) {
            cachedArtSourceKey == sourceKey &&
                cachedArtSourceWidth == source.width &&
                cachedArtSourceHeight == source.height &&
                (cachedArtSourceOriginal?.get() !== source || cachedArtSourceGenerationId == generationId)
        } else {
            cachedArtSourceOriginal?.get() === source && cachedArtSourceGenerationId == generationId
        }
        if (
            cachedSource != null &&
            sourceMatches &&
            cachedArtSourceMaxWidth == maxWidth &&
            cachedArtSourceMaxHeight == maxHeight
        ) {
            return cachedSource
        }

        cachedArtSource?.recycle()
        cachedArtSource = null
        cachedArtSourceOriginal = null
        cachedArtSourceKey = null
        cachedArtSourceWidth = 0
        cachedArtSourceHeight = 0
        val scaled = FilterBitmapRenderer.scaledSource(
            source = source,
            maxWidth = maxWidth * ART_SOURCE_SCALE,
            maxHeight = maxHeight * ART_SOURCE_SCALE,
        )
        if (scaled === source) {
            return source
        }
        cachedArtSource = scaled
        cachedArtSourceOriginal = WeakReference(source)
        cachedArtSourceKey = sourceKey
        cachedArtSourceWidth = source.width
        cachedArtSourceHeight = source.height
        cachedArtSourceGenerationId = generationId
        cachedArtSourceMaxWidth = maxWidth
        cachedArtSourceMaxHeight = maxHeight
        return scaled
    }

    private fun renderScaledFirst(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments,
        maxWidth: Int,
        maxHeight: Int,
        sourceKey: String?,
        isCancelled: () -> Boolean,
    ): Bitmap = synchronized(thumbnailSourceCacheLock) {
        val renderSource = scaledThumbnailSource(source, maxWidth, maxHeight, sourceKey)
        try {
            throwIfCancelled(isCancelled)
            FilterGpuBitmapRenderer.getBitmap(
                source = renderSource,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                scaleSource = false,
                texelScale = texelScaleFor(recipe, renderSource.width, renderSource.height, maxWidth, maxHeight),
                isCancelled = isCancelled,
            )
        } catch (error: RuntimeException) {
            if (isCancelled()) {
                throw error
            }
            throwIfCancelled(isCancelled)
            FilterBitmapRenderer.getBitmap(
                source = renderSource,
                recipe = recipe,
                adjustments = adjustments,
            )
        }
    }

    private fun scaledThumbnailSource(
        source: Bitmap,
        maxWidth: Int,
        maxHeight: Int,
        sourceKey: String?,
    ): Bitmap {
        val generationId = source.generationId
        val cachedSource = cachedThumbnailSource
        val sourceMatches = if (sourceKey != null) {
            cachedThumbnailSourceKey == sourceKey &&
                cachedThumbnailSourceWidth == source.width &&
                cachedThumbnailSourceHeight == source.height &&
                (cachedThumbnailSourceOriginal?.get() !== source || cachedThumbnailSourceGenerationId == generationId)
        } else {
            cachedThumbnailSourceOriginal?.get() === source && cachedThumbnailSourceGenerationId == generationId
        }
        if (
            cachedSource != null &&
            sourceMatches &&
            cachedThumbnailSourceMaxWidth == maxWidth &&
            cachedThumbnailSourceMaxHeight == maxHeight
        ) {
            return cachedSource
        }

        cachedThumbnailSource?.recycle()
        cachedThumbnailSource = null
        cachedThumbnailSourceOriginal = null
        cachedThumbnailSourceKey = null
        cachedThumbnailSourceWidth = 0
        cachedThumbnailSourceHeight = 0
        val scaled = FilterBitmapRenderer.scaledSource(
            source = source,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        )
        if (scaled === source) {
            return source
        }
        cachedThumbnailSource = scaled
        cachedThumbnailSourceOriginal = WeakReference(source)
        cachedThumbnailSourceKey = sourceKey
        cachedThumbnailSourceWidth = source.width
        cachedThumbnailSourceHeight = source.height
        cachedThumbnailSourceGenerationId = generationId
        cachedThumbnailSourceMaxWidth = maxWidth
        cachedThumbnailSourceMaxHeight = maxHeight
        return scaled
    }

    private fun throwIfCancelled(isCancelled: () -> Boolean) {
        if (isCancelled()) {
            throw CancellationException("Thumbnail render cancelled")
        }
    }

    private fun copyThumbnail(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val scaled = FilterBitmapRenderer.scaledSource(source, maxWidth, maxHeight)
        return if (scaled === source) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            scaled
        }
    }

    internal fun shouldUseFullSourceTexture(recipe: FilterRecipe): Boolean =
        recipe.effect != FilterEffect.Color

    internal fun maxSizeFor(recipe: FilterRecipe): Int =
        if (recipe.effect == FilterEffect.Color) THUMBNAIL_MAX_SIZE else ART_THUMBNAIL_MAX_SIZE

    internal fun texelScaleFor(
        recipe: FilterRecipe,
        sourceWidth: Int,
        sourceHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
    ): Float {
        if (recipe.effect == FilterEffect.Color) {
            return DEFAULT_TEXEL_SCALE
        }
        val output = FilterBitmapRenderer.targetSize(sourceWidth, sourceHeight, maxWidth, maxHeight)
        return minOf(
            output.width.toFloat() / sourceWidth,
            output.height.toFloat() / sourceHeight,
        )
    }

    internal fun thumbnailRecipe(recipe: FilterRecipe): FilterRecipe =
        when (recipe.effect) {
            FilterEffect.Color -> recipe
            FilterEffect.Sketch -> recipe
            FilterEffect.Ink -> recipe
            FilterEffect.Pencil -> recipe.copy(
                adjustments = recipe.adjustments.copy(grain = recipe.adjustments.grain.scale(50)),
            )
            FilterEffect.ColorPencil -> recipe
            FilterEffect.Charcoal -> recipe.copy(
                adjustments = recipe.adjustments.copy(grain = recipe.adjustments.grain.scale(50)),
            )
            FilterEffect.CrossHatch -> recipe
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

    private const val PERCENT_MAX = 100
    private const val EFFECT_MIN = 0
    private const val EFFECT_MAX = 100
    private const val ART_SOURCE_SCALE = 2
    private const val DEFAULT_TEXEL_SCALE = 1f
}
