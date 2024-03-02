package com.maxim.shacamera

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.core.math.MathUtils

class Zoom(private val cameraCharacteristics: CameraCharacteristics) {
    private val cropRegion = Rect()
    private var hasSupport = true
    private val maxZoom = 100f
    private var sensorSize: Rect? = null

    init {
        sensorSize =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

        if (sensorSize == null) {
            hasSupport = false
        }
    }

    fun setZoom(builder: CaptureRequest.Builder, zoom: Float) {
        if (!hasSupport) return

        val newZoom = MathUtils.clamp(zoom, DEFAULT_ZOOM, maxZoom)
        val centerX = sensorSize!!.width() / 2
        val centerY = sensorSize!!.height() / 2
        val deltaX = (0.5f * sensorSize!!.width() / newZoom).toInt()
        val deltaY = (0.5f * sensorSize!!.height() / newZoom).toInt()

        cropRegion.set(centerX - deltaX, centerY - deltaY, centerX+ deltaX, centerY + deltaY)

        builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegion)
    }

    companion object {
        private const val DEFAULT_ZOOM = 1f
    }
}