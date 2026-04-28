package com.thestudypath.pdfviewer.ui.tools.text

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultExtractTextBaseName = "extracted-text"

enum class TextExtractionScope {
    AllPages,
    PageRange,
}

internal fun buildDefaultExtractedTextFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-text" }
        ?: DefaultExtractTextBaseName
    return normalizeExtractedTextFileName(baseName)
}

internal fun normalizeExtractedTextFileName(
    rawValue: String,
    fallbackValue: String = "$DefaultExtractTextBaseName.txt",
): String {
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removeTxtExtension() else trimmed.removeTxtExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removeTxtExtension().ifBlank { DefaultExtractTextBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".txt")) {
        sanitizedBase
    } else {
        "$sanitizedBase.txt"
    }
}

internal fun buildInternalExtractedTextFileName(
    displayFileName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFileName
        .removeTxtExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultExtractTextBaseName }
        .take(48)

    return "text_${timestampMillis}_${sanitizedStem}.txt"
}

internal fun resolveExtractionPageRange(
    scope: TextExtractionScope,
    totalPageCount: Int,
    startPageInput: String,
    endPageInput: String,
): PdfPageRange {
    require(totalPageCount > 0) { "The PDF does not contain any pages." }
    return when (scope) {
        TextExtractionScope.AllPages -> PdfPageRange(1, totalPageCount)
        TextExtractionScope.PageRange -> {
            val startPage = startPageInput.trim().toIntOrNull()
                ?: error("Enter a valid start page.")
            val endPage = endPageInput.trim().toIntOrNull()
                ?: error("Enter a valid end page.")
            require(startPage >= 1) { "Page ranges must start at page 1 or later." }
            require(endPage >= startPage) { "End page must be on or after the start page." }
            require(endPage <= totalPageCount) {
                "Page range $startPage-$endPage exceeds the document length of $totalPageCount pages."
            }
            PdfPageRange(startPage, endPage)
        }
    }
}

private fun String.removePdfExtension(): String = removeSuffix(".pdf").removeSuffix(".PDF")
private fun String.removeTxtExtension(): String = removeSuffix(".txt").removeSuffix(".TXT")


