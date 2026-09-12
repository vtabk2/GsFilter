package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams
import com.gsfilter.filter.gl.GlFilterProgram

object FilterRenderer {

    fun getBitmap(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int? = null,
        maxHeight: Int? = null,
    ): Bitmap {
        val params = ShaderFilterParams.from(recipe, adjustments)
        return getBitmapWithParams(
            source = source,
            params = params,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            isNoOp = FilterBitmapRenderer.isNoOp(params),
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
        val params = ShaderFilterParams.from(recipe, adjustments)
        val isNoOp = FilterBitmapRenderer.isNoOp(params)
        val makeupControlsEnabled = if (isNoOp) false else GlFilterProgram.hasMakeupControls(params)
        val adjustmentValuesEnabled = if (isNoOp) false else GlFilterProgram.hasAdjustmentValues(params)
        var completedCount = 0
        val batches = sources.withIndex().groupBy { indexedSource ->
            FilterBitmapRenderer.targetSize(
                width = indexedSource.value.width,
                height = indexedSource.value.height,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
            )
        }
        batches.forEach { (renderSize, batch) ->
            batch.forEach { indexedSource ->
                val bitmap = getBitmapWithParams(
                    source = indexedSource.value,
                    params = params,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                    renderSize = renderSize,
                    isNoOp = isNoOp,
                    makeupControlsEnabled = makeupControlsEnabled,
                    adjustmentValuesEnabled = adjustmentValuesEnabled,
                )
                onBitmap(indexedSource.index, bitmap)
                completedCount++
                onProgress(FilterRenderProgress(completedCount = completedCount, totalCount = totalCount))
            }
        }
    }

    private fun getBitmapWithParams(
        source: Bitmap,
        params: ShaderFilterParams,
        maxWidth: Int?,
        maxHeight: Int?,
        renderSize: FilterBitmapRenderer.RenderSize? = null,
        isNoOp: Boolean,
        makeupControlsEnabled: Boolean? = null,
        adjustmentValuesEnabled: Boolean? = null,
    ): Bitmap {
        if (isNoOp || FilterGpuBitmapRenderer.isOffscreenGpuUnavailable) {
            return FilterBitmapRenderer.getBitmapWithParams(
                source = source,
                params = params,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                renderSize = renderSize,
                noOp = isNoOp,
            )
        }
        return try {
            FilterGpuBitmapRenderer.getBitmapWithParams(
                source = source,
                params = params,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                renderSize = renderSize,
                makeupControlsEnabled = makeupControlsEnabled,
                adjustmentValuesEnabled = adjustmentValuesEnabled,
            )
        } catch (_: RuntimeException) {
            // Fall back for devices/contexts where offscreen EGL is unavailable.
            FilterBitmapRenderer.getBitmapWithParams(
                source = source,
                params = params,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                renderSize = renderSize,
                noOp = false,
            )
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
}
