package com.thestudypath.pdfviewer.ui.tools.pages

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class RotatePagesUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val pageRangesInput: String = "1",
    val rotationOption: PageRotationOption = PageRotationOption.Clockwise90,
    val outputFileName: String = normalizePageToolPdfFileName("", "rotated-document"),
    val isProcessing: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val errorMessage: String? = null,
)

