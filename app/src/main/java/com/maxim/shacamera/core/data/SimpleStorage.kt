package com.maxim.shacamera.core.data

import android.content.SharedPreferences

interface SimpleStorage {
    fun save(key: String, value: Int)
    fun read(key: String, default: Int): Int

    class Base(private val pref: SharedPreferences): SimpleStorage {
        override fun save(key: String, value: Int) {
            pref.edit().putInt(key, value).apply()
        }

        override fun read(key: String, default: Int) = pref.getInt(key, default)
    }
}