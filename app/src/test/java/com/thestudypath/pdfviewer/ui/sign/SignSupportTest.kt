package com.thestudypath.pdfviewer.ui.sign

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.VisibleSignaturePlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class SignSupportTest {

    @Test
    fun buildDefaultSignedPdfFileName_usesDocumentName() {
        val fileName = buildDefaultSignedPdfFileName(document("Agreement.pdf"))

        assertEquals("Agreement-signed.pdf", fileName)
    }

    @Test
    fun normalizeSignedPdfFileName_addsExtension_andStripsInvalidCharacters() {
        val fileName = normalizeSignedPdfFileName("Client/Agreement:*Final")

        assertEquals("Client Agreement Final.pdf", fileName)
    }

    @Test
    fun resolveSigningPageNumber_rejectsOutOfBoundsPage() {
        try {
            resolveSigningPageNumber(rawValue = "9", totalPages = 4)
            error("Expected invalid page error")
        } catch (expected: IllegalArgumentException) {
            assertEquals("Page 9 is outside this document's 4 pages.", expected.message)
        }
    }

    @Test
    fun placementLabel_returnsFriendlyText() {
        assertEquals("Top left", placementLabel(VisibleSignaturePlacement.TopLeft))
        assertEquals("Bottom right", placementLabel(VisibleSignaturePlacement.BottomRight))
        assertEquals("Center", placementLabel(VisibleSignaturePlacement.Center))
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

