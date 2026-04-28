package com.thestudypath.pdfviewer.ui.tools.compress

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfCompressionProfile
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultCompressedOutputBaseName = "compressed-document"

internal fun buildDefaultCompressedFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-compressed" }
        ?: DefaultCompressedOutputBaseName
    return normalizeCompressedFileName(baseName)
}

internal fun normalizeCompressedFileName(
    rawValue: String,
    fallbackValue: String = "$DefaultCompressedOutputBaseName.pdf",
): String {
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removePdfExtension() else trimmed.removePdfExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removePdfExtension().ifBlank { DefaultCompressedOutputBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        sanitizedBase
    } else {
        "$sanitizedBase.pdf"
    }
}

internal fun buildInternalCompressedFileName(
    displayFileName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFileName
        .removePdfExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultCompressedOutputBaseName }
        .take(48)

    return "compressed_${timestampMillis}_${sanitizedStem}.pdf"
}

internal fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}

internal fun profileLabel(profile: PdfCompressionProfile): String = when (profile) {
    PdfCompressionProfile.Light -> "Light"
    PdfCompressionProfile.Balanced -> "Balanced"
    PdfCompressionProfile.Strong -> "Strong"
}

internal fun profileGuidance(profile: PdfCompressionProfile): String = when (profile) {
    PdfCompressionProfile.Light -> "Light keeps more image quality and usually yields smaller size savings."
    PdfCompressionProfile.Balanced -> "Balanced is recommended for a good quality-to-size tradeoff."
    PdfCompressionProfile.Strong -> "Strong maximizes size reduction and may noticeably soften images."
}

private fun String.removePdfExtension(): String = removeSuffix(".pdf").removeSuffix(".PDF")

