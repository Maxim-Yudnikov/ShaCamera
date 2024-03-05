package com.maxim.shacamera.stickers.data

interface StickersSharedCommunication {
    interface Read {
        fun setCallback(callback: ShowSticker)
    }

    interface Call {
        fun showSticker(drawableId: Int)
    }

    interface Mutable : Read, Call


    class Base : Mutable {
        private var callback: ShowSticker? = null

        override fun setCallback(callback: ShowSticker) {
            this.callback = callback
        }

        override fun showSticker(drawableId: Int) {
            callback?.showSticker(drawableId)
        }
    }
}

interface ShowSticker {
    fun showSticker(drawableId: Int)
}