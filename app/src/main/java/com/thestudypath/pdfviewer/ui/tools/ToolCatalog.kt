package com.thestudypath.pdfviewer.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
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
    const val PdfToImages = "pdf-to-images"
    const val ExtractEmbeddedImages = "extract-embedded-images"
    const val CropPageRegion = "crop-page-region"
    const val MergePdf = "merge-pdf"
    const val SplitPdf = "split-pdf"
    const val ExtractText = "extract-text"
    const val AddPassword = "add-password"
    const val RemovePassword = "remove-password"
    const val WatermarkPdf = "watermark-pdf"
    const val RotatePages = "rotate-pages"
    const val ExtractPages = "extract-pages"
    const val DeletePages = "delete-pages"
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
        id = ToolIds.PdfToImages,
        title = "PDF to Images",
        description = "Choose one PDF, export selected pages as PNG or JPEG, and save one image per page.",
        icon = Icons.Default.Image,
        selectionMode = DocumentSelectionMode.Single,
    ),
    ToolEntry(
        id = ToolIds.ExtractEmbeddedImages,
        title = "Extract Embedded Images",
        description = "Choose one PDF, scan selected pages for embedded images, and save each unique image into a folder.",
        icon = Icons.Default.Image,
        selectionMode = DocumentSelectionMode.Single,
    ),
    ToolEntry(
        id = ToolIds.CropPageRegion,
        title = "Crop Page Region",
        description = "Choose one PDF, crop a selected region from chosen pages, and export the result as PNG or JPEG images.",
        icon = Icons.Default.Crop,
        selectionMode = DocumentSelectionMode.Single,
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
        description = "Choose one PDF, enter a password, and save a protected copy.",
        icon = Icons.Default.Lock,
        selectionMode = DocumentSelectionMode.Single,
    ),
    ToolEntry(
        id = ToolIds.RemovePassword,
        title = "Remove Password",
        description = "Choose a protected PDF, enter its current password, and save an unlocked copy.",
        icon = Icons.Default.LockOpen,
        selectionMode = DocumentSelectionMode.Single,
    ),
    ToolEntry(
        id = ToolIds.WatermarkPdf,
        title = "Watermark PDF",
        description = "Add text watermarks to one PDF with placement presets and optional page-range targeting.",
        icon = Icons.Default.PictureAsPdf,
        selectionMode = DocumentSelectionMode.Single,
    ),
    ToolEntry(
        id = ToolIds.RotatePages,
        title = "Rotate Pages",
        description = "Rotate selected pages in one PDF and save the result as a new copy.",
        icon = Icons.Default.PictureAsPdf,
        selectionMode = DocumentSelectionMode.Single,
    ),
    ToolEntry(
        id = ToolIds.ExtractPages,
        title = "Extract Pages",
        description = "Keep selected pages from one PDF and export them into a new PDF.",
        icon = Icons.Default.ContentCut,
        selectionMode = DocumentSelectionMode.Single,
    ),
    ToolEntry(
        id = ToolIds.DeletePages,
        title = "Delete Pages",
        description = "Remove selected pages from one PDF and save the remaining pages as a new copy.",
        icon = Icons.Default.Delete,
        selectionMode = DocumentSelectionMode.Single,
    ),
)

internal fun toolDefinitionFor(toolId: String): ToolEntry? =
    toolDefinitions.firstOrNull { it.id == toolId }

