package com.maxim.shacamera.camera.data

import android.util.Size

class ComparableByArea : Comparator<Size> {
    override fun compare(o1: Size, o2: Size): Int {
        return (o1.height * o1.width) - (o2.height * o2.width)
    }
}