package com.thestudypath.pdfviewer

data class RecentFile(
    val fileName: String,
    val displayName: String,
    val filePath: String,
    val fileSize: Long,
    val lastOpenedAt: Long,
    val pageCount: Int = 0,
)

