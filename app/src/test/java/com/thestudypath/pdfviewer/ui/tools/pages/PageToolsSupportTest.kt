package com.thestudypath.pdfviewer.ui.tools.pages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageToolsSupportTest {

    @Test
    fun parsePageRangesInput_acceptsMixedPagesAndRanges() {
        val ranges = parsePageRangesInput(
            rawValue = "1-3, 5, 8-10",
            totalPageCount = 10,
        )

        assertEquals(
            listOf(
                PdfPageRange(1, 3),
                PdfPageRange(5, 5),
                PdfPageRange(8, 10),
            ),
            ranges,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parsePageRangesInput_rejectsOverlaps() {
        parsePageRangesInput(
            rawValue = "1-3, 3-4",
            totalPageCount = 8,
        )
    }

    @Test
    fun buildDefaultDeletedPagesPdfFileName_usesDocumentName() {
        val fileName = buildDefaultDeletedPagesPdfFileName(document("Packet.pdf"))

        assertEquals("Packet-deleted-pages.pdf", fileName)
    }

    @Test
    fun buildInternalPageToolFileName_createsStablePdfFileName() {
        val fileName = buildInternalPageToolFileName(
            prefix = "rotated",
            displayFileName = "Packet Final.pdf",
            fallbackBaseName = "rotated-document",
            timestampMillis = 42L,
        )

        assertEquals("rotated_42_Packet_Final.pdf", fileName)
        assertTrue(fileName.endsWith(".pdf"))
    }

    private fun document(displayName: String) = DocumentItem(
        id = displayName,
        fileName = displayName,
        displayName = displayName,
        filePath = "C:/docs/$displayName",
        sizeBytes = 1024,
        pageCount = 8,
        lastOpenedAt = 1L,
        lastModifiedAt = 1L,
    )
}

