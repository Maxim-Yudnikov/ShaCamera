package com.maxim.shacamera.core.sl

import androidx.lifecycle.ViewModel
import com.maxim.shacamera.camera.CameraModule
import com.maxim.shacamera.camera.presentation.CameraViewModel
import com.maxim.shacamera.main.MainModule
import com.maxim.shacamera.main.MainViewModel

interface DependencyContainer {
    fun <T : ViewModel> module(clasz: Class<T>): Module<T>

    class Error : DependencyContainer {
        override fun <T : ViewModel> module(clasz: Class<T>): Module<T> {
            throw IllegalStateException("unknown viewModel: $clasz")
        }
    }

    class Base(
        private val core: Core,
        private val clear: ClearViewModel,
        private val next: DependencyContainer = Error()
    ) : DependencyContainer {
        override fun <T : ViewModel> module(clasz: Class<T>) = when (clasz) {
            MainViewModel::class.java -> MainModule(core)
            CameraViewModel::class.java -> CameraModule()
            else -> next.module(clasz)
        } as Module<T>
    }
}