package com.maxim.shacamera.camera.presentation

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.camera.data.CameraRepository
import com.maxim.shacamera.core.presentation.Communication
import com.maxim.shacamera.core.presentation.Navigation
import com.maxim.shacamera.core.presentation.Reload
import com.maxim.shacamera.settings.data.ManageFilters
import com.maxim.shacamera.settings.data.ManageRatio
import com.maxim.shacamera.settings.presentation.SettingsScreen
import com.maxim.shacamera.stickers.data.ShowSticker
import com.maxim.shacamera.stickers.data.StickersSharedCommunication
import com.maxim.shacamera.stickers.presentation.StickersScreen

class CameraViewModel(
    private val communication: CameraCommunication,
    private val repository: CameraRepository,
    private val manageRatio: ManageRatio,
    private val manageFilters: ManageFilters,
    private val stickersSharedCommunication: StickersSharedCommunication.Read,
    private val navigation: Navigation.Update
) : ViewModel(), Reload, ShowSticker, Communication.Observe<CameraState> {
    private var manageCamera: ManageCamera? = null
    private val myCameras = mutableListOf<CameraService>()
    private var currentCameraIndex = 0

    fun init(isFirstRun: Boolean, manageCamera: ManageCamera) {
        if (isFirstRun) {
            communication.update(CameraState.Base(1f))
        }
        manageRatio.setCallback(this)
        manageFilters.setCallback(this)
        stickersSharedCommunication.setCallback(this)
        this.manageCamera = manageCamera
    }

    fun screenSizeMode() = manageRatio.currentSizeMode()

    fun settings() {
        navigation.update(SettingsScreen)
    }

    fun stickers() {
        navigation.update(StickersScreen)
    }

    fun dlssMode() = manageFilters.dlssMode()

    fun cameraFilters(): List<CameraFilter> = manageFilters.allCameraFilters()

    fun resetBitmapZoom(
        cameraCharacteristics: CameraCharacteristics,
        captureRequestBuilder: CaptureRequest.Builder
    ) {
        val zoom = repository.setCameraZoomToMax(cameraCharacteristics, captureRequestBuilder)
        communication.update(CameraState.Base(zoom))
    }

    fun currentCamera() = myCameras[currentCameraIndex]
    fun currentCameraId() = myCameras[currentCameraIndex].cameraId()

    fun setCameras(list: List<CameraService>) {
        myCameras.clear()
        myCameras.addAll(list)
        currentCameraIndex = 0
    }

    fun bitmapZoom() = repository.bitmapZoom()

    fun setZoom(captureRequestBuilder: CaptureRequest.Builder) {
        val zoom = repository.setZoom(captureRequestBuilder)
        communication.update(CameraState.Base(zoom))
    }

    fun handleZoom(
        cameraCharacteristics: CameraCharacteristics,
        event: MotionEvent,
        screenMinSize: Int,
        captureRequestBuilder: CaptureRequest.Builder,
        cameraCaptureSession: CameraCaptureSession,
        handler: Handler,
        isRecording: Boolean
    ): Boolean {
        val value = repository.handleZoom(
            cameraCharacteristics,
            event,
            screenMinSize,
            captureRequestBuilder,
            cameraCaptureSession,
            handler,
            isRecording
        )
        communication.update(CameraState.Base(value.first))
        return value.second
    }

    fun changeCamera() {
        myCameras[currentCameraIndex].closeCamera()
        currentCameraIndex++
        if (currentCameraIndex == myCameras.size)
            currentCameraIndex = 0
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

    override fun showSticker(drawableId: Int) {
        (manageCamera as ShowSticker).showSticker(drawableId)
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<CameraState>) {
        communication.observe(owner, observer)
    }
}