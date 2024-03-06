package com.maxim.shacamera.settings.presentation

import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner

interface SettingsState {
    fun show(
        radioGroup: RadioGroup,
        rtxSpinner: Spinner,
        dlssSpinner: Spinner,
        fsrSpinner: Spinner,
    )

    class Base(
        private val ratioPosition: Int,
        private val rtxMode: Int,
        private val dlssMode: Int,
        private val fsrMode: Int
    ) : SettingsState {
        override fun show(
            radioGroup: RadioGroup,
            rtxSpinner: Spinner,
            dlssSpinner: Spinner,
            fsrSpinner: Spinner
        ) {
            (radioGroup.getChildAt(ratioPosition) as RadioButton).isChecked = true
            rtxSpinner.setSelection(rtxMode)
            dlssSpinner.setSelection(dlssMode)
            fsrSpinner.setSelection(fsrMode)
        }
    }
}