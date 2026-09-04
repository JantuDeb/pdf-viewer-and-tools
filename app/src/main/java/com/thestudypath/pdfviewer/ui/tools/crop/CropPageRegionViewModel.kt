package com.thestudypath.pdfviewer.ui.tools.crop

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfCropPageRegionRequest
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

internal class CropPageRegionViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(CropPageRegionUiState())
    internal val uiState: StateFlow<CropPageRegionUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to crop.") }
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
                imageFormat = PdfImageExportFormat.Png,
                qualityOption = CropExportQualityOption.Standard,
                preset = PageCropPreset.FullPage,
                cropRegion = PageCropPreset.FullPage.region,
                outputFolderName = buildDefaultCropExportFolderName(selectedDocument),
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

    internal fun updateImageFormat(format: PdfImageExportFormat) {
        _uiState.update {
            it.copy(
                imageFormat = format,
                result = null,
                errorMessage = null,
            )
        }
    }

    internal fun updateQualityOption(option: CropExportQualityOption) {
        _uiState.update {
            it.copy(
                qualityOption = option,
                result = null,
                errorMessage = null,
            )
        }
    }

    internal fun applyPreset(preset: PageCropPreset) {
        val region = applyCropPreset(preset)
        _uiState.update {
            it.copy(
                preset = preset,
                cropRegion = region,
                result = null,
                errorMessage = null,
            )
        }
    }

    internal fun updateLeftPercent(value: Float) {
        updateCropRegion(_uiState.value.cropRegion.copy(leftPercent = value.toInt()))
    }

    internal fun updateTopPercent(value: Float) {
        updateCropRegion(_uiState.value.cropRegion.copy(topPercent = value.toInt()))
    }

    internal fun updateRightPercent(value: Float) {
        updateCropRegion(_uiState.value.cropRegion.copy(rightPercent = value.toInt()))
    }

    internal fun updateBottomPercent(value: Float) {
        updateCropRegion(_uiState.value.cropRegion.copy(bottomPercent = value.toInt()))
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

    internal fun exportCroppedRegions() {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to crop.") }
            return
        }
        val totalPages = state.selectedDocumentPageCount
        if (totalPages == null || totalPages <= 0) {
            _uiState.update { it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.") }
            return
        }

        val pageRanges = runCatching {
            parseCropPageRangesInput(state.pageRangesInput, totalPages)
        }.getOrElse { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Enter valid page ranges.") }
            return
        }

        val normalizedFolderName = normalizeCropExportFolderName(state.outputFolderName)
        val timestamp = System.currentTimeMillis()
        val internalDirectoryName = buildInternalCropExportDirectoryName(
            displayFolderName = normalizedFolderName,
            timestampMillis = timestamp,
        )
        val outputDirectory = File(appContext.filesDir, "page_crop_exports/$internalDirectoryName")

        _uiState.update {
            it.copy(
                outputFolderName = normalizedFolderName,
                isProcessing = true,
                result = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.cropPageRegions(
                PdfCropPageRegionRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputDirectory = outputDirectory,
                    outputFileNamePrefix = normalizedFolderName,
                    pageRanges = pageRanges,
                    cropRect = state.cropRegion.toNormalizedCropRect(),
                    format = state.imageFormat,
                    renderDpi = state.qualityOption.dpi,
                ),
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
                        errorMessage = throwable.message ?: "Failed to export the cropped page regions.",
                    )
                }
            }
        }
    }

    private fun updateCropRegion(region: CropRegionSelection) {
        val sanitized = sanitizeCropRegion(region)
        _uiState.update {
            it.copy(
                cropRegion = sanitized,
                preset = resolvePresetForRegion(sanitized),
                result = null,
                errorMessage = null,
            )
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
                                buildDefaultCropExportFolderName(selectedDocument)
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
                if (modelClass.isAssignableFrom(CropPageRegionViewModel::class.java)) {
                    return CropPageRegionViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

