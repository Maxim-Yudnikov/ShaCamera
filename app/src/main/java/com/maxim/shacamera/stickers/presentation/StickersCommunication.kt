package com.maxim.shacamera.stickers.presentation

import com.maxim.shacamera.core.presentation.Communication

interface StickersCommunication: Communication.Mutable<StickersState> {
    class Base: Communication.Regular<StickersState>(), StickersCommunication
}