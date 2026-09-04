package com.thestudypath.pdfviewer.ui.tools.embeddedimages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.processing.EmbeddedImageExportOutput
import com.thestudypath.pdfviewer.processing.ExtractEmbeddedImagesResult
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.tools.compress.formatFileSize
import java.io.File

@Composable
internal fun ExtractEmbeddedImagesWorkspaceCard(
    uiState: ExtractEmbeddedImagesUiState,
    onChooseDocument: () -> Unit,
    onPageRangesChange: (String) -> Unit,
    onOutputFolderNameChange: (String) -> Unit,
    onExtractImages: () -> Unit,
    onOpenImage: (File) -> Unit,
    onShareImage: (File) -> Unit,
    onShareAllImages: (ExtractEmbeddedImagesResult) -> Unit,
) {
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Extract Embedded Images",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose one PDF, scan selected pages for embedded images, and save each image into one folder.",
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
                onClick = onChooseDocument,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.selectedDocument == null) "Choose PDF" else "Change PDF")
            }

            uiState.selectedDocument?.let { document ->
                DocumentSummaryCard(document = document)
            }

            if (uiState.isInspectingDocument) {
                AssistChip(
                    onClick = {},
                    label = { Text("Inspecting document pages…") },
                )
            } else {
                uiState.selectedDocumentPageCount?.let { pageCount ->
                    AssistChip(
                        onClick = {},
                        label = { Text("$pageCount pages available") },
                    )
                }
            }

            OutlinedTextField(
                value = uiState.pageRangesInput,
                onValueChange = onPageRangesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pages to scan") },
                supportingText = { Text("Leave blank for all pages. Examples: 1-3, 5, 8-10") },
                singleLine = true,
            )

            OutlinedTextField(
                value = uiState.outputFolderName,
                onValueChange = onOutputFolderNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Output folder name") },
                supportingText = { Text("Each unique embedded image is exported once into one app-private folder") },
                singleLine = true,
            )

            Button(
                onClick = onExtractImages,
                enabled = uiState.selectedDocument != null && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Extracting images…")
                } else {
                    Text("Extract images")
                }
            }

            uiState.result?.let { result ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Embedded images ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${result.exportedImages.size} image${if (result.exportedImages.size == 1) "" else "s"} exported"
                            )
                        },
                    )
                    Text(
                        text = "Saved in the ${uiState.outputFolderName} folder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        result.exportedImages.firstOrNull()?.let { firstImage ->
                            Button(
                                onClick = { onOpenImage(firstImage.outputFile) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Text("Open first")
                            }
                        }
                        Button(
                            onClick = { onShareAllImages(result) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text("Share all")
                        }
                    }
                    result.exportedImages.take(6).forEach { image ->
                        EmbeddedImageRow(
                            image = image,
                            onOpenImage = { onOpenImage(image.outputFile) },
                            onShareImage = { onShareImage(image.outputFile) },
                        )
                    }
                    if (result.exportedImages.size > 6) {
                        Text(
                            text = "+ ${result.exportedImages.size - 6} more image(s) saved in the same folder.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmbeddedImageRow(
    image: EmbeddedImageExportOutput,
    onOpenImage: () -> Unit,
    onShareImage: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = image.outputFile.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${image.width} × ${image.height} • ${formatFileSize(image.fileSizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                image.pageNumbers.forEach { pageNumber ->
                    AssistChip(
                        onClick = {},
                        label = { Text("Page $pageNumber") },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onOpenImage,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Open")
                }
                Button(
                    onClick = onShareImage,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Share")
                }
            }
        }
    }
}

