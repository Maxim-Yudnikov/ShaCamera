package com.maxim.shacamera.stickers

import com.maxim.shacamera.core.sl.ClearViewModel
import com.maxim.shacamera.core.sl.Core
import com.maxim.shacamera.core.sl.Module
import com.maxim.shacamera.stickers.data.StickersRepository
import com.maxim.shacamera.stickers.presentation.StickersCommunication
import com.maxim.shacamera.stickers.presentation.StickersViewModel

class StickersModule(private val core: Core, private val clearViewModel: ClearViewModel) : Module<StickersViewModel> {
    override fun viewModel(): StickersViewModel {
        return StickersViewModel(
            StickersRepository.Base(),
            StickersCommunication.Base(),
            core.stickerSharedCommunication(),
            clearViewModel
        )
    }
}