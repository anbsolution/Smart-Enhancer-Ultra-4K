package com.anb.smartenhancer

import kotlin.math.roundToInt

/** Platform-independent ARGB contrast/sharpening kernel, kept pure for deterministic JVM tests. */
object PixelEnhancer {
    fun transformArgb(input: IntArray, width: Int, height: Int, strength: Float, stronger: Boolean): IntArray {
        require(width > 0 && height > 0) { "Image dimensions must be positive" }
        require(input.size == Math.multiplyExact(width, height)) { "Pixel buffer size does not match dimensions" }
        require(strength.isFinite() && strength >= 0f) { "Strength must be finite and non-negative" }

        val output = IntArray(input.size)
        val contrast = 1f + (if (stronger) 0.23f else 0.12f) * strength
        val sharpness = (if (stronger) 0.62f else 0.30f) * strength

        fun adjust(value: Int): Int = ((value - 128) * contrast + 128).roundToInt().coerceIn(0, 255)
        fun sharpen(center: Int, up: Int, down: Int, left: Int, right: Int): Int {
            val c = adjust(center).toFloat()
            val average = (adjust(up) + adjust(down) + adjust(left) + adjust(right)) / 4f
            return (c + (c - average) * sharpness).roundToInt().coerceIn(0, 255)
        }

        for (y in 0 until height) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Image processing cancelled")
            val row = y * width
            for (x in 0 until width) {
                val i = row + x
                val pixel = input[i]
                val up = input[if (y > 0) i - width else i]
                val down = input[if (y < height - 1) i + width else i]
                val left = input[if (x > 0) i - 1 else i]
                val right = input[if (x < width - 1) i + 1 else i]
                val alpha = (pixel ushr 24) and 0xff
                val red = sharpen((pixel ushr 16) and 0xff, (up ushr 16) and 0xff, (down ushr 16) and 0xff, (left ushr 16) and 0xff, (right ushr 16) and 0xff)
                val green = sharpen((pixel ushr 8) and 0xff, (up ushr 8) and 0xff, (down ushr 8) and 0xff, (left ushr 8) and 0xff, (right ushr 8) and 0xff)
                val blue = sharpen(pixel and 0xff, up and 0xff, down and 0xff, left and 0xff, right and 0xff)
                output[i] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }
        return output
    }

}
