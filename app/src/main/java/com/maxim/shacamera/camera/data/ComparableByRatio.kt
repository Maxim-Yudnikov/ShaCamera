package com.maxim.shacamera.camera.data

import android.util.Size
import kotlin.math.abs

class ComparableByRatio(private val ratio: Double) : Comparator<Size> {
    override fun compare(o1: Size, o2: Size): Int {
        val r = abs((o2.width.toDouble() / o2.height) - ratio) - abs((o1.width.toDouble() / o1.height) - ratio)
        return r.compareTo(0)
    }
}