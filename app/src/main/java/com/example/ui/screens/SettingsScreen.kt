package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.SearchEngine
import com.example.data.CustomRuleEntity
import com.example.data.PrivacySettings
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: PrivacySettings,
    customRules: List<CustomRuleEntity>,
    onBack: () -> Unit,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleCosmeticBlock: (Boolean) -> Unit,
    onToggleStripTrackers: (Boolean) -> Unit,
    onToggleHttpsOnly: (Boolean) -> Unit,
    onToggleThirdPartyCookies: (Boolean) -> Unit,
    onToggleGpcHeader: (Boolean) -> Unit,
    onToggleJavascript: (Boolean) -> Unit,
    onToggleEphemeral: (Boolean) -> Unit,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onAddCustomRule: (String, Boolean) -> Unit,
    onRemoveCustomRule: (Long) -> Unit,
    onShredAllData: () -> Unit
) {
    BackHandler(onBack = onBack)

    var newRuleDomain by remember { mutableStateOf("") }
    var newRuleIsAllowlist by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Privacy Stat Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "${settings.lifetimeBlockedCount} ads & trackers blocked",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "All filtering happens locally on your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Ad & Tracker Blocking Section
                SettingsSectionCard(title = "AD & TRACKER SHIELD") {
                    SettingsSwitchRow(
                        title = "Ad & Tracker Blocking",
                        subtitle = "Block ad networks, analytics, and social pixels before they load",
                        checked = settings.adBlockEnabled,
                        onCheckedChange = onToggleAdBlock,
                        testTag = "setting_adblock"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Cosmetic Ad Hiding",
                        subtitle = "Collapse leftover blank ad slots and sponsored containers",
                        checked = settings.cosmeticBlockingEnabled,
                        onCheckedChange = onToggleCosmeticBlock,
                        testTag = "setting_cosmetic"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Strip URL Tracking Parameters",
                        subtitle = "Remove fbclid, gclid, and utm_* tags from links automatically",
                        checked = settings.stripTrackingParams,
                        onCheckedChange = onToggleStripTrackers,
                        testTag = "setting_strip_params"
                    )
                }

                // Security Hardening Section
                SettingsSectionCard(title = "SECURITY & HARDENING") {
                    SettingsSwitchRow(
                        title = "HTTPS-Only Mode",
                        subtitle = "Upgrade links to HTTPS and warn before loading insecure HTTP",
                        checked = settings.httpsOnlyMode,
                        onCheckedChange = onToggleHttpsOnly,
                        testTag = "setting_https_only"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Block Third-Party Cookies",
                        subtitle = "Prevent cross-site tracking cookies across domains",
                        checked = settings.blockThirdPartyCookies,
                        onCheckedChange = onToggleThirdPartyCookies,
                        testTag = "setting_3p_cookies"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Send Global Privacy Control",
                        subtitle = "Attach Sec-GPC: 1 and DNT: 1 headers to page requests",
                        checked = settings.sendGpcHeader,
                        onCheckedChange = onToggleGpcHeader,
                        testTag = "setting_gpc"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "JavaScript",
                        subtitle = "Disable for maximum attack-surface reduction on untrusted sites",
                        checked = settings.javascriptEnabled,
                        onCheckedChange = onToggleJavascript,
                        testTag = "setting_js"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        title = "Ephemeral Browsing",
                        subtitle = "Avoid persisting cache or site databases between sessions",
                        checked = settings.ephemeralMode,
                        onCheckedChange = onToggleEphemeral,
                        testTag = "setting_ephemeral"
                    )
                }

                // Custom Domain Filter Rules
                SettingsSectionCard(title = "CUSTOM DOMAIN RULES") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !newRuleIsAllowlist,
                                onClick = { newRuleIsAllowlist = false },
                                label = { Text("Block Domain") }
                            )
                            FilterChip(
                                selected = newRuleIsAllowlist,
                                onClick = { newRuleIsAllowlist = true },
                                label = { Text("Allowlist Site") }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newRuleDomain,
                                onValueChange = { newRuleDomain = it },
                                placeholder = { Text("e.g. tracker.example.com") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_rule_input")
                            )
                            Button(
                                onClick = {
                                    if (newRuleDomain.isNotBlank()) {
                                        onAddCustomRule(newRuleDomain, newRuleIsAllowlist)
                                        newRuleDomain = ""
                                    }
                                },
                                modifier = Modifier.testTag("custom_rule_add_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Add rule",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (customRules.isNotEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            customRules.forEach { rule ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rule.domainPattern,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (rule.isAllowlist) "Allowlisted (shields paused)" else "Blocked",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (rule.isAllowlist) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                    IconButton(onClick = { onRemoveCustomRule(rule.id) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DeleteOutline,
                                            contentDescription = "Delete rule",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Engine & Appearance
                SettingsSectionCard(title = "SEARCH & APPEARANCE") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Default Search Engine",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchEngine.entries.forEach { engine ->
                                FilterChip(
                                    selected = engine == settings.searchEngine,
                                    onClick = { onSelectSearchEngine(engine) },
                                    label = { Text(engine.displayName) }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = mode == settings.themeMode,
                                    onClick = { onSelectThemeMode(mode) },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                    }
                }

                // Shredder Action
                Button(
                    onClick = onShredAllData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_shred_all_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shred All Cookies, Cache & Open Tabs")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
