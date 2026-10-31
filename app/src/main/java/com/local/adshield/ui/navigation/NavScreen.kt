package com.local.adshield.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavScreen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : NavScreen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    )

    data object Inspector : NavScreen(
        route = "inspector",
        title = "Logs",
        selectedIcon = Icons.AutoMirrored.Filled.ListAlt,
        unselectedIcon = Icons.AutoMirrored.Outlined.ListAlt
    )

    data object Rules : NavScreen(
        route = "rules",
        title = "Rules",
        selectedIcon = Icons.Filled.Block,
        unselectedIcon = Icons.Outlined.Block
    )

    companion object {
        val items: List<NavScreen>
            get() = listOf(Dashboard, Inspector, Rules)
    }
}
