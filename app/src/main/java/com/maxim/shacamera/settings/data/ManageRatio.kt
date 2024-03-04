package com.maxim.shacamera.settings.data

import com.maxim.shacamera.camera.data.ScreenSizeMode
import com.maxim.shacamera.core.data.SimpleStorage
import com.maxim.shacamera.core.presentation.Reload

interface ManageRatio {
    fun setCallback(reload: Reload)
    fun setRatio(position: Int)
    fun currentRatioPosition(): Int
    fun currentSizeMode(): ScreenSizeMode

    class Base(private val simpleStorage: SimpleStorage): ManageRatio {
        private var reload: Reload? = null

        override fun setCallback(reload: Reload) {
            this.reload = reload
        }

        override fun setRatio(position: Int) {
            simpleStorage.save(RATIO_KEY, position)
            reload?.reload()
        }

        override fun currentRatioPosition() =
            simpleStorage.read(RATIO_KEY, 0)

        override fun currentSizeMode(): ScreenSizeMode {
            return when (simpleStorage.read(RATIO_KEY, 2)) {
                0 -> ScreenSizeMode.FourToThree
                else -> ScreenSizeMode.SixteenToNine
            }
        }

        companion object {
            private const val RATIO_KEY = "RATIO_KEY"
        }
    }
}