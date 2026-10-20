package com.anb.smartenhancer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageSizingTest {
    @Test fun samplingKeepsWorkingLongEdgeAtOrBelowLimit() {
        assertEquals(1, ImageSizing.calculateInSampleSize(2048, 1536))
        assertEquals(2, ImageSizing.calculateInSampleSize(4096, 3072))
        assertEquals(4, ImageSizing.calculateInSampleSize(4097, 3000))
        assertEquals(8, ImageSizing.calculateInSampleSize(12000, 8000))
    }

    @Test fun invalidImageDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ImageSizing.calculateInSampleSize(0, 100)
        }
    }

    @Test fun standardOutputPreservesDecodedWorkingDimensions() {
        assertEquals(
            ImageSizing.Size(1600, 900),
            ImageSizing.outputSize(1600, 900, ImageSizing.OutputMode.STANDARD)
        )
    }

    @Test fun twoTimesUpscaleUsesRequestedFactorWhenUnderCap() {
        assertEquals(
            ImageSizing.Size(3000, 2000),
            ImageSizing.outputSize(1500, 1000, ImageSizing.OutputMode.TWO_X)
        )
    }

    @Test fun fourTimesUpscaleIsCappedAt4096LongEdge() {
        val size = ImageSizing.outputSize(2048, 1024, ImageSizing.OutputMode.FOUR_X)
        assertEquals(4096, size.width)
        assertEquals(2048, size.height)
        assertEquals(4096, size.longEdge)
    }

    @Test fun fourKOutputHas3840LongEdgeAndPreservesAspectRatioApproximately() {
        val size = ImageSizing.outputSize(1600, 900, ImageSizing.OutputMode.FOUR_K)
        assertEquals(3840, size.width)
        assertEquals(2160, size.height)
    }

    @Test fun portraitFourKOutputKeepsPortraitOrientation() {
        val size = ImageSizing.outputSize(900, 1600, ImageSizing.OutputMode.FOUR_K)
        assertEquals(2160, size.width)
        assertEquals(3840, size.height)
    }
}
