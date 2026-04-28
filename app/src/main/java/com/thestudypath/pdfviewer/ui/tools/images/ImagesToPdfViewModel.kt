package com.thestudypath.pdfviewer.ui.tools.images

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.DocumentSourceType
import com.thestudypath.pdfviewer.processing.ImageToPdfRequest
import com.thestudypath.pdfviewer.processing.ImageToPdfResult
import com.thestudypath.pdfviewer.processing.PdfBoxProcessingEngine
import com.thestudypath.pdfviewer.processing.PdfProcessingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ImagesToPdfViewModel(
    context: Context,
    private val processingEngine: PdfProcessingEngine,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(ImagesToPdfUiState())
    val uiState: StateFlow<ImagesToPdfUiState> = _uiState.asStateFlow()

    private var isOutputNameEdited = false

    fun importPickedImages(uris: List<Uri>) {
        if (uris.isEmpty()) return

        _uiState.update {
            it.copy(
                isPreparingImages = true,
                resultDocument = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            runCatching {
                val imageDirectory = File(appContext.cacheDir, "tool-images").apply { mkdirs() }
                _uiState.value.selectedImages.forEach { existing ->
                    runCatching { File(existing.workingFilePath).delete() }
                }
                uris.mapIndexed { index, uri -> copyImageToWorkingFile(uri, index, imageDirectory) }
            }.onSuccess { images ->
                isOutputNameEdited = false
                _uiState.update {
                    it.copy(
                        selectedImages = images,
                        isPreparingImages = false,
                        outputFileName = buildDefaultImagesPdfName(images.map(ImageInputItem::displayName)),
                        resultDocument = null,
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isPreparingImages = false,
                        errorMessage = throwable.message ?: "Failed to import selected images.",
                    )
                }
            }
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

    fun removeImage(imageId: String) {
        val updatedImages = _uiState.value.selectedImages.filterNot { it.id == imageId }
        val removedImage = _uiState.value.selectedImages.firstOrNull { it.id == imageId }
        removedImage?.let { runCatching { File(it.workingFilePath).delete() } }
        updateSelectedImages(updatedImages)
    }

    fun moveImageUp(imageId: String) {
        val images = _uiState.value.selectedImages.toMutableList()
        val index = images.indexOfFirst { it.id == imageId }
        if (index <= 0) return
        val item = images.removeAt(index)
        images.add(index - 1, item)
        updateSelectedImages(images)
    }

    fun moveImageDown(imageId: String) {
        val images = _uiState.value.selectedImages.toMutableList()
        val index = images.indexOfFirst { it.id == imageId }
        if (index == -1 || index >= images.lastIndex) return
        val item = images.removeAt(index)
        images.add(index + 1, item)
        updateSelectedImages(images)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun createPdf(onPdfCreated: (DocumentItem) -> Unit) {
        val selectedImages = _uiState.value.selectedImages
        if (selectedImages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Choose at least 1 image first.") }
            return
        }

        val normalizedOutputName = normalizeImagesPdfName(
            rawValue = _uiState.value.outputFileName,
            fallbackValue = buildDefaultImagesPdfName(selectedImages.map(ImageInputItem::displayName)),
        )
        val timestamp = System.currentTimeMillis()
        val internalFileName = buildInternalImagesPdfFileName(normalizedOutputName, timestamp)
        val outputFile = File(appContext.filesDir, internalFileName)

        _uiState.update {
            it.copy(
                outputFileName = normalizedOutputName,
                isCreatingPdf = true,
                resultDocument = null,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            processingEngine.createPdfFromImages(
                ImageToPdfRequest(
                    imageFiles = selectedImages.map { File(it.workingFilePath) },
                    outputFile = outputFile,
                )
            ).onSuccess { result ->
                val document = result.toDocumentItem(
                    internalFileName = internalFileName,
                    displayName = normalizedOutputName,
                )
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isCreatingPdf = false,
                        resultDocument = document,
                    )
                }
                onPdfCreated(document)
            }.onFailure { throwable ->
                outputFile.delete()
                _uiState.update {
                    it.copy(
                        outputFileName = normalizedOutputName,
                        isCreatingPdf = false,
                        errorMessage = throwable.message ?: "Failed to create a PDF from the selected images.",
                    )
                }
            }
        }
    }

    private fun updateSelectedImages(images: List<ImageInputItem>) {
        if (images.isEmpty()) {
            isOutputNameEdited = false
        }
        _uiState.update { currentState ->
            currentState.copy(
                selectedImages = images,
                outputFileName = if (isOutputNameEdited && currentState.outputFileName.isNotBlank()) {
                    currentState.outputFileName
                } else {
                    buildDefaultImagesPdfName(images.map(ImageInputItem::displayName))
                },
                resultDocument = null,
                errorMessage = null,
            )
        }
    }

    private fun copyImageToWorkingFile(
        uri: Uri,
        index: Int,
        imageDirectory: File,
    ): ImageInputItem {
        val displayName = queryDisplayName(uri) ?: "image-${index + 1}.jpg"
        val workingFile = File(
            imageDirectory,
            "img_${System.currentTimeMillis()}_${index}_${displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")}",
        )
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            workingFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open $displayName.")

        return ImageInputItem(
            id = uri.toString(),
            displayName = displayName,
            workingFilePath = workingFile.absolutePath,
            sizeBytes = workingFile.length(),
        )
    }

    private fun queryDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                .takeIf { it >= 0 }
                ?.let(cursor::getString)
        }
    }

    private fun ImageToPdfResult.toDocumentItem(
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
                if (modelClass.isAssignableFrom(ImagesToPdfViewModel::class.java)) {
                    return ImagesToPdfViewModel(context, processingEngine) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}

