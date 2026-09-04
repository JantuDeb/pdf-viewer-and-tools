package com.thestudypath.pdfviewer.ui.tools.watermark

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import com.thestudypath.pdfviewer.processing.PdfWatermarkPlacement
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultWatermarkBaseName = "watermarked-document"

internal enum class WatermarkSizeOption(
    val textScalePercent: Int,
    val label: String,
) {
    Small(10, "Small"),
    Medium(14, "Medium"),
    Large(18, "Large"),
}

internal data class WatermarkPlacementOption(
    val placement: PdfWatermarkPlacement,
    val label: String,
)

internal val watermarkPlacementOptions = listOf(
    WatermarkPlacementOption(PdfWatermarkPlacement.DiagonalCenter, "Diagonal"),
    WatermarkPlacementOption(PdfWatermarkPlacement.Center, "Center"),
    WatermarkPlacementOption(PdfWatermarkPlacement.TopLeft, "Top left"),
    WatermarkPlacementOption(PdfWatermarkPlacement.TopRight, "Top right"),
    WatermarkPlacementOption(PdfWatermarkPlacement.BottomLeft, "Bottom left"),
    WatermarkPlacementOption(PdfWatermarkPlacement.BottomRight, "Bottom right"),
)

internal fun buildDefaultWatermarkedPdfFileName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-watermarked" }
        ?: DefaultWatermarkBaseName
    return normalizeWatermarkPdfFileName(baseName, DefaultWatermarkBaseName)
}

internal fun normalizeWatermarkPdfFileName(
    rawValue: String,
    fallbackBaseName: String = DefaultWatermarkBaseName,
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

internal fun buildInternalWatermarkPdfFileName(
    displayFileName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFileName
        .removePdfExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultWatermarkBaseName }
        .take(48)

    return "watermarked_${timestampMillis}_${sanitizedStem}.pdf"
}

internal fun parseWatermarkPageRangesInput(
    rawValue: String,
    totalPageCount: Int,
): List<PdfPageRange> {
    require(totalPageCount >= 1) { "The PDF does not contain any pages." }
    if (rawValue.isBlank()) {
        return listOf(PdfPageRange(startPage = 1, endPage = totalPageCount))
    }

    val tokens = rawValue.split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)

    require(tokens.isNotEmpty()) {
        "Enter one or more page ranges, for example 1-3, 5, 8-10. Leave it blank to watermark every page."
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

