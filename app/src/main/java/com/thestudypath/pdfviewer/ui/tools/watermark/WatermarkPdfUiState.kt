package com.thestudypath.pdfviewer.ui.tools.watermark

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfWatermarkPlacement

internal data class WatermarkPdfUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val watermarkText: String = "CONFIDENTIAL",
    val placement: PdfWatermarkPlacement = PdfWatermarkPlacement.DiagonalCenter,
    val sizeOption: WatermarkSizeOption = WatermarkSizeOption.Medium,
    val pageRangesInput: String = "",
    val outputFileName: String = normalizeWatermarkPdfFileName(""),
    val isProcessing: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val watermarkedPageCount: Int? = null,
    val errorMessage: String? = null,
)

