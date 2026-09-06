package com.gsfilter

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleBitmapDecodeTest {

    @Test
    fun `sample size caps 16k asset to 4k before drawing`() {
        assertEquals(4, sampleBitmapInSampleSize(width = 15360, height = 15360, maxEdge = 4096))
    }

    @Test
    fun `sample size does not upscale smaller assets`() {
        assertEquals(1, sampleBitmapInSampleSize(width = 1024, height = 768, maxEdge = 4096))
    }
}
