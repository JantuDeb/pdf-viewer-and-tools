package com.thestudypath.pdfviewer.ui.tools.split

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class SplitPdfUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val splitMode: SplitMode = SplitMode.EveryNPages,
    val pagesPerSplitInput: String = "1",
    val customRangesInput: String = "",
    val outputBaseName: String = normalizeSplitBaseName(""),
    val isSplitting: Boolean = false,
    val resultDocuments: List<DocumentItem> = emptyList(),
    val errorMessage: String? = null,
)

