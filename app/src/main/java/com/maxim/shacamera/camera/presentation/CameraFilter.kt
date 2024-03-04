package com.maxim.shacamera.camera.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.maxim.shacamera.R

interface CameraFilter {
    fun show(bitmap: Bitmap, context: Context, bitmapZoom: Float)

    object Rtx: CameraFilter {
        override fun show(
            bitmap: Bitmap,
            context: Context,
            bitmapZoom: Float
        ) {
            val sizes = Size(
                (180 / bitmapZoom).toInt(),
                (78 / bitmapZoom).toInt()
            )
            val filterBitmap =
                ContextCompat.getDrawable(context, R.drawable.rtx_on)!!
                    .toBitmap(
                        if (sizes.width > 1) sizes.width else 1,
                        if (sizes.height > 1) sizes.height else 1
                    )
            Canvas(bitmap).drawBitmap(
                filterBitmap,
                bitmap.width / 10f * 6,
                bitmap.height / 10f * 8,
                null
            )
        }
    }

    object Dlss: CameraFilter {
        override fun show(bitmap: Bitmap, context: Context, bitmapZoom: Float) {
            val sizes = Size(
                (420 / bitmapZoom).toInt(),
                (40 / bitmapZoom).toInt()
            )
            val filterBitmap =
                ContextCompat.getDrawable(context, R.drawable.dlss_on)!!
                    .toBitmap(
                        if (sizes.width > 1) sizes.width else 1,
                        if (sizes.height > 1) sizes.height else 1
                    )
            Canvas(bitmap).drawBitmap(filterBitmap, 0f, 0f, null)
        }
    }

    object Fsr: CameraFilter {
        override fun show(bitmap: Bitmap, context: Context, bitmapZoom: Float) {
            val sizes = Size(
                (180 / bitmapZoom).toInt(),
                (78 / bitmapZoom).toInt()
            )
            val filterBitmap =
                ContextCompat.getDrawable(context, R.drawable.fsr_on)!!
                    .toBitmap(
                        if (sizes.width > 1) sizes.width else 1,
                        if (sizes.height > 1) sizes.height else 1
                    )
            Canvas(bitmap).drawBitmap(
                filterBitmap,
                0f,
                bitmap.height / 10f * 8,
                null
            )
        }
    }
}