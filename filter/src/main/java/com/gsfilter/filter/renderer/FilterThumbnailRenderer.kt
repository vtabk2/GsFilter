package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.renderer.FilterBitmapRenderer
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams

object FilterThumbnailRenderer {

    fun render(source: Bitmap, recipe: FilterRecipe): Bitmap =
        FilterBitmapRenderer.getBitmap(
            source = source,
            recipe = recipe,
            maxWidth = THUMBNAIL_MAX_SIZE,
            maxHeight = THUMBNAIL_MAX_SIZE,
        )

    internal fun filterPixel(
        pixels: IntArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
    ): Int = FilterBitmapRenderer.filterPixel(pixels, x, y, width, height, params)

    internal const val THUMBNAIL_MAX_SIZE = 128
}
