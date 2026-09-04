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
class PdfBoxProcessingEngineCropPageRegionTest {

    private val engine = PdfBoxProcessingEngine()

    @Before
    fun setUpPdfBoxResources() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun cropPageRegions_exportsCroppedPngsForSelectedPages() = runBlocking {
        val workingDir = createTempDirectory("page-crop-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputDirectory = File(workingDir, "cropped-images")
        createSimplePdf(inputFile, 2)

        val result = engine.cropPageRegions(
            PdfCropPageRegionRequest(
                inputFile = inputFile,
                outputDirectory = outputDirectory,
                outputFileNamePrefix = "Packet-cropped-region",
                pageRanges = listOf(PdfPageRange(1, 2)),
                cropRect = PdfNormalizedCropRect(
                    leftFraction = 0.25f,
                    topFraction = 0.25f,
                    rightFraction = 0.75f,
                    bottomFraction = 0.75f,
                ),
                format = PdfImageExportFormat.Png,
                renderDpi = 144,
            ),
        )

        assertTrue(result.exceptionOrNull()?.message ?: "Expected cropped export to succeed.", result.isSuccess)
        val exportResult = result.getOrThrow()
        assertEquals(2, exportResult.pageCount)
        assertEquals(PdfImageExportFormat.Png, exportResult.format)
        assertEquals(2, exportResult.exportedImages.size)
        assertTrue(exportResult.outputDirectory.isDirectory)

        exportResult.exportedImages.forEach { output ->
            val decodedBitmap = BitmapFactory.decodeFile(output.outputFile.absolutePath)
            assertTrue(output.outputFile.exists())
            assertTrue(decodedBitmap != null)
            assertEquals(612, output.width)
            assertEquals(792, output.height)
            assertEquals(output.width, decodedBitmap!!.width)
            assertEquals(output.height, decodedBitmap.height)
            decodedBitmap.recycle()
        }
    }

    @Test
    fun cropPageRegions_rejectsZeroWidthCrop() = runBlocking {
        val workingDir = createTempDirectory("page-crop-invalid-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputDirectory = File(workingDir, "cropped-images")
        createSimplePdf(inputFile, 1)

        val result = engine.cropPageRegions(
            PdfCropPageRegionRequest(
                inputFile = inputFile,
                outputDirectory = outputDirectory,
                outputFileNamePrefix = "Packet-cropped-region",
                pageRanges = listOf(PdfPageRange(1, 1)),
                cropRect = PdfNormalizedCropRect(
                    leftFraction = 0.5f,
                    topFraction = 0.1f,
                    rightFraction = 0.5f,
                    bottomFraction = 0.9f,
                ),
            ),
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Crop width"))
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

