package com.gsfilter.filter.gl

import android.opengl.GLES20
import com.gsfilter.filter.ShaderFilterParams
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal object GlFilterProgram {

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
        renderWidth: Int,
        renderHeight: Int,
        params: ShaderFilterParams,
        texelScale: Float = 1f,
    ) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(handles.texture, 0)
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

    fun disableAttributes(handles: ProgramHandles) {
        GLES20.glDisableVertexAttribArray(handles.position)
        GLES20.glDisableVertexAttribArray(handles.textureCoordinate)
    }

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
            vec3 rgb = texture2D(uTexture, clamp(coord, vec2(0.0), vec2(1.0))).rgb;
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

        float stripe(float value) {
            return 1.0 - smoothstep(0.0, 0.055, abs(fract(value) - 0.5));
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

            rgb = mix(rgb, vec3(0.5), clamp(uFade * 0.35, 0.0, 0.35));

            float edgeDistance = distance(vTexCoord, vec2(0.5));
            float edgeMask = smoothstep(0.35, 0.75, edgeDistance);
            rgb = rgb * (1.0 - (uVignette * 0.7 * edgeMask));

            float grain = (random(vTexCoord * vec2(1024.0, 768.0)) - 0.5) * uGrain * 0.16;
            rgb = rgb + grain;

            gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
        }
        """
}
