package com.thestudypath.pdfviewer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    fun matchesRoute(currentRoute: String?): Boolean {
        return currentRoute == route || currentRoute?.startsWith("$route/") == true
    }

    data object Files : AppDestination(
        route = "files",
        label = "Files",
        icon = Icons.Default.Folder,
    )

    data object Tools : AppDestination(
        route = "tools",
        label = "Tools",
        icon = Icons.Default.Build,
    )

    data object Sign : AppDestination(
        route = "sign",
        label = "Sign",
        icon = Icons.Default.Draw,
    )

    data object Scanner : AppDestination(
        route = "scanner",
        label = "Scanner",
        icon = Icons.Default.CameraAlt,
    )
}

val topLevelDestinations = listOf(
    AppDestination.Files,
    AppDestination.Tools,
    AppDestination.Sign,
    AppDestination.Scanner,
)

object ToolsNavDestination {
    const val toolIdArg = "toolId"
    val detailRouteBase = "${AppDestination.Tools.route}/detail"
    val detailRoutePattern = "$detailRouteBase/{$toolIdArg}"

    fun detailRoute(toolId: String): String = "$detailRouteBase/$toolId"
}

