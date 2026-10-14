package com.pooloverlay

import android.graphics.Bitmap
import android.media.Image
import java.nio.ByteBuffer

object ImageUtils {
    fun toBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer: ByteBuffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bmp = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height, Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(bmp, 0, 0, image.width, image.height)
        if (cropped != bmp) bmp.recycle()
        return cropped
    }
}