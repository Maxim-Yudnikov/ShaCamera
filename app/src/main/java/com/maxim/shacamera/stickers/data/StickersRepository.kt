package com.maxim.shacamera.stickers.data

import com.maxim.shacamera.R
import com.maxim.shacamera.stickers.presentation.StickerUi

interface StickersRepository {
    fun stickers(): List<StickerUi>

    class Base : StickersRepository {
        private val stickers = listOf(
            R.drawable.wednesday,
            R.drawable.smile,
            R.drawable.boom,
            R.drawable.cosy,
            R.drawable.dinner,
            R.drawable.fillips,
            R.drawable.moon,
            R.drawable.music,
            R.drawable.oh_no,
            R.drawable.rep,
            R.drawable.sauna,
            R.drawable.scuid_game,
            R.drawable.scuid_game_2,
            R.drawable.shershen,
            R.drawable.window,
            R.drawable.waffles,
            R.drawable.potato,
            R.drawable.roverbook,
            R.drawable.roverbook2,
            R.drawable.titan_channel,
            R.drawable.grifer,
            R.drawable.loading,
            R.drawable.artem,
            R.drawable.dama_pick,
            R.drawable.ad,
            R.drawable.boks,
            R.drawable.money,
            R.drawable.push_ups,
            R.drawable.wait,
            R.drawable.what,
            R.drawable.how,
            R.drawable.as_you_want,
            R.drawable.bread,
            R.drawable.cases,
            R.drawable.cooler,
            R.drawable.free_nitro,
            R.drawable.journal,
            R.drawable.sleng,
            R.drawable.sleng_2,
            R.drawable.wi_fi,
        )

        override fun stickers(): List<StickerUi> {
            val list = mutableListOf<StickerUi>()
            list.add(StickerUi.Random(stickers))
            list.addAll(stickers.map { StickerUi.Base(it) })
            return list
        }
    }
}