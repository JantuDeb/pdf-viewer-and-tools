package com.thestudypath.pdfviewer.ui.tools.compress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfCompressionProfile
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.results.ResultActionButtons

@Composable
fun CompressPdfWorkspaceCard(
    uiState: CompressPdfUiState,
    onChooseDocument: () -> Unit,
    onProfileSelected: (PdfCompressionProfile) -> Unit,
    onKeepMetadataChanged: (Boolean) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onCompress: () -> Unit,
    onOpenResult: (DocumentItem) -> Unit,
    onShareResult: (DocumentItem) -> Unit,
    onSaveCopyResult: (DocumentItem) -> Unit,
) {
    val canCompress = uiState.selectedDocument != null &&
        uiState.selectedDocumentPageCount != null &&
        !uiState.isInspectingDocument &&
        !uiState.isCompressing

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
                        text = "Compress PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose one PDF and create a smaller copy. Stronger profiles may reduce visual quality more aggressively.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Compress,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = onChooseDocument,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.selectedDocument == null) "Choose PDF" else "Choose Another PDF")
            }

            uiState.selectedDocument?.let { document ->
                DocumentSummaryCard(document = document)
                AssistChip(
                    onClick = onChooseDocument,
                    label = {
                        Text(
                            when {
                                uiState.isInspectingDocument -> "Inspecting PDF…"
                                uiState.selectedDocumentPageCount != null -> "${uiState.selectedDocumentPageCount} pages"
                                else -> "Inspection unavailable"
                            }
                        )
                    },
                )
            }

            Text(
                text = "Compression profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileButton(
                    title = "Light",
                    badge = "Best quality",
                    selected = uiState.selectedProfile == PdfCompressionProfile.Light,
                    onClick = { onProfileSelected(PdfCompressionProfile.Light) },
                    modifier = Modifier.weight(1f),
                )
                ProfileButton(
                    title = "Balanced",
                    badge = "Recommended",
                    selected = uiState.selectedProfile == PdfCompressionProfile.Balanced,
                    onClick = { onProfileSelected(PdfCompressionProfile.Balanced) },
                    modifier = Modifier.weight(1f),
                )
                ProfileButton(
                    title = "Strong",
                    badge = "Max reduction",
                    selected = uiState.selectedProfile == PdfCompressionProfile.Strong,
                    onClick = { onProfileSelected(PdfCompressionProfile.Strong) },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = profileGuidance(uiState.selectedProfile),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileButton(
                    title = "Keep metadata",
                    badge = "Document info",
                    selected = uiState.keepMetadata,
                    onClick = { onKeepMetadataChanged(true) },
                    modifier = Modifier.weight(1f),
                )
                ProfileButton(
                    title = "Strip metadata",
                    badge = "Privacy first",
                    selected = !uiState.keepMetadata,
                    onClick = { onKeepMetadataChanged(false) },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = uiState.outputFileName,
                onValueChange = onOutputFileNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Output file name") },
                supportingText = { Text(".pdf is added automatically if needed") },
                singleLine = true,
            )

            Button(
                onClick = onCompress,
                enabled = canCompress,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isCompressing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Compressing PDF…")
                } else {
                    Text("Compress PDF")
                }
            }

            if (uiState.resultDocument != null && uiState.compressionSummary != null) {
                val resultDocument = uiState.resultDocument
                val summary = uiState.compressionSummary
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Compressed PDF ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "${profileLabel(summary.appliedProfile)} • ${formatFileSize(summary.originalSizeBytes)} -> ${formatFileSize(summary.compressedSizeBytes)} (${summary.savingsPercent}% saved)"
                            )
                        },
                    )
                    DocumentSummaryCard(document = resultDocument)
                    ResultActionButtons(
                        onOpen = { onOpenResult(resultDocument) },
                        onShare = { onShareResult(resultDocument) },
                        onSaveCopy = { onSaveCopyResult(resultDocument) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileButton(
    title: String,
    badge: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title)
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title)
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

