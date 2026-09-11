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
import com.gsfilter.filter.MakeupFeatures
import com.gsfilter.filter.ShaderFilterParams
import com.gsfilter.filter.gl.GlFilterProgram
import com.gsfilter.filter.gl.GlLutTexture
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

object FilterGpuBitmapRenderer {

    private val vertexBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.VERTICES)
    private val textureBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.TEXTURE_COORDS)

    fun getBitmap(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        scaleSource: Boolean = true,
        texelScale: Float = 1f,
        makeupFeatures: MakeupFeatures? = null,
        isCancelled: () -> Boolean = { false },
    ): Bitmap {
        acquireRenderLock(isCancelled)
        return try {
            if (isCancelled()) {
                throw CancellationException("Thumbnail render cancelled")
            }
            renderLocked(
                source = source,
                recipe = recipe,
                adjustments = adjustments,
                maxWidth = maxWidth,
                maxHeight = maxHeight,
                scaleSource = scaleSource,
                texelScale = texelScale,
                makeupFeatures = makeupFeatures,
            )
        } finally {
            renderLock.unlock()
        }
    }

    private fun acquireRenderLock(isCancelled: () -> Boolean) {
        try {
            while (!renderLock.tryLock(LOCK_WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                if (isCancelled()) {
                    throw CancellationException("Thumbnail render cancelled")
                }
            }
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw CancellationException("Thumbnail render interrupted").apply { initCause(error) }
        }
    }

    private fun renderLocked(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments,
        maxWidth: Int?,
        maxHeight: Int?,
        scaleSource: Boolean,
        texelScale: Float,
        makeupFeatures: MakeupFeatures?,
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
        val session = sessionFor(width, height)
        val params = ShaderFilterParams.from(recipe, adjustments, makeupFeatures)
        val makeupControlsEnabled = GlFilterProgram.hasMakeupControls(params)
        val uploadMakeupUniforms = session.makeupUniformsEnabled || makeupControlsEnabled
        val adjustmentValuesEnabled = GlFilterProgram.hasAdjustmentValues(params)
        val uploadAdjustmentUniforms = session.adjustmentUniformsNeedUpload || adjustmentValuesEnabled
        var lutTextureId = 0
        var invalidateSession = false

        return try {
            session.egl.makeCurrent()
            uploadTexture(renderSource, session)
            if (params.lutStrength > 0f) {
                lutTextureId = GlLutTexture.upload(params.lut)
            }

            val handles = session.handles
            GlFilterProgram.bindUniforms(
                handles = handles,
                textureId = session.inputTextureId,
                lutTextureId = lutTextureId,
                renderWidth = width,
                renderHeight = height,
                params = params,
                texelScale = texelScale,
                uploadMakeupUniforms = uploadMakeupUniforms,
                uploadAdjustmentUniforms = uploadAdjustmentUniforms,
                bindTextureSampler = false,
            )
            session.makeupUniformsEnabled = makeupControlsEnabled
            session.adjustmentUniformsNeedUpload = adjustmentValuesEnabled
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, GlFilterProgram.VERTEX_COUNT)

            readBitmap(width, height)
        } catch (error: RuntimeException) {
            invalidateSession = true
            throw error
        } finally {
            if (lutTextureId != 0) {
                GLES20.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
            }
            session.egl.detach()
            if (invalidateSession && cachedSession === session) {
                cachedSession = null
                session.release()
            }
            FilterBitmapRenderer.recycleIfTemporary(renderSource, source)
            readbackBuffers.trim()
        }
    }

    private fun sessionFor(width: Int, height: Int): RenderSession {
        val current = cachedSession
        if (current != null && current.width == width && current.height == height) {
            return current
        }
        cachedSession = null
        current?.release()
        return RenderSession(width, height).also { cachedSession = it }
    }

    private fun uploadTexture(bitmap: Bitmap, session: RenderSession) {
        if (
            session.inputBitmap?.get() === bitmap &&
            session.inputBitmapGenerationId == bitmap.generationId &&
            session.inputTextureWidth == bitmap.width &&
            session.inputTextureHeight == bitmap.height &&
            session.inputTextureConfig == bitmap.config
        ) {
            return
        }
        val textureId = session.inputTextureId
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        if (
            bitmap.config == Bitmap.Config.ARGB_8888 &&
            session.inputTextureConfig == bitmap.config &&
            session.inputTextureWidth == bitmap.width &&
            session.inputTextureHeight == bitmap.height
        ) {
            GLUtils.texSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                bitmap,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
            )
        } else {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            session.inputTextureConfig = bitmap.config
            session.inputTextureWidth = bitmap.width
            session.inputTextureHeight = bitmap.height
        }
        session.inputBitmap = WeakReference(bitmap)
        session.inputBitmapGenerationId = bitmap.generationId
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        return textureId
    }

    private fun readBitmap(width: Int, height: Int): Bitmap {
        val pixelCount = width * height
        readbackBuffers.ensure(pixelCount)
        val buffer = requireNotNull(readbackBuffers.buffer)
        val pixels = readbackBuffers.pixels
        buffer.clear()
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        buffer.asIntBuffer().get(pixels, 0, pixelCount)
        var topRow = 0
        var bottomRow = (height - 1) * width
        repeat(height / 2) {
            for (column in 0 until width) {
                val top = pixels[topRow + column]
                val bottom = pixels[bottomRow + column]
                pixels[topRow + column] = argbFromRgba(bottom)
                pixels[bottomRow + column] = argbFromRgba(top)
            }
            topRow += width
            bottomRow -= width
        }
        if (height % 2 != 0) {
            for (column in 0 until width) {
                pixels[topRow + column] = argbFromRgba(pixels[topRow + column])
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun argbFromRgba(rgba: Int): Int = (rgba shl 24) or (rgba ushr 8)

    private class ReadbackBuffers {
        var buffer: ByteBuffer? = null
        var pixels = IntArray(0)

        fun ensure(pixelCount: Int) {
            val byteCount = pixelCount * BYTES_PER_PIXEL
            if ((buffer?.capacity() ?: 0) < byteCount) {
                buffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.BIG_ENDIAN)
            }
            if (pixels.size < pixelCount) {
                pixels = IntArray(pixelCount)
            }
        }

        fun trim() {
            if (pixels.size > MAX_CACHED_READBACK_PIXELS) {
                buffer = null
                pixels = IntArray(0)
            }
        }
    }

    private class RenderSession(
        val width: Int,
        val height: Int,
    ) {
        val egl = EglPbuffer(width, height)
        var program = 0
            private set
        var inputTextureId = 0
            private set
        var inputTextureWidth = 0
        var inputTextureHeight = 0
        var inputTextureConfig: Bitmap.Config? = null
        var inputBitmap: WeakReference<Bitmap>? = null
        var inputBitmapGenerationId = 0
        var makeupUniformsEnabled = false
        var adjustmentUniformsNeedUpload = true
        lateinit var handles: GlFilterProgram.ProgramHandles
            private set

        init {
            var initialized = false
            try {
                egl.makeCurrent()
                program = GlFilterProgram.buildProgram()
                handles = GlFilterProgram.resolveHandles(program)
                inputTextureId = createTexture()
                GLES20.glUseProgram(program)
                GLES20.glUniform1i(handles.texture, 0)
                GLES20.glViewport(0, 0, width, height)
                GlFilterProgram.bindAttributes(handles, vertexBuffer, textureBuffer)
                initialized = true
            } finally {
                egl.detach()
                if (!initialized) {
                    egl.release()
                }
            }
        }

        fun release() {
            try {
                egl.makeCurrent()
                if (program != 0) {
                    GLES20.glDeleteProgram(program)
                    program = 0
                }
                if (inputTextureId != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(inputTextureId), 0)
                    inputTextureId = 0
                }
            } catch (_: RuntimeException) {
                // The EGL context may already be lost; release still must run.
            } finally {
                egl.release()
            }
        }
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

        fun detach() {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    display,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
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
    private const val MAX_CACHED_READBACK_PIXELS = 1_048_576
    // ponytail: one offscreen GL render at a time; split locks if profiling proves parallel EGL helps.
    private val renderLock = ReentrantLock()
    private const val LOCK_WAIT_MILLIS = 8L
    // Accessed only from getBitmap(), while renderLock is held.
    private val readbackBuffers = ReadbackBuffers()
    // Accessed only from getBitmap(), while renderLock is held.
    // ponytail: cache one output size; replace on size changes to avoid unbounded EGL resources.
    private var cachedSession: RenderSession? = null

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
