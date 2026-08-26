package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams
import com.gsfilter.filter.gl.GlFilterProgram
import java.nio.ByteBuffer

object FilterGpuBitmapRenderer {

    fun getBitmap(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        scaleSource: Boolean = true,
        texelScale: Float = 1f,
    ): Bitmap {
        val renderSize = FilterBitmapRenderer.targetSize(source.width, source.height, maxWidth, maxHeight)
        val renderSource =
            if (scaleSource) {
                FilterBitmapRenderer.scaledSource(source, maxWidth, maxHeight)
            } else {
                source
            }
        val width = if (scaleSource) renderSource.width else renderSize.width
        val height = if (scaleSource) renderSource.height else renderSize.height
        val egl = EglPbuffer(width, height)
        var program = 0
        var textureId = 0

        return try {
            egl.makeCurrent()
            program = GlFilterProgram.buildProgram()
            textureId = uploadTexture(renderSource)
            GLES20.glViewport(0, 0, width, height)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)

            val handles = GlFilterProgram.resolveHandles(program)
            val vertexBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.VERTICES)
            val textureBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.TEXTURE_COORDS)
            GlFilterProgram.bindAttributes(handles, vertexBuffer, textureBuffer)
            GlFilterProgram.bindUniforms(
                handles = handles,
                textureId = textureId,
                renderWidth = width,
                renderHeight = height,
                params = ShaderFilterParams.from(recipe, adjustments),
                texelScale = texelScale,
            )
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, GlFilterProgram.VERTEX_COUNT)
            GlFilterProgram.disableAttributes(handles)
            GLES20.glFinish()

            readBitmap(width, height)
        } finally {
            if (textureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            }
            if (program != 0) {
                GLES20.glDeleteProgram(program)
            }
            egl.release()
            FilterBitmapRenderer.recycleIfTemporary(renderSource, source)
        }
    }

    private fun uploadTexture(bitmap: Bitmap): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        return textureId
    }

    private fun readBitmap(width: Int, height: Int): Bitmap {
        val buffer = ByteBuffer.allocateDirect(width * height * BYTES_PER_PIXEL)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val targetY = height - 1 - y
            for (x in 0 until width) {
                val offset = ((y * width) + x) * BYTES_PER_PIXEL
                val red = buffer.get(offset).toInt() and CHANNEL_MASK
                val green = buffer.get(offset + 1).toInt() and CHANNEL_MASK
                val blue = buffer.get(offset + 2).toInt() and CHANNEL_MASK
                val alpha = buffer.get(offset + 3).toInt() and CHANNEL_MASK
                pixels[(targetY * width) + x] =
                    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private class EglPbuffer(
        private val width: Int,
        private val height: Int,
    ) {
        private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        private var context: EGLContext = EGL14.EGL_NO_CONTEXT
        private var surface: EGLSurface = EGL14.EGL_NO_SURFACE

        init {
            try {
                display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                check(display != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display." }
                val version = IntArray(2)
                check(EGL14.eglInitialize(display, version, 0, version, 1)) {
                    "Unable to initialize EGL."
                }

                val configs = arrayOfNulls<EGLConfig>(1)
                val configCount = IntArray(1)
                check(
                    EGL14.eglChooseConfig(
                        display,
                        CONFIG_ATTRIBUTES,
                        0,
                        configs,
                        0,
                        configs.size,
                        configCount,
                        0,
                    ),
                ) {
                    "Unable to choose EGL config."
                }
                val config = requireNotNull(configs[0]) { "EGL config is missing." }

                context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, CONTEXT_ATTRIBUTES, 0)
                check(context != EGL14.EGL_NO_CONTEXT) { "Unable to create EGL context." }

                val surfaceAttributes = intArrayOf(
                    EGL14.EGL_WIDTH,
                    width,
                    EGL14.EGL_HEIGHT,
                    height,
                    EGL14.EGL_NONE,
                )
                surface = EGL14.eglCreatePbufferSurface(display, config, surfaceAttributes, 0)
                check(surface != EGL14.EGL_NO_SURFACE) { "Unable to create EGL pbuffer surface." }
            } catch (error: RuntimeException) {
                release()
                throw error
            }
        }

        fun makeCurrent() {
            check(EGL14.eglMakeCurrent(display, surface, surface, context)) {
                "Unable to make EGL context current."
            }
        }

        fun release() {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    display,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
                if (surface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(display, surface)
                }
                if (context != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(display, context)
                }
                EGL14.eglTerminate(display)
            }
            display = EGL14.EGL_NO_DISPLAY
            surface = EGL14.EGL_NO_SURFACE
            context = EGL14.EGL_NO_CONTEXT
        }
    }

    private const val BYTES_PER_PIXEL = 4
    private const val CHANNEL_MASK = 255

    private val CONFIG_ATTRIBUTES = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE,
        EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_SURFACE_TYPE,
        EGL14.EGL_PBUFFER_BIT,
        EGL14.EGL_RED_SIZE,
        8,
        EGL14.EGL_GREEN_SIZE,
        8,
        EGL14.EGL_BLUE_SIZE,
        8,
        EGL14.EGL_ALPHA_SIZE,
        8,
        EGL14.EGL_NONE,
    )

    private val CONTEXT_ATTRIBUTES = intArrayOf(
        EGL14.EGL_CONTEXT_CLIENT_VERSION,
        2,
        EGL14.EGL_NONE,
    )
}
