package com.thestudypath.pdfviewer.ui.tools.crop

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat
import com.thestudypath.pdfviewer.processing.PdfNormalizedCropRect
import com.thestudypath.pdfviewer.processing.PdfPageRange
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultCropExportBaseName = "cropped-page-region"
private const val MinimumCropGapPercent = 5

internal enum class CropExportQualityOption(
    val dpi: Int,
    val label: String,
) {
    Standard(144, "Standard"),
    High(216, "High"),
}

internal enum class PageCropPreset(
    val label: String,
    val region: CropRegionSelection,
) {
    FullPage(
        label = "Full page",
        region = CropRegionSelection(0, 0, 100, 100),
    ),
    Header(
        label = "Header",
        region = CropRegionSelection(0, 0, 100, 35),
    ),
    Footer(
        label = "Footer",
        region = CropRegionSelection(0, 65, 100, 100),
    ),
    LeftHalf(
        label = "Left half",
        region = CropRegionSelection(0, 0, 50, 100),
    ),
    RightHalf(
        label = "Right half",
        region = CropRegionSelection(50, 0, 100, 100),
    ),
    CenterSquare(
        label = "Center square",
        region = CropRegionSelection(20, 20, 80, 80),
    ),
    Custom(
        label = "Custom",
        region = CropRegionSelection(10, 10, 90, 90),
    ),
}

internal data class CropRegionSelection(
    val leftPercent: Int,
    val topPercent: Int,
    val rightPercent: Int,
    val bottomPercent: Int,
) {
    fun toNormalizedCropRect(): PdfNormalizedCropRect = PdfNormalizedCropRect(
        leftFraction = leftPercent / 100f,
        topFraction = topPercent / 100f,
        rightFraction = rightPercent / 100f,
        bottomFraction = bottomPercent / 100f,
    )
}

internal fun buildDefaultCropExportFolderName(document: DocumentItem?): String {
    val baseName = document?.displayName?.removePdfExtension()?.takeIf { it.isNotBlank() }
        ?.let { "$it-cropped-region" }
        ?: DefaultCropExportBaseName
    return normalizeCropExportFolderName(baseName)
}

internal fun normalizeCropExportFolderName(
    rawValue: String,
    fallbackBaseName: String = DefaultCropExportBaseName,
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

internal fun buildInternalCropExportDirectoryName(
    displayFolderName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFolderName
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultCropExportBaseName }
        .take(48)

    return "crop_region_${timestampMillis}_${sanitizedStem}"
}

internal fun parseCropPageRangesInput(
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
        "Enter one or more page ranges, for example 1-3, 5, 8-10. Leave it blank to crop every page."
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

internal fun applyCropPreset(preset: PageCropPreset): CropRegionSelection = preset.region

internal fun sanitizeCropRegion(selection: CropRegionSelection): CropRegionSelection {
    val left = selection.leftPercent.coerceIn(0, 100 - MinimumCropGapPercent)
    val top = selection.topPercent.coerceIn(0, 100 - MinimumCropGapPercent)
    val right = selection.rightPercent.coerceIn(left + MinimumCropGapPercent, 100)
    val bottom = selection.bottomPercent.coerceIn(top + MinimumCropGapPercent, 100)
    return CropRegionSelection(
        leftPercent = left,
        topPercent = top,
        rightPercent = right,
        bottomPercent = bottom,
    )
}

internal fun resolvePresetForRegion(selection: CropRegionSelection): PageCropPreset =
    PageCropPreset.entries.firstOrNull { it != PageCropPreset.Custom && it.region == selection }
        ?: PageCropPreset.Custom

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

