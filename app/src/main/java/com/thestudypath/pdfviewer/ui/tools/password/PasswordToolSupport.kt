package com.thestudypath.pdfviewer.ui.tools.password

import com.thestudypath.pdfviewer.catalog.DocumentItem
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultProtectedBaseName = "protected-pdf"
private const val DefaultUnlockedBaseName = "unlocked-pdf"

internal fun buildDefaultProtectedPdfFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-protected" }
        ?: DefaultProtectedBaseName
    return normalizeProtectedPdfFileName(baseName)
}

internal fun normalizeProtectedPdfFileName(
    rawValue: String,
    fallbackValue: String = "$DefaultProtectedBaseName.pdf",
): String = normalizePdfOutputFileName(
    rawValue = rawValue,
    fallbackValue = fallbackValue,
)

internal fun buildInternalProtectedPdfFileName(
    displayFileName: String,
    timestampMillis: Long,
): String = buildInternalPdfOutputFileName(
    prefix = "protected",
    displayFileName = displayFileName,
    fallbackBaseName = DefaultProtectedBaseName,
    timestampMillis = timestampMillis,
)

internal fun buildDefaultUnlockedPdfFileName(document: DocumentItem?): String {
    val baseName = document?.displayName
        ?.removePdfExtension()
        ?.removeKnownProtectionSuffix()
        ?.takeIf { it.isNotBlank() }
        ?.let { "$it-unlocked" }
        ?: DefaultUnlockedBaseName
    return normalizeUnlockedPdfFileName(baseName)
}

internal fun normalizeUnlockedPdfFileName(
    rawValue: String,
    fallbackValue: String = "$DefaultUnlockedBaseName.pdf",
): String = normalizePdfOutputFileName(
    rawValue = rawValue,
    fallbackValue = fallbackValue,
)

internal fun buildInternalUnlockedPdfFileName(
    displayFileName: String,
    timestampMillis: Long,
): String = buildInternalPdfOutputFileName(
    prefix = "unlocked",
    displayFileName = displayFileName,
    fallbackBaseName = DefaultUnlockedBaseName,
    timestampMillis = timestampMillis,
)

internal fun validateProtectionPasswords(
    userPassword: String,
    confirmPassword: String,
) {
    require(userPassword.isNotBlank()) { "Enter a password to protect the PDF." }
    require(confirmPassword.isNotBlank()) { "Confirm the password to continue." }
    require(userPassword == confirmPassword) { "The passwords do not match." }
}

private fun normalizePdfOutputFileName(
    rawValue: String,
    fallbackValue: String,
): String {
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removePdfExtension() else trimmed.removePdfExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removePdfExtension().ifBlank { DefaultProtectedBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        sanitizedBase
    } else {
        "$sanitizedBase.pdf"
    }
}

private fun buildInternalPdfOutputFileName(
    prefix: String,
    displayFileName: String,
    fallbackBaseName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFileName
        .removePdfExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { fallbackBaseName }
        .take(48)

    return "${prefix}_${timestampMillis}_${sanitizedStem}.pdf"
}

private fun String.removePdfExtension(): String = removeSuffix(".pdf").removeSuffix(".PDF")

private fun String.removeKnownProtectionSuffix(): String {
    val normalized = trim()
    return normalized
        .removeSuffix("-protected")
        .removeSuffix("-secured")
        .removeSuffix("-locked")
}

