package com.maxim.shacamera.camera.presentation

import android.os.Bundle
import java.io.Serializable

interface BundleWrapper {
    interface Save {
        fun <T: Serializable> save(key: String, value: T)
    }

    interface Restore {
        fun <T: Serializable?> restore(key: String): T?
    }

    interface Mutable: Save, Restore

    class Base(private val bundle: Bundle): Mutable {
        override fun <T : Serializable> save(key: String, value: T) {
            bundle.putSerializable(key, value)
        }

        override fun <T : Serializable?> restore(key: String): T? {
            return bundle.getSerializable(key) as T
        }
    }
}