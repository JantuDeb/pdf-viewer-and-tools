package com.thestudypath.pdfviewer.processing

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PdfBoxProcessingEnginePdfToImagesTest {

    private val engine = PdfBoxProcessingEngine()

    @Before
    fun setUpPdfBoxResources() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun exportPdfToImages_exportsOnlySelectedPagesAsPng() = runBlocking {
        val workingDir = createTempDirectory("pdf-to-images-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputDirectory = File(workingDir, "exported-images")
        createSimplePdf(inputFile, 3)

        val result = engine.exportPdfToImages(
            PdfToImagesRequest(
                inputFile = inputFile,
                outputDirectory = outputDirectory,
                outputFileNamePrefix = "Quarterly Report-images",
                pageRanges = listOf(PdfPageRange(1, 1), PdfPageRange(3, 3)),
                format = PdfImageExportFormat.Png,
                renderDpi = 144,
            ),
        )

        assertTrue(result.exceptionOrNull()?.message ?: "Expected PDF-to-images export to succeed.", result.isSuccess)
        val exportResult = result.getOrThrow()
        assertEquals(3, exportResult.pageCount)
        assertEquals(PdfImageExportFormat.Png, exportResult.format)
        assertEquals(listOf(1, 3), exportResult.exportedImages.map { it.pageNumber })
        assertEquals(2, exportResult.exportedImages.size)
        assertTrue(exportResult.outputDirectory.isDirectory)
        assertTrue(exportResult.exportedImages.all { it.outputFile.extension.equals("png", ignoreCase = true) })
        assertTrue(exportResult.exportedImages.all { it.fileSizeBytes > 0L })

        exportResult.exportedImages.forEach { output ->
            val decodedBitmap = BitmapFactory.decodeFile(output.outputFile.absolutePath)
            assertTrue(output.outputFile.exists())
            assertTrue(decodedBitmap != null)
            assertTrue(output.width > 0)
            assertTrue(output.height > 0)
            assertEquals(output.width, decodedBitmap!!.width)
            assertEquals(output.height, decodedBitmap.height)
            decodedBitmap.recycle()
        }
    }

    @Test
    fun exportPdfToImages_exportsJpegFiles() = runBlocking {
        val workingDir = createTempDirectory("pdf-to-jpeg-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputDirectory = File(workingDir, "jpeg-images")
        createSimplePdf(inputFile, 1)

        val result = engine.exportPdfToImages(
            PdfToImagesRequest(
                inputFile = inputFile,
                outputDirectory = outputDirectory,
                outputFileNamePrefix = "Packet-images",
                pageRanges = listOf(PdfPageRange(1, 1)),
                format = PdfImageExportFormat.Jpeg,
                renderDpi = 144,
                jpegQualityPercent = 90,
            ),
        )

        assertTrue(result.exceptionOrNull()?.message ?: "Expected JPEG export to succeed.", result.isSuccess)
        val output = result.getOrThrow().exportedImages.single()
        assertTrue(output.outputFile.extension.equals("jpg", ignoreCase = true))
        assertTrue(output.fileSizeBytes > 0L)
    }

    private fun createSimplePdf(file: File, pageCount: Int) {
        file.parentFile?.mkdirs()
        PDDocument().use { document ->
            repeat(pageCount) {
                document.addPage(PDPage(PDRectangle.LETTER))
            }
            document.save(file)
        }
    }
}

