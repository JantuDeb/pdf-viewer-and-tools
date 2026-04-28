package com.thestudypath.pdfviewer.ui.tools.merge

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfMergeRequest
import com.thestudypath.pdfviewer.processing.PdfMergeResult
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MergePdfViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(MergePdfUiState())
    val uiState: StateFlow<MergePdfUiState> = _uiState.asStateFlow()

    private var isOutputNameEdited = false

    fun showDocumentPicker() {
        _uiState.update { it.copy(showDocumentPicker = true, errorMessage = null) }
    }

    fun dismissDocumentPicker() {
        _uiState.update { it.copy(showDocumentPicker = false) }
    }

    fun confirmDocumentSelection(
        allDocuments: List<DocumentItem>,
        selectedDocumentIds: Set<String>,
    ) {
        val resolvedSelection = resolveMergeSelection(
            currentSelection = _uiState.value.selectedDocuments,
            allDocuments = allDocuments,
            selectedDocumentIds = selectedDocumentIds,
        )
        updateSelectedDocuments(resolvedSelection)
    }

    fun updateOutputFileName(value: String) {
        isOutputNameEdited = true
        _uiState.update {
            it.copy(
                outputFileName = value,
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun removeDocument(documentId: String) {
        updateSelectedDocuments(_uiState.value.selectedDocuments.filterNot { it.id == documentId })
    }

    fun moveDocumentUp(documentId: String) {
        val documents = _uiState.value.selectedDocuments.toMutableList()
        val index = documents.indexOfFirst { it.id == documentId }
        if (index <= 0) return
        val item = documents.removeAt(index)
        documents.add(index - 1, item)
        updateSelectedDocuments(documents)
    }

    fun moveDocumentDown(documentId: String) {
        val documents = _uiState.value.selectedDocuments.toMutableList()
        val index = documents.indexOfFirst { it.id == documentId }
        if (index == -1 || index >= documents.lastIndex) return
        val item = documents.removeAt(index)
        documents.add(index + 1, item)
        updateSelectedDocuments(documents)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun mergeSelectedDocuments(onMergeSucceeded: (DocumentItem) -> Unit) {
        val selectedDocuments = _uiState.value.selectedDocuments
        if (selectedDocuments.size < 2) {
            _uiState.update { it.copy(errorMessage = "Select at least 2 PDFs to merge.") }
            return
        }

        val normalizedOutputName = normalizeMergeOutputName(
            rawValue = _uiState.value.outputFileName,
            fallbackValue = buildDefaultMergeOutputName(selectedDocuments),
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalMergeFileName(normalizedOutputName, timestamp)
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isMerging = true,
                errorMessage = null,
                resultDocument = null,
            )
        }

        viewModelScope.launch {
            processingEngine.mergeDocuments(
                PdfMergeRequest(
                    inputFiles = selectedDocuments.map { File(it.filePath) },
                    outputFile = outputFile,
                )
            ).onSuccess { result ->
                val mergedDocument = result.toDocumentItem(
                    internalFileName = internalFileName,
                    displayName = normalizedOutputName,
                )
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isMerging = false,
                        resultDocument = mergedDocument,
                    )
                }
                onMergeSucceeded(mergedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isMerging = false,
                        errorMessage = throwable.message ?: "Failed to merge PDFs.",
                    )
                }
            }
        }
    }

    private fun updateSelectedDocuments(documents: List<DocumentItem>) {
        if (documents.isEmpty()) {
            isOutputNameEdited = false
        }
        _uiState.update { currentState ->
            currentState.copy(
                selectedDocuments = documents,
                showDocumentPicker = false,
                outputFileName = if (isOutputNameEdited && currentState.outputFileName.isNotBlank()) {
                    currentState.outputFileName
                } else {
                    buildDefaultMergeOutputName(documents)
                },
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    private fun PdfMergeResult.toDocumentItem(
        internalFileName: String,
        displayName: String,
    ): DocumentItem {
        val createdAt = System.currentTimeMillis()
        return DocumentItem(
            id = outputFile.absolutePath,
            fileName = internalFileName,
            displayName = displayName,
            filePath = outputFile.absolutePath,
            sizeBytes = fileSizeBytes,
            pageCount = pageCount,
            lastOpenedAt = createdAt,
            lastModifiedAt = outputFile.lastModified().takeIf { it > 0 } ?: createdAt,
            sourceType = DocumentSourceType.ToolOutput,
        )
    }

    companion object {
        fun factory(
            context: Context,
            processingEngine: PdfProcessingEngine = PdfBoxProcessingEngine(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MergePdfViewModel::class.java)) {
                    return MergePdfViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

