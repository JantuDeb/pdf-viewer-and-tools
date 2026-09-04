package com.thestudypath.pdfviewer.ui.tools.pages

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class ExtractPagesUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val pageRangesInput: String = "1",
    val outputFileName: String = normalizePageToolPdfFileName("", "extracted-pages"),
    val isProcessing: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val extractedPageCount: Int? = null,
    val errorMessage: String? = null,
)

