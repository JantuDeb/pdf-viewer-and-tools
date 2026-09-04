package com.thestudypath.pdfviewer.ui.tools.password

import com.thestudypath.pdfviewer.catalog.DocumentItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordToolSupportTest {

    @Test
    fun buildDefaultProtectedPdfFileName_usesDocumentName() {
        val fileName = buildDefaultProtectedPdfFileName(document("Tax Return.pdf"))

        assertEquals("Tax Return-protected.pdf", fileName)
    }

    @Test
    fun buildDefaultUnlockedPdfFileName_removesKnownProtectedSuffix() {
        val fileName = buildDefaultUnlockedPdfFileName(document("Tax Return-protected.pdf"))

        assertEquals("Tax Return-unlocked.pdf", fileName)
    }

    @Test
    fun normalizeProtectedPdfFileName_addsPdfExtension_and_stripsInvalidCharacters() {
        val fileName = normalizeProtectedPdfFileName("Client/Packet:*Final")

        assertEquals("Client Packet Final.pdf", fileName)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateProtectionPasswords_requiresMatchingPasswords() {
        validateProtectionPasswords(
            userPassword = "secret123",
            confirmPassword = "different123",
        )
    }

    @Test
    fun buildInternalUnlockedPdfFileName_createsStablePdfFileName() {
        val fileName = buildInternalUnlockedPdfFileName(
            displayFileName = "Ready Copy.pdf",
            timestampMillis = 42L,
        )

        assertEquals("unlocked_42_Ready_Copy.pdf", fileName)
    }

    private fun document(displayName: String) = DocumentItem(
        id = displayName,
        fileName = displayName,
        displayName = displayName,
        filePath = "C:/docs/$displayName",
        sizeBytes = 1024,
        pageCount = 8,
        lastOpenedAt = 1L,
        lastModifiedAt = 1L,
    )
}

