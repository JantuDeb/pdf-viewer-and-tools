package com.thestudypath.pdfviewer.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thestudypath.pdfviewer.catalog.DocumentCatalogUiState
import com.thestudypath.pdfviewer.catalog.DocumentCatalogViewModel
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.ui.documents.DocumentPickerSheet
import com.thestudypath.pdfviewer.ui.documents.DocumentSelectionMode
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.tools.compress.CompressPdfViewModel
import com.thestudypath.pdfviewer.ui.tools.compress.CompressPdfWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.compress.formatFileSize
import com.thestudypath.pdfviewer.ui.tools.compress.profileLabel
import com.thestudypath.pdfviewer.ui.tools.images.ImagesToPdfViewModel
import com.thestudypath.pdfviewer.ui.tools.images.ImagesToPdfWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.merge.MergePdfUiState
import com.thestudypath.pdfviewer.ui.tools.merge.MergePdfViewModel
import com.thestudypath.pdfviewer.ui.tools.split.SplitPdfViewModel
import com.thestudypath.pdfviewer.ui.tools.split.SplitPdfWorkspaceCard
import com.thestudypath.pdfviewer.ui.tools.text.ExtractTextViewModel
import com.thestudypath.pdfviewer.ui.tools.text.ExtractTextWorkspaceCard
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
        ToolIds.AddPassword,
        ToolIds.RemovePassword,
        -> PlaceholderToolDetailRoute(
            toolId = toolId,
            catalogViewModel = catalogViewModel,
            onNavigateBack = onNavigateBack,
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
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
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
                    onShareResult = { document -> shareDocument(context, document) },
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
    val snackbarHostState = remember { SnackbarHostState() }
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
                    onShareResult = { document -> shareDocument(context, document) },
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
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
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
                    onShareMergedDocument = { document -> shareDocument(context, document) },
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
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
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
                    onShareSplitDocument = { document -> shareDocument(context, document) },
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
private fun PlaceholderToolDetailRoute(
    toolId: String,
    catalogViewModel: DocumentCatalogViewModel,
    onNavigateBack: () -> Unit,
    onImportDocument: () -> Unit,
) {
    val tool = toolDefinitionFor(toolId)
    val catalogUiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val availableDocuments = (catalogUiState as? DocumentCatalogUiState.Success)?.documents.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDocumentPicker by remember { mutableStateOf(false) }
    var selectedDocumentIds by remember { mutableStateOf(setOf<String>()) }
    val selectedDocuments = remember(availableDocuments, selectedDocumentIds) {
        availableDocuments.filter { selectedDocumentIds.contains(it.id) }
    }

    ToolDetailScaffold(
        title = tool?.title ?: "Tool",
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
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        tool?.let {
                            Icon(
                                imageVector = it.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = it.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            it.status?.let { status ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(status) },
                                )
                            }
                        }

                        Button(
                            onClick = { showDocumentPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                when (tool?.selectionMode) {
                                    DocumentSelectionMode.Multiple -> "Choose PDFs"
                                    else -> "Choose PDF"
                                }
                            )
                        }

                        if (selectedDocumentIds.isNotEmpty()) {
                            AssistChip(
                                onClick = { showDocumentPicker = true },
                                label = {
                                    Text(
                                        "${selectedDocumentIds.size} document${if (selectedDocumentIds.size == 1) "" else "s"} selected"
                                    )
                                },
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "${tool?.title ?: "Tool"} staged with ${selectedDocumentIds.size} document(s). Processing arrives in the next milestone."
                                    )
                                }
                            },
                            enabled = selectedDocumentIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Continue")
                        }
                    }
                }
            }

            items(selectedDocuments, key = { it.id }) { document ->
                DocumentSummaryCard(document = document)
            }
        }
    }

    if (showDocumentPicker && tool?.selectionMode != null) {
        DocumentPickerSheet(
            title = tool.title,
            uiState = catalogUiState,
            selectionMode = tool.selectionMode,
            selectedDocumentIds = selectedDocumentIds,
            onDocumentToggle = { document ->
                selectedDocumentIds = when (tool.selectionMode) {
                    DocumentSelectionMode.Single -> setOf(document.id)
                    DocumentSelectionMode.Multiple -> {
                        if (selectedDocumentIds.contains(document.id)) {
                            selectedDocumentIds - document.id
                        } else {
                            selectedDocumentIds + document.id
                        }
                    }
                }
            },
            onDismissRequest = { showDocumentPicker = false },
            onImportClick = onImportDocument,
            onConfirm = {
                selectedDocumentIds = when (tool.selectionMode) {
                    DocumentSelectionMode.Single -> selectedDocumentIds.take(1).toSet()
                    DocumentSelectionMode.Multiple -> selectedDocumentIds
                }
                showDocumentPicker = false
            },
            confirmLabel = "Use selection",
            helperText = when (tool.id) {
                ToolIds.AddPassword -> "Choose one PDF to prepare the password-protection flow."
                ToolIds.RemovePassword -> "Choose one protected PDF to prepare the unlock flow."
                else -> null
            },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { onOpenMergedDocument(mergedDocument) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                            Text(
                                text = "Open",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Button(
                            onClick = { onShareMergedDocument(mergedDocument) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Text(
                                text = "Share",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
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

private fun shareDocument(
    context: Context,
    document: DocumentItem,
) {
    val file = File(document.filePath)
    if (!file.exists()) return

    shareFile(
        context = context,
        file = file,
        mimeType = "application/pdf",
        chooserTitle = "Share PDF",
        title = document.displayName,
    )
}

private fun shareFile(
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

private fun openFileExternally(
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

private fun copyTextToClipboard(
    context: Context,
    text: String,
) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboardManager.setPrimaryClip(ClipData.newPlainText("Extracted text", text))
}

