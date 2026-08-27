package com.gsfilter.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FilterLutTest {

    @Test
    fun `none LUT leaves color unchanged`() {
        val output = FloatArray(3)

        FilterLut.None.apply(0.2f, 0.4f, 0.6f, output)

        assertEquals(0.2f, output[0], DELTA)
        assertEquals(0.4f, output[1], DELTA)
        assertEquals(0.6f, output[2], DELTA)
    }

    @Test
    fun `color LUTs produce visible grade changes`() {
        val output = FloatArray(3)

        FilterLut.entries
            .filterNot { it == FilterLut.None }
            .forEach { lut ->
                lut.apply(0.35f, 0.45f, 0.55f, output)

                val delta = abs(output[0] - 0.35f) + abs(output[1] - 0.45f) + abs(output[2] - 0.55f)
                assertTrue("$lut delta was $delta", delta >= VISIBLE_DELTA)
            }
    }

    private companion object {
        const val DELTA = 0.0001f
        const val VISIBLE_DELTA = 0.08f
    }
}
