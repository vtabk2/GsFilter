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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LoadUtils {

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
                val ratio = if (bitmapOptions.outWidth > bitmapOptions.outHeight) {
                    bitmapOptions.outWidth / bitmapOptions.outHeight.toFloat()
                } else {
                    bitmapOptions.outHeight / bitmapOptions.outWidth.toFloat()
                }

                val max = (bitmapOptions.outWidth.coerceAtLeast(bitmapOptions.outHeight) / 2).coerceAtLeast(threshold)
                val size = if (ratio > 3f) {
                    max
                } else {
                    threshold.coerceAtMost(bitmapOptions.outWidth.coerceAtLeast(bitmapOptions.outHeight))
                }

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
                val ratio = if (bitmapOptions.outWidth > bitmapOptions.outHeight) {
                    bitmapOptions.outWidth / bitmapOptions.outHeight.toFloat()
                } else {
                    bitmapOptions.outHeight / bitmapOptions.outWidth.toFloat()
                }

                val max = (bitmapOptions.outWidth.coerceAtLeast(bitmapOptions.outHeight) / 2).coerceAtLeast(threshold)
                val size = if (ratio > 3f) {
                    max
                } else {
                    threshold.coerceAtMost(bitmapOptions.outWidth.coerceAtLeast(bitmapOptions.outHeight))
                }

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
}