package com.maxim.shacamera.settings.data

import com.maxim.shacamera.camera.filters.CameraFilter
import com.maxim.shacamera.core.data.SimpleStorage
import com.maxim.shacamera.core.presentation.Reload

interface ManageFilters {
    fun setCallback(reload: Reload)
    fun setRxt(value: Boolean)
    fun rtxIsOn(): Boolean

    fun allCameraFilters(): List<CameraFilter>

    class Base(private val simpleStorage: SimpleStorage) : ManageFilters {
        private var callback: Reload? = null

        override fun setCallback(reload: Reload) {
            callback = reload
        }

        override fun setRxt(value: Boolean) {
            simpleStorage.save(RTX_KEY, value)
            callback?.reload()
        }

        override fun rtxIsOn() = simpleStorage.read(RTX_KEY, false)

        override fun allCameraFilters(): List<CameraFilter> {
            val list = mutableListOf<CameraFilter>()
            if (rtxIsOn())
                list.add(CameraFilter.Rtx())
            return list
        }

        companion object {
            private const val RTX_KEY = "RTX_KEY"
        }
    }
}