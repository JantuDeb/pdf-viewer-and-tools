package com.thestudypath.pdfviewer.ui.tools.merge

import com.thestudypath.pdfviewer.catalog.DocumentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MergePdfSupportTest {

    @Test
    fun resolveMergeSelection_preservesExistingOrder_andAppendsNewSelections() {
        val first = document("1", "Alpha.pdf")
        val second = document("2", "Beta.pdf")
        val third = document("3", "Gamma.pdf")

        val resolved = resolveMergeSelection(
            currentSelection = listOf(third, first),
            allDocuments = listOf(first, second, third),
            selectedDocumentIds = setOf(first.id, second.id, third.id),
        )

        assertEquals(listOf(third.id, first.id, second.id), resolved.map { it.id })
    }

    @Test
    fun normalizeMergeOutputName_addsPdfExtension_andStripsInvalidCharacters() {
        assertEquals(
            "Quarterly Report Final.pdf",
            normalizeMergeOutputName("Quarterly/Report:*Final"),
        )
    }

    @Test
    fun buildInternalMergeFileName_createsStablePdfFileName() {
        val fileName = buildInternalMergeFileName(
            displayFileName = "Team Notes.pdf",
            timestampMillis = 42L,
        )

        assertEquals("merged_42_Team_Notes.pdf", fileName)
        assertTrue(fileName.endsWith(".pdf"))
    }

    private fun document(id: String, displayName: String) = DocumentItem(
        id = id,
        fileName = displayName,
        displayName = displayName,
        filePath = "C:/docs/$displayName",
        sizeBytes = 1024,
        pageCount = 2,
        lastOpenedAt = 1L,
        lastModifiedAt = 1L,
    )
}

