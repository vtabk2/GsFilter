package com.gsfilter.filter.glide

import android.graphics.Bitmap
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.gsfilter.filter.renderer.FilterThumbnailRenderer

class FilterThumbnailModelLoader : ModelLoader<FilterThumbnailModel, Bitmap> {

    override fun buildLoadData(
        model: FilterThumbnailModel,
        width: Int,
        height: Int,
        options: Options,
    ): ModelLoader.LoadData<Bitmap> {
        val maxSize = FilterThumbnailRenderer.maxSizeFor(model.filter.recipe)
        val maxWidth = thumbnailBound(width, maxSize)
        val maxHeight = thumbnailBound(height, maxSize)
        return ModelLoader.LoadData(
            ObjectKey("${model.cacheKey}:${maxWidth}x$maxHeight"),
            FilterThumbnailDataFetcher(model, maxWidth, maxHeight),
        )
    }

    override fun handles(model: FilterThumbnailModel): Boolean = true

    class Factory : ModelLoaderFactory<FilterThumbnailModel, Bitmap> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<FilterThumbnailModel, Bitmap> =
            FilterThumbnailModelLoader()

        override fun teardown() = Unit
    }
}

private class FilterThumbnailDataFetcher(
    private val model: FilterThumbnailModel,
    private val maxWidth: Int,
    private val maxHeight: Int,
) : DataFetcher<Bitmap> {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        try {
            callback.onDataReady(
                FilterThumbnailRenderer.render(
                    source = model.source,
                    recipe = model.filter.recipe,
                    adjustments = model.adjustments,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight,
                ),
            )
        } catch (error: RuntimeException) {
            callback.onLoadFailed(error)
        }
    }

    override fun cleanup() = Unit

    override fun cancel() = Unit

    override fun getDataClass(): Class<Bitmap> = Bitmap::class.java

    override fun getDataSource(): DataSource = DataSource.LOCAL
}

private fun thumbnailBound(size: Int, maxSize: Int): Int =
    if (size > 0) {
        size.coerceAtMost(maxSize)
    } else {
        maxSize
    }
