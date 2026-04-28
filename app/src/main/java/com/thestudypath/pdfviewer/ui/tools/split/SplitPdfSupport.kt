package com.thestudypath.pdfviewer.ui.tools.split

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultSplitOutputBaseName = "split-document"

enum class SplitMode {
    EveryNPages,
    CustomRanges,
}

data class SplitOutputName(
    val displayName: String,
    val internalFileName: String,
)

internal fun buildDefaultSplitBaseName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-split" }
        ?: DefaultSplitOutputBaseName
    return normalizeSplitBaseName(baseName)
}

internal fun normalizeSplitBaseName(
    rawValue: String,
    fallbackValue: String = "$DefaultSplitOutputBaseName.pdf",
): String {
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removePdfExtension() else trimmed.removePdfExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removePdfExtension().ifBlank { DefaultSplitOutputBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        sanitizedBase
    } else {
        "$sanitizedBase.pdf"
    }
}

internal fun buildPageRangesForEveryNPages(
    totalPageCount: Int,
    pagesPerSplit: Int,
): List<PdfPageRange> {
    require(totalPageCount >= 1) { "The PDF does not contain any pages." }
    require(pagesPerSplit >= 1) { "Pages per split must be at least 1." }

    return buildList {
        var startPage = 1
        while (startPage <= totalPageCount) {
            val endPage = minOf(startPage + pagesPerSplit - 1, totalPageCount)
            add(PdfPageRange(startPage = startPage, endPage = endPage))
            startPage = endPage + 1
        }
    }
}

internal fun parseCustomPageRanges(
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

internal fun buildSplitOutputName(
    baseFileName: String,
    timestampMillis: Long,
    outputIndex: Int,
    pageRange: PdfPageRange,
): SplitOutputName {
    require(outputIndex >= 0) { "Output index must be zero or greater." }
    val normalizedBase = normalizeSplitBaseName(baseFileName)
    val sanitizedStem = normalizedBase
        .removePdfExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultSplitOutputBaseName }
        .take(48)

    val partLabel = String.format(Locale.US, "%02d", outputIndex + 1)
    val pageLabel = if (pageRange.startPage == pageRange.endPage) {
        "page-${pageRange.startPage}"
    } else {
        "pages-${pageRange.startPage}-${pageRange.endPage}"
    }

    return SplitOutputName(
        displayName = "${normalizedBase.removePdfExtension()} part $partLabel ($pageLabel).pdf",
        internalFileName = "split_${timestampMillis}_${partLabel}_${sanitizedStem}_${pageLabel.replace('-', '_')}.pdf",
    )
}

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

