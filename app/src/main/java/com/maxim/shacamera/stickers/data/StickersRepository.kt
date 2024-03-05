package com.maxim.shacamera.stickers.data

import com.maxim.shacamera.R
import com.maxim.shacamera.stickers.presentation.StickerUi

interface StickersRepository {
    fun stickers(): List<StickerUi>

    class Base : StickersRepository {
        override fun stickers(): List<StickerUi> {
            return listOf(
                StickerUi.Base(R.drawable.wednesday),
                StickerUi.Base(R.drawable.smile),
                StickerUi.Base(R.drawable.boom),
                StickerUi.Base(R.drawable.cosy),
                StickerUi.Base(R.drawable.dinner),
                StickerUi.Base(R.drawable.fillips),
                StickerUi.Base(R.drawable.moon),
                StickerUi.Base(R.drawable.music),
                StickerUi.Base(R.drawable.oh_no),
                StickerUi.Base(R.drawable.rep),
                StickerUi.Base(R.drawable.sauna),
                StickerUi.Base(R.drawable.scuid_game),
                StickerUi.Base(R.drawable.scuid_game_2),
                StickerUi.Base(R.drawable.shershen),
                StickerUi.Base(R.drawable.window),
                StickerUi.Base(R.drawable.waffles),
                StickerUi.Base(R.drawable.potato),
                StickerUi.Base(R.drawable.roverbook),
                StickerUi.Base(R.drawable.roverbook2),
                StickerUi.Base(R.drawable.titan_channel),
                StickerUi.Base(R.drawable.grifer),
                StickerUi.Base(R.drawable.loading),
                StickerUi.Base(R.drawable.artem),
                StickerUi.Base(R.drawable.dama_pick),
            )
        }
    }
}