package com.maxim.shacamera.camera.data

import android.graphics.Point
import android.util.Size
import com.maxim.shacamera.camera.presentation.AutoFitTextureView

interface ScreenSizeMode {
    fun setAspectRatio(
        textureView: AutoFitTextureView,
        displaySize: Point,
        previewSize: Size,
        isDimensionSwapped: Boolean
    )

    fun comparator(): Comparator<Size>

    object SixteenToNine : ScreenSizeMode {
        override fun setAspectRatio(
            textureView: AutoFitTextureView,
            displaySize: Point,
            previewSize: Size,
            isDimensionSwapped: Boolean
        ) {
            val size = listOf(displaySize.x, displaySize.y).min()
            if (isDimensionSwapped)
                textureView.setAspectRatio(size / 16 * 9, size)
            else
                textureView.setAspectRatio(size, size / 16 * 9)
        }

        override fun comparator() = ComparableByRatio(16.0 / 9.0)
    }

    object FourToThree : ScreenSizeMode {
        override fun setAspectRatio(
            textureView: AutoFitTextureView,
            displaySize: Point,
            previewSize: Size,
            isDimensionSwapped: Boolean
        ) {
            val size = listOf(displaySize.x, displaySize.y).min()
            if (isDimensionSwapped)
                textureView.setAspectRatio(size / 4 * 3, size)
            else
                textureView.setAspectRatio(size, size / 4 * 3)
        }

        override fun comparator() = ComparableByRatio(4.0 / 3.0)
    }
}