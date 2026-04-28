package com.thestudypath.pdfviewer.processing

import android.graphics.Bitmap
import android.graphics.Rect
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceGray
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Collections
import java.util.IdentityHashMap
import java.io.File
import androidx.core.graphics.scale

class PdfBoxProcessingEngine : PdfProcessingEngine {
    override suspend fun inspectDocument(
        file: File,
        password: String?,
    ): Result<PdfDocumentInspection> = withContext(Dispatchers.IO) {
        runCatching {
            val document = if (password.isNullOrEmpty()) PDDocument.load(file) else PDDocument.load(file, password)
            document.use {
                PdfDocumentInspection(
                    pageCount = it.numberOfPages,
                    isEncrypted = it.isEncrypted,
                )
            }
        }
    }

    override suspend fun mergeDocuments(
        request: PdfMergeRequest,
    ): Result<PdfMergeResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(request.inputFiles.size >= 2) { "Select at least 2 PDFs to merge." }

            request.inputFiles.forEach(::validatePdfInputFile)

            request.outputFile.parentFile?.mkdirs()
            if (request.outputFile.exists() && !request.outputFile.delete()) {
                error("Could not overwrite ${request.outputFile.name}.")
            }

            PDFMergerUtility().apply {
                destinationFileName = request.outputFile.absolutePath
                request.inputFiles.forEach(::addSource)
            }.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

            val pageCount = PDDocument.load(request.outputFile).use { it.numberOfPages }
            PdfMergeResult(
                outputFile = request.outputFile,
                pageCount = pageCount,
                inputFileCount = request.inputFiles.size,
                fileSizeBytes = request.outputFile.length(),
            )
        }
    }

    override suspend fun compressDocument(
        request: PdfCompressRequest,
    ): Result<PdfCompressResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)

            val originalSizeBytes = request.inputFile.length()
            request.outputFile.parentFile?.mkdirs()
            if (request.outputFile.exists() && !request.outputFile.delete()) {
                error("Could not overwrite ${request.outputFile.name}.")
            }

            try {
                PDDocument.load(request.inputFile).use { document ->
                    val changedResources = applyCompressionProfile(document, request.profile)

                    if (!shouldRewriteCompressedPdf(changedResources, request.keepMetadata)) {
                        Files.copy(
                            request.inputFile.toPath(),
                            request.outputFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING,
                        )

                        return@use PdfCompressResult(
                            outputFile = request.outputFile,
                            pageCount = document.numberOfPages,
                            originalSizeBytes = originalSizeBytes,
                            compressedSizeBytes = request.outputFile.length(),
                        )
                    }

                    if (!request.keepMetadata) {
                        document.documentInformation = PDDocumentInformation()
                    }

                    document.save(request.outputFile)

                    if (request.outputFile.length() > originalSizeBytes) {
                        request.outputFile.delete()
                        PDDocument.load(request.inputFile).use { fallbackDocument ->
                            if (!request.keepMetadata) {
                                fallbackDocument.documentInformation = PDDocumentInformation()
                            }
                            fallbackDocument.save(request.outputFile)
                        }
                    }

                    PdfCompressResult(
                        outputFile = request.outputFile,
                        pageCount = document.numberOfPages,
                        originalSizeBytes = originalSizeBytes,
                        compressedSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and cannot be compressed yet.")
            }
        }
    }

    private fun applyCompressionProfile(
        document: PDDocument,
        profile: PdfCompressionProfile,
    ): Boolean {
        val tuning = profile.toTuning()
        val visitedXObjects = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        var changed = false
        document.pages.forEach { page ->
            changed = recompressResources(
                document = document,
                resources = page.resources,
                tuning = tuning,
                visitedXObjects = visitedXObjects,
            ) || changed
        }
        return changed
    }

    private fun recompressResources(
        document: PDDocument,
        resources: PDResources?,
        tuning: CompressionTuning,
        visitedXObjects: MutableSet<Any>,
    ): Boolean {
        if (resources == null) return false

        var changed = false

        resources.xObjectNames.forEach { name ->
            val xObject = runCatching { resources.getXObject(name) }.getOrNull() ?: return@forEach
            val xObjectKey = xObject.cosObject
            if (!visitedXObjects.add(xObjectKey)) return@forEach

            when (xObject) {
                is PDImageXObject -> {
                    runCatching {
                        val support = inspectImageCompressionSupport(xObject) ?: return@runCatching
                        val strategy = chooseImageCompressionStrategy(support)
                        when (strategy) {
                            PdfImageCompressionStrategy.Skip -> return@runCatching
                            else -> Unit
                        }
                        if (!shouldRecompressImage(support, tuning.maxDimension)) return@runCatching

                        val subsampling = calculateSubsampling(
                            width = xObject.width,
                            height = xObject.height,
                            maxDimension = tuning.maxDimension,
                        )
                        val sourceBitmap = xObject.getImage(Rect(0, 0, xObject.width, xObject.height), subsampling)
                        val scaledBitmap = scaleBitmapIfNeeded(sourceBitmap, tuning.maxDimension)
                        try {
                            val effectiveStrategy = if (strategy == PdfImageCompressionStrategy.Jpeg && scaledBitmap.hasAlpha()) {
                                PdfImageCompressionStrategy.Skip
                            } else {
                                strategy
                            }

                            val recompressedImage = when (effectiveStrategy) {
                                PdfImageCompressionStrategy.Jpeg -> JPEGFactory.createFromImage(
                                    document,
                                    scaledBitmap,
                                    tuning.jpegQuality,
                                )

                                PdfImageCompressionStrategy.Skip -> null
                            }

                            if (recompressedImage != null) {
                                resources.put(name, recompressedImage)
                                changed = true
                            }
                        } finally {
                            if (scaledBitmap !== sourceBitmap && !scaledBitmap.isRecycled) {
                                scaledBitmap.recycle()
                            }
                        }
                    }
                }

                is PDFormXObject -> {
                    changed = recompressResources(
                        document = document,
                        resources = xObject.resources,
                        tuning = tuning,
                        visitedXObjects = visitedXObjects,
                    ) || changed
                }
            }
        }

        return changed
    }

    private fun scaleBitmapIfNeeded(
        bitmap: Bitmap,
        maxDimension: Int,
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxOf(width, height).toFloat()
        val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
        return bitmap.scale(scaledWidth, scaledHeight)
    }

    private fun inspectImageCompressionSupport(
        image: PDImageXObject,
    ): PdfImageCompressionSupport? {
        return runCatching {
            PdfImageCompressionSupport(
                hasSoftMask = image.softMask != null,
                hasExplicitMask = image.mask != null,
                hasColorKeyMask = image.colorKeyMask != null,
                isStencil = image.isStencil,
                colorSpaceName = image.colorSpace.name,
                suffix = image.suffix,
                width = image.width,
                height = image.height,
            )
        }.getOrNull()
    }

    private fun calculateSubsampling(
        width: Int,
        height: Int,
        maxDimension: Int,
    ): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        val largestDimension = maxOf(width, height)
        if (largestDimension <= maxDimension) return 1
        return ((largestDimension + maxDimension - 1) / maxDimension).coerceAtLeast(1)
    }

    private fun PdfCompressionProfile.toTuning(): CompressionTuning = when (this) {
        PdfCompressionProfile.Light -> CompressionTuning(
            jpegQuality = 0.85f,
            maxDimension = 2200,
        )

        PdfCompressionProfile.Balanced -> CompressionTuning(
            jpegQuality = 0.70f,
            maxDimension = 1700,
        )

        PdfCompressionProfile.Strong -> CompressionTuning(
            jpegQuality = 0.55f,
            maxDimension = 1200,
        )
    }

    private data class CompressionTuning(
        val jpegQuality: Float,
        val maxDimension: Int,
    )

    override suspend fun createPdfFromImages(
        request: ImageToPdfRequest,
    ): Result<ImageToPdfResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(request.imageFiles.isNotEmpty()) {
                "Choose at least 1 image to create a PDF."
            }

            request.imageFiles.forEach { imageFile ->
                require(imageFile.exists()) {
                    "${imageFile.name} is no longer available. Pick the image again and retry."
                }
            }

            request.outputFile.parentFile?.mkdirs()
            if (request.outputFile.exists() && !request.outputFile.delete()) {
                error("Could not overwrite ${request.outputFile.name}.")
            }

            PDDocument(MemoryUsageSetting.setupMainMemoryOnly()).use { document ->
                request.imageFiles.forEach { imageFile ->
                    val image = PDImageXObject.createFromFileByContent(imageFile, document)
                    require(image.width > 0 && image.height > 0) {
                        "${imageFile.name} could not be decoded as an image."
                    }

                    val page = PDPage(PDRectangle(image.width.toFloat(), image.height.toFloat()))
                    document.addPage(page)
                    PDPageContentStream(document, page).use { contentStream ->
                        contentStream.drawImage(
                            image,
                            0f,
                            0f,
                            page.mediaBox.width,
                            page.mediaBox.height,
                        )
                    }
                }
                document.save(request.outputFile)
            }

            ImageToPdfResult(
                outputFile = request.outputFile,
                pageCount = request.imageFiles.size,
                fileSizeBytes = request.outputFile.length(),
            )
        }
    }

    override suspend fun splitDocument(
        request: PdfSplitRequest,
    ): Result<PdfSplitResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(request.inputFile.exists()) {
                "${request.inputFile.name} is no longer available. Re-import it and try again."
            }
            require(request.targets.isNotEmpty()) {
                "Add at least one split output."
            }

            try {
                PDDocument.load(request.inputFile).use { sourceDocument ->
                    val totalPages = sourceDocument.numberOfPages
                    val claimedPages = mutableSetOf<Int>()
                    val outputs = request.targets.mapIndexed { index, target ->
                        validatePageRange(target.pageRange, totalPages)
                        (target.pageRange.startPage..target.pageRange.endPage).forEach { pageNumber ->
                            require(claimedPages.add(pageNumber)) {
                                "Split outputs cannot overlap. Page $pageNumber was selected more than once."
                            }
                        }

                        val outputFile = target.outputFile
                        outputFile.parentFile?.mkdirs()
                        if (outputFile.exists() && !outputFile.delete()) {
                            error("Could not overwrite ${outputFile.name}.")
                        }

                        PDDocument(MemoryUsageSetting.setupMainMemoryOnly()).use { destinationDocument ->
                            val importedPages = (target.pageRange.startPage..target.pageRange.endPage).map { pageNumber ->
                                val sourcePage = sourceDocument.getPage(pageNumber - 1)
                                destinationDocument.importPage(sourcePage).also { importedPage ->
                                    copyPageResourcesIfNeeded(importedPage, sourcePage)
                                }
                            }
                            require(importedPages.isNotEmpty()) {
                                "Split output ${index + 1} does not contain any pages."
                            }
                            destinationDocument.save(outputFile)
                        }

                        PdfSplitOutput(
                            outputFile = outputFile,
                            pageRange = target.pageRange,
                            pageCount = target.pageRange.pageCount,
                            fileSizeBytes = outputFile.length(),
                        )
                    }

                    PdfSplitResult(
                        inputFile = request.inputFile,
                        outputs = outputs,
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and cannot be split yet.")
            }
        }
    }

    override suspend fun extractText(
        request: TextExtractionRequest,
    ): Result<TextExtractionResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    val totalPages = document.numberOfPages
                    val pageRange = request.pageRange ?: PdfPageRange(
                        startPage = 1,
                        endPage = totalPages,
                    )
                    validatePageRange(pageRange, totalPages)

                    val stripper = PDFTextStripper().apply {
                        startPage = pageRange.startPage
                        endPage = pageRange.endPage
                    }
                    val text = stripper.getText(document).trimEnd()

                    TextExtractionResult(
                        text = text,
                        pageRange = pageRange,
                        pageCount = pageRange.pageCount,
                        characterCount = text.length,
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and text extraction is not available yet.")
            }
        }
    }

    private fun validatePdfInputFile(file: File) {
        require(file.exists()) {
            "${file.name} is no longer available. Re-import it and try again."
        }
        try {
            PDDocument.load(file).use { }
        } catch (_: InvalidPasswordException) {
            error("${file.name} is password protected and cannot be processed yet.")
        }
    }

    private fun validatePageRange(
        pageRange: PdfPageRange,
        totalPages: Int,
    ) {
        require(pageRange.startPage >= 1) {
            "Page ranges must start at page 1 or later."
        }
        require(pageRange.endPage >= pageRange.startPage) {
            "Page ranges must end on or after their start page."
        }
        require(pageRange.endPage <= totalPages) {
            "Page range ${pageRange.startPage}-${pageRange.endPage} exceeds the document length of $totalPages pages."
        }
    }

    private fun copyPageResourcesIfNeeded(
        importedPage: PDPage,
        sourcePage: PDPage,
    ) {
        if (sourcePage.resources != null && !sourcePage.cosObject.containsKey(COSName.RESOURCES)) {
            importedPage.resources = sourcePage.resources
        }
    }
}

internal data class PdfImageCompressionSupport(
    val hasSoftMask: Boolean,
    val hasExplicitMask: Boolean,
    val hasColorKeyMask: Boolean,
    val isStencil: Boolean,
    val colorSpaceName: String?,
    val suffix: String?,
    val width: Int,
    val height: Int,
)

internal enum class PdfImageCompressionStrategy {
    Jpeg,
    Skip,
}

internal fun chooseImageCompressionStrategy(
    support: PdfImageCompressionSupport,
): PdfImageCompressionStrategy {
    if (support.isStencil) return PdfImageCompressionStrategy.Skip

    when (support.suffix?.lowercase()) {
        "jpx", "jb2", "tiff" -> return PdfImageCompressionStrategy.Skip
    }

    val isSimpleDeviceColorSpace = support.colorSpaceName == PDDeviceRGB.INSTANCE.name ||
        support.colorSpaceName == PDDeviceGray.INSTANCE.name
    if (!isSimpleDeviceColorSpace) return PdfImageCompressionStrategy.Skip

    if (support.hasSoftMask || support.hasExplicitMask || support.hasColorKeyMask) {
        return PdfImageCompressionStrategy.Skip
    }

    return PdfImageCompressionStrategy.Jpeg
}

internal fun shouldRecompressImage(
    support: PdfImageCompressionSupport,
    maxDimension: Int,
): Boolean = support.width > maxDimension || support.height > maxDimension

internal fun shouldRewriteCompressedPdf(
    changedResources: Boolean,
    keepMetadata: Boolean,
): Boolean = changedResources || !keepMetadata

