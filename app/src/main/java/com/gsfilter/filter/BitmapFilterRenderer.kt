package com.gsfilter.filter

import android.graphics.Bitmap

object BitmapFilterRenderer {

    fun render(
        source: Bitmap,
        filter: FilterOption,
        adjustments: Adjustments,
    ): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        val filteredPixels = IntArray(pixels.size)

        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (index in pixels.indices) {
            filteredPixels[index] = ColorFilters.apply(
                argb = pixels[index],
                recipe = filter.recipe,
                adjustments = adjustments,
            )
        }

        return Bitmap.createBitmap(filteredPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
