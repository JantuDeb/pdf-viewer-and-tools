package com.thestudypath.pdfviewer.processing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfBoxProcessingEngineVisibleSignaturePlacementTest {

    @Test
    fun resolveVisibleSignatureRect_placesSignatureInBottomRight() {
        val rect = resolveVisibleSignatureRect(
            pageWidth = 600f,
            pageHeight = 800f,
            imageWidth = 400f,
            imageHeight = 100f,
            widthPercent = 25,
            marginPercent = 4,
            placement = VisibleSignaturePlacement.BottomRight,
        )

        assertEquals(150f, rect.width)
        assertEquals(37.5f, rect.height)
        assertEquals(426f, rect.x, 0.01f)
        assertEquals(32f, rect.y, 0.01f)
    }

    @Test
    fun resolveVisibleSignatureRect_centersSignatureWithinPage() {
        val rect = resolveVisibleSignatureRect(
            pageWidth = 612f,
            pageHeight = 792f,
            imageWidth = 300f,
            imageHeight = 150f,
            widthPercent = 20,
            marginPercent = 4,
            placement = VisibleSignaturePlacement.Center,
        )

        assertEquals((612f - rect.width) / 2f, rect.x, 0.01f)
        assertEquals((792f - rect.height) / 2f, rect.y, 0.01f)
    }

    @Test
    fun resolveVisibleSignatureRect_keepsSignatureInsideShortPage() {
        val rect = resolveVisibleSignatureRect(
            pageWidth = 300f,
            pageHeight = 120f,
            imageWidth = 150f,
            imageHeight = 100f,
            widthPercent = 40,
            marginPercent = 8,
            placement = VisibleSignaturePlacement.TopLeft,
        )

        assertTrue(rect.x >= 0f)
        assertTrue(rect.y >= 0f)
        assertTrue(rect.x + rect.width <= 300f + 0.01f)
        assertTrue(rect.y + rect.height <= 120f + 0.01f)
    }
}

