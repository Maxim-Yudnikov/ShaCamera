package com.maxim.shacamera.settings.presentation

import android.widget.RadioButton
import android.widget.RadioGroup

interface SettingsState {
    fun show(radioGroup: RadioGroup)

    class Base(private val ratioPosition: Int) : SettingsState {
        override fun show(radioGroup: RadioGroup) {
            (radioGroup.getChildAt(ratioPosition) as RadioButton).isChecked = true
        }
    }
}