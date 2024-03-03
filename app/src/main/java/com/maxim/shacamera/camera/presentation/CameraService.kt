package com.maxim.shacamera.camera.presentation

import android.annotation.SuppressLint
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler

interface CameraService {
    fun openCamera(handler: Handler)
    fun closeCamera()
    fun isOpen(): Boolean

    //todo
    fun cameraId(): String

    class Base(
        private val cameraId: String,
        private val cameraManager: CameraManager,
        private val manageCamera: ManageCamera
    ) : CameraService {
        private var cameraDevice: CameraDevice? = null
        private val cameraCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                manageCamera.createCameraPreviewSession(cameraDevice!!)
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice!!.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) = Unit
        }

        @SuppressLint("MissingPermission")
        override fun openCamera(handler: Handler) {
            cameraManager.openCamera(cameraId, cameraCallback, handler)
        }

        override fun closeCamera() {
            cameraDevice?.let {
                it.close()
                cameraDevice = null
            }
        }

        override fun isOpen() = cameraDevice != null

        override fun cameraId() = cameraId
    }
}