package com.thestudypath.pdfviewer.ui.tools.password

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class AddPasswordUiState(
    val selectedDocument: DocumentItem? = null,
    val showDocumentPicker: Boolean = false,
    val userPassword: String = "",
    val confirmPassword: String = "",
    val ownerPassword: String = "",
    val outputFileName: String = normalizeProtectedPdfFileName(""),
    val isApplyingPassword: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val errorMessage: String? = null,
)

