package com.gsfilter.filter.glide

import android.graphics.Bitmap
import com.gsfilter.filter.FilterOption

class FilterThumbnailModel(
    val sourceKey: String,
    val source: Bitmap,
    val filter: FilterOption,
) {

    override fun equals(other: Any?): Boolean {
        val model = other as? FilterThumbnailModel ?: return false
        return sourceKey == model.sourceKey &&
            source.width == model.source.width &&
            source.height == model.source.height &&
            filter.id == model.filter.id &&
            filter.recipe == model.filter.recipe
    }

    override fun hashCode(): Int {
        var result = sourceKey.hashCode()
        result = (31 * result) + source.width
        result = (31 * result) + source.height
        result = (31 * result) + filter.id.hashCode()
        result = (31 * result) + filter.recipe.hashCode()
        return result
    }

    override fun toString(): String =
        "FilterThumbnailModel($sourceKey,${source.width}x${source.height},${filter.id},${filter.recipe.hashCode()})"
}
