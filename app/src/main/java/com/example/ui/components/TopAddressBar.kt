package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPasteGo
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.SearchEngine
import com.example.core.UrlSecurityUtils
import com.example.data.BookmarkEntity
import com.example.ui.BrowserTab
import com.example.ui.SecurityAlert

@Composable
fun TopAddressBar(
    activeTab: BrowserTab,
    adBlockEnabled: Boolean,
    searchEngine: SearchEngine,
    bookmarks: List<BookmarkEntity>,
    onSubmitInput: (String) -> Unit,
    onSelectSearchEngine: (SearchEngine) -> Unit,
    onOpenPrivacyInspector: () -> Unit,
    onOpenSecurityDetails: () -> Unit,
    onReloadOrStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

    val unfocusedDisplay = remember(activeTab.id, activeTab.url) {
        if (activeTab.isStartPage) {
            ""
        } else {
            UrlSecurityUtils.extractHost(activeTab.url).ifBlank { activeTab.url }
        }
    }

    LaunchedEffect(activeTab.id, activeTab.url, isFocused) {
        if (!isFocused) {
            textFieldValue = TextFieldValue(unfocusedDisplay)
        }
    }

    BackHandler(enabled = isFocused) {
        focusManager.clearFocus()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (activeTab.isLoading) (activeTab.progress / 100f).coerceIn(0.05f, 1f) else 0f,
        label = "top_bar_progress"
    )

    val submitCurrentText: () -> Unit = {
        val query = textFieldValue.text.trim()
        if (query.isNotEmpty()) {
            focusManager.clearFocus()
            onSubmitInput(query)
        }
    }

    val clipboardText = remember(isFocused) {
        if (isFocused) clipboard.getText()?.text?.trim().orEmpty() else ""
    }

    val matchingBookmarks = remember(isFocused, textFieldValue.text, bookmarks) {
        if (!isFocused) {
            emptyList()
        } else {
            val q = textFieldValue.text.trim().lowercase()
            if (q.isEmpty()) {
                bookmarks.take(4)
            } else {
                bookmarks.filter {
                    it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
                }.take(4)
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = if (isFocused) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Surface(
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.background
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    },
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = if (isFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leading security / search icon
                        IconButton(
                            onClick = {
                                if (!activeTab.isStartPage && !isFocused) {
                                    onOpenSecurityDetails()
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("top_bar_security_button")
                        ) {
                            val (icon, tint) = when {
                                isFocused || activeTab.isStartPage ->
                                    Icons.Outlined.Search to MaterialTheme.colorScheme.primary
                                activeTab.securityAlert is SecurityAlert.SslCertificateError ->
                                    Icons.Outlined.GppBad to MaterialTheme.colorScheme.error
                                !activeTab.isHttps ->
                                    Icons.Outlined.LockOpen to MaterialTheme.colorScheme.tertiary
                                else ->
                                    Icons.Filled.Lock to MaterialTheme.colorScheme.primary
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(R.string.cd_security_info),
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Persistent editable URL / Search input field
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_or_enter_url),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { textFieldValue = it },
                                singleLine = true,
                                textStyle = (if (isFocused || !activeTab.isStartPage) {
                                    MaterialTheme.typography.labelMedium
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                }).copy(color = MaterialTheme.colorScheme.onSurface),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = KeyboardActions(
                                    onGo = { submitCurrentText() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        val nowFocused = focusState.isFocused
                                        if (nowFocused != isFocused) {
                                            isFocused = nowFocused
                                            if (nowFocused) {
                                                val editableText = if (activeTab.isStartPage) "" else activeTab.url
                                                textFieldValue = TextFieldValue(
                                                    text = editableText,
                                                    selection = TextRange(0, editableText.length)
                                                )
                                            }
                                        }
                                    }
                                    .testTag("top_url_input")
                            )
                        }

                        if (isFocused) {
                            if (textFieldValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = { textFieldValue = TextFieldValue("") },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_url_clear_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = "Clear address",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = submitCurrentText,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_url_go_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = "Go",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { focusManager.clearFocus() },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_url_cancel_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.cd_close),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            // Live Ad-Block Shield Counter Pill
                            Surface(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .clickable(onClick = onOpenPrivacyInspector)
                                    .testTag("top_bar_shield_button"),
                                color = if (adBlockEnabled) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = CircleShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (adBlockEnabled) Icons.Rounded.Shield else Icons.Outlined.Shield,
                                        contentDescription = stringResource(R.string.cd_shield_inspector),
                                        tint = if (adBlockEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = activeTab.blockedCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (adBlockEnabled) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }

                            if (!activeTab.isStartPage) {
                                IconButton(
                                    onClick = onReloadOrStop,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("top_bar_reload_button")
                                ) {
                                    Icon(
                                        imageVector = if (activeTab.isLoading) Icons.Outlined.Close else Icons.Outlined.Refresh,
                                        contentDescription = stringResource(
                                            if (activeTab.isLoading) R.string.cd_stop else R.string.cd_reload
                                        ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Inline Quick Suggestions & Search Engine Strip while focused
                AnimatedVisibility(visible = isFocused) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (clipboardText.isNotEmpty() && clipboardText != textFieldValue.text) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable {
                                        focusManager.clearFocus()
                                        onSubmitInput(clipboardText)
                                    }
                                    .testTag("top_bar_paste_go"),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentPasteGo,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = "${stringResource(R.string.paste_and_go)}: $clipboardText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SearchEngine.entries.forEach { engine ->
                                FilterChip(
                                    selected = engine == searchEngine,
                                    onClick = { onSelectSearchEngine(engine) },
                                    label = {
                                        Text(
                                            text = engine.displayName,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                )
                            }
                        }

                        if (matchingBookmarks.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                matchingBookmarks.forEach { bookmark ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.small)
                                            .clickable {
                                                focusManager.clearFocus()
                                                onSubmitInput(bookmark.url)
                                            },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.BookmarkBorder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = bookmark.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = bookmark.host,
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
                    }
                }
            }

            AnimatedVisibility(visible = activeTab.isLoading) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            if (!activeTab.isLoading) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
