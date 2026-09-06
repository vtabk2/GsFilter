package com.gsfilter.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import androidx.core.graphics.scale

object ImageCacheUtils {
    private const val CACHE_DIR = "image_cache_4k"
    private const val MAX_SIDE = 4096

    fun cache4k(context: Context, path: String): String {
        if (path.startsWith("file:///android_asset/") || path.endsWith(".svg", true)) return path

        val source = File(path)
        if (!source.exists() || source.parentFile?.name == CACHE_DIR) return path

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val maxSide = bounds.outWidth.coerceAtLeast(bounds.outHeight)
        if (maxSide <= 0 || maxSide <= MAX_SIDE) return path

        val isPng = path.endsWith(".png", true)
        val cacheFile = File(cacheDir(context), "img4k_${Integer.toHexString(path.hashCode())}_${source.length()}_${source.lastModified()}.${if (isPng) "png" else "jpg"}")
        if (cacheFile.length() > 0) return cacheFile.absolutePath

        return try {
            val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                inSampleSize = sampleSize(maxSide)
            }) ?: return path
            val rotated = rotate(decoded, exifRotation(path))
            if (rotated !== decoded) decoded.recycle()
            val scaled = scaleDown(rotated)
            if (scaled !== rotated) rotated.recycle()

            FileOutputStream(cacheFile).use {
                scaled.compress(if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, it)
            }
            scaled.recycle()
            cacheFile.absolutePath
        } catch (e: Exception) {
            cacheFile.delete()
            path
        }
    }

    private fun cacheDir(context: Context): File {
        return File(context.externalCacheDir ?: context.cacheDir, CACHE_DIR).apply { mkdirs() }
    }

    private fun sampleSize(maxSide: Int): Int {
        var sample = 1
        while (maxSide / sample > MAX_SIDE) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val maxSide = bitmap.width.coerceAtLeast(bitmap.height)
        if (maxSide <= MAX_SIDE) return bitmap
        val scale = MAX_SIDE / maxSide.toFloat()
        return bitmap.scale((bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt())
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
    }

    private fun exifRotation(path: String): Int {
        return try {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }
}