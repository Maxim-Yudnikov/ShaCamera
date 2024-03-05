package com.maxim.shacamera.stickers.presentation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.core.presentation.Communication
import com.maxim.shacamera.core.presentation.SimpleInit
import com.maxim.shacamera.core.sl.ClearViewModel
import com.maxim.shacamera.stickers.data.StickersRepository
import com.maxim.shacamera.stickers.data.StickersSharedCommunication

class StickersViewModel(
    private val repository: StickersRepository,
    private val communication: StickersCommunication,
    private val sharedCommunication: StickersSharedCommunication.Call,
    private val clearViewModel: ClearViewModel
): ViewModel(), SimpleInit, Communication.Observe<StickersState> {

    override fun init() {
        communication.update(StickersState.Base(repository.stickers()))
    }

    fun create(drawableId: Int) {
        sharedCommunication.showSticker(drawableId)
    }

    fun clear() {
        clearViewModel.clear(StickersViewModel::class.java)
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<StickersState>) {
        communication.observe(owner, observer)
    }
}