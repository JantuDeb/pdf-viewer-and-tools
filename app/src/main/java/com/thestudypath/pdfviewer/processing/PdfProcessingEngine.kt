package com.thestudypath.pdfviewer.processing

import java.io.File

data class PdfDocumentInspection(
    val pageCount: Int,
    val isEncrypted: Boolean,
)

data class PdfMergeRequest(
    val inputFiles: List<File>,
    val outputFile: File,
)

data class PdfMergeResult(
    val outputFile: File,
    val pageCount: Int,
    val inputFileCount: Int,
    val fileSizeBytes: Long,
)

data class ImageToPdfRequest(
    val imageFiles: List<File>,
    val outputFile: File,
)

data class ImageToPdfResult(
    val outputFile: File,
    val pageCount: Int,
    val fileSizeBytes: Long,
)

enum class PdfCompressionProfile {
    Light,
    Balanced,
    Strong,
}

data class PdfCompressRequest(
    val inputFile: File,
    val outputFile: File,
    val profile: PdfCompressionProfile = PdfCompressionProfile.Balanced,
    val keepMetadata: Boolean = true,
)

data class PdfCompressResult(
    val outputFile: File,
    val pageCount: Int,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
) {
    val savingsBytes: Long get() = originalSizeBytes - compressedSizeBytes
    val savingsPercent: Int
        get() = if (originalSizeBytes <= 0) 0 else ((savingsBytes * 100f) / originalSizeBytes).toInt()
}

data class PdfPageRange(
    val startPage: Int,
    val endPage: Int,
) {
    val pageCount: Int get() = endPage - startPage + 1
}

data class PdfSplitTarget(
    val pageRange: PdfPageRange,
    val outputFile: File,
)

data class PdfSplitRequest(
    val inputFile: File,
    val targets: List<PdfSplitTarget>,
)

data class PdfSplitOutput(
    val outputFile: File,
    val pageRange: PdfPageRange,
    val pageCount: Int,
    val fileSizeBytes: Long,
)

data class PdfSplitResult(
    val inputFile: File,
    val outputs: List<PdfSplitOutput>,
)

data class TextExtractionRequest(
    val inputFile: File,
    val pageRange: PdfPageRange? = null,
)

data class TextExtractionResult(
    val text: String,
    val pageRange: PdfPageRange,
    val pageCount: Int,
    val characterCount: Int,
)

interface PdfProcessingEngine {
    suspend fun inspectDocument(
        file: File,
        password: String? = null,
    ): Result<PdfDocumentInspection>

    suspend fun mergeDocuments(request: PdfMergeRequest): Result<PdfMergeResult>

    suspend fun compressDocument(request: PdfCompressRequest): Result<PdfCompressResult>

    suspend fun createPdfFromImages(request: ImageToPdfRequest): Result<ImageToPdfResult>

    suspend fun splitDocument(request: PdfSplitRequest): Result<PdfSplitResult>

    suspend fun extractText(request: TextExtractionRequest): Result<TextExtractionResult>
}

