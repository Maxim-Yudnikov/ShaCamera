package com.maxim.shacamera

import android.media.Image
import java.io.File
import java.io.FileOutputStream

class ImageServer(private val image: Image, private val file: File): Runnable {

    override fun run() {
        val buffer = image.planes.first().buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file)
            output.write(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close()
            output?.close()
        }
    }
}