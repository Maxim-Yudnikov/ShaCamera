package com.maxim.shacamera.core.sl

import android.content.Context
import com.maxim.shacamera.core.data.SimpleStorage
import com.maxim.shacamera.core.presentation.Navigation
import com.maxim.shacamera.settings.data.RatioManager

interface Core : ProvideNavigation, ProvideSimpleStorage, ProvideRatioManager {

    class Base(private val context: Context) : Core {
        private val navigation = Navigation.Base()
        override fun navigation() = navigation

        private val simpleStorage =
            SimpleStorage.Base(context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE))
        override fun simpleStorage() = simpleStorage

        private val ratioManager = RatioManager.Base(simpleStorage)
        override fun ratioManager() = ratioManager

        companion object {
            private const val SHARED_PREF_NAME = "shacamera-storage"
        }
    }
}

interface ProvideRatioManager {
    fun ratioManager(): RatioManager
}

interface ProvideNavigation {
    fun navigation(): Navigation.Mutable
}

interface ProvideSimpleStorage {
    fun simpleStorage(): SimpleStorage
}