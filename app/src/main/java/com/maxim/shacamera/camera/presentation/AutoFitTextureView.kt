package com.maxim.shacamera.camera.presentation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.TextureView

class AutoFitTextureView: TextureView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private var ratioWidth = 0
    private var ratioHeight = 0



    fun setAspectRatio(width: Int, height: Int) {
        Handler(Looper.getMainLooper()).post {
            if (width < 0 || height < 0) {
                throw IllegalArgumentException("Size cannot be negative.")
            }
            ratioWidth = width
            ratioHeight = height
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }
}