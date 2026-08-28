package com.gsfilter.filter.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import com.gsfilter.filter.FilterLut

internal object GlLutTexture {

    fun upload(lut: FilterLut): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap(lut), 0)
        return textureId
    }

    private fun bitmap(lut: FilterLut): Bitmap =
        synchronized(bitmapLock) {
            bitmaps.getOrPut(lut) {
                val output = FloatArray(CHANNELS)
                val pixels = IntArray(TEXTURE_WIDTH * LUT_SIZE)
                for (blue in 0 until LUT_SIZE) {
                    for (green in 0 until LUT_SIZE) {
                        for (red in 0 until LUT_SIZE) {
                            lut.apply(
                                red = red / LUT_MAX,
                                green = green / LUT_MAX,
                                blue = blue / LUT_MAX,
                                out = output,
                            )
                            val x = (blue * LUT_SIZE) + red
                            val y = green
                            pixels[(y * TEXTURE_WIDTH) + x] = argb(output[0], output[1], output[2])
                        }
                    }
                }
                Bitmap.createBitmap(pixels, TEXTURE_WIDTH, LUT_SIZE, Bitmap.Config.ARGB_8888)
            }
        }

    private fun argb(red: Float, green: Float, blue: Float): Int =
        ALPHA or (channel(red) shl RED_SHIFT) or (channel(green) shl GREEN_SHIFT) or channel(blue)

    private fun channel(value: Float): Int =
        (value.coerceIn(0f, 1f) * CHANNEL_MASK).toInt().coerceIn(0, CHANNEL_MASK)

    private const val LUT_SIZE = 33
    private const val LUT_MAX = 32f
    private const val TEXTURE_WIDTH = LUT_SIZE * LUT_SIZE
    private const val CHANNELS = 3
    private const val CHANNEL_MASK = 255
    private const val ALPHA = 0xFF shl 24
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
    private val bitmapLock = Any()
    private val bitmaps = mutableMapOf<FilterLut, Bitmap>()
}
