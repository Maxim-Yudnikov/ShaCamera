package com.maxim.shacamera.core.presentation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

interface Communication {
    interface Update<T> {
        fun update(value: T)
    }

    interface Observe<T> {
        fun observe(owner: LifecycleOwner, observer: Observer<T>)
    }

    interface Mutable<T>: Update<T>, Observe<T>

    abstract class Abstract<T>(protected val liveData: MutableLiveData<T>): Mutable<T> {
        override fun update(value: T) {
            liveData.value = value
        }

        override fun observe(owner: LifecycleOwner, observer: Observer<T>) {
            liveData.observe(owner, observer)
        }
    }

    abstract class Regular<T>: Abstract<T>(MutableLiveData())
    abstract class Single<T>: Abstract<T>(SingleLiveEvent())
}