package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.UrlSecurityUtils
import com.example.ui.ActiveOverlay
import com.example.ui.BrowserViewModel
import com.example.ui.components.BookmarksSheet
import com.example.ui.components.BottomOmnibar
import com.example.ui.components.LinkContextSheet
import com.example.ui.components.PrivacyInspectorSheet
import com.example.ui.components.SecurityDetailsSheet
import com.example.ui.components.TabSwitcherSheet
import com.example.ui.components.TopAddressBar
import com.example.ui.components.VeilWebViewHost
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VeilStartPage
import com.example.ui.theme.VeilTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeilApp(viewModel = viewModel)
        }
    }
}

@Composable
fun VeilApp(viewModel: BrowserViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val activeOverlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val customRules by viewModel.customRules.collectAsStateWithLifecycle()
    val adBlockSnapshot by viewModel.adBlockSnapshot.collectAsStateWithLifecycle()
    val linkContextMenu by viewModel.linkContextMenu.collectAsStateWithLifecycle()

    val activeTab = remember(tabs, activeTabId) {
        tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    }
    val isBookmarked = remember(bookmarks, activeTab.url) {
        !activeTab.isStartPage && bookmarks.any { it.url == activeTab.url }
    }
    val currentHost = remember(activeTab.url) {
        UrlSecurityUtils.extractHost(activeTab.url)
    }
    val isSiteAllowlisted = remember(customRules, currentHost) {
        currentHost.isNotEmpty() && customRules.any { it.isAllowlist && it.domainPattern == currentHost }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle system back inside active web page
    BackHandler(enabled = activeOverlay == ActiveOverlay.NONE && !activeTab.isStartPage) {
        if (activeTab.canGoBack) {
            viewModel.goBack()
        } else {
            viewModel.goHomeStartPage()
        }
    }

    VeilTheme(themeMode = settings.themeMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                ) {
                    // Persistent Top-Aligned URL / Search Input Bar
                    TopAddressBar(
                        activeTab = activeTab,
                        adBlockEnabled = settings.adBlockEnabled && !isSiteAllowlisted,
                        searchEngine = settings.searchEngine,
                        bookmarks = bookmarks,
                        onSubmitInput = { input -> viewModel.navigateTo(input) },
                        onSelectSearchEngine = viewModel::setSearchEngine,
                        onOpenPrivacyInspector = { viewModel.setOverlay(ActiveOverlay.PRIVACY_INSPECTOR) },
                        onOpenSecurityDetails = { viewModel.setOverlay(ActiveOverlay.SECURITY_DETAILS) },
                        onReloadOrStop = viewModel::reloadOrStop
                    )

                    // Main Content Viewport
                    Box(modifier = Modifier.weight(1f)) {
                        VeilWebViewHost(
                            activeTab = activeTab,
                            allTabs = tabs,
                            settings = settings,
                            adBlockSnapshot = adBlockSnapshot,
                            commands = viewModel.commands,
                            onPageStarted = viewModel::onPageStarted,
                            onProgressChanged = viewModel::onProgressChanged,
                            onTitleChanged = viewModel::onPageTitleChanged,
                            onPageFinished = viewModel::onPageFinished,
                            onPageError = viewModel::onPageError,
                            onHttpsUpgradeBlocked = viewModel::onHttpsUpgradeBlocked,
                            onSslCertificateError = viewModel::onSslCertificateError,
                            onRequestBlocked = viewModel::onRequestBlocked,
                            onOpenNewTab = viewModel::openUrlInNewTab,
                            onLongPressLink = { url -> viewModel.showLinkContextMenu(url) },
                            onAllowHttpOnce = viewModel::allowHttpOnce,
                            onDismissAlertBack = viewModel::dismissSecurityAlertAndGoBack,
                            onReload = viewModel::reloadOrStop
                        )

                        if (activeTab.isStartPage) {
                            VeilStartPage(
                                settings = settings,
                                bookmarks = bookmarks,
                                onNavigateToUrl = { url -> viewModel.navigateTo(url) },
                                onOpenBookmarks = { viewModel.setOverlay(ActiveOverlay.BOOKMARKS) },
                                onOpenSettings = { viewModel.setOverlay(ActiveOverlay.SETTINGS) },
                                onToggleAdBlock = viewModel::setAdBlockEnabled,
                                onToggleHttpsOnly = viewModel::setHttpsOnlyMode,
                                onToggleStripTrackers = viewModel::setStripTrackingParams,
                                onToggleEphemeral = viewModel::setEphemeralMode
                            )
                        }
                    }

                    // Slim Bottom Navigation Toolbar
                    BottomOmnibar(
                        activeTab = activeTab,
                        tabCount = tabs.size,
                        isBookmarked = isBookmarked,
                        onBack = viewModel::goBack,
                        onForward = viewModel::goForward,
                        onToggleFocusMode = viewModel::toggleFocusMode,
                        onOpenTabSwitcher = { viewModel.setOverlay(ActiveOverlay.TAB_SWITCHER) },
                        onShredSession = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.shredSession()
                        },
                        onToggleBookmark = viewModel::toggleBookmarkCurrentPage,
                        onOpenBookmarks = { viewModel.setOverlay(ActiveOverlay.BOOKMARKS) },
                        onToggleDesktopMode = viewModel::toggleDesktopMode,
                        onGoHome = viewModel::goHomeStartPage,
                        onOpenSettings = { viewModel.setOverlay(ActiveOverlay.SETTINGS) }
                    )
                }

                // Full-screen Settings Screen
                AnimatedVisibility(
                    visible = activeOverlay == ActiveOverlay.SETTINGS,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SettingsScreen(
                        settings = settings,
                        customRules = customRules,
                        onBack = viewModel::dismissOverlay,
                        onToggleAdBlock = viewModel::setAdBlockEnabled,
                        onToggleCosmeticBlock = viewModel::setCosmeticBlockingEnabled,
                        onToggleStripTrackers = viewModel::setStripTrackingParams,
                        onToggleHttpsOnly = viewModel::setHttpsOnlyMode,
                        onToggleThirdPartyCookies = viewModel::setBlockThirdPartyCookies,
                        onToggleGpcHeader = viewModel::setSendGpcHeader,
                        onToggleJavascript = viewModel::setJavascriptEnabled,
                        onToggleEphemeral = viewModel::setEphemeralMode,
                        onSelectSearchEngine = viewModel::setSearchEngine,
                        onSelectThemeMode = viewModel::setThemeMode,
                        onAddCustomRule = viewModel::addCustomRule,
                        onRemoveCustomRule = viewModel::removeCustomRule,
                        onShredAllData = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.shredSession()
                        }
                    )
                }

                // Bottom Sheets
                when (activeOverlay) {
                    ActiveOverlay.PRIVACY_INSPECTOR -> {
                        PrivacyInspectorSheet(
                            activeTab = activeTab,
                            settings = settings,
                            isSiteAllowlisted = isSiteAllowlisted,
                            onToggleSiteAllowlist = viewModel::toggleSiteAllowlist,
                            onToggleGlobalAdBlock = viewModel::setAdBlockEnabled,
                            onDismiss = viewModel::dismissOverlay
                        )
                    }
                    ActiveOverlay.SECURITY_DETAILS -> {
                        SecurityDetailsSheet(
                            activeTab = activeTab,
                            settings = settings,
                            onDismiss = viewModel::dismissOverlay
                        )
                    }
                    ActiveOverlay.TAB_SWITCHER -> {
                        TabSwitcherSheet(
                            tabs = tabs,
                            activeTabId = activeTab.id,
                            onSelectTab = viewModel::selectTab,
                            onCloseTab = viewModel::closeTab,
                            onCreateNewTab = viewModel::createNewTab,
                            onShredAll = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.shredSession()
                            },
                            onDismiss = viewModel::dismissOverlay
                        )
                    }
                    ActiveOverlay.BOOKMARKS -> {
                        BookmarksSheet(
                            bookmarks = bookmarks,
                            activeTab = activeTab,
                            onOpenBookmark = { url -> viewModel.navigateTo(url) },
                            onAddCurrentPage = viewModel::toggleBookmarkCurrentPage,
                            onDeleteBookmark = viewModel::deleteBookmark,
                            onDismiss = viewModel::dismissOverlay
                        )
                    }
                    else -> Unit
                }

                linkContextMenu?.let { menuState ->
                    LinkContextSheet(
                        state = menuState,
                        onOpenInCurrentTab = { url -> viewModel.navigateTo(url) },
                        onOpenInNewTab = { url -> viewModel.openUrlInNewTab(url) },
                        onCopiedLink = { viewModel.notifyMessage("Clean link copied") },
                        onDismiss = viewModel::dismissLinkContextMenu
                    )
                }
            }
        }
    }
}
