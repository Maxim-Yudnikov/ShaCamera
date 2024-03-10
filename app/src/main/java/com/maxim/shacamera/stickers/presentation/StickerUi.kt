package com.maxim.shacamera.stickers.presentation

import android.widget.ImageView
import com.maxim.shacamera.R

interface StickerUi {
    fun showPreview(imageView: ImageView)
    fun onClick(listener: StickersAdapter.Listener)

    data class Random(private val list: List<Int>): StickerUi {
        override fun showPreview(imageView: ImageView) {
            imageView.setImageResource(R.drawable.random)
        }

        override fun onClick(listener: StickersAdapter.Listener) {
            listener.onClick(list.random())
        }
    }

    data class Base(private val drawableId: Int): StickerUi {
        override fun showPreview(imageView: ImageView) {
            imageView.setImageResource(drawableId)
        }

        override fun onClick(listener: StickersAdapter.Listener) {
            listener.onClick(drawableId)
        }
    }
}