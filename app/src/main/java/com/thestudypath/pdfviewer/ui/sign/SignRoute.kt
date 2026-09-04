package com.thestudypath.pdfviewer.ui.sign

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thestudypath.pdfviewer.catalog.DocumentCatalogUiState
import com.thestudypath.pdfviewer.catalog.DocumentCatalogViewModel
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.navigation.AppDestination
import com.thestudypath.pdfviewer.processing.VisibleSignaturePlacement
import com.thestudypath.pdfviewer.ui.documents.DocumentPickerSheet
import com.thestudypath.pdfviewer.ui.documents.DocumentSelectionMode
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard
import com.thestudypath.pdfviewer.ui.results.ResultActionButtons
import com.thestudypath.pdfviewer.ui.results.rememberFileExportLauncher
import com.thestudypath.pdfviewer.ui.results.sharePdfDocument
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignRoute(
    catalogViewModel: DocumentCatalogViewModel,
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
    val signViewModel: SignViewModel = viewModel(
        factory = remember(appContext) { SignViewModel.factory(appContext) },
    )
    val signUiState by signViewModel.uiState.collectAsStateWithLifecycle()
    var pickerSelection by remember { mutableStateOf<String?>(null) }
    var drawnStrokes by remember { mutableStateOf(emptyList<SignatureStroke>()) }
    val signatureImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let(signViewModel::importSignature)
    }

    LaunchedEffect(signUiState.showDocumentPicker, signUiState.selectedDocument?.id) {
        if (signUiState.showDocumentPicker) {
            pickerSelection = signUiState.selectedDocument?.id
        }
    }

    LaunchedEffect(signUiState.errorMessage) {
        signUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            signViewModel.clearError()
        }
    }

    LaunchedEffect(signUiState.resultDocument?.id) {
        signUiState.resultDocument?.let { document ->
            snackbarHostState.showSnackbar("Created signed copy ${document.displayName}.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(AppDestination.Sign.label) })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Sign a Document",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Choose a PDF, create or import a visible signature, then place it on a page and save a signed copy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Button(
                            onClick = signViewModel::showDocumentPicker,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (signUiState.selectedDocument == null) "Choose PDF" else "Change PDF")
                        }

                        signUiState.selectedDocument?.let { selectedDocument ->
                            DocumentSummaryCard(document = selectedDocument)
                            if (signUiState.isInspectingDocument) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("Inspecting document pages…")
                                }
                            } else {
                                signUiState.selectedDocumentPageCount?.let { pageCount ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("$pageCount pages available for placement") },
                                    )
                                }
                            }
                        }
                    }
                }
            }

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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Create or import signature",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Draw with your finger/stylus or import a PNG/JPG signature image.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        SignaturePad(
                            strokes = drawnStrokes,
                            onStrokesChanged = { drawnStrokes = it },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Button(
                                onClick = { signViewModel.saveDrawnSignature(renderSignaturePng(drawnStrokes)) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Brush, contentDescription = null)
                                Text("Use drawing", modifier = Modifier.padding(start = 8.dp))
                            }
                            Button(
                                onClick = { drawnStrokes = emptyList() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Text("Clear", modifier = Modifier.padding(start = 8.dp))
                            }
                        }

                        Button(
                            onClick = { signatureImagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Text("Import signature image", modifier = Modifier.padding(start = 8.dp))
                        }

                        signUiState.selectedSignature?.let { signature ->
                            SelectedSignatureCard(
                                signature = signature,
                                onClear = signViewModel::clearSignature,
                            )
                        }
                    }
                }
            }

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
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Placement and export",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Choose the page, pick a placement preset, tune the signature width, and create a signed PDF copy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        OutlinedTextField(
                            value = signUiState.pageNumberInput,
                            onValueChange = signViewModel::updatePageNumberInput,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Page number") },
                            supportingText = {
                                Text(
                                    signUiState.selectedDocumentPageCount?.let { totalPages ->
                                        "Choose any page from 1 to $totalPages"
                                    } ?: "Choose a document first to load page count"
                                )
                            },
                            singleLine = true,
                        )

                        Text(
                            text = "Placement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val placements = VisibleSignaturePlacement.entries.chunked(3)
                            placements.forEach { rowPlacements ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowPlacements.forEach { placement ->
                                        FilterChip(
                                            selected = signUiState.placement == placement,
                                            onClick = { signViewModel.updatePlacement(placement) },
                                            label = { Text(placementLabel(placement)) },
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Signature width: ${signUiState.signatureWidthPercent.toInt()}% of page width",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Slider(
                            value = signUiState.signatureWidthPercent,
                            onValueChange = signViewModel::updateSignatureWidthPercent,
                            valueRange = 10f..40f,
                        )

                        OutlinedTextField(
                            value = signUiState.outputFileName,
                            onValueChange = signViewModel::updateOutputFileName,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Output file name") },
                            supportingText = { Text(".pdf is added automatically if needed") },
                            singleLine = true,
                        )

                        Button(
                            onClick = { signViewModel.signSelectedDocument(catalogViewModel::addOrUpdate) },
                            enabled = !signUiState.isSigning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (signUiState.isSigning) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text("Creating signed PDF…")
                            } else {
                                Text("Create signed PDF")
                            }
                        }

                        signUiState.resultDocument?.let { signedDocument ->
                            Text(
                                text = "Signed PDF ready",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            DocumentSummaryCard(document = signedDocument)
                            ResultActionButtons(
                                onOpen = { onOpenDocument(signedDocument) },
                                onShare = {
                                    sharePdfDocument(
                                        context = context,
                                        document = signedDocument,
                                        chooserTitle = "Share signed PDF",
                                    )
                                },
                                onSaveCopy = {
                                    fileExportLauncher.launch(
                                        file = File(signedDocument.filePath),
                                        mimeType = "application/pdf",
                                        displayName = signedDocument.displayName,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (signUiState.showDocumentPicker) {
        DocumentPickerSheet(
            title = "Choose a document to sign",
            uiState = catalogUiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = pickerSelection?.let(::setOf).orEmpty(),
            onDocumentToggle = { pickerSelection = it.id },
            onDismissRequest = signViewModel::dismissDocumentPicker,
            onImportClick = onImportDocument,
            onConfirm = {
                pickerSelection?.let { selectedId ->
                    signViewModel.confirmDocumentSelection(
                        allDocuments = availableDocuments,
                        selectedDocumentId = selectedId,
                    )
                }
            },
            confirmLabel = "Use document",
            helperText = "Choose one PDF, then add a visible signature to one of its pages.",
        )
    }
}

@Composable
private fun SelectedSignatureCard(
    signature: SignatureInput,
    onClear: () -> Unit,
) {
    val bitmap = remember(signature.filePath) { BitmapFactory.decodeFile(signature.filePath) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Active signature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = signature.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        when (signature.sourceType) {
                            SignatureSourceType.Drawn -> "Using drawn signature"
                            SignatureSourceType.Imported -> "Using imported image"
                        }
                    )
                },
            )
            Text(
                text = signature.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onClear) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Text("Remove signature", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

