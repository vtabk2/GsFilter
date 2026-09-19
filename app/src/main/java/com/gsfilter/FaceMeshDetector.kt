package com.gsfilter

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.gsfilter.filter.MakeupFeatures
import com.gsfilter.filter.NormalizedPoint
import kotlin.math.atan2
import kotlin.math.max

internal class FaceMeshDetector(
    context: Context,
    private val listener: Listener,
) {

    private var lastTimestampMs = Long.MIN_VALUE
    private val faceLandmarker = FaceLandmarker.createFromOptions(
        context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .setDelegate(Delegate.CPU)
                    .build(),
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setResultListener(::onResult)
            .setErrorListener(listener::onError)
            .build(),
    )

    fun detect(bitmap: Bitmap, timestampNanos: Long) {
        val timestampMs = max(
            lastTimestampMs + 1L,
            timestampNanos / NANOS_PER_MILLISECOND,
        )
        lastTimestampMs = timestampMs
        val inputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        faceLandmarker.detectAsync(BitmapImageBuilder(inputBitmap).build(), timestampMs)
    }

    fun close() {
        faceLandmarker.close()
    }

    private fun onResult(result: FaceLandmarkerResult, _input: com.google.mediapipe.framework.image.MPImage) {
        val landmarks = result.faceLandmarks().firstOrNull()
        listener.onResult(result.timestampMs(), landmarks?.toMakeupFeatures())
    }

    private fun List<NormalizedLandmark>.toMakeupFeatures(): MakeupFeatures? {
        if (size < LANDMARK_COUNT) {
            return null
        }
        val faceOval = points(FACE_OVAL)
        val leftEye = points(LEFT_EYE)
        val rightEye = points(RIGHT_EYE)
        val leftEyebrow = points(LEFT_EYEBROW)
        val rightEyebrow = points(RIGHT_EYEBROW)
        val upperLip = points(UPPER_LIP)
        val lowerLip = points(LOWER_LIP)
        val lipContour = upperLip + lowerLip.asReversed()
        val faceLeft = faceOval.minOf { it.x }
        val faceRight = faceOval.maxOf { it.x }
        val faceTop = faceOval.minOf { it.y }
        val faceBottom = faceOval.maxOf { it.y }
        val leftEyeCenter = leftEye.center()
        val rightEyeCenter = rightEye.center()
        val lipCenter = lipContour.center()
        val rotation = if (leftEyeCenter != null && rightEyeCenter != null) {
            atan2(
                rightEyeCenter.y - leftEyeCenter.y,
                rightEyeCenter.x - leftEyeCenter.x,
            )
        } else {
            0f
        }

        return MakeupFeatures(
            leftCheekX = point(234).x,
            leftCheekY = point(234).y,
            rightCheekX = point(454).x,
            rightCheekY = point(454).y,
            cheekRadiusX = ((faceRight - faceLeft) * 0.12f).coerceAtLeast(0.01f),
            cheekRadiusY = ((faceBottom - faceTop) * 0.08f).coerceAtLeast(0.01f),
            lipCenterX = lipCenter?.x ?: 0.5f,
            lipCenterY = lipCenter?.y ?: 0.5f,
            lipRadiusX = lipContour.radiusX(lipCenter?.x ?: 0.5f),
            lipRadiusY = lipContour.radiusY(lipCenter?.y ?: 0.5f),
            rotationRadians = rotation,
            lipContour = lipContour,
            upperLipContour = upperLip,
            lowerLipContour = lowerLip,
            faceCenterX = ((faceLeft + faceRight) * 0.5f).coerceIn(0f, 1f),
            faceCenterY = ((faceTop + faceBottom) * 0.5f).coerceIn(0f, 1f),
            faceRadiusX = ((faceRight - faceLeft) * 0.58f).coerceAtLeast(0.01f),
            faceRadiusY = ((faceBottom - faceTop) * 0.58f).coerceAtLeast(0.01f),
            leftEyeCenterX = leftEyeCenter?.x ?: 0f,
            leftEyeCenterY = leftEyeCenter?.y ?: 0f,
            rightEyeCenterX = rightEyeCenter?.x ?: 0f,
            rightEyeCenterY = rightEyeCenter?.y ?: 0f,
            leftEyeRadiusX = leftEye.radiusX(leftEyeCenter?.x ?: 0f),
            leftEyeRadiusY = leftEye.radiusY(leftEyeCenter?.y ?: 0f),
            rightEyeRadiusX = rightEye.radiusX(rightEyeCenter?.x ?: 0f),
            rightEyeRadiusY = rightEye.radiusY(rightEyeCenter?.y ?: 0f),
            leftEyebrowContour = leftEyebrow,
            rightEyebrowContour = rightEyebrow,
        )
    }

    private fun List<NormalizedLandmark>.points(indices: IntArray): List<NormalizedPoint> =
        indices.map { point(it) }

    private fun List<NormalizedLandmark>.point(index: Int): NormalizedPoint =
        NormalizedPoint(
            x = this[index].x().coerceIn(0f, 1f),
            y = this[index].y().coerceIn(0f, 1f),
        )

    private fun List<NormalizedPoint>.center(): NormalizedPoint? {
        if (isEmpty()) return null
        return NormalizedPoint(
            x = map { it.x }.average().toFloat(),
            y = map { it.y }.average().toFloat(),
        )
    }

    private fun List<NormalizedPoint>.radiusX(center: Float): Float =
        maxOf(map { kotlin.math.abs(it.x - center) }.maxOrNull() ?: 0f, 0.004f)

    private fun List<NormalizedPoint>.radiusY(center: Float): Float =
        maxOf(map { kotlin.math.abs(it.y - center) }.maxOrNull() ?: 0f, 0.004f)

    interface Listener {
        fun onResult(timestampMs: Long, features: MakeupFeatures?)
        fun onError(error: RuntimeException)
    }

    private companion object {
        const val MODEL_ASSET = "face_landmarker.task"
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val LANDMARK_COUNT = 468
        val FACE_OVAL = intArrayOf(
            10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
            397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
            172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
        )
        val LEFT_EYE = intArrayOf(
            33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246,
        )
        val RIGHT_EYE = intArrayOf(
            362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398,
        )
        val LEFT_EYEBROW = intArrayOf(70, 63, 105, 66, 107, 55, 65, 52, 53, 46)
        val RIGHT_EYEBROW = intArrayOf(300, 293, 334, 296, 336, 285, 295, 282, 283, 276)
        val UPPER_LIP = intArrayOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291)
        val LOWER_LIP = intArrayOf(61, 185, 40, 39, 37, 0, 267, 269, 270, 409, 291)
    }
}
