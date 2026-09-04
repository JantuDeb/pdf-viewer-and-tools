package com.thestudypath.pdfviewer.processing

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class PdfBoxProcessingEnginePasswordTest {

    private val engine = PdfBoxProcessingEngine()

    @Test
    fun addPassword_and_removePassword_roundTripsDocument() = runBlocking {
        val workingDir = createTempDirectory("pdf-password-test").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val protectedFile = File(workingDir, "protected.pdf")
        val unlockedFile = File(workingDir, "unlocked.pdf")
        createSimplePdf(inputFile)

        val protectResult = engine.addPassword(
            PdfAddPasswordRequest(
                inputFile = inputFile,
                outputFile = protectedFile,
                userPassword = "secret123",
            )
        )
        assertTrue(protectResult.isSuccess)
        assertTrue(protectedFile.exists())

        val protectedInspection = engine.inspectDocument(
            file = protectedFile,
            password = "secret123",
        )
        assertTrue(protectedInspection.isSuccess)
        assertEquals(true, protectedInspection.getOrThrow().isEncrypted)
        assertEquals(1, protectedInspection.getOrThrow().pageCount)

        val unlockedResult = engine.removePassword(
            PdfRemovePasswordRequest(
                inputFile = protectedFile,
                outputFile = unlockedFile,
                password = "secret123",
            )
        )
        assertTrue(unlockedResult.isSuccess)
        assertTrue(unlockedFile.exists())

        val unlockedInspection = engine.inspectDocument(unlockedFile)
        assertTrue(unlockedInspection.isSuccess)
        assertEquals(false, unlockedInspection.getOrThrow().isEncrypted)
        assertEquals(1, unlockedInspection.getOrThrow().pageCount)
    }

    @Test
    fun removePassword_failsWithHelpfulMessage_whenPasswordIsWrong() = runBlocking {
        val workingDir = createTempDirectory("pdf-password-test-wrong").toFile()
        val inputFile = File(workingDir, "input.pdf")
        val protectedFile = File(workingDir, "protected.pdf")
        val unlockedFile = File(workingDir, "unlocked.pdf")
        createSimplePdf(inputFile)
        engine.addPassword(
            PdfAddPasswordRequest(
                inputFile = inputFile,
                outputFile = protectedFile,
                userPassword = "secret123",
            )
        ).getOrThrow()

        val result = engine.removePassword(
            PdfRemovePasswordRequest(
                inputFile = protectedFile,
                outputFile = unlockedFile,
                password = "wrong-password",
            )
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("Incorrect password"))
    }

    private fun createSimplePdf(file: File) {
        file.parentFile?.mkdirs()
        PDDocument().use { document ->
            document.addPage(PDPage())
            document.save(file)
        }
    }
}

