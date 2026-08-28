package com.gsfilter.filter

import com.gsfilter.filter.renderer.FilterRenderProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterRenderProgressTest {

    @Test
    fun `empty batch is complete`() {
        assertEquals(100, FilterRenderProgress(completedCount = 0, totalCount = 0).percent)
    }

    @Test
    fun `percent uses completed count`() {
        assertEquals(0, FilterRenderProgress(completedCount = 0, totalCount = 20).percent)
        assertEquals(5, FilterRenderProgress(completedCount = 1, totalCount = 20).percent)
        assertEquals(50, FilterRenderProgress(completedCount = 10, totalCount = 20).percent)
        assertEquals(100, FilterRenderProgress(completedCount = 20, totalCount = 20).percent)
    }

    @Test
    fun `percent stays in valid range`() {
        assertEquals(0, FilterRenderProgress(completedCount = -1, totalCount = 20).percent)
        assertEquals(100, FilterRenderProgress(completedCount = 21, totalCount = 20).percent)
    }
}
