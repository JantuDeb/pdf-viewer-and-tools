package com.thestudypath.pdfviewer.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thestudypath.pdfviewer.catalog.DocumentCatalogUiState
import com.thestudypath.pdfviewer.catalog.DocumentCatalogViewModel
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.processing.PdfCropPageRegionResult
import com.thestudypath.pdfviewer.processing.ExtractEmbeddedImagesResult
import com.thestudypath.pdfviewer.processing.PdfToImagesResult
import com.thestudypath.pdfviewer.ui.tools.crop.CropPageRegionViewModel
import com.thestudypath.pdfviewer.ui.tools.crop.CropPageRegionWorkspaceCard
import com.thestudypath.pdfviewer.ui.documents.DocumentPickerSheet
import com.thestudypath.pdfviewer.ui.documents.DocumentSelectionMode
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.results.ResultActionButtons
import com.thestudypath.pdfviewer.ui.results.openFileExternally
import com.thestudypath.pdfviewer.ui.results.rememberFileExportLauncher
import com.thestudypath.pdfviewer.ui.results.shareFile
import com.thestudypath.pdfviewer.ui.results.shareFiles
import com.thestudypath.pdfviewer.ui.results.sharePdfDocument
import com.thestudypath.pdfviewer.ui.tools.compress.CompressPdfViewModel
import com.thestudypath.pdfviewer.ui.tools.compress.CompressPdfWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.compress.formatFileSize
import com.thestudypath.pdfviewer.ui.tools.compress.profileLabel
import com.thestudypath.pdfviewer.ui.tools.embeddedimages.ExtractEmbeddedImagesViewModel
import com.thestudypath.pdfviewer.ui.tools.embeddedimages.ExtractEmbeddedImagesWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.embeddedimages.imageMimeTypeFor
import com.thestudypath.pdfviewer.ui.tools.images.ImagesToPdfViewModel
import com.thestudypath.pdfviewer.ui.tools.images.ImagesToPdfWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.merge.MergePdfUiState
import com.thestudypath.pdfviewer.ui.tools.merge.MergePdfViewModel
import com.thestudypath.pdfviewer.ui.tools.password.AddPasswordViewModel
import com.thestudypath.pdfviewer.ui.tools.password.AddPasswordWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.password.RemovePasswordViewModel
import com.thestudypath.pdfviewer.ui.tools.password.RemovePasswordWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.pdfimages.PdfToImagesViewModel
import com.thestudypath.pdfviewer.ui.tools.pdfimages.PdfToImagesWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.pdfimages.mimeType
import com.thestudypath.pdfviewer.ui.tools.pages.DeletePagesViewModel
import com.thestudypath.pdfviewer.ui.tools.pages.DeletePagesWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.pages.ExtractPagesViewModel
import com.thestudypath.pdfviewer.ui.tools.pages.ExtractPagesWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.pages.RotatePagesViewModel
import com.thestudypath.pdfviewer.ui.tools.pages.RotatePagesWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.split.SplitPdfViewModel
import com.thestudypath.pdfviewer.ui.tools.split.SplitPdfWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.text.ExtractTextViewModel
import com.thestudypath.pdfviewer.ui.tools.text.ExtractTextWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.watermark.WatermarkPdfViewModel
import com.thestudypath.pdfviewer.ui.tools.watermark.WatermarkPdfWorkspaceCard
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ToolDetailRoute(
    toolId: String,
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    when (toolId) {
        ToolIds.CompressPdf -> CompressPdfToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )
        ToolIds.ImagesToPdf -> ImagesToPdfToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
        )
        ToolIds.PdfToImages -> PdfToImagesToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onImportDocument = onImportDocument,
        )
        ToolIds.ExtractEmbeddedImages -> ExtractEmbeddedImagesToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onImportDocument = onImportDocument,
        )
        ToolIds.CropPageRegion -> CropPageRegionToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onImportDocument = onImportDocument,
        )
        ToolIds.MergePdf -> MergePdfToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )
        ToolIds.SplitPdf -> SplitPdfToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )
        ToolIds.ExtractText -> ExtractTextToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onImportDocument = onImportDocument,
        )
        ToolIds.AddPassword -> AddPasswordToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onImportDocument = onImportDocument,
        )
        ToolIds.RemovePassword -> RemovePasswordToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )
        ToolIds.WatermarkPdf -> WatermarkPdfToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )
        ToolIds.RotatePages -> RotatePagesToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )
        ToolIds.ExtractPages -> ExtractPagesToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )
        ToolIds.DeletePages -> DeletePagesToolRoute(
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
            onOpenDocument = onOpenDocument,
            onImportDocument = onImportDocument,
        )

        else -> UnknownToolRoute(onNavigateBack = onNavigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompressPdfToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val compressViewModel: CompressPdfViewModel = viewModel(
        factory = remember(appContext) { CompressPdfViewModel.factory(appContext) },
    )
    val compressUiState by compressViewModel.uiState.collectAsStateWithLifecycle()
    var compressPickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(compressUiState.showDocumentPicker, compressUiState.selectedDocument?.id) {
        if (compressUiState.showDocumentPicker) {
            compressPickerSelection = compressUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(compressUiState.errorMessage) {
        compressUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            compressViewModel.clearError()
        }
    }

    LaunchedEffect(compressUiState.resultDocument?.id) {
        compressUiState.resultDocument?.let { document ->
            val summary = compressUiState.compressionSummary
            if (summary != null) {
                snackbarHostState.showSnackbar(
                    "${profileLabel(summary.appliedProfile)} compression: ${formatFileSize(summary.compressedSizeBytes)} (${summary.savingsPercent}% saved)."
                )
            } else {
                snackbarHostState.showSnackbar("Compressed ${document.displayName}.")
            }
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.CompressPdf)?.title ?: "Compress PDF",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CompressPdfWorkspaceCard(
                    uiState = compressUiState,
                    onChooseDocument = compressViewModel::showDocumentPicker,
                    onProfileSelected = compressViewModel::updateProfile,
                    onKeepMetadataChanged = compressViewModel::updateKeepMetadata,
                    onOutputFileNameChange = compressViewModel::updateOutputFileName,
                    onCompress = {
                        compressViewModel.compressSelectedDocument(catalogViewModel::addOrUpdate)
                    },
                    onOpenResult = onOpenDocument,
                    onShareResult = { document -> sharePdfDocument(context, document) },
                    onSaveCopyResult = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (compressUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to compress",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = compressPickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document ->
                compressPickerSelection = document.id
            },
            onDismissRequest = compressViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                compressPickerSelection?.let { selectedId ->
                    compressViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, pick a compression profile, and create a smaller copy.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagesToPdfToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val imagesToPdfViewModel: ImagesToPdfViewModel = viewModel(
        factory = remember(appContext) { ImagesToPdfViewModel.factory(appContext) },
    )
    val imagesToPdfUiState by imagesToPdfViewModel.uiState.collectAsStateWithLifecycle()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        imagesToPdfViewModel.importPickedImages(uris)
    }

    LaunchedEffect(imagesToPdfUiState.errorMessage) {
        imagesToPdfUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            imagesToPdfViewModel.clearError()
        }
    }

    LaunchedEffect(imagesToPdfUiState.resultDocument?.id) {
        imagesToPdfUiState.resultDocument?.let { document ->
            snackbarHostState.showSnackbar(
                "Created ${document.displayName} from ${imagesToPdfUiState.selectedImages.size} image(s)."
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.ImagesToPdf)?.title ?: "Images to PDF",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ImagesToPdfWorkspaceCard(
                    uiState = imagesToPdfUiState,
                    onPickImages = { imagePicker.launch("image/*") },
                    onOutputFileNameChange = imagesToPdfViewModel::updateOutputFileName,
                    onMoveImageUp = imagesToPdfViewModel::moveImageUp,
                    onMoveImageDown = imagesToPdfViewModel::moveImageDown,
                    onRemoveImage = imagesToPdfViewModel::removeImage,
                    onCreatePdf = {
                        imagesToPdfViewModel.createPdf(catalogViewModel::addOrUpdate)
                    },
                    onOpenResult = onOpenDocument,
                    onShareResult = { document -> sharePdfDocument(context, document) },
                    onSaveCopyResult = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MergePdfToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val mergeViewModel: MergePdfViewModel = viewModel(
        factory = remember(appContext) { MergePdfViewModel.factory(appContext) },
    )
    val mergeUiState by mergeViewModel.uiState.collectAsStateWithLifecycle()
    var mergePickerSelection by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(mergeUiState.showDocumentPicker, mergeUiState.selectedDocuments) {
        if (mergeUiState.showDocumentPicker) {
            mergePickerSelection = mergeUiState.selectedDocuments.map { it.id }.toSet()
        }
    }

    LaunchedEffect(mergeUiState.errorMessage) {
        mergeUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            mergeViewModel.clearError()
        }
    }

    LaunchedEffect(mergeUiState.resultDocument?.id) {
        mergeUiState.resultDocument?.let { document ->
            catalogViewModel.addOrUpdate(document)
            snackbarHostState.showSnackbar(
                "Merged ${mergeUiState.selectedDocuments.size} PDFs into ${document.displayName}."
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.MergePdf)?.title ?: "Merge PDFs",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MergePdfWorkspaceCard(
                    uiState = mergeUiState,
                    onChooseDocuments = mergeViewModel::showDocumentPicker,
                    onOutputFileNameChange = mergeViewModel::updateOutputFileName,
                    onMoveDocumentUp = mergeViewModel::moveDocumentUp,
                    onMoveDocumentDown = mergeViewModel::moveDocumentDown,
                    onRemoveDocument = mergeViewModel::removeDocument,
                    onMerge = { mergeViewModel.mergeSelectedDocuments { } },
                    onOpenMergedDocument = onOpenDocument,
                    onShareMergedDocument = { document -> sharePdfDocument(context, document) },
                    onSaveCopyMergedDocument = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (mergeUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Select PDFs to merge",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Multiple,
            selectedDocumentIds = mergePickerSelection,
            onDocumentToggle = { document ->
                mergePickerSelection = if (mergePickerSelection.contains(document.id)) {
                    mergePickerSelection - document.id
                } else {
                    mergePickerSelection + document.id
                }
            },
            onDismissRequest = mergeViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                mergeViewModel.confirmDocumentSelection(
                    allDocuments = availableDocuments,
                    selectedDocumentIds = mergePickerSelection,
                )
            },
            confirmLabel = "Use selection",
            helperText = "Select 2 or more PDFs, then order them before running the merge.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitPdfToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val splitViewModel: SplitPdfViewModel = viewModel(
        factory = remember(appContext) { SplitPdfViewModel.factory(appContext) },
    )
    val splitUiState by splitViewModel.uiState.collectAsStateWithLifecycle()
    var splitPickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(splitUiState.showDocumentPicker, splitUiState.selectedDocument?.id) {
        if (splitUiState.showDocumentPicker) {
            splitPickerSelection = splitUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(splitUiState.errorMessage) {
        splitUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            splitViewModel.clearError()
        }
    }

    LaunchedEffect(splitUiState.resultDocuments.map { it.id }) {
        if (splitUiState.resultDocuments.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                "Created ${splitUiState.resultDocuments.size} split PDF${if (splitUiState.resultDocuments.size == 1) "" else "s"}."
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.SplitPdf)?.title ?: "Split PDF",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SplitPdfWorkspaceCard(
                    uiState = splitUiState,
                    onChooseDocument = splitViewModel::showDocumentPicker,
                    onSplitModeChange = splitViewModel::updateSplitMode,
                    onPagesPerSplitChange = splitViewModel::updatePagesPerSplitInput,
                    onCustomRangesChange = splitViewModel::updateCustomRangesInput,
                    onOutputBaseNameChange = splitViewModel::updateOutputBaseName,
                    onSplit = {
                        splitViewModel.splitSelectedDocument { documents ->
                            documents.forEach(catalogViewModel::addOrUpdate)
                        }
                    },
                    onOpenSplitDocument = onOpenDocument,
                    onShareSplitDocument = { document -> sharePdfDocument(context, document) },
                    onSaveCopySplitDocument = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (splitUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to split",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = splitPickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document ->
                splitPickerSelection = document.id
            },
            onDismissRequest = splitViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                splitPickerSelection?.let { selectedId ->
                    splitViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, then split it by every N pages or by custom ranges.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractTextToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val extractTextViewModel: ExtractTextViewModel = viewModel(
        factory = remember(appContext) { ExtractTextViewModel.factory(appContext) },
    )
    val extractTextUiState by extractTextViewModel.uiState.collectAsStateWithLifecycle()
    var extractTextPickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(extractTextUiState.showDocumentPicker, extractTextUiState.selectedDocument?.id) {
        if (extractTextUiState.showDocumentPicker) {
            extractTextPickerSelection = extractTextUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(extractTextUiState.errorMessage) {
        extractTextUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            extractTextViewModel.clearError()
        }
    }

    LaunchedEffect(extractTextUiState.extractedTextResult?.outputFile?.absolutePath) {
        extractTextUiState.extractedTextResult?.let { result ->
            snackbarHostState.showSnackbar(
                "Extracted ${result.characterCount} characters into ${result.displayName}."
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.ExtractText)?.title ?: "Extract Text",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ExtractTextWorkspaceCard(
                    uiState = extractTextUiState,
                    onChooseDocument = extractTextViewModel::showDocumentPicker,
                    onScopeChange = extractTextViewModel::updateExtractionScope,
                    onStartPageChange = extractTextViewModel::updateStartPageInput,
                    onEndPageChange = extractTextViewModel::updateEndPageInput,
                    onOutputFileNameChange = extractTextViewModel::updateOutputFileName,
                    onExtractText = extractTextViewModel::extractText,
                    onOpenTextFile = { result ->
                        openFileExternally(
                            context = context,
                            file = result.outputFile,
                            mimeType = "text/plain",
                            chooserTitle = "Open extracted text",
                        )
                    },
                    onShareTextFile = { result ->
                        shareFile(
                            context = context,
                            file = result.outputFile,
                            mimeType = "text/plain",
                            chooserTitle = "Share text file",
                            title = result.displayName,
                        )
                    },
                    onSaveCopyTextFile = { result ->
                        fileExportLauncher.launch(
                            file = result.outputFile,
                            mimeType = "text/plain",
                            displayName = result.displayName,
                        )
                    },
                    onCopyText = { text ->
                        copyTextToClipboard(context, text)
                        scope.launch {
                            snackbarHostState.showSnackbar("Extracted text copied to clipboard.")
                        }
                    },
                )
            }
        }
    }

    if (extractTextUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to extract text",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = extractTextPickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document ->
                extractTextPickerSelection = document.id
            },
            onDismissRequest = extractTextViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                extractTextPickerSelection?.let { selectedId ->
                    extractTextViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF and extract text from all pages or a selected range.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPasswordToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val addPasswordViewModel: AddPasswordViewModel = viewModel(
        factory = remember(appContext) { AddPasswordViewModel.factory(appContext) },
    )
    val addPasswordUiState by addPasswordViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(addPasswordUiState.showDocumentPicker, addPasswordUiState.selectedDocument?.id) {
        if (addPasswordUiState.showDocumentPicker) {
            pickerSelection = addPasswordUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(addPasswordUiState.errorMessage) {
        addPasswordUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            addPasswordViewModel.clearError()
        }
    }

    LaunchedEffect(addPasswordUiState.resultDocument?.id) {
        addPasswordUiState.resultDocument?.let { document ->
            snackbarHostState.showSnackbar("Protected ${document.displayName} with a password.")
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.AddPassword)?.title ?: "Add Password",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AddPasswordWorkspaceCard(
                    uiState = addPasswordUiState,
                    onChooseDocument = addPasswordViewModel::showDocumentPicker,
                    onUserPasswordChange = addPasswordViewModel::updateUserPassword,
                    onConfirmPasswordChange = addPasswordViewModel::updateConfirmPassword,
                    onOwnerPasswordChange = addPasswordViewModel::updateOwnerPassword,
                    onOutputFileNameChange = addPasswordViewModel::updateOutputFileName,
                    onProtectPdf = {
                        addPasswordViewModel.protectSelectedDocument(catalogViewModel::addOrUpdate)
                    },
                    onOpenProtectedPdf = { document ->
                        openFileExternally(
                            context = context,
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            chooserTitle = "Open protected PDF",
                        )
                    },
                    onShareProtectedPdf = { document -> sharePdfDocument(context, document) },
                    onSaveCopyProtectedPdf = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (addPasswordUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to protect",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document ->
                pickerSelection = document.id
            },
            onDismissRequest = addPasswordViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    addPasswordViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF and save a new password-protected copy in app storage.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemovePasswordToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val removePasswordViewModel: RemovePasswordViewModel = viewModel(
        factory = remember(appContext) { RemovePasswordViewModel.factory(appContext) },
    )
    val removePasswordUiState by removePasswordViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(removePasswordUiState.showDocumentPicker, removePasswordUiState.selectedDocument?.id) {
        if (removePasswordUiState.showDocumentPicker) {
            pickerSelection = removePasswordUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(removePasswordUiState.errorMessage) {
        removePasswordUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            removePasswordViewModel.clearError()
        }
    }

    LaunchedEffect(removePasswordUiState.resultDocument?.id) {
        removePasswordUiState.resultDocument?.let { document ->
            snackbarHostState.showSnackbar("Removed the password from ${document.displayName}.")
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.RemovePassword)?.title ?: "Remove Password",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                RemovePasswordWorkspaceCard(
                    uiState = removePasswordUiState,
                    onChooseDocument = removePasswordViewModel::showDocumentPicker,
                    onCurrentPasswordChange = removePasswordViewModel::updateCurrentPassword,
                    onOutputFileNameChange = removePasswordViewModel::updateOutputFileName,
                    onRemovePassword = {
                        removePasswordViewModel.removePassword(catalogViewModel::addOrUpdate)
                    },
                    onOpenUnlockedPdf = onOpenDocument,
                    onShareUnlockedPdf = { document -> sharePdfDocument(context, document) },
                    onSaveCopyUnlockedPdf = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (removePasswordUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a protected PDF",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document ->
                pickerSelection = document.id
            },
            onDismissRequest = removePasswordViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    removePasswordViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one protected PDF, enter its current password, and save an unlocked copy.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotatePagesToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val rotatePagesViewModel: RotatePagesViewModel = viewModel(
        factory = remember(appContext) { RotatePagesViewModel.factory(appContext) },
    )
    val rotatePagesUiState by rotatePagesViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rotatePagesUiState.showDocumentPicker, rotatePagesUiState.selectedDocument?.id) {
        if (rotatePagesUiState.showDocumentPicker) {
            pickerSelection = rotatePagesUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(rotatePagesUiState.errorMessage) {
        rotatePagesUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            rotatePagesViewModel.clearError()
        }
    }

    LaunchedEffect(rotatePagesUiState.resultDocument?.id) {
        rotatePagesUiState.resultDocument?.let { document ->
            snackbarHostState.showSnackbar("Rotated pages in ${document.displayName}.")
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.RotatePages)?.title ?: "Rotate Pages",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                RotatePagesWorkspaceCard(
                    uiState = rotatePagesUiState,
                    onChooseDocument = rotatePagesViewModel::showDocumentPicker,
                    onPageRangesChange = rotatePagesViewModel::updatePageRangesInput,
                    onRotationOptionChange = rotatePagesViewModel::updateRotationOption,
                    onOutputFileNameChange = rotatePagesViewModel::updateOutputFileName,
                    onRotatePages = { rotatePagesViewModel.rotatePages(catalogViewModel::addOrUpdate) },
                    onOpenResult = onOpenDocument,
                    onShareResult = { document -> sharePdfDocument(context, document) },
                    onSaveCopyResult = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (rotatePagesUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to rotate pages",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document -> pickerSelection = document.id },
            onDismissRequest = rotatePagesViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    rotatePagesViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, specify which pages to rotate, and save the result as a new copy.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractPagesToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val extractPagesViewModel: ExtractPagesViewModel = viewModel(
        factory = remember(appContext) { ExtractPagesViewModel.factory(appContext) },
    )
    val extractPagesUiState by extractPagesViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(extractPagesUiState.showDocumentPicker, extractPagesUiState.selectedDocument?.id) {
        if (extractPagesUiState.showDocumentPicker) {
            pickerSelection = extractPagesUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(extractPagesUiState.errorMessage) {
        extractPagesUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            extractPagesViewModel.clearError()
        }
    }

    LaunchedEffect(extractPagesUiState.resultDocument?.id) {
        extractPagesUiState.resultDocument?.let { document ->
            snackbarHostState.showSnackbar("Extracted pages into ${document.displayName}.")
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.ExtractPages)?.title ?: "Extract Pages",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ExtractPagesWorkspaceCard(
                    uiState = extractPagesUiState,
                    onChooseDocument = extractPagesViewModel::showDocumentPicker,
                    onPageRangesChange = extractPagesViewModel::updatePageRangesInput,
                    onOutputFileNameChange = extractPagesViewModel::updateOutputFileName,
                    onExtractPages = { extractPagesViewModel.extractPages(catalogViewModel::addOrUpdate) },
                    onOpenResult = onOpenDocument,
                    onShareResult = { document -> sharePdfDocument(context, document) },
                    onSaveCopyResult = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (extractPagesUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to extract pages",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document -> pickerSelection = document.id },
            onDismissRequest = extractPagesViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    extractPagesViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF and keep only the pages you want in a new PDF copy.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeletePagesToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val deletePagesViewModel: DeletePagesViewModel = viewModel(
        factory = remember(appContext) { DeletePagesViewModel.factory(appContext) },
    )
    val deletePagesUiState by deletePagesViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deletePagesUiState.showDocumentPicker, deletePagesUiState.selectedDocument?.id) {
        if (deletePagesUiState.showDocumentPicker) {
            pickerSelection = deletePagesUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(deletePagesUiState.errorMessage) {
        deletePagesUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            deletePagesViewModel.clearError()
        }
    }

    LaunchedEffect(deletePagesUiState.resultDocument?.id) {
        deletePagesUiState.resultDocument?.let { document ->
            snackbarHostState.showSnackbar("Deleted pages from ${document.displayName}.")
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.DeletePages)?.title ?: "Delete Pages",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                DeletePagesWorkspaceCard(
                    uiState = deletePagesUiState,
                    onChooseDocument = deletePagesViewModel::showDocumentPicker,
                    onPageRangesChange = deletePagesViewModel::updatePageRangesInput,
                    onOutputFileNameChange = deletePagesViewModel::updateOutputFileName,
                    onDeletePages = { deletePagesViewModel.deletePages(catalogViewModel::addOrUpdate) },
                    onOpenResult = onOpenDocument,
                    onShareResult = { document -> sharePdfDocument(context, document) },
                    onSaveCopyResult = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (deletePagesUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to delete pages from",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document -> pickerSelection = document.id },
            onDismissRequest = deletePagesViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    deletePagesViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, list the pages to remove, and save the remaining pages as a new copy.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatermarkPdfToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val fileExportLauncher = rememberFileExportLauncher { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }
    val watermarkViewModel: WatermarkPdfViewModel = viewModel(
        factory = remember(appContext) { WatermarkPdfViewModel.factory(appContext) },
    )
    val watermarkUiState by watermarkViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(watermarkUiState.showDocumentPicker, watermarkUiState.selectedDocument?.id) {
        if (watermarkUiState.showDocumentPicker) {
            pickerSelection = watermarkUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(watermarkUiState.errorMessage) {
        watermarkUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            watermarkViewModel.clearError()
        }
    }

    LaunchedEffect(watermarkUiState.resultDocument?.id) {
        watermarkUiState.resultDocument?.let { document ->
            val pageCount = watermarkUiState.watermarkedPageCount
            snackbarHostState.showSnackbar(
                if (pageCount != null) {
                    "Applied watermark to $pageCount page${if (pageCount == 1) "" else "s"} in ${document.displayName}."
                } else {
                    "Saved ${document.displayName}."
                }
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.WatermarkPdf)?.title ?: "Watermark PDF",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                WatermarkPdfWorkspaceCard(
                    uiState = watermarkUiState,
                    onChooseDocument = watermarkViewModel::showDocumentPicker,
                    onWatermarkTextChange = watermarkViewModel::updateWatermarkText,
                    onPlacementChange = watermarkViewModel::updatePlacement,
                    onSizeOptionChange = watermarkViewModel::updateSizeOption,
                    onPageRangesChange = watermarkViewModel::updatePageRangesInput,
                    onOutputFileNameChange = watermarkViewModel::updateOutputFileName,
                    onApplyWatermark = { watermarkViewModel.applyWatermark(catalogViewModel::addOrUpdate) },
                    onOpenResult = onOpenDocument,
                    onShareResult = { document -> sharePdfDocument(context, document) },
                    onSaveCopyResult = { document ->
                        fileExportLauncher.launch(
                            file = File(document.filePath),
                            mimeType = "application/pdf",
                            displayName = document.displayName,
                        )
                    },
                )
            }
        }
    }

    if (watermarkUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to watermark",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document -> pickerSelection = document.id },
            onDismissRequest = watermarkViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    watermarkViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, add a text watermark, and save a new watermarked copy.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfToImagesToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val pdfToImagesViewModel: PdfToImagesViewModel = viewModel(
        factory = remember(appContext) { PdfToImagesViewModel.factory(appContext) },
    )
    val pdfToImagesUiState by pdfToImagesViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfToImagesUiState.showDocumentPicker, pdfToImagesUiState.selectedDocument?.id) {
        if (pdfToImagesUiState.showDocumentPicker) {
            pickerSelection = pdfToImagesUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(pdfToImagesUiState.errorMessage) {
        pdfToImagesUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            pdfToImagesViewModel.clearError()
        }
    }

    LaunchedEffect(pdfToImagesUiState.result?.outputDirectory?.absolutePath) {
        pdfToImagesUiState.result?.let { result ->
            snackbarHostState.showSnackbar(
                "Exported ${result.exportedImages.size} page${if (result.exportedImages.size == 1) "" else "s"} as ${result.format.name.uppercase()} images."
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.PdfToImages)?.title ?: "PDF to Images",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PdfToImagesWorkspaceCard(
                    uiState = pdfToImagesUiState,
                    onChooseDocument = pdfToImagesViewModel::showDocumentPicker,
                    onPageRangesChange = pdfToImagesViewModel::updatePageRangesInput,
                    onImageFormatChange = pdfToImagesViewModel::updateImageFormat,
                    onQualityOptionChange = pdfToImagesViewModel::updateQualityOption,
                    onOutputFolderNameChange = pdfToImagesViewModel::updateOutputFolderName,
                    onExportImages = pdfToImagesViewModel::exportImages,
                    onOpenImage = { file, format ->
                        openFileExternally(
                            context = context,
                            file = file,
                            mimeType = format.mimeType(),
                            chooserTitle = "Open image",
                        )
                    },
                    onShareImage = { file, format ->
                        shareFile(
                            context = context,
                            file = file,
                            mimeType = format.mimeType(),
                            chooserTitle = "Share image",
                            title = file.name,
                        )
                    },
                    onShareAllImages = { result -> shareImageExportResult(context, result) },
                )
            }
        }
    }

    if (pdfToImagesUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to export as images",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document -> pickerSelection = document.id },
            onDismissRequest = pdfToImagesViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    pdfToImagesViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, pick which pages to export, and save one PNG or JPEG image per page.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractEmbeddedImagesToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val extractEmbeddedImagesViewModel: ExtractEmbeddedImagesViewModel = viewModel(
        factory = remember(appContext) { ExtractEmbeddedImagesViewModel.factory(appContext) },
    )
    val extractEmbeddedImagesUiState by extractEmbeddedImagesViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(extractEmbeddedImagesUiState.showDocumentPicker, extractEmbeddedImagesUiState.selectedDocument?.id) {
        if (extractEmbeddedImagesUiState.showDocumentPicker) {
            pickerSelection = extractEmbeddedImagesUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(extractEmbeddedImagesUiState.errorMessage) {
        extractEmbeddedImagesUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            extractEmbeddedImagesViewModel.clearError()
        }
    }

    LaunchedEffect(extractEmbeddedImagesUiState.result?.outputDirectory?.absolutePath) {
        extractEmbeddedImagesUiState.result?.let { result ->
            snackbarHostState.showSnackbar(
                "Extracted ${result.exportedImages.size} embedded image${if (result.exportedImages.size == 1) "" else "s"}."
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.ExtractEmbeddedImages)?.title ?: "Extract Embedded Images",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ExtractEmbeddedImagesWorkspaceCard(
                    uiState = extractEmbeddedImagesUiState,
                    onChooseDocument = extractEmbeddedImagesViewModel::showDocumentPicker,
                    onPageRangesChange = extractEmbeddedImagesViewModel::updatePageRangesInput,
                    onOutputFolderNameChange = extractEmbeddedImagesViewModel::updateOutputFolderName,
                    onExtractImages = extractEmbeddedImagesViewModel::extractImages,
                    onOpenImage = { file ->
                        openFileExternally(
                            context = context,
                            file = file,
                            mimeType = imageMimeTypeFor(file),
                            chooserTitle = "Open image",
                        )
                    },
                    onShareImage = { file ->
                        shareFile(
                            context = context,
                            file = file,
                            mimeType = imageMimeTypeFor(file),
                            chooserTitle = "Share image",
                            title = file.name,
                        )
                    },
                    onShareAllImages = { result -> shareEmbeddedImageResult(context, result) },
                )
            }
        }
    }

    if (extractEmbeddedImagesUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to extract images from",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document -> pickerSelection = document.id },
            onDismissRequest = extractEmbeddedImagesViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    extractEmbeddedImagesViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, scan selected pages for embedded images, and export each unique image into a folder.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CropPageRegionToolRoute(
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val cropViewModel: CropPageRegionViewModel = viewModel(
        factory = remember(appContext) { CropPageRegionViewModel.factory(appContext) },
    )
    val cropUiState by cropViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cropUiState.showDocumentPicker, cropUiState.selectedDocument?.id) {
        if (cropUiState.showDocumentPicker) {
            pickerSelection = cropUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(cropUiState.errorMessage) {
        cropUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            cropViewModel.clearError()
        }
    }

    LaunchedEffect(cropUiState.result?.outputDirectory?.absolutePath) {
        cropUiState.result?.let { result ->
            snackbarHostState.showSnackbar(
                "Exported ${result.exportedImages.size} cropped page region${if (result.exportedImages.size == 1) "" else "s"}."
            )
        }
    }

    ToolDetailScaffold(
        title = toolDefinitionFor(ToolIds.CropPageRegion)?.title ?: "Crop Page Region",
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CropPageRegionWorkspaceCard(
                    uiState = cropUiState,
                    onChooseDocument = cropViewModel::showDocumentPicker,
                    onPageRangesChange = cropViewModel::updatePageRangesInput,
                    onImageFormatChange = cropViewModel::updateImageFormat,
                    onQualityOptionChange = cropViewModel::updateQualityOption,
                    onPresetChange = cropViewModel::applyPreset,
                    onLeftPercentChange = cropViewModel::updateLeftPercent,
                    onTopPercentChange = cropViewModel::updateTopPercent,
                    onRightPercentChange = cropViewModel::updateRightPercent,
                    onBottomPercentChange = cropViewModel::updateBottomPercent,
                    onOutputFolderNameChange = cropViewModel::updateOutputFolderName,
                    onExportCrop = cropViewModel::exportCroppedRegions,
                    onOpenImage = { file, format ->
                        openFileExternally(
                            context = context,
                            file = file,
                            mimeType = format.mimeType(),
                            chooserTitle = "Open cropped image",
                        )
                    },
                    onShareImage = { file, format ->
                        shareFile(
                            context = context,
                            file = file,
                            mimeType = format.mimeType(),
                            chooserTitle = "Share cropped image",
                            title = file.name,
                        )
                    },
                    onShareAllImages = { result -> shareCropPageRegionResult(context, result) },
                )
            }
        }
    }

    if (cropUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a PDF to crop",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { document -> pickerSelection = document.id },
            onDismissRequest = cropViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    cropViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, define a page region, and export the selected crop area as PNG or JPEG images.",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnknownToolRoute(
    onNavigateBack: () -> Unit,
) {
    ToolDetailScaffold(
        title = "Tool",
        onNavigateBack = onNavigateBack,
        snackbarHostState = remember { SnackbarHostState() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "This tool could not be loaded.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Return to the tools list and try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolDetailScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
private fun MergePdfWorkspaceCard(
    uiState: MergePdfUiState,
    onChooseDocuments: () -> Unit,
    onOutputFileNameChange: (String) -> Unit,
    onMoveDocumentUp: (String) -> Unit,
    onMoveDocumentDown: (String) -> Unit,
    onRemoveDocument: (String) -> Unit,
    onMerge: () -> Unit,
    onOpenMergedDocument: (DocumentItem) -> Unit,
    onShareMergedDocument: (DocumentItem) -> Unit,
    onSaveCopyMergedDocument: (DocumentItem) -> Unit,
) {
    val totalPages = uiState.selectedDocuments.sumOf { it.pageCount }
    val canMerge = uiState.selectedDocuments.size > 1 && !uiState.isMerging

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Merge PDFs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Choose multiple PDFs, arrange their order, and create a new merged file in app storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = toolDefinitionFor(ToolIds.MergePdf)?.icon ?: return@Card,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = onChooseDocuments,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.selectedDocuments.isEmpty()) "Choose PDFs" else "Update PDF Selection")
            }

            if (uiState.selectedDocuments.isEmpty()) {
                Text(
                    text = "Pick at least 2 PDFs from your catalog to start a merge job.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                AssistChip(
                    onClick = onChooseDocuments,
                    label = {
                        Text("${uiState.selectedDocuments.size} files • ${totalPages.coerceAtLeast(0)} pages")
                    },
                )

                OutlinedTextField(
                    value = uiState.outputFileName,
                    onValueChange = onOutputFileNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Output file name") },
                    supportingText = { Text(".pdf is added automatically if needed") },
                    singleLine = true,
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.selectedDocuments.forEachIndexed { index, document ->
                        MergeSelectionItem(
                            index = index,
                            document = document,
                            canMoveUp = index > 0,
                            canMoveDown = index < uiState.selectedDocuments.lastIndex,
                            onMoveUp = { onMoveDocumentUp(document.id) },
                            onMoveDown = { onMoveDocumentDown(document.id) },
                            onRemove = { onRemoveDocument(document.id) },
                        )
                    }
                }
            }

            Button(
                onClick = onMerge,
                enabled = canMerge,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isMerging) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Text("Merging PDFs…")
                } else {
                    Text(
                        if (uiState.selectedDocuments.size > 1) {
                            "Merge ${uiState.selectedDocuments.size} PDFs"
                        } else {
                            "Select PDFs to merge"
                        }
                    )
                }
            }

            uiState.resultDocument?.let { mergedDocument ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Merged PDF ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    DocumentSummaryCard(document = mergedDocument)
                    ResultActionButtons(
                        onOpen = { onOpenMergedDocument(mergedDocument) },
                        onShare = { onShareMergedDocument(mergedDocument) },
                        onSaveCopy = { onSaveCopyMergedDocument(mergedDocument) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MergeSelectionItem(
    index: Int,
    document: DocumentItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}. ${document.displayName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }
            DocumentSummaryCard(document = document)
        }
    }
}


private fun shareImageExportResult(
    context: Context,
    result: PdfToImagesResult,
) {
    val outputFiles = result.exportedImages.map { it.outputFile }.filter(File::exists)
    if (outputFiles.isEmpty()) return

    if (outputFiles.size == 1) {
        shareFile(
            context = context,
            file = outputFiles.first(),
            mimeType = result.format.mimeType(),
            chooserTitle = "Share image",
            title = outputFiles.first().name,
        )
        return
    }

    shareFiles(
        context = context,
        files = outputFiles,
        mimeType = result.format.mimeType(),
        chooserTitle = "Share images",
        title = result.outputDirectory.name,
    )
}

private fun shareEmbeddedImageResult(
    context: Context,
    result: ExtractEmbeddedImagesResult,
) {
    val outputFiles = result.exportedImages.map { it.outputFile }.filter(File::exists)
    if (outputFiles.isEmpty()) return

    if (outputFiles.size == 1) {
        val file = outputFiles.first()
        shareFile(
            context = context,
            file = file,
            mimeType = imageMimeTypeFor(file),
            chooserTitle = "Share image",
            title = file.name,
        )
        return
    }

    shareFiles(
        context = context,
        files = outputFiles,
        mimeType = "image/*",
        chooserTitle = "Share images",
        title = result.outputDirectory.name,
    )
}

private fun shareCropPageRegionResult(
    context: Context,
    result: PdfCropPageRegionResult,
) {
    val outputFiles = result.exportedImages.map { it.outputFile }.filter(File::exists)
    if (outputFiles.isEmpty()) return

    if (outputFiles.size == 1) {
        val file = outputFiles.first()
        shareFile(
            context = context,
            file = file,
            mimeType = result.format.mimeType(),
            chooserTitle = "Share cropped image",
            title = file.name,
        )
        return
    }

    shareFiles(
        context = context,
        files = outputFiles,
        mimeType = result.format.mimeType(),
        chooserTitle = "Share cropped images",
        title = result.outputDirectory.name,
    )
}


private fun copyTextToClipboard(
    context: Context,
    text: String,
) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboardManager.setPrimaryClip(ClipData.newPlainText("Extracted text", text))
}

