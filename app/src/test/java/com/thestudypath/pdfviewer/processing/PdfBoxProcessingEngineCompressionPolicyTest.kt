package com.thestudypath.pdfviewer.processing

import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceGray
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfBoxProcessingEngineCompressionPolicyTest {

    @Test
    fun shouldRecompressImage_isFalse_whenImageIsWithinTargetSize() {
        val shouldRecompress = shouldRecompressImage(
            support = PdfImageCompressionSupport(
                hasSoftMask = false,
                hasExplicitMask = false,
                hasColorKeyMask = false,
                isStencil = false,
                colorSpaceName = PDDeviceRGB.INSTANCE.name,
                suffix = "jpg",
                width = 1200,
                height = 800,
            ),
            maxDimension = 1700,
        )

        assertEquals(false, shouldRecompress)
    }

    @Test
    fun shouldRecompressImage_isTrue_whenImageExceedsTargetSize() {
        val shouldRecompress = shouldRecompressImage(
            support = PdfImageCompressionSupport(
                hasSoftMask = false,
                hasExplicitMask = false,
                hasColorKeyMask = false,
                isStencil = false,
                colorSpaceName = PDDeviceRGB.INSTANCE.name,
                suffix = "jpg",
                width = 2400,
                height = 1600,
            ),
            maxDimension = 1700,
        )

        assertEquals(true, shouldRecompress)
    }

    @Test
    fun shouldRewriteCompressedPdf_isFalse_whenNothingChanged_andMetadataIsKept() {
        val shouldRewrite = shouldRewriteCompressedPdf(
            changedResources = false,
            keepMetadata = true,
        )

        assertEquals(false, shouldRewrite)
    }

    @Test
    fun shouldRewriteCompressedPdf_isTrue_whenMetadataMustBeStripped() {
        val shouldRewrite = shouldRewriteCompressedPdf(
            changedResources = false,
            keepMetadata = false,
        )

        assertEquals(true, shouldRewrite)
    }

    @Test
    fun shouldRewriteCompressedPdf_isTrue_whenImagesWereRecompressed() {
        val shouldRewrite = shouldRewriteCompressedPdf(
            changedResources = true,
            keepMetadata = true,
        )

        assertEquals(true, shouldRewrite)
    }

    @Test
    fun chooseImageCompressionStrategy_usesJpeg_forOpaqueDeviceRgbImages() {
        val strategy = chooseImageCompressionStrategy(
            PdfImageCompressionSupport(
                hasSoftMask = false,
                hasExplicitMask = false,
                hasColorKeyMask = false,
                isStencil = false,
                colorSpaceName = PDDeviceRGB.INSTANCE.name,
                suffix = "jpg",
                width = 2400,
                height = 1600,
            ),
        )

        assertEquals(PdfImageCompressionStrategy.Jpeg, strategy)
    }

    @Test
    fun chooseImageCompressionStrategy_skips_whenTransparencyIsPresent() {
        val strategy = chooseImageCompressionStrategy(
            PdfImageCompressionSupport(
                hasSoftMask = true,
                hasExplicitMask = false,
                hasColorKeyMask = false,
                isStencil = false,
                colorSpaceName = PDDeviceGray.INSTANCE.name,
                suffix = "png",
                width = 2400,
                height = 1600,
            ),
        )

        assertEquals(PdfImageCompressionStrategy.Skip, strategy)
    }

    @Test
    fun chooseImageCompressionStrategy_skipsUnsupportedColorSpaces() {
        val strategy = chooseImageCompressionStrategy(
            PdfImageCompressionSupport(
                hasSoftMask = false,
                hasExplicitMask = false,
                hasColorKeyMask = false,
                isStencil = false,
                colorSpaceName = "DeviceCMYK",
                suffix = "jpg",
                width = 2400,
                height = 1600,
            ),
        )

        assertEquals(PdfImageCompressionStrategy.Skip, strategy)
    }

    @Test
    fun chooseImageCompressionStrategy_skipsProblematicEncodedFormats() {
        val strategy = chooseImageCompressionStrategy(
            PdfImageCompressionSupport(
                hasSoftMask = false,
                hasExplicitMask = false,
                hasColorKeyMask = false,
                isStencil = false,
                colorSpaceName = PDDeviceRGB.INSTANCE.name,
                suffix = "jpx",
                width = 2400,
                height = 1600,
            ),
        )

        assertEquals(PdfImageCompressionStrategy.Skip, strategy)
    }
}

