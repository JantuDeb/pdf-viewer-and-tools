package com.thestudypath.pdfviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class RecentFilesViewModel(
    private val recentFilesRepository: RecentFilesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecentFilesUiState>(RecentFilesUiState.Loading)
    val uiState: StateFlow<RecentFilesUiState> = _uiState.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()
    private var observeRecentFilesJob: Job? = null

    init {
        observeRecentFiles()
    }

    fun retry() {
        observeRecentFiles()
    }

    fun addOrUpdate(file: RecentFile) {
        launchAction(
            errorMessage = "Failed to update recent files."
        ) {
            recentFilesRepository.addOrUpdate(file)
        }
    }

    fun onMissingFileOpened(filePath: String) {
        launchAction(
            successMessage = "File no longer exists.",
            errorMessage = "Failed to remove missing file."
        ) {
            recentFilesRepository.remove(filePath)
        }
    }

    fun removeRecentFile(file: RecentFile) {
        launchAction(
            successMessage = "Removed",
            errorMessage = "Failed to remove file."
        ) {
            recentFilesRepository.remove(file.filePath)
            runCatching { File(file.filePath).delete() }
        }
    }

    fun clearAll(files: List<RecentFile>) {
        launchAction(
            errorMessage = "Failed to clear recent files."
        ) {
            files.forEach { runCatching { File(it.filePath).delete() } }
            recentFilesRepository.clearAll()
        }
    }

    private fun observeRecentFiles() {
        observeRecentFilesJob?.cancel()
        observeRecentFilesJob = viewModelScope.launch {
            recentFilesRepository.observeRecentFiles()
                .onStart {
                    _uiState.value = RecentFilesUiState.Loading
                }
                .catch { throwable ->
                    _uiState.value = RecentFilesUiState.Error(
                        throwable.message ?: "Failed to load recent files."
                    )
                }
                .collect { files ->
                    _uiState.value = RecentFilesUiState.Success(files)
                }
        }
    }

    private fun launchAction(
        successMessage: String? = null,
        errorMessage: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess {
                    successMessage?.let(_messages::tryEmit)
                }
                .onFailure { throwable ->
                    _messages.tryEmit(throwable.message ?: errorMessage)
                }
        }
    }

    companion object {
        fun factory(repository: RecentFilesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(RecentFilesViewModel::class.java)) {
                        return RecentFilesViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}


