package com.anb.smartenhancer

import kotlin.math.max
import kotlin.math.roundToInt

/** Pure sizing policy shared by image decoding, output generation, and unit tests. */
object ImageSizing {
    const val MAX_WORKING_EDGE = 2048
    const val MAX_OUTPUT_EDGE = 4096
    const val FOUR_K_LONG_EDGE = 3840

    enum class OutputMode { STANDARD, TWO_X, FOUR_X, FOUR_K }

    data class Size(val width: Int, val height: Int) {
        init {
            require(width > 0 && height > 0) { "Image dimensions must be positive" }
        }
        val longEdge: Int get() = max(width, height)
    }

    fun calculateInSampleSize(width: Int, height: Int, maxEdge: Int = MAX_WORKING_EDGE): Int {
        require(width > 0 && height > 0) { "Invalid source dimensions: ${width}x$height" }
        require(maxEdge > 0) { "maxEdge must be positive" }
        val longEdge = max(width, height)
        var sample = 1
        while ((longEdge.toLong() + sample - 1L) / sample > maxEdge.toLong()) {
            // Dimensions come from image metadata and are Ints; this guard prevents overflow.
            if (sample > (1 shl 29)) break
            sample *= 2
        }
        return sample
    }

    fun outputSize(sourceWidth: Int, sourceHeight: Int, mode: OutputMode): Size {
        require(sourceWidth > 0 && sourceHeight > 0) { "Invalid source dimensions" }
        val source = Size(sourceWidth, sourceHeight)
        return when (mode) {
            OutputMode.STANDARD -> source
            OutputMode.TWO_X -> scaledAndCapped(source, 2L)
            OutputMode.FOUR_X -> scaledAndCapped(source, 4L)
            OutputMode.FOUR_K -> fitLongEdge(source, FOUR_K_LONG_EDGE)
        }
    }

    private fun scaledAndCapped(source: Size, multiplier: Long): Size {
        val targetWidth = source.width.toLong() * multiplier
        val targetHeight = source.height.toLong() * multiplier
        val targetLongEdge = max(targetWidth, targetHeight)
        val cap = MAX_OUTPUT_EDGE.toLong()
        val factor = minOf(1.0, cap.toDouble() / targetLongEdge.toDouble())
        return Size(
            (targetWidth * factor).roundToInt().coerceAtLeast(1),
            (targetHeight * factor).roundToInt().coerceAtLeast(1)
        )
    }

    private fun fitLongEdge(source: Size, longEdge: Int): Size {
        val factor = longEdge.toDouble() / source.longEdge.toDouble()
        return Size(
            (source.width * factor).roundToInt().coerceAtLeast(1),
            (source.height * factor).roundToInt().coerceAtLeast(1)
        )
    }
}
