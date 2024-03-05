package com.maxim.shacamera.settings.data

import com.maxim.shacamera.camera.presentation.CameraFilter
import com.maxim.shacamera.core.data.SimpleStorage
import com.maxim.shacamera.core.presentation.Reload

interface ManageFilters {
    fun setCallback(reload: Reload)

    fun setRxt(value: Boolean)
    fun setDlss(value: Boolean)
    fun setFsr(value: Boolean)

    fun rtxIsOn(): Boolean
    fun dlssIsOn(): Boolean
    fun fsrIsOn(): Boolean

    fun allCameraFilters(): List<CameraFilter>

    class Base(private val simpleStorage: SimpleStorage) : ManageFilters {
        private var callback: Reload? = null

        override fun setCallback(reload: Reload) {
            callback = reload
        }

        override fun setRxt(value: Boolean) {
            simpleStorage.save(RTX_KEY, value)
        }

        override fun setDlss(value: Boolean) {
            simpleStorage.save(DLSS_KEY, value)
            callback?.reload()
        }

        override fun setFsr(value: Boolean) {
            simpleStorage.save(FSR_KEY, value)
        }

        override fun rtxIsOn() = simpleStorage.read(RTX_KEY, false)
        override fun dlssIsOn() = simpleStorage.read(DLSS_KEY, false)
        override fun fsrIsOn() = simpleStorage.read(FSR_KEY, false)

        override fun allCameraFilters(): List<CameraFilter> {
            val list = mutableListOf<CameraFilter>()
            if (fsrIsOn())
                list.add(CameraFilter.Fsr)
            if (rtxIsOn())
                list.add(CameraFilter.Rtx)
            if (dlssIsOn())
                list.add(CameraFilter.Dlss)
            return list
        }

        companion object {
            private const val RTX_KEY = "RTX_KEY"
            private const val DLSS_KEY = "DLSS_KEY"
            private const val FSR_KEY = "FSR_KEY"
        }
    }
}