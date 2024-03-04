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
import com.maxim.shacamera.settings.data.ManageFilters
import com.maxim.shacamera.settings.data.ManageRatio
import com.maxim.shacamera.settings.presentation.SettingsScreen

class CameraViewModel(
    private val repository: CameraRepository,
    private val manageRatio: ManageRatio,
    private val manageFilters: ManageFilters,
    private val navigation: Navigation.Update
) : ViewModel(), Reload {
    private var manageCamera: ManageCamera? = null
    private val myCameras = mutableListOf<CameraService>()
    private var currentCameraId = 0

    fun init(isFirstRun: Boolean, manageCamera: ManageCamera) {
        if (isFirstRun) {
            manageRatio.setCallback(this)
            manageFilters.setCallback(this)
            this.manageCamera = manageCamera
        }
    }

    fun screenSizeMode() = manageRatio.currentSizeMode()

    fun settings() {
        navigation.update(SettingsScreen)
    }

    fun rtxIsOn() = manageFilters.rtxIsOn()

    fun currentCamera() = myCameras[currentCameraId]
    fun currentCameraId() = currentCameraId

    fun setCameras(list: List<CameraService>) {
        myCameras.clear()
        myCameras.addAll(list)
        currentCameraId = 0
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

    fun changeCamera() {
        myCameras[currentCameraId].closeCamera()
        currentCameraId = if (currentCameraId == 0) 1 else 0
        manageCamera?.openCamera(myCameras[currentCameraId])
    }

    fun makePhoto() {
        manageCamera?.makePhoto()
    }

    fun onResume() {
        manageCamera?.startBackgroundThread()
    }

    fun onPause() {
        manageCamera?.stopBackgroundThread()
        myCameras.forEach {
            it.closeCamera()
        }
    }

    fun openCamera() {
        manageCamera?.openCamera(myCameras[currentCameraId])
    }

    override fun reload() {
        myCameras[currentCameraId].closeCamera()
        manageCamera?.openCamera(myCameras[currentCameraId])
    }
}