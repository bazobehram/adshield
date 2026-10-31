package com.local.adshield

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.local.adshield.data.local.AppPreferences
import com.local.adshield.ui.navigation.NavScreen
import com.local.adshield.ui.screens.dashboard.DashboardScreen
import com.local.adshield.ui.screens.dashboard.DashboardViewModel
import com.local.adshield.ui.screens.inspector.InspectorViewModel
import com.local.adshield.ui.screens.inspector.TrafficInspectorScreen
import com.local.adshield.ui.screens.rules.RulesScreen
import com.local.adshield.ui.screens.rules.RulesViewModel
import com.local.adshield.ui.theme.AdShieldTheme
import com.local.adshield.ui.theme.DarkBackground
import com.local.adshield.ui.theme.DarkBorder
import com.local.adshield.ui.theme.DarkSurface
import com.local.adshield.ui.theme.ShieldGreen
import com.local.adshield.ui.theme.TextMuted
import com.local.adshield.ui.theme.TextPrimary
import com.local.adshield.ui.theme.TextSecondary
import com.local.adshield.vpn.VpnStateHolder

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val inspectorViewModel: InspectorViewModel by viewModels()
    private val rulesViewModel: RulesViewModel by viewModels()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            dashboardViewModel.startVpn()
            Toast.makeText(this, "AdShield DNS Protection Started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "VPN permission is required to filter DNS", Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkNotificationPermission()
        autoStartIfPermissionGranted()

        setContent {
            AdShieldTheme {
                MainAppScaffold(
                    dashboardViewModel = dashboardViewModel,
                    inspectorViewModel = inspectorViewModel,
                    rulesViewModel = rulesViewModel,
                    onRequestVpnPermission = { requestVpnPermission() }
                )
            }
        }
    }

    private fun autoStartIfPermissionGranted() {
        val prefs = AppPreferences(this)
        if (prefs.isProtectionEnabled && !VpnStateHolder.isRunning.value) {
            if (VpnService.prepare(this) == null) {
                dashboardViewModel.startVpn()
            }
        }
    }

    private fun requestVpnPermission() {
        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            vpnPermissionLauncher.launch(vpnIntent)
        } else {
            dashboardViewModel.startVpn()
            Toast.makeText(this, "AdShield DNS Protection Started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    dashboardViewModel: DashboardViewModel,
    inspectorViewModel: InspectorViewModel,
    rulesViewModel: RulesViewModel,
    onRequestVpnPermission: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavScreen.Dashboard.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentRoute) {
                            NavScreen.Dashboard.route -> "AdShield"
                            NavScreen.Inspector.route -> "Traffic Logs"
                            NavScreen.Rules.route -> "Rules & Feeds"
                            else -> "AdShield"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder)
            ) {
                NavScreen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ShieldGreen,
                            selectedTextColor = ShieldGreen,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = DarkSurface
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavScreen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(NavScreen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onRequestVpnPermission = onRequestVpnPermission,
                    onNavigateToInspector = {
                        navController.navigate(NavScreen.Inspector.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(NavScreen.Inspector.route) {
                TrafficInspectorScreen(viewModel = inspectorViewModel)
            }
            composable(NavScreen.Rules.route) {
                RulesScreen(viewModel = rulesViewModel)
            }
        }
    }
}
