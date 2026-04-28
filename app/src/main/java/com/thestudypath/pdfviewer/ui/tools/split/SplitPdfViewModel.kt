package com.thestudypath.pdfviewer.ui.tools.split

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfSplitOutput
import com.thestudypath.pdfviewer.processing.PdfSplitRequest
import com.thestudypath.pdfviewer.processing.PdfSplitResult
import com.thestudypath.pdfviewer.processing.PdfSplitTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class SplitPdfViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(SplitPdfUiState())
    val uiState: StateFlow<SplitPdfUiState> = _uiState.asStateFlow()

    private var isOutputBaseNameEdited = false

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to split.") }
            return
        }

        isOutputBaseNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                selectedDocumentPageCount = selectedDocument.pageCount.takeIf { count -> count > 0 },
                showDocumentPicker = false,
                isInspectingDocument = true,
                outputBaseName = buildDefaultSplitBaseName(selectedDocument),
                resultDocuments = emptyList(),
                errorMessage = null,
            )
        }

        inspectSelectedDocument(selectedDocument)
    }

    fun updateSplitMode(mode: SplitMode) {
        _uiState.update {
            it.copy(
                splitMode = mode,
                resultDocuments = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun updatePagesPerSplitInput(value: String) {
        _uiState.update {
            it.copy(
                pagesPerSplitInput = value,
                resultDocuments = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun updateCustomRangesInput(value: String) {
        _uiState.update {
            it.copy(
                customRangesInput = value,
                resultDocuments = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun updateOutputBaseName(value: String) {
        isOutputBaseNameEdited = true
        _uiState.update {
            it.copy(
                outputBaseName = value,
                resultDocuments = emptyList(),
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun splitSelectedDocument(onSplitSucceeded: (List<DocumentItem>) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to split.") }
            return
        }

        val pageCount = state.selectedDocumentPageCount
        if (pageCount == null || pageCount <= 0) {
            _uiState.update {
                it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.")
            }
            return
        }

        val normalizedBaseName = normalizeSplitBaseName(
            rawValue = state.outputBaseName,
            fallbackValue = buildDefaultSplitBaseName(selectedDocument),
        )

        val pageRanges = runCatching {
            when (state.splitMode) {
                SplitMode.EveryNPages -> buildPageRangesForEveryNPages(
                    totalPageCount = pageCount,
                    pagesPerSplit = state.pagesPerSplitInput.toIntOrNull()
                        ?: error("Enter a valid number of pages per output PDF."),
                )
                SplitMode.CustomRanges -> parseCustomPageRanges(
                    rawValue = state.customRangesInput,
                    totalPageCount = pageCount,
                )
            }
        }.getOrElse { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Invalid split settings.") }
            return
        }

        val timestampMillis = System.currentTimeMillis()
        val outputNames = pageRanges.mapIndexed { index, pageRange ->
            buildSplitOutputName(
                baseFileName = normalizedBaseName,
                timestampMillis = timestampMillis,
                outputIndex = index,
                pageRange = pageRange,
            )
        }
        val splitTargets = outputNames.zip(pageRanges).map { (outputName, pageRange) ->
            PdfSplitTarget(
                pageRange = pageRange,
                outputFile = File(appContext.filesDir, outputName.internalFileName),
            )
        }

        _uiState.update {
            it.copy(
                outputBaseName = normalizedBaseName,
                isSplitting = true,
                resultDocuments = emptyList(),
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.splitDocument(
                PdfSplitRequest(
                    inputFile = File(selectedDocument.filePath),
                    targets = splitTargets,
                )
            ).onSuccess { result ->
                val documents = result.toDocumentItems(outputNames)
                _uiState.update {
                    it.copy(
                        outputBaseName = normalizedBaseName,
                        isSplitting = false,
                        resultDocuments = documents,
                    )
                }
                onSplitSucceeded(documents)
            }.onFailure { throwable ->
                splitTargets.forEach { it.outputFile.delete() }
                _uiState.update {
                    it.copy(
                        outputBaseName = normalizedBaseName,
                        isSplitting = false,
                        errorMessage = throwable.message ?: "Failed to split the PDF.",
                    )
                }
            }
        }
    }

    private fun inspectSelectedDocument(selectedDocument: DocumentItem) {
        viewModelScope.launch {
            processingEngine.inspectDocument(File(selectedDocument.filePath))
                .onSuccess { inspection ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedDocument = selectedDocument,
                            selectedDocumentPageCount = inspection.pageCount,
                            isInspectingDocument = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedDocument = selectedDocument,
                            selectedDocumentPageCount = null,
                            isInspectingDocument = false,
                            errorMessage = throwable.message
                                ?: "Failed to inspect ${selectedDocument.displayName}.",
                        )
                    }
                }
        }
    }

    private fun PdfSplitResult.toDocumentItems(outputNames: List<SplitOutputName>): List<DocumentItem> {
        return outputs.mapIndexed { index, splitOutput ->
            splitOutput.toDocumentItem(outputNames.getOrNull(index))
        }
    }

    private fun PdfSplitOutput.toDocumentItem(
        outputName: SplitOutputName?,
    ): DocumentItem {
        val createdAt = System.currentTimeMillis()
        return DocumentItem(
            id = outputFile.absolutePath,
            fileName = outputName?.internalFileName ?: outputFile.name,
            displayName = outputName?.displayName ?: outputFile.name,
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
                if (modelClass.isAssignableFrom(SplitPdfViewModel::class.java)) {
                    return SplitPdfViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

