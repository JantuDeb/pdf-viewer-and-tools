package com.thestudypath.pdfviewer.ui.sign

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import com.thestudypath.pdfviewer.processing.VisibleSignaturePlacement
import com.thestudypath.pdfviewer.processing.VisibleSignatureStampRequest
import com.thestudypath.pdfviewer.processing.VisibleSignatureStampResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class SignViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(SignUiState())
    val uiState: StateFlow<SignUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(errorMessage = "Choose a PDF to sign.") }
            return
        }

        isOutputNameEdited = false
        _uiState.update {
            it.copy(
                selectedDocument = selectedDocument,
                selectedDocumentPageCount = selectedDocument.pageCount.takeIf { count -> count > 0 },
                showDocumentPicker = false,
                isInspectingDocument = true,
                pageNumberInput = "1",
                outputFileName = buildDefaultSignedPdfFileName(selectedDocument),
                resultDocument = null,
                errorMessage = null,
            )
        }

        inspectSelectedDocument(selectedDocument)
    }

    fun updatePageNumberInput(value: String) {
        _uiState.update {
            it.copy(
                pageNumberInput = value,
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun updatePlacement(placement: VisibleSignaturePlacement) {
        _uiState.update {
            it.copy(
                placement = placement,
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun updateSignatureWidthPercent(value: Float) {
        _uiState.update {
            it.copy(
                signatureWidthPercent = value.coerceIn(10f, 40f),
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

    fun importSignature(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val signatureDirectory = ensureSignatureDirectory()
                val displayName = queryDisplayName(uri) ?: "signature.png"
                val sanitizedName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val signatureFile = File(
                    signatureDirectory,
                    "imported_${System.currentTimeMillis()}_$sanitizedName",
                )
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    signatureFile.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Could not open $displayName.")

                SignatureInput(
                    filePath = signatureFile.absolutePath,
                    displayName = displayName,
                    sourceType = SignatureSourceType.Imported,
                )
            }.onSuccess(::setSelectedSignature)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Failed to import the selected signature image.")
                    }
                }
        }
    }

    fun saveDrawnSignature(
        pngBytes: ByteArray,
        suggestedDisplayName: String = "drawn-signature.png",
    ) {
        if (pngBytes.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Draw a signature before using it.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                val signatureDirectory = ensureSignatureDirectory()
                val signatureFile = File(
                    signatureDirectory,
                    "drawn_${System.currentTimeMillis()}_${suggestedDisplayName.replace(Regex("[^A-Za-z0-9._-]"), "_")}",
                )
                signatureFile.writeBytes(pngBytes)
                SignatureInput(
                    filePath = signatureFile.absolutePath,
                    displayName = suggestedDisplayName,
                    sourceType = SignatureSourceType.Drawn,
                )
            }.onSuccess(::setSelectedSignature)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = throwable.message ?: "Failed to save the drawn signature.")
                    }
                }
        }
    }

    fun clearSignature() {
        val existingSignature = _uiState.value.selectedSignature
        existingSignature?.let { runCatching { File(it.filePath).delete() } }
        _uiState.update {
            it.copy(
                selectedSignature = null,
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun signSelectedDocument(onSuccess: (DocumentItem) -> Unit) {
        val state = _uiState.value
        val selectedDocument = state.selectedDocument
        if (selectedDocument == null) {
            _uiState.update { it.copy(errorMessage = "Choose a PDF to sign.") }
            return
        }
        val selectedSignature = state.selectedSignature
        if (selectedSignature == null) {
            _uiState.update { it.copy(errorMessage = "Draw or import a signature first.") }
            return
        }
        val totalPages = state.selectedDocumentPageCount
        if (totalPages == null || totalPages <= 0) {
            _uiState.update { it.copy(errorMessage = "We couldn't inspect this PDF. Choose it again or import a different file.") }
            return
        }

        val pageNumber = runCatching {
            resolveSigningPageNumber(
                rawValue = state.pageNumberInput,
                totalPages = totalPages,
            )
        }.getOrElse { throwable ->
            _uiState.update { it.copy(errorMessage = throwable.message ?: "Enter a valid page number.") }
            return
        }

        val normalizedOutputName = normalizeSignedPdfFileName(
            rawValue = state.outputFileName,
            fallbackValue = buildDefaultSignedPdfFileName(selectedDocument),
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalSignedPdfFileName(normalizedOutputName, timestamp)
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                pageNumberInput = normalizePageNumberInput(state.pageNumberInput, totalPages),
                isSigning = true,
                resultDocument = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.stampVisibleSignature(
                VisibleSignatureStampRequest(
                    inputFile = File(selectedDocument.filePath),
                    signatureImageFile = File(selectedSignature.filePath),
                    outputFile = outputFile,
                    pageNumber = pageNumber,
                    placement = state.placement,
                    widthPercent = state.signatureWidthPercent.toInt(),
                )
            ).onSuccess { result ->
                val signedDocument = result.toDocumentItem(
                    internalFileName = internalFileName,
                    displayName = normalizedOutputName,
                )
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isSigning = false,
                        resultDocument = signedDocument,
                    )
                }
                onSuccess(signedDocument)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isSigning = false,
                        errorMessage = throwable.message ?: "Failed to create the signed PDF.",
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
                            pageNumberInput = normalizePageNumberInput(it.pageNumberInput, inspection.pageCount),
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

    private fun setSelectedSignature(signature: SignatureInput) {
        val previousSignature = _uiState.value.selectedSignature
        if (previousSignature?.filePath != signature.filePath) {
            previousSignature?.let { runCatching { File(it.filePath).delete() } }
        }
        _uiState.update {
            it.copy(
                selectedSignature = signature,
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    private fun ensureSignatureDirectory(): File = File(appContext.cacheDir, "signatures").apply { mkdirs() }

    private fun queryDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                .takeIf { it >= 0 }
                ?.let(cursor::getString)
        }
    }

    private fun VisibleSignatureStampResult.toDocumentItem(
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
                if (modelClass.isAssignableFrom(SignViewModel::class.java)) {
                    return SignViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

