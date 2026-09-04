package com.thestudypath.pdfviewer.processing

import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
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
class PdfBoxProcessingEngineWatermarkTest {

    private val engine = PdfBoxProcessingEngine()

    @Before
    fun setUpPdfBoxResources() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun addTextWatermark_appliesTextOnlyToSelectedPages() = runBlocking {
        val workingDir = createTempDirectory("pdf-watermark-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputFile = File(workingDir, "watermarked.pdf")
        createSimplePdf(inputFile, 3)

        val result = engine.addTextWatermark(
            PdfTextWatermarkRequest(
                inputFile = inputFile,
                outputFile = outputFile,
                text = "APPROVED",
                pageRanges = listOf(PdfPageRange(2, 3)),
                placement = PdfWatermarkPlacement.Center,
                textScalePercent = 10,
            ),
        )

        assertTrue(result.exceptionOrNull()?.message ?: "Expected watermarking to succeed.", result.isSuccess)
        assertEquals(3, result.getOrThrow().pageCount)
        assertEquals(2, result.getOrThrow().watermarkedPageCount)
        assertTrue(outputFile.length() > inputFile.length())

        PDDocument.load(outputFile).use { document ->
            assertEquals("", extractPageText(document, 1))
            assertTrue(extractPageText(document, 2).contains("APPROVED"))
            assertTrue(extractPageText(document, 3).contains("APPROVED"))
        }
    }

    @Test
    fun addTextWatermark_rejectsBlankText() = runBlocking {
        val workingDir = createTempDirectory("pdf-watermark-empty-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val outputFile = File(workingDir, "watermarked.pdf")
        createSimplePdf(inputFile, 1)

        val result = engine.addTextWatermark(
            PdfTextWatermarkRequest(
                inputFile = inputFile,
                outputFile = outputFile,
                text = "   ",
                pageRanges = listOf(PdfPageRange(1, 1)),
            ),
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Enter watermark text"))
    }

    @Test
    fun resolveTextWatermarkLayout_centersDiagonalWatermarkBounds() {
        val textWidth = 240f
        val textHeight = 48f
        val layout = resolveTextWatermarkLayout(
            pageWidth = 600f,
            pageHeight = 800f,
            textWidth = textWidth,
            textHeight = textHeight,
            marginPercent = 5,
            placement = PdfWatermarkPlacement.DiagonalCenter,
        )

        val center = rotatedBoundsCenter(
            layout = layout,
            textWidth = textWidth,
            textHeight = textHeight,
        )

        assertEquals(300f, center.first, 0.05f)
        assertEquals(400f, center.second, 0.05f)
    }

    private fun extractPageText(document: PDDocument, pageNumber: Int): String = PDFTextStripper().apply {
        startPage = pageNumber
        endPage = pageNumber
    }.getText(document).trim()

    private fun rotatedBoundsCenter(
        layout: TextWatermarkLayout,
        textWidth: Float,
        textHeight: Float,
    ): Pair<Float, Float> {
        val cosValue = kotlin.math.cos(layout.rotationRadians)
        val sinValue = kotlin.math.sin(layout.rotationRadians)
        val corners = listOf(
            0f to 0f,
            textWidth to 0f,
            0f to textHeight,
            textWidth to textHeight,
        ).map { (x, y) ->
            val rotatedX = layout.translationX + (x * cosValue) - (y * sinValue)
            val rotatedY = layout.translationY + (x * sinValue) + (y * cosValue)
            rotatedX to rotatedY
        }
        val minX = corners.minOf { it.first }
        val maxX = corners.maxOf { it.first }
        val minY = corners.minOf { it.second }
        val maxY = corners.maxOf { it.second }
        return ((minX + maxX) / 2f) to ((minY + maxY) / 2f)
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

