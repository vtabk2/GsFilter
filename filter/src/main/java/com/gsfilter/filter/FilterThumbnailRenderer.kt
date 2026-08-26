package com.gsfilter.filter

import android.graphics.Bitmap

object FilterThumbnailRenderer {

    fun render(source: Bitmap, recipe: FilterRecipe): Bitmap =
        FilterBitmapRenderer.getBitmap(source, recipe)

    internal fun filterPixel(
        pixels: IntArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
    ): Int = FilterBitmapRenderer.filterPixel(pixels, x, y, width, height, params)
}
