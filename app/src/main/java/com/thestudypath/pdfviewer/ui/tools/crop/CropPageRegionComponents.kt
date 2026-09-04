package com.thestudypath.pdfviewer.ui.tools.crop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thestudypath.pdfviewer.processing.PdfCropPageRegionResult
import com.thestudypath.pdfviewer.processing.PdfImageExportFormat
import com.thestudypath.pdfviewer.processing.PdfImageExportOutput
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.tools.compress.formatFileSize
import java.io.File

@Composable
internal fun CropPageRegionWorkspaceCard(
    uiState: CropPageRegionUiState,
    onChooseDocument: () -> Unit,
    onPageRangesChange: (String) -> Unit,
    onImageFormatChange: (PdfImageExportFormat) -> Unit,
    onQualityOptionChange: (CropExportQualityOption) -> Unit,
    onPresetChange: (PageCropPreset) -> Unit,
    onLeftPercentChange: (Float) -> Unit,
    onTopPercentChange: (Float) -> Unit,
    onRightPercentChange: (Float) -> Unit,
    onBottomPercentChange: (Float) -> Unit,
    onOutputFolderNameChange: (String) -> Unit,
    onExportCrop: () -> Unit,
    onOpenImage: (File, PdfImageExportFormat) -> Unit,
    onShareImage: (File, PdfImageExportFormat) -> Unit,
    onShareAllImages: (PdfCropPageRegionResult) -> Unit,
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
                        text = "Crop Page Region",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose one PDF, define a crop area, and export the selected region from each chosen page as an image.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Crop,
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Image format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfImageExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = uiState.imageFormat == format,
                            onClick = { onImageFormatChange(format) },
                            label = { Text(format.label()) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quality",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CropExportQualityOption.entries.forEach { option ->
                        FilterChip(
                            selected = uiState.qualityOption == option,
                            onClick = { onQualityOptionChange(option) },
                            label = { Text("${option.label} (${option.dpi} DPI)") },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.pageRangesInput,
                onValueChange = onPageRangesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pages to crop") },
                supportingText = { Text("Leave blank for all pages. Examples: 1-3, 5, 8-10") },
                singleLine = true,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Crop preset",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                PageCropPreset.entries.chunked(3).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { preset ->
                            FilterChip(
                                selected = uiState.preset == preset,
                                onClick = { onPresetChange(preset) },
                                label = { Text(preset.label) },
                            )
                        }
                    }
                }
                Text(
                    text = "The crop percentages below are applied uniformly to every selected page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            CropSliderRow(
                label = "Left",
                value = uiState.cropRegion.leftPercent,
                rangeText = "${uiState.cropRegion.leftPercent}% from left edge",
                onValueChange = onLeftPercentChange,
            )
            CropSliderRow(
                label = "Top",
                value = uiState.cropRegion.topPercent,
                rangeText = "${uiState.cropRegion.topPercent}% from top edge",
                onValueChange = onTopPercentChange,
            )
            CropSliderRow(
                label = "Right",
                value = uiState.cropRegion.rightPercent,
                rangeText = "${uiState.cropRegion.rightPercent}% from left edge",
                onValueChange = onRightPercentChange,
            )
            CropSliderRow(
                label = "Bottom",
                value = uiState.cropRegion.bottomPercent,
                rangeText = "${uiState.cropRegion.bottomPercent}% from top edge",
                onValueChange = onBottomPercentChange,
            )

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        "Crop: ${uiState.cropRegion.leftPercent}%-${uiState.cropRegion.rightPercent}% width, ${uiState.cropRegion.topPercent}%-${uiState.cropRegion.bottomPercent}% height"
                    )
                },
            )

            OutlinedTextField(
                value = uiState.outputFolderName,
                onValueChange = onOutputFolderNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Output folder name") },
                supportingText = { Text("Each cropped page region is exported into one app-private folder") },
                singleLine = true,
            )

            Button(
                onClick = onExportCrop,
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
                    Text("Exporting cropped regions…")
                } else {
                    Text("Export cropped regions")
                }
            }

            uiState.result?.let { result ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cropped images ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "${result.exportedImages.size} image${if (result.exportedImages.size == 1) "" else "s"}"
                                )
                            },
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(result.format.label()) },
                        )
                    }
                    Text(
                        text = "Saved in the ${uiState.outputFolderName} folder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        result.exportedImages.firstOrNull()?.let { firstImage ->
                            Button(
                                onClick = { onOpenImage(firstImage.outputFile, result.format) },
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
                        CroppedImageRow(
                            image = image,
                            onOpenImage = { onOpenImage(image.outputFile, result.format) },
                            onShareImage = { onShareImage(image.outputFile, result.format) },
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
private fun CropSliderRow(
    label: String,
    value: Int,
    rangeText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = 0f..100f,
        )
        Text(
            text = rangeText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CroppedImageRow(
    image: PdfImageExportOutput,
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
                text = "Page ${image.pageNumber}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${image.width} × ${image.height} • ${formatFileSize(image.fileSizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

