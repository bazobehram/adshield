package com.local.adshield.ui.screens.rules

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.local.adshield.data.model.BlocklistSource
import com.local.adshield.data.model.RuleEntity
import com.local.adshield.data.model.RuleType
import com.local.adshield.ui.theme.DarkBackground
import com.local.adshield.ui.theme.DarkBorder
import com.local.adshield.ui.theme.DarkSurface
import com.local.adshield.ui.theme.DarkSurfaceVariant
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
fun RulesScreen(
    viewModel: RulesViewModel,
    modifier: Modifier = Modifier
) {
    val currentSubTab by viewModel.currentSubTab.collectAsState()
    val customRuleTypeFilter by viewModel.customRuleTypeFilter.collectAsState()
    val customRules by viewModel.customRules.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()

    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showAddSubscriptionDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentSubTab == RulesSubTab.CUSTOM_RULES) {
                        showAddRuleDialog = true
                    } else {
                        showAddSubscriptionDialog = true
                    }
                },
                containerColor = ShieldGreen,
                contentColor = DarkBackground,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = currentSubTab.ordinal,
                containerColor = DarkSurface,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentSubTab.ordinal]),
                        color = ShieldGreen
                    )
                },
                divider = {}
            ) {
                Tab(
                    selected = currentSubTab == RulesSubTab.CUSTOM_RULES,
                    onClick = { viewModel.setSubTab(RulesSubTab.CUSTOM_RULES) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (currentSubTab == RulesSubTab.CUSTOM_RULES) ShieldGreen else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Custom Rules")
                        }
                    }
                )
                Tab(
                    selected = currentSubTab == RulesSubTab.SUBSCRIPTIONS,
                    onClick = { viewModel.setSubTab(RulesSubTab.SUBSCRIPTIONS) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.RssFeed,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (currentSubTab == RulesSubTab.SUBSCRIPTIONS) ShieldGreen else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Blocklists & Feeds")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (currentSubTab) {
                RulesSubTab.CUSTOM_RULES -> {
                    CustomRulesContent(
                        rules = customRules,
                        typeFilter = customRuleTypeFilter,
                        onFilterChange = { viewModel.setCustomRuleTypeFilter(it) },
                        onToggle = { viewModel.toggleCustomRule(it) },
                        onDelete = { viewModel.deleteCustomRule(it) }
                    )
                }

                RulesSubTab.SUBSCRIPTIONS -> {
                    SubscriptionsContent(
                        subscriptions = subscriptions,
                        isSyncing = isSyncing,
                        syncStatusMessage = syncStatusMessage,
                        onSyncAll = { viewModel.syncAllSubscriptions() },
                        onToggle = { viewModel.toggleSubscription(it) },
                        onDelete = { viewModel.deleteSubscription(it) },
                        onSyncSingle = { viewModel.syncSingleSubscription(it) }
                    )
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            initialType = customRuleTypeFilter,
            onDismiss = { showAddRuleDialog = false },
            onConfirm = { domain, type, note ->
                viewModel.addCustomRule(domain, type, note)
                showAddRuleDialog = false
            }
        )
    }

    if (showAddSubscriptionDialog) {
        AddSubscriptionDialog(
            onDismiss = { showAddSubscriptionDialog = false },
            onConfirm = { name, url ->
                viewModel.addSubscription(name, url)
                showAddSubscriptionDialog = false
            }
        )
    }
}

@Composable
private fun CustomRulesContent(
    rules: List<RuleEntity>,
    typeFilter: RuleType,
    onFilterChange: (RuleType) -> Unit,
    onToggle: (RuleEntity) -> Unit,
    onDelete: (RuleEntity) -> Unit
) {
    val filteredRules = rules.filter { it.type == typeFilter }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (typeFilter == RuleType.WHITELIST) ShieldGreenMuted else Color.Transparent)
                    .border(
                        1.dp,
                        if (typeFilter == RuleType.WHITELIST) ShieldGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onFilterChange(RuleType.WHITELIST) }) {
                    Text(
                        text = "Whitelist (${rules.count { it.type == RuleType.WHITELIST }})",
                        color = if (typeFilter == RuleType.WHITELIST) ShieldGreen else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (typeFilter == RuleType.BLACKLIST) ShieldRedMuted else Color.Transparent)
                    .border(
                        1.dp,
                        if (typeFilter == RuleType.BLACKLIST) ShieldRed else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onFilterChange(RuleType.BLACKLIST) }) {
                    Text(
                        text = "Blacklist (${rules.count { it.type == RuleType.BLACKLIST }})",
                        color = if (typeFilter == RuleType.BLACKLIST) ShieldRed else TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredRules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No custom ${if (typeFilter == RuleType.WHITELIST) "whitelist" else "blacklist"} rules yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the + button below to add your first domain rule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredRules, key = { it.id }) { rule ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rule.domain,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (!rule.note.isNullOrBlank()) {
                                    Text(
                                        text = rule.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { onToggle(rule) },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = if (rule.type == RuleType.WHITELIST) ShieldGreen else ShieldRed
                                    )
                                )
                                IconButton(onClick = { onDelete(rule) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsContent(
    subscriptions: List<BlocklistSource>,
    isSyncing: Boolean,
    syncStatusMessage: String?,
    onSyncAll: () -> Unit,
    onToggle: (BlocklistSource) -> Unit,
    onDelete: (BlocklistSource) -> Unit,
    onSyncSingle: (Long) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onSyncAll,
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ShieldGreen,
                contentColor = DarkBackground
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = DarkBackground,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Syncing Blocklists...", fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Filled.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Sync / Update Now", fontWeight = FontWeight.Bold)
            }
        }

        AnimatedVisibility(visible = !syncStatusMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = syncStatusMessage ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ShieldGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(subscriptions, key = { it.id }) { source ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = source.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1
                                )
                            }
                            Switch(
                                checked = source.enabled,
                                onCheckedChange = { onToggle(source) },
                                colors = SwitchDefaults.colors(checkedTrackColor = ShieldGreen)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(ShieldBlue.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${String.format(Locale.US, "%,d", source.ruleCount)} rules",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ShieldBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (source.last_synced != null) {
                                        "Updated: ${dateFormatter.format(Date(source.last_synced))}"
                                    } else {
                                        "Not yet synced"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            Row {
                                IconButton(onClick = { onSyncSingle(source.id) }, enabled = !isSyncing) {
                                    Icon(
                                        imageVector = Icons.Filled.Sync,
                                        contentDescription = "Sync feed",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { onDelete(source) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddRuleDialog(
    initialType: RuleType,
    onDismiss: () -> Unit,
    onConfirm: (domain: String, type: RuleType, note: String?) -> Unit
) {
    var domainText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Custom Rule", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = domainText,
                    onValueChange = {
                        domainText = it
                        isError = false
                    },
                    label = { Text("Domain Name") },
                    placeholder = { Text("e.g. ads.example.com or *.tracker.com") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Please enter a valid domain name",
                        color = ShieldRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. Block telemetry") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Rule Type",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == RuleType.WHITELIST,
                        onClick = { selectedType = RuleType.WHITELIST },
                        colors = RadioButtonDefaults.colors(selectedColor = ShieldGreen)
                    )
                    Text("Whitelist (Always Allow)", color = TextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == RuleType.BLACKLIST,
                        onClick = { selectedType = RuleType.BLACKLIST },
                        colors = RadioButtonDefaults.colors(selectedColor = ShieldRed)
                    )
                    Text("Blacklist (Sinkhole)", color = TextPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clean = domainText.trim()
                    if (clean.isNotEmpty() && clean.contains(".")) {
                        onConfirm(clean, selectedType, noteText.trim().ifEmpty { null })
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Add Rule", color = ShieldGreen)
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

@Composable
private fun AddSubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Blocklist Feed", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Feed Name") },
                    placeholder = { Text("e.g. Malware Domains List") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        isError = false
                    },
                    label = { Text("Hosts / Domain Feed URL") },
                    placeholder = { Text("https://example.com/hosts.txt") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Please enter a valid HTTP(S) URL",
                        color = ShieldRed,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val url = urlText.trim()
                    val name = nameText.trim().ifEmpty { "Custom Feed" }
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        onConfirm(name, url)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Add Feed", color = ShieldGreen)
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
