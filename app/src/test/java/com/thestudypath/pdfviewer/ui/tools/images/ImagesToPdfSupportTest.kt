package com.thestudypath.pdfviewer.ui.tools.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagesToPdfSupportTest {

    @Test
    fun buildDefaultImagesPdfName_usesFirstImageAndCount() {
        val outputName = buildDefaultImagesPdfName(
            listOf("Receipt.jpg", "Page 2.png", "Page 3.png"),
        )

        assertEquals("Receipt-3-images.pdf", outputName)
    }

    @Test
    fun normalizeImagesPdfName_addsExtension_andStripsInvalidCharacters() {
        assertEquals(
            "Travel Album Final.pdf",
            normalizeImagesPdfName("Travel/Album:*Final"),
        )
    }

    @Test
    fun buildInternalImagesPdfFileName_createsStablePdfFileName() {
        val fileName = buildInternalImagesPdfFileName(
            displayFileName = "Scanned Notes.pdf",
            timestampMillis = 42L,
        )

        assertEquals("images_42_Scanned_Notes.pdf", fileName)
        assertTrue(fileName.endsWith(".pdf"))
    }
}

