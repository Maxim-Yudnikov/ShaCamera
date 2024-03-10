package com.maxim.shacamera.camera

import com.maxim.shacamera.camera.data.CameraRepository
import com.maxim.shacamera.camera.presentation.CameraCommunication
import com.maxim.shacamera.camera.presentation.CameraViewModel
import com.maxim.shacamera.core.sl.Core
import com.maxim.shacamera.core.sl.Module

class CameraModule(private val core: Core) : Module<CameraViewModel> {
    override fun viewModel() = CameraViewModel(
        CameraCommunication.Base(),
        CameraRepository.Base(),
        core.manageRatio(),
        core.manageFilters(),
        core.stickerSharedCommunication(),
        core.navigation()
    )
}