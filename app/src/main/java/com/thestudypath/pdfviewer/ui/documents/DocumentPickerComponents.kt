package com.thestudypath.pdfviewer.ui.documents

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.thestudypath.pdfviewer.ThumbnailCache
import com.thestudypath.pdfviewer.catalog.DocumentCatalogUiState
import com.thestudypath.pdfviewer.catalog.DocumentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DocumentViewMode {
    Grid,
    List,
}

enum class DocumentSelectionMode {
    Single,
    Multiple,
}

@Composable
fun DocumentCollectionContent(
    uiState: DocumentCatalogUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewMode: DocumentViewMode,
    onViewModeToggle: (() -> Unit)?,
    onRetry: () -> Unit,
    onDocumentClick: (DocumentItem) -> Unit,
    onDocumentLongClick: ((DocumentItem) -> Unit)? = null,
    selectedDocumentIds: Set<String> = emptySet(),
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 88.dp,
) {
    val allDocuments = (uiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val filteredDocuments by remember(allDocuments, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allDocuments
            } else {
                allDocuments.filter { document ->
                    document.displayName.contains(searchQuery.trim(), ignoreCase = true)
                }
            }
        }
    }

    Column(modifier = modifier) {
        DocumentSearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onViewModeToggle = onViewModeToggle,
            viewMode = viewMode,
        )

        when (val state = uiState) {
            DocumentCatalogUiState.Loading -> DocumentLoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding)
            )

            is DocumentCatalogUiState.Error -> DocumentErrorState(
                message = state.message,
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding)
            )

            is DocumentCatalogUiState.Success -> {
                if (filteredDocuments.isEmpty()) {
                    DocumentEmptyState(
                        isSearch = searchQuery.isNotBlank(),
                        query = searchQuery,
                        emptyTitle = emptyTitle,
                        emptyMessage = emptyMessage,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding)
                    )
                } else {
                    val itemPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = bottomPadding,
                    )
                    if (viewMode == DocumentViewMode.Grid) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(160.dp),
                            contentPadding = itemPadding,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(filteredDocuments, key = { it.id }) { document ->
                                DocumentCard(
                                    document = document,
                                    selected = selectedDocumentIds.contains(document.id),
                                    onClick = { onDocumentClick(document) },
                                    onLongClick = onDocumentLongClick?.let { handler ->
                                        { handler(document) }
                                    },
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = itemPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(filteredDocuments.size, key = { filteredDocuments[it].id }) { index ->
                                val document = filteredDocuments[index]
                                DocumentListItem(
                                    document = document,
                                    selected = selectedDocumentIds.contains(document.id),
                                    onClick = { onDocumentClick(document) },
                                    onLongClick = onDocumentLongClick?.let { handler ->
                                        { handler(document) }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPickerSheet(
    title: String,
    uiState: DocumentCatalogUiState,
    selectionMode: DocumentSelectionMode,
    selectedDocumentIds: Set<String>,
    onDocumentToggle: (DocumentItem) -> Unit,
    onDismissRequest: () -> Unit,
    onImportClick: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
    helperText: String? = null,
) {
    var searchQuery by rememberSaveable(title) { mutableStateOf("") }
    var viewMode by rememberSaveable(title) { mutableStateOf(DocumentViewMode.List) }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = helperText ?: when (selectionMode) {
                    DocumentSelectionMode.Single -> "Pick one PDF from your document catalog."
                    DocumentSelectionMode.Multiple -> "Pick one or more PDFs from your document catalog."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            DocumentCollectionContent(
                uiState = uiState,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                viewMode = viewMode,
                onViewModeToggle = {
                    viewMode = if (viewMode == DocumentViewMode.Grid) DocumentViewMode.List else DocumentViewMode.Grid
                },
                onRetry = onDismissRequest,
                onDocumentClick = onDocumentToggle,
                selectedDocumentIds = selectedDocumentIds,
                emptyTitle = "No PDFs available",
                emptyMessage = "Import a PDF first to use this flow.",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                bottomPadding = 24.dp,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Import PDF")
                }
                Button(
                    onClick = onConfirm,
                    enabled = selectedDocumentIds.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(confirmLabel)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DocumentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onViewModeToggle: (() -> Unit)?,
    viewMode: DocumentViewMode,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search PDFs…") },
            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
        )
        if (onViewModeToggle != null) {
            Surface(
                shape = CircleShape,
                tonalElevation = 2.dp,
            ) {
                IconButton(onClick = onViewModeToggle) {
                    Icon(
                        imageVector = if (viewMode == DocumentViewMode.Grid) {
                            Icons.AutoMirrored.Filled.List
                        } else {
                            Icons.Default.GridView
                        },
                        contentDescription = if (viewMode == DocumentViewMode.Grid) {
                            "Switch to list view"
                        } else {
                            "Switch to grid view"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentLoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DocumentErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text("⚠️", style = MaterialTheme.typography.displayLarge)
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun DocumentEmptyState(
    isSearch: Boolean,
    query: String,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isSearch) "🔍" else "📄",
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isSearch) "No results for \"$query\"" else emptyTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isSearch) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun rememberPdfThumbnail(filePath: String): Bitmap? =
    produceState<Bitmap?>(null, filePath) {
        ThumbnailCache.get(filePath)?.let {
            value = it
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(filePath)
                if (!file.exists()) return@runCatching null
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        renderer.openPage(0).use { page ->
                            val scale = 2f
                            createBitmap(
                                (page.width * scale).toInt(),
                                (page.height * scale).toInt(),
                            ).also { bitmap ->
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                                )
                                ThumbnailCache.put(filePath, bitmap)
                            }
                        }
                    }
                }
            }.getOrNull()
        }
    }.value

@Composable
fun DocumentSummaryCard(
    document: DocumentItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DocumentThumbnailSurface(
                filePath = document.filePath,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                DocumentMetaText(document = document)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentCard(
    document: DocumentItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .documentClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(12.dp),
        border = selectedBorder(selected),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Box {
                DocumentThumbnailSurface(
                    filePath = document.filePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                )
                if (selected) {
                    SelectedBadge(modifier = Modifier.align(Alignment.TopEnd))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = document.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            DocumentMetaText(document)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentListItem(
    document: DocumentItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .documentClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(8.dp),
        border = selectedBorder(selected),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                DocumentThumbnailSurface(
                    filePath = document.filePath,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    shape = RoundedCornerShape(6.dp),
                )
                if (selected) {
                    SelectedBadge(modifier = Modifier.align(Alignment.TopEnd))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                DocumentMetaText(document)
            }
        }
    }
}

@Composable
private fun DocumentThumbnailSurface(
    filePath: String,
    modifier: Modifier,
    shape: RoundedCornerShape,
) {
    val thumbnail = rememberPdfThumbnail(filePath)
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = shape,
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = "PDF thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "PDF",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun DocumentMetaText(document: DocumentItem) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    Text(
        text = buildString {
            append(dateFormat.format(Date(document.lastOpenedAt)))
            if (document.sizeBytes > 0) append(" • ${formatFileSize(document.sizeBytes)}")
            if (document.pageCount > 0) append(" • ${document.pageCount}p")
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun selectedBorder(selected: Boolean): BorderStroke? =
    if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.documentClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier = if (onLongClick != null) {
    combinedClickable(onClick = onClick, onLongClick = onLongClick)
} else {
    clickable(onClick = onClick)
}

private fun formatFileSize(bytes: Long) = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}

