package com.maxim.shacamera.settings.presentation

import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.SwitchCompat

interface SettingsState {
    fun show(
        radioGroup: RadioGroup,
        rtxSwitch: SwitchCompat,
        dlssSwitchCompat: SwitchCompat,
        fsrSwitchCompat: SwitchCompat,
    )

    class Base(
        private val ratioPosition: Int,
        private val rtxIsOn: Boolean,
        private val dlssIsOn: Boolean,
        private val fsrIsOn: Boolean
    ) : SettingsState {
        override fun show(
            radioGroup: RadioGroup,
            rtxSwitch: SwitchCompat,
            dlssSwitchCompat: SwitchCompat,
            fsrSwitchCompat: SwitchCompat
        ) {
            (radioGroup.getChildAt(ratioPosition) as RadioButton).isChecked = true
            rtxSwitch.isChecked = rtxIsOn
            dlssSwitchCompat.isChecked = dlssIsOn
            fsrSwitchCompat.isChecked = fsrIsOn
        }
    }
}