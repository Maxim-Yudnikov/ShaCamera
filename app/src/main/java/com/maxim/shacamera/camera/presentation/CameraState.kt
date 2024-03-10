package com.maxim.shacamera.camera.presentation

import android.widget.TextView
import kotlin.math.roundToInt

interface CameraState {
    fun show(zoomValueTextView: TextView)

    class Base(private val zoom: Float): CameraState {
        override fun show(zoomValueTextView: TextView) {
            val zoomValueText = (if (zoom < 10) "${((zoom * 100).roundToInt() / 100f)}x" else "${zoom.roundToInt()}x").toString()
            zoomValueTextView.text = zoomValueText
        }
    }
}