package com.gsfilter

import com.gsfilter.utils.LoadUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleBitmapDecodeTest {

    @Test
    fun `sample size caps 16k asset to 4k before drawing`() {
        assertEquals(4, LoadUtils.bitmapInSampleSize(width = 15360, height = 15360, threshold = 4096))
    }

    @Test
    fun `sample size does not upscale smaller assets`() {
        assertEquals(1, LoadUtils.bitmapInSampleSize(width = 1024, height = 768, threshold = 4096))
    }

    @Test
    fun `image assets are filtered and sample stays first`() {
        assertEquals(
            listOf("sample.jpg", "portrait.png", "tilted.webp"),
            imageAssetPaths(arrayOf("filter_pack.json", "tilted.webp", "portrait.png", "sample.jpg", "notes.txt")),
        )
    }
}
