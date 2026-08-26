package com.gsfilter.filter.view

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.AttributeSet
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.ShaderFilterParams
import com.gsfilter.filter.gl.GlFilterProgram
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FilterPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val filterRenderer = FilterRenderer()
    private var lastFilterParams: ShaderFilterParams? = null
    private var pendingFilterParams: ShaderFilterParams? = null
    private var isFilterRenderPosted = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(filterRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setSourceBitmap(bitmap: Bitmap?) {
        queueEvent {
            filterRenderer.setSourceBitmap(bitmap)
            requestRender()
        }
    }

    fun setFilterState(recipe: FilterRecipe, adjustments: Adjustments) {
        val params = ShaderFilterParams.from(recipe, adjustments)
        if (params == lastFilterParams) {
            return
        }

        lastFilterParams = params
        pendingFilterParams = params
        if (isFilterRenderPosted) {
            return
        }

        isFilterRenderPosted = true
        postOnAnimation {
            val nextParams = pendingFilterParams
            pendingFilterParams = null
            isFilterRenderPosted = false
            if (nextParams != null) {
                queueEvent {
                    filterRenderer.setFilterParams(nextParams)
                    requestRender()
                }
            }
        }
    }

    private class FilterRenderer : Renderer {

        private val vertexBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.VERTICES)
        private val textureBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.TEXTURE_COORDS)

        private var program = 0
        private var handles: GlFilterProgram.ProgramHandles? = null
        private var textureId = 0
        private var pendingBitmap: Bitmap? = null
        private var imageWidth = 0
        private var imageHeight = 0
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var renderWidth = 0
        private var renderHeight = 0
        private var params = ShaderFilterParams.from(FilterRecipe(), Adjustments())

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            program = GlFilterProgram.buildProgram()
            handles = GlFilterProgram.resolveHandles(program)
            GLES20.glClearColor(0.93f, 0.93f, 0.93f, 1f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            surfaceWidth = width
            surfaceHeight = height
            GLES20.glViewport(0, 0, width, height)
            updateVertexBuffer()
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            uploadPendingBitmap()
            if (textureId == 0 || program == 0) {
                return
            }
            val currentHandles = handles ?: return

            GLES20.glUseProgram(program)
            GlFilterProgram.bindAttributes(currentHandles, vertexBuffer, textureBuffer)
            GlFilterProgram.bindUniforms(currentHandles, textureId, renderWidth, renderHeight, params)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, GlFilterProgram.VERTEX_COUNT)
            GlFilterProgram.disableAttributes(currentHandles)
        }

        fun setSourceBitmap(bitmap: Bitmap?) {
            pendingBitmap = bitmap
        }

        fun setFilterParams(nextParams: ShaderFilterParams) {
            params = nextParams
        }

        private fun uploadPendingBitmap() {
            val bitmap = pendingBitmap ?: return
            pendingBitmap = null

            if (textureId == 0) {
                val textures = IntArray(1)
                GLES20.glGenTextures(1, textures, 0)
                textureId = textures[0]
            }

            imageWidth = bitmap.width
            imageHeight = bitmap.height
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            updateVertexBuffer()
        }

        private fun updateVertexBuffer() {
            if (imageWidth == 0 || imageHeight == 0 || surfaceWidth == 0 || surfaceHeight == 0) {
                renderWidth = 0
                renderHeight = 0
                vertexBuffer.clear()
                vertexBuffer.put(GlFilterProgram.VERTICES).position(0)
                return
            }

            val imageRatio = imageWidth.toFloat() / imageHeight
            val surfaceRatio = surfaceWidth.toFloat() / surfaceHeight
            val scaleX: Float
            val scaleY: Float
            if (imageRatio > surfaceRatio) {
                scaleX = 1f
                scaleY = surfaceRatio / imageRatio
            } else {
                scaleX = imageRatio / surfaceRatio
                scaleY = 1f
            }
            renderWidth = ((surfaceWidth * scaleX) + 0.5f).toInt().coerceAtLeast(1)
            renderHeight = ((surfaceHeight * scaleY) + 0.5f).toInt().coerceAtLeast(1)

            vertexBuffer.clear()
            vertexBuffer.put(
                floatArrayOf(
                    -scaleX,
                    -scaleY,
                    scaleX,
                    -scaleY,
                    -scaleX,
                    scaleY,
                    scaleX,
                    scaleY,
                ),
            ).position(0)
        }
    }
}
