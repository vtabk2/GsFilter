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
        queueEvent {
            filterRenderer.setFilterParams(params)
            requestRender()
        }
    }

    private class FilterRenderer : Renderer {

        private val vertexBuffer = floatBufferOf(VERTICES)
        private val textureBuffer = floatBufferOf(TEXTURE_COORDS)

        private var program = 0
        private var textureId = 0
        private var pendingBitmap: Bitmap? = null
        private var imageWidth = 0
        private var imageHeight = 0
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var params = ShaderFilterParams.from(FilterRecipe(), Adjustments())

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
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

            GLES20.glUseProgram(program)
            bindAttributes()
            bindUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)
            GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(program, A_POSITION))
            GLES20.glDisableVertexAttribArray(GLES20.glGetAttribLocation(program, A_TEX_COORD))
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

        private fun bindAttributes() {
            val position = GLES20.glGetAttribLocation(program, A_POSITION)
            val textureCoordinate = GLES20.glGetAttribLocation(program, A_TEX_COORD)
            GLES20.glEnableVertexAttribArray(position)
            GLES20.glVertexAttribPointer(position, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glEnableVertexAttribArray(textureCoordinate)
            GLES20.glVertexAttribPointer(
                textureCoordinate,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                0,
                textureBuffer,
            )
        }

        private fun bindUniforms() {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, U_TEXTURE), 0)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, U_MONO), params.isMonochrome)
            GLES20.glUniform3f(
                GLES20.glGetUniformLocation(program, U_RGB_SHIFT),
                params.redShift,
                params.greenShift,
                params.blueShift,
            )
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, U_BRIGHTNESS), params.brightness)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, U_CONTRAST), params.contrast)
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, U_SATURATION), params.saturation)
        }

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
    }

    private companion object {
        const val VERTEX_COUNT = 4
        const val COORDS_PER_VERTEX = 2
        const val A_POSITION = "aPosition"
        const val A_TEX_COORD = "aTexCoord"
        const val U_TEXTURE = "uTexture"
        const val U_MONO = "uMono"
        const val U_RGB_SHIFT = "uRgbShift"
        const val U_BRIGHTNESS = "uBrightness"
        const val U_CONTRAST = "uContrast"
        const val U_SATURATION = "uSaturation"

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
            uniform vec3 uRgbShift;
            uniform float uBrightness;
            uniform float uContrast;
            uniform float uSaturation;
            varying vec2 vTexCoord;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                vec3 rgb = color.rgb + uRgbShift;
                float gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(rgb, vec3(gray), uMono);
                rgb = rgb + uBrightness;
                rgb = (rgb - 0.5) * uContrast + 0.5;
                gray = dot(rgb, vec3(0.299, 0.587, 0.114));
                rgb = mix(vec3(gray), rgb, uSaturation);
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
