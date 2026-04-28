package com.thestudypath.pdfviewer

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.thestudypath.pdfviewer.catalog.DocumentCatalogRepository
import com.thestudypath.pdfviewer.catalog.DocumentCatalogViewModel
import com.thestudypath.pdfviewer.catalog.toDocumentItem
import com.thestudypath.pdfviewer.navigation.PdfViewerNavHost
import com.thestudypath.pdfviewer.navigation.topLevelDestinations
import com.thestudypath.pdfviewer.ui.theme.PDFViewerTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private val documentCatalogRepository by lazy { DocumentCatalogRepository(applicationContext) }
    private val documentCatalogViewModelFactory by lazy {
        DocumentCatalogViewModel.factory(documentCatalogRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            PDFViewerTheme {
                PdfWorkspaceApp(factory = documentCatalogViewModelFactory)
            }
        }
    }
}

@Composable
fun PdfWorkspaceApp(
    factory: ViewModelProvider.Factory,
    viewModel: DocumentCatalogViewModel = viewModel(factory = factory),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun openDocument(document: com.thestudypath.pdfviewer.catalog.DocumentItem) {
        if (File(document.filePath).exists()) {
            viewModel.addOrUpdate(document)
            PdfViewerActivity.launchExisting(context, document)
        } else {
            viewModel.onMissingDocumentOpened(document.filePath)
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        var displayName = "document.pdf"
        var fileSize = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 }
                    ?.let { displayName = cursor.getString(it) ?: displayName }
                cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 }
                    ?.let { fileSize = cursor.getLong(it) }
            }
        }
        scope.launch {
            PdfViewerActivity.copyAndLaunch(context, uri, displayName, fileSize)
                ?.also { viewModel.addOrUpdate(it.toDocumentItem()) }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = destination.matchesRoute(currentRoute),
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        PdfViewerNavHost(
            navController = navController,
            viewModel = viewModel,
            onOpenDocument = ::openDocument,
            onImportDocument = { pdfPicker.launch(arrayOf("application/pdf")) },
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        )
    }
}
