package com.maxim.shacamera

import android.util.Size
import kotlin.math.sign

class CompareSizeByArea: Comparator<Size> {
    override fun compare(o1: Size?, o2: Size?): Int {
        return sign((o1!!.width * o1.height / o2!!.width * o2.height).toFloat()).toInt()
    }
}