package com.thestudypath.pdfviewer.ui.tools.embeddedimages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExtractEmbeddedImagesSupportTest {

    @Test
    fun parseEmbeddedImagesPageRangesInput_defaultsToAllPagesWhenBlank() {
        val ranges = parseEmbeddedImagesPageRangesInput(
            rawValue = "   ",
            totalPageCount = 7,
        )

        assertEquals(1, ranges.single().startPage)
        assertEquals(7, ranges.single().endPage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseEmbeddedImagesPageRangesInput_rejectsOverlappingRanges() {
        parseEmbeddedImagesPageRangesInput(
            rawValue = "1-2, 2-4",
            totalPageCount = 9,
        )
    }

    @Test
    fun buildDefaultEmbeddedImagesFolderName_usesDocumentName() {
        val folderName = buildDefaultEmbeddedImagesFolderName(document("Quarterly Report.pdf"))

        assertEquals("Quarterly Report-embedded-images", folderName)
    }

    @Test
    fun buildInternalEmbeddedImagesDirectoryName_createsStableFolderName() {
        val directoryName = buildInternalEmbeddedImagesDirectoryName(
            displayFolderName = "Quarterly Report Final",
            timestampMillis = 42L,
        )

        assertEquals("embedded_images_42_Quarterly_Report_Final", directoryName)
    }

    @Test
    fun imageMimeTypeFor_resolvesJpegAndPng() {
        assertEquals("image/jpeg", imageMimeTypeFor(File("sample.jpg")))
        assertEquals("image/png", imageMimeTypeFor(File("sample.png")))
        assertEquals("image/png", imageMimeTypeFor(File("sample.unknown")))
        assertTrue(normalizeEmbeddedImagesFolderName(" report .png ").contains("report"))
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

