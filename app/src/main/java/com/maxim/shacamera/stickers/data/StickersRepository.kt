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
                StickerUi.Base(R.drawable.ad),
                StickerUi.Base(R.drawable.boks),
                StickerUi.Base(R.drawable.money),
                StickerUi.Base(R.drawable.push_ups),
                StickerUi.Base(R.drawable.wait),
                StickerUi.Base(R.drawable.what),
                StickerUi.Base(R.drawable.how),
                StickerUi.Base(R.drawable.as_you_want),
                StickerUi.Base(R.drawable.bread),
                StickerUi.Base(R.drawable.cases),
                StickerUi.Base(R.drawable.cooler),
                StickerUi.Base(R.drawable.free_nitro),
                StickerUi.Base(R.drawable.journal),
                StickerUi.Base(R.drawable.sleng),
                StickerUi.Base(R.drawable.sleng_2),
                StickerUi.Base(R.drawable.wi_fi),
            )
        }
    }
}