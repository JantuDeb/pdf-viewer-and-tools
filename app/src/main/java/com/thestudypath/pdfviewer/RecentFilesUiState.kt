package com.thestudypath.pdfviewer

sealed interface RecentFilesUiState {
    data object Loading : RecentFilesUiState
    data class Success(val files: List<RecentFile>) : RecentFilesUiState
    data class Error(val message: String) : RecentFilesUiState
}

