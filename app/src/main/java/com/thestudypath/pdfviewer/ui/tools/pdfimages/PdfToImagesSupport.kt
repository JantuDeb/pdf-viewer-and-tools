package com.thestudypath.pdfviewer.ui.tools.pdfimages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat
import com.thestudypath.pdfviewer.processing.PdfPageRange
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultPdfImagesBaseName = "exported-images"

internal enum class PdfImageExportQualityOption(
    val dpi: Int,
    val label: String,
) {
    Standard(144, "Standard"),
    High(216, "High"),
}

internal fun buildDefaultPdfImagesFolderName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-images" }
        ?: DefaultPdfImagesBaseName
    return normalizePdfImagesFolderName(baseName)
}

internal fun normalizePdfImagesFolderName(
    rawValue: String,
    fallbackBaseName: String = DefaultPdfImagesBaseName,
): String {
    val trimmed = rawValue.trim()
    val sanitizedBase = trimmed
        .removePdfExtension()
        .removeImageExtension()
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackBaseName }

    return sanitizedBase.take(64)
}

internal fun buildInternalPdfImagesDirectoryName(
    displayFolderName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFolderName
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultPdfImagesBaseName }
        .take(48)

    return "pdf_images_${timestampMillis}_${sanitizedStem}"
}

internal fun parsePdfToImagesPageRangesInput(
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
        "Enter one or more page ranges, for example 1-3, 5, 8-10. Leave it blank to export every page."
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

internal fun PdfImageExportFormat.label(): String = when (this) {
    PdfImageExportFormat.Png -> "PNG"
    PdfImageExportFormat.Jpeg -> "JPEG"
}

internal fun PdfImageExportFormat.mimeType(): String = when (this) {
    PdfImageExportFormat.Png -> "image/png"
    PdfImageExportFormat.Jpeg -> "image/jpeg"
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

private fun String.removeImageExtension(): String {
    val lowerValue = lowercase(Locale.getDefault())
    return when {
        lowerValue.endsWith(".png") -> dropLast(4)
        lowerValue.endsWith(".jpg") -> dropLast(4)
        lowerValue.endsWith(".jpeg") -> dropLast(5)
        else -> this
    }
}

