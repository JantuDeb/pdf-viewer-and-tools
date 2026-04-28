package com.thestudypath.pdfviewer.ui.files

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thestudypath.pdfviewer.catalog.DocumentCatalogUiState
import com.thestudypath.pdfviewer.catalog.DocumentCatalogViewModel
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.navigation.AppDestination
import com.thestudypath.pdfviewer.ui.documents.DocumentCollectionContent
import com.thestudypath.pdfviewer.ui.documents.DocumentViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesRoute(
    viewModel: DocumentCatalogViewModel,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allDocuments = (uiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var viewMode by rememberSaveable { mutableStateOf(DocumentViewMode.Grid) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DocumentItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppDestination.Files.label) },
                actions = {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear History") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            },
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportDocument,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Open PDF") },
            )
        },
    ) { innerPadding ->
        DocumentCollectionContent(
            uiState = uiState,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            viewMode = viewMode,
            onViewModeToggle = {
                viewMode = if (viewMode == DocumentViewMode.Grid) {
                    DocumentViewMode.List
                } else {
                    DocumentViewMode.Grid
                }
            },
            onRetry = viewModel::retry,
            onDocumentClick = onOpenDocument,
            onDocumentLongClick = { pendingDelete = it },
            emptyTitle = "No recent PDFs",
            emptyMessage = "Tap \"Open PDF\" to get started",
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Remove all recent files? Internal copies will also be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll(allDocuments)
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    pendingDelete?.let { document ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove PDF?") },
            text = { Text("Remove \"${document.displayName}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeDocument(document)
                        pendingDelete = null
                    }
                ) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

