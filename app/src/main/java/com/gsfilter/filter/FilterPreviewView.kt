package com.gsfilter.filter

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.util.AttributeSet
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FilterPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : GLSurfaceView(context, attrs) {

    private val filterRenderer = FilterRenderer()
    private var lastFilterParams: ShaderFilterParams? = null

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
        queueEvent {
            filterRenderer.setFilterParams(params)
            requestRender()
        }
    }

    private class FilterRenderer : Renderer {

        private val vertexBuffer = floatBufferOf(VERTICES)
        private val textureBuffer = floatBufferOf(TEXTURE_COORDS)

        private var program = 0
        private var handles: ProgramHandles? = null
        private var textureId = 0
        private var pendingBitmap: Bitmap? = null
        private var imageWidth = 0
        private var imageHeight = 0
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var params = ShaderFilterParams.from(FilterRecipe(), Adjustments())

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            handles = resolveProgramHandles(program)
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
            bindAttributes(currentHandles)
            bindUniforms(currentHandles)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)
            GLES20.glDisableVertexAttribArray(currentHandles.position)
            GLES20.glDisableVertexAttribArray(currentHandles.textureCoordinate)
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
                vertexBuffer.clear()
                vertexBuffer.put(VERTICES).position(0)
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

        private fun bindAttributes(handles: ProgramHandles) {
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

        private fun bindUniforms(handles: ProgramHandles) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(handles.texture, 0)
            GLES20.glUniform1f(handles.mono, params.isMonochrome)
            GLES20.glUniform2f(
                handles.texelSize,
                if (imageWidth > 0) 1f / imageWidth else 0f,
                if (imageHeight > 0) 1f / imageHeight else 0f,
            )
            GLES20.glUniform3f(
                handles.rgbShift,
                params.redShift,
                params.greenShift,
                params.blueShift,
            )
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

        private fun resolveProgramHandles(program: Int): ProgramHandles =
            ProgramHandles(
                position = GLES20.glGetAttribLocation(program, A_POSITION),
                textureCoordinate = GLES20.glGetAttribLocation(program, A_TEX_COORD),
                texture = GLES20.glGetUniformLocation(program, U_TEXTURE),
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

        private fun buildProgram(vertexShader: String, fragmentShader: String): Int {
            val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
            val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
            val nextProgram = GLES20.glCreateProgram()
            GLES20.glAttachShader(nextProgram, vertex)
            GLES20.glAttachShader(nextProgram, fragment)
            GLES20.glLinkProgram(nextProgram)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(nextProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
            check(linkStatus[0] == GLES20.GL_TRUE) {
                GLES20.glGetProgramInfoLog(nextProgram)
            }
            return nextProgram
        }

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

        private data class ProgramHandles(
            val position: Int,
            val textureCoordinate: Int,
            val texture: Int,
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
    }

    private companion object {
        const val VERTEX_COUNT = 4
        const val COORDS_PER_VERTEX = 2
        const val A_POSITION = "aPosition"
        const val A_TEX_COORD = "aTexCoord"
        const val U_TEXTURE = "uTexture"
        const val U_MONO = "uMono"
        const val U_TEXEL_SIZE = "uTexelSize"
        const val U_RGB_SHIFT = "uRgbShift"
        const val U_BRIGHTNESS = "uBrightness"
        const val U_EXPOSURE = "uExposure"
        const val U_CONTRAST = "uContrast"
        const val U_HIGHLIGHTS = "uHighlights"
        const val U_SHADOWS = "uShadows"
        const val U_SATURATION = "uSaturation"
        const val U_VIBRANCE = "uVibrance"
        const val U_TEMPERATURE = "uTemperature"
        const val U_TINT = "uTint"
        const val U_SHARPNESS = "uSharpness"
        const val U_CLARITY = "uClarity"
        const val U_FADE = "uFade"
        const val U_VIGNETTE = "uVignette"
        const val U_GRAIN = "uGrain"

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

        const val VERTEX_SHADER =
            """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;

            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
            """

        const val FRAGMENT_SHADER =
            """
            precision mediump float;
            uniform sampler2D uTexture;
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

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                vec3 left = texture2D(uTexture, vTexCoord - vec2(uTexelSize.x, 0.0)).rgb;
                vec3 right = texture2D(uTexture, vTexCoord + vec2(uTexelSize.x, 0.0)).rgb;
                vec3 up = texture2D(uTexture, vTexCoord - vec2(0.0, uTexelSize.y)).rgb;
                vec3 down = texture2D(uTexture, vTexCoord + vec2(0.0, uTexelSize.y)).rgb;
                vec3 blur = (left + right + up + down) * 0.25;
                vec3 rgb = color.rgb + (color.rgb - blur) * ((uSharpness * 0.65) + (uClarity * 0.35));

                rgb = rgb + uRgbShift;
                float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(rgb, vec3(gray), uMono);

                rgb = rgb + uBrightness;
                rgb = rgb * pow(2.0, uExposure);
                gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                float shadowMask = 1.0 - smoothstep(0.0, 0.6, gray);
                float highlightMask = smoothstep(0.4, 1.0, gray);
                rgb = rgb + (shadowMask * uShadows * 0.35);
                rgb = rgb + (highlightMask * uHighlights * 0.35);
                rgb = (rgb - 0.5) * uContrast + 0.5;

                rgb.r = rgb.r + (uTemperature * 0.12) + (uTint * 0.06);
                rgb.g = rgb.g - (uTint * 0.08);
                rgb.b = rgb.b - (uTemperature * 0.12) + (uTint * 0.06);

                gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(vec3(gray), rgb, uSaturation);
                float maxChannel = max(max(rgb.r, rgb.g), rgb.b);
                float average = (rgb.r + rgb.g + rgb.b) / 3.0;
                float vibranceMask = 1.0 - clamp(maxChannel - average, 0.0, 1.0);
                rgb = mix(vec3(gray), rgb, 1.0 + (uVibrance * vibranceMask));

                rgb = mix(rgb, vec3(0.5), clamp(uFade * 0.35, 0.0, 0.35));

                float edgeDistance = distance(vTexCoord, vec2(0.5));
                float edgeMask = smoothstep(0.35, 0.75, edgeDistance);
                rgb = rgb * (1.0 - (uVignette * 0.7 * edgeMask));

                float grain = (random(vTexCoord * vec2(1024.0, 768.0)) - 0.5) * uGrain * 0.16;
                rgb = rgb + grain;

                gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
            }
            """

        fun floatBufferOf(values: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * java.lang.Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values)
                    position(0)
                }
    }
}
