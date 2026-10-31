package com.local.adshield.ui.screens.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.local.adshield.data.model.DnsServer
import com.local.adshield.data.model.QueryLogEntity
import com.local.adshield.data.model.QueryStatus
import com.local.adshield.ui.theme.DarkBorder
import com.local.adshield.ui.theme.DarkSurface
import com.local.adshield.ui.theme.DarkSurfaceVariant
import com.local.adshield.ui.theme.ShieldAmber
import com.local.adshield.ui.theme.ShieldAmberMuted
import com.local.adshield.ui.theme.ShieldBlue
import com.local.adshield.ui.theme.ShieldGreen
import com.local.adshield.ui.theme.ShieldGreenMuted
import com.local.adshield.ui.theme.ShieldRed
import com.local.adshield.ui.theme.ShieldRedMuted
import com.local.adshield.ui.theme.TextMuted
import com.local.adshield.ui.theme.TextPrimary
import com.local.adshield.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onRequestVpnPermission: () -> Unit,
    onNavigateToInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVpnActive by viewModel.isVpnActive.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val upstreamDns by viewModel.upstreamDns.collectAsState()
    val activeRulesCount by viewModel.activeRulesCount.collectAsState()
    val recentQueries by viewModel.recentQueries.collectAsState()

    var showCustomDnsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 1. Hero Protection Card
        item {
            HeroProtectionCard(
                isActive = isVpnActive,
                onToggle = { active ->
                    if (active) {
                        onRequestVpnPermission()
                    } else {
                        viewModel.stopVpn()
                    }
                }
            )
        }

        // 2. Statistics Grid
        item {
            StatisticsGrid(
                totalQueries = stats.totalQueriesToday,
                totalBlocked = stats.totalBlockedToday,
                blockPercentage = stats.blockPercentage,
                activeRulesCount = activeRulesCount
            )
        }

        // 3. Upstream DNS Selector
        item {
            UpstreamDnsCard(
                currentDns = upstreamDns,
                onSelectDns = { viewModel.setUpstreamDns(it) },
                onCustomDnsClick = { showCustomDnsDialog = true }
            )
        }

        // 4. Recent Queries Preview
        item {
            RecentQueriesPreview(
                recentQueries = recentQueries,
                onViewAllClick = onNavigateToInspector
            )
        }
    }

    if (showCustomDnsDialog) {
        CustomDnsDialog(
            initialIp = upstreamDns.ip,
            onDismiss = { showCustomDnsDialog = false },
            onConfirm = { customIp ->
                val custom = DnsServer(
                    id = "custom",
                    name = "Custom DNS",
                    ip = customIp,
                    description = "Custom Upstream ($customIp)"
                )
                viewModel.setUpstreamDns(custom)
                showCustomDnsDialog = false
            }
        )
    }
}

@Composable
private fun HeroProtectionCard(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowColor by animateColorAsState(
        targetValue = if (isActive) ShieldGreen else ShieldAmber,
        animationSpec = tween(500),
        label = "glowColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(if (isActive) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isActive) ShieldGreenMuted else ShieldAmberMuted,
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, glowColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Security else Icons.Filled.Shield,
                    contentDescription = "Shield Status",
                    tint = glowColor,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) ShieldGreenMuted else ShieldAmberMuted)
                    .border(1.dp, glowColor.copy(alpha = 0.3f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(glowColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isActive) "ACTIVE (PROTECTED)" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = glowColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isActive) "Local DNS Sinkhole is Filtering Traffic" else "AdShield Protection is Inactive",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = if (isActive) ShieldGreen else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isActive) "Enable DNS Sinkhole" else "Start DNS Protection",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ShieldGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkBorder
                    )
                )
            }
        }
    }
}

@Composable
private fun StatisticsGrid(
    totalQueries: Int,
    totalBlocked: Int,
    blockPercentage: Float,
    activeRulesCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Today's Protection Stats",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "Total Queries",
                value = String.format(Locale.US, "%,d", totalQueries),
                accentColor = ShieldBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Ads Blocked",
                value = String.format(Locale.US, "%,d", totalBlocked),
                accentColor = ShieldRed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Block Ratio",
                value = "${String.format(Locale.US, "%.1f", blockPercentage)}%",
                accentColor = ShieldGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun UpstreamDnsCard(
    currentDns: DnsServer,
    onSelectDns: (DnsServer) -> Unit,
    onCustomDnsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = "DNS",
                        tint = ShieldBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upstream DNS Resolver",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onCustomDnsClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Custom DNS",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Resolved queries forward to: ${currentDns.name} (${currentDns.ip})",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DnsServer.PRESETS) { server ->
                    val isSelected = currentDns.ip == server.ip
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectDns(server) },
                        label = {
                            Text(
                                text = server.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) ShieldGreen else TextSecondary
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = ShieldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShieldGreenMuted,
                            containerColor = DarkSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) ShieldGreen else DarkBorder
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentQueriesPreview(
    recentQueries: List<QueryLogEntity>,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Activity Preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                TextButton(onClick = onViewAllClick) {
                    Text(text = "View Logs →", color = ShieldBlue)
                }
            }

            if (recentQueries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No DNS traffic intercepted yet. Start protection to monitor.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentQueries.take(5).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.domain,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${timeFormat.format(Date(log.timestamp))} • ${log.queryType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            val isBlocked = log.status == QueryStatus.BLOCKED
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isBlocked) ShieldRedMuted else ShieldGreenMuted)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isBlocked) "Blocked" else "Allowed",
                                    color = if (isBlocked) ShieldRed else ShieldGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomDnsDialog(
    initialIp: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var ipText by remember { mutableStateOf(initialIp) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Custom Upstream DNS", color = TextPrimary) },
        text = {
            Column {
                Text(
                    text = "Enter IPv4 address for custom upstream DNS resolver:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = {
                        ipText = it
                        isError = false
                    },
                    isError = isError,
                    label = { Text("IPv4 Address") },
                    placeholder = { Text("e.g. 1.0.0.1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Please enter a valid IPv4 address (e.g. 9.9.9.9)",
                        color = ShieldRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parts = ipText.trim().split(".")
                    val valid = parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }
                    if (valid) {
                        onConfirm(ipText.trim())
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Set DNS", color = ShieldGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
