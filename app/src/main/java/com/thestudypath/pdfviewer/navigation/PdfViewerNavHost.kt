package com.thestudypath.pdfviewer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.thestudypath.pdfviewer.catalog.DocumentCatalogViewModel
import com.thestudypath.pdfviewer.catalog.DocumentItem
import com.thestudypath.pdfviewer.ui.files.FilesRoute
import com.thestudypath.pdfviewer.ui.scanner.ScannerRoute
import com.thestudypath.pdfviewer.ui.sign.SignRoute
import com.thestudypath.pdfviewer.ui.tools.ToolDetailRoute
import com.thestudypath.pdfviewer.ui.tools.ToolsRoute

@Composable
fun PdfViewerNavHost(
    navController: NavHostController,
    viewModel: DocumentCatalogViewModel,
    onOpenDocument: (DocumentItem) -> Unit,
    onImportDocument: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Files.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Files.route) {
            FilesRoute(
                viewModel = viewModel,
                onOpenDocument = onOpenDocument,
                onImportDocument = onImportDocument,
            )
        }
        composable(AppDestination.Tools.route) {
            ToolsRoute(
                onOpenTool = { toolId ->
                    navController.navigate(ToolsNavDestination.detailRoute(toolId))
                },
            )
        }
        composable(
            route = ToolsNavDestination.detailRoutePattern,
            arguments = listOf(
                navArgument(ToolsNavDestination.toolIdArg) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            backStackEntry.arguments?.getString(ToolsNavDestination.toolIdArg)?.let { toolId ->
                ToolDetailRoute(
                    toolId = toolId,
                    catalogViewModel = viewModel,
                    onNavigateBack = navController::popBackStack,
                    onOpenDocument = onOpenDocument,
                    onImportDocument = onImportDocument,
                )
            }
        }
        composable(AppDestination.Sign.route) {
            SignRoute(
                viewModel = viewModel,
                onImportDocument = onImportDocument,
            )
        }
        composable(AppDestination.Scanner.route) {
            ScannerRoute()
        }
    }
}

