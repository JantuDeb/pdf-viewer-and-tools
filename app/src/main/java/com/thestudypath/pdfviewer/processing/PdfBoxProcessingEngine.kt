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
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream.AppendMode
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceGray
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.PDXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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

    override suspend fun addPassword(
        request: PdfAddPasswordRequest,
    ): Result<PdfAddPasswordResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(request.userPassword.isNotBlank()) {
                "Enter a password to protect the PDF."
            }
            validatePdfInputPresence(request.inputFile)
            prepareOutputFile(request.outputFile)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    require(!document.isEncrypted) {
                        "${request.inputFile.name} is already password protected. Remove the existing password first."
                    }

                    val accessPermission = AccessPermission()
                    val ownerPassword = request.ownerPassword
                        ?.takeIf { it.isNotBlank() }
                        ?: request.userPassword
                    val policy = StandardProtectionPolicy(
                        ownerPassword,
                        request.userPassword,
                        accessPermission,
                    ).apply {
                        setEncryptionKeyLength(128)
                        setPreferAES(true)
                    }

                    document.protect(policy)
                    document.save(request.outputFile)

                    PdfAddPasswordResult(
                        outputFile = request.outputFile,
                        pageCount = document.numberOfPages,
                        fileSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is already password protected. Remove the existing password first.")
            }
        }
    }

    override suspend fun removePassword(
        request: PdfRemovePasswordRequest,
    ): Result<PdfRemovePasswordResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(request.password.isNotBlank()) {
                "Enter the current password for this PDF."
            }
            validatePdfInputPresence(request.inputFile)
            prepareOutputFile(request.outputFile)

            try {
                PDDocument.load(request.inputFile, request.password).use { document ->
                    require(document.isEncrypted) {
                        "${request.inputFile.name} is not password protected."
                    }

                    document.setAllSecurityToBeRemoved(true)
                    document.save(request.outputFile)

                    PdfRemovePasswordResult(
                        outputFile = request.outputFile,
                        pageCount = document.numberOfPages,
                        fileSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("Incorrect password for ${request.inputFile.name}.")
            }
        }
    }

    override suspend fun stampVisibleSignature(
        request: VisibleSignatureStampRequest,
    ): Result<VisibleSignatureStampResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputPresence(request.inputFile)
            require(request.signatureImageFile.exists()) {
                "Signature image is no longer available. Draw or import it again and retry."
            }
            require(request.widthPercent in 5..60) {
                "Signature size must stay between 5% and 60% of the page width."
            }
            require(request.marginPercent in 0..20) {
                "Signature margin must stay between 0% and 20% of the page width."
            }
            prepareOutputFile(request.outputFile)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    require(request.pageNumber in 1..document.numberOfPages) {
                        "Page ${request.pageNumber} is outside this document's ${document.numberOfPages} pages."
                    }

                    val page = document.getPage(request.pageNumber - 1)
                    val pageBox = page.cropBox ?: page.mediaBox
                    val signatureImage = PDImageXObject.createFromFileByContent(
                        request.signatureImageFile,
                        document,
                    )
                    val signatureRect = resolveVisibleSignatureRect(
                        pageWidth = pageBox.width,
                        pageHeight = pageBox.height,
                        imageWidth = signatureImage.width.toFloat(),
                        imageHeight = signatureImage.height.toFloat(),
                        widthPercent = request.widthPercent,
                        marginPercent = request.marginPercent,
                        placement = request.placement,
                    )

                    PDPageContentStream(
                        document,
                        page,
                        AppendMode.APPEND,
                        true,
                        true,
                    ).use { contentStream ->
                        contentStream.drawImage(
                            signatureImage,
                            pageBox.lowerLeftX + signatureRect.x,
                            pageBox.lowerLeftY + signatureRect.y,
                            signatureRect.width,
                            signatureRect.height,
                        )
                    }

                    document.save(request.outputFile)

                    VisibleSignatureStampResult(
                        outputFile = request.outputFile,
                        pageCount = document.numberOfPages,
                        signedPageNumber = request.pageNumber,
                        fileSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and visible signing is not available yet.")
            }
        }
    }

    override suspend fun rotatePages(
        request: PdfRotatePagesRequest,
    ): Result<PdfRotatePagesResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)
            validateRotationDegrees(request.rotationDegrees)
            prepareOutputFile(request.outputFile)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    val selectedPages = validateAndExpandPageRanges(
                        pageRanges = request.pageRanges,
                        totalPages = document.numberOfPages,
                    )
                    selectedPages.forEach { pageNumber ->
                        val page = document.getPage(pageNumber - 1)
                        val currentRotation = page.rotation
                        page.rotation = normalizePageRotation(currentRotation + request.rotationDegrees)
                    }
                    document.save(request.outputFile)

                    PdfRotatePagesResult(
                        outputFile = request.outputFile,
                        pageCount = document.numberOfPages,
                        rotatedPageCount = selectedPages.size,
                        fileSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and pages cannot be rotated yet.")
            }
        }
    }

    override suspend fun extractPages(
        request: PdfExtractPagesRequest,
    ): Result<PdfExtractPagesResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)
            prepareOutputFile(request.outputFile)

            try {
                PDDocument.load(request.inputFile).use { sourceDocument ->
                    val selectedPages = validateAndExpandPageRanges(
                        pageRanges = request.pageRanges,
                        totalPages = sourceDocument.numberOfPages,
                    )

                    PDDocument(MemoryUsageSetting.setupMainMemoryOnly()).use { destinationDocument ->
                        selectedPages.forEach { pageNumber ->
                            val sourcePage = sourceDocument.getPage(pageNumber - 1)
                            destinationDocument.importPage(sourcePage).also { importedPage ->
                                copyPageResourcesIfNeeded(importedPage, sourcePage)
                            }
                        }
                        destinationDocument.save(request.outputFile)
                    }

                    PdfExtractPagesResult(
                        outputFile = request.outputFile,
                        pageCount = selectedPages.size,
                        extractedPageCount = selectedPages.size,
                        fileSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and pages cannot be extracted yet.")
            }
        }
    }

    override suspend fun deletePages(
        request: PdfDeletePagesRequest,
    ): Result<PdfDeletePagesResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)
            prepareOutputFile(request.outputFile)

            try {
                PDDocument.load(request.inputFile).use { sourceDocument ->
                    val pagesToRemove = validateAndExpandPageRanges(
                        pageRanges = request.pageRanges,
                        totalPages = sourceDocument.numberOfPages,
                    )
                    require(pagesToRemove.size < sourceDocument.numberOfPages) {
                        "Select fewer pages to remove. A PDF must keep at least one page."
                    }

                    val pagesToKeep = (1..sourceDocument.numberOfPages)
                        .filterNot(pagesToRemove::contains)
                    require(pagesToKeep.isNotEmpty()) {
                        "Select fewer pages to remove. A PDF must keep at least one page."
                    }

                    PDDocument(MemoryUsageSetting.setupMainMemoryOnly()).use { destinationDocument ->
                        pagesToKeep.forEach { pageNumber ->
                            val sourcePage = sourceDocument.getPage(pageNumber - 1)
                            destinationDocument.importPage(sourcePage).also { importedPage ->
                                copyPageResourcesIfNeeded(importedPage, sourcePage)
                            }
                        }
                        destinationDocument.save(request.outputFile)
                    }

                    PdfDeletePagesResult(
                        outputFile = request.outputFile,
                        pageCount = pagesToKeep.size,
                        removedPageCount = pagesToRemove.size,
                        fileSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and pages cannot be deleted yet.")
            }
        }
    }

    override suspend fun addTextWatermark(
        request: PdfTextWatermarkRequest,
    ): Result<PdfTextWatermarkResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)
            val watermarkText = request.text.trim()
            require(watermarkText.isNotEmpty()) {
                "Enter watermark text before saving the new PDF."
            }
            validateWatermarkScale(request.textScalePercent)
            validateWatermarkMargin(request.marginPercent)
            validateWatermarkOpacity(request.opacityPercent)
            prepareOutputFile(request.outputFile)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    val selectedPages = validateAndExpandPageRanges(
                        pageRanges = request.pageRanges,
                        totalPages = document.numberOfPages,
                    )
                    val font = PDType1Font.HELVETICA_BOLD

                    selectedPages.forEach { pageNumber ->
                        val page = document.getPage(pageNumber - 1)
                        val pageBox = page.cropBox ?: page.mediaBox
                        val safeMarginX = pageBox.width * (request.marginPercent / 100f)
                        val safeMarginY = pageBox.height * (request.marginPercent / 100f)
                        val baseFontSize = min(pageBox.width, pageBox.height) * (request.textScalePercent / 100f)
                        val unscaledTextWidth = runCatching {
                            font.getStringWidth(watermarkText) / 1000f
                        }.getOrElse {
                            error("This watermark text contains unsupported characters for the current PDF font. Use letters, digits, and common punctuation.")
                        }
                        require(unscaledTextWidth > 0f) {
                            "Enter watermark text before saving the new PDF."
                        }

                        val availableWidth = (pageBox.width - (safeMarginX * 2f)).coerceAtLeast(pageBox.width * 0.25f)
                        val availableHeight = (pageBox.height - (safeMarginY * 2f)).coerceAtLeast(pageBox.height * 0.25f)
                        val maxTextWidth = if (request.placement == PdfWatermarkPlacement.DiagonalCenter) {
                            (((min(availableWidth, availableHeight) / DiagonalWatermarkComponent) - baseFontSize)
                                .coerceAtLeast(pageBox.width * 0.25f))
                        } else {
                            availableWidth
                        }
                        val fittedFontSize = min(
                            baseFontSize,
                            maxTextWidth / unscaledTextWidth,
                        ).coerceAtLeast(min(baseFontSize, 12f))
                        val measuredTextWidth = unscaledTextWidth * fittedFontSize
                        val layout = resolveTextWatermarkLayout(
                            pageWidth = pageBox.width,
                            pageHeight = pageBox.height,
                            textWidth = measuredTextWidth,
                            textHeight = fittedFontSize,
                            marginPercent = request.marginPercent,
                            placement = request.placement,
                        )

                        PDPageContentStream(
                            document,
                            page,
                            AppendMode.APPEND,
                            true,
                            true,
                        ).use { contentStream ->
                            val graphicsState = PDExtendedGraphicsState().apply {
                                nonStrokingAlphaConstant = request.opacityPercent / 100f
                            }

                            contentStream.saveGraphicsState()
                            contentStream.setGraphicsStateParameters(graphicsState)
                            contentStream.beginText()
                            contentStream.setFont(font, fittedFontSize)
                            contentStream.setNonStrokingColor(0.38f)
                            contentStream.setTextMatrix(
                                if (layout.rotationRadians == 0f) {
                                    Matrix.getTranslateInstance(
                                        pageBox.lowerLeftX + layout.translationX,
                                        pageBox.lowerLeftY + layout.translationY,
                                    )
                                } else {
                                    Matrix.getRotateInstance(
                                        layout.rotationRadians.toDouble(),
                                        pageBox.lowerLeftX + layout.translationX,
                                        pageBox.lowerLeftY + layout.translationY,
                                    )
                                }
                            )
                            contentStream.showText(watermarkText)
                            contentStream.endText()
                            contentStream.restoreGraphicsState()
                        }
                    }

                    document.save(request.outputFile)

                    PdfTextWatermarkResult(
                        outputFile = request.outputFile,
                        pageCount = document.numberOfPages,
                        watermarkedPageCount = selectedPages.size,
                        fileSizeBytes = request.outputFile.length(),
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and watermarking is not available yet.")
            }
        }
    }

    override suspend fun exportPdfToImages(
        request: PdfToImagesRequest,
    ): Result<PdfToImagesResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)
            require(request.outputFileNamePrefix.isNotBlank()) {
                "Choose a name for the exported images folder."
            }
            validateImageExportDpi(request.renderDpi)
            validateJpegQuality(request.jpegQualityPercent)
            prepareOutputDirectory(request.outputDirectory)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    val selectedPages = validateAndExpandPageRanges(
                        pageRanges = request.pageRanges,
                        totalPages = document.numberOfPages,
                    )
                    val renderer = PDFRenderer(document).apply {
                        isSubsamplingAllowed = true
                    }
                    val pageNumberPadding = document.numberOfPages.toString().length.coerceAtLeast(3)
                    val fileExtension = when (request.format) {
                        PdfImageExportFormat.Png -> "png"
                        PdfImageExportFormat.Jpeg -> "jpg"
                    }
                    val compressFormat = when (request.format) {
                        PdfImageExportFormat.Png -> Bitmap.CompressFormat.PNG
                        PdfImageExportFormat.Jpeg -> Bitmap.CompressFormat.JPEG
                    }
                    val quality = when (request.format) {
                        PdfImageExportFormat.Png -> 100
                        PdfImageExportFormat.Jpeg -> request.jpegQualityPercent
                    }

                    val exportedImages = selectedPages.map { pageNumber ->
                        val renderedBitmap = renderer.renderImageWithDPI(
                            pageNumber - 1,
                            request.renderDpi.toFloat(),
                        )
                        val imageWidth = renderedBitmap.width
                        val imageHeight = renderedBitmap.height
                        val outputFile = File(
                            request.outputDirectory,
                            buildPdfImageExportFileName(
                                prefix = request.outputFileNamePrefix,
                                pageNumber = pageNumber,
                                pageNumberPadding = pageNumberPadding,
                                fileExtension = fileExtension,
                            ),
                        )
                        FileOutputStream(outputFile).use { outputStream ->
                            require(renderedBitmap.compress(compressFormat, quality, outputStream)) {
                                "Couldn't write ${outputFile.name}."
                            }
                            outputStream.flush()
                        }
                        renderedBitmap.recycle()

                        PdfImageExportOutput(
                            outputFile = outputFile,
                            pageNumber = pageNumber,
                            width = imageWidth,
                            height = imageHeight,
                            fileSizeBytes = outputFile.length(),
                        )
                    }

                    PdfToImagesResult(
                        outputDirectory = request.outputDirectory,
                        pageCount = document.numberOfPages,
                        exportedImages = exportedImages,
                        format = request.format,
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and PDF-to-images export is not available yet.")
            }
        }
    }

    override suspend fun extractEmbeddedImages(
        request: ExtractEmbeddedImagesRequest,
    ): Result<ExtractEmbeddedImagesResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)
            require(request.outputFileNamePrefix.isNotBlank()) {
                "Choose a name for the extracted images folder."
            }
            prepareOutputDirectory(request.outputDirectory)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    val selectedPages = validateAndExpandPageRanges(
                        pageRanges = request.pageRanges,
                        totalPages = document.numberOfPages,
                    )
                    val visitedXObjects = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
                    val discoveredImages = LinkedHashMap<Any, EmbeddedImageExtractionRecord>()

                    selectedPages.forEach { pageNumber ->
                        val page = document.getPage(pageNumber - 1)
                        collectEmbeddedImages(
                            resources = page.resources,
                            pageNumber = pageNumber,
                            visitedXObjects = visitedXObjects,
                            discoveredImages = discoveredImages,
                        )
                    }

                    require(discoveredImages.isNotEmpty()) {
                        "No embedded images were found in the selected pages. Try another PDF or export whole pages with PDF to Images instead."
                    }

                    val exportedImages = discoveredImages.values.mapIndexed { index, record ->
                        val format = resolveEmbeddedImageExportFormat(record.image)
                        val fileExtension = when (format) {
                            PdfImageExportFormat.Png -> "png"
                            PdfImageExportFormat.Jpeg -> "jpg"
                        }
                        val outputFile = File(
                            request.outputDirectory,
                            buildEmbeddedImageExportFileName(
                                prefix = request.outputFileNamePrefix,
                                index = index + 1,
                                fileExtension = fileExtension,
                            ),
                        )
                        val bitmap = record.image.getImage()
                        val quality = when (format) {
                            PdfImageExportFormat.Png -> 100
                            PdfImageExportFormat.Jpeg -> 92
                        }
                        val compressFormat = when (format) {
                            PdfImageExportFormat.Png -> Bitmap.CompressFormat.PNG
                            PdfImageExportFormat.Jpeg -> Bitmap.CompressFormat.JPEG
                        }

                        FileOutputStream(outputFile).use { outputStream ->
                            require(bitmap.compress(compressFormat, quality, outputStream)) {
                                "Couldn't write ${outputFile.name}."
                            }
                            outputStream.flush()
                        }
                        bitmap.recycle()

                        EmbeddedImageExportOutput(
                            outputFile = outputFile,
                            pageNumbers = record.pageNumbers.toList().sorted(),
                            width = record.image.width,
                            height = record.image.height,
                            fileSizeBytes = outputFile.length(),
                            format = format,
                        )
                    }

                    ExtractEmbeddedImagesResult(
                        outputDirectory = request.outputDirectory,
                        pageCount = document.numberOfPages,
                        exportedImages = exportedImages,
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and embedded-image extraction is not available yet.")
            }
        }
    }

    override suspend fun cropPageRegions(
        request: PdfCropPageRegionRequest,
    ): Result<PdfCropPageRegionResult> = withContext(Dispatchers.IO) {
        runCatching {
            validatePdfInputFile(request.inputFile)
            require(request.outputFileNamePrefix.isNotBlank()) {
                "Choose a name for the cropped images folder."
            }
            validateImageExportDpi(request.renderDpi)
            validateJpegQuality(request.jpegQualityPercent)
            validateCropRect(request.cropRect)
            prepareOutputDirectory(request.outputDirectory)

            try {
                PDDocument.load(request.inputFile).use { document ->
                    val selectedPages = validateAndExpandPageRanges(
                        pageRanges = request.pageRanges,
                        totalPages = document.numberOfPages,
                    )
                    val renderer = PDFRenderer(document).apply {
                        isSubsamplingAllowed = true
                    }
                    val pageNumberPadding = document.numberOfPages.toString().length.coerceAtLeast(3)
                    val fileExtension = when (request.format) {
                        PdfImageExportFormat.Png -> "png"
                        PdfImageExportFormat.Jpeg -> "jpg"
                    }
                    val compressFormat = when (request.format) {
                        PdfImageExportFormat.Png -> Bitmap.CompressFormat.PNG
                        PdfImageExportFormat.Jpeg -> Bitmap.CompressFormat.JPEG
                    }
                    val quality = when (request.format) {
                        PdfImageExportFormat.Png -> 100
                        PdfImageExportFormat.Jpeg -> request.jpegQualityPercent
                    }

                    val exportedImages = selectedPages.map { pageNumber ->
                        val renderedBitmap = renderer.renderImageWithDPI(
                            pageNumber - 1,
                            request.renderDpi.toFloat(),
                        )
                        val cropBounds = resolveCropPixelBounds(
                            bitmapWidth = renderedBitmap.width,
                            bitmapHeight = renderedBitmap.height,
                            cropRect = request.cropRect,
                        )
                        val croppedBitmap = Bitmap.createBitmap(
                            renderedBitmap,
                            cropBounds.left,
                            cropBounds.top,
                            cropBounds.width,
                            cropBounds.height,
                        )
                        renderedBitmap.recycle()

                        val outputFile = File(
                            request.outputDirectory,
                            buildPdfImageExportFileName(
                                prefix = request.outputFileNamePrefix,
                                pageNumber = pageNumber,
                                pageNumberPadding = pageNumberPadding,
                                fileExtension = fileExtension,
                            ),
                        )

                        FileOutputStream(outputFile).use { outputStream ->
                            require(croppedBitmap.compress(compressFormat, quality, outputStream)) {
                                "Couldn't write ${outputFile.name}."
                            }
                            outputStream.flush()
                        }

                        val croppedWidth = croppedBitmap.width
                        val croppedHeight = croppedBitmap.height
                        croppedBitmap.recycle()

                        PdfImageExportOutput(
                            outputFile = outputFile,
                            pageNumber = pageNumber,
                            width = croppedWidth,
                            height = croppedHeight,
                            fileSizeBytes = outputFile.length(),
                        )
                    }

                    PdfCropPageRegionResult(
                        outputDirectory = request.outputDirectory,
                        pageCount = document.numberOfPages,
                        exportedImages = exportedImages,
                        format = request.format,
                        cropRect = request.cropRect,
                    )
                }
            } catch (_: InvalidPasswordException) {
                error("${request.inputFile.name} is password protected and page-region export is not available yet.")
            }
        }
    }

    private fun collectEmbeddedImages(
        resources: PDResources?,
        pageNumber: Int,
        visitedXObjects: MutableSet<Any>,
        discoveredImages: MutableMap<Any, EmbeddedImageExtractionRecord>,
    ) {
        if (resources == null) return

        resources.xObjectNames.forEach { name ->
            val xObject = runCatching { resources.getXObject(name) }.getOrNull() ?: return@forEach
            val xObjectKey = xObject.cosObject

            when (xObject) {
                is PDImageXObject -> {
                    val record = discoveredImages.getOrPut(xObjectKey) {
                        EmbeddedImageExtractionRecord(image = xObject)
                    }
                    record.pageNumbers.add(pageNumber)
                }

                is PDFormXObject -> {
                    if (!visitedXObjects.add(xObjectKey)) return@forEach
                    collectEmbeddedImages(
                        resources = xObject.resources,
                        pageNumber = pageNumber,
                        visitedXObjects = visitedXObjects,
                        discoveredImages = discoveredImages,
                    )
                }
            }
        }
    }

    private fun resolveEmbeddedImageExportFormat(image: PDImageXObject): PdfImageExportFormat {
        val suffix = image.suffix?.lowercase()
        return if (suffix == "jpg" || suffix == "jpeg") {
            PdfImageExportFormat.Jpeg
        } else {
            PdfImageExportFormat.Png
        }
    }

    private fun validatePdfInputFile(file: File) {
        validatePdfInputPresence(file)
        try {
            PDDocument.load(file).use { }
        } catch (_: InvalidPasswordException) {
            error("${file.name} is password protected and cannot be processed yet.")
        }
    }

    private fun validatePdfInputPresence(file: File) {
        require(file.exists()) {
            "${file.name} is no longer available. Re-import it and try again."
        }
    }

    private fun prepareOutputFile(outputFile: File) {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists() && !outputFile.delete()) {
            error("Could not overwrite ${outputFile.name}.")
        }
    }

    private fun prepareOutputDirectory(outputDirectory: File) {
        if (outputDirectory.exists() && !outputDirectory.deleteRecursively()) {
            error("Could not overwrite ${outputDirectory.name}.")
        }
        require(outputDirectory.mkdirs() || outputDirectory.exists()) {
            "Could not create ${outputDirectory.name}."
        }
    }

    private fun validateAndExpandPageRanges(
        pageRanges: List<PdfPageRange>,
        totalPages: Int,
    ): List<Int> {
        require(pageRanges.isNotEmpty()) {
            "Select at least one page or page range."
        }

        val claimedPages = linkedSetOf<Int>()
        pageRanges.forEach { pageRange ->
            validatePageRange(pageRange, totalPages)
            (pageRange.startPage..pageRange.endPage).forEach { pageNumber ->
                require(claimedPages.add(pageNumber)) {
                    "Page $pageNumber is included more than once. Remove overlapping ranges and try again."
                }
            }
        }
        return claimedPages.toList().sorted()
    }

    private fun validateRotationDegrees(rotationDegrees: Int) {
        require(rotationDegrees in setOf(90, 180, 270)) {
            "Choose 90°, 180°, or 270° rotation."
        }
    }

    private fun validateWatermarkScale(textScalePercent: Int) {
        require(textScalePercent in 6..24) {
            "Watermark size must stay between 6% and 24% of the shorter page edge."
        }
    }

    private fun validateWatermarkMargin(marginPercent: Int) {
        require(marginPercent in 0..15) {
            "Watermark margin must stay between 0% and 15% of the page size."
        }
    }

    private fun validateWatermarkOpacity(opacityPercent: Int) {
        require(opacityPercent in 5..60) {
            "Watermark opacity must stay between 5% and 60%."
        }
    }

    private fun validateImageExportDpi(renderDpi: Int) {
        require(renderDpi in 96..300) {
            "Image export quality must stay between 96 DPI and 300 DPI."
        }
    }

    private fun validateJpegQuality(jpegQualityPercent: Int) {
        require(jpegQualityPercent in 60..100) {
            "JPEG quality must stay between 60% and 100%."
        }
    }

    private fun validateCropRect(cropRect: PdfNormalizedCropRect) {
        require(cropRect.leftFraction in 0f..1f) { "Crop left edge must stay between 0% and 100%." }
        require(cropRect.topFraction in 0f..1f) { "Crop top edge must stay between 0% and 100%." }
        require(cropRect.rightFraction in 0f..1f) { "Crop right edge must stay between 0% and 100%." }
        require(cropRect.bottomFraction in 0f..1f) { "Crop bottom edge must stay between 0% and 100%." }
        require(cropRect.rightFraction > cropRect.leftFraction) {
            "Crop width must be greater than zero."
        }
        require(cropRect.bottomFraction > cropRect.topFraction) {
            "Crop height must be greater than zero."
        }
    }

    private fun normalizePageRotation(rotation: Int): Int {
        val normalized = rotation % 360
        return if (normalized < 0) normalized + 360 else normalized
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

internal data class VisibleSignatureRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

internal data class TextWatermarkLayout(
    val translationX: Float,
    val translationY: Float,
    val rotationRadians: Float,
)

private const val DiagonalWatermarkRotationDegrees = 45.0
private const val DiagonalWatermarkComponent = 0.70710677f

internal fun resolveVisibleSignatureRect(
    pageWidth: Float,
    pageHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
    widthPercent: Int,
    marginPercent: Int,
    placement: VisibleSignaturePlacement,
): VisibleSignatureRect {
    require(pageWidth > 0f && pageHeight > 0f) { "Page dimensions must be positive." }
    require(imageWidth > 0f && imageHeight > 0f) { "Signature image dimensions must be positive." }
    require(widthPercent in 5..60) { "Signature size must stay between 5% and 60% of the page width." }
    require(marginPercent in 0..20) { "Signature margin must stay between 0% and 20% of the page width." }

    val width = pageWidth * (widthPercent / 100f)
    val height = width * (imageHeight / imageWidth)
    val safeMarginX = pageWidth * (marginPercent / 100f)
    val safeMarginY = pageHeight * (marginPercent / 100f)
    val maxX = (pageWidth - width).coerceAtLeast(0f)
    val maxY = (pageHeight - height).coerceAtLeast(0f)

    val rawX = when (placement) {
        VisibleSignaturePlacement.TopLeft,
        VisibleSignaturePlacement.BottomLeft,
        -> safeMarginX

        VisibleSignaturePlacement.TopRight,
        VisibleSignaturePlacement.BottomRight,
        -> pageWidth - width - safeMarginX

        VisibleSignaturePlacement.Center -> (pageWidth - width) / 2f
    }
    val rawY = when (placement) {
        VisibleSignaturePlacement.BottomLeft,
        VisibleSignaturePlacement.BottomRight,
        -> safeMarginY

        VisibleSignaturePlacement.TopLeft,
        VisibleSignaturePlacement.TopRight,
        -> pageHeight - height - safeMarginY

        VisibleSignaturePlacement.Center -> (pageHeight - height) / 2f
    }

    return VisibleSignatureRect(
        x = rawX.coerceIn(0f, maxX),
        y = rawY.coerceIn(0f, maxY),
        width = width,
        height = height.coerceAtMost(pageHeight),
    )
}

internal fun resolveTextWatermarkLayout(
    pageWidth: Float,
    pageHeight: Float,
    textWidth: Float,
    textHeight: Float,
    marginPercent: Int,
    placement: PdfWatermarkPlacement,
): TextWatermarkLayout {
    require(pageWidth > 0f && pageHeight > 0f) { "Page dimensions must be positive." }
    require(textWidth > 0f && textHeight > 0f) { "Watermark text dimensions must be positive." }
    require(marginPercent in 0..15) { "Watermark margin must stay between 0% and 15% of the page size." }

    val safeMarginX = pageWidth * (marginPercent / 100f)
    val safeMarginY = pageHeight * (marginPercent / 100f)
    val maxX = (pageWidth - textWidth).coerceAtLeast(0f)
    val maxY = (pageHeight - textHeight).coerceAtLeast(0f)

    if (placement == PdfWatermarkPlacement.DiagonalCenter) {
        val rotationRadians = Math.toRadians(DiagonalWatermarkRotationDegrees).toFloat()
        val cosValue = cos(rotationRadians)
        val sinValue = sin(rotationRadians)
        val rotatedCorners = listOf(
            0f to 0f,
            textWidth to 0f,
            0f to textHeight,
            textWidth to textHeight,
        ).map { (x, y) ->
            val rotatedX = (x * cosValue) - (y * sinValue)
            val rotatedY = (x * sinValue) + (y * cosValue)
            rotatedX to rotatedY
        }
        val minRotatedX = rotatedCorners.minOf { it.first }
        val maxRotatedX = rotatedCorners.maxOf { it.first }
        val minRotatedY = rotatedCorners.minOf { it.second }
        val maxRotatedY = rotatedCorners.maxOf { it.second }
        val centeredTranslationX = (pageWidth / 2f) - ((minRotatedX + maxRotatedX) / 2f)
        val centeredTranslationY = (pageHeight / 2f) - ((minRotatedY + maxRotatedY) / 2f)
        return TextWatermarkLayout(
            translationX = centeredTranslationX,
            translationY = centeredTranslationY,
            rotationRadians = rotationRadians,
        )
    }

    val rawX = when (placement) {
        PdfWatermarkPlacement.TopLeft,
        PdfWatermarkPlacement.BottomLeft,
        -> safeMarginX

        PdfWatermarkPlacement.TopRight,
        PdfWatermarkPlacement.BottomRight,
        -> pageWidth - textWidth - safeMarginX

        PdfWatermarkPlacement.Center -> (pageWidth - textWidth) / 2f
    }
    val rawY = when (placement) {
        PdfWatermarkPlacement.BottomLeft,
        PdfWatermarkPlacement.BottomRight,
        -> safeMarginY + (textHeight * 0.15f)

        PdfWatermarkPlacement.TopLeft,
        PdfWatermarkPlacement.TopRight,
        -> pageHeight - textHeight - safeMarginY

        PdfWatermarkPlacement.Center -> (pageHeight - textHeight) / 2f
    }

    return TextWatermarkLayout(
        translationX = rawX.coerceIn(0f, maxX),
        translationY = rawY.coerceIn(0f, maxY),
        rotationRadians = 0f,
    )
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

private val ImageExportInvalidFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
private val ImageExportExtraWhitespace = Regex("\\s+")

private fun buildPdfImageExportFileName(
    prefix: String,
    pageNumber: Int,
    pageNumberPadding: Int,
    fileExtension: String,
): String {
    val sanitizedPrefix = prefix
        .replace(ImageExportInvalidFileNameCharacters, "_")
        .replace(ImageExportExtraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { "exported_page" }
        .take(48)

    return "${sanitizedPrefix}_page_${pageNumber.toString().padStart(pageNumberPadding, '0')}.$fileExtension"
}

private fun buildEmbeddedImageExportFileName(
    prefix: String,
    index: Int,
    fileExtension: String,
): String {
    val sanitizedPrefix = prefix
        .replace(ImageExportInvalidFileNameCharacters, "_")
        .replace(ImageExportExtraWhitespace, "_")
        .replace(Regex("_+"), "_")
        .trim('_')
        .ifBlank { "embedded_image" }
        .take(48)

    return "${sanitizedPrefix}_embedded_${index.toString().padStart(3, '0')}.$fileExtension"
}

private data class EmbeddedImageExtractionRecord(
    val image: PDImageXObject,
    val pageNumbers: MutableSet<Int> = linkedSetOf(),
)

private data class CropPixelBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

private fun resolveCropPixelBounds(
    bitmapWidth: Int,
    bitmapHeight: Int,
    cropRect: PdfNormalizedCropRect,
): CropPixelBounds {
    require(bitmapWidth > 0 && bitmapHeight > 0) { "Rendered page dimensions must be positive." }

    val left = (bitmapWidth * cropRect.leftFraction).toInt().coerceIn(0, bitmapWidth - 1)
    val top = (bitmapHeight * cropRect.topFraction).toInt().coerceIn(0, bitmapHeight - 1)
    val right = kotlin.math.ceil(bitmapWidth * cropRect.rightFraction.toDouble()).toInt().coerceIn(left + 1, bitmapWidth)
    val bottom = kotlin.math.ceil(bitmapHeight * cropRect.bottomFraction.toDouble()).toInt().coerceIn(top + 1, bitmapHeight)

    return CropPixelBounds(
        left = left,
        top = top,
        width = (right - left).coerceAtLeast(1),
        height = (bottom - top).coerceAtLeast(1),
    )
}

