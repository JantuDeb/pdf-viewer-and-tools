package com.thestudypath.pdfviewer.ui.tools.crop

import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfCropPageRegionResult
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat

internal data class CropPageRegionUiState(
    val selectedDocument: DocumentItem? = null,
    val selectedDocumentPageCount: Int? = null,
    val showDocumentPicker: Boolean = false,
    val isInspectingDocument: Boolean = false,
    val pageRangesInput: String = "",
    val imageFormat: PdfImageExportFormat = PdfImageExportFormat.Png,
    val qualityOption: CropExportQualityOption = CropExportQualityOption.Standard,
    val preset: PageCropPreset = PageCropPreset.FullPage,
    val cropRegion: CropRegionSelection = PageCropPreset.FullPage.region,
    val outputFolderName: String = normalizeCropExportFolderName(""),
    val isProcessing: Boolean = false,
    val result: PdfCropPageRegionResult? = null,
    val errorMessage: String? = null,
)

