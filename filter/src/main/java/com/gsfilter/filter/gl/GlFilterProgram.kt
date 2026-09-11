package com.gsfilter.filter.gl

import android.opengl.GLES20
import com.gsfilter.filter.ShaderFilterParams
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

internal object GlFilterProgram {

    private val contourUniforms = object : ThreadLocal<ContourUniforms>() {
        override fun initialValue(): ContourUniforms = ContourUniforms()
    }

    private class ContourUniforms {
        val lipPoints = FloatArray(MAX_LIP_POINTS * 2)
        val upperLipPoints = FloatArray(MAX_LIP_POINTS * 2)
        val lowerLipPoints = FloatArray(MAX_LIP_POINTS * 2)
        val leftEyebrowPoints = FloatArray(MAX_CONTOUR_POINTS * 2)
        val rightEyebrowPoints = FloatArray(MAX_CONTOUR_POINTS * 2)

        fun clear() {
            lipPoints.fill(0f)
            upperLipPoints.fill(0f)
            lowerLipPoints.fill(0f)
            leftEyebrowPoints.fill(0f)
            rightEyebrowPoints.fill(0f)
        }
    }

    const val VERTEX_COUNT = 4
    const val COORDS_PER_VERTEX = 2

    val VERTICES = floatArrayOf(
        -1f,
        -1f,
        1f,
        -1f,
        -1f,
        1f,
        1f,
        1f,
    )

    val TEXTURE_COORDS = floatArrayOf(
        0f,
        1f,
        1f,
        1f,
        0f,
        0f,
        1f,
        0f,
    )

    fun buildProgram(): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        check(linkStatus[0] == GLES20.GL_TRUE) {
            GLES20.glGetProgramInfoLog(program)
        }
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        return program
    }

    fun resolveHandles(program: Int): ProgramHandles =
        ProgramHandles(
            position = GLES20.glGetAttribLocation(program, A_POSITION),
            textureCoordinate = GLES20.glGetAttribLocation(program, A_TEX_COORD),
            texture = GLES20.glGetUniformLocation(program, U_TEXTURE),
            lutTexture = GLES20.glGetUniformLocation(program, U_LUT_TEXTURE),
            lutStrength = GLES20.glGetUniformLocation(program, U_LUT_STRENGTH),
            skinSmoothing = GLES20.glGetUniformLocation(program, U_SKIN_SMOOTHING),
            skinWhitening = GLES20.glGetUniformLocation(program, U_SKIN_WHITENING),
            blush = GLES20.glGetUniformLocation(program, U_BLUSH),
            lipstick = GLES20.glGetUniformLocation(program, U_LIPSTICK),
            underEye = GLES20.glGetUniformLocation(program, U_UNDER_EYE),
            teethWhitening = GLES20.glGetUniformLocation(program, U_TEETH_WHITENING),
            eyeShadow = GLES20.glGetUniformLocation(program, U_EYE_SHADOW),
            eyeliner = GLES20.glGetUniformLocation(program, U_EYELINER),
            eyebrow = GLES20.glGetUniformLocation(program, U_EYEBROW),
            faceSlimming = GLES20.glGetUniformLocation(program, U_FACE_SLIMMING),
            eyeEnlargement = GLES20.glGetUniformLocation(program, U_EYE_ENLARGEMENT),
            blushLeft = GLES20.glGetUniformLocation(program, U_BLUSH_LEFT),
            blushRight = GLES20.glGetUniformLocation(program, U_BLUSH_RIGHT),
            blushStrengths = GLES20.glGetUniformLocation(program, U_BLUSH_STRENGTHS),
            faceArea = GLES20.glGetUniformLocation(program, U_FACE_AREA),
            underEyeLeft = GLES20.glGetUniformLocation(program, U_UNDER_EYE_LEFT),
            underEyeRight = GLES20.glGetUniformLocation(program, U_UNDER_EYE_RIGHT),
            eyeLeft = GLES20.glGetUniformLocation(program, U_EYE_LEFT),
            eyeRight = GLES20.glGetUniformLocation(program, U_EYE_RIGHT),
            lipArea = GLES20.glGetUniformLocation(program, U_LIP_AREA),
            lipPoints = GLES20.glGetUniformLocation(program, U_LIP_POINTS),
            lipPointCount = GLES20.glGetUniformLocation(program, U_LIP_POINT_COUNT),
            upperLipPoints = GLES20.glGetUniformLocation(program, U_UPPER_LIP_POINTS),
            upperLipPointCount = GLES20.glGetUniformLocation(program, U_UPPER_LIP_POINT_COUNT),
            lowerLipPoints = GLES20.glGetUniformLocation(program, U_LOWER_LIP_POINTS),
            lowerLipPointCount = GLES20.glGetUniformLocation(program, U_LOWER_LIP_POINT_COUNT),
            leftEyebrowPoints = GLES20.glGetUniformLocation(program, U_LEFT_EYEBROW_POINTS),
            leftEyebrowPointCount = GLES20.glGetUniformLocation(program, U_LEFT_EYEBROW_POINT_COUNT),
            rightEyebrowPoints = GLES20.glGetUniformLocation(program, U_RIGHT_EYEBROW_POINTS),
            rightEyebrowPointCount = GLES20.glGetUniformLocation(program, U_RIGHT_EYEBROW_POINT_COUNT),
            makeupRotation = GLES20.glGetUniformLocation(program, U_MAKEUP_ROTATION),
            effect = GLES20.glGetUniformLocation(program, U_EFFECT),
            effectStrength = GLES20.glGetUniformLocation(program, U_EFFECT_STRENGTH),
            effectThreshold = GLES20.glGetUniformLocation(program, U_EFFECT_THRESHOLD),
            effectTone = GLES20.glGetUniformLocation(program, U_EFFECT_TONE),
            intensity = GLES20.glGetUniformLocation(program, U_INTENSITY),
            mono = GLES20.glGetUniformLocation(program, U_MONO),
            texelSize = GLES20.glGetUniformLocation(program, U_TEXEL_SIZE),
            rgbShift = GLES20.glGetUniformLocation(program, U_RGB_SHIFT),
            brightness = GLES20.glGetUniformLocation(program, U_BRIGHTNESS),
            exposure = GLES20.glGetUniformLocation(program, U_EXPOSURE),
            contrast = GLES20.glGetUniformLocation(program, U_CONTRAST),
            highlights = GLES20.glGetUniformLocation(program, U_HIGHLIGHTS),
            shadows = GLES20.glGetUniformLocation(program, U_SHADOWS),
            saturation = GLES20.glGetUniformLocation(program, U_SATURATION),
            vibrance = GLES20.glGetUniformLocation(program, U_VIBRANCE),
            temperature = GLES20.glGetUniformLocation(program, U_TEMPERATURE),
            tint = GLES20.glGetUniformLocation(program, U_TINT),
            sharpness = GLES20.glGetUniformLocation(program, U_SHARPNESS),
            clarity = GLES20.glGetUniformLocation(program, U_CLARITY),
            fade = GLES20.glGetUniformLocation(program, U_FADE),
            vignette = GLES20.glGetUniformLocation(program, U_VIGNETTE),
            grain = GLES20.glGetUniformLocation(program, U_GRAIN),
        )

    fun bindAttributes(
        handles: ProgramHandles,
        vertexBuffer: FloatBuffer,
        textureBuffer: FloatBuffer,
    ) {
        GLES20.glEnableVertexAttribArray(handles.position)
        GLES20.glVertexAttribPointer(handles.position, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(handles.textureCoordinate)
        GLES20.glVertexAttribPointer(
            handles.textureCoordinate,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            0,
            textureBuffer,
        )
    }

    fun bindUniforms(
        handles: ProgramHandles,
        textureId: Int,
        lutTextureId: Int = 0,
        renderWidth: Int,
        renderHeight: Int,
        params: ShaderFilterParams,
        texelScale: Float = 1f,
        uploadMakeupUniforms: Boolean = true,
        uploadAdjustmentUniforms: Boolean = true,
        bindTextureSampler: Boolean = true,
    ) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        if (bindTextureSampler) {
            GLES20.glUniform1i(handles.texture, 0)
        }
        val lutStrength = if (lutTextureId != 0) params.lutStrength else 0f
        if (lutStrength > 0f) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTextureId)
        }
        GLES20.glUniform1f(handles.lutStrength, lutStrength)
        if (uploadMakeupUniforms) {
            GLES20.glUniform1f(handles.skinSmoothing, params.skinSmoothing)
            GLES20.glUniform1f(handles.skinWhitening, params.skinWhitening)
            GLES20.glUniform1f(handles.blush, params.blush)
            GLES20.glUniform1f(handles.lipstick, params.lipstick)
            GLES20.glUniform1f(handles.underEye, params.underEye)
            GLES20.glUniform1f(handles.teethWhitening, params.teethWhitening)
            GLES20.glUniform1f(handles.eyeShadow, params.eyeShadow)
            GLES20.glUniform1f(handles.eyeliner, params.eyeliner)
            GLES20.glUniform1f(handles.eyebrow, params.eyebrow)
            GLES20.glUniform1f(handles.faceSlimming, params.faceSlimming)
            GLES20.glUniform1f(handles.eyeEnlargement, params.eyeEnlargement)
            GLES20.glUniform1f(handles.makeupRotation, params.makeupFeatures?.rotationRadians ?: 0f)
            val makeupGeometryEnabled = hasMakeupControls(params)
            if (makeupGeometryEnabled) {
                params.makeupFeatures?.let { features ->
            val uniforms = requireNotNull(contourUniforms.get())
            val sine = sin(features.rotationRadians)
            val cosine = cos(features.rotationRadians)
            GLES20.glUniform4f(
                handles.underEyeLeft,
                features.leftEyeCenterX + (sine * features.leftEyeRadiusY * 1.55f),
                features.leftEyeCenterY + (cosine * features.leftEyeRadiusY * 1.55f),
                features.leftEyeRadiusX * 1.25f,
                features.leftEyeRadiusY * 0.80f,
            )
            GLES20.glUniform4f(
                handles.underEyeRight,
                features.rightEyeCenterX + (sine * features.rightEyeRadiusY * 1.55f),
                features.rightEyeCenterY + (cosine * features.rightEyeRadiusY * 1.55f),
                features.rightEyeRadiusX * 1.25f,
                features.rightEyeRadiusY * 0.80f,
            )
            GLES20.glUniform4f(
                handles.eyeLeft,
                features.leftEyeCenterX,
                features.leftEyeCenterY,
                features.leftEyeRadiusX,
                features.leftEyeRadiusY,
            )
            GLES20.glUniform4f(
                handles.eyeRight,
                features.rightEyeCenterX,
                features.rightEyeCenterY,
                features.rightEyeRadiusX,
                features.rightEyeRadiusY,
            )
            GLES20.glUniform4f(
                handles.blushLeft,
                features.leftCheekX,
                features.leftCheekY,
                features.cheekRadiusX,
                features.cheekRadiusY,
            )
            GLES20.glUniform4f(
                handles.blushRight,
                features.rightCheekX,
                features.rightCheekY,
                features.cheekRadiusX,
                features.cheekRadiusY,
            )
            GLES20.glUniform2f(
                handles.blushStrengths,
                features.leftCheekStrength,
                features.rightCheekStrength,
            )
            GLES20.glUniform4f(
                handles.faceArea,
                features.faceCenterX,
                features.faceCenterY,
                features.faceRadiusX,
                features.faceRadiusY,
            )
            GLES20.glUniform4f(
                handles.lipArea,
                features.lipCenterX,
                features.lipCenterY,
                features.lipRadiusX,
                features.lipRadiusY,
            )
            val lipPointValues = uniforms.lipPoints
            val lipPointCount = features.lipContour.size.coerceAtMost(MAX_LIP_POINTS)
            repeat(lipPointCount) { index ->
                val point = features.lipContour[index]
                lipPointValues[index * 2] = point.x
                lipPointValues[(index * 2) + 1] = point.y
            }
            GLES20.glUniform2fv(handles.lipPoints, MAX_LIP_POINTS, lipPointValues, 0)
            GLES20.glUniform1i(handles.lipPointCount, lipPointCount)
            val upperLipPointValues = uniforms.upperLipPoints
            val upperLipPointCount = features.upperLipContour.size.coerceAtMost(MAX_LIP_POINTS)
            repeat(upperLipPointCount) { index ->
                val point = features.upperLipContour[index]
                upperLipPointValues[index * 2] = point.x
                upperLipPointValues[(index * 2) + 1] = point.y
            }
            GLES20.glUniform2fv(handles.upperLipPoints, MAX_LIP_POINTS, upperLipPointValues, 0)
            GLES20.glUniform1i(handles.upperLipPointCount, upperLipPointCount)
            val lowerLipPointValues = uniforms.lowerLipPoints
            val lowerLipPointCount = features.lowerLipContour.size.coerceAtMost(MAX_LIP_POINTS)
            repeat(lowerLipPointCount) { index ->
                val point = features.lowerLipContour[index]
                lowerLipPointValues[index * 2] = point.x
                lowerLipPointValues[(index * 2) + 1] = point.y
            }
            GLES20.glUniform2fv(handles.lowerLipPoints, MAX_LIP_POINTS, lowerLipPointValues, 0)
            GLES20.glUniform1i(handles.lowerLipPointCount, lowerLipPointCount)
            val leftEyebrowPointValues = uniforms.leftEyebrowPoints
            val leftEyebrowPointCount = features.leftEyebrowContour.size.coerceAtMost(MAX_CONTOUR_POINTS)
            repeat(leftEyebrowPointCount) { index ->
                val point = features.leftEyebrowContour[index]
                leftEyebrowPointValues[index * 2] = point.x
                leftEyebrowPointValues[(index * 2) + 1] = point.y
            }
            GLES20.glUniform2fv(handles.leftEyebrowPoints, MAX_CONTOUR_POINTS, leftEyebrowPointValues, 0)
            GLES20.glUniform1i(handles.leftEyebrowPointCount, leftEyebrowPointCount)
            val rightEyebrowPointValues = uniforms.rightEyebrowPoints
            val rightEyebrowPointCount = features.rightEyebrowContour.size.coerceAtMost(MAX_CONTOUR_POINTS)
            repeat(rightEyebrowPointCount) { index ->
                val point = features.rightEyebrowContour[index]
                rightEyebrowPointValues[index * 2] = point.x
                rightEyebrowPointValues[(index * 2) + 1] = point.y
            }
            GLES20.glUniform2fv(handles.rightEyebrowPoints, MAX_CONTOUR_POINTS, rightEyebrowPointValues, 0)
            GLES20.glUniform1i(handles.rightEyebrowPointCount, rightEyebrowPointCount)
                } ?: run {
                    val uniforms = requireNotNull(contourUniforms.get())
                    uniforms.clear()
                    GLES20.glUniform4f(handles.blushLeft, 0f, 0f, 0f, 0f)
                    GLES20.glUniform4f(handles.blushRight, 0f, 0f, 0f, 0f)
                    GLES20.glUniform2f(handles.blushStrengths, 0f, 0f)
                    GLES20.glUniform4f(handles.faceArea, 0f, 0f, 0f, 0f)
                    GLES20.glUniform4f(handles.underEyeLeft, 0f, 0f, 0f, 0f)
                    GLES20.glUniform4f(handles.underEyeRight, 0f, 0f, 0f, 0f)
                    GLES20.glUniform4f(handles.eyeLeft, 0f, 0f, 0f, 0f)
                    GLES20.glUniform4f(handles.eyeRight, 0f, 0f, 0f, 0f)
                    GLES20.glUniform4f(handles.lipArea, 0f, 0f, 0f, 0f)
                    GLES20.glUniform2fv(handles.lipPoints, MAX_LIP_POINTS, uniforms.lipPoints, 0)
                    GLES20.glUniform1i(handles.lipPointCount, 0)
                    GLES20.glUniform2fv(handles.upperLipPoints, MAX_LIP_POINTS, uniforms.upperLipPoints, 0)
                    GLES20.glUniform1i(handles.upperLipPointCount, 0)
                    GLES20.glUniform2fv(handles.lowerLipPoints, MAX_LIP_POINTS, uniforms.lowerLipPoints, 0)
                    GLES20.glUniform1i(handles.lowerLipPointCount, 0)
                    GLES20.glUniform2fv(handles.leftEyebrowPoints, MAX_CONTOUR_POINTS, uniforms.leftEyebrowPoints, 0)
                    GLES20.glUniform1i(handles.leftEyebrowPointCount, 0)
                    GLES20.glUniform2fv(handles.rightEyebrowPoints, MAX_CONTOUR_POINTS, uniforms.rightEyebrowPoints, 0)
                    GLES20.glUniform1i(handles.rightEyebrowPointCount, 0)
                }
            }
        }
        GLES20.glUniform1f(handles.effect, params.effect.shaderValue)
        GLES20.glUniform1f(handles.effectStrength, params.effectStrength)
        GLES20.glUniform1f(handles.effectThreshold, params.effectThreshold)
        GLES20.glUniform1f(handles.effectTone, params.effectTone)
        GLES20.glUniform1f(handles.intensity, params.intensity)
        GLES20.glUniform1f(handles.mono, params.isMonochrome)
        GLES20.glUniform2f(
            handles.texelSize,
            if (renderWidth > 0) texelScale / renderWidth else 0f,
            if (renderHeight > 0) texelScale / renderHeight else 0f,
        )
        if (uploadAdjustmentUniforms) {
            GLES20.glUniform3f(handles.rgbShift, params.redShift, params.greenShift, params.blueShift)
            GLES20.glUniform1f(handles.brightness, params.brightness)
            GLES20.glUniform1f(handles.exposure, params.exposure)
            GLES20.glUniform1f(handles.contrast, params.contrast)
            GLES20.glUniform1f(handles.highlights, params.highlights)
            GLES20.glUniform1f(handles.shadows, params.shadows)
            GLES20.glUniform1f(handles.saturation, params.saturation)
            GLES20.glUniform1f(handles.vibrance, params.vibrance)
            GLES20.glUniform1f(handles.temperature, params.temperature)
            GLES20.glUniform1f(handles.tint, params.tint)
            GLES20.glUniform1f(handles.sharpness, params.sharpness)
            GLES20.glUniform1f(handles.clarity, params.clarity)
            GLES20.glUniform1f(handles.fade, params.fade)
            GLES20.glUniform1f(handles.vignette, params.vignette)
            GLES20.glUniform1f(handles.grain, params.grain)
        }
    }

    fun disableAttributes(handles: ProgramHandles) {
        GLES20.glDisableVertexAttribArray(handles.position)
        GLES20.glDisableVertexAttribArray(handles.textureCoordinate)
    }

    fun hasMakeupControls(params: ShaderFilterParams): Boolean =
        params.skinSmoothing != 0f ||
            params.skinWhitening != 0f ||
            params.blush != 0f ||
            params.lipstick != 0f ||
            params.underEye != 0f ||
            params.teethWhitening != 0f ||
            params.eyeShadow != 0f ||
            params.eyeliner != 0f ||
            params.eyebrow != 0f ||
            params.faceSlimming != 0f ||
            params.eyeEnlargement != 0f

    fun hasAdjustmentValues(params: ShaderFilterParams): Boolean =
        params.redShift != 0f ||
            params.greenShift != 0f ||
            params.blueShift != 0f ||
            params.brightness != 0f ||
            params.exposure != 0f ||
            params.contrast != 1f ||
            params.highlights != 0f ||
            params.shadows != 0f ||
            params.saturation != 1f ||
            params.vibrance != 0f ||
            params.temperature != 0f ||
            params.tint != 0f ||
            params.sharpness != 0f ||
            params.clarity != 0f ||
            params.fade != 0f ||
            params.vignette != 0f ||
            params.grain != 0f

    fun floatBufferOf(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * java.lang.Float.BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }

    data class ProgramHandles(
        val position: Int,
        val textureCoordinate: Int,
        val texture: Int,
        val lutTexture: Int,
        val lutStrength: Int,
        val skinSmoothing: Int,
        val skinWhitening: Int,
        val blush: Int,
        val lipstick: Int,
        val underEye: Int,
        val teethWhitening: Int,
        val eyeShadow: Int,
        val eyeliner: Int,
        val eyebrow: Int,
        val faceSlimming: Int,
        val eyeEnlargement: Int,
        val blushLeft: Int,
        val blushRight: Int,
        val blushStrengths: Int,
        val faceArea: Int,
        val underEyeLeft: Int,
        val underEyeRight: Int,
        val eyeLeft: Int,
        val eyeRight: Int,
        val lipArea: Int,
        val lipPoints: Int,
        val lipPointCount: Int,
        val upperLipPoints: Int,
        val upperLipPointCount: Int,
        val lowerLipPoints: Int,
        val lowerLipPointCount: Int,
        val leftEyebrowPoints: Int,
        val leftEyebrowPointCount: Int,
        val rightEyebrowPoints: Int,
        val rightEyebrowPointCount: Int,
        val makeupRotation: Int,
        val effect: Int,
        val effectStrength: Int,
        val effectThreshold: Int,
        val effectTone: Int,
        val intensity: Int,
        val mono: Int,
        val texelSize: Int,
        val rgbShift: Int,
        val brightness: Int,
        val exposure: Int,
        val contrast: Int,
        val highlights: Int,
        val shadows: Int,
        val saturation: Int,
        val vibrance: Int,
        val temperature: Int,
        val tint: Int,
        val sharpness: Int,
        val clarity: Int,
        val fade: Int,
        val vignette: Int,
        val grain: Int,
    )

    private fun compileShader(type: Int, shader: String): Int {
        val handle = GLES20.glCreateShader(type)
        GLES20.glShaderSource(handle, shader)
        GLES20.glCompileShader(handle)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(handle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        check(compileStatus[0] == GLES20.GL_TRUE) {
            GLES20.glGetShaderInfoLog(handle)
        }
        return handle
    }

    private const val A_POSITION = "aPosition"
    private const val A_TEX_COORD = "aTexCoord"
    private const val U_TEXTURE = "uTexture"
    private const val U_LUT_TEXTURE = "uLutTexture"
    private const val U_LUT_STRENGTH = "uLutStrength"
    private const val U_SKIN_SMOOTHING = "uSkinSmoothing"
    private const val U_SKIN_WHITENING = "uSkinWhitening"
    private const val U_BLUSH = "uBlush"
    private const val U_LIPSTICK = "uLipstick"
    private const val U_UNDER_EYE = "uUnderEye"
    private const val U_TEETH_WHITENING = "uTeethWhitening"
    private const val U_EYE_SHADOW = "uEyeShadow"
    private const val U_EYELINER = "uEyeliner"
    private const val U_EYEBROW = "uEyebrow"
    private const val U_FACE_SLIMMING = "uFaceSlimming"
    private const val U_EYE_ENLARGEMENT = "uEyeEnlargement"
    private const val U_BLUSH_LEFT = "uBlushLeft"
    private const val U_BLUSH_RIGHT = "uBlushRight"
    private const val U_BLUSH_STRENGTHS = "uBlushStrengths"
    private const val U_FACE_AREA = "uFaceArea"
    private const val U_UNDER_EYE_LEFT = "uUnderEyeLeft"
    private const val U_UNDER_EYE_RIGHT = "uUnderEyeRight"
    private const val U_EYE_LEFT = "uEyeLeft"
    private const val U_EYE_RIGHT = "uEyeRight"
    private const val U_LIP_AREA = "uLipArea"
    private const val U_LIP_POINTS = "uLipPoints[0]"
    private const val U_LIP_POINT_COUNT = "uLipPointCount"
    private const val U_UPPER_LIP_POINTS = "uUpperLipPoints[0]"
    private const val U_UPPER_LIP_POINT_COUNT = "uUpperLipPointCount"
    private const val U_LOWER_LIP_POINTS = "uLowerLipPoints[0]"
    private const val U_LOWER_LIP_POINT_COUNT = "uLowerLipPointCount"
    private const val U_LEFT_EYEBROW_POINTS = "uLeftEyebrowPoints[0]"
    private const val U_LEFT_EYEBROW_POINT_COUNT = "uLeftEyebrowPointCount"
    private const val U_RIGHT_EYEBROW_POINTS = "uRightEyebrowPoints[0]"
    private const val U_RIGHT_EYEBROW_POINT_COUNT = "uRightEyebrowPointCount"
    private const val U_MAKEUP_ROTATION = "uMakeupRotation"
    private const val MAX_LIP_POINTS = 32
    private const val MAX_CONTOUR_POINTS = 32
    private const val U_EFFECT = "uEffect"
    private const val U_EFFECT_STRENGTH = "uEffectStrength"
    private const val U_EFFECT_THRESHOLD = "uEffectThreshold"
    private const val U_EFFECT_TONE = "uEffectTone"
    private const val U_INTENSITY = "uIntensity"
    private const val U_MONO = "uMono"
    private const val U_TEXEL_SIZE = "uTexelSize"
    private const val U_RGB_SHIFT = "uRgbShift"
    private const val U_BRIGHTNESS = "uBrightness"
    private const val U_EXPOSURE = "uExposure"
    private const val U_CONTRAST = "uContrast"
    private const val U_HIGHLIGHTS = "uHighlights"
    private const val U_SHADOWS = "uShadows"
    private const val U_SATURATION = "uSaturation"
    private const val U_VIBRANCE = "uVibrance"
    private const val U_TEMPERATURE = "uTemperature"
    private const val U_TINT = "uTint"
    private const val U_SHARPNESS = "uSharpness"
    private const val U_CLARITY = "uClarity"
    private const val U_FADE = "uFade"
    private const val U_VIGNETTE = "uVignette"
    private const val U_GRAIN = "uGrain"

    private const val VERTEX_SHADER =
        """
        attribute vec4 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;

        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
        """

    private const val FRAGMENT_SHADER =
        """
        precision mediump float;
        uniform sampler2D uTexture;
        uniform sampler2D uLutTexture;
        uniform float uLutStrength;
        uniform float uSkinSmoothing;
        uniform float uSkinWhitening;
        uniform float uBlush;
        uniform float uLipstick;
        uniform float uUnderEye;
        uniform float uTeethWhitening;
        uniform float uEyeShadow;
        uniform float uEyeliner;
        uniform float uEyebrow;
        uniform float uFaceSlimming;
        uniform float uEyeEnlargement;
        uniform vec4 uBlushLeft;
        uniform vec4 uBlushRight;
        uniform vec2 uBlushStrengths;
        uniform vec4 uFaceArea;
        uniform vec4 uUnderEyeLeft;
        uniform vec4 uUnderEyeRight;
        uniform vec4 uEyeLeft;
        uniform vec4 uEyeRight;
        uniform vec4 uLipArea;
        uniform vec2 uLipPoints[32];
        uniform int uLipPointCount;
        uniform vec2 uUpperLipPoints[32];
        uniform int uUpperLipPointCount;
        uniform vec2 uLowerLipPoints[32];
        uniform int uLowerLipPointCount;
        uniform vec2 uLeftEyebrowPoints[32];
        uniform int uLeftEyebrowPointCount;
        uniform vec2 uRightEyebrowPoints[32];
        uniform int uRightEyebrowPointCount;
        uniform float uMakeupRotation;
        uniform float uEffect;
        uniform float uEffectStrength;
        uniform float uEffectThreshold;
        uniform float uEffectTone;
        uniform float uIntensity;
        uniform float uMono;
        uniform vec2 uTexelSize;
        uniform vec3 uRgbShift;
        uniform float uBrightness;
        uniform float uExposure;
        uniform float uContrast;
        uniform float uHighlights;
        uniform float uShadows;
        uniform float uSaturation;
        uniform float uVibrance;
        uniform float uTemperature;
        uniform float uTint;
        uniform float uSharpness;
        uniform float uClarity;
        uniform float uFade;
        uniform float uVignette;
        uniform float uGrain;
        varying vec2 vTexCoord;

        float random(vec2 point) {
            return fract(sin(dot(point, vec2(12.9898, 78.233))) * 43758.5453);
        }

        float lumaAt(vec2 coord) {
            vec3 rgb = texture2D(uTexture, coord).rgb;
            return dot(rgb, vec3(0.299, 0.587, 0.114));
        }

        float edgeAt(vec2 coord) {
            vec2 texel = uTexelSize;
            float topLeft = lumaAt(coord + texel * vec2(-1.0, -1.0));
            float top = lumaAt(coord + texel * vec2(0.0, -1.0));
            float topRight = lumaAt(coord + texel * vec2(1.0, -1.0));
            float left = lumaAt(coord + texel * vec2(-1.0, 0.0));
            float right = lumaAt(coord + texel * vec2(1.0, 0.0));
            float bottomLeft = lumaAt(coord + texel * vec2(-1.0, 1.0));
            float bottom = lumaAt(coord + texel * vec2(0.0, 1.0));
            float bottomRight = lumaAt(coord + texel * vec2(1.0, 1.0));
            float horizontal = -topLeft - (2.0 * left) - bottomLeft + topRight + (2.0 * right) + bottomRight;
            float vertical = -topLeft - (2.0 * top) - topRight + bottomLeft + (2.0 * bottom) + bottomRight;
            return clamp(length(vec2(horizontal, vertical)), 0.0, 1.0);
        }

        float lineFromEdge(float edge, float softness) {
            float threshold = mix(0.04, 0.34, uEffectThreshold);
            return smoothstep(threshold - softness, threshold + softness, edge) * uEffectStrength;
        }

        float skinMask(vec3 rgb) {
            float cb = 0.5 - (0.168736 * rgb.r) - (0.331264 * rgb.g) + (0.5 * rgb.b);
            float cr = 0.5 + (0.5 * rgb.r) - (0.418688 * rgb.g) - (0.081312 * rgb.b);
            float cbMask = 1.0 - smoothstep(0.08, 0.18, abs(cb - 0.40));
            float crMask = 1.0 - smoothstep(0.05, 0.25, abs(cr - 0.55));
            float redBias = smoothstep(0.01, 0.14, rgb.r - ((rgb.g + rgb.b) * 0.5));
            return clamp(cbMask * crMask * redBias, 0.0, 1.0);
        }

        float ellipseMask(vec2 coord, vec4 area, float rotation, float innerEdge, float outerEdge) {
            vec2 radius = max(area.zw, vec2(0.0001));
            vec2 delta = (coord - area.xy) / radius;
            float sine = sin(rotation);
            float cosine = cos(rotation);
            vec2 rotated = vec2(
                (delta.x * cosine) + (delta.y * sine),
                (-delta.x * sine) + (delta.y * cosine)
            );
            return 1.0 - smoothstep(innerEdge, outerEdge, length(rotated));
        }

        float faceAreaMask(vec2 coord) {
            if (uFaceArea.z <= 0.0 || uFaceArea.w <= 0.0) {
                return 1.0;
            }
            return ellipseMask(coord, uFaceArea, uMakeupRotation, 0.55, 1.05);
        }

        float underEyeRegionMask(vec2 coord, vec4 area) {
            if (area.z <= 0.0 || area.w <= 0.0) {
                return 0.0;
            }
            return ellipseMask(coord, area, uMakeupRotation, 0.35, 1.15);
        }

        float eyeShadowRegionMask(vec2 coord, vec4 area) {
            if (area.z <= 0.0 || area.w <= 0.0) {
                return 0.0;
            }
            vec2 radius = max(area.zw * vec2(1.55, 1.35), vec2(0.0001));
            vec2 center = area.xy - vec2(
                sin(uMakeupRotation) * area.w * 0.28,
                cos(uMakeupRotation) * area.w * 0.28
            );
            vec2 delta = (coord - center) / radius;
            float sine = sin(uMakeupRotation);
            float cosine = cos(uMakeupRotation);
            vec2 rotated = vec2(
                (delta.x * cosine) + (delta.y * sine),
                (-delta.x * sine) + (delta.y * cosine)
            );
            float ellipse = 1.0 - smoothstep(0.38, 1.05, length(rotated));
            float upperLid = 1.0 - smoothstep(-0.10, 0.48, rotated.y);
            return ellipse * upperLid;
        }

        float eyelinerRegionMask(vec2 coord, vec4 area) {
            if (area.z <= 0.0 || area.w <= 0.0) {
                return 0.0;
            }
            vec2 radius = max(area.zw * vec2(1.14, 1.12), vec2(0.0001));
            vec2 delta = (coord - area.xy) / radius;
            float sine = sin(uMakeupRotation);
            float cosine = cos(uMakeupRotation);
            vec2 rotated = vec2(
                (delta.x * cosine) + (delta.y * sine),
                (-delta.x * sine) + (delta.y * cosine)
            );
            float distanceFromCenter = length(rotated);
            float band = smoothstep(0.70, 0.86, distanceFromCenter) *
                (1.0 - smoothstep(0.88, 1.08, distanceFromCenter));
            float upperLid = 1.0 - smoothstep(-0.12, 0.30, rotated.y);
            return band * upperLid;
        }

        vec2 eyeWarp(vec2 coord, vec4 area, float amount) {
            if (amount <= 0.0 || area.z <= 0.0 || area.w <= 0.0) {
                return coord;
            }
            vec2 delta = coord - area.xy;
            float sine = sin(uMakeupRotation);
            float cosine = cos(uMakeupRotation);
            vec2 rotated = vec2(
                (delta.x * cosine) + (delta.y * sine),
                (-delta.x * sine) + (delta.y * cosine)
            );
            vec2 normalized = rotated / max(area.zw * vec2(1.55, 1.55), vec2(0.0001));
            float distanceFromCenter = length(normalized);
            float falloff = 1.0 - smoothstep(0.25, 1.05, distanceFromCenter);
            float scale = 1.0 - (amount * 0.18 * falloff);
            rotated *= scale;
            return area.xy + vec2(
                (rotated.x * cosine) - (rotated.y * sine),
                (rotated.x * sine) + (rotated.y * cosine)
            );
        }

        vec2 warpCoordinate(vec2 coord) {
            if (uFaceSlimming <= 0.0 && uEyeEnlargement <= 0.0) {
                return coord;
            }
            vec2 warped = coord;
            if (uFaceSlimming > 0.0 && uFaceArea.z > 0.0 && uFaceArea.w > 0.0) {
                vec2 delta = coord - uFaceArea.xy;
                float sine = sin(uMakeupRotation);
                float cosine = cos(uMakeupRotation);
                vec2 rotated = vec2(
                    (delta.x * cosine) + (delta.y * sine),
                    (-delta.x * sine) + (delta.y * cosine)
                );
                vec2 normalized = rotated / uFaceArea.zw;
                float distanceFromCenter = length(normalized);
                float falloff = 1.0 - smoothstep(0.25, 1.05, distanceFromCenter);
                rotated.x *= 1.0 + (uFaceSlimming * 0.18 * falloff);
                warped = uFaceArea.xy + vec2(
                    (rotated.x * cosine) - (rotated.y * sine),
                    (rotated.x * sine) + (rotated.y * cosine)
                );
            }
            warped = eyeWarp(warped, uEyeLeft, uEyeEnlargement);
            warped = eyeWarp(warped, uEyeRight, uEyeEnlargement);
            return clamp(warped, vec2(0.0), vec2(1.0));
        }

        float pointSegmentDistance(vec2 point, vec2 start, vec2 end) {
            vec2 segment = end - start;
            float lengthSquared = max(dot(segment, segment), 0.000001);
            float projection = clamp(dot(point - start, segment) / lengthSquared, 0.0, 1.0);
            return distance(point, start + (segment * projection));
        }

        float lipColorMask(vec3 rgb) {
            float saturation = max(max(rgb.r, rgb.g), rgb.b) - min(min(rgb.r, rgb.g), rgb.b);
            float redness = rgb.r - ((rgb.g + rgb.b) * 0.5);
            return smoothstep(0.20, 0.34, saturation) * smoothstep(0.16, 0.26, redness);
        }

        float teethColorMask(vec3 rgb) {
            float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
            float saturation = max(max(rgb.r, rgb.g), rgb.b) - min(min(rgb.r, rgb.g), rgb.b);
            return smoothstep(0.55, 0.72, luma) * (1.0 - smoothstep(0.10, 0.22, saturation));
        }

        float contourMask(vec2 coord, vec2 points[32], int pointCount) {
            if (pointCount < 3) {
                return 0.0;
            }
            bool inside = false;
            float edgeDistance = 1.0;
            for (int index = 0; index < 32; index++) {
                if (index >= pointCount) {
                    break;
                }
                int nextIndex = index + 1;
                if (nextIndex >= pointCount) {
                    nextIndex = 0;
                }
                vec2 start = points[index];
                vec2 end = points[nextIndex];
                edgeDistance = min(edgeDistance, pointSegmentDistance(coord, start, end));
                if ((start.y > coord.y) != (end.y > coord.y)) {
                    float intersectionX = start.x +
                        ((coord.y - start.y) * (end.x - start.x) / (end.y - start.y));
                    if (coord.x < intersectionX) {
                        inside = !inside;
                    }
                }
            }
            return (inside && edgeDistance > 0.004) ? 1.0 : 0.0;
        }

        float lipContourMask(vec2 coord) {
            if (uUpperLipPointCount >= 3 || uLowerLipPointCount >= 3) {
                float mask = 0.0;
                if (uUpperLipPointCount >= 3) {
                    bool upperInside = false;
                    for (int index = 0; index < 32; index++) {
                        if (index >= uUpperLipPointCount) {
                            break;
                        }
                        int nextIndex = index + 1;
                        if (nextIndex >= uUpperLipPointCount) {
                            nextIndex = 0;
                        }
                        vec2 start = uUpperLipPoints[index];
                        vec2 end = uUpperLipPoints[nextIndex];
                        if ((start.y > coord.y) != (end.y > coord.y)) {
                            float intersectionX = start.x +
                                ((coord.y - start.y) * (end.x - start.x) / (end.y - start.y));
                            if (coord.x < intersectionX) {
                                upperInside = !upperInside;
                            }
                        }
                    }
                    if (upperInside) {
                        mask = 1.0;
                    }
                }
                if (uLowerLipPointCount >= 3) {
                    bool lowerInside = false;
                    for (int index = 0; index < 32; index++) {
                        if (index >= uLowerLipPointCount) {
                            break;
                        }
                        int nextIndex = index + 1;
                        if (nextIndex >= uLowerLipPointCount) {
                            nextIndex = 0;
                        }
                        vec2 start = uLowerLipPoints[index];
                        vec2 end = uLowerLipPoints[nextIndex];
                        if ((start.y > coord.y) != (end.y > coord.y)) {
                            float intersectionX = start.x +
                                ((coord.y - start.y) * (end.x - start.x) / (end.y - start.y));
                            if (coord.x < intersectionX) {
                                lowerInside = !lowerInside;
                            }
                        }
                    }
                    if (lowerInside) {
                        mask = 1.0;
                    }
                }
                return mask;
            }
            if (uLipPointCount < 3) {
                return ellipseMask(coord, uLipArea, uMakeupRotation, 0.75, 1.05);
            }

            bool inside = false;
            float edgeDistance = 1.0;
            for (int index = 0; index < 32; index++) {
                if (index >= uLipPointCount) {
                    break;
                }
                int nextIndex = index + 1;
                if (nextIndex >= uLipPointCount) {
                    nextIndex = 0;
                }
                vec2 start = uLipPoints[index];
                vec2 end = uLipPoints[nextIndex];
                edgeDistance = min(edgeDistance, pointSegmentDistance(coord, start, end));
                if ((start.y > coord.y) != (end.y > coord.y)) {
                    float intersectionX = start.x +
                        ((coord.y - start.y) * (end.x - start.x) / (end.y - start.y));
                    if (coord.x < intersectionX) {
                        inside = !inside;
                    }
                }
            }
            return (inside && edgeDistance > 0.004) ? 1.0 : 0.0;
        }

        float stripe(float value) {
            return 1.0 - smoothstep(0.0, 0.055, abs(fract(value) - 0.5));
        }

        vec3 sampleLut(vec3 rgb) {
            vec3 color = clamp(rgb, 0.0, 1.0);
            float blue = color.b * 32.0;
            float sliceLow = floor(blue);
            float sliceHigh = min(sliceLow + 1.0, 32.0);
            float mixAmount = blue - sliceLow;
            vec2 lowCoord = vec2(
                ((sliceLow * 33.0) + (color.r * 32.0) + 0.5) / 1089.0,
                ((color.g * 32.0) + 0.5) / 33.0
            );
            vec2 highCoord = vec2(
                ((sliceHigh * 33.0) + (color.r * 32.0) + 0.5) / 1089.0,
                lowCoord.y
            );
            return mix(texture2D(uLutTexture, lowCoord).rgb, texture2D(uLutTexture, highCoord).rgb, mixAmount);
        }

        void main() {
            vec2 sourceCoord = warpCoordinate(vTexCoord);
            vec4 color = texture2D(uTexture, sourceCoord);
            vec3 blur = color.rgb;
            if (uSkinSmoothing != 0.0 || uSharpness != 0.0 || uClarity != 0.0) {
                vec3 left = texture2D(uTexture, sourceCoord - vec2(uTexelSize.x, 0.0)).rgb;
                vec3 right = texture2D(uTexture, sourceCoord + vec2(uTexelSize.x, 0.0)).rgb;
                vec3 up = texture2D(uTexture, sourceCoord - vec2(0.0, uTexelSize.y)).rgb;
                vec3 down = texture2D(uTexture, sourceCoord + vec2(0.0, uTexelSize.y)).rgb;
                blur = (left + right + up + down) * 0.25;
            }
            bool beautyEnabled = uSkinSmoothing > 0.0 ||
                uSkinWhitening > 0.0 ||
                uBlush > 0.0 ||
                uLipstick > 0.0 ||
                uUnderEye > 0.0 ||
                uTeethWhitening > 0.0 ||
                uEyeShadow > 0.0 ||
                uEyeliner > 0.0 ||
                uEyebrow > 0.0;
            float beautyMask = 0.0;
            vec3 rgb = color.rgb;
            if (beautyEnabled) {
                beautyMask = skinMask(color.rgb) * faceAreaMask(vTexCoord);
                vec3 localContrast = abs(color.rgb - blur);
                float edgeGuard = 1.0 - smoothstep(
                    0.06,
                    0.20,
                    max(max(localContrast.r, localContrast.g), localContrast.b)
                );
                float beautyEdge = 0.0;
                if (uSkinSmoothing > 0.0) {
                    beautyEdge = edgeAt(sourceCoord);
                }
                float smoothAmount = uSkinSmoothing * beautyMask * edgeGuard *
                    (1.0 - smoothstep(0.18, 0.55, beautyEdge));
                rgb = mix(color.rgb, blur, smoothAmount);
            }
            rgb = rgb + (rgb - blur) * ((uSharpness * 0.65) + (uClarity * 0.35));
            if (beautyEnabled) {
            float whitening = uSkinWhitening * beautyMask;
            float skinLuma = dot(rgb, vec3(0.299, 0.587, 0.114));
            float highlightGuard = 1.0 - smoothstep(0.55, 0.92, skinLuma);
            float skinLift = whitening * (1.0 - skinLuma) * 0.10 * highlightGuard;
            rgb += vec3(skinLift);
            float skinDesaturate = whitening * 0.08 * highlightGuard;
            rgb = mix(rgb, vec3(skinLuma + skinLift), skinDesaturate);
            float underEyeMask = max(
                underEyeRegionMask(vTexCoord, uUnderEyeLeft),
                underEyeRegionMask(vTexCoord, uUnderEyeRight)
            );
            float underEyeAmount = uUnderEye * underEyeMask * beautyMask;
            float underEyeLuma = dot(rgb, vec3(0.299, 0.587, 0.114));
            float underEyeLift = underEyeAmount * (1.0 - underEyeLuma) * 0.08;
            rgb += vec3(underEyeLift);
            float teethRegionMask = ellipseMask(
                vTexCoord,
                vec4(uLipArea.xy, uLipArea.z * 1.08, uLipArea.w * 0.65),
                uMakeupRotation,
                0.15,
                1.05
            );
            float teethAmount = uTeethWhitening * teethRegionMask * faceAreaMask(vTexCoord) *
                teethColorMask(color.rgb) * 0.65;
            float teethLuma = dot(rgb, vec3(0.299, 0.587, 0.114));
            float teethLift = teethAmount * (1.0 - teethLuma) * 0.22;
            rgb += vec3(teethLift);
            float eyeShadowMask = max(
                eyeShadowRegionMask(vTexCoord, uEyeLeft),
                eyeShadowRegionMask(vTexCoord, uEyeRight)
            );
            float eyeShadowAmount = uEyeShadow * eyeShadowMask * faceAreaMask(vTexCoord) * 0.28;
            rgb = mix(rgb, vec3(0.34, 0.16, 0.22), eyeShadowAmount);
            float eyelinerMask = max(
                eyelinerRegionMask(vTexCoord, uEyeLeft),
                eyelinerRegionMask(vTexCoord, uEyeRight)
            );
            float eyelinerAmount = uEyeliner * eyelinerMask * faceAreaMask(vTexCoord) * 0.48;
            rgb = mix(rgb, vec3(0.06, 0.04, 0.05), eyelinerAmount);
            float eyebrowMask = max(
                contourMask(vTexCoord, uLeftEyebrowPoints, uLeftEyebrowPointCount),
                contourMask(vTexCoord, uRightEyebrowPoints, uRightEyebrowPointCount)
            );
            float eyebrowAmount = uEyebrow * eyebrowMask * faceAreaMask(vTexCoord) * 0.32;
            rgb = mix(rgb, rgb * vec3(0.42, 0.32, 0.28), eyebrowAmount);
            float blushMask = max(
                ellipseMask(vTexCoord, uBlushLeft, uMakeupRotation, 0.35, 1.15) * uBlushStrengths.x,
                ellipseMask(vTexCoord, uBlushRight, uMakeupRotation, 0.35, 1.15) * uBlushStrengths.y
            );
            float blushAmount = uBlush * blushMask * skinMask(rgb) * 0.40;
            float blushLuma = dot(rgb, vec3(0.299, 0.587, 0.114));
            rgb = mix(
                rgb,
                clamp(vec3(blushLuma + 0.20, blushLuma - 0.05, blushLuma - 0.02), 0.0, 1.0),
                blushAmount
            );
            float lipstickAmount = uLipstick * lipContourMask(vTexCoord) * lipColorMask(color.rgb) * 0.40;
            rgb = mix(rgb, vec3(0.70, 0.16, 0.22), lipstickAmount);
            }

            if (uRgbShift.r != 0.0 || uRgbShift.g != 0.0 || uRgbShift.b != 0.0) {
                rgb = rgb + uRgbShift;
            }
            if (uMono != 0.0) {
                float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(rgb, vec3(gray), uMono);
            }
            if (uBrightness != 0.0) {
                rgb = rgb + uBrightness;
            }
            if (uExposure != 0.0) {
                rgb = rgb * pow(2.0, uExposure);
            }
            if (uShadows != 0.0 || uHighlights != 0.0) {
                float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                float shadowMask = 1.0 - smoothstep(0.0, 0.6, gray);
                float highlightMask = smoothstep(0.4, 1.0, gray);
                rgb = rgb + (shadowMask * uShadows * 0.35);
                rgb = rgb + (highlightMask * uHighlights * 0.35);
            }
            if (uContrast != 1.0) {
                rgb = (rgb - 0.5) * uContrast + 0.5;
            }
            if (uTemperature != 0.0 || uTint != 0.0) {
                rgb.r = rgb.r + (uTemperature * 0.12) + (uTint * 0.06);
                rgb.g = rgb.g - (uTint * 0.08);
                rgb.b = rgb.b - (uTemperature * 0.12) + (uTint * 0.06);
            }
            if (uSaturation != 1.0 || uVibrance != 0.0) {
                float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                if (uSaturation != 1.0) {
                    rgb = mix(vec3(gray), rgb, uSaturation);
                }
                if (uVibrance != 0.0) {
                    float maxChannel = max(max(rgb.r, rgb.g), rgb.b);
                    float average = (rgb.r + rgb.g + rgb.b) / 3.0;
                    float vibranceMask = 1.0 - clamp(maxChannel - average, 0.0, 1.0);
                    rgb = mix(vec3(gray), rgb, 1.0 + (uVibrance * vibranceMask));
                }
            }

            if (uLutStrength > 0.0) {
                rgb = mix(rgb, sampleLut(rgb), uLutStrength);
            }

            if (uEffect > 0.5) {
                vec3 beforeEffect = rgb;
                float sourceGray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                float edge = edgeAt(vTexCoord);
                if (uEffect > 5.5) {
                    float dark = 1.0 - sourceGray;
                    float hatch = stripe((vTexCoord.x + vTexCoord.y) * 34.0) * step(0.18, dark);
                    hatch += stripe((vTexCoord.x - vTexCoord.y) * 38.0) * step(0.42, dark);
                    hatch += stripe(vTexCoord.x * 46.0) * step(0.68, dark);
                    float line = lineFromEdge(edge, 0.10) * 0.45;
                    rgb = vec3(1.0 - clamp(((hatch * 0.55) + line) * uEffectStrength, 0.0, 0.95));
                } else if (uEffect > 4.5) {
                    float line = lineFromEdge(edge, 0.14);
                    float texture = (random(vTexCoord * vec2(680.0, 920.0)) - 0.5) * 0.28 * uEffectStrength;
                    float charcoal = clamp(mix(0.92, sourceGray, 0.65 + (uEffectTone * 0.2)) - (line * 0.95) - texture, 0.0, 1.0);
                    rgb = vec3(charcoal);
                } else if (uEffect > 3.5) {
                    float line = lineFromEdge(edge, 0.11);
                    vec3 paper = mix(vec3(1.0), color.rgb, 0.35 + (uEffectTone * 0.5));
                    rgb = clamp(paper - (line * 0.58), 0.0, 1.0);
                } else if (uEffect > 2.5) {
                    float line = lineFromEdge(edge, 0.10);
                    float paper = mix(1.0, sourceGray, 0.35 + (uEffectTone * 0.35));
                    rgb = vec3(clamp(paper - (line * 0.92), 0.0, 1.0));
                } else if (uEffect > 1.5) {
                    float line = lineFromEdge(edge, 0.08);
                    rgb = vec3(1.0 - line);
                } else {
                    float line = lineFromEdge(edge, 0.12);
                    float paper = mix(1.0, sourceGray, uEffectTone);
                    rgb = vec3(clamp(paper - (line * 0.8), 0.0, 1.0));
                }
                rgb = mix(beforeEffect, rgb, uIntensity);
            }

            if (uFade != 0.0) {
                rgb = mix(rgb, vec3(0.5), clamp(uFade * 0.35, 0.0, 0.35));
            }

            if (uVignette != 0.0) {
                float edgeDistance = distance(vTexCoord, vec2(0.5));
                float edgeMask = smoothstep(0.35, 0.75, edgeDistance);
                rgb = rgb * (1.0 - (uVignette * 0.7 * edgeMask));
            }

            if (uGrain != 0.0) {
                float grain = (random(vTexCoord * vec2(1024.0, 768.0)) - 0.5) * uGrain * 0.16;
                rgb = rgb + grain;
            }

            gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
        }
        """
}
