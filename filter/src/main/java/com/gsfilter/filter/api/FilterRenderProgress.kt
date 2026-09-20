package com.gsfilter.filter.api

data class FilterRenderProgress(
    val completedCount: Int,
    val totalCount: Int,
) {
    val percent: Int
        get() =
            if (totalCount <= 0) {
                100
            } else {
                ((completedCount.coerceIn(0, totalCount).toLong() * PERCENT_MAX) / totalCount).toInt()
            }

    private companion object {
        const val PERCENT_MAX = 100
    }
}
