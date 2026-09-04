package com.thestudypath.pdfviewer.ui.tools.embeddedimages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import java.io.File

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultEmbeddedImagesBaseName = "embedded-images"

internal fun buildDefaultEmbeddedImagesFolderName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-embedded-images" }
        ?: DefaultEmbeddedImagesBaseName
    return normalizeEmbeddedImagesFolderName(baseName)
}

internal fun normalizeEmbeddedImagesFolderName(
    rawValue: String,
    fallbackBaseName: String = DefaultEmbeddedImagesBaseName,
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

internal fun buildInternalEmbeddedImagesDirectoryName(
    displayFolderName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFolderName
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultEmbeddedImagesBaseName }
        .take(48)

    return "embedded_images_${timestampMillis}_${sanitizedStem}"
}

internal fun parseEmbeddedImagesPageRangesInput(
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
        "Enter one or more page ranges, for example 1-3, 5, 8-10. Leave it blank to scan every page for images."
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

internal fun imageMimeTypeFor(file: File): String = when (file.extension.lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    else -> "image/png"
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

private fun String.removeImageExtension(): String = when {
    endsWith(".png", ignoreCase = true) -> dropLast(4)
    endsWith(".jpg", ignoreCase = true) -> dropLast(4)
    endsWith(".jpeg", ignoreCase = true) -> dropLast(5)
    else -> this
}

