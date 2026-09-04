package com.thestudypath.pdfviewer.ui.results

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.thestudypath.pdfviewer.catalog.DocumentItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val invalidExportFileNameCharacters = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

internal class FileExportLauncher internal constructor(
    private val launchImpl: (File, String, String) -> Unit,
) {
    fun launch(
        file: File,
        mimeType: String,
        displayName: String,
    ) {
        launchImpl(file, mimeType, displayName)
    }
}

@Composable
internal fun rememberFileExportLauncher(
    onMessage: (String) -> Unit,
): FileExportLauncher {
    val context = LocalContext.current
    val appContext = context.applicationContext
    var pendingExport by remember { mutableStateOf<PendingFileExport?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val exportRequest = pendingExport ?: return@rememberLauncherForActivityResult
        pendingExport = null
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val destinationUri = result.data?.data ?: run {
            onMessage("Could not access the chosen save location.")
            return@rememberLauncherForActivityResult
        }

        launchExportCopy(
            context = appContext,
            sourceFile = exportRequest.file,
            destinationUri = destinationUri,
            displayName = buildSuggestedExportFileName(
                displayName = exportRequest.displayName,
                sourceFileName = exportRequest.file.name,
            ),
            onMessage = onMessage,
        )
    }

    return remember(launcher, appContext, onMessage) {
        FileExportLauncher { file, mimeType, displayName ->
            if (!file.exists()) {
                onMessage("${file.name} is no longer available.")
                return@FileExportLauncher
            }

            pendingExport = PendingFileExport(
                file = file,
                mimeType = mimeType,
                displayName = displayName,
            )
            launcher.launch(
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mimeType
                    putExtra(
                        Intent.EXTRA_TITLE,
                        buildSuggestedExportFileName(
                            displayName = displayName,
                            sourceFileName = file.name,
                        ),
                    )
                },
            )
        }
    }
}

internal fun buildSuggestedExportFileName(
    displayName: String,
    sourceFileName: String,
): String {
    val normalizedSource = sourceFileName.trim().ifBlank { "exported-file" }
    val sanitizedDisplayName = displayName
        .trim()
        .replace(invalidExportFileNameCharacters, " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { normalizedSource }

    val sourceExtension = normalizedSource.substringAfterLast('.', missingDelimiterValue = "")
    val hasDisplayExtension = sanitizedDisplayName.substringAfterLast('.', missingDelimiterValue = "")
        .isNotBlank()

    return if (hasDisplayExtension || sourceExtension.isBlank()) {
        sanitizedDisplayName
    } else {
        "$sanitizedDisplayName.$sourceExtension"
    }
}

internal fun openFileExternally(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String,
) {
    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(openIntent, chooserTitle))
}

internal fun shareFile(
    context: Context,
    file: File,
    mimeType: String,
    chooserTitle: String,
    title: String,
) {
    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}

internal fun shareFiles(
    context: Context,
    files: List<File>,
    mimeType: String,
    chooserTitle: String,
    title: String,
) {
    val existingFiles = files.filter(File::exists)
    if (existingFiles.isEmpty()) return

    val uris = ArrayList(existingFiles.map { file ->
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    })

    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = mimeType
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}

internal fun sharePdfDocument(
    context: Context,
    document: DocumentItem,
    chooserTitle: String = "Share PDF",
) {
    shareFile(
        context = context,
        file = File(document.filePath),
        mimeType = "application/pdf",
        chooserTitle = chooserTitle,
        title = document.displayName,
    )
}

private fun launchExportCopy(
    context: Context,
    sourceFile: File,
    destinationUri: Uri,
    displayName: String,
    onMessage: (String) -> Unit,
) {
    CoroutineScope(Dispatchers.Main.immediate).launch {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                sourceFile.inputStream().use { inputStream ->
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    } ?: error("Could not write to the chosen save location.")
                }
            }
        }

        result.onSuccess {
            onMessage("Saved copy as $displayName.")
        }.onFailure { throwable ->
            onMessage(throwable.message ?: "Failed to save a copy of $displayName.")
        }
    }
}

private data class PendingFileExport(
    val file: File,
    val mimeType: String,
    val displayName: String,
)


