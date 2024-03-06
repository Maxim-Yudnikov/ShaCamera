package com.maxim.shacamera.settings.data

import com.maxim.shacamera.camera.presentation.CameraFilter
import com.maxim.shacamera.core.data.SimpleStorage
import com.maxim.shacamera.core.presentation.Reload

interface ManageFilters {
    fun setCallback(reload: Reload)

    fun setRxt(value: Int)
    fun setDlss(value: Int)
    fun setFsr(value: Int)

    fun rtxMode(): Int
    fun dlssMode(): Int
    fun fsrMode(): Int

    fun allCameraFilters(): List<CameraFilter>

    class Base(private val simpleStorage: SimpleStorage) : ManageFilters {
        private var callback: Reload? = null

        override fun setCallback(reload: Reload) {
            callback = reload
        }

        override fun setRxt(value: Int) {
            simpleStorage.save(RTX_KEY, value)
        }

        override fun setDlss(value: Int) {
            simpleStorage.save(DLSS_KEY, value)
            callback?.reload()
        }

        override fun setFsr(value: Int) {
            simpleStorage.save(FSR_KEY, value)
        }

        override fun rtxMode() = simpleStorage.read(RTX_KEY, 0)
        override fun dlssMode() = simpleStorage.read(DLSS_KEY, 0)
        override fun fsrMode() = simpleStorage.read(FSR_KEY, 0)

        override fun allCameraFilters(): List<CameraFilter> {
            val list = mutableListOf<CameraFilter>()
            if (fsrMode() != 0)
                list.add(CameraFilter.Fsr(fsrMode() == 1))
            if (rtxMode() != 0)
                list.add(CameraFilter.Rtx(rtxMode() == 1))
            if (dlssMode() != 0)
                list.add(CameraFilter.Dlss(dlssMode() == 1))
            return list
        }

        companion object {
            private const val RTX_KEY = "RTX_KEY"
            private const val DLSS_KEY = "DLSS_KEY"
            private const val FSR_KEY = "FSR_KEY"
        }
    }
}