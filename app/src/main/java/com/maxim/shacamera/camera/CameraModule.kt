package com.maxim.shacamera.camera

import com.maxim.shacamera.camera.data.CameraRepository
import com.maxim.shacamera.camera.presentation.CameraViewModel
import com.maxim.shacamera.core.sl.Module

class CameraModule: Module<CameraViewModel> {
    override fun viewModel() = CameraViewModel(CameraRepository.Base())
}