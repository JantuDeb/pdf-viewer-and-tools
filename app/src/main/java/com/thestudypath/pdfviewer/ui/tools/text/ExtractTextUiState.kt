package com.thestudypath.pdfviewer.ui.tools.text

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfPageRange
import java.io.File

data class ExtractedTextResult(
    val outputFile: File,
    val displayName: String,
    val pageRange: PdfPageRange,
    val text: String,
    val characterCount: Int,
)

data class ExtractTextUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val extractionScope: TextExtractionScope = TextExtractionScope.AllPages,
    val startPageInput: String = "1",
    val endPageInput: String = "1",
    val outputFileName: String = normalizeExtractedTextFileName(""),
    val isExtracting: Boolean = false,
    val extractedTextResult: ExtractedTextResult? = null,
    val errorMessage: String? = null,
)

