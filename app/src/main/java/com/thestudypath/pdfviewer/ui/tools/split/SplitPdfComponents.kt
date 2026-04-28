package com.thestudypath.pdfviewer.ui.tools.split

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Share
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
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard

@Composable
fun SplitPdfWorkspaceCard(
    uiState: SplitPdfUiState,
    onChooseDocument: () -> Unit,
    onSplitModeChange: (SplitMode) -> Unit,
    onPagesPerSplitChange: (String) -> Unit,
    onCustomRangesChange: (String) -> Unit,
    onOutputBaseNameChange: (String) -> Unit,
    onSplit: () -> Unit,
    onOpenSplitDocument: (DocumentItem) -> Unit,
    onShareSplitDocument: (DocumentItem) -> Unit,
) {
    val canSplit = uiState.selectedDocument != null &&
        uiState.selectedDocumentPageCount != null &&
        !uiState.isInspectingDocument &&
        !uiState.isSplitting

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
                        text = "Split PDF",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose one PDF, then split it by every N pages or by custom page ranges.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ContentCut,
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
            } ?: Text(
                text = "Pick one PDF from your catalog to configure a split job.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "Split mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SplitModeButton(
                    title = "Every N pages",
                    selected = uiState.splitMode == SplitMode.EveryNPages,
                    onClick = { onSplitModeChange(SplitMode.EveryNPages) },
                    modifier = Modifier.weight(1f),
                )
                SplitModeButton(
                    title = "Custom ranges",
                    selected = uiState.splitMode == SplitMode.CustomRanges,
                    onClick = { onSplitModeChange(SplitMode.CustomRanges) },
                    modifier = Modifier.weight(1f),
                )
            }

            when (uiState.splitMode) {
                SplitMode.EveryNPages -> {
                    OutlinedTextField(
                        value = uiState.pagesPerSplitInput,
                        onValueChange = onPagesPerSplitChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pages per output PDF") },
                        supportingText = { Text("Example: enter 2 to split a 9-page PDF into 2,2,2,2,1") },
                        singleLine = true,
                    )
                }
                SplitMode.CustomRanges -> {
                    OutlinedTextField(
                        value = uiState.customRangesInput,
                        onValueChange = onCustomRangesChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Custom page ranges") },
                        supportingText = { Text("Use comma-separated ranges such as 1-3, 5, 8-10") },
                        singleLine = true,
                    )
                }
            }

            OutlinedTextField(
                value = uiState.outputBaseName,
                onValueChange = onOutputBaseNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Output base name") },
                supportingText = { Text("Each output gets a part number and page label automatically") },
                singleLine = true,
            )

            Button(
                onClick = onSplit,
                enabled = canSplit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSplitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Splitting PDF…")
                } else {
                    Text("Split PDF")
                }
            }

            if (uiState.resultDocuments.isNotEmpty()) {
                Text(
                    text = "Split outputs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false,
                ) {
                    items(uiState.resultDocuments, key = { it.id }) { document ->
                        SplitOutputItem(
                            document = document,
                            onOpen = { onOpenSplitDocument(document) },
                            onShare = { onShareSplitDocument(document) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitModeButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(title)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Text(title)
        }
    }
}

@Composable
private fun SplitOutputItem(
    document: DocumentItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
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
            DocumentSummaryCard(document = document)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Text(
                        text = "Open",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Button(
                    onClick = onShare,
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

