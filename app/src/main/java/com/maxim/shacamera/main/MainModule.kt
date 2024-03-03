package com.maxim.shacamera.main

import com.maxim.shacamera.core.sl.Core
import com.maxim.shacamera.core.sl.Module

class MainModule(private val core: Core): Module<MainViewModel> {
    override fun viewModel() = MainViewModel(core.navigation())
}