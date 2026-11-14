package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Https
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.BookmarkEntity
import com.example.data.PrivacySettings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VeilStartPage(
    settings: PrivacySettings,
    bookmarks: List<BookmarkEntity>,
    onNavigateToUrl: (String) -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleHttpsOnly: (Boolean) -> Unit,
    onToggleStripTrackers: (Boolean) -> Unit,
    onToggleEphemeral: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shieldColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(bgColor)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            shieldColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.24f),
                        radius = size.minDimension * 0.72f
                    ),
                    radius = size.minDimension * 0.72f,
                    center = Offset(size.width * 0.5f, size.height * 0.24f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            VeilEmblem(
                shieldColor = shieldColor,
                isActive = settings.adBlockEnabled
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSettings)
                    .testTag("start_privacy_status_pill"),
                color = if (settings.adBlockEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (settings.adBlockEnabled) Icons.Rounded.Shield else Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = if (settings.adBlockEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (settings.adBlockEnabled) {
                            "${stringResource(R.string.shields_active)} • ${settings.lifetimeBlockedCount} blocked"
                        } else {
                            stringResource(R.string.shields_paused)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (settings.adBlockEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Privacy Protections Strip
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "ACTIVE PROTECTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProtectionToggleChip(
                        label = "Ad & Tracker Block",
                        icon = Icons.Outlined.Shield,
                        selected = settings.adBlockEnabled,
                        onClick = { onToggleAdBlock(!settings.adBlockEnabled) },
                        testTag = "chip_adblock"
                    )
                    ProtectionToggleChip(
                        label = "HTTPS-Only",
                        icon = Icons.Outlined.Https,
                        selected = settings.httpsOnlyMode,
                        onClick = { onToggleHttpsOnly(!settings.httpsOnlyMode) },
                        testTag = "chip_https_only"
                    )
                    ProtectionToggleChip(
                        label = "Strip URL Trackers",
                        icon = Icons.Outlined.LinkOff,
                        selected = settings.stripTrackingParams,
                        onClick = { onToggleStripTrackers(!settings.stripTrackingParams) },
                        testTag = "chip_strip_trackers"
                    )
                    ProtectionToggleChip(
                        label = "Ephemeral",
                        icon = Icons.Outlined.VisibilityOff,
                        selected = settings.ephemeralMode,
                        onClick = { onToggleEphemeral(!settings.ephemeralMode) },
                        testTag = "chip_ephemeral"
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Private Bookmarks Section
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRIVATE PINS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bookmarks.isNotEmpty()) {
                        Text(
                            text = "Manage",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(onClick = onOpenBookmarks)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("manage_bookmarks_button")
                        )
                    }
                }

                if (bookmarks.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No local bookmarks yet. Pin any site from the browser menu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        bookmarks.take(8).forEach { bookmark ->
                            BookmarkPinCard(
                                title = bookmark.title,
                                host = bookmark.host,
                                onClick = { onNavigateToUrl(bookmark.url) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VeilEmblem(
    shieldColor: Color,
    isActive: Boolean
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = if (isActive) shieldColor else muted
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val shieldPath = Path().apply {
                moveTo(w * 0.50f, h * 0.08f)
                lineTo(w * 0.86f, h * 0.23f)
                cubicTo(w * 0.86f, h * 0.56f, w * 0.72f, h * 0.80f, w * 0.50f, h * 0.92f)
                cubicTo(w * 0.28f, h * 0.80f, w * 0.14f, h * 0.56f, w * 0.14f, h * 0.23f)
                close()
            }
            drawPath(
                path = shieldPath,
                color = activeColor.copy(alpha = 0.14f)
            )
            drawPath(
                path = shieldPath,
                color = activeColor,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            val innerPath = Path().apply {
                moveTo(w * 0.36f, h * 0.48f)
                lineTo(w * 0.50f, h * 0.62f)
                lineTo(w * 0.66f, h * 0.38f)
            }
            drawPath(
                path = innerPath,
                color = activeColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun ProtectionToggleChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun BookmarkPinCard(
    title: String,
    host: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(horizontal = 14.dp, vertical = 11.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (host.isNotBlank()) {
                    Text(
                        text = host,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
