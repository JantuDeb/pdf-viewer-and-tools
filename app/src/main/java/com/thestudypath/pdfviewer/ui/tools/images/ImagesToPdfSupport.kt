package com.thestudypath.pdfviewer.ui.tools.images

import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultImagesPdfBaseName = "images-document"

data class ImageInputItem(
    val id: String,
    val displayName: String,
    val workingFilePath: String,
    val sizeBytes: Long,
)

internal fun buildDefaultImagesPdfName(imageNames: List<String>): String {
    val baseName = when {
        imageNames.isEmpty() -> DefaultImagesPdfBaseName
        imageNames.size == 1 -> "${imageNames.first().removeExtension()}-pdf"
        else -> "${imageNames.first().removeExtension()}-${imageNames.size}-images"
    }
    return normalizeImagesPdfName(baseName)
}

internal fun normalizeImagesPdfName(
    rawValue: String,
    fallbackValue: String = "$DefaultImagesPdfBaseName.pdf",
): String {
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removePdfExtension() else trimmed.removePdfExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removePdfExtension().ifBlank { DefaultImagesPdfBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        sanitizedBase
    } else {
        "$sanitizedBase.pdf"
    }
}

internal fun buildInternalImagesPdfFileName(
    displayFileName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFileName
        .removePdfExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultImagesPdfBaseName }
        .take(48)

    return "images_${timestampMillis}_${sanitizedStem}.pdf"
}

private fun String.removeExtension(): String = substringBeforeLast('.', this)
private fun String.removePdfExtension(): String = removeSuffix(".pdf").removeSuffix(".PDF")

