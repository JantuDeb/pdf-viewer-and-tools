package com.thestudypath.pdfviewer.catalog

sealed interface DocumentCatalogUiState {
    data object Loading : DocumentCatalogUiState
    data class Success(val documents: List<DocumentItem>) : DocumentCatalogUiState
    data class Error(val message: String) : DocumentCatalogUiState
}

