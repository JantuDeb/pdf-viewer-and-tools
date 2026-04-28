package com.thestudypath.pdfviewer.ui.tools.merge

import com.thestudypath.pdfviewer.catalog.DocumentItem
import java.util.Locale

private val invalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val extraWhitespace = Regex("\\s+")
private const val DefaultMergeOutputBaseName = "merged-document"

internal fun resolveMergeSelection(
    currentSelection: List<DocumentItem>,
    allDocuments: List<DocumentItem>,
    selectedDocumentIds: Set<String>,
): List<DocumentItem> {
    if (selectedDocumentIds.isEmpty()) return emptyList()

    val currentIds = currentSelection.map { it.id }.toSet()
    return buildList {
        currentSelection.forEach { document ->
            if (selectedDocumentIds.contains(document.id)) add(document)
        }
        allDocuments.forEach { document ->
            if (selectedDocumentIds.contains(document.id) && !currentIds.contains(document.id)) {
                add(document)
            }
        }
    }
}

internal fun buildDefaultMergeOutputName(selectedDocuments: List<DocumentItem>): String {
    val baseName = when {
        selectedDocuments.isEmpty() -> DefaultMergeOutputBaseName
        selectedDocuments.size == 1 -> "${selectedDocuments.first().displayName.removePdfExtension()}-merged"
        else -> "${selectedDocuments.first().displayName.removePdfExtension()}-merged-${selectedDocuments.size}-files"
    }
    return normalizeMergeOutputName(baseName)
}

internal fun normalizeMergeOutputName(
    rawValue: String,
    fallbackValue: String = "$DefaultMergeOutputBaseName.pdf",
): String {
    val trimmed = rawValue.trim()
    val baseValue = if (trimmed.isBlank()) fallbackValue.removePdfExtension() else trimmed.removePdfExtension()
    val sanitizedBase = baseValue
        .replace(invalidFileNameCharacters, " ")
        .replace(extraWhitespace, " ")
        .trim()
        .ifBlank { fallbackValue.removePdfExtension().ifBlank { DefaultMergeOutputBaseName } }

    return if (sanitizedBase.lowercase(Locale.getDefault()).endsWith(".pdf")) {
        sanitizedBase
    } else {
        "$sanitizedBase.pdf"
    }
}

internal fun buildInternalMergeFileName(
    displayFileName: String,
    timestampMillis: Long,
): String {
    val sanitizedStem = displayFileName
        .removePdfExtension()
        .replace(invalidFileNameCharacters, "_")
        .replace(extraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { DefaultMergeOutputBaseName }
        .take(48)

    return "merged_${timestampMillis}_${sanitizedStem}.pdf"
}

private fun String.removePdfExtension(): String = removeSuffix(".pdf").removeSuffix(".PDF")

