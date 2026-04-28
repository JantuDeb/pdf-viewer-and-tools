package com.thestudypath.pdfviewer.ui.tools.compress

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfCompressRequest
import com.thestudypath.pdfviewer.processing.PdfCompressResult
import com.thestudypath.pdfviewer.processing.PdfCompressionProfile
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class CompressPdfViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(CompressPdfUiState())
    val uiState: StateFlow<CompressPdfUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to compress.") }
            return
        }

        isOutputNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                selectedDocumentPageCount = selectedDocument.pageCount.takeIf { count -> count > 0 },
                showDocumentPicker = false,
                isInspectingDocument = true,
                outputFileName = buildDefaultCompressedFileName(selectedDocument),
                resultDocument = null,
                compressionSummary = null,
                errorMessage = null,
            )
        }

        inspectSelectedDocument(selectedDocument)
    }

    fun updateOutputFileName(value: String) {
        isOutputNameEdited = true
        _uiState.update {
            it.copy(
                outputFileName = value,
                resultDocument = null,
                compressionSummary = null,
                errorMessage = null,
            )
        }
    }

    fun updateProfile(profile: PdfCompressionProfile) {
        _uiState.update {
            it.copy(
                selectedProfile = profile,
                resultDocument = null,
                compressionSummary = null,
                errorMessage = null,
            )
        }
    }

    fun updateKeepMetadata(keepMetadata: Boolean) {
        _uiState.update {
            it.copy(
                keepMetadata = keepMetadata,
                resultDocument = null,
                compressionSummary = null,
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun compressSelectedDocument(onCompressSucceeded: (DocumentItem) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to compress.") }
            return
        }

        val pageCount = state.selectedDocumentPageCount
        if (pageCount == null || pageCount <= 0) {
            _uiState.update {
                it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.")
            }
            return
        }

        val normalizedOutputName = normalizeCompressedFileName(
            rawValue = state.outputFileName,
            fallbackValue = buildDefaultCompressedFileName(selectedDocument),
        )
        val selectedProfile = state.selectedProfile
        val keepMetadata = state.keepMetadata
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalCompressedFileName(normalizedOutputName, timestamp)
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isCompressing = true,
                resultDocument = null,
                compressionSummary = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.compressDocument(
                PdfCompressRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputFile = outputFile,
                    profile = selectedProfile,
                    keepMetadata = keepMetadata,
                )
            ).onSuccess { result ->
                val compressedDocument = result.toDocumentItem(
                    internalFileName = internalFileName,
                    displayName = normalizedOutputName,
                )
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isCompressing = false,
                        resultDocument = compressedDocument,
                        compressionSummary = CompressionSummary(
                            originalSizeBytes = result.originalSizeBytes,
                            compressedSizeBytes = result.compressedSizeBytes,
                            savingsPercent = result.savingsPercent,
                            appliedProfile = selectedProfile,
                        ),
                    )
                }
                onCompressSucceeded(compressedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isCompressing = false,
                        errorMessage = throwable.message ?: "Failed to compress the selected PDF.",
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

    private fun PdfCompressResult.toDocumentItem(
        internalFileName: String,
        displayName: String,
    ): DocumentItem {
        val createdAt = System.currentTimeMillis()
        return DocumentItem(
            id = outputFile.absolutePath,
            fileName = internalFileName,
            displayName = displayName,
            filePath = outputFile.absolutePath,
            sizeBytes = compressedSizeBytes,
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
                if (modelClass.isAssignableFrom(CompressPdfViewModel::class.java)) {
                    return CompressPdfViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

