package com.maxim.shacamera.settings.presentation

import com.maxim.shacamera.core.presentation.Communication

interface SettingsCommunication: Communication.Mutable<SettingsState> {
    class Base: Communication.Regular<SettingsState>(), SettingsCommunication
}