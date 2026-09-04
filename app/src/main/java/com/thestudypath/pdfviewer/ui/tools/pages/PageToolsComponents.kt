package com.thestudypath.pdfviewer.ui.tools.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.results.ResultActionButtons

@Composable
fun RotatePagesWorkspaceCard(
    uiState: RotatePagesUiState,
    onChooseDocument: () -> Unit,
    onPageRangesChange: (String) -> Unit,
    onRotationOptionChange: (PageRotationOption) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onRotatePages: () -> Unit,
    onOpenResult: (DocumentItem) -> Unit,
    onShareResult: (DocumentItem) -> Unit,
    onSaveCopyResult: (DocumentItem) -> Unit,
) {
    PageToolCard(
        title = "Rotate Pages",
        description = "Choose one PDF, enter the page ranges to rotate, and save a new copy.",
        icon = Icons.Default.PictureAsPdf,
        chooseLabel = if (uiState.selectedDocument == null) "Choose PDF" else "Change PDF",
        selectedDocument = uiState.selectedDocument,
        selectedDocumentPageCount = uiState.selectedDocumentPageCount,
        isInspectingDocument = uiState.isInspectingDocument,
        onChooseDocument = onChooseDocument,
        content = {
            Text(
                text = "Rotation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PageRotationOption.entries.forEach { option ->
                    FilterChip(
                        selected = uiState.rotationOption == option,
                        onClick = { onRotationOptionChange(option) },
                        label = { Text(option.label) },
                    )
                }
            }
            OutlinedTextField(
                value = uiState.pageRangesInput,
                onValueChange = onPageRangesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pages to rotate") },
                supportingText = { Text("Examples: 1-3, 5, 8-10") },
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
        },
        actionLabel = "Rotate selected pages",
        isProcessing = uiState.isProcessing,
        processingLabel = "Rotating pages…",
        isActionEnabled = uiState.selectedDocument != null && !uiState.isProcessing,
        onAction = onRotatePages,
        resultTitle = "Rotated PDF ready",
        resultDocument = uiState.resultDocument,
        onOpenResult = onOpenResult,
        onShareResult = onShareResult,
        onSaveCopyResult = onSaveCopyResult,
    )
}

@Composable
fun ExtractPagesWorkspaceCard(
    uiState: ExtractPagesUiState,
    onChooseDocument: () -> Unit,
    onPageRangesChange: (String) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onExtractPages: () -> Unit,
    onOpenResult: (DocumentItem) -> Unit,
    onShareResult: (DocumentItem) -> Unit,
    onSaveCopyResult: (DocumentItem) -> Unit,
) {
    PageToolCard(
        title = "Extract Pages",
        description = "Choose one PDF, enter the page ranges to keep, and save them as a new PDF.",
        icon = Icons.Default.ContentCut,
        chooseLabel = if (uiState.selectedDocument == null) "Choose PDF" else "Change PDF",
        selectedDocument = uiState.selectedDocument,
        selectedDocumentPageCount = uiState.selectedDocumentPageCount,
        isInspectingDocument = uiState.isInspectingDocument,
        onChooseDocument = onChooseDocument,
        content = {
            OutlinedTextField(
                value = uiState.pageRangesInput,
                onValueChange = onPageRangesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pages to extract") },
                supportingText = { Text("Examples: 1-3, 5, 8-10") },
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
            uiState.extractedPageCount?.let { extractedPageCount ->
                AssistChip(
                    onClick = {},
                    label = { Text("Last result extracted $extractedPageCount page${if (extractedPageCount == 1) "" else "s"}") },
                )
            }
        },
        actionLabel = "Extract selected pages",
        isProcessing = uiState.isProcessing,
        processingLabel = "Extracting pages…",
        isActionEnabled = uiState.selectedDocument != null && !uiState.isProcessing,
        onAction = onExtractPages,
        resultTitle = "Extracted PDF ready",
        resultDocument = uiState.resultDocument,
        onOpenResult = onOpenResult,
        onShareResult = onShareResult,
        onSaveCopyResult = onSaveCopyResult,
    )
}

@Composable
fun DeletePagesWorkspaceCard(
    uiState: DeletePagesUiState,
    onChooseDocument: () -> Unit,
    onPageRangesChange: (String) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onDeletePages: () -> Unit,
    onOpenResult: (DocumentItem) -> Unit,
    onShareResult: (DocumentItem) -> Unit,
    onSaveCopyResult: (DocumentItem) -> Unit,
) {
    PageToolCard(
        title = "Delete Pages",
        description = "Choose one PDF, enter the page ranges to remove, and save a cleaned-up copy.",
        icon = Icons.Default.Delete,
        chooseLabel = if (uiState.selectedDocument == null) "Choose PDF" else "Change PDF",
        selectedDocument = uiState.selectedDocument,
        selectedDocumentPageCount = uiState.selectedDocumentPageCount,
        isInspectingDocument = uiState.isInspectingDocument,
        onChooseDocument = onChooseDocument,
        content = {
            OutlinedTextField(
                value = uiState.pageRangesInput,
                onValueChange = onPageRangesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pages to delete") },
                supportingText = { Text("Examples: 2, 5-7") },
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
            uiState.removedPageCount?.let { removedPageCount ->
                AssistChip(
                    onClick = {},
                    label = { Text("Last result removed $removedPageCount page${if (removedPageCount == 1) "" else "s"}") },
                )
            }
        },
        actionLabel = "Delete selected pages",
        isProcessing = uiState.isProcessing,
        processingLabel = "Deleting pages…",
        isActionEnabled = uiState.selectedDocument != null && !uiState.isProcessing,
        onAction = onDeletePages,
        resultTitle = "Updated PDF ready",
        resultDocument = uiState.resultDocument,
        onOpenResult = onOpenResult,
        onShareResult = onShareResult,
        onSaveCopyResult = onSaveCopyResult,
    )
}

@Composable
private fun PageToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    chooseLabel: String,
    selectedDocument: DocumentItem?,
    selectedDocumentPageCount: Int?,
    isInspectingDocument: Boolean,
    onChooseDocument: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actionLabel: String,
    isProcessing: Boolean,
    processingLabel: String,
    isActionEnabled: Boolean,
    onAction: () -> Unit,
    resultTitle: String,
    resultDocument: DocumentItem?,
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
            content = {
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
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Button(
                    onClick = onChooseDocument,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(chooseLabel)
                }

                selectedDocument?.let { document ->
                    DocumentSummaryCard(document = document)
                    if (isInspectingDocument) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text("Inspecting document pages…")
                        }
                    } else {
                        selectedDocumentPageCount?.let { pageCount ->
                            AssistChip(
                                onClick = {},
                                label = { Text("$pageCount pages available") },
                            )
                        }
                    }
                }

                content()

                Button(
                    onClick = onAction,
                    enabled = isActionEnabled,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(processingLabel)
                    } else {
                        Text(actionLabel)
                    }
                }

                resultDocument?.let { document ->
                    Text(
                        text = resultTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DocumentSummaryCard(document = document)
                    ResultActionButtons(
                        onOpen = { onOpenResult(document) },
                        onShare = { onShareResult(document) },
                        onSaveCopy = { onSaveCopyResult(document) },
                    )
                }
            },
        )
    }
}

