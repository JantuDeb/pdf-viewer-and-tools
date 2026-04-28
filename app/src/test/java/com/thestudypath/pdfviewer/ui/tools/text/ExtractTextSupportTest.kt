package com.thestudypath.pdfviewer.ui.tools.text

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractTextSupportTest {

    @Test
    fun buildDefaultExtractedTextFileName_usesDocumentName() {
        val fileName = buildDefaultExtractedTextFileName(document("Quarterly Report.pdf"))

        assertEquals("Quarterly Report-text.txt", fileName)
    }

    @Test
    fun resolveExtractionPageRange_allPages_usesWholeDocument() {
        val pageRange = resolveExtractionPageRange(
            scope = TextExtractionScope.AllPages,
            totalPageCount = 9,
            startPageInput = "",
            endPageInput = "",
        )

        assertEquals(PdfPageRange(1, 9), pageRange)
    }

    @Test
    fun resolveExtractionPageRange_pageRange_usesUserInput() {
        val pageRange = resolveExtractionPageRange(
            scope = TextExtractionScope.PageRange,
            totalPageCount = 12,
            startPageInput = "3",
            endPageInput = "7",
        )

        assertEquals(PdfPageRange(3, 7), pageRange)
    }

    @Test(expected = IllegalArgumentException::class)
    fun resolveExtractionPageRange_rejectsInvalidRange() {
        resolveExtractionPageRange(
            scope = TextExtractionScope.PageRange,
            totalPageCount = 5,
            startPageInput = "4",
            endPageInput = "7",
        )
    }

    @Test
    fun buildInternalExtractedTextFileName_createsStableTxtFileName() {
        val fileName = buildInternalExtractedTextFileName(
            displayFileName = "Meeting Notes.txt",
            timestampMillis = 42L,
        )

        assertEquals("text_42_Meeting_Notes.txt", fileName)
        assertTrue(fileName.endsWith(".txt"))
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

