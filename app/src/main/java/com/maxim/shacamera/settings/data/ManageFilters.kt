package com.maxim.shacamera.settings.data

import com.maxim.shacamera.core.data.SimpleStorage
import com.maxim.shacamera.core.presentation.Reload

interface ManageFilters {
    fun setCallback(reload: Reload)
    fun setFilter(value: Boolean, pos: Int)
    fun rtxIsOn(): Boolean

    class Base(private val simpleStorage: SimpleStorage) : ManageFilters {
        private var callback: Reload? = null

        override fun setCallback(reload: Reload) {
            callback = reload
        }

        override fun setFilter(value: Boolean, pos: Int) {
            when (pos) {
                0 -> simpleStorage.save(RTX_KEY, value)
            }
            callback?.reload()
        }

        override fun rtxIsOn() = simpleStorage.read(RTX_KEY, false)

        companion object {
            private const val RTX_KEY = "RTX_KEY"
        }
    }
}