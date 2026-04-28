package com.thestudypath.pdfviewer.ui.tools.compress

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfCompressionProfile

data class CompressionSummary(
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val savingsPercent: Int,
    val appliedProfile: PdfCompressionProfile,
)

data class CompressPdfUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val selectedProfile: PdfCompressionProfile = PdfCompressionProfile.Balanced,
    val keepMetadata: Boolean = true,
    val outputFileName: String = normalizeCompressedFileName(""),
    val isCompressing: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val compressionSummary: CompressionSummary? = null,
    val errorMessage: String? = null,
)

