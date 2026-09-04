package com.thestudypath.pdfviewer.ui.sign

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.VisibleSignaturePlacement

data class SignUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val selectedSignature: SignatureInput? = null,
    val pageNumberInput: String = "1",
    val placement: VisibleSignaturePlacement = VisibleSignaturePlacement.BottomRight,
    val signatureWidthPercent: Float = 24f,
    val outputFileName: String = normalizeSignedPdfFileName(""),
    val isSigning: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val errorMessage: String? = null,
)

