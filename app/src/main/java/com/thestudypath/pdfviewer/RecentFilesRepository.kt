package com.thestudypath.pdfviewer

import android.content.Context
import com.thestudypath.pdfviewer.data.AppDatabase
import com.thestudypath.pdfviewer.data.toDomain
import com.thestudypath.pdfviewer.data.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecentFilesRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).recentFileDao()

    fun observeRecentFiles(): Flow<List<RecentFile>> =
        dao.getAllFiles().map { list -> list.map { it.toDomain() } }

    suspend fun addOrUpdate(file: RecentFile) {
        dao.upsert(file.copy(lastOpenedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun remove(filePath: String) {
        dao.deleteByPath(filePath)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}

