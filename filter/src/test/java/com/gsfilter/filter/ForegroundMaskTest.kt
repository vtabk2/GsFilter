package com.gsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundMaskTest {

    @Test
    fun sampleUsesBilinearConfidenceAndClampsCoordinates() {
        val mask = ForegroundMask(
            width = 2,
            height = 2,
            confidence = floatArrayOf(0f, 1f, 1f, 0f),
        )

        assertEquals(0.5f, mask.sample(0.5f, 0.5f), 0.001f)
        assertEquals(0f, mask.sample(-1f, -1f), 0.001f)
        assertEquals(0f, mask.sample(2f, 2f), 0.001f)
    }
}
