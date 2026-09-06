package com.gsfilter.utils

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.svg.SvgDecoder
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LoadUtils {

    fun getBitmapFromAsset(context: Context, assetPath: String, threshold: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.assets.open(assetPath).use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Cannot decode asset bounds")
        }

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = bitmapInSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
                threshold = threshold,
            )
        }
        context.assets.open(assetPath).use { input ->
            return BitmapFactory.decodeStream(input, null, options)
                ?: throw IOException("Cannot decode asset")
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun getBitmapFromPath(
        activity: FragmentActivity, path: String, index: Int = -1, threshold: Int, callback: (pathSuccess: String, bitmap: Bitmap, index: Int) -> Unit, callbackFailed: (pathFailed: String) -> Unit
    ) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cachePath = ImageCacheUtils.cache4k(activity, path)
                val bitmapOptions: BitmapFactory.Options = BitmapFactory.Options()
                bitmapOptions.inJustDecodeBounds = true
                BitmapFactory.decodeFile(cachePath, bitmapOptions)
                val size = bitmapRequestSize(
                    width = bitmapOptions.outWidth,
                    height = bitmapOptions.outHeight,
                    threshold = threshold,
                )

                val loader = ImageLoader(activity)

                val builder = ImageRequest.Builder(activity).data(cachePath).allowHardware(false) // Disable hardware bitmaps.

                if (size != 0) {
                    builder.size(size)
                }

                if (path.endsWith(".svg")) {
                    builder.decoderFactory(SvgDecoder.Factory())
                }

                val request = builder.build()

                when (val imageResult = loader.execute(request)) {
                    is SuccessResult -> {
                        val result = imageResult.image.asDrawable(activity.resources)
                        val bitmap = (result as BitmapDrawable).bitmap

                        withContext(Dispatchers.Main) {
                            callback.invoke(cachePath, bitmap, index)
                        }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            callbackFailed.invoke(path)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    callbackFailed.invoke(path)
                }
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun getBitmapFromResId(
        activity: FragmentActivity, resId: Int, threshold: Int, callback: (bitmap: Bitmap) -> Unit, callbackFailed: () -> Unit
    ) {
        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val bitmapOptions: BitmapFactory.Options = BitmapFactory.Options()
                bitmapOptions.inJustDecodeBounds = true
                BitmapFactory.decodeResource(activity.resources, resId, bitmapOptions)
                val size = bitmapRequestSize(
                    width = bitmapOptions.outWidth,
                    height = bitmapOptions.outHeight,
                    threshold = threshold,
                )

                val loader = ImageLoader(activity)

                val builder = ImageRequest.Builder(activity).data(resId).allowHardware(false) // Disable hardware bitmaps.

                if (size != 0) {
                    builder.size(size)
                }

                val request = builder.build()

                when (val imageResult = loader.execute(request)) {
                    is SuccessResult -> {
                        val result = imageResult.image.asDrawable(activity.resources)
                        val bitmap = (result as BitmapDrawable).bitmap

                        withContext(Dispatchers.Main) {
                            callback.invoke(bitmap)
                        }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            callbackFailed.invoke()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    callbackFailed.invoke()
                }
            }
        }
    }

    fun calculatorImageSize(context: Context): Int {
        val memory = getAvailableMemory(context)
        val availableMemory = memory.availMem.bytesToGb()
        return when {
            availableMemory >= 2 -> 1080
            availableMemory >= 1.5 -> 960
            availableMemory >= 1.0 -> 720
            else -> 512
        }
    }

    fun getAvailableMemory(context: Context): ActivityManager.MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also { memoryInfo ->
            activityManager.getMemoryInfo(memoryInfo)
        }
    }

    fun Long.bytesToGb(): Float {
        return this.toFloat() / 1024 / 1024 / 1024
    }

    internal fun bitmapInSampleSize(width: Int, height: Int, threshold: Int): Int {
        val size = bitmapRequestSize(width, height, threshold)
        if (size <= 0) {
            return 1
        }

        var sampleSize = 1
        while (width.coerceAtLeast(height) / sampleSize > size) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun bitmapRequestSize(width: Int, height: Int, threshold: Int): Int {
        if (width <= 0 || height <= 0 || threshold <= 0) {
            return 0
        }

        val maxSide = width.coerceAtLeast(height)
        val ratio = maxSide / width.coerceAtMost(height).toFloat()
        val max = (maxSide / 2).coerceAtLeast(threshold)
        return if (ratio > 3f) {
            max
        } else {
            threshold.coerceAtMost(maxSide)
        }
    }
}
