package com.thestudypath.pdfviewer.ui.tools.embeddedimages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.ExtractEmbeddedImagesResult

internal data class ExtractEmbeddedImagesUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val pageRangesInput: String = "",
    val outputFolderName: String = normalizeEmbeddedImagesFolderName(""),
    val isProcessing: Boolean = false,
    val result: ExtractEmbeddedImagesResult? = null,
    val errorMessage: String? = null,
)

