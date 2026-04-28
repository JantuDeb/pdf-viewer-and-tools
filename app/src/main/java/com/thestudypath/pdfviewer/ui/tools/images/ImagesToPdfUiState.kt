package com.thestudypath.pdfviewer.ui.tools.images

import com.thestudypath.pdfviewer.catalog.DocumentItem

data class ImagesToPdfUiState(
    val selectedImages: List<ImageInputItem> = emptyList(),
    val isPreparingImages: Boolean = false,
    val outputFileName: String = normalizeImagesPdfName(""),
    val isCreatingPdf: Boolean = false,
    val resultDocument: DocumentItem? = null,
    val errorMessage: String? = null,
)

