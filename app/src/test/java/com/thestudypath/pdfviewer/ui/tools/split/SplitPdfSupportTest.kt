package com.thestudypath.pdfviewer.ui.tools.split

import com.thestudypath.pdfviewer.processing.PdfPageRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitPdfSupportTest {

    @Test
    fun buildPageRangesForEveryNPages_createsExpectedSegments() {
        val ranges = buildPageRangesForEveryNPages(
            totalPageCount = 9,
            pagesPerSplit = 2,
        )

        assertEquals(
            listOf(
                PdfPageRange(1, 2),
                PdfPageRange(3, 4),
                PdfPageRange(5, 6),
                PdfPageRange(7, 8),
                PdfPageRange(9, 9),
            ),
            ranges,
        )
    }

    @Test
    fun parseCustomPageRanges_acceptsMixedSinglePagesAndRanges() {
        val ranges = parseCustomPageRanges(
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
    fun parseCustomPageRanges_rejectsOverlaps() {
        parseCustomPageRanges(
            rawValue = "1-3, 3-5",
            totalPageCount = 8,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseCustomPageRanges_rejectsOutOfBoundsPages() {
        parseCustomPageRanges(
            rawValue = "2-6",
            totalPageCount = 5,
        )
    }

    @Test
    fun buildSplitOutputName_buildsStableDisplayAndInternalNames() {
        val outputName = buildSplitOutputName(
            baseFileName = "Quarterly Summary.pdf",
            timestampMillis = 42L,
            outputIndex = 1,
            pageRange = PdfPageRange(4, 7),
        )

        assertEquals("Quarterly Summary part 02 (pages-4-7).pdf", outputName.displayName)
        assertEquals("split_42_02_Quarterly_Summary_pages_4_7.pdf", outputName.internalFileName)
        assertTrue(outputName.internalFileName.endsWith(".pdf"))
    }
}

