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

data class PdfAddPasswordRequest(
    val inputFile: File,
    val outputFile: File,
    val userPassword: String,
    val ownerPassword: String? = null,
)

data class PdfAddPasswordResult(
    val outputFile: File,
    val pageCount: Int,
    val fileSizeBytes: Long,
)

data class PdfRemovePasswordRequest(
    val inputFile: File,
    val outputFile: File,
    val password: String,
)

data class PdfRemovePasswordResult(
    val outputFile: File,
    val pageCount: Int,
    val fileSizeBytes: Long,
)

enum class VisibleSignaturePlacement {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    Center,
}

data class VisibleSignatureStampRequest(
    val inputFile: File,
    val signatureImageFile: File,
    val outputFile: File,
    val pageNumber: Int,
    val placement: VisibleSignaturePlacement = VisibleSignaturePlacement.BottomRight,
    val widthPercent: Int = 24,
    val marginPercent: Int = 4,
)

data class VisibleSignatureStampResult(
    val outputFile: File,
    val pageCount: Int,
    val signedPageNumber: Int,
    val fileSizeBytes: Long,
)

data class PdfRotatePagesRequest(
    val inputFile: File,
    val outputFile: File,
    val pageRanges: List<PdfPageRange>,
    val rotationDegrees: Int,
)

data class PdfRotatePagesResult(
    val outputFile: File,
    val pageCount: Int,
    val rotatedPageCount: Int,
    val fileSizeBytes: Long,
)

data class PdfExtractPagesRequest(
    val inputFile: File,
    val outputFile: File,
    val pageRanges: List<PdfPageRange>,
)

data class PdfExtractPagesResult(
    val outputFile: File,
    val pageCount: Int,
    val extractedPageCount: Int,
    val fileSizeBytes: Long,
)

data class PdfDeletePagesRequest(
    val inputFile: File,
    val outputFile: File,
    val pageRanges: List<PdfPageRange>,
)

data class PdfDeletePagesResult(
    val outputFile: File,
    val pageCount: Int,
    val removedPageCount: Int,
    val fileSizeBytes: Long,
)

enum class PdfWatermarkPlacement {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    Center,
    DiagonalCenter,
}

data class PdfTextWatermarkRequest(
    val inputFile: File,
    val outputFile: File,
    val text: String,
    val pageRanges: List<PdfPageRange>,
    val placement: PdfWatermarkPlacement = PdfWatermarkPlacement.DiagonalCenter,
    val textScalePercent: Int = 14,
    val marginPercent: Int = 5,
    val opacityPercent: Int = 18,
)

data class PdfTextWatermarkResult(
    val outputFile: File,
    val pageCount: Int,
    val watermarkedPageCount: Int,
    val fileSizeBytes: Long,
)

enum class PdfImageExportFormat {
    Png,
    Jpeg,
}

data class PdfImageExportOutput(
    val outputFile: File,
    val pageNumber: Int,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
)

data class PdfToImagesRequest(
    val inputFile: File,
    val outputDirectory: File,
    val outputFileNamePrefix: String,
    val pageRanges: List<PdfPageRange>,
    val format: PdfImageExportFormat = PdfImageExportFormat.Png,
    val renderDpi: Int = 144,
    val jpegQualityPercent: Int = 92,
)

data class PdfToImagesResult(
    val outputDirectory: File,
    val pageCount: Int,
    val exportedImages: List<PdfImageExportOutput>,
    val format: PdfImageExportFormat,
)

data class EmbeddedImageExportOutput(
    val outputFile: File,
    val pageNumbers: List<Int>,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val format: PdfImageExportFormat,
)

data class ExtractEmbeddedImagesRequest(
    val inputFile: File,
    val outputDirectory: File,
    val outputFileNamePrefix: String,
    val pageRanges: List<PdfPageRange>,
)

data class ExtractEmbeddedImagesResult(
    val outputDirectory: File,
    val pageCount: Int,
    val exportedImages: List<EmbeddedImageExportOutput>,
)

data class PdfNormalizedCropRect(
    val leftFraction: Float,
    val topFraction: Float,
    val rightFraction: Float,
    val bottomFraction: Float,
)

data class PdfCropPageRegionRequest(
    val inputFile: File,
    val outputDirectory: File,
    val outputFileNamePrefix: String,
    val pageRanges: List<PdfPageRange>,
    val cropRect: PdfNormalizedCropRect,
    val format: PdfImageExportFormat = PdfImageExportFormat.Png,
    val renderDpi: Int = 144,
    val jpegQualityPercent: Int = 92,
)

data class PdfCropPageRegionResult(
    val outputDirectory: File,
    val pageCount: Int,
    val exportedImages: List<PdfImageExportOutput>,
    val format: PdfImageExportFormat,
    val cropRect: PdfNormalizedCropRect,
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

    suspend fun addPassword(request: PdfAddPasswordRequest): Result<PdfAddPasswordResult>

    suspend fun removePassword(request: PdfRemovePasswordRequest): Result<PdfRemovePasswordResult>

    suspend fun stampVisibleSignature(request: VisibleSignatureStampRequest): Result<VisibleSignatureStampResult>

    suspend fun rotatePages(request: PdfRotatePagesRequest): Result<PdfRotatePagesResult>

    suspend fun extractPages(request: PdfExtractPagesRequest): Result<PdfExtractPagesResult>

    suspend fun deletePages(request: PdfDeletePagesRequest): Result<PdfDeletePagesResult>

    suspend fun addTextWatermark(request: PdfTextWatermarkRequest): Result<PdfTextWatermarkResult>

    suspend fun exportPdfToImages(request: PdfToImagesRequest): Result<PdfToImagesResult>

    suspend fun extractEmbeddedImages(request: ExtractEmbeddedImagesRequest): Result<ExtractEmbeddedImagesResult>

    suspend fun cropPageRegions(request: PdfCropPageRegionRequest): Result<PdfCropPageRegionResult>
}

