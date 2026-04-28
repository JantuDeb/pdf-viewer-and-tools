package com.thestudypath.pdfviewer

import android.content.Context
import kotlinx.coroutines.flow.Flow

@Deprecated("Use RecentFilesRepository instead.")
class RecentFilesManager(context: Context) {

    private val repository = RecentFilesRepository(context)

    val recentFilesFlow: Flow<List<RecentFile>> = repository.observeRecentFiles()

    suspend fun addOrUpdate(file: RecentFile) {
        repository.addOrUpdate(file)
    }

    suspend fun remove(filePath: String) {
        repository.remove(filePath)
    }

    suspend fun clearAll() {
        repository.clearAll()
    }
}
