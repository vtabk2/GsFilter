package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterRecipe

object FilterRenderer {

    fun getBitmap(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int? = null,
        maxHeight: Int? = null,
    ): Bitmap =
        try {
            FilterGpuBitmapRenderer.getBitmap(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
        } catch (_: RuntimeException) {
            // Fall back for devices/contexts where offscreen EGL is unavailable.
            FilterBitmapRenderer.getBitmap(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
        }

    /**
     * Renders one bitmap at a time so callers can save or recycle each result before the next render.
     * Items are grouped by their effective render size; [onBitmap] receives the original index.
     */
    fun renderBatch(
        sources: List<Bitmap>,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        onProgress: (FilterRenderProgress) -> Unit = {},
        onBitmap: (index: Int, bitmap: Bitmap) -> Unit,
    ) {
        val totalCount = sources.size
        onProgress(FilterRenderProgress(completedCount = 0, totalCount = totalCount))
        var completedCount = 0
        val batches = sources.withIndex().groupBy { indexedSource ->
            FilterBitmapRenderer.targetSize(
                width = indexedSource.value.width,
                height = indexedSource.value.height,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
        }
        batches.values.forEach { batch ->
            batch.forEach { indexedSource ->
                val bitmap = getBitmap(
                    source = indexedSource.value,
                    recipe = recipe,
                    adjustments = adjustments,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                )
                onBitmap(indexedSource.index, bitmap)
                completedCount++
                onProgress(FilterRenderProgress(completedCount = completedCount, totalCount = totalCount))
            }
        }
    }
}

data class FilterRenderProgress(
    val completedCount: Int,
    val totalCount: Int,
) {
    val percent: Int
        get() =
            if (totalCount <= 0) {
                100
            } else {
                ((completedCount.coerceIn(0, totalCount).toLong() * PERCENT_MAX) / totalCount).toInt()
            }

    private companion object {
        const val PERCENT_MAX = 100
    }
}
