package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.FilterRenderOptions
import com.gsfilter.filter.FilterRenderProgress
import com.gsfilter.filter.ShaderFilterParams
import com.gsfilter.filter.gl.GlFilterProgram
import java.util.concurrent.CancellationException

internal object FilterRenderer {

    fun render(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments.DEFAULT,
        options: FilterRenderOptions = FilterRenderOptions(),
    ): Bitmap {
        val params = ShaderFilterParams.from(recipe, adjustments, options.makeupFeatures)
        return getBitmapWithParams(
            source = source,
            params = params,
            maxWidth = options.maxWidth,
            maxHeight = options.maxHeight,
            isNoOp = FilterBitmapRenderer.isNoOp(params),
            useGpu = options.useGpu,
        )
    }

    /**
     * Renders one bitmap at a time so callers can save or recycle each result before the next render.
     * Items are grouped by their effective render size; [onBitmap] receives the original index.
     */
    fun renderBatch(
        sources: List<Bitmap>,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments.DEFAULT,
        options: FilterRenderOptions = FilterRenderOptions(),
        onProgress: (FilterRenderProgress) -> Unit = {},
        onBitmap: (index: Int, bitmap: Bitmap) -> Unit,
    ) {
        val totalCount = sources.size
        onProgress(FilterRenderProgress(completedCount = 0, totalCount = totalCount))
        if (sources.isEmpty()) {
            return
        }
        val params = ShaderFilterParams.from(recipe, adjustments, options.makeupFeatures)
        val isNoOp = FilterBitmapRenderer.isNoOp(params)
        val makeupControlsEnabled = if (isNoOp) false else GlFilterProgram.hasMakeupControls(params)
        val adjustmentValuesEnabled = if (isNoOp) false else GlFilterProgram.hasAdjustmentValues(params)
        var completedCount = 0
        val batches = sources.withIndex().groupBy { indexedSource ->
            FilterBitmapRenderer.targetSize(
                width = indexedSource.value.width,
                height = indexedSource.value.height,
                maxWidth = options.maxWidth,
                maxHeight = options.maxHeight,
            )
        }
        batches.forEach { (renderSize, batch) ->
            batch.forEach { indexedSource ->
                val bitmap = getBitmapWithParams(
                    source = indexedSource.value,
                    params = params,
                    maxWidth = options.maxWidth,
                    maxHeight = options.maxHeight,
                    renderSize = renderSize,
                    isNoOp = isNoOp,
                    makeupControlsEnabled = makeupControlsEnabled,
                    adjustmentValuesEnabled = adjustmentValuesEnabled,
                    useGpu = options.useGpu,
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
        useGpu: Boolean,
    ): Bitmap {
        if (!useGpu || isNoOp || FilterGpuBitmapRenderer.isOffscreenGpuUnavailable) {
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
        } catch (error: RuntimeException) {
            if (error is CancellationException) {
                throw error
            }
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

}
