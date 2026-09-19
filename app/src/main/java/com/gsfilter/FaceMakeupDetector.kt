package com.gsfilter

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.gsfilter.filter.MakeupFeatures
import com.gsfilter.filter.NormalizedPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

internal class FaceMakeupDetector {

    private val detector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build(),
    )

    fun detect(bitmap: Bitmap, onResult: (MakeupFeatures?) -> Unit) {
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                onResult(faces.firstOrNull()?.toMakeupFeatures(bitmap.width, bitmap.height))
            }
            .addOnFailureListener { onResult(null) }
    }

    fun close() {
        detector.close()
    }

    private fun Face.toMakeupFeatures(width: Int, height: Int, rotationDegrees: Int = 0): MakeupFeatures? {
        val mapPoint = { point: PointF -> point.unrotate(width, height, rotationDegrees) }
        val leftCheek = getLandmark(FaceLandmark.LEFT_CHEEK)?.position?.let(mapPoint)
            ?: getContour(FaceContour.LEFT_CHEEK)?.points.orEmpty().map(mapPoint).center()
            ?: return null
        val rightCheek = getLandmark(FaceLandmark.RIGHT_CHEEK)?.position?.let(mapPoint)
            ?: getContour(FaceContour.RIGHT_CHEEK)?.points.orEmpty().map(mapPoint).center()
            ?: return null
        val lipPoints = listOf(
            FaceContour.UPPER_LIP_TOP,
            FaceContour.UPPER_LIP_BOTTOM,
            FaceContour.LOWER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM,
        ).flatMap { type -> getContour(type)?.points.orEmpty().map(mapPoint) }
        if (lipPoints.isEmpty()) {
            return null
        }
        val upperLipPoints = lipContourPolygon(
            FaceContour.UPPER_LIP_TOP,
            FaceContour.UPPER_LIP_BOTTOM,
        ).map(mapPoint)
        val lowerLipPoints = lipContourPolygon(
            FaceContour.LOWER_LIP_TOP,
            FaceContour.LOWER_LIP_BOTTOM,
        ).map(mapPoint)
        val outerLipPoints = (upperLipPoints + lowerLipPoints).takeIf { it.size >= 3 } ?: lipPoints

        val lipContourCenter = lipPoints.center() ?: return null
        val mouthLeft = getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.let(mapPoint)
        val mouthRight = getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.let(mapPoint)
        val lipCenterX = if (mouthLeft != null && mouthRight != null) {
            (mouthLeft.x + mouthRight.x) * 0.5f
        } else {
            lipContourCenter.x
        }
        val lipCenterY = lipContourCenter.y
        val bounds = boundingBox.toRectF(width, height, rotationDegrees)
        val leftEyePoints = getContour(FaceContour.LEFT_EYE)?.points.orEmpty().map(mapPoint)
        val rightEyePoints = getContour(FaceContour.RIGHT_EYE)?.points.orEmpty().map(mapPoint)
        val leftEyebrowPoints = eyebrowContour(
            FaceContour.LEFT_EYEBROW_TOP,
            FaceContour.LEFT_EYEBROW_BOTTOM,
        ).map(mapPoint)
        val rightEyebrowPoints = eyebrowContour(
            FaceContour.RIGHT_EYEBROW_TOP,
            FaceContour.RIGHT_EYEBROW_BOTTOM,
        ).map(mapPoint)
        val leftEye = leftEyePoints.center()
        val rightEye = rightEyePoints.center()
        val rotationRadians = if (leftEye != null && rightEye != null) {
            atan2(rightEye.y - leftEye.y, rightEye.x - leftEye.x)
        } else {
            0f
        }
        val cheekStrengths = cheekStrengths(leftCheek, rightCheek)
        val leftEyeRadius = eyeRadius(leftEyePoints, leftEye, width, height)
        val rightEyeRadius = eyeRadius(rightEyePoints, rightEye, width, height)

        // ponytail: one shared roll angle keeps the shader small; polygon masks can handle extreme poses later.
        return MakeupFeatures(
            leftCheekX = leftCheek.normalizedX(width),
            leftCheekY = leftCheek.normalizedY(height),
            rightCheekX = rightCheek.normalizedX(width),
            rightCheekY = rightCheek.normalizedY(height),
            cheekRadiusX = (bounds.width().toFloat() / width * 0.12f).coerceAtLeast(0.01f),
            cheekRadiusY = (bounds.height().toFloat() / height * 0.08f).coerceAtLeast(0.01f),
            leftCheekStrength = cheekStrengths.first,
            rightCheekStrength = cheekStrengths.second,
            lipCenterX = lipCenterX.coerceIn(0f, width.toFloat()) / width,
            lipCenterY = lipCenterY.coerceIn(0f, height.toFloat()) / height,
            lipRadiusX = (lipPoints.maxOf { abs(it.x - lipCenterX) } / width * 1.08f)
                .coerceAtLeast(0.01f),
            lipRadiusY = (lipPoints.maxOf { abs(it.y - lipCenterY) } / height * 1.12f)
                .coerceAtLeast(0.006f),
            rotationRadians = rotationRadians,
            lipContour = normalize(outerLipPoints, width, height),
            upperLipContour = normalize(upperLipPoints, width, height),
            lowerLipContour = normalize(lowerLipPoints, width, height),
            faceCenterX = bounds.centerX().toFloat().coerceIn(0f, width.toFloat()) / width,
            faceCenterY = bounds.centerY().toFloat().coerceIn(0f, height.toFloat()) / height,
            faceRadiusX = (bounds.width().toFloat() / width * 0.58f).coerceAtLeast(0.01f),
            faceRadiusY = (bounds.height().toFloat() / height * 0.58f).coerceAtLeast(0.01f),
            leftEyeCenterX = leftEye?.normalizedX(width) ?: 0f,
            leftEyeCenterY = leftEye?.normalizedY(height) ?: 0f,
            rightEyeCenterX = rightEye?.normalizedX(width) ?: 0f,
            rightEyeCenterY = rightEye?.normalizedY(height) ?: 0f,
            leftEyeRadiusX = leftEyeRadius.first,
            leftEyeRadiusY = leftEyeRadius.second,
            rightEyeRadiusX = rightEyeRadius.first,
            rightEyeRadiusY = rightEyeRadius.second,
            leftEyebrowContour = normalize(leftEyebrowPoints, width, height),
            rightEyebrowContour = normalize(rightEyebrowPoints, width, height),
        )
    }

    private fun Face.eyebrowContour(upperType: Int, lowerType: Int): List<PointF> =
        (getContour(upperType)?.points.orEmpty() +
            getContour(lowerType)?.points.orEmpty().asReversed())
            .takeIf { it.size >= 3 }
            .orEmpty()

    private fun Face.lipContourPolygon(outerType: Int, innerType: Int): List<PointF> =
        (getContour(outerType)?.points.orEmpty() +
            getContour(innerType)?.points.orEmpty().asReversed())
            .takeIf { it.size >= 3 }
            .orEmpty()

    private fun normalize(points: List<PointF>, width: Int, height: Int): List<NormalizedPoint> =
        points.map {
            NormalizedPoint(
                x = (it.x / width).coerceIn(0f, 1f),
                y = (it.y / height).coerceIn(0f, 1f),
            )
        }

    private fun Face.cheekStrengths(leftCheek: PointF, rightCheek: PointF): Pair<Float, Float> {
        val yaw = abs(headEulerAngleY)
        if (yaw < 15f) {
            return 1f to 1f
        }

        val leftEyeAvailable = getContour(FaceContour.LEFT_EYE)?.points?.isNotEmpty() == true
        val rightEyeAvailable = getContour(FaceContour.RIGHT_EYE)?.points?.isNotEmpty() == true
        val visibleLeft = when {
            leftEyeAvailable != rightEyeAvailable -> leftEyeAvailable
            else -> {
                val nose = getLandmark(FaceLandmark.NOSE_BASE)?.position
                    ?: PointF(boundingBox.centerX().toFloat(), boundingBox.centerY().toFloat())
                distance(leftCheek, nose) >= distance(rightCheek, nose)
            }
        }
        val hiddenStrength = (1f - ((yaw - 15f) / 20f)).coerceIn(0f, 1f)
        return if (visibleLeft) {
            1f to hiddenStrength
        } else {
            hiddenStrength to 1f
        }
    }

    private fun eyeRadius(
        points: List<PointF>,
        center: PointF?,
        width: Int,
        height: Int,
    ): Pair<Float, Float> {
        if (center == null || points.isEmpty()) {
            return 0f to 0f
        }
        return (
            points.maxOf { abs(it.x - center.x) } / width * 1.20f
        ).coerceAtLeast(0.004f) to (
            points.maxOf { abs(it.y - center.y) } / height * 1.20f
        ).coerceAtLeast(0.004f)
    }

    private fun distance(first: PointF, second: PointF): Float =
        hypot(first.x - second.x, first.y - second.y)

    private fun List<PointF>.center(): PointF? {
        if (isEmpty()) {
            return null
        }
        return PointF(
            map { it.x }.average().toFloat(),
            map { it.y }.average().toFloat(),
        )
    }

    private fun PointF.normalizedX(width: Int): Float = (x / width).coerceIn(0f, 1f)

    private fun PointF.normalizedY(height: Int): Float = (y / height).coerceIn(0f, 1f)

    private fun PointF.unrotate(width: Int, height: Int, rotationDegrees: Int): PointF =
        when ((rotationDegrees % 360 + 360) % 360) {
            90 -> PointF(y, height - x)
            180 -> PointF(width - x, height - y)
            270 -> PointF(width - y, x)
            else -> this
        }

    private fun android.graphics.Rect.toRectF(width: Int, height: Int, rotationDegrees: Int): RectF {
        val points = listOf(
            PointF(left.toFloat(), top.toFloat()).unrotate(width, height, rotationDegrees),
            PointF(right.toFloat(), top.toFloat()).unrotate(width, height, rotationDegrees),
            PointF(left.toFloat(), bottom.toFloat()).unrotate(width, height, rotationDegrees),
            PointF(right.toFloat(), bottom.toFloat()).unrotate(width, height, rotationDegrees),
        )
        return RectF(
            points.minOf { it.x },
            points.minOf { it.y },
            points.maxOf { it.x },
            points.maxOf { it.y },
        )
    }
}
