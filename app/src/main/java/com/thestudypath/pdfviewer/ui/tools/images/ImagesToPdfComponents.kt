package com.thestudypath.pdfviewer.ui.tools.images

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import java.util.Locale

@Composable
fun ImagesToPdfWorkspaceCard(
    uiState: ImagesToPdfUiState,
    onPickImages: () -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onMoveImageUp: (String) -> Unit,
    onMoveImageDown: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onCreatePdf: () -> Unit,
    onOpenResult: (DocumentItem) -> Unit,
    onShareResult: (DocumentItem) -> Unit,
) {
    val canCreatePdf = uiState.selectedImages.isNotEmpty() && !uiState.isPreparingImages && !uiState.isCreatingPdf

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Images to PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Pick one or more images, order them, and create a PDF with one page per image.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = onPickImages,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.selectedImages.isEmpty()) "Choose Images" else "Replace Image Selection")
            }

            if (uiState.selectedImages.isEmpty()) {
                Text(
                    text = "Pick at least 1 image to create a PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AssistChip(
                    onClick = onPickImages,
                    label = { Text("${uiState.selectedImages.size} images selected") },
                )

                OutlinedTextField(
                    value = uiState.outputFileName,
                    onValueChange = onOutputFileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Output file name") },
                    supportingText = { Text(".pdf is added automatically if needed") },
                    singleLine = true,
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false,
                ) {
                    itemsIndexed(uiState.selectedImages, key = { _, image -> image.id }) { index, image ->
                        ImagesToPdfSelectionItem(
                            index = index,
                            image = image,
                            canMoveUp = index > 0,
                            canMoveDown = index < uiState.selectedImages.lastIndex,
                            onMoveUp = { onMoveImageUp(image.id) },
                            onMoveDown = { onMoveImageDown(image.id) },
                            onRemove = { onRemoveImage(image.id) },
                        )
                    }
                }
            }

            Button(
                onClick = onCreatePdf,
                enabled = canCreatePdf,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    uiState.isPreparingImages -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Preparing images…")
                    }
                    uiState.isCreatingPdf -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text("Creating PDF…")
                    }
                    else -> Text("Create PDF from Images")
                }
            }

            uiState.resultDocument?.let { document ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PDF ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DocumentSummaryCard(document = document)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { onOpenResult(document) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Text(
                                text = "Open",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Button(
                            onClick = { onShareResult(document) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text(
                                text = "Share",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagesToPdfSelectionItem(
    index: Int,
    image: ImageInputItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${index + 1}. ${image.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatSize(image.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
        }
    }
}

private fun formatSize(sizeBytes: Long): String = when {
    sizeBytes < 1024 -> "$sizeBytes B"
    sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
    else -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes / (1024f * 1024f))
}


