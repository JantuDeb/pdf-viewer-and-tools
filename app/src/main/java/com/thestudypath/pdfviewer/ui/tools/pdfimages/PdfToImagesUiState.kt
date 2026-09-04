package com.thestudypath.pdfviewer.ui.tools.pdfimages

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat
import com.thestudypath.pdfviewer.processing.PdfToImagesResult

internal data class PdfToImagesUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val pageRangesInput: String = "",
    val imageFormat: PdfImageExportFormat = PdfImageExportFormat.Png,
    val qualityOption: PdfImageExportQualityOption = PdfImageExportQualityOption.Standard,
    val outputFolderName: String = normalizePdfImagesFolderName(""),
    val isProcessing: Boolean = false,
    val result: PdfToImagesResult? = null,
    val errorMessage: String? = null,
)

