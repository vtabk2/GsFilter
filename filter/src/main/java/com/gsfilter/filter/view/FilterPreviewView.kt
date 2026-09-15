package com.gsfilter.filter.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.AttributeSet
import android.view.Surface
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterLut
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.MakeupFeatures
import com.gsfilter.filter.ShaderFilterParams
import com.gsfilter.filter.gl.GlFilterProgram
import com.gsfilter.filter.gl.GlLutTexture
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FilterPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val filterRenderer = FilterRenderer()
    private var lastSourceBitmap: Bitmap? = null
    private var lastSourceGenerationId = 0
    private var pendingSourceBitmap: Bitmap? = null
    private var hasPendingSourceBitmap = false
    private var isSourceUpdatePosted = false
    private var lastFilterParams: ShaderFilterParams? = null
    private var lastRecipe: FilterRecipe? = null
    private var lastAdjustments: Adjustments? = null
    private var lastMakeupFeatures: MakeupFeatures? = null
    private var pendingFilterParams: ShaderFilterParams? = null
    private var pendingFilterGeneration = 0L
    private var nextFilterGeneration = 0L
    @Volatile
    private var latestFilterGeneration = 0L
    private var isFilterRenderPosted = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(filterRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setSourceBitmap(bitmap: Bitmap?) {
        val generationId = bitmap?.generationId ?: 0
        if (lastSourceBitmap === bitmap && lastSourceGenerationId == generationId) {
            return
        }
        lastSourceBitmap = bitmap
        lastSourceGenerationId = generationId
        pendingSourceBitmap = bitmap
        hasPendingSourceBitmap = true
        if (isSourceUpdatePosted) {
            return
        }
        isSourceUpdatePosted = true
        queueEvent {
            if (hasPendingSourceBitmap) {
                val nextBitmap = pendingSourceBitmap
                pendingSourceBitmap = null
                hasPendingSourceBitmap = false
                isSourceUpdatePosted = false
                filterRenderer.setSourceBitmap(nextBitmap)
                requestRender()
            } else {
                isSourceUpdatePosted = false
            }
        }
    }

    fun setCameraSource(enabled: Boolean, onSurfaceReady: ((Surface?) -> Unit)? = null) {
        renderMode = RENDERMODE_WHEN_DIRTY
        queueEvent {
            filterRenderer.setCameraSource(
                enabled = enabled,
                width = CAMERA_WIDTH,
                height = CAMERA_HEIGHT,
                onFrameAvailable = ::requestRender,
            ) { surface -> post { onSurfaceReady?.invoke(surface) } }
            requestRender()
        }
    }

    fun setFilterState(
        recipe: FilterRecipe,
        adjustments: Adjustments,
        makeupFeatures: MakeupFeatures? = null,
    ) {
        if (
            recipe == lastRecipe &&
            adjustments == lastAdjustments &&
            (makeupFeatures === lastMakeupFeatures || makeupFeatures == lastMakeupFeatures)
        ) {
            return
        }
        val params = ShaderFilterParams.from(recipe, adjustments, makeupFeatures)
        lastRecipe = recipe
        lastAdjustments = adjustments
        lastMakeupFeatures = makeupFeatures
        if (params == lastFilterParams) {
            return
        }

        lastFilterParams = params
        val filterGeneration = ++nextFilterGeneration
        latestFilterGeneration = filterGeneration
        pendingFilterParams = params
        pendingFilterGeneration = filterGeneration
        if (isFilterRenderPosted) {
            return
        }

        isFilterRenderPosted = true
        postOnAnimation {
            val nextParams = pendingFilterParams
            val nextGeneration = pendingFilterGeneration
            pendingFilterParams = null
            isFilterRenderPosted = false
            if (nextParams != null) {
                queueEvent {
                    if (nextGeneration == latestFilterGeneration) {
                        filterRenderer.setFilterParams(nextParams)
                        requestRender()
                    }
                }
            }
        }
    }

    private class FilterRenderer : Renderer {

        private val vertexBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.VERTICES)
        private val textureBuffer = GlFilterProgram.floatBufferOf(GlFilterProgram.TEXTURE_COORDS)
        private val defaultTextureCoords = GlFilterProgram.TEXTURE_COORDS.copyOf()
        private val cameraSourceCoords = floatArrayOf(
            0f,
            0f,
            1f,
            0f,
            0f,
            1f,
            1f,
            1f,
        )
        private val cameraTextureMatrix = FloatArray(16)
        private val cameraTextureCoords = FloatArray(defaultTextureCoords.size)

        private var program = 0
        private var bitmapProgram = 0
        private var cameraProgram = 0
        private var handles: GlFilterProgram.ProgramHandles? = null
        private var textureId = 0
        private var cameraTextureId = 0
        private var textureConfig: Bitmap.Config? = null
        private var lutTextureId = 0
        private var lutTexture = FilterLut.None
        private var sourceBitmap: Bitmap? = null
        private var pendingBitmap: Bitmap? = null
        private var imageWidth = 0
        private var imageHeight = 0
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var renderWidth = 0
        private var renderHeight = 0
        private var params = ShaderFilterParams.from(FilterRecipe.DEFAULT, Adjustments.DEFAULT)
        private var makeupUniformsNeedUpload = true
        private var adjustmentUniformsNeedUpload = true
        private var effectUniformsNeedUpload = true
        private var texelSizeNeedUpload = true
        private var cameraSurfaceTexture: SurfaceTexture? = null
        private var cameraSurface: Surface? = null
        private var cameraWidth = 0
        private var cameraHeight = 0
        @Volatile
        private var cameraFrameAvailable = false
        private var hasCameraFrame = false
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            bitmapProgram = GlFilterProgram.buildProgram()
            program = bitmapProgram
            val currentHandles = GlFilterProgram.resolveHandles(program)
            handles = currentHandles
            GLES20.glUseProgram(program)
            GLES20.glUniform1i(currentHandles.texture, 0)
            GLES20.glUniform1i(currentHandles.lutTexture, 1)
            GlFilterProgram.bindAttributes(currentHandles, vertexBuffer, textureBuffer)
            textureId = 0
            textureConfig = null
            lutTextureId = 0
            lutTexture = FilterLut.None
            imageWidth = 0
            imageHeight = 0
            makeupUniformsNeedUpload = true
            adjustmentUniformsNeedUpload = true
            effectUniformsNeedUpload = true
            texelSizeNeedUpload = true
            pendingBitmap = sourceBitmap
            GLES20.glClearColor(0.93f, 0.93f, 0.93f, 1f)
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            surfaceWidth = width
            surfaceHeight = height
            GLES20.glViewport(0, 0, width, height)
            updateVertexBuffer()
            texelSizeNeedUpload = true
        }

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            val cameraTexture = cameraSurfaceTexture
            if (cameraTexture != null) {
                if (cameraFrameAvailable) {
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
                    cameraTexture.updateTexImage()
                    cameraTexture.getTransformMatrix(cameraTextureMatrix)
                    updateCameraTextureCoordinates()
                    cameraFrameAvailable = false
                    hasCameraFrame = true
                }
                if (!hasCameraFrame) {
                    return
                }
            } else {
                uploadPendingBitmap()
            }
            val inputTextureId = if (cameraTexture != null) cameraTextureId else textureId
            if (inputTextureId == 0 || program == 0) {
                return
            }
            val currentHandles = handles ?: return
            GlFilterProgram.bindUniforms(
                handles = currentHandles,
                textureId = inputTextureId,
                lutTextureId = lutTextureFor(params),
                renderWidth = renderWidth,
                renderHeight = renderHeight,
                params = params,
                uploadMakeupUniforms = makeupUniformsNeedUpload,
                uploadAdjustmentUniforms = adjustmentUniformsNeedUpload,
                uploadEffectUniforms = effectUniformsNeedUpload,
                uploadTexelSize = texelSizeNeedUpload,
                inputTextureTarget = if (cameraTexture != null) {
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                } else {
                    GLES20.GL_TEXTURE_2D
                },
            )
            makeupUniformsNeedUpload = false
            adjustmentUniformsNeedUpload = false
            effectUniformsNeedUpload = false
            texelSizeNeedUpload = false
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, GlFilterProgram.VERTEX_COUNT)
        }

        fun setSourceBitmap(bitmap: Bitmap?) {
            sourceBitmap = bitmap
            pendingBitmap = bitmap
        }

        fun setCameraSource(
            enabled: Boolean,
            width: Int,
            height: Int,
            onFrameAvailable: () -> Unit,
            onSurfaceReady: (Surface?) -> Unit,
        ) {
            if (!enabled) {
                cameraSurface?.release()
                cameraSurfaceTexture?.release()
                cameraSurface = null
                cameraSurfaceTexture = null
                cameraFrameAvailable = false
                hasCameraFrame = false
                makeupUniformsNeedUpload = true
                adjustmentUniformsNeedUpload = true
                effectUniformsNeedUpload = true
                texelSizeNeedUpload = true
                textureBuffer.clear()
                textureBuffer.put(defaultTextureCoords).position(0)
                if (cameraTextureId != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(cameraTextureId), 0)
                    cameraTextureId = 0
                }
                if (cameraProgram != 0) {
                    GLES20.glDeleteProgram(cameraProgram)
                    cameraProgram = 0
                }
                program = bitmapProgram
                handles = if (program != 0) GlFilterProgram.resolveHandles(program) else null
                if (handles != null) {
                    GLES20.glUseProgram(program)
                    GlFilterProgram.bindAttributes(handles!!, vertexBuffer, textureBuffer)
                }
                pendingBitmap = sourceBitmap
                return
            }
            if (cameraSurface != null) {
                onSurfaceReady(cameraSurface)
                return
            }

            cameraWidth = width
            cameraHeight = height
            cameraProgram = try {
                GlFilterProgram.buildProgram(useExternalTexture = true)
            } catch (error: IllegalStateException) {
                onSurfaceReady(null)
                return
            }
            val textures = IntArray(1)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glGenTextures(1, textures, 0)
            cameraTextureId = textures[0]
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR,
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE,
            )
            cameraSurfaceTexture = SurfaceTexture(cameraTextureId).apply {
                setDefaultBufferSize(width, height)
                setOnFrameAvailableListener {
                    cameraFrameAvailable = true
                    onFrameAvailable()
                }
            }
            cameraSurface = Surface(requireNotNull(cameraSurfaceTexture))
            program = cameraProgram
            handles = GlFilterProgram.resolveHandles(program)
            makeupUniformsNeedUpload = true
            adjustmentUniformsNeedUpload = true
            effectUniformsNeedUpload = true
            texelSizeNeedUpload = true
            GLES20.glUseProgram(program)
            GlFilterProgram.bindAttributes(handles!!, vertexBuffer, textureBuffer)
            imageWidth = height
            imageHeight = width
            updateVertexBuffer()
            onSurfaceReady(cameraSurface)
        }

        private fun updateCameraTextureCoordinates() {
            var index = 0
            while (index < defaultTextureCoords.size) {
                val x = cameraSourceCoords[index]
                val y = cameraSourceCoords[index + 1]
                val transformedX =
                    (cameraTextureMatrix[0] * x) + (cameraTextureMatrix[4] * y) + cameraTextureMatrix[12]
                val transformedY =
                    (cameraTextureMatrix[1] * x) + (cameraTextureMatrix[5] * y) + cameraTextureMatrix[13]
                val transformedW =
                    (cameraTextureMatrix[3] * x) + (cameraTextureMatrix[7] * y) + cameraTextureMatrix[15]
                val safeW = if (transformedW == 0f) 1f else transformedW
                cameraTextureCoords[index] = transformedX / safeW
                cameraTextureCoords[index + 1] = transformedY / safeW
                index += 2
            }
            textureBuffer.clear()
            textureBuffer.put(cameraTextureCoords).position(0)
        }

        fun setFilterParams(nextParams: ShaderFilterParams) {
            makeupUniformsNeedUpload = makeupUniformsNeedUpload || makeupParamsChanged(params, nextParams)
            adjustmentUniformsNeedUpload = adjustmentUniformsNeedUpload || adjustmentParamsChanged(params, nextParams)
            effectUniformsNeedUpload = effectUniformsNeedUpload || effectParamsChanged(params, nextParams)
            params = nextParams
        }

        private fun effectParamsChanged(
            previous: ShaderFilterParams,
            next: ShaderFilterParams,
        ): Boolean =
            previous.effect != next.effect ||
                previous.effectStrength != next.effectStrength ||
                previous.effectThreshold != next.effectThreshold ||
                previous.effectTone != next.effectTone ||
                previous.intensity != next.intensity ||
                previous.isMonochrome != next.isMonochrome

        private fun makeupParamsChanged(
            previous: ShaderFilterParams,
            next: ShaderFilterParams,
        ): Boolean =
            previous.skinSmoothing != next.skinSmoothing ||
                previous.skinWhitening != next.skinWhitening ||
                previous.blush != next.blush ||
                previous.lipstick != next.lipstick ||
                previous.underEye != next.underEye ||
                previous.teethWhitening != next.teethWhitening ||
                previous.eyeShadow != next.eyeShadow ||
                previous.eyeliner != next.eyeliner ||
                previous.eyebrow != next.eyebrow ||
                previous.faceSlimming != next.faceSlimming ||
                previous.eyeEnlargement != next.eyeEnlargement ||
                (previous.makeupFeatures !== next.makeupFeatures &&
                    previous.makeupFeatures != next.makeupFeatures)

        private fun adjustmentParamsChanged(
            previous: ShaderFilterParams,
            next: ShaderFilterParams,
        ): Boolean =
            previous.redShift != next.redShift ||
                previous.greenShift != next.greenShift ||
                previous.blueShift != next.blueShift ||
                previous.brightness != next.brightness ||
                previous.exposure != next.exposure ||
                previous.contrast != next.contrast ||
                previous.highlights != next.highlights ||
                previous.shadows != next.shadows ||
                previous.saturation != next.saturation ||
                previous.vibrance != next.vibrance ||
                previous.temperature != next.temperature ||
                previous.tint != next.tint ||
                previous.sharpness != next.sharpness ||
                previous.clarity != next.clarity ||
                previous.fade != next.fade ||
                previous.vignette != next.vignette ||
                previous.grain != next.grain

        private fun lutTextureFor(params: ShaderFilterParams): Int {
            if (params.lutStrength <= 0f) {
                if (lutTextureId != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
                    lutTextureId = 0
                    lutTexture = FilterLut.None
                }
                return 0
            }
            if (lutTextureId != 0 && lutTexture == params.lut) {
                return lutTextureId
            }
            if (lutTextureId != 0) {
                GlLutTexture.update(lutTextureId, params.lut)
            } else {
                lutTextureId = GlLutTexture.upload(params.lut)
            }
            lutTexture = params.lut
            return lutTextureId
        }

        private fun uploadPendingBitmap() {
            val bitmap = pendingBitmap ?: return
            pendingBitmap = null

            val isNewTexture = textureId == 0
            if (isNewTexture) {
                val textures = IntArray(1)
                GLES20.glGenTextures(1, textures, 0)
                textureId = textures[0]
            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            if (isNewTexture) {
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            }
            if (
                !isNewTexture &&
                bitmap.config == Bitmap.Config.ARGB_8888 &&
                textureConfig == Bitmap.Config.ARGB_8888 &&
                imageWidth == bitmap.width &&
                imageHeight == bitmap.height
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
            }
            imageWidth = bitmap.width
            imageHeight = bitmap.height
            textureConfig = bitmap.config
            updateVertexBuffer()
        }

        private fun updateVertexBuffer() {
            if (imageWidth == 0 || imageHeight == 0 || surfaceWidth == 0 || surfaceHeight == 0) {
                renderWidth = 0
                renderHeight = 0
                texelSizeNeedUpload = true
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
            texelSizeNeedUpload = true

            vertexBuffer.clear()
            vertexBuffer
                .put(-scaleX)
                .put(-scaleY)
                .put(scaleX)
                .put(-scaleY)
                .put(-scaleX)
                .put(scaleY)
                .put(scaleX)
                .put(scaleY)
                .position(0)
        }
    }

    private companion object {
        const val CAMERA_WIDTH = 640
        const val CAMERA_HEIGHT = 480
    }
}
