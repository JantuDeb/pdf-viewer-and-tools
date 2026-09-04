package com.thestudypath.pdfviewer.ui.tools.password

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfAddPasswordRequest
import com.thestudypath.pdfviewer.processing.PdfAddPasswordResult
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class AddPasswordViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(AddPasswordUiState())
    val uiState: StateFlow<AddPasswordUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to protect.") }
            return
        }

        isOutputNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                showDocumentPicker = false,
                outputFileName = buildDefaultProtectedPdfFileName(selectedDocument),
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun updateUserPassword(value: String) {
        _uiState.update {
            it.copy(
                userPassword = value,
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun updateOwnerPassword(value: String) {
        _uiState.update {
            it.copy(
                ownerPassword = value,
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

    fun protectSelectedDocument(onSuccess: (DocumentItem) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to protect.") }
            return
        }

        runCatching {
            validateProtectionPasswords(
                userPassword = state.userPassword,
                confirmPassword = state.confirmPassword,
            )
        }.onFailure { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Enter a valid password.") }
            return
        }

        val normalizedOutputName = normalizeProtectedPdfFileName(
            rawValue = state.outputFileName,
            fallbackValue = buildDefaultProtectedPdfFileName(selectedDocument),
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalProtectedPdfFileName(normalizedOutputName, timestamp)
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isApplyingPassword = true,
                resultDocument = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.addPassword(
                PdfAddPasswordRequest(
                    inputFile = File(selectedDocument.filePath),
                    outputFile = outputFile,
                    userPassword = state.userPassword,
                    ownerPassword = state.ownerPassword.takeIf { it.isNotBlank() },
                )
            ).onSuccess { result ->
                val protectedDocument = result.toDocumentItem(
                    internalFileName = internalFileName,
                    displayName = normalizedOutputName,
                )
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isApplyingPassword = false,
                        resultDocument = protectedDocument,
                    )
                }
                onSuccess(protectedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isApplyingPassword = false,
                        errorMessage = throwable.message ?: "Failed to protect the selected PDF.",
                    )
                }
            }
        }
    }

    private fun PdfAddPasswordResult.toDocumentItem(
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
            isPasswordProtected = true,
        )
    }

    companion object {
        fun factory(
            context: Context,
            processingEngine: PdfProcessingEngine = PdfBoxProcessingEngine(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AddPasswordViewModel::class.java)) {
                    return AddPasswordViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

