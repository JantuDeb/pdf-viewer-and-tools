package com.thestudypath.pdfviewer.ui.tools.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.results.ResultActionButtons

@Composable
fun ExtractTextWorkspaceCard(
    uiState: ExtractTextUiState,
    onChooseDocument: () -> Unit,
    onScopeChange: (TextExtractionScope) -> Unit,
    onStartPageChange: (String) -> Unit,
    onEndPageChange: (String) -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onExtractText: () -> Unit,
    onOpenTextFile: (ExtractedTextResult) -> Unit,
    onShareTextFile: (ExtractedTextResult) -> Unit,
    onSaveCopyTextFile: (ExtractedTextResult) -> Unit,
    onCopyText: (String) -> Unit,
) {
    val canExtract = uiState.selectedDocument != null &&
        uiState.selectedDocumentPageCount != null &&
        !uiState.isInspectingDocument &&
        !uiState.isExtracting

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
                        text = "Extract Text",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose a PDF and extract plain text from all pages or a page range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Description,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExtractionScopeButton(
                    title = "All pages",
                    selected = uiState.extractionScope == TextExtractionScope.AllPages,
                    onClick = { onScopeChange(TextExtractionScope.AllPages) },
                    modifier = Modifier.weight(1f),
                )
                ExtractionScopeButton(
                    title = "Page range",
                    selected = uiState.extractionScope == TextExtractionScope.PageRange,
                    onClick = { onScopeChange(TextExtractionScope.PageRange) },
                    modifier = Modifier.weight(1f),
                )
            }

            if (uiState.extractionScope == TextExtractionScope.PageRange) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.startPageInput,
                        onValueChange = onStartPageChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Start page") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.endPageInput,
                        onValueChange = onEndPageChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("End page") },
                        singleLine = true,
                    )
                }
            }

            OutlinedTextField(
                value = uiState.outputFileName,
                onValueChange = onOutputFileNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Output text file name") },
                supportingText = { Text(".txt is added automatically if needed") },
                singleLine = true,
            )

            Button(
                onClick = onExtractText,
                enabled = canExtract,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isExtracting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Extracting text…")
                } else {
                    Text("Extract Text")
                }
            }

            uiState.extractedTextResult?.let { result ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Extracted text ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AssistChip(
                        onClick = {},
                        label = {
                            val pageLabel = if (result.pageRange.startPage == result.pageRange.endPage) {
                                "page ${result.pageRange.startPage}"
                            } else {
                                "pages ${result.pageRange.startPage}-${result.pageRange.endPage}"
                            }
                            Text("${result.characterCount} chars • $pageLabel")
                        },
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    ) {
                        Text(
                            text = result.text.ifBlank { "No extractable text was found in the selected pages." },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            maxLines = 12,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    ResultActionButtons(
                        onOpen = { onOpenTextFile(result) },
                        onShare = { onShareTextFile(result) },
                        onSaveCopy = { onSaveCopyTextFile(result) },
                    )
                    OutlinedButton(
                        onClick = { onCopyText(result.text) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Text(
                            text = "Copy extracted text",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractionScopeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) {
            Text(title)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(title)
        }
    }
}

