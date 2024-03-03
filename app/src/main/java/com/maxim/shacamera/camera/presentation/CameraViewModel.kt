package com.maxim.shacamera.camera.presentation

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.view.MotionEvent
import android.widget.TextView
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.camera.data.CameraRepository

class CameraViewModel(
    private val repository: CameraRepository
) : ViewModel() {

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

    fun changeCamera(manageCamera: ManageCamera, cameraId: Int) {
        manageCamera.closeCamera(cameraId)
        manageCamera.openCamera(if (cameraId == 0) 1 else 0)
    }

    fun makePhoto(manageCamera: ManageCamera) {
        manageCamera.makePhoto()
    }

    fun onResume(manageCamera: ManageCamera) {
        manageCamera.startBackgroundThread()
    }

    fun onPause(manageCamera: ManageCamera) {
        manageCamera.stopBackgroundThread()
        manageCamera.closeCamera(0)
        manageCamera.closeCamera(1)
    }

    fun openCamera(manageCamera: ManageCamera) {
        manageCamera.openCamera(0)
    }
}