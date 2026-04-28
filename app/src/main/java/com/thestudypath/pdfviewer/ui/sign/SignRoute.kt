package com.thestudypath.pdfviewer.ui.sign

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thestudypath.pdfviewer.catalog.DocumentCatalogViewModel
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.navigation.AppDestination
import com.thestudypath.pdfviewer.ui.documents.DocumentSelectionMode
import com.thestudypath.pdfviewer.ui.documents.DocumentPickerSheet
import com.thestudypath.pdfviewer.ui.documents.DocumentSummaryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignRoute(
    viewModel: DocumentCatalogViewModel,
    onImportDocument: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedDocument by remember { mutableStateOf<DocumentItem?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(AppDestination.Sign.label) })
        }
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
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "My Signatures",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Milestone 0 sets up the document picker flow. Signature drawing, PNG import, and placement editor come next.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = {
                            Toast.makeText(
                                context,
                                "Signature canvas will land in the signing milestone.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }) {
                            Icon(Icons.Default.Draw, contentDescription = null)
                            Text(
                                text = "Create signature",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Sign a Document",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Pick a PDF from the shared catalog so the future sign placement flow can start from the same recent-files foundation as Tools.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { showPicker = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text(
                                text = "Choose PDF",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        selectedDocument?.let { document ->
                            DocumentSummaryCard(document = document)
                            Button(onClick = {
                                Toast.makeText(
                                    context,
                                    "${document.displayName} is staged for visual signing. Placement editor comes next.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Text(
                                    text = "Prepare placement flow",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        DocumentPickerSheet(
            title = "Choose a document to sign",
            uiState = uiState,
            selectionMode = DocumentSelectionMode.Single,
            selectedDocumentIds = selectedDocument?.let { setOf(it.id) }.orEmpty(),
            onDocumentToggle = { selectedDocument = it },
            onDismissRequest = { showPicker = false },
            onImportClick = onImportDocument,
            onConfirm = { showPicker = false },
            confirmLabel = "Use document",
            helperText = "The same shared picker can now be reused for future signing and document tools.",
        )
    }
}

