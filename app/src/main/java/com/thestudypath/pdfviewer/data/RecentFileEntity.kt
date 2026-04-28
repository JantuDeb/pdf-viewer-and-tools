package com.thestudypath.pdfviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thestudypath.pdfviewer.RecentFile

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey val filePath: String,
    val fileName: String,
    val displayName: String,
    val fileSize: Long,
    val lastOpenedAt: Long,
    val pageCount: Int,
)

fun RecentFileEntity.toDomain() = RecentFile(
    fileName = fileName,
    displayName = displayName,
    filePath = filePath,
    fileSize = fileSize,
    lastOpenedAt = lastOpenedAt,
    pageCount = pageCount,
)

fun RecentFile.toEntity() = RecentFileEntity(
    filePath = filePath,
    fileName = fileName,
    displayName = displayName,
    fileSize = fileSize,
    lastOpenedAt = lastOpenedAt,
    pageCount = pageCount,
)

