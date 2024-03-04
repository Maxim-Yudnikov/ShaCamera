package com.maxim.shacamera.camera.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.maxim.shacamera.R

interface CameraFilter {
    fun show(bitmap: Bitmap, context: Context, width: Int, height: Int, bitmapZoom: Float)

    class Rtx: CameraFilter {
        override fun show(
            bitmap: Bitmap,
            context: Context,
            width: Int,
            height: Int,
            bitmapZoom: Float
        ) {
            val sizes = Size(
                (180 / bitmapZoom).toInt(),
                (96 / bitmapZoom).toInt()
            )
            val rtxBitmap =
                ContextCompat.getDrawable(context, R.drawable.rtx_on)!!
                    .toBitmap(
                        if (sizes.width > 1) sizes.width else 1,
                        if (sizes.height > 1) sizes.height else 1
                    )
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(
                rtxBitmap,
                width / 10f * 6,
                height / 10f * 8,
                null
            )
        }
    }
}