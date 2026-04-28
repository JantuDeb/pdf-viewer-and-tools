package com.thestudypath.pdfviewer.ui.tools.text

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import com.thestudypath.pdfviewer.processing.TextExtractionRequest
import com.thestudypath.pdfviewer.processing.TextExtractionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ExtractTextViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(ExtractTextUiState())
    val uiState: StateFlow<ExtractTextUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to extract text from.") }
            return
        }

        isOutputNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                selectedDocumentPageCount = selectedDocument.pageCount.takeIf { count -> count > 0 },
                showDocumentPicker = false,
                isInspectingDocument = true,
                outputFileName = buildDefaultExtractedTextFileName(selectedDocument),
                extractedTextResult = null,
                errorMessage = null,
            )
        }

        inspectSelectedDocument(selectedDocument)
    }

    fun updateExtractionScope(scope: TextExtractionScope) {
        _uiState.update {
            it.copy(
                extractionScope = scope,
                extractedTextResult = null,
                errorMessage = null,
            )
        }
    }

    fun updateStartPageInput(value: String) {
        _uiState.update {
            it.copy(
                startPageInput = value,
                extractedTextResult = null,
                errorMessage = null,
            )
        }
    }

    fun updateEndPageInput(value: String) {
        _uiState.update {
            it.copy(
                endPageInput = value,
                extractedTextResult = null,
                errorMessage = null,
            )
        }
    }

    fun updateOutputFileName(value: String) {
        isOutputNameEdited = true
        _uiState.update {
            it.copy(
                outputFileName = value,
                extractedTextResult = null,
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun extractText() {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to extract text from.") }
            return
        }

        val totalPages = state.selectedDocumentPageCount
        if (totalPages == null || totalPages <= 0) {
            _uiState.update {
                it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.")
            }
            return
        }

        val pageRange = runCatching {
            resolveExtractionPageRange(
                scope = state.extractionScope,
                totalPageCount = totalPages,
                startPageInput = state.startPageInput,
                endPageInput = state.endPageInput,
            )
        }.getOrElse { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Invalid extraction range.") }
            return
        }

        val normalizedOutputName = normalizeExtractedTextFileName(
            rawValue = state.outputFileName,
            fallbackValue = buildDefaultExtractedTextFileName(selectedDocument),
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalExtractedTextFileName(normalizedOutputName, timestamp)
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isExtracting = true,
                extractedTextResult = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.extractText(
                TextExtractionRequest(
                    inputFile = File(selectedDocument.filePath),
                    pageRange = pageRange,
                )
            ).onSuccess { result ->
                runCatching {
                    outputFile.writeText(result.text)
                }.onSuccess {
                    _uiState.update {
                        it.copy(
                            outputFileName = normalizedOutputName,
                            isExtracting = false,
                            extractedTextResult = result.toUiResult(outputFile, normalizedOutputName),
                        )
                    }
                }.onFailure { throwable ->
                    outputFile.delete()
                    _uiState.update {
                        it.copy(
                            outputFileName = normalizedOutputName,
                            isExtracting = false,
                            errorMessage = throwable.message ?: "Failed to save extracted text.",
                        )
                    }
                }
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isExtracting = false,
                        errorMessage = throwable.message ?: "Failed to extract text from the selected PDF.",
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
                            startPageInput = "1",
                            endPageInput = inspection.pageCount.toString(),
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

    private fun TextExtractionResult.toUiResult(
        outputFile: File,
        displayName: String,
    ) = ExtractedTextResult(
        outputFile = outputFile,
        displayName = displayName,
        pageRange = pageRange,
        text = text,
        characterCount = characterCount,
    )

    companion object {
        fun factory(
            context: Context,
            processingEngine: PdfProcessingEngine = PdfBoxProcessingEngine(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ExtractTextViewModel::class.java)) {
                    return ExtractTextViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

