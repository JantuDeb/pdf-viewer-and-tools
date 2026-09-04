package com.thestudypath.pdfviewer.ui.tools.password

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfRemovePasswordRequest
import com.thestudypath.pdfviewer.processing.PdfRemovePasswordResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class RemovePasswordViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(RemovePasswordUiState())
    val uiState: StateFlow<RemovePasswordUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to unlock.") }
            return
        }

        isOutputNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                showDocumentPicker = false,
                outputFileName = buildDefaultUnlockedPdfFileName(selectedDocument),
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun updateCurrentPassword(value: String) {
        _uiState.update {
            it.copy(
                currentPassword = value,
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

    fun removePassword(onSuccess: (DocumentItem) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to unlock.") }
            return
        }
        if (state.currentPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter the current password for this PDF.") }
            return
        }

        val normalizedOutputName = normalizeUnlockedPdfFileName(
            rawValue = state.outputFileName,
            fallbackValue = buildDefaultUnlockedPdfFileName(selectedDocument),
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalUnlockedPdfFileName(normalizedOutputName, timestamp)
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isRemovingPassword = true,
                resultDocument = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.removePassword(
                PdfRemovePasswordRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputFile = outputFile,
                    password = state.currentPassword,
                )
            ).onSuccess { result ->
                val unlockedDocument = result.toDocumentItem(
                    internalFileName = internalFileName,
                    displayName = normalizedOutputName,
                )
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isRemovingPassword = false,
                        resultDocument = unlockedDocument,
                    )
                }
                onSuccess(unlockedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isRemovingPassword = false,
                        errorMessage = throwable.message ?: "Failed to remove the password from the selected PDF.",
                    )
                }
            }
        }
    }

    private fun PdfRemovePasswordResult.toDocumentItem(
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
            isPasswordProtected = false,
        )
    }

    companion object {
        fun factory(
            context: Context,
            processingEngine: PdfProcessingEngine = PdfBoxProcessingEngine(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(RemovePasswordViewModel::class.java)) {
                    return RemovePasswordViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

