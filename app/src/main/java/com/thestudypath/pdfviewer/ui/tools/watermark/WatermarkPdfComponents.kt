package com.thestudypath.pdfviewer.ui.tools.watermark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfWatermarkPlacement
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.results.ResultActionButtons

@Composable
internal fun WatermarkPdfWorkspaceCard(
    uiState: WatermarkPdfUiState,
    onChooseDocument: () -> Unit,
    onWatermarkTextChange: (String) -> Unit,
    onPlacementChange: (PdfWatermarkPlacement) -> Unit,
    onSizeOptionChange: (WatermarkSizeOption) -> Unit,
    onPageRangesChange: (String) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onApplyWatermark: () -> Unit,
    onOpenResult: (DocumentItem) -> Unit,
    onShareResult: (DocumentItem) -> Unit,
    onSaveCopyResult: (DocumentItem) -> Unit,
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
                        text = "Watermark PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose one PDF, enter watermark text, pick where it should appear, and save a new copy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
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
                value = uiState.watermarkText,
                onValueChange = onWatermarkTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Watermark text") },
                supportingText = {
                    Text("Built-in PDF font support is best with Latin letters, digits, and common punctuation.")
                },
                maxLines = 2,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Placement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                watermarkPlacementOptions.chunked(3).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { option ->
                            FilterChip(
                                selected = uiState.placement == option.placement,
                                onClick = { onPlacementChange(option.placement) },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WatermarkSizeOption.entries.forEach { option ->
                        FilterChip(
                            selected = uiState.sizeOption == option,
                            onClick = { onSizeOptionChange(option) },
                            label = { Text(option.label) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.pageRangesInput,
                onValueChange = onPageRangesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pages to watermark") },
                supportingText = { Text("Leave blank for all pages. Examples: 1-3, 5, 8-10") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.outputFileName,
                onValueChange = onOutputFileNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Output file name") },
                supportingText = { Text(".pdf is added automatically if needed") },
                singleLine = true,
            )

            Button(
                onClick = onApplyWatermark,
                enabled = uiState.selectedDocument != null && uiState.watermarkText.isNotBlank() && !uiState.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Applying watermark…")
                } else {
                    Text("Save watermarked PDF")
                }
            }

            uiState.resultDocument?.let { resultDocument ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Watermarked PDF ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DocumentSummaryCard(document = resultDocument)
                    uiState.watermarkedPageCount?.let { pageCount ->
                        AssistChip(
                            onClick = {},
                            label = { Text("Applied to $pageCount page${if (pageCount == 1) "" else "s"}") },
                        )
                    }
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

