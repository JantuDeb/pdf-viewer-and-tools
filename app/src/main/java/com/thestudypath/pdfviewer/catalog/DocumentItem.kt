package com.thestudypath.pdfviewer.catalog

import com.thestudypath.pdfviewer.RecentFile

enum class DocumentSourceType {
    Imported,
    Scanned,
    Created,
    ToolOutput,
}

data class DocumentItem(
    val id: String,
    val fileName: String,
    val displayName: String,
    val filePath: String,
    val mimeType: String = "application/pdf",
    val sizeBytes: Long,
    val pageCount: Int,
    val lastOpenedAt: Long,
    val lastModifiedAt: Long,
    val sourceType: DocumentSourceType = DocumentSourceType.Imported,
    val isPasswordProtected: Boolean = false,
    val thumbnailKey: String = filePath,
)

fun RecentFile.toDocumentItem() = DocumentItem(
    id = filePath,
    fileName = fileName,
    displayName = displayName,
    filePath = filePath,
    sizeBytes = fileSize,
    pageCount = pageCount,
    lastOpenedAt = lastOpenedAt,
    lastModifiedAt = lastOpenedAt,
)

fun DocumentItem.toRecentFile() = RecentFile(
    fileName = fileName,
    displayName = displayName,
    filePath = filePath,
    fileSize = sizeBytes,
    lastOpenedAt = lastOpenedAt,
    pageCount = pageCount,
)

