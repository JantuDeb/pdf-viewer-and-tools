package com.thestudypath.pdfviewer.ui.tools.merge

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class MergePdfUiState(
    val selectedDocuments: List<DocumentItem> = emptyList(),
    val showDocumentPicker: Boolean = false,
    val outputFileName: String = buildDefaultMergeOutputName(emptyList()),
    val isMerging: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val errorMessage: String? = null,
)

