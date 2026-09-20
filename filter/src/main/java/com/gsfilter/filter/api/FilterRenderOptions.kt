package com.gsfilter.filter.api

import com.gsfilter.filter.vision.FilterAnalysis

data class FilterRenderOptions(
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val useGpu: Boolean = true,
    val analysis: FilterAnalysis? = null,
)
