package com.maxim.shacamera.settings

import com.maxim.shacamera.core.sl.ClearViewModel
import com.maxim.shacamera.core.sl.Core
import com.maxim.shacamera.core.sl.Module
import com.maxim.shacamera.settings.presentation.SettingsCommunication
import com.maxim.shacamera.settings.presentation.SettingsViewModel

class SettingsModule(private val core: Core, private val clearViewModel: ClearViewModel) :
    Module<SettingsViewModel> {
    override fun viewModel() = SettingsViewModel(
        SettingsCommunication.Base(),
        core.ratioManager(),
        clearViewModel
    )
}