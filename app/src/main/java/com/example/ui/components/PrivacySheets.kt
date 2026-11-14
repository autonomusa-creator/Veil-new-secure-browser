package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.BlockCategory
import com.example.core.UrlSecurityUtils
import com.example.data.PrivacySettings
import com.example.ui.BrowserTab
import com.example.ui.LinkContextMenuState
import com.example.ui.SecurityAlert

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrivacyInspectorSheet(
    activeTab: BrowserTab,
    settings: PrivacySettings,
    isSiteAllowlisted: Boolean,
    onToggleSiteAllowlist: (String) -> Unit,
    onToggleGlobalAdBlock: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val host = remember(activeTab.url) {
        UrlSecurityUtils.extractHost(activeTab.url).ifBlank { "Start Page" }
    }
    val siteShieldsActive = settings.adBlockEnabled && !isSiteAllowlisted
    val groupedByDomain = remember(activeTab.blockedRequests) {
        activeTab.blockedRequests
            .groupBy { it.domain to it.category }
            .map { (key, list) -> Triple(key.first, key.second, list.size) }
            .sortedByDescending { it.third }
    }
    val countsByCategory = remember(activeTab.blockedRequests) {
        activeTab.blockedRequests.groupingBy { it.category }.eachCount()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (siteShieldsActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (siteShieldsActive) Icons.Rounded.Shield else Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = if (siteShieldsActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (siteShieldsActive) "Veil Shields Up" else "Shields Paused",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = host,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!activeTab.isStartPage) {
                    Switch(
                        checked = siteShieldsActive,
                        onCheckedChange = {
                            if (!settings.adBlockEnabled) {
                                onToggleGlobalAdBlock(true)
                            } else {
                                onToggleSiteAllowlist(host)
                            }
                        },
                        modifier = Modifier.testTag("inspector_site_shield_switch")
                    )
                } else {
                    Switch(
                        checked = settings.adBlockEnabled,
                        onCheckedChange = onToggleGlobalAdBlock,
                        modifier = Modifier.testTag("inspector_global_shield_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat summary card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = activeTab.blockedCount.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Blocked on this page",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = settings.lifetimeBlockedCount.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Lifetime blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (countsByCategory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BlockCategory.entries.forEach { cat ->
                        val count = countsByCategory[cat] ?: 0
                        if (count > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${cat.label}: $count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BLOCKED REQUESTS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (groupedByDomain.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (siteShieldsActive) {
                            "No third-party ads or trackers detected on this page."
                        } else {
                            "Ad & tracker blocking is paused for this site."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(groupedByDomain, key = { "${it.first}:${it.second}" }) { (domain, category, count) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = domain,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = category.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "×$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityDetailsSheet(
    activeTab: BrowserTab,
    settings: PrivacySettings,
    onDismiss: () -> Unit
) {
    val host = UrlSecurityUtils.extractHost(activeTab.url).ifBlank { activeTab.url }
    val hasSslError = activeTab.securityAlert is SecurityAlert.SslCertificateError

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (icon, tint, title) = when {
                    hasSslError -> Triple(
                        Icons.Outlined.GppBad,
                        MaterialTheme.colorScheme.error,
                        "Certificate Untrusted"
                    )
                    activeTab.isHttps -> Triple(
                        Icons.Filled.Lock,
                        MaterialTheme.colorScheme.primary,
                        "Encrypted Connection (HTTPS)"
                    )
                    else -> Triple(
                        Icons.Outlined.LockOpen,
                        MaterialTheme.colorScheme.tertiary,
                        "Unencrypted Connection (HTTP)"
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = host,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            val cert = activeTab.sslInfo
            if (cert != null && activeTab.isHttps) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TLS CERTIFICATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SecurityDetailRow("Issued to", cert.issuedTo)
                        SecurityDetailRow("Issued by", cert.issuedBy)
                        SecurityDetailRow("Valid from", cert.validFrom)
                        SecurityDetailRow("Expires", cert.validUntil)
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "HARDENING STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SecurityDetailRow(
                        "HTTPS-Only Mode",
                        if (settings.httpsOnlyMode) "Enforced" else "Off"
                    )
                    SecurityDetailRow(
                        "Third-Party Cookies",
                        if (settings.blockThirdPartyCookies) "Blocked" else "Allowed"
                    )
                    SecurityDetailRow(
                        "Global Privacy Control",
                        if (settings.sendGpcHeader) "Sec-GPC: 1" else "Off"
                    )
                    SecurityDetailRow(
                        "JavaScript",
                        if (settings.javascriptEnabled) "Enabled" else "Blocked"
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkContextSheet(
    state: LinkContextMenuState,
    onOpenInCurrentTab: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onCopiedLink: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val cleanedUrl = remember(state.url) { UrlSecurityUtils.stripTrackingParameters(state.url) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = cleanedUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ListItem(
                headlineContent = { Text("Open in New Private Tab") },
                leadingContent = {
                    Icon(Icons.Outlined.AddCircleOutline, contentDescription = null)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenInNewTab(cleanedUrl) }
            )
            ListItem(
                headlineContent = { Text("Open Link") },
                leadingContent = {
                    Icon(Icons.Outlined.OpenInBrowser, contentDescription = null)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onDismiss()
                        onOpenInCurrentTab(cleanedUrl)
                    }
            )
            ListItem(
                headlineContent = { Text("Copy Clean Link") },
                supportingContent = { Text("Tracking parameters stripped automatically") },
                leadingContent = {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        clipboard.setText(AnnotatedString(cleanedUrl))
                        onDismiss()
                        onCopiedLink()
                    }
            )
        }
    }
}
