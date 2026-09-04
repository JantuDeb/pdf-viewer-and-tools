package com.thestudypath.pdfviewer.ui.tools.password

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class RemovePasswordUiState(
    val selectedDocument: DocumentItem? = null,
    val showDocumentPicker: Boolean = false,
    val currentPassword: String = "",
    val outputFileName: String = normalizeUnlockedPdfFileName(""),
    val isRemovingPassword: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val errorMessage: String? = null,
)

