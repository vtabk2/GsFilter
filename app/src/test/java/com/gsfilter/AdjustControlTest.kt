package com.gsfilter

import com.gsfilter.filter.Adjustments
import org.junit.Assert.assertEquals
import org.junit.Test

class AdjustControlTest {

    @Test
    fun `signed controls map seekbar progress through negative and positive values`() {
        assertEquals(-100, AdjustControl.Brightness.valueFrom(0))
        assertEquals(0, AdjustControl.Brightness.valueFrom(100))
        assertEquals(100, AdjustControl.Brightness.valueFrom(200))
        assertEquals(100, AdjustControl.Brightness.progressFrom(0))
    }

    @Test
    fun `amount controls start at zero`() {
        assertEquals(100, AdjustControl.Sharpness.progressMax)
        assertEquals(0, AdjustControl.Sharpness.valueFrom(0))
        assertEquals(100, AdjustControl.Sharpness.valueFrom(100))
        assertEquals(0, AdjustControl.Sharpness.progressFrom(0))
    }

    @Test
    fun `control updates clamp to configured range`() {
        val adjustments = Adjustments()

        assertEquals(0, AdjustControl.Sharpness.update(adjustments, -20).sharpness)
        assertEquals(100, AdjustControl.Brightness.update(adjustments, 150).brightness)
    }
}
