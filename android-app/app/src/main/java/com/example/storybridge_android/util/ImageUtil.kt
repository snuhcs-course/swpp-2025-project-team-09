package com.example.storybridge_android.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

object ImageUtil {

    private const val TAG = "ImageUtil"
    private const val COMPRESSION_QUALITY = 80
    private const val MAX_IMAGE_DIMENSION = 1920

    fun decodeAndCompressToBase64(imagePath: String): String? {
        val file = File(imagePath)
        if (!file.exists()) {
            Log.e(TAG, "File not found: $imagePath")
            return null
        }

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, options)

        val (width, height) = options.outWidth to options.outHeight
        var inSampleSize = 1
        if (max(width, height) > MAX_IMAGE_DIMENSION) {
            inSampleSize = (max(width, height).toFloat() / MAX_IMAGE_DIMENSION).toInt()
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val scaledBitmap = BitmapFactory.decodeFile(imagePath, options) ?: return null

        val finalBitmap = if (max(scaledBitmap.width, scaledBitmap.height) > MAX_IMAGE_DIMENSION) {
            val ratio = MAX_IMAGE_DIMENSION.toFloat() / max(scaledBitmap.width, scaledBitmap.height)
            scaledBitmap.scale(
                (scaledBitmap.width * ratio).toInt(),
                (scaledBitmap.height * ratio).toInt()
            )
        } else scaledBitmap

        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
        finalBitmap.recycle()
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        Log.d(TAG, "Encoded Base64 length: ${base64.length}")
        return base64
    }
}
