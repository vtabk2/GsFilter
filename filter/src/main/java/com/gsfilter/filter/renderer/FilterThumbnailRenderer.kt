package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import com.gsfilter.filter.renderer.FilterBitmapRenderer
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams

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