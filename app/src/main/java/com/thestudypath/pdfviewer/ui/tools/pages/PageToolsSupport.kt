package com.thestudypath.pdfviewer.ui.tools.pages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultRotateBaseName = "rotated-document"
private const val DefaultExtractBaseName = "extracted-pages"
private const val DefaultDeleteBaseName = "deleted-pages"

enum class PageRotationOption(
    val degrees: Int,
    val label: String,
) {
    Clockwise90(90, "90°"),
    Clockwise180(180, "180°"),
    Clockwise270(270, "270°"),
}

internal fun buildDefaultRotatedPdfFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-rotated" }
        ?: DefaultRotateBaseName
    return normalizePageToolPdfFileName(baseName, DefaultRotateBaseName)
}

internal fun buildDefaultExtractedPagesPdfFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-extracted-pages" }
        ?: DefaultExtractBaseName
    return normalizePageToolPdfFileName(baseName, DefaultExtractBaseName)
}

internal fun buildDefaultDeletedPagesPdfFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-deleted-pages" }
        ?: DefaultDeleteBaseName
    return normalizePageToolPdfFileName(baseName, DefaultDeleteBaseName)
}

internal fun normalizePageToolPdfFileName(
    rawValue: String,
    fallbackBaseName: String,
): String {
    val fallbackValue = "$fallbackBaseName.pdf"
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removePdfExtension() else trimmed.removePdfExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removePdfExtension().ifBlank { fallbackBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        sanitizedBase
    } else {
        "$sanitizedBase.pdf"
    }
}

internal fun buildInternalPageToolFileName(
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

internal fun parsePageRangesInput(
    rawValue: String,
    totalPageCount: Int,
): List<PdfPageRange> {
    require(totalPageCount >= 1) { "The PDF does not contain any pages." }
    val tokens = rawValue.split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)

    require(tokens.isNotEmpty()) {
        "Enter one or more page ranges, for example 1-3, 5, 8-10."
    }

    val claimedPages = mutableSetOf<Int>()
    return tokens.map { token ->
        val range = parsePageRangeToken(token)
        require(range.startPage >= 1) {
            "Page ranges must start at page 1 or later."
        }
        require(range.endPage <= totalPageCount) {
            "Page range ${range.startPage}-${range.endPage} exceeds the document length of $totalPageCount pages."
        }
        (range.startPage..range.endPage).forEach { pageNumber ->
            require(claimedPages.add(pageNumber)) {
                "Page $pageNumber is included more than once. Remove overlapping ranges and try again."
            }
        }
        range
    }
}

internal fun countPagesInRanges(pageRanges: List<PdfPageRange>): Int =
    pageRanges.sumOf { it.pageCount }

private fun parsePageRangeToken(token: String): PdfPageRange {
    val rangeParts = token.split('-').map(String::trim).filter(String::isNotEmpty)
    return when (rangeParts.size) {
        1 -> {
            val pageNumber = rangeParts.first().toIntOrNull()
                ?: error("\"$token\" is not a valid page number.")
            PdfPageRange(startPage = pageNumber, endPage = pageNumber)
        }
        2 -> {
            val startPage = rangeParts[0].toIntOrNull()
                ?: error("\"$token\" is not a valid page range.")
            val endPage = rangeParts[1].toIntOrNull()
                ?: error("\"$token\" is not a valid page range.")
            require(endPage >= startPage) {
                "Page range $token must end on or after its start page."
            }
            PdfPageRange(startPage = startPage, endPage = endPage)
        }
        else -> error("\"$token\" is not a valid page range.")
    }
}

private fun String.removePdfExtension(): String = removeSuffix(".pdf").removeSuffix(".PDF")

