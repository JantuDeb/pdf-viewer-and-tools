package com.thestudypath.pdfviewer.ui.results

import org.junit.Assert.assertEquals
import org.junit.Test

class ResultExportSupportTest {

    @Test
    fun buildSuggestedExportFileName_appendsSourceExtension_whenDisplayNameHasNoExtension() {
        val fileName = buildSuggestedExportFileName(
            displayName = "Merged Statement",
            sourceFileName = "statement.pdf",
        )

        assertEquals("Merged Statement.pdf", fileName)
    }

    @Test
    fun buildSuggestedExportFileName_keepsExplicitExtension_onDisplayName() {
        val fileName = buildSuggestedExportFileName(
            displayName = "notes.txt",
            sourceFileName = "report.pdf",
        )

        assertEquals("notes.txt", fileName)
    }

    @Test
    fun buildSuggestedExportFileName_sanitizesInvalidCharacters_andFallsBackToSourceName() {
        val fileName = buildSuggestedExportFileName(
            displayName = "  :Quarterly/Review*  ",
            sourceFileName = "review.pdf",
        )

        assertEquals("Quarterly Review.pdf", fileName)
    }

    @Test
    fun buildSuggestedExportFileName_usesSafeFallbackWhenInputsAreBlank() {
        val fileName = buildSuggestedExportFileName(
            displayName = "   ",
            sourceFileName = "   ",
        )

        assertEquals("exported-file", fileName)
    }
}

