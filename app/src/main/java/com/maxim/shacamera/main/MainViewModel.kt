package com.maxim.shacamera.main

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.maxim.shacamera.camera.presentation.CameraScreen
import com.maxim.shacamera.core.presentation.Communication
import com.maxim.shacamera.core.presentation.Init
import com.maxim.shacamera.core.presentation.Navigation
import com.maxim.shacamera.core.presentation.Screen

class MainViewModel(
    private val navigation: Navigation.Mutable
) : ViewModel(), Init, Communication.Observe<Screen> {

    override fun init(isFirstRun: Boolean) {
        if (isFirstRun)
            navigation.update(CameraScreen)
    }

    override fun observe(owner: LifecycleOwner, observer: Observer<Screen>) {
        navigation.observe(owner, observer)
    }
}