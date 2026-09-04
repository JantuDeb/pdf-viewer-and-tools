package com.thestudypath.pdfviewer.ui.tools.pages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfRotatePagesRequest
import com.thestudypath.pdfviewer.processing.PdfRotatePagesResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class RotatePagesViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(RotatePagesUiState())
    val uiState: StateFlow<RotatePagesUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to rotate pages in.") }
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
                outputFileName = buildDefaultRotatedPdfFileName(selectedDocument),
                resultDocument = null,
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
                errorMessage = null,
            )
        }
    }

    fun updateRotationOption(option: PageRotationOption) {
        _uiState.update {
            it.copy(
                rotationOption = option,
                resultDocument = null,
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
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun rotatePages(onSuccess: (DocumentItem) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to rotate pages in.") }
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
            fallbackBaseName = "rotated-document",
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalPageToolFileName(
            prefix = "rotated",
            displayFileName = normalizedOutputName,
            fallbackBaseName = "rotated-document",
            timestampMillis = timestamp,
        )
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isProcessing = true,
                resultDocument = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.rotatePages(
                PdfRotatePagesRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputFile = outputFile,
                    pageRanges = pageRanges,
                    rotationDegrees = state.rotationOption.degrees,
                )
            ).onSuccess { result ->
                val rotatedDocument = result.toDocumentItem(internalFileName, normalizedOutputName)
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isProcessing = false,
                        resultDocument = rotatedDocument,
                    )
                }
                onSuccess(rotatedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isProcessing = false,
                        errorMessage = throwable.message ?: "Failed to rotate the selected pages.",
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

    private fun PdfRotatePagesResult.toDocumentItem(
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
                if (modelClass.isAssignableFrom(RotatePagesViewModel::class.java)) {
                    return RotatePagesViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

