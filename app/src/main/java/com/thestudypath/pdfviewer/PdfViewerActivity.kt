package com.thestudypath.pdfviewer

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import com.thestudypath.pdf.PdfActivityCompose
import com.thestudypath.pdf.PdfConfig
import com.thestudypath.pdf.interfaces.PdfAnnotationSaver
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.catalog.toDocumentItem
import com.thestudypath.pdfviewer.catalog.toRecentFile
import java.io.File

/**
 * Concrete PdfActivityCompose subclass used by the app module.
 * Copies the picked file into internal storage, then opens it.
 */
class PdfViewerActivity : PdfActivityCompose() {

    override fun providePdfConfig(): PdfConfig {
        val fileName = intent.getStringExtra(EXTRA_INTERNAL_FILE_NAME) ?: ""
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: fileName
        return PdfConfig(
            fileName = fileName,
            displayName = displayName,
            isDownloadable = true,
            showNightModeToggle = true,
            showSearchButton = true,
            showEditButtons = true,
            showSaveAsOption = true,
            showDownloadOption = false,
            showOrientationOption = true,
            showFullscreenButton = true,
        )
    }

    override fun provideAnnotationSaver(): PdfAnnotationSaver {
        return object : PdfAnnotationSaver {
            override suspend fun save(
                workingFile: File,
                originalFile: File,
                password: String?
            ): Result<Unit> {
                return try {
                    workingFile.copyTo(originalFile, overwrite = true)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    companion object {
        const val EXTRA_INTERNAL_FILE_NAME = "internal_file_name"
        const val EXTRA_DISPLAY_NAME = "display_name"

        fun copyAndLaunch(
            context: Context,
            uri: Uri,
            displayName: String,
            fileSize: Long,
        ): RecentFile? {
            return try {
                val internalName = "pdf_${System.currentTimeMillis()}.pdf"
                val dest = File(context.filesDir, internalName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }

                val pageCount = try {
                    val fd = ParcelFileDescriptor.open(dest, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    val count = renderer.pageCount
                    renderer.close()
                    count
                } catch (_: Exception) { 0 }

                val recent = RecentFile(
                    fileName = internalName,
                    displayName = displayName,
                    filePath = dest.absolutePath,
                    fileSize = fileSize,
                    lastOpenedAt = System.currentTimeMillis(),
                    pageCount = pageCount,
                )

                context.startActivity(Intent(context, PdfViewerActivity::class.java).apply {
                    putExtra(EXTRA_INTERNAL_FILE_NAME, internalName)
                    putExtra(EXTRA_DISPLAY_NAME, displayName)
                })

                recent
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                null
            }
        }

        fun copyAndLaunchDocument(
            context: Context,
            uri: Uri,
            displayName: String,
            fileSize: Long,
        ): DocumentItem? = copyAndLaunch(context, uri, displayName, fileSize)?.toDocumentItem()

        fun launchExisting(context: Context, recentFile: RecentFile) {
            context.startActivity(Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(EXTRA_INTERNAL_FILE_NAME, recentFile.fileName)
                putExtra(EXTRA_DISPLAY_NAME, recentFile.displayName)
            })
        }

        fun launchExisting(context: Context, document: DocumentItem) {
            launchExisting(context, document.toRecentFile())
        }
    }
}

