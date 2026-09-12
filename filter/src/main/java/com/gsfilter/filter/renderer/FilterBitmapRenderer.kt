package com.gsfilter.filter.renderer

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.gsfilter.filter.Adjustments
import com.gsfilter.filter.FilterEffect
import com.gsfilter.filter.FilterRecipe
import com.gsfilter.filter.MakeupFeatures
import com.gsfilter.filter.NormalizedPoint
import com.gsfilter.filter.ShaderFilterParams
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object FilterBitmapRenderer {

    fun getBitmap(
        source: Bitmap,
        recipe: FilterRecipe,
        adjustments: Adjustments = Adjustments(),
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        makeupFeatures: MakeupFeatures? = null,
    ): Bitmap =
        getBitmapWithParams(
            source = source,
            params = ShaderFilterParams.from(recipe, adjustments, makeupFeatures),
            maxWidth = maxWidth,
            maxHeight = maxHeight,
        )

    internal fun getBitmapWithParams(
        source: Bitmap,
        params: ShaderFilterParams,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        renderSize: RenderSize? = null,
        noOp: Boolean = isNoOp(params),
    ): Bitmap {
        val renderSource = scaledSource(
            source = source,
            size = renderSize ?: targetSize(source.width, source.height, maxWidth, maxHeight),
        )
        val width = renderSource.width
        val height = renderSource.height
        if (noOp) {
            return try {
                requireNotNull(renderSource.copy(Bitmap.Config.ARGB_8888, false))
            } finally {
                recycleIfTemporary(renderSource, source)
            }
        }
        val pixels = IntArray(width * height)

        return try {
            renderSource.getPixels(pixels, 0, width, 0, 0, width, height)
            val output = renderPixels(
                pixels = pixels,
                width = width,
                height = height,
                params = params,
            )
            Bitmap.createBitmap(output, width, height, Bitmap.Config.ARGB_8888)
        } finally {
            recycleIfTemporary(renderSource, source)
        }
    }

    internal fun targetSize(
        width: Int,
        height: Int,
        maxWidth: Int?,
        maxHeight: Int?,
    ): RenderSize {
        require(width > 0 && height > 0) { "Source size must be positive." }
        require(maxWidth == null || maxWidth > 0) { "maxWidth must be positive." }
        require(maxHeight == null || maxHeight > 0) { "maxHeight must be positive." }

        val widthScale = maxWidth?.let { it.toFloat() / width } ?: 1f
        val heightScale = maxHeight?.let { it.toFloat() / height } ?: 1f
        val scale = min(1f, min(widthScale, heightScale))
        return RenderSize(
            width = (width * scale).roundToInt().coerceAtLeast(1),
            height = (height * scale).roundToInt().coerceAtLeast(1),
        )
    }

    internal fun scaledSource(source: Bitmap, maxWidth: Int?, maxHeight: Int?): Bitmap {
        return scaledSource(source, targetSize(source.width, source.height, maxWidth, maxHeight))
    }

    internal fun scaledSource(source: Bitmap, size: RenderSize): Bitmap {
        if (size.width == source.width && size.height == source.height) {
            return source
        }
        return source.scale(size.width, size.height)
    }

    internal fun recycleIfTemporary(renderSource: Bitmap, originalSource: Bitmap) {
        if (renderSource !== originalSource) {
            renderSource.recycle()
        }
    }

    internal fun renderPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
    ): IntArray {
        require(pixels.size == width * height) { "Pixel array size must match width * height." }
        if (isNoOp(params)) {
            return pixels.copyOf()
        }

        val output = IntArray(pixels.size)
        val lutOutput = FloatArray(3)
        val warpCoordinate = FloatArray(2)
        val texelX = 1f / width
        val texelY = 1f / height
        val exposure = 2.0.pow(params.exposure.toDouble()).toFloat()
        val hasWarp = hasActiveWarp(params)
        val sharpAmount = (params.sharpness * 0.65f) + (params.clarity * 0.35f)
        val needsNeighborhood = params.skinSmoothing != 0f || sharpAmount != 0f
        val needsEdge = params.skinSmoothing != 0f ||
            (params.effect != FilterEffect.Color &&
                params.intensity != 0f &&
                params.effectStrength != 0f)
        val hasFeatureBeauty = hasFeatureBeautyControls(params)
        val needsFaceMask = needsFaceMask(params)
        val needsBeautyMask = needsBeautyMask(params)
        val hasBeauty = params.skinSmoothing != 0f ||
            params.skinWhitening != 0f ||
            hasFeatureBeauty
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                output[index] = filterPixel(
                    pixels,
                    x,
                    y,
                    width,
                    height,
                    params,
                    lutOutput,
                    warpCoordinate,
                    texelX,
                    texelY,
                    exposure,
                    hasWarp,
                    sharpAmount,
                    needsNeighborhood,
                    needsEdge,
                    hasBeauty,
                    hasFeatureBeauty,
                    needsFaceMask,
                    needsBeautyMask,
                )
            }
        }
        return output
    }

    internal fun filterPixel(
        pixels: IntArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
    ): Int {
        val sharpAmount = (params.sharpness * 0.65f) + (params.clarity * 0.35f)
        return filterPixel(
            pixels,
            x,
            y,
            width,
            height,
            params,
            FloatArray(3),
            FloatArray(2),
            1f / width,
            1f / height,
            2.0.pow(params.exposure.toDouble()).toFloat(),
            hasActiveWarp(params),
            sharpAmount,
            params.skinSmoothing != 0f || sharpAmount != 0f,
            params.skinSmoothing != 0f ||
                (params.effect != FilterEffect.Color &&
                    params.intensity != 0f &&
                    params.effectStrength != 0f),
            hasBeautyControls(params),
            hasFeatureBeautyControls(params),
            needsFaceMask(params),
            needsBeautyMask(params),
        )
    }

    private fun filterPixel(
        pixels: IntArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        params: ShaderFilterParams,
        lutOutput: FloatArray,
        warpCoordinate: FloatArray,
        texelX: Float,
        texelY: Float,
        exposure: Float,
        hasWarp: Boolean,
        sharpAmount: Float,
        needsNeighborhood: Boolean,
        needsEdge: Boolean,
        hasBeauty: Boolean,
        hasFeatureBeauty: Boolean,
        needsFaceMask: Boolean,
        needsBeautyMask: Boolean,
    ): Int {
        val textureX = (x + 0.5f) / width
        val textureY = (y + 0.5f) / height
        if (hasWarp) {
            warpSourceCoordinate(textureX, textureY, params, warpCoordinate)
        } else {
            warpCoordinate[0] = textureX
            warpCoordinate[1] = textureY
        }
        val sourceX = warpCoordinate[0]
        val sourceY = warpCoordinate[1]
        val pixelIndex = y * width + x
        val color = if (hasWarp) {
            sampleBilinear(pixels, sourceX, sourceY, width, height)
        } else {
            pixels[pixelIndex]
        }

        val sourceRed = red(color)
        val sourceGreen = green(color)
        val sourceBlue = blue(color)
        var red = sourceRed
        var green = sourceGreen
        var blue = sourceBlue
        val left: Int
        val right: Int
        val up: Int
        val down: Int
        if (needsNeighborhood) {
            if (hasWarp) {
                left = sampleBilinear(pixels, sourceX - texelX, sourceY, width, height)
                right = sampleBilinear(pixels, sourceX + texelX, sourceY, width, height)
                up = sampleBilinear(pixels, sourceX, sourceY - texelY, width, height)
                down = sampleBilinear(pixels, sourceX, sourceY + texelY, width, height)
            } else {
                val rowStart = y * width
                left = pixels[rowStart + (x - 1).coerceAtLeast(0)]
                right = pixels[rowStart + (x + 1).coerceAtMost(width - 1)]
                up = pixels[((y - 1).coerceAtLeast(0) * width) + x]
                down = pixels[((y + 1).coerceAtMost(height - 1) * width) + x]
            }
        } else {
            left = color
            right = color
            up = color
            down = color
        }

        val blurredRed: Float
        val blurredGreen: Float
        val blurredBlue: Float
        if (needsNeighborhood) {
            blurredRed = average(red(left), red(right), red(up), red(down))
            blurredGreen = average(green(left), green(right), green(up), green(down))
            blurredBlue = average(blue(left), blue(right), blue(up), blue(down))
        } else {
            blurredRed = sourceRed
            blurredGreen = sourceGreen
            blurredBlue = sourceBlue
        }
        val edge = if (needsEdge) {
            edgeAt(pixels, x, y, width, height)
        } else {
            0f
        }
        val faceMask: Float
        val beautyMask: Float
        if (hasBeauty) {
            faceMask = if (needsFaceMask) {
                params.makeupFeatures?.let { features ->
                    if (features.faceRadiusX > 0f && features.faceRadiusY > 0f) {
                        ellipseMask(
                            textureX,
                            textureY,
                            features.faceCenterX,
                            features.faceCenterY,
                            features.faceRadiusX,
                            features.faceRadiusY,
                            features.rotationRadians,
                            innerEdge = 0.55f,
                            outerEdge = 1.05f,
                        )
                    } else {
                        1f
                    }
                } ?: 1f
            } else {
                1f
            }
            beautyMask = if (needsBeautyMask) {
                skinMask(sourceRed, sourceGreen, sourceBlue) * faceMask
            } else {
                0f
            }
            val beautySmoothAmount = if (params.skinSmoothing != 0f) {
                val localContrast = maxOf(
                    abs(sourceRed - blurredRed),
                    abs(sourceGreen - blurredGreen),
                    abs(sourceBlue - blurredBlue),
                )
                val edgeGuard = 1f - smoothstep(0.06f, 0.20f, localContrast)
                params.skinSmoothing * beautyMask * edgeGuard *
                    (1f - smoothstep(0.18f, 0.55f, edge))
            } else {
                0f
            }
            red = mix(red, blurredRed, beautySmoothAmount)
            green = mix(green, blurredGreen, beautySmoothAmount)
            blue = mix(blue, blurredBlue, beautySmoothAmount)
            if (params.skinWhitening != 0f) {
                val beautyWhiteAmount = params.skinWhitening * beautyMask
                val skinLuma = gray(red, green, blue)
                val highlightGuard = 1f - smoothstep(0.55f, 0.92f, skinLuma)
                val skinLift = beautyWhiteAmount * (1f - skinLuma) * 0.10f * highlightGuard
                red += skinLift
                green += skinLift
                blue += skinLift
                val skinDesaturate = beautyWhiteAmount * 0.08f * highlightGuard
                val liftedLuma = skinLuma + skinLift
                red = mix(red, liftedLuma, skinDesaturate)
                green = mix(green, liftedLuma, skinDesaturate)
                blue = mix(blue, liftedLuma, skinDesaturate)
            }
        } else {
            faceMask = 1f
            beautyMask = 0f
        }
        params.makeupFeatures?.takeIf { hasFeatureBeauty }?.let { features ->
            if (params.underEye != 0f) {
                val underEyeMask = max(
                    underEyeMask(
                        textureX,
                        textureY,
                        features.leftEyeCenterX,
                        features.leftEyeCenterY,
                        features.leftEyeRadiusX,
                        features.leftEyeRadiusY,
                        features.rotationRadians,
                    ),
                    underEyeMask(
                        textureX,
                        textureY,
                        features.rightEyeCenterX,
                        features.rightEyeCenterY,
                        features.rightEyeRadiusX,
                        features.rightEyeRadiusY,
                        features.rotationRadians,
                    ),
                )
                val underEyeAmount = params.underEye * underEyeMask * beautyMask
                val underEyeLuma = gray(red, green, blue)
                val underEyeLift = underEyeAmount * (1f - underEyeLuma) * 0.08f
                red += underEyeLift
                green += underEyeLift
                blue += underEyeLift
            }
            if (params.teethWhitening != 0f) {
                val teethRegionMask = ellipseMask(
                    textureX,
                    textureY,
                    features.lipCenterX,
                    features.lipCenterY,
                    features.lipRadiusX * 1.08f,
                    features.lipRadiusY * 0.65f,
                    features.rotationRadians,
                    innerEdge = 0.15f,
                    outerEdge = 1.05f,
                )
                val teethAmount = params.teethWhitening * teethRegionMask * faceMask *
                    teethColorMask(red, green, blue) * 0.65f
                val teethLuma = gray(red, green, blue)
                val teethLift = teethAmount * (1f - teethLuma) * 0.22f
                red += teethLift
                green += teethLift
                blue += teethLift
            }
            if (params.eyeShadow != 0f) {
                val eyeShadowMask = max(
                    eyeShadowMask(
                        textureX,
                        textureY,
                        features.leftEyeCenterX,
                        features.leftEyeCenterY,
                        features.leftEyeRadiusX,
                        features.leftEyeRadiusY,
                        features.rotationRadians,
                    ),
                    eyeShadowMask(
                        textureX,
                        textureY,
                        features.rightEyeCenterX,
                        features.rightEyeCenterY,
                        features.rightEyeRadiusX,
                        features.rightEyeRadiusY,
                        features.rotationRadians,
                    ),
                )
                val eyeShadowAmount = params.eyeShadow * eyeShadowMask * faceMask * 0.28f
                red = mix(red, 0.34f, eyeShadowAmount)
                green = mix(green, 0.16f, eyeShadowAmount)
                blue = mix(blue, 0.22f, eyeShadowAmount)
            }
            if (params.eyeliner != 0f) {
                val eyelinerMask = max(
                    eyelinerMask(
                        textureX,
                        textureY,
                        features.leftEyeCenterX,
                        features.leftEyeCenterY,
                        features.leftEyeRadiusX,
                        features.leftEyeRadiusY,
                        features.rotationRadians,
                    ),
                    eyelinerMask(
                        textureX,
                        textureY,
                        features.rightEyeCenterX,
                        features.rightEyeCenterY,
                        features.rightEyeRadiusX,
                        features.rightEyeRadiusY,
                        features.rotationRadians,
                    ),
                )
                val eyelinerAmount = params.eyeliner * eyelinerMask * faceMask * 0.48f
                red = mix(red, 0.06f, eyelinerAmount)
                green = mix(green, 0.04f, eyelinerAmount)
                blue = mix(blue, 0.05f, eyelinerAmount)
            }
            if (params.eyebrow != 0f) {
                val eyebrowMask = max(
                    polygonMask(textureX, textureY, features.leftEyebrowContour),
                    polygonMask(textureX, textureY, features.rightEyebrowContour),
                )
                val eyebrowAmount = params.eyebrow * eyebrowMask * faceMask * 0.32f
                red = mix(red, red * 0.42f, eyebrowAmount)
                green = mix(green, green * 0.32f, eyebrowAmount)
                blue = mix(blue, blue * 0.28f, eyebrowAmount)
            }
            if (params.blush != 0f) {
                val blushMask = max(
                    ellipseMask(
                        textureX,
                        textureY,
                        features.leftCheekX,
                        features.leftCheekY,
                        features.cheekRadiusX,
                        features.cheekRadiusY,
                        features.rotationRadians,
                        innerEdge = 0.35f,
                        outerEdge = 1.15f,
                    ) * features.leftCheekStrength,
                    ellipseMask(
                        textureX,
                        textureY,
                        features.rightCheekX,
                        features.rightCheekY,
                        features.cheekRadiusX,
                        features.cheekRadiusY,
                        features.rotationRadians,
                        innerEdge = 0.35f,
                        outerEdge = 1.15f,
                    ) * features.rightCheekStrength,
                )
                val blushAmount = params.blush * blushMask * skinMask(red, green, blue) * 0.40f
                val blushLuma = gray(red, green, blue)
                red = mix(red, clamp(blushLuma + 0.20f, 0f, 1f), blushAmount)
                green = mix(green, clamp(blushLuma - 0.05f, 0f, 1f), blushAmount)
                blue = mix(blue, clamp(blushLuma - 0.02f, 0f, 1f), blushAmount)
            }
            if (params.lipstick != 0f) {
                val lipstickMask = when {
                    features.upperLipContour.size >= 3 || features.lowerLipContour.size >= 3 -> max(
                        if (features.upperLipContour.size >= 3) {
                            polygonMask(textureX, textureY, features.upperLipContour)
                        } else {
                            0f
                        },
                        if (features.lowerLipContour.size >= 3) {
                            polygonMask(textureX, textureY, features.lowerLipContour)
                        } else {
                            0f
                        },
                    )
                    features.lipContour.size >= 3 -> polygonMask(textureX, textureY, features.lipContour)
                    else -> ellipseMask(
                        textureX,
                        textureY,
                        features.lipCenterX,
                        features.lipCenterY,
                        features.lipRadiusX,
                        features.lipRadiusY,
                        features.rotationRadians,
                        innerEdge = 0.75f,
                        outerEdge = 1.05f,
                    )
                }
                val lipstickAmount = params.lipstick * lipstickMask * lipColorMask(red, green, blue) * 0.40f
                red = mix(red, 0.70f, lipstickAmount)
                green = mix(green, 0.16f, lipstickAmount)
                blue = mix(blue, 0.22f, lipstickAmount)
            }
        }

        if (sharpAmount != 0f) {
            red += (red - blurredRed) * sharpAmount
            green += (green - blurredGreen) * sharpAmount
            blue += (blue - blurredBlue) * sharpAmount
        }

        if (params.redShift != 0f || params.greenShift != 0f || params.blueShift != 0f) {
            red += params.redShift
            green += params.greenShift
            blue += params.blueShift
        }

        if (params.isMonochrome != 0f) {
            val gray = gray(red, green, blue)
            red = mix(red, gray, params.isMonochrome)
            green = mix(green, gray, params.isMonochrome)
            blue = mix(blue, gray, params.isMonochrome)
        }

        if (params.brightness != 0f) {
            red += params.brightness
            green += params.brightness
            blue += params.brightness
        }

        if (exposure != 1f) {
            red *= exposure
            green *= exposure
            blue *= exposure
        }

        if (params.shadows != 0f || params.highlights != 0f) {
            val gray = gray(red, green, blue)
            val shadowMask = 1f - smoothstep(0f, 0.6f, gray)
            val highlightMask = smoothstep(0.4f, 1f, gray)
            red += shadowMask * params.shadows * 0.35f
            green += shadowMask * params.shadows * 0.35f
            blue += shadowMask * params.shadows * 0.35f
            red += highlightMask * params.highlights * 0.35f
            green += highlightMask * params.highlights * 0.35f
            blue += highlightMask * params.highlights * 0.35f
        }

        if (params.contrast != 1f) {
            red = ((red - 0.5f) * params.contrast) + 0.5f
            green = ((green - 0.5f) * params.contrast) + 0.5f
            blue = ((blue - 0.5f) * params.contrast) + 0.5f
        }

        if (params.temperature != 0f || params.tint != 0f) {
            red += (params.temperature * 0.12f) + (params.tint * 0.06f)
            green -= params.tint * 0.08f
            blue += (-params.temperature * 0.12f) + (params.tint * 0.06f)
        }

        if (params.saturation != 1f || params.vibrance != 0f) {
            val gray = gray(red, green, blue)
            if (params.saturation != 1f) {
                red = mix(gray, red, params.saturation)
                green = mix(gray, green, params.saturation)
                blue = mix(gray, blue, params.saturation)
            }
            if (params.vibrance != 0f) {
                val maxChannel = max(max(red, green), blue)
                val channelAverage = (red + green + blue) / 3f
                val vibranceMask = 1f - clamp(maxChannel - channelAverage, 0f, 1f)
                val vibranceAmount = 1f + (params.vibrance * vibranceMask)
                red = mix(gray, red, vibranceAmount)
                green = mix(gray, green, vibranceAmount)
                blue = mix(gray, blue, vibranceAmount)
            }
        }

        if (params.lutStrength > 0f) {
            params.lut.apply(red, green, blue, lutOutput)
            red = mix(red, lutOutput[0], params.lutStrength)
            green = mix(green, lutOutput[1], params.lutStrength)
            blue = mix(blue, lutOutput[2], params.lutStrength)
        }

        if (params.effect != FilterEffect.Color && params.intensity != 0f) {
            val beforeEffectRed = red
            val beforeEffectGreen = green
            val beforeEffectBlue = blue
            when (params.effect) {
                FilterEffect.Color -> Unit
                FilterEffect.Sketch -> {
                    val sourceGray = gray(sourceRed, sourceGreen, sourceBlue)
                    val line = lineFromEdge(edge, params, 0.12f)
                    val sketch = clamp(mix(1f, sourceGray, params.effectTone) - (line * 0.8f), 0f, 1f)
                    red = sketch
                    green = sketch
                    blue = sketch
                }

                FilterEffect.Ink -> {
                    val line = lineFromEdge(edge, params, 0.08f)
                    val ink = 1f - line
                    red = ink
                    green = ink
                    blue = ink
                }

                FilterEffect.Pencil -> {
                    val sourceGray = gray(sourceRed, sourceGreen, sourceBlue)
                    val line = lineFromEdge(edge, params, 0.10f)
                    val paper = mix(1f, sourceGray, 0.35f + (params.effectTone * 0.35f))
                    val pencil = clamp(paper - (line * 0.92f), 0f, 1f)
                    red = pencil
                    green = pencil
                    blue = pencil
                }

                FilterEffect.ColorPencil -> {
                    val line = lineFromEdge(edge, params, 0.11f)
                    red = clamp(mix(1f, sourceRed, 0.35f + (params.effectTone * 0.5f)) - (line * 0.58f), 0f, 1f)
                    green = clamp(mix(1f, sourceGreen, 0.35f + (params.effectTone * 0.5f)) - (line * 0.58f), 0f, 1f)
                    blue = clamp(mix(1f, sourceBlue, 0.35f + (params.effectTone * 0.5f)) - (line * 0.58f), 0f, 1f)
                }

                FilterEffect.Charcoal -> {
                    val sourceGray = gray(sourceRed, sourceGreen, sourceBlue)
                    val line = lineFromEdge(edge, params, 0.14f)
                    val texture = (random(textureX * 680f, textureY * 920f) - 0.5f) * 0.28f * params.effectStrength
                    val charcoal = clamp(
                        mix(0.92f, sourceGray, 0.65f + (params.effectTone * 0.2f)) - (line * 0.95f) - texture,
                        0f,
                        1f,
                    )
                    red = charcoal
                    green = charcoal
                    blue = charcoal
                }

                FilterEffect.CrossHatch -> {
                    val sourceGray = gray(sourceRed, sourceGreen, sourceBlue)
                    val dark = 1f - sourceGray
                    var hatch = stripe((textureX + textureY) * 34f) * if (dark >= 0.18f) 1f else 0f
                    hatch += stripe((textureX - textureY) * 38f) * if (dark >= 0.42f) 1f else 0f
                    hatch += stripe(textureX * 46f) * if (dark >= 0.68f) 1f else 0f
                    val line = lineFromEdge(edge, params, 0.10f) * 0.45f
                    val crossHatch = 1f - clamp(((hatch * 0.55f) + line) * params.effectStrength, 0f, 0.95f)
                    red = crossHatch
                    green = crossHatch
                    blue = crossHatch
                }
            }
            red = mix(beforeEffectRed, red, params.intensity)
            green = mix(beforeEffectGreen, green, params.intensity)
            blue = mix(beforeEffectBlue, blue, params.intensity)
        }

        if (params.fade != 0f) {
            val fade = clamp(params.fade * 0.35f, 0f, 0.35f)
            red = mix(red, 0.5f, fade)
            green = mix(green, 0.5f, fade)
            blue = mix(blue, 0.5f, fade)
        }

        if (params.vignette != 0f) {
            val edgeDistance = sqrt(
                ((textureX - 0.5f) * (textureX - 0.5f)) +
                    ((textureY - 0.5f) * (textureY - 0.5f)),
            )
            val edgeMask = smoothstep(0.35f, 0.75f, edgeDistance)
            val vignette = 1f - (params.vignette * 0.7f * edgeMask)
            red *= vignette
            green *= vignette
            blue *= vignette
        }

        if (params.grain != 0f) {
            val grain = (random(textureX * 1024f, textureY * 768f) - 0.5f) * params.grain * 0.16f
            red += grain
            green += grain
            blue += grain
        }

        return argb(alpha(color), red, green, blue)
    }

    private fun alpha(color: Int): Int = color ushr 24

    private fun sampleBilinear(
        pixels: IntArray,
        x: Float,
        y: Float,
        width: Int,
        height: Int,
    ): Int {
        val pixelX = (x * width - 0.5f).coerceIn(0f, (width - 1).toFloat())
        val pixelY = (y * height - 0.5f).coerceIn(0f, (height - 1).toFloat())
        val left = floor(pixelX).toInt()
        val top = floor(pixelY).toInt()
        val right = (left + 1).coerceAtMost(width - 1)
        val bottom = (top + 1).coerceAtMost(height - 1)
        val horizontal = pixelX - left
        val vertical = pixelY - top
        val topLeft = pixels[top * width + left]
        val topRight = pixels[top * width + right]
        val bottomLeft = pixels[bottom * width + left]
        val bottomRight = pixels[bottom * width + right]

        return argb(
            alpha = blendChannel(
                alpha(topLeft).toFloat(),
                alpha(topRight).toFloat(),
                alpha(bottomLeft).toFloat(),
                alpha(bottomRight).toFloat(),
                horizontal,
                vertical,
            ).roundToInt(),
            red = blendChannel(red(topLeft), red(topRight), red(bottomLeft), red(bottomRight), horizontal, vertical),
            green = blendChannel(
                green(topLeft),
                green(topRight),
                green(bottomLeft),
                green(bottomRight),
                horizontal,
                vertical,
            ),
            blue = blendChannel(blue(topLeft), blue(topRight), blue(bottomLeft), blue(bottomRight), horizontal, vertical),
        )
    }

    private fun blendChannel(
        topLeft: Float,
        topRight: Float,
        bottomLeft: Float,
        bottomRight: Float,
        horizontal: Float,
        vertical: Float,
    ): Float {
        val top = topLeft + ((topRight - topLeft) * horizontal)
        val bottom = bottomLeft + ((bottomRight - bottomLeft) * horizontal)
        return top + ((bottom - top) * vertical)
    }

    private fun warpSourceCoordinate(
        textureX: Float,
        textureY: Float,
        params: ShaderFilterParams,
        output: FloatArray,
    ) {
        output[0] = textureX
        output[1] = textureY
        val features = params.makeupFeatures ?: return
        if (params.faceSlimming > 0f && features.faceRadiusX > 0f && features.faceRadiusY > 0f) {
            val sine = sin(features.rotationRadians)
            val cosine = cos(features.rotationRadians)
            val deltaX = (output[0] - features.faceCenterX) * cosine +
                (output[1] - features.faceCenterY) * sine
            val deltaY = -(output[0] - features.faceCenterX) * sine +
                (output[1] - features.faceCenterY) * cosine
            val normalizedX = deltaX / features.faceRadiusX
            val normalizedY = deltaY / features.faceRadiusY
            val distance = sqrt((normalizedX * normalizedX) + (normalizedY * normalizedY))
            val falloff = 1f - smoothstep(0.25f, 1.05f, distance)
            val slimmedX = deltaX * (1f + (params.faceSlimming * 0.18f * falloff))
            output[0] = features.faceCenterX + (slimmedX * cosine) - (deltaY * sine)
            output[1] = features.faceCenterY + (slimmedX * sine) + (deltaY * cosine)
        }
        if (params.eyeEnlargement > 0f) {
            applyEyeWarp(
                output,
                features.leftEyeCenterX,
                features.leftEyeCenterY,
                features.leftEyeRadiusX,
                features.leftEyeRadiusY,
                features.rotationRadians,
                params.eyeEnlargement,
            )
            applyEyeWarp(
                output,
                features.rightEyeCenterX,
                features.rightEyeCenterY,
                features.rightEyeRadiusX,
                features.rightEyeRadiusY,
                features.rotationRadians,
                params.eyeEnlargement,
            )
        }
    }

    private fun hasActiveWarp(params: ShaderFilterParams): Boolean {
        val features = params.makeupFeatures ?: return false
        return (params.faceSlimming > 0f && features.faceRadiusX > 0f && features.faceRadiusY > 0f) ||
            (params.eyeEnlargement > 0f &&
                ((features.leftEyeRadiusX > 0f && features.leftEyeRadiusY > 0f) ||
                    (features.rightEyeRadiusX > 0f && features.rightEyeRadiusY > 0f)))
    }

    private fun hasBeautyControls(params: ShaderFilterParams): Boolean =
        params.skinSmoothing != 0f || params.skinWhitening != 0f || hasFeatureBeautyControls(params)

    private fun hasFeatureBeautyControls(params: ShaderFilterParams): Boolean =
        params.makeupFeatures != null && (
            params.blush != 0f ||
                params.lipstick != 0f ||
                params.underEye != 0f ||
                params.teethWhitening != 0f ||
                params.eyeShadow != 0f ||
                params.eyeliner != 0f ||
                params.eyebrow != 0f
            )

    private fun needsFaceMask(params: ShaderFilterParams): Boolean =
        params.skinSmoothing != 0f ||
            params.skinWhitening != 0f ||
            params.underEye != 0f ||
            params.teethWhitening != 0f ||
            params.eyeShadow != 0f ||
            params.eyeliner != 0f ||
            params.eyebrow != 0f

    private fun needsBeautyMask(params: ShaderFilterParams): Boolean =
        params.skinSmoothing != 0f || params.skinWhitening != 0f || params.underEye != 0f

    internal fun isNoOp(params: ShaderFilterParams): Boolean =
        (params.effect == FilterEffect.Color || params.intensity == 0f) &&
            params.lutStrength <= 0f &&
            params.skinSmoothing == 0f &&
            params.skinWhitening == 0f &&
            params.blush == 0f &&
            params.lipstick == 0f &&
            params.underEye == 0f &&
            params.teethWhitening == 0f &&
            params.eyeShadow == 0f &&
            params.eyeliner == 0f &&
            params.eyebrow == 0f &&
            params.faceSlimming <= 0f &&
            params.eyeEnlargement <= 0f &&
            params.isMonochrome == 0f &&
            params.redShift == 0f &&
            params.greenShift == 0f &&
            params.blueShift == 0f &&
            params.brightness == 0f &&
            params.exposure == 1f &&
            params.contrast == 1f &&
            params.highlights == 0f &&
            params.shadows == 0f &&
            params.saturation == 1f &&
            params.vibrance == 0f &&
            params.temperature == 0f &&
            params.tint == 0f &&
            params.sharpness == 0f &&
            params.clarity == 0f &&
            params.fade == 0f &&
            params.vignette == 0f &&
            params.grain == 0f

    private fun applyEyeWarp(
        coordinate: FloatArray,
        centerX: Float,
        centerY: Float,
        radiusX: Float,
        radiusY: Float,
        rotationRadians: Float,
        amount: Float,
    ) {
        if (radiusX <= 0f || radiusY <= 0f) {
            return
        }
        val sine = sin(rotationRadians)
        val cosine = cos(rotationRadians)
        val deltaX = (coordinate[0] - centerX) * cosine + (coordinate[1] - centerY) * sine
        val deltaY = -(coordinate[0] - centerX) * sine + (coordinate[1] - centerY) * cosine
        val normalizedX = deltaX / (radiusX * 1.55f)
        val normalizedY = deltaY / (radiusY * 1.55f)
        val distance = sqrt((normalizedX * normalizedX) + (normalizedY * normalizedY))
        val falloff = 1f - smoothstep(0.25f, 1.05f, distance)
        val scale = 1f - (amount * 0.18f * falloff)
        val warpedX = deltaX * scale
        val warpedY = deltaY * scale
        coordinate[0] = centerX + (warpedX * cosine) - (warpedY * sine)
        coordinate[1] = centerY + (warpedX * sine) + (warpedY * cosine)
    }

    private fun red(color: Int): Float = ((color shr 16) and CHANNEL_MASK) / CHANNEL_MAX

    private fun green(color: Int): Float = ((color shr 8) and CHANNEL_MASK) / CHANNEL_MAX

    private fun blue(color: Int): Float = (color and CHANNEL_MASK) / CHANNEL_MAX

    private fun average(a: Float, b: Float, c: Float, d: Float): Float = (a + b + c + d) * 0.25f

    private fun gray(red: Float, green: Float, blue: Float): Float = (red * 0.299f) + (green * 0.587f) + (blue * 0.114f)

    private fun edgeAt(pixels: IntArray, x: Int, y: Int, width: Int, height: Int): Float {
        val leftX = (x - 1).coerceAtLeast(0)
        val rightX = (x + 1).coerceAtMost(width - 1)
        val topRow = (y - 1).coerceAtLeast(0) * width
        val middleRow = y * width
        val bottomRow = (y + 1).coerceAtMost(height - 1) * width
        val topLeft = luma(pixels[topRow + leftX])
        val top = luma(pixels[topRow + x])
        val topRight = luma(pixels[topRow + rightX])
        val left = luma(pixels[middleRow + leftX])
        val right = luma(pixels[middleRow + rightX])
        val bottomLeft = luma(pixels[bottomRow + leftX])
        val bottom = luma(pixels[bottomRow + x])
        val bottomRight = luma(pixels[bottomRow + rightX])
        val horizontal = -topLeft - (2f * left) - bottomLeft + topRight + (2f * right) + bottomRight
        val vertical = -topLeft - (2f * top) - topRight + bottomLeft + (2f * bottom) + bottomRight
        return clamp(sqrt((horizontal * horizontal) + (vertical * vertical)), 0f, 1f)
    }

    private fun luma(color: Int): Float = gray(red(color), green(color), blue(color))

    private fun lineFromEdge(edge: Float, params: ShaderFilterParams, softness: Float): Float {
        val threshold = mix(0.04f, 0.34f, params.effectThreshold)
        return smoothstep(threshold - softness, threshold + softness, edge) * params.effectStrength
    }

    private fun stripe(value: Float): Float = 1f - smoothstep(0f, 0.055f, abs((value - floor(value)) - 0.5f))

    private fun mix(start: Float, end: Float, amount: Float): Float = start * (1f - amount) + end * amount

    private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
        val t = clamp((value - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - (2f * t))
    }

    private fun skinMask(red: Float, green: Float, blue: Float): Float {
        val cb = 0.5f - (0.168736f * red) - (0.331264f * green) + (0.5f * blue)
        val cr = 0.5f + (0.5f * red) - (0.418688f * green) - (0.081312f * blue)
        val cbMask = 1f - smoothstep(0.08f, 0.18f, abs(cb - 0.40f))
        val crMask = 1f - smoothstep(0.05f, 0.25f, abs(cr - 0.55f))
        val redBias = smoothstep(0.01f, 0.14f, red - ((green + blue) * 0.5f))
        return clamp(cbMask * crMask * redBias, 0f, 1f)
    }

    private fun ellipseMask(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        radiusX: Float,
        radiusY: Float,
        rotationRadians: Float = 0f,
        innerEdge: Float = 0.45f,
        outerEdge: Float = 1f,
    ): Float {
        val sine = sin(rotationRadians)
        val cosine = cos(rotationRadians)
        val deltaX = (x - centerX) * cosine + (y - centerY) * sine
        val deltaY = -(x - centerX) * sine + (y - centerY) * cosine
        val distance = sqrt(
            ((deltaX / radiusX.coerceAtLeast(0.0001f)).pow(2f)) +
                ((deltaY / radiusY.coerceAtLeast(0.0001f)).pow(2f)),
        )
        return 1f - smoothstep(innerEdge, outerEdge, distance)
    }

    private fun underEyeMask(
        x: Float,
        y: Float,
        eyeCenterX: Float,
        eyeCenterY: Float,
        eyeRadiusX: Float,
        eyeRadiusY: Float,
        rotationRadians: Float,
    ): Float {
        if (eyeRadiusX <= 0f || eyeRadiusY <= 0f) {
            return 0f
        }
        val offsetX = sin(rotationRadians) * eyeRadiusY * 1.55f
        val offsetY = cos(rotationRadians) * eyeRadiusY * 1.55f
        return ellipseMask(
            x = x,
            y = y,
            centerX = eyeCenterX + offsetX,
            centerY = eyeCenterY + offsetY,
            radiusX = eyeRadiusX * 1.25f,
            radiusY = eyeRadiusY * 0.80f,
            rotationRadians = rotationRadians,
            innerEdge = 0.35f,
            outerEdge = 1.15f,
        )
    }

    private fun eyeShadowMask(
        x: Float,
        y: Float,
        eyeCenterX: Float,
        eyeCenterY: Float,
        eyeRadiusX: Float,
        eyeRadiusY: Float,
        rotationRadians: Float,
    ): Float {
        if (eyeRadiusX <= 0f || eyeRadiusY <= 0f) {
            return 0f
        }
        val sine = sin(rotationRadians)
        val cosine = cos(rotationRadians)
        val centerX = eyeCenterX - (sine * eyeRadiusY * 0.28f)
        val centerY = eyeCenterY - (cosine * eyeRadiusY * 0.28f)
        val deltaX = (x - centerX) * cosine + (y - centerY) * sine
        val deltaY = -(x - centerX) * sine + (y - centerY) * cosine
        val normalizedX = deltaX / (eyeRadiusX * 1.55f)
        val normalizedY = deltaY / (eyeRadiusY * 1.35f)
        val distance = sqrt((normalizedX * normalizedX) + (normalizedY * normalizedY))
        val ellipse = 1f - smoothstep(0.38f, 1.05f, distance)
        val upperLid = 1f - smoothstep(-0.10f, 0.48f, normalizedY)
        return ellipse * upperLid
    }

    private fun eyelinerMask(
        x: Float,
        y: Float,
        eyeCenterX: Float,
        eyeCenterY: Float,
        eyeRadiusX: Float,
        eyeRadiusY: Float,
        rotationRadians: Float,
    ): Float {
        if (eyeRadiusX <= 0f || eyeRadiusY <= 0f) {
            return 0f
        }
        val sine = sin(rotationRadians)
        val cosine = cos(rotationRadians)
        val deltaX = (x - eyeCenterX) * cosine + (y - eyeCenterY) * sine
        val deltaY = -(x - eyeCenterX) * sine + (y - eyeCenterY) * cosine
        val normalizedX = deltaX / (eyeRadiusX * 1.14f)
        val normalizedY = deltaY / (eyeRadiusY * 1.12f)
        val distance = sqrt((normalizedX * normalizedX) + (normalizedY * normalizedY))
        val band = smoothstep(0.70f, 0.86f, distance) * (1f - smoothstep(0.88f, 1.08f, distance))
        val upperLid = 1f - smoothstep(-0.12f, 0.30f, normalizedY)
        return band * upperLid
    }

    private fun polygonMask(x: Float, y: Float, points: List<NormalizedPoint>): Float {
        var inside = false
        var edgeDistance = 1f
        points.forEachIndexed { index, start ->
            val end = points[(index + 1) % points.size]
            edgeDistance = min(edgeDistance, pointSegmentDistance(x, y, start, end))
            if ((start.y > y) != (end.y > y)) {
                val intersectionX = start.x + ((y - start.y) * (end.x - start.x) / (end.y - start.y))
                if (x < intersectionX) {
                    inside = !inside
                }
            }
        }
        return if (inside && edgeDistance > 0.004f) 1f else 0f
    }

    private fun pointSegmentDistance(
        x: Float,
        y: Float,
        start: NormalizedPoint,
        end: NormalizedPoint,
    ): Float {
        val segmentX = end.x - start.x
        val segmentY = end.y - start.y
        val lengthSquared = max((segmentX * segmentX) + (segmentY * segmentY), 0.000001f)
        val projection = clamp(
            (((x - start.x) * segmentX) + ((y - start.y) * segmentY)) / lengthSquared,
            0f,
            1f,
        )
        val closestX = start.x + (segmentX * projection)
        val closestY = start.y + (segmentY * projection)
        return sqrt(((x - closestX).pow(2f)) + ((y - closestY).pow(2f)))
    }

    private fun lipColorMask(red: Float, green: Float, blue: Float): Float {
        val saturation = maxOf(red, green, blue) - minOf(red, green, blue)
        val redness = red - ((green + blue) * 0.5f)
        return smoothstep(0.20f, 0.34f, saturation) * smoothstep(0.16f, 0.26f, redness)
    }

    private fun teethColorMask(red: Float, green: Float, blue: Float): Float {
        val luma = gray(red, green, blue)
        val saturation = maxOf(red, green, blue) - minOf(red, green, blue)
        return smoothstep(0.55f, 0.72f, luma) * (1f - smoothstep(0.10f, 0.22f, saturation))
    }

    private fun clamp(value: Float, minValue: Float, maxValue: Float): Float = min(max(value, minValue), maxValue)

    private fun random(x: Float, y: Float): Float {
        val value = sin((x * 12.9898) + (y * 78.233)) * 43758.5453
        return (value - floor(value)).toFloat()
    }

    private fun argb(alpha: Int, red: Float, green: Float, blue: Float): Int =
        (alpha shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)

    private fun channel(value: Float): Int = (clamp(value, 0f, 1f) * CHANNEL_MASK).roundToInt().coerceIn(0, CHANNEL_MASK)

    internal data class RenderSize(
        val width: Int,
        val height: Int,
    )

    private const val CHANNEL_MASK = 255
    private const val CHANNEL_MAX = 255f
}
