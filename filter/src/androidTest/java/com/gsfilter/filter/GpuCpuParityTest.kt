package com.gsfilter.filter

import android.graphics.Bitmap
import com.gsfilter.filter.renderer.FilterBitmapRenderer
import com.gsfilter.filter.renderer.FilterGpuBitmapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuCpuParityTest {

    @Test
    fun beautyWarpArtStayCloseBetweenGpuAndCpu() {
        val features = MakeupFeatures(
            leftCheekX = 0.28f,
            leftCheekY = 0.5f,
            rightCheekX = 0.72f,
            rightCheekY = 0.5f,
            cheekRadiusX = 0.22f,
            cheekRadiusY = 0.3f,
            lipCenterX = 0f,
            lipCenterY = 0f,
            lipRadiusX = 0f,
            lipRadiusY = 0f,
            faceCenterX = 0.5f,
            faceCenterY = 0.5f,
            faceRadiusX = 0.45f,
            faceRadiusY = 0.5f,
            leftEyeCenterX = 0.35f,
            leftEyeCenterY = 0.43f,
            rightEyeCenterX = 0.65f,
            rightEyeCenterY = 0.43f,
            leftEyeRadiusX = 0.08f,
            leftEyeRadiusY = 0.05f,
            rightEyeRadiusX = 0.08f,
            rightEyeRadiusY = 0.05f,
        )
        listOf(
            FilterRecipe(effect = FilterEffect.Sketch, faceSlimming = 65),
            FilterRecipe(effect = FilterEffect.Ink, eyeEnlargement = 55),
        ).forEach { recipe ->
            val source = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(32 * 32) { index ->
                val x = index % 32
                val y = index / 32
                0xff000000.toInt() or ((x * 7) shl 16) or ((y * 7) shl 8) or (x * 3)
            }
            source.setPixels(pixels, 0, 32, 0, 0, 32, 32)
            val cpu = FilterBitmapRenderer.getBitmap(source, recipe, Adjustments.DEFAULT, makeupFeatures = features)
            val gpu = try {
                FilterGpuBitmapRenderer.getBitmap(source, recipe, Adjustments.DEFAULT, makeupFeatures = features)
            } finally {
                source.recycle()
            }

            try {
                assertEquals(cpu.width, gpu.width)
                assertEquals(cpu.height, gpu.height)
                val cpuPixels = IntArray(cpu.width * cpu.height)
                val gpuPixels = IntArray(gpu.width * gpu.height)
                cpu.getPixels(cpuPixels, 0, cpu.width, 0, 0, cpu.width, cpu.height)
                gpu.getPixels(gpuPixels, 0, gpu.width, 0, 0, gpu.width, gpu.height)
                var totalDifference = 0L
                var maxDifference = 0
                val channelShifts = intArrayOf(16, 8, 0)
                cpuPixels.indices.forEach { index ->
                    val cpuPixel = cpuPixels[index]
                    val gpuPixel = gpuPixels[index]
                    channelShifts.forEach { shift ->
                        val difference = kotlin.math.abs(
                            ((cpuPixel ushr shift) and 0xff) - ((gpuPixel ushr shift) and 0xff),
                        )
                        totalDifference += difference
                        maxDifference = maxOf(maxDifference, difference)
                    }
                }
                val meanDifference = totalDifference.toDouble() / (cpuPixels.size * 3)
                assertTrue("$recipe mean channel difference=$meanDifference", meanDifference < 12.0)
                assertTrue("$recipe max channel difference=$maxDifference", maxDifference <= 45)
            } finally {
                cpu.recycle()
                gpu.recycle()
            }
        }
    }
}
