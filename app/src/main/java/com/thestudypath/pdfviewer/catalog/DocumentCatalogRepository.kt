package com.thestudypath.pdfviewer.catalog

import android.content.Context
import com.thestudypath.pdfviewer.RecentFilesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocumentCatalogRepository(context: Context) {

    private val recentFilesRepository = RecentFilesRepository(context)

    fun observeDocuments(): Flow<List<DocumentItem>> =
        recentFilesRepository.observeRecentFiles().map { files ->
            files.map { it.toDocumentItem() }
        }

    suspend fun addOrUpdate(document: DocumentItem) {
        recentFilesRepository.addOrUpdate(document.toRecentFile())
    }

    suspend fun remove(document: DocumentItem) {
        recentFilesRepository.remove(document.filePath)
    }

    suspend fun removeByPath(filePath: String) {
        recentFilesRepository.remove(filePath)
    }

    suspend fun clearAll() {
        recentFilesRepository.clearAll()
    }
}

