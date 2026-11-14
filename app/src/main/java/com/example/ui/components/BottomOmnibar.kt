package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.BrowserTab

@Composable
fun BottomOmnibar(
    activeTab: BrowserTab,
    tabCount: Int,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onOpenTabSwitcher: () -> Unit,
    onShredSession: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onGoHome: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = activeTab.canGoBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = if (activeTab.canGoBack) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = activeTab.canGoForward,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = stringResource(R.string.cd_forward),
                        tint = if (activeTab.canGoForward) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        }
                    )
                }

                IconButton(
                    onClick = onToggleFocusMode,
                    enabled = !activeTab.isStartPage,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_focus_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = stringResource(R.string.cd_focus_mode),
                        tint = when {
                            activeTab.isStartPage -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            activeTab.isFocusMode -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTabSwitcher)
                        .testTag("nav_tabs_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.6.dp, MaterialTheme.colorScheme.onSurface),
                        color = Color.Transparent,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (tabCount > 99) "∞" else tabCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onShredSession,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_shred_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = stringResource(R.string.cd_shred),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("nav_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.cd_menu),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (!activeTab.isStartPage) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (isBookmarked) "Remove Bookmark" else "Bookmark Page")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleBookmark()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(if (activeTab.isDesktopMode) "Mobile View" else "Desktop View")
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DesktopWindows, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleDesktopMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Start Page") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Home, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onGoHome()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.bookmarks_title)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenBookmarks()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Settings, contentDescription = null)
                            },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            }
                        )
                    }
                }
            }
        }
    }
}
