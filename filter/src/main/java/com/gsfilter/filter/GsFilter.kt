package com.gsfilter.filter

import android.graphics.Bitmap
import com.gsfilter.filter.renderer.FilterRenderer

object GsFilter {

    fun render(
        source: Bitmap,
        recipe: FilterRecipe = FilterRecipe.DEFAULT,
        adjustments: Adjustments = Adjustments.DEFAULT,
        options: FilterRenderOptions = FilterRenderOptions(),
    ): Bitmap = FilterRenderer.render(source, recipe, adjustments, options)

    fun renderBatch(
        sources: List<Bitmap>,
        recipe: FilterRecipe = FilterRecipe.DEFAULT,
        adjustments: Adjustments = Adjustments.DEFAULT,
        options: FilterRenderOptions = FilterRenderOptions(),
        onProgress: (FilterRenderProgress) -> Unit = {},
        onBitmap: (index: Int, bitmap: Bitmap) -> Unit,
    ) {
        FilterRenderer.renderBatch(
            sources = sources,
            recipe = recipe,
            adjustments = adjustments,
            options = options,
            onProgress = onProgress,
            onBitmap = onBitmap,
        )
    }
}
