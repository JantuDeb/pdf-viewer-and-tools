package com.thestudypath.pdfviewer.ui.tools.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfExtractPagesRequest
import com.thestudypath.pdfviewer.processing.PdfExtractPagesResult
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ExtractPagesViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(ExtractPagesUiState())
    val uiState: StateFlow<ExtractPagesUiState> = _uiState.asStateFlow()

    private var isOutputNameEdited = false

    fun showDocumentPicker() {
        _uiState.update { it.copy(showDocumentPicker = true, errorMessage = null) }
    }

    fun dismissDocumentPicker() {
        _uiState.update { it.copy(showDocumentPicker = false) }
    }

    fun confirmDocumentSelection(
        allDocuments: List<DocumentItem>,
        selectedDocumentId: String,
    ) {
        val selectedDocument = allDocuments.firstOrNull { it.id == selectedDocumentId }
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to extract pages from.") }
            return
        }

        isOutputNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                selectedDocumentPageCount = selectedDocument.pageCount.takeIf { count -> count > 0 },
                showDocumentPicker = false,
                isInspectingDocument = true,
                pageRangesInput = "1",
                outputFileName = buildDefaultExtractedPagesPdfFileName(selectedDocument),
                resultDocument = null,
                extractedPageCount = null,
                errorMessage = null,
            )
        }
        inspectSelectedDocument(selectedDocument)
    }

    fun updatePageRangesInput(value: String) {
        _uiState.update {
            it.copy(
                pageRangesInput = value,
                resultDocument = null,
                extractedPageCount = null,
                errorMessage = null,
            )
        }
    }

    fun updateOutputFileName(value: String) {
        isOutputNameEdited = true
        _uiState.update {
            it.copy(
                outputFileName = value,
                resultDocument = null,
                extractedPageCount = null,
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun extractPages(onSuccess: (DocumentItem) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to extract pages from.") }
            return
        }
        val totalPages = state.selectedDocumentPageCount
        if (totalPages == null || totalPages <= 0) {
            _uiState.update { it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.") }
            return
        }

        val pageRanges = runCatching {
            parsePageRangesInput(state.pageRangesInput, totalPages)
        }.getOrElse { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Enter valid page ranges.") }
            return
        }

        val normalizedOutputName = normalizePageToolPdfFileName(
            rawValue = state.outputFileName,
            fallbackBaseName = "extracted-pages",
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalPageToolFileName(
            prefix = "extract_pages",
            displayFileName = normalizedOutputName,
            fallbackBaseName = "extracted-pages",
            timestampMillis = timestamp,
        )
        val outputFile = File(appContext.filesDir, internalFileName)
        val extractedPageCount = countPagesInRanges(pageRanges)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isProcessing = true,
                resultDocument = null,
                extractedPageCount = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.extractPages(
                PdfExtractPagesRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputFile = outputFile,
                    pageRanges = pageRanges,
                )
            ).onSuccess { result ->
                val extractedDocument = result.toDocumentItem(internalFileName, normalizedOutputName)
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isProcessing = false,
                        resultDocument = extractedDocument,
                        extractedPageCount = extractedPageCount,
                    )
                }
                onSuccess(extractedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isProcessing = false,
                        extractedPageCount = null,
                        errorMessage = throwable.message ?: "Failed to extract the selected pages.",
                    )
                }
            }
        }
    }

    private fun inspectSelectedDocument(selectedDocument: DocumentItem) {
        viewModelScope.launch {
            processingEngine.inspectDocument(File(selectedDocument.filePath))
                .onSuccess { inspection ->
                    _uiState.update {
                        it.copy(
                            selectedDocument = selectedDocument,
                            selectedDocumentPageCount = inspection.pageCount,
                            isInspectingDocument = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            selectedDocument = selectedDocument,
                            selectedDocumentPageCount = null,
                            isInspectingDocument = false,
                            errorMessage = throwable.message ?: "Failed to inspect ${selectedDocument.displayName}.",
                        )
                    }
                }
        }
    }

    private fun PdfExtractPagesResult.toDocumentItem(
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
                if (modelClass.isAssignableFrom(ExtractPagesViewModel::class.java)) {
                    return ExtractPagesViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

