package com.maxim.shacamera.camera.presentation

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import kotlin.reflect.KMutableProperty

interface CameraService {
    fun openCamera(handler: Handler)
    fun closeCamera()
    fun isOpen(): Boolean
    fun createCameraPreviewSession(
        textureView: TextureView,
        handler: Handler,
        captureRequestBuilder: KMutableProperty<CaptureRequest.Builder?>,
        cameraCaptureSession: KMutableProperty<CameraCaptureSession?>
    )

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
                manageCamera.createCameraPreviewSession()
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

        @RequiresApi(Build.VERSION_CODES.S)
        override fun createCameraPreviewSession(
            textureView: TextureView,
            handler: Handler,
            captureRequestBuilder: KMutableProperty<CaptureRequest.Builder?>,
            cameraCaptureSession: KMutableProperty<CameraCaptureSession?>
        ) {
            val texture = textureView.surfaceTexture
            val surface = Surface(texture)

            try {
                captureRequestBuilder.setter.call(cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW))
                captureRequestBuilder.getter.call()!!.addTarget(surface)
                cameraDevice!!.createCaptureSession(
                    listOf(surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            cameraCaptureSession.setter.call(session)
                            try {
                                session.setRepeatingRequest(
                                    captureRequestBuilder.getter.call()!!.build(),
                                    null,
                                    handler
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) = Unit
                    },
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun cameraId() = cameraId
    }
}