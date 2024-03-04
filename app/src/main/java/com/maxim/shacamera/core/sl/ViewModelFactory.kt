package com.maxim.shacamera.core.sl

import androidx.lifecycle.ViewModel
import java.lang.IllegalStateException

interface ViewModelFactory: ProvideViewModel, ClearViewModel {

    class Base(private val provider: ProvideViewModel): ViewModelFactory {
        private val map = mutableMapOf<Class<out ViewModel>, ViewModel>()

        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> viewModel(clasz: Class<T>): T {
            if (map[clasz] == null)
                map[clasz] = provider.viewModel(clasz)
            return map[clasz] as T
        }

        override fun clear(clasz: Class<out ViewModel>) {
            map.remove(clasz)
        }
    }

    object Empty: ViewModelFactory {
        override fun <T : ViewModel> viewModel(clasz: Class<T>): T {
            throw IllegalStateException("empty viewModelFactory")
        }

        override fun clear(clasz: Class<out ViewModel>) = Unit
    }
}

interface ProvideViewModel {
    fun <T: ViewModel> viewModel(clasz: Class<T>): T

    class Base(private val dependencyContainer: DependencyContainer): ProvideViewModel {
        override fun <T : ViewModel> viewModel(clasz: Class<T>) =
            dependencyContainer.module(clasz).viewModel()
    }
}

interface ClearViewModel {
    fun clear(clasz: Class<out ViewModel>)
}