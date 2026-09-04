package com.thestudypath.pdfviewer.ui.tools.watermark

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatermarkPdfSupportTest {

    @Test
    fun parseWatermarkPageRangesInput_defaultsToAllPagesWhenBlank() {
        val ranges = parseWatermarkPageRangesInput(
            rawValue = "   ",
            totalPageCount = 7,
        )

        assertEquals(listOf(PdfPageRange(1, 7)), ranges)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseWatermarkPageRangesInput_rejectsOverlappingRanges() {
        parseWatermarkPageRangesInput(
            rawValue = "1-2, 2-4",
            totalPageCount = 9,
        )
    }

    @Test
    fun buildDefaultWatermarkedPdfFileName_usesDocumentName() {
        val fileName = buildDefaultWatermarkedPdfFileName(document("Quarterly Report.pdf"))

        assertEquals("Quarterly Report-watermarked.pdf", fileName)
    }

    @Test
    fun buildInternalWatermarkPdfFileName_keepsPdfExtension() {
        val fileName = buildInternalWatermarkPdfFileName(
            displayFileName = "Quarterly Report Final.pdf",
            timestampMillis = 42L,
        )

        assertEquals("watermarked_42_Quarterly_Report_Final.pdf", fileName)
        assertTrue(fileName.endsWith(".pdf"))
    }

    private fun document(displayName: String) = DocumentItem(
        id = displayName,
        fileName = displayName,
        displayName = displayName,
        filePath = "C:/docs/$displayName",
        sizeBytes = 1024,
        pageCount = 7,
        lastOpenedAt = 1L,
        lastModifiedAt = 1L,
    )
}

