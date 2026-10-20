package com.anb.smartenhancer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PixelEnhancerTest {
    @Test fun flatMiddleGrayImageRemainsStable() {
        val input = IntArray(9) { 0xff808080.toInt() }
        val output = PixelEnhancer.transformArgb(input, 3, 3, 1f, false)
        assertEquals(input.toList(), output.toList())
    }

    @Test fun alphaChannelIsPreserved() {
        val input = intArrayOf(0x20808080, 0x40808080, 0x60808080, 0x80808080L.toInt())
        val output = PixelEnhancer.transformArgb(input, 2, 2, 1f, true)
        input.indices.forEach { index ->
            assertEquals((input[index] ushr 24) and 0xff, (output[index] ushr 24) and 0xff)
        }
    }

    @Test fun strongerModeChangesEdgeEnhancementMoreThanFastMode() {
        val input = IntArray(9) { 0xff808080.toInt() }
        input[4] = 0xffa0a0a0.toInt()
        val fast = PixelEnhancer.transformArgb(input, 3, 3, 1f, false)
        val stronger = PixelEnhancer.transformArgb(input, 3, 3, 1f, true)
        assertNotEquals(fast[4], stronger[4])
        assert((stronger[4] and 0xff) > (fast[4] and 0xff))
    }

    @Test fun inputBufferIsNotModified() {
        val input = intArrayOf(0xff000000.toInt(), 0xffffffff.toInt(), 0xff808080.toInt(), 0xff404040.toInt())
        val originalCopy = input.copyOf()
        PixelEnhancer.transformArgb(input, 2, 2, 0.8f, true)
        assertEquals(originalCopy.toList(), input.toList())
    }

    @Test fun invalidBufferDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PixelEnhancer.transformArgb(intArrayOf(0), 2, 2, 1f, false)
        }
    }

    @Test fun negativeStrengthIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PixelEnhancer.transformArgb(intArrayOf(0xff808080.toInt()), 1, 1, -1f, false)
        }
    }
}
