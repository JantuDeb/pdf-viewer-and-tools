package com.thestudypath.pdfviewer.ui.tools.pdfimages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat
import com.thestudypath.pdfviewer.processing.PdfPageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfToImagesSupportTest {

    @Test
    fun parsePdfToImagesPageRangesInput_defaultsToAllPagesWhenBlank() {
        val ranges = parsePdfToImagesPageRangesInput(
            rawValue = "   ",
            totalPageCount = 6,
        )

        assertEquals(listOf(PdfPageRange(1, 6)), ranges)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parsePdfToImagesPageRangesInput_rejectsOverlappingRanges() {
        parsePdfToImagesPageRangesInput(
            rawValue = "1-2, 2-3",
            totalPageCount = 8,
        )
    }

    @Test
    fun buildDefaultPdfImagesFolderName_usesDocumentName() {
        val folderName = buildDefaultPdfImagesFolderName(document("Quarterly Report.pdf"))

        assertEquals("Quarterly Report-images", folderName)
    }

    @Test
    fun buildInternalPdfImagesDirectoryName_createsStableFolderName() {
        val directoryName = buildInternalPdfImagesDirectoryName(
            displayFolderName = "Quarterly Report Final",
            timestampMillis = 42L,
        )

        assertEquals("pdf_images_42_Quarterly_Report_Final", directoryName)
    }

    @Test
    fun mimeType_returnsExpectedValue() {
        assertEquals("image/png", PdfImageExportFormat.Png.mimeType())
        assertEquals("image/jpeg", PdfImageExportFormat.Jpeg.mimeType())
        assertTrue(PdfImageExportFormat.Jpeg.label().contains("JPEG"))
    }

    private fun document(displayName: String) = DocumentItem(
        id = displayName,
        fileName = displayName,
        displayName = displayName,
        filePath = "C:/docs/$displayName",
        sizeBytes = 1024,
        pageCount = 6,
        lastOpenedAt = 1L,
        lastModifiedAt = 1L,
    )
}

