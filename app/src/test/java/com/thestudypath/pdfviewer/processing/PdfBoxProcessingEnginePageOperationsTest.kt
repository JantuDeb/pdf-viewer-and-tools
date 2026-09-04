package com.thestudypath.pdfviewer.processing

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class PdfBoxProcessingEnginePageOperationsTest {

    private val engine = PdfBoxProcessingEngine()

    @Test
    fun rotatePages_rotatesOnlySelectedPages() = runBlocking {
        val workingDir = createTempDirectory("pdf-rotate-pages-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputFile = File(workingDir, "rotated.pdf")
        createSimplePdf(inputFile, 4)

        val result = engine.rotatePages(
            PdfRotatePagesRequest(
                inputFile = inputFile,
                outputFile = outputFile,
                pageRanges = listOf(PdfPageRange(2, 3)),
                rotationDegrees = 90,
            ),
        )

        assertTrue(result.isSuccess)
        PDDocument.load(outputFile).use { document ->
            assertEquals(0, document.getPage(0).rotation)
            assertEquals(90, document.getPage(1).rotation)
            assertEquals(90, document.getPage(2).rotation)
            assertEquals(0, document.getPage(3).rotation)
        }
    }

    @Test
    fun extractPages_keepsOnlySelectedPages() = runBlocking {
        val workingDir = createTempDirectory("pdf-extract-pages-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputFile = File(workingDir, "extracted.pdf")
        createSimplePdf(inputFile, 5)

        val result = engine.extractPages(
            PdfExtractPagesRequest(
                inputFile = inputFile,
                outputFile = outputFile,
                pageRanges = listOf(PdfPageRange(2, 3), PdfPageRange(5, 5)),
            ),
        )

        assertTrue(result.isSuccess)
        val inspection = engine.inspectDocument(outputFile)
        assertTrue(inspection.isSuccess)
        assertEquals(3, inspection.getOrThrow().pageCount)
    }

    @Test
    fun deletePages_removesSelectedPages_andKeepsAtLeastOnePage() = runBlocking {
        val workingDir = createTempDirectory("pdf-delete-pages-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputFile = File(workingDir, "deleted.pdf")
        createSimplePdf(inputFile, 5)

        val result = engine.deletePages(
            PdfDeletePagesRequest(
                inputFile = inputFile,
                outputFile = outputFile,
                pageRanges = listOf(PdfPageRange(2, 4)),
            ),
        )

        assertTrue(result.isSuccess)
        val inspection = engine.inspectDocument(outputFile)
        assertTrue(inspection.isSuccess)
        assertEquals(2, inspection.getOrThrow().pageCount)
    }

    private fun createSimplePdf(file: File, pageCount: Int) {
        file.parentFile?.mkdirs()
        PDDocument().use { document ->
            repeat(pageCount) {
                document.addPage(PDPage())
            }
            document.save(file)
        }
    }
}

