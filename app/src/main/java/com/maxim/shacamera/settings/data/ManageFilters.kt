package com.maxim.shacamera.settings.data

import com.maxim.shacamera.camera.filters.CameraFilter
import com.maxim.shacamera.core.data.SimpleStorage

interface ManageFilters {
    fun setRxt(value: Boolean)
    fun setDlss(value: Boolean)
    fun setFsr(value: Boolean)

    fun rtxIsOn(): Boolean
    fun dlssIsOn(): Boolean
    fun fsrIsOn(): Boolean

    fun allCameraFilters(): List<CameraFilter>

    class Base(private val simpleStorage: SimpleStorage) : ManageFilters {

        override fun setRxt(value: Boolean) {
            simpleStorage.save(RTX_KEY, value)
        }

        override fun setDlss(value: Boolean) {
            simpleStorage.save(DLSS_KEY, value)
        }

        override fun setFsr(value: Boolean) {
            simpleStorage.save(FSR_KEY, value)
        }

        override fun rtxIsOn() = simpleStorage.read(RTX_KEY, false)
        override fun dlssIsOn() = simpleStorage.read(DLSS_KEY, false)
        override fun fsrIsOn() = simpleStorage.read(FSR_KEY, false)

        override fun allCameraFilters(): List<CameraFilter> {
            val list = mutableListOf<CameraFilter>()
            if (rtxIsOn())
                list.add(CameraFilter.Rtx)
            if (dlssIsOn())
                list.add(CameraFilter.Dlss)
            if (fsrIsOn())
                list.add(CameraFilter.Fsr)
            return list
        }

        companion object {
            private const val RTX_KEY = "RTX_KEY"
            private const val DLSS_KEY = "DLSS_KEY"
            private const val FSR_KEY = "FSR_KEY"
        }
    }
}