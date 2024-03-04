package com.maxim.shacamera.core.sl

import android.content.Context
import com.maxim.shacamera.core.data.SimpleStorage
import com.maxim.shacamera.core.presentation.Navigation
import com.maxim.shacamera.settings.data.ManageFilters
import com.maxim.shacamera.settings.data.ManageRatio

interface Core : ProvideNavigation, ProvideSimpleStorage, ProvideManageRatio, ProvideManageFilters {

    class Base(private val context: Context) : Core {
        private val navigation = Navigation.Base()
        override fun navigation() = navigation

        private val simpleStorage =
            SimpleStorage.Base(context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE))
        override fun simpleStorage() = simpleStorage

        private val manageRatio = ManageRatio.Base(simpleStorage)
        override fun manageRatio() = manageRatio

        private val manageFilters = ManageFilters.Base(simpleStorage)
        override fun manageFilters() = manageFilters

        companion object {
            private const val SHARED_PREF_NAME = "shacamera-storage"
        }
    }
}

interface ProvideManageRatio {
    fun manageRatio(): ManageRatio
}

interface ProvideManageFilters {
    fun manageFilters(): ManageFilters
}

interface ProvideNavigation {
    fun navigation(): Navigation.Mutable
}

interface ProvideSimpleStorage {
    fun simpleStorage(): SimpleStorage
}