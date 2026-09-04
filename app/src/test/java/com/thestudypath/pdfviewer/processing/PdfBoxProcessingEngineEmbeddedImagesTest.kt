package com.thestudypath.pdfviewer.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDFormContentStream
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
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
class PdfBoxProcessingEngineEmbeddedImagesTest {

    private val engine = PdfBoxProcessingEngine()

    @Before
    fun setUpPdfBoxResources() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun extractEmbeddedImages_extractsUniqueImagesAcrossPagesAndForms() = runBlocking {
        val workingDir = createTempDirectory("embedded-images-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputDirectory = File(workingDir, "embedded-images")
        createPdfWithEmbeddedImages(inputFile)

        val result = engine.extractEmbeddedImages(
            ExtractEmbeddedImagesRequest(
                inputFile = inputFile,
                outputDirectory = outputDirectory,
                outputFileNamePrefix = "Quarterly Report-embedded-images",
                pageRanges = listOf(PdfPageRange(1, 2)),
            ),
        )

        assertTrue(result.exceptionOrNull()?.message ?: "Expected embedded-image extraction to succeed.", result.isSuccess)
        val exportResult = result.getOrThrow()
        assertEquals(2, exportResult.pageCount)
        assertEquals(2, exportResult.exportedImages.size)
        assertTrue(exportResult.outputDirectory.isDirectory)
        assertTrue(exportResult.exportedImages.all { it.outputFile.exists() })
        assertTrue(exportResult.exportedImages.all { it.fileSizeBytes > 0L })

        val pageSets = exportResult.exportedImages.map { it.pageNumbers.sorted() }.sortedBy { it.size }
        assertEquals(listOf(listOf(2), listOf(1, 2)), pageSets)

        exportResult.exportedImages.forEach { output ->
            val decodedBitmap = BitmapFactory.decodeFile(output.outputFile.absolutePath)
            assertTrue(decodedBitmap != null)
            assertEquals(output.width, decodedBitmap!!.width)
            assertEquals(output.height, decodedBitmap.height)
            decodedBitmap.recycle()
        }
    }

    @Test
    fun extractEmbeddedImages_failsWhenNoImagesAreFound() = runBlocking {
        val workingDir = createTempDirectory("embedded-images-empty-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputDirectory = File(workingDir, "embedded-images")
        PDDocument().use { document ->
            document.addPage(PDPage(PDRectangle.LETTER))
            document.save(inputFile)
        }

        val result = engine.extractEmbeddedImages(
            ExtractEmbeddedImagesRequest(
                inputFile = inputFile,
                outputDirectory = outputDirectory,
                outputFileNamePrefix = "Packet-embedded-images",
                pageRanges = listOf(PdfPageRange(1, 1)),
            ),
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("No embedded images"))
    }

    private fun createPdfWithEmbeddedImages(file: File) {
        file.parentFile?.mkdirs()
        PDDocument().use { document ->
            val pageOne = PDPage(PDRectangle.LETTER)
            val pageTwo = PDPage(PDRectangle.LETTER)
            document.addPage(pageOne)
            document.addPage(pageTwo)

            val sharedBitmap = Bitmap.createBitmap(24, 18, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.RED)
            }
            val uniqueBitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.BLUE)
            }

            val sharedImage = LosslessFactory.createFromImage(document, sharedBitmap)
            val uniqueImage = LosslessFactory.createFromImage(document, uniqueBitmap)

            PDPageContentStream(document, pageOne).use { contentStream ->
                contentStream.drawImage(sharedImage, 36f, 640f, 72f, 54f)
            }

            val form = PDFormXObject(document).apply {
                resources = PDResources()
                setBBox(PDRectangle(0f, 0f, 72f, 54f))
            }
            PDFormContentStream(form).use { formContentStream ->
                formContentStream.drawImage(sharedImage, 0f, 0f, 72f, 54f)
            }

            PDPageContentStream(document, pageTwo).use { contentStream ->
                contentStream.drawForm(form)
                contentStream.drawImage(uniqueImage, 120f, 620f, 48f, 48f)
            }

            sharedBitmap.recycle()
            uniqueBitmap.recycle()
            document.save(file)
        }
    }
}


