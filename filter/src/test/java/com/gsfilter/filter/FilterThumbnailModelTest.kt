package com.gsfilter.filter

import com.gsfilter.filter.glide.FilterThumbnailModel
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterThumbnailModelTest {

    @Test
    fun `cache key changes when effect tuning changes`() {
        val first = FilterThumbnailModel.buildCacheKey(
            sourceKey = "asset:sample.jpg",
            sourceWidth = 128,
            sourceHeight = 96,
            filter = FilterOption(
                id = "pencil",
                categoryIds = setOf("art"),
                name = "Pencil",
                recipe = FilterRecipe(
                    effect = FilterEffect.Pencil,
                    effectStrength = 80,
                    effectThreshold = 40,
                ),
            ),
        )
        val second = FilterThumbnailModel.buildCacheKey(
            sourceKey = "asset:sample.jpg",
            sourceWidth = 128,
            sourceHeight = 96,
            filter = FilterOption(
                id = "pencil",
                categoryIds = setOf("art"),
                name = "Pencil",
                recipe = FilterRecipe(
                    effect = FilterEffect.Pencil,
                    effectStrength = 81,
                    effectThreshold = 40,
                ),
            ),
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `cache key includes render version`() {
        val key = FilterThumbnailModel.buildCacheKey(
            sourceKey = "asset:sample.jpg",
            sourceWidth = 128,
            sourceHeight = 96,
            filter = FilterOption(
                id = "fresh",
                categoryIds = setOf("popular"),
                name = "Fresh",
                recipe = FilterRecipe(),
            ),
        )

        assertTrue(key.startsWith("gpu-preview-v11:"))
    }

    @Test
    fun `cache key changes when adjustments change`() {
        val filter = FilterOption(
            id = "fresh",
            categoryIds = setOf("popular"),
            name = "Fresh",
            recipe = FilterRecipe(),
        )
        val first = FilterThumbnailModel.buildCacheKey(
            sourceKey = "asset:sample.jpg",
            sourceWidth = 128,
            sourceHeight = 96,
            filter = filter,
            adjustments = Adjustments(brightness = 10),
        )
        val second = FilterThumbnailModel.buildCacheKey(
            sourceKey = "asset:sample.jpg",
            sourceWidth = 128,
            sourceHeight = 96,
            filter = filter,
            adjustments = Adjustments(brightness = 20),
        )

        assertNotEquals(first, second)
    }
}
