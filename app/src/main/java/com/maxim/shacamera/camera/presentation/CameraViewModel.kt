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
    private var currentCameraIndex = 0

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

    fun dlssIsOn() = manageFilters.dlssIsOn()

    fun cameraFilters(): List<CameraFilter> = manageFilters.allCameraFilters()

    fun currentCamera() = myCameras[currentCameraIndex]
    fun currentCameraId() = myCameras[currentCameraIndex].cameraId()

    fun setCameras(list: List<CameraService>) {
        myCameras.clear()
        myCameras.addAll(list)
        currentCameraIndex = 0
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
    ): Boolean {
        return repository.handleZoom(
            cameraCharacteristics,
            event,
            screenMinSize,
            zoomValueTextView,
            captureRequestBuilder,
            cameraCaptureSession,
            handler
        )
    }

    fun changeCamera() {
        myCameras[currentCameraIndex].closeCamera()
        currentCameraIndex = if (currentCameraIndex == 0) 1 else 0
        manageCamera?.openCamera(myCameras[currentCameraIndex])
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
        manageCamera?.openCamera(myCameras[currentCameraIndex])
    }

    override fun reload() {
        myCameras[currentCameraIndex].closeCamera()
        manageCamera?.openCamera(myCameras[currentCameraIndex])
    }
}