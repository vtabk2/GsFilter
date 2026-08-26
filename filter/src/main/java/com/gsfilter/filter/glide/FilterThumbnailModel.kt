package com.gsfilter.filter.glide

import android.graphics.Bitmap
import com.gsfilter.filter.FilterOption

class FilterThumbnailModel(
    val sourceKey: String,
    val source: Bitmap,
    val filter: FilterOption,
) {

    val cacheKey: String = buildCacheKey(sourceKey, source.width, source.height, filter)

    override fun equals(other: Any?): Boolean {
        val model = other as? FilterThumbnailModel ?: return false
        return cacheKey == model.cacheKey
    }

    override fun hashCode(): Int = cacheKey.hashCode()

    override fun toString(): String = cacheKey

    internal companion object {
        fun buildCacheKey(
            sourceKey: String,
            sourceWidth: Int,
            sourceHeight: Int,
            filter: FilterOption,
        ): String =
            "$sourceKey:${sourceWidth}x$sourceHeight:${filter.id}:${filter.recipe}"
    }
}
