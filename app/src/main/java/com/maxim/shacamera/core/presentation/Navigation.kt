package com.maxim.shacamera.core.presentation

interface Navigation {
    interface Update: Communication.Mutable<Screen>
    interface Observe: Communication.Mutable<Screen>
    interface Mutable: Update, Observe
    class Base: Communication.Single<Screen>(), Mutable
}