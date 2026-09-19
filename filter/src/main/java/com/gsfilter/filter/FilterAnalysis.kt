package com.gsfilter.filter

data class FilterAnalysis(
    val makeupFeatures: MakeupFeatures? = null,
    val foregroundMask: ForegroundMask? = null,
)

data class ForegroundMask(
    val width: Int,
    val height: Int,
    val confidence: FloatArray,
) {
    init {
        require(width > 0 && height > 0) { "Mask size must be positive." }
        require(confidence.size == width * height) { "Mask size must match width * height." }
    }

    fun sample(x: Float, y: Float): Float {
        val sourceX = (x.coerceIn(0f, 1f) * (width - 1)).coerceAtLeast(0f)
        val sourceY = (y.coerceIn(0f, 1f) * (height - 1)).coerceAtLeast(0f)
        val left = sourceX.toInt()
        val top = sourceY.toInt()
        val right = (left + 1).coerceAtMost(width - 1)
        val bottom = (top + 1).coerceAtMost(height - 1)
        val xAmount = sourceX - left
        val yAmount = sourceY - top
        val topValue = confidence[(top * width) + left] * (1f - xAmount) +
            confidence[(top * width) + right] * xAmount
        val bottomValue = confidence[(bottom * width) + left] * (1f - xAmount) +
            confidence[(bottom * width) + right] * xAmount
        return (topValue * (1f - yAmount) + bottomValue * yAmount).coerceIn(0f, 1f)
    }
}
