package com.thestudypath.pdfviewer.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.graphics.vector.ImageVector
import com.thestudypath.pdfviewer.ui.documents.DocumentSelectionMode

internal data class ToolEntry(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val selectionMode: DocumentSelectionMode? = null,
    val status: String? = null,
)

internal object ToolIds {
    const val CompressPdf = "compress-pdf"
    const val ImagesToPdf = "images-to-pdf"
    const val MergePdf = "merge-pdf"
    const val SplitPdf = "split-pdf"
    const val ExtractText = "extract-text"
    const val AddPassword = "add-password"
    const val RemovePassword = "remove-password"
}

internal val toolDefinitions = listOf(
    ToolEntry(
        id = ToolIds.CompressPdf,
        title = "Compress PDF",
        description = "Choose one PDF and create a smaller copy with light, balanced, or strong compression.",
        icon = Icons.Default.Compress,
    ),
    ToolEntry(
        id = ToolIds.ImagesToPdf,
        title = "Images to PDF",
        description = "Pick one or more images, arrange them, and create a PDF with one page per image.",
        icon = Icons.Default.Image,
    ),
    ToolEntry(
        id = ToolIds.MergePdf,
        title = "Merge PDFs",
        description = "Select multiple PDFs, order them, and combine them into a single file.",
        icon = Icons.Default.PictureAsPdf,
    ),
    ToolEntry(
        id = ToolIds.SplitPdf,
        title = "Split PDF",
        description = "Split one PDF by every N pages or by custom page ranges.",
        icon = Icons.Default.PictureAsPdf,
    ),
    ToolEntry(
        id = ToolIds.ExtractText,
        title = "Extract Text",
        description = "Extract plain text from all pages or a selected page range in a PDF.",
        icon = Icons.AutoMirrored.Filled.TextSnippet,
    ),
    ToolEntry(
        id = ToolIds.AddPassword,
        title = "Add Password",
        description = "Select a PDF to secure once encryption support is ready.",
        icon = Icons.Default.Lock,
        selectionMode = DocumentSelectionMode.Single,
        status = "Validation pending",
    ),
    ToolEntry(
        id = ToolIds.RemovePassword,
        title = "Remove Password",
        description = "Select a protected PDF to stage a decrypt flow.",
        icon = Icons.Default.LockOpen,
        selectionMode = DocumentSelectionMode.Single,
        status = "Validation pending",
    ),
)

internal fun toolDefinitionFor(toolId: String): ToolEntry? =
    toolDefinitions.firstOrNull { it.id == toolId }

