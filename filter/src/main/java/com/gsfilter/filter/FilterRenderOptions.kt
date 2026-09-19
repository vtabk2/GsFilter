package com.gsfilter.filter

data class FilterRenderOptions(
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val useGpu: Boolean = true,
    val analysis: FilterAnalysis? = null,
)
