package com.maxim.shacamera.camera.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.maxim.shacamera.R
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core.LUT
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

interface CameraFilter {
    fun showFilter(bitmap: Bitmap)
    fun showImage(bitmap: Bitmap, context: Context, bitmapZoom: Float)

    data class Rtx(private val isWeak: Boolean) : CameraFilter {
        override fun showFilter(bitmap: Bitmap) {
            if (isWeak) return
            if (OpenCVLoader.initLocal()) {
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                val lut = Lut().createLUT(10)
                LUT(mat, lut, mat)
                val result = Lut().reduceColors(mat, 150, 150, 0)
                Utils.matToBitmap(result, bitmap)
            }
        }

        override fun showImage(bitmap: Bitmap, context: Context, bitmapZoom: Float) {
            val screen = context.resources.displayMetrics
            val width = 180 * (screen.widthPixels / 1080f)
            val sizes = Size(
                (width / bitmapZoom).toInt(),
                (width / 2.3f / bitmapZoom).toInt()
            )
            val filterBitmap =
                ContextCompat.getDrawable(context, R.drawable.rtx_on)!!
                    .toBitmap(
                        if (sizes.width > 1) sizes.width else 1,
                        if (sizes.height > 1) sizes.height else 1
                    )
            Canvas(bitmap).drawBitmap(
                filterBitmap,
                null,
                Rect(
                    (bitmap.width * 0.6f).toInt(),
                    (bitmap.height * 0.8f).toInt(),
                    screen.widthPixels,
                    (bitmap.height * 0.95f).toInt()
                ),
                null
            )
        }
    }

    //bitmap.width / 10f * 6,
    //bitmap.height / 10f * 8,

    data class Dlss(private val isWeak: Boolean) : CameraFilter {
        override fun showFilter(bitmap: Bitmap) = Unit

        override fun showImage(bitmap: Bitmap, context: Context, bitmapZoom: Float) {
            val screen = context.resources.displayMetrics
            val sizes = Size(
                (screen.widthPixels / bitmapZoom).toInt(),
                (screen.widthPixels / 9f / bitmapZoom).toInt()
            )
            val filterBitmap =
                ContextCompat.getDrawable(context, R.drawable.dlss_on)!!
                    .toBitmap(
                        if (sizes.width > 1) sizes.width else 1,
                        if (sizes.height > 1) sizes.height else 1
                    )
            Canvas(bitmap).drawBitmap(
                filterBitmap,
                Rect(0, 0, screen.widthPixels, screen.heightPixels),
                Rect(0, 0, screen.widthPixels, screen.heightPixels),
                null
            )
        }
    }

    data class Fsr(private val isWeak: Boolean) : CameraFilter {
        override fun showFilter(bitmap: Bitmap) {
            if (isWeak) return
            if (OpenCVLoader.initLocal()) {
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)
                Imgproc.GaussianBlur(mat, mat, org.opencv.core.Size(25.0, 25.0), 20.0)
                Utils.matToBitmap(mat, bitmap)
            }
        }

        override fun showImage(bitmap: Bitmap, context: Context, bitmapZoom: Float) {
            val screen = context.resources.displayMetrics
            val width = 180 * (screen.widthPixels / 1080f)
            val sizes = Size(
                (width / bitmapZoom).toInt(),
                (width / 2.3f / bitmapZoom).toInt()
            )
            val filterBitmap =
                ContextCompat.getDrawable(context, R.drawable.fsr_on)!!
                    .toBitmap(
                        if (sizes.width > 1) sizes.width else 1,
                        if (sizes.height > 1) sizes.height else 1
                    )
            Canvas(bitmap).drawBitmap(
                filterBitmap,
                null,
                Rect(
                    0,
                    (bitmap.height * 0.8f).toInt(),
                    (bitmap.width * 0.4f).toInt(),
                    (bitmap.height * 0.95f).toInt()
                ),
                null
            )
        }
    }

    //Rect(
    //                    (bitmap.width * 0.6f).toInt(),
    //                    (bitmap.height * 0.8f).toInt(),
    //                    screen.widthPixels,
    //                    (bitmap.height * 0.95f).toInt()
    //                ),
}