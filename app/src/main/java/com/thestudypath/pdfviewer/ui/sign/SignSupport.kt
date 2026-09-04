package com.thestudypath.pdfviewer.ui.sign

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.VisibleSignaturePlacement
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultSignedBaseName = "signed-document"

enum class SignatureSourceType {
    Drawn,
    Imported,
}

data class SignatureInput(
    val filePath: String,
    val displayName: String,
    val sourceType: SignatureSourceType,
)

internal fun buildDefaultSignedPdfFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-signed" }
        ?: DefaultSignedBaseName
    return normalizeSignedPdfFileName(baseName)
}

internal fun normalizeSignedPdfFileName(
    rawValue: String,
    fallbackValue: String = "$DefaultSignedBaseName.pdf",
): String {
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removePdfExtension() else trimmed.removePdfExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removePdfExtension().ifBlank { DefaultSignedBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        sanitizedBase
    } else {
        "$sanitizedBase.pdf"
    }
}

internal fun buildInternalSignedPdfFileName(
    displayFileName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFileName
        .removePdfExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultSignedBaseName }
        .take(48)

    return "signed_${timestampMillis}_${sanitizedStem}.pdf"
}

internal fun normalizePageNumberInput(
    rawValue: String,
    totalPages: Int,
): String {
    if (totalPages <= 0) return "1"
    val pageNumber = rawValue.trim().toIntOrNull() ?: 1
    return pageNumber.coerceIn(1, totalPages).toString()
}

internal fun resolveSigningPageNumber(
    rawValue: String,
    totalPages: Int,
): Int {
    require(totalPages > 0) { "The selected PDF does not contain any pages." }
    val pageNumber = rawValue.trim().toIntOrNull() ?: error("Enter a valid page number.")
    require(pageNumber in 1..totalPages) {
        "Page $pageNumber is outside this document's $totalPages pages."
    }
    return pageNumber
}

internal fun placementLabel(placement: VisibleSignaturePlacement): String = when (placement) {
    VisibleSignaturePlacement.TopLeft -> "Top left"
    VisibleSignaturePlacement.TopRight -> "Top right"
    VisibleSignaturePlacement.BottomLeft -> "Bottom left"
    VisibleSignaturePlacement.BottomRight -> "Bottom right"
    VisibleSignaturePlacement.Center -> "Center"
}

private fun String.removePdfExtension(): String = removeSuffix(".pdf").removeSuffix(".PDF")

