package com.maxim.shacamera.core.data

import android.content.SharedPreferences

interface SimpleStorage {
    fun save(key: String, value: Int)
    fun read(key: String, default: Int): Int

    fun save(key: String, value: Boolean)
    fun read(key: String, default: Boolean): Boolean

    class Base(private val pref: SharedPreferences): SimpleStorage {
        override fun save(key: String, value: Int) {
            pref.edit().putInt(key, value).apply()
        }

        override fun save(key: String, value: Boolean) {
            pref.edit().putBoolean(key, value).apply()
        }

        override fun read(key: String, default: Int) = pref.getInt(key, default)
        override fun read(key: String, default: Boolean) = pref.getBoolean(key, default)
    }
}