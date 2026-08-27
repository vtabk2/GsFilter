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
}
