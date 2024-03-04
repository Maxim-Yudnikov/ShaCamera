package com.maxim.shacamera.settings.presentation

import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.SwitchCompat

interface SettingsState {
    fun show(radioGroup: RadioGroup, rtxSwitch: SwitchCompat)

    class Base(
        private val ratioPosition: Int,
        private val rtxIsOn: Boolean
    ) : SettingsState {
        override fun show(radioGroup: RadioGroup, rtxSwitch: SwitchCompat) {
            (radioGroup.getChildAt(ratioPosition) as RadioButton).isChecked = true
            rtxSwitch.isChecked = rtxIsOn
        }
    }
}