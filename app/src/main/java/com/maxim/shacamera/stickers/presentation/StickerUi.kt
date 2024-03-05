package com.maxim.shacamera.stickers.presentation

import android.widget.ImageView

interface StickerUi {
    fun showPreview(imageView: ImageView)
    fun onClick(listener: StickersAdapter.Listener)

    data class Base(private val drawableId: Int): StickerUi {
        override fun showPreview(imageView: ImageView) {
            imageView.setImageResource(drawableId)
        }

        override fun onClick(listener: StickersAdapter.Listener) {
            listener.onClick(drawableId)
        }
    }
}