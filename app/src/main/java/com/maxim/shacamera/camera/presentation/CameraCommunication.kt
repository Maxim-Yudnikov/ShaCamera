package com.maxim.shacamera.camera.presentation

import com.maxim.shacamera.core.presentation.Communication

interface CameraCommunication: Communication.Mutable<CameraState> {
    class Base: Communication.Regular<CameraState>(), CameraCommunication
}