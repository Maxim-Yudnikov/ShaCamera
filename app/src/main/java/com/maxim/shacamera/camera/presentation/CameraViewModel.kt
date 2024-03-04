package com.maxim.shacamera.camera.presentation

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.view.MotionEvent
import android.widget.TextView
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.camera.data.CameraRepository
import com.maxim.shacamera.core.presentation.Navigation
import com.maxim.shacamera.core.presentation.Reload
import com.maxim.shacamera.settings.data.RatioManager
import com.maxim.shacamera.settings.presentation.SettingsScreen

class CameraViewModel(
    private val repository: CameraRepository,
    private val ratioManager: RatioManager,
    private val navigation: Navigation.Update
) : ViewModel(), Reload {
    private var manageCamera: ManageCamera? = null

    fun init(isFirstRun: Boolean, manageCamera: ManageCamera) {
        if (isFirstRun) {
            ratioManager.setCallback(this)
            this.manageCamera = manageCamera
        }
    }

    fun screenSizeMode() = ratioManager.currentSizeMode()

    fun settings() {
        navigation.update(SettingsScreen)
    }

    fun bitmapZoom() = repository.bitmapZoom()

    fun handleZoom(
        cameraCharacteristics: CameraCharacteristics,
        event: MotionEvent,
        screenMinSize: Int,
        zoomValueTextView: TextView,
        captureRequestBuilder: CaptureRequest.Builder,
        cameraCaptureSession: CameraCaptureSession,
        handler: Handler
    ): Boolean =
        repository.handleZoom(
            cameraCharacteristics,
            event,
            screenMinSize,
            zoomValueTextView,
            captureRequestBuilder,
            cameraCaptureSession,
            handler
        )

    fun changeCamera(cameraId: Int) {
        manageCamera?.closeCamera(cameraId)
        manageCamera?.openCamera(if (cameraId == 0) 1 else 0)
    }

    fun makePhoto() {
        manageCamera?.makePhoto()
    }

    fun onResume() {
        manageCamera?.startBackgroundThread()
    }

    fun onPause() {
        manageCamera?.stopBackgroundThread()
        manageCamera?.closeCamera(0)
        manageCamera?.closeCamera(1)
    }

    fun openCamera() {
        manageCamera?.openCamera(0)
    }

    override fun reload() {
        manageCamera?.closeCamera(0)
        manageCamera?.openCamera(0)
    }
}