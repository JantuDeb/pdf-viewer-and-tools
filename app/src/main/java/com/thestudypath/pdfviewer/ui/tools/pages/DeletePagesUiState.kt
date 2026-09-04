package com.thestudypath.pdfviewer.ui.tools.pages

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class DeletePagesUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val pageRangesInput: String = "1",
    val outputFileName: String = normalizePageToolPdfFileName("", "deleted-pages"),
    val isProcessing: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val removedPageCount: Int? = null,
    val errorMessage: String? = null,
)

