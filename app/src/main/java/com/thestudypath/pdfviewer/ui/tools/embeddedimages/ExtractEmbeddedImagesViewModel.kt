package com.thestudypath.pdfviewer.ui.tools.embeddedimages

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.ExtractEmbeddedImagesRequest
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

internal class ExtractEmbeddedImagesViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(ExtractEmbeddedImagesUiState())
    internal val uiState: StateFlow<ExtractEmbeddedImagesUiState> = _uiState.asStateFlow()

    private var isOutputNameEdited = false

    internal fun showDocumentPicker() {
        _uiState.update { it.copy(showDocumentPicker = true, errorMessage = null) }
    }

    internal fun dismissDocumentPicker() {
        _uiState.update { it.copy(showDocumentPicker = false) }
    }

    internal fun confirmDocumentSelection(
        allDocuments: List<DocumentItem>,
        selectedDocumentId: String,
    ) {
        val selectedDocument = allDocuments.firstOrNull { it.id == selectedDocumentId }
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to extract images from.") }
            return
        }

        isOutputNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                selectedDocumentPageCount = selectedDocument.pageCount.takeIf { count -> count > 0 },
                showDocumentPicker = false,
                isInspectingDocument = true,
                pageRangesInput = "",
                outputFolderName = buildDefaultEmbeddedImagesFolderName(selectedDocument),
                result = null,
                errorMessage = null,
            )
        }
        inspectSelectedDocument(selectedDocument)
    }

    internal fun updatePageRangesInput(value: String) {
        _uiState.update {
            it.copy(
                pageRangesInput = value,
                result = null,
                errorMessage = null,
            )
        }
    }

    internal fun updateOutputFolderName(value: String) {
        isOutputNameEdited = true
        _uiState.update {
            it.copy(
                outputFolderName = value,
                result = null,
                errorMessage = null,
            )
        }
    }

    internal fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    internal fun extractImages() {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to extract images from.") }
            return
        }
        val totalPages = state.selectedDocumentPageCount
        if (totalPages == null || totalPages <= 0) {
            _uiState.update { it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.") }
            return
        }

        val pageRanges = runCatching {
            parseEmbeddedImagesPageRangesInput(state.pageRangesInput, totalPages)
        }.getOrElse { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Enter valid page ranges.") }
            return
        }

        val normalizedFolderName = normalizeEmbeddedImagesFolderName(state.outputFolderName)
        val timestamp = System.currentTimeMillis()
        val internalDirectoryName = buildInternalEmbeddedImagesDirectoryName(
            displayFolderName = normalizedFolderName,
            timestampMillis = timestamp,
        )
        val outputDirectory = File(appContext.filesDir, "embedded_image_exports/$internalDirectoryName")

        _uiState.update {
            it.copy(
                outputFolderName = normalizedFolderName,
                isProcessing = true,
                result = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.extractEmbeddedImages(
                ExtractEmbeddedImagesRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputDirectory = outputDirectory,
                    outputFileNamePrefix = normalizedFolderName,
                    pageRanges = pageRanges,
                )
            ).onSuccess { result ->
                _uiState.update {
                    it.copy(
                        outputFolderName = normalizedFolderName,
                        isProcessing = false,
                        result = result,
                    )
                }
            }.onFailure { throwable ->
                outputDirectory.deleteRecursively()
                _uiState.update {
                    it.copy(
                        outputFolderName = normalizedFolderName,
                        isProcessing = false,
                        errorMessage = throwable.message ?: "Failed to extract embedded images.",
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
                        val shouldRefreshOutputName = !isOutputNameEdited || it.outputFolderName.isBlank()
                        it.copy(
                            selectedDocument = selectedDocument,
                            selectedDocumentPageCount = inspection.pageCount,
                            isInspectingDocument = false,
                            outputFolderName = if (shouldRefreshOutputName) {
                                buildDefaultEmbeddedImagesFolderName(selectedDocument)
                            } else {
                                it.outputFolderName
                            },
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

    companion object {
        fun factory(
            context: Context,
            processingEngine: PdfProcessingEngine = PdfBoxProcessingEngine(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ExtractEmbeddedImagesViewModel::class.java)) {
                    return ExtractEmbeddedImagesViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

