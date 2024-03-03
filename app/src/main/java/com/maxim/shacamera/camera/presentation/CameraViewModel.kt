package com.maxim.shacamera.camera.presentation

import androidx.lifecycle.ViewModel

class CameraViewModel: ViewModel() {

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