package com.thestudypath.pdfviewer.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.io.File

class DocumentCatalogViewModel(
    private val documentCatalogRepository: DocumentCatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentCatalogUiState>(DocumentCatalogUiState.Loading)
    val uiState: StateFlow<DocumentCatalogUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var observeDocumentsJob: Job? = null

    init {
        observeDocuments()
    }

    fun retry() {
        observeDocuments()
    }

    fun addOrUpdate(document: DocumentItem) {
        launchAction(errorMessage = "Failed to update documents.") {
            documentCatalogRepository.addOrUpdate(document)
        }
    }

    fun onMissingDocumentOpened(filePath: String) {
        launchAction(
            successMessage = "File no longer exists.",
            errorMessage = "Failed to remove missing file.",
        ) {
            documentCatalogRepository.removeByPath(filePath)
        }
    }

    fun removeDocument(document: DocumentItem) {
        launchAction(
            successMessage = "Removed",
            errorMessage = "Failed to remove file.",
        ) {
            documentCatalogRepository.remove(document)
            runCatching { File(document.filePath).delete() }
        }
    }

    fun clearAll(documents: List<DocumentItem>) {
        launchAction(errorMessage = "Failed to clear documents.") {
            documents.forEach { runCatching { File(it.filePath).delete() } }
            documentCatalogRepository.clearAll()
        }
    }

    private fun observeDocuments() {
        observeDocumentsJob?.cancel()
        observeDocumentsJob = viewModelScope.launch {
            documentCatalogRepository.observeDocuments()
                .onStart { _uiState.value = DocumentCatalogUiState.Loading }
                .catch { throwable ->
                    _uiState.value = DocumentCatalogUiState.Error(
                        throwable.message ?: "Failed to load documents."
                    )
                }
                .collect { documents ->
                    _uiState.value = DocumentCatalogUiState.Success(documents)
                }
        }
    }

    private fun launchAction(
        successMessage: String? = null,
        errorMessage: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { successMessage?.let(_messages::tryEmit) }
                .onFailure { throwable ->
                    _messages.tryEmit(throwable.message ?: errorMessage)
                }
        }
    }

    companion object {
        fun factory(repository: DocumentCatalogRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DocumentCatalogViewModel::class.java)) {
                        return DocumentCatalogViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}

