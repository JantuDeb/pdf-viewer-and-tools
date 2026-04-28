package com.thestudypath.pdfviewer.ui.tools.compress

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfCompressionProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressPdfSupportTest {

    @Test
    fun buildDefaultCompressedFileName_usesDocumentName() {
        val outputName = buildDefaultCompressedFileName(document("Quarterly Report.pdf"))

        assertEquals("Quarterly Report-compressed.pdf", outputName)
    }

    @Test
    fun normalizeCompressedFileName_addsExtension_andStripsInvalidCharacters() {
        assertEquals(
            "My Report Final.pdf",
            normalizeCompressedFileName("My/Report:*Final"),
        )
    }

    @Test
    fun buildInternalCompressedFileName_createsStablePdfFileName() {
        val fileName = buildInternalCompressedFileName(
            displayFileName = "Client Pack.pdf",
            timestampMillis = 42L,
        )

        assertEquals("compressed_42_Client_Pack.pdf", fileName)
        assertTrue(fileName.endsWith(".pdf"))
    }

    @Test
    fun formatFileSize_formatsMegabytes() {
        assertEquals("1.5 MB", formatFileSize((1.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun profileLabel_returnsExpectedText() {
        assertEquals("Light", profileLabel(PdfCompressionProfile.Light))
        assertEquals("Balanced", profileLabel(PdfCompressionProfile.Balanced))
        assertEquals("Strong", profileLabel(PdfCompressionProfile.Strong))
    }

    @Test
    fun profileGuidance_mentionsTradeoff() {
        assertTrue(profileGuidance(PdfCompressionProfile.Light).contains("quality"))
        assertTrue(profileGuidance(PdfCompressionProfile.Balanced).contains("recommended"))
        assertTrue(profileGuidance(PdfCompressionProfile.Strong).contains("reduction"))
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

