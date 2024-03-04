package com.maxim.shacamera.settings.presentation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.core.presentation.Communication
import com.maxim.shacamera.core.presentation.SimpleInit
import com.maxim.shacamera.core.sl.ClearViewModel
import com.maxim.shacamera.settings.data.ManageFilters
import com.maxim.shacamera.settings.data.ManageRatio

class SettingsViewModel(
    private val communication: SettingsCommunication,
    private val manageRatio: ManageRatio,
    private val manageFilters: ManageFilters,
    private val clearViewModel: ClearViewModel
) : ViewModel(), Communication.Observe<SettingsState>, SimpleInit {

    override fun init() {
        communication.update(
            SettingsState.Base(
                manageRatio.currentRatioPosition(),
                manageFilters.rtxIsOn()
            )
        )
    }

    fun setRatio(position: Int) {
        manageRatio.setRatio(position)
        init()
    }

    fun setRtx(value: Boolean) {
        manageFilters.setRxt(value)
        init()
    }

    fun clear() {
        clearViewModel.clear(SettingsViewModel::class.java)
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<SettingsState>) {
        communication.observe(owner, observer)
    }
}