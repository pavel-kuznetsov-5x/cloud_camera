package com.spqrta.camera2demo.utility.pure

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Base64
import java.io.*

object BitmapUtils {

    fun toFile(path: String, bitmap: Bitmap, quality: Int = 85) {
        try {
            FileOutputStream(path).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
        } catch (e: IOException) {
            throw e
        }
    }

    fun fromFile(path: String): Bitmap {
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeFile(path, options)
    }

    fun bytesFromFile(path: String): ByteArray {
        val file = File(path)
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: IOException) {
            throw e
        }
        return bytes
    }

    fun toBase64(bitmap: Bitmap): String {
        return Base64.encodeToString(
            toByteArray(
                bitmap
            ), Base64.DEFAULT);
    }

    fun fromByteArray(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    fun toByteArray(bitmap: Bitmap, quality: Int = 75): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    fun crop(bitmap: Bitmap, rect: Rect): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            rect.left,
            rect.top,
            rect.width(),
            rect.height()
        )
    }

    fun scale(bitmap: Bitmap, ratio: Float): Bitmap {
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            false
        )
    }

    fun scale(bitmap: Bitmap, width: Int): Bitmap {
        val ratio = width.toFloat() / bitmap.width

        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            false
        )
    }
}