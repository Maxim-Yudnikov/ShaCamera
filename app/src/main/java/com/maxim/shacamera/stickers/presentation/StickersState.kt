package com.maxim.shacamera.stickers.presentation

interface StickersState {
    fun show(adapter: StickersAdapter)

    class Base(private val list: List<StickerUi>): StickersState {
        override fun show(adapter: StickersAdapter) {
            adapter.update(list)
        }
    }
}