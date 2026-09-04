package com.thestudypath.pdfviewer.ui.tools.crop

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropPageRegionSupportTest {

    @Test
    fun parseCropPageRangesInput_defaultsToAllPagesWhenBlank() {
        val ranges = parseCropPageRangesInput(
            rawValue = "   ",
            totalPageCount = 6,
        )

        assertEquals(1, ranges.single().startPage)
        assertEquals(6, ranges.single().endPage)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseCropPageRangesInput_rejectsOverlappingRanges() {
        parseCropPageRangesInput(
            rawValue = "1-2, 2-3",
            totalPageCount = 8,
        )
    }

    @Test
    fun buildDefaultCropExportFolderName_usesDocumentName() {
        val folderName = buildDefaultCropExportFolderName(document("Packet.pdf"))

        assertEquals("Packet-cropped-region", folderName)
    }

    @Test
    fun sanitizeCropRegion_preservesMinimumGap() {
        val sanitized = sanitizeCropRegion(
            CropRegionSelection(
                leftPercent = 98,
                topPercent = 99,
                rightPercent = 99,
                bottomPercent = 100,
            ),
        )

        assertTrue(sanitized.rightPercent - sanitized.leftPercent >= 5)
        assertTrue(sanitized.bottomPercent - sanitized.topPercent >= 5)
    }

    @Test
    fun resolvePresetForRegion_returnsCustomWhenNeeded() {
        assertEquals(PageCropPreset.Header, resolvePresetForRegion(PageCropPreset.Header.region))
        assertEquals(
            PageCropPreset.Custom,
            resolvePresetForRegion(CropRegionSelection(7, 8, 92, 93)),
        )
    }

    @Test
    fun toNormalizedCropRect_convertsPercentages() {
        val cropRect = CropRegionSelection(25, 10, 75, 60).toNormalizedCropRect()

        assertEquals(0.25f, cropRect.leftFraction)
        assertEquals(0.10f, cropRect.topFraction)
        assertEquals(0.75f, cropRect.rightFraction)
        assertEquals(0.60f, cropRect.bottomFraction)
        assertEquals("image/jpeg", PdfImageExportFormat.Jpeg.mimeType())
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

