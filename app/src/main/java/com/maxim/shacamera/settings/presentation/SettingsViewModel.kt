package com.maxim.shacamera.settings.presentation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.core.presentation.Communication
import com.maxim.shacamera.core.presentation.SimpleInit
import com.maxim.shacamera.core.sl.ClearViewModel
import com.maxim.shacamera.settings.data.RatioManager

class SettingsViewModel(
    private val communication: SettingsCommunication,
    private val ratioManager: RatioManager,
    private val clearViewModel: ClearViewModel
): ViewModel(), Communication.Observe<SettingsState>, SimpleInit {

    override fun init() {
        communication.update(SettingsState.Base(ratioManager.currentRatioPosition()))
    }

    fun setRatio(position: Int) {
        ratioManager.setRatio(position)
        init()
    }

    fun clear() {
        clearViewModel.clear(SettingsViewModel::class.java)
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<SettingsState>) {
        communication.observe(owner, observer)
    }
}