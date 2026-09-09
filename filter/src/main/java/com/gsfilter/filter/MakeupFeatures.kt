package com.gsfilter.filter

data class NormalizedPoint(
    val x: Float,
    val y: Float,
)

/** Normalized face regions used by the GPU and CPU makeup paths. */
data class MakeupFeatures(
    val leftCheekX: Float,
    val leftCheekY: Float,
    val rightCheekX: Float,
    val rightCheekY: Float,
    val cheekRadiusX: Float,
    val cheekRadiusY: Float,
    val leftCheekStrength: Float = 1f,
    val rightCheekStrength: Float = 1f,
    val lipCenterX: Float,
    val lipCenterY: Float,
    val lipRadiusX: Float,
    val lipRadiusY: Float,
    val rotationRadians: Float = 0f,
    val lipContour: List<NormalizedPoint> = emptyList(),
    val upperLipContour: List<NormalizedPoint> = emptyList(),
    val lowerLipContour: List<NormalizedPoint> = emptyList(),
)
