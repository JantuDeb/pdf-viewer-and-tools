package com.thestudypath.pdfviewer.ui.tools.watermark

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfTextWatermarkRequest
import com.thestudypath.pdfviewer.processing.PdfTextWatermarkResult
import com.thestudypath.pdfviewer.processing.PdfWatermarkPlacement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

internal class WatermarkPdfViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(WatermarkPdfUiState())
    internal val uiState: StateFlow<WatermarkPdfUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to watermark.") }
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
                outputFileName = buildDefaultWatermarkedPdfFileName(selectedDocument),
                resultDocument = null,
                watermarkedPageCount = null,
                errorMessage = null,
            )
        }
        inspectSelectedDocument(selectedDocument)
    }

    internal fun updateWatermarkText(value: String) {
        _uiState.update {
            it.copy(
                watermarkText = value,
                resultDocument = null,
                watermarkedPageCount = null,
                errorMessage = null,
            )
        }
    }

    internal fun updatePlacement(placement: PdfWatermarkPlacement) {
        _uiState.update {
            it.copy(
                placement = placement,
                resultDocument = null,
                watermarkedPageCount = null,
                errorMessage = null,
            )
        }
    }

    internal fun updateSizeOption(option: WatermarkSizeOption) {
        _uiState.update {
            it.copy(
                sizeOption = option,
                resultDocument = null,
                watermarkedPageCount = null,
                errorMessage = null,
            )
        }
    }

    internal fun updatePageRangesInput(value: String) {
        _uiState.update {
            it.copy(
                pageRangesInput = value,
                resultDocument = null,
                watermarkedPageCount = null,
                errorMessage = null,
            )
        }
    }

    internal fun updateOutputFileName(value: String) {
        isOutputNameEdited = true
        _uiState.update {
            it.copy(
                outputFileName = value,
                resultDocument = null,
                watermarkedPageCount = null,
                errorMessage = null,
            )
        }
    }

    internal fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    internal fun applyWatermark(onSuccess: (DocumentItem) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to watermark.") }
            return
        }
        val totalPages = state.selectedDocumentPageCount
        if (totalPages == null || totalPages <= 0) {
            _uiState.update { it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.") }
            return
        }

        val normalizedText = state.watermarkText.trim()
        if (normalizedText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter watermark text before saving the new PDF.") }
            return
        }

        val pageRanges = runCatching {
            parseWatermarkPageRangesInput(state.pageRangesInput, totalPages)
        }.getOrElse { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Enter valid page ranges.") }
            return
        }

        val normalizedOutputName = normalizeWatermarkPdfFileName(state.outputFileName)
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalWatermarkPdfFileName(
            displayFileName = normalizedOutputName,
            timestampMillis = timestamp,
        )
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isProcessing = true,
                resultDocument = null,
                watermarkedPageCount = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.addTextWatermark(
                PdfTextWatermarkRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputFile = outputFile,
                    text = normalizedText,
                    pageRanges = pageRanges,
                    placement = state.placement,
                    textScalePercent = state.sizeOption.textScalePercent,
                )
            ).onSuccess { result ->
                val watermarkedDocument = result.toDocumentItem(internalFileName, normalizedOutputName)
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isProcessing = false,
                        resultDocument = watermarkedDocument,
                        watermarkedPageCount = result.watermarkedPageCount,
                    )
                }
                onSuccess(watermarkedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isProcessing = false,
                        errorMessage = throwable.message ?: "Failed to apply the watermark.",
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
                        val shouldRefreshOutputName = !isOutputNameEdited || it.outputFileName.isBlank()
                        it.copy(
                            selectedDocument = selectedDocument,
                            selectedDocumentPageCount = inspection.pageCount,
                            isInspectingDocument = false,
                            outputFileName = if (shouldRefreshOutputName) {
                                buildDefaultWatermarkedPdfFileName(selectedDocument)
                            } else {
                                it.outputFileName
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

    private fun PdfTextWatermarkResult.toDocumentItem(
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
                if (modelClass.isAssignableFrom(WatermarkPdfViewModel::class.java)) {
                    return WatermarkPdfViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

