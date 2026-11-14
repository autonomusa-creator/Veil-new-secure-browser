package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.SearchEngine
import com.example.core.UrlSecurityUtils
import com.example.data.BookmarkEntity
import com.example.data.BrowserRepository
import com.example.data.CustomRuleEntity
import com.example.data.PrivacyPreferencesRepository
import com.example.data.PrivacySettings
import com.example.data.VeilDatabase
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BrowserRepository = run {
        val db = VeilDatabase.getInstance(application)
        val prefs = PrivacyPreferencesRepository(application)
        BrowserRepository(db.browserDao(), prefs)
    }

    val settings: StateFlow<PrivacySettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, PrivacySettings())

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customRules: StateFlow<List<CustomRuleEntity>> = repository.customRulesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adBlockSnapshot: StateFlow<AdBlockSnapshot> = combine(
        settings,
        customRules
    ) { currentSettings, rules ->
        AdBlockSnapshot(
            adBlockEnabled = currentSettings.adBlockEnabled,
            customBlockedPatterns = rules.filter { !it.isAllowlist }.map { it.domainPattern }.toSet(),
            allowlistedHosts = rules.filter { it.isAllowlist }.map { it.domainPattern }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AdBlockSnapshot())

    private val initialTabId = UUID.randomUUID().toString()
    private val _tabs = MutableStateFlow(listOf(BrowserTab(id = initialTabId)))
    val tabs: StateFlow<List<BrowserTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(initialTabId)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _activeOverlay = MutableStateFlow(ActiveOverlay.NONE)
    val activeOverlay: StateFlow<ActiveOverlay> = _activeOverlay.asStateFlow()

    private val _linkContextMenu = MutableStateFlow<LinkContextMenuState?>(null)
    val linkContextMenu: StateFlow<LinkContextMenuState?> = _linkContextMenu.asStateFlow()

    private val _commands = MutableSharedFlow<WebViewCommand>(extraBufferCapacity = 32)
    val commands = _commands.asSharedFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.ensureStarterBookmarksSeeded()
        }
    }

    fun currentActiveTab(): BrowserTab {
        val currentId = _activeTabId.value
        return _tabs.value.firstOrNull { it.id == currentId } ?: _tabs.value.first()
    }

    fun setOverlay(overlay: ActiveOverlay) {
        _activeOverlay.value = overlay
    }

    fun dismissOverlay() {
        _activeOverlay.value = ActiveOverlay.NONE
    }

    fun showLinkContextMenu(url: String, title: String = "") {
        if (url.isNotBlank()) {
            _linkContextMenu.value = LinkContextMenuState(url = url, title = title)
        }
    }

    fun dismissLinkContextMenu() {
        _linkContextMenu.value = null
    }

    fun navigateTo(rawInput: String, targetTabId: String = _activeTabId.value) {
        val currentSettings = settings.value
        val resolvedUrl = UrlSecurityUtils.normalizeInputToUrl(
            rawInput = rawInput,
            searchEngine = currentSettings.searchEngine,
            httpsOnly = currentSettings.httpsOnlyMode,
            stripTrackingParams = currentSettings.stripTrackingParams
        )
        if (resolvedUrl.isBlank()) return

        _activeOverlay.value = ActiveOverlay.NONE
        updateTab(targetTabId) { tab ->
            tab.copy(
                url = resolvedUrl,
                title = UrlSecurityUtils.extractHost(resolvedUrl).ifBlank { resolvedUrl },
                isLoading = true,
                progress = 8,
                isHttps = UrlSecurityUtils.isSecureUrl(resolvedUrl),
                securityAlert = SecurityAlert.None,
                pageError = null,
                blockedRequests = emptyList(),
                blockedCount = 0
            )
        }
        _commands.tryEmit(
            WebViewCommand.LoadUrl(
                tabId = targetTabId,
                url = resolvedUrl,
                sendGpc = currentSettings.sendGpcHeader
            )
        )
    }

    fun openUrlInNewTab(rawUrl: String) {
        val currentSettings = settings.value
        val resolvedUrl = UrlSecurityUtils.normalizeInputToUrl(
            rawInput = rawUrl,
            searchEngine = currentSettings.searchEngine,
            httpsOnly = currentSettings.httpsOnlyMode,
            stripTrackingParams = currentSettings.stripTrackingParams
        )
        if (resolvedUrl.isBlank()) return

        val newId = UUID.randomUUID().toString()
        val newTab = BrowserTab(
            id = newId,
            title = UrlSecurityUtils.extractHost(resolvedUrl).ifBlank { "Loading..." },
            url = resolvedUrl,
            isLoading = true,
            progress = 8,
            isHttps = UrlSecurityUtils.isSecureUrl(resolvedUrl)
        )
        _tabs.update { it + newTab }
        _activeTabId.value = newId
        _activeOverlay.value = ActiveOverlay.NONE
        _linkContextMenu.value = null
        _commands.tryEmit(
            WebViewCommand.LoadUrl(
                tabId = newId,
                url = resolvedUrl,
                sendGpc = currentSettings.sendGpcHeader
            )
        )
    }

    fun createNewTab() {
        val newId = UUID.randomUUID().toString()
        _tabs.update { it + BrowserTab(id = newId) }
        _activeTabId.value = newId
        _activeOverlay.value = ActiveOverlay.NONE
    }

    fun selectTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
        _activeOverlay.value = ActiveOverlay.NONE
    }

    fun closeTab(tabId: String) {
        _commands.tryEmit(WebViewCommand.DestroyTab(tabId))
        val currentTabs = _tabs.value
        if (currentTabs.size <= 1) {
            val freshId = UUID.randomUUID().toString()
            _tabs.value = listOf(BrowserTab(id = freshId))
            _activeTabId.value = freshId
            return
        }
        val idx = currentTabs.indexOfFirst { it.id == tabId }
        val remaining = currentTabs.filterNot { it.id == tabId }
        _tabs.value = remaining
        if (_activeTabId.value == tabId) {
            val nextIdx = (idx - 1).coerceIn(0, remaining.lastIndex)
            _activeTabId.value = remaining[nextIdx].id
        }
    }

    fun goBack() {
        val tab = currentActiveTab()
        if (tab.canGoBack) {
            _commands.tryEmit(WebViewCommand.GoBack(tab.id))
        }
    }

    fun goForward() {
        val tab = currentActiveTab()
        if (tab.canGoForward) {
            _commands.tryEmit(WebViewCommand.GoForward(tab.id))
        }
    }

    fun reloadOrStop() {
        val tab = currentActiveTab()
        if (tab.isStartPage) return
        if (tab.isLoading) {
            _commands.tryEmit(WebViewCommand.StopLoading(tab.id))
            updateTab(tab.id) { it.copy(isLoading = false, progress = 0) }
        } else {
            updateTab(tab.id) { it.copy(pageError = null, securityAlert = SecurityAlert.None) }
            _commands.tryEmit(WebViewCommand.Reload(tab.id))
        }
    }

    fun toggleFocusMode() {
        val tab = currentActiveTab()
        if (tab.isStartPage) return
        val next = !tab.isFocusMode
        updateTab(tab.id) { it.copy(isFocusMode = next) }
        _commands.tryEmit(WebViewCommand.SetFocusMode(tab.id, next))
        _snackbarMessages.tryEmit(if (next) "Focus Mode enabled" else "Focus Mode disabled")
    }

    fun toggleDesktopMode() {
        val tab = currentActiveTab()
        if (tab.isStartPage) return
        val next = !tab.isDesktopMode
        updateTab(tab.id) { it.copy(isDesktopMode = next) }
        _commands.tryEmit(WebViewCommand.SetDesktopMode(tab.id, next))
        _snackbarMessages.tryEmit(if (next) "Desktop view requested" else "Mobile view requested")
    }

    fun goHomeStartPage() {
        val tab = currentActiveTab()
        _commands.tryEmit(WebViewCommand.DestroyTab(tab.id))
        updateTab(tab.id) {
            BrowserTab(id = tab.id)
        }
        _activeOverlay.value = ActiveOverlay.NONE
    }

    fun shredSession() {
        _commands.tryEmit(WebViewCommand.ShredAll)
        val freshId = UUID.randomUUID().toString()
        _tabs.value = listOf(BrowserTab(id = freshId))
        _activeTabId.value = freshId
        _activeOverlay.value = ActiveOverlay.NONE
        _linkContextMenu.value = null
        _snackbarMessages.tryEmit("Session shredded — cache, cookies & tabs cleared")
    }

    // --- WebView Callbacks ---

    fun onPageStarted(tabId: String, url: String, canGoBack: Boolean, canGoForward: Boolean) {
        updateTab(tabId) { tab ->
            val isNewPage = UrlSecurityUtils.extractHost(tab.url) != UrlSecurityUtils.extractHost(url)
            tab.copy(
                url = url,
                isLoading = true,
                progress = 12,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isHttps = UrlSecurityUtils.isSecureUrl(url),
                pageError = null,
                securityAlert = SecurityAlert.None,
                blockedRequests = if (isNewPage) emptyList() else tab.blockedRequests,
                blockedCount = if (isNewPage) 0 else tab.blockedCount
            )
        }
    }

    fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) { tab ->
            tab.copy(
                progress = progress,
                isLoading = progress in 1..99
            )
        }
    }

    fun onPageTitleChanged(tabId: String, title: String) {
        if (title.isBlank()) return
        updateTab(tabId) { it.copy(title = title) }
    }

    fun onPageFinished(
        tabId: String,
        url: String,
        title: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
        sslInfo: SslCertificateInfo?
    ) {
        updateTab(tabId) { tab ->
            tab.copy(
                url = url,
                title = title.ifBlank { UrlSecurityUtils.extractHost(url).ifBlank { tab.title } },
                isLoading = false,
                progress = 100,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isHttps = UrlSecurityUtils.isSecureUrl(url),
                sslInfo = sslInfo ?: tab.sslInfo
            )
        }
    }

    fun onPageError(tabId: String, failingUrl: String, description: String) {
        updateTab(tabId) { tab ->
            tab.copy(
                isLoading = false,
                progress = 0,
                pageError = "$description (${UrlSecurityUtils.extractHost(failingUrl)})"
            )
        }
    }

    fun onHttpsUpgradeBlocked(tabId: String, insecureUrl: String, reason: String) {
        updateTab(tabId) { tab ->
            tab.copy(
                isLoading = false,
                progress = 0,
                isHttps = false,
                securityAlert = SecurityAlert.HttpsUpgradePrompt(insecureUrl, reason)
            )
        }
    }

    fun onSslCertificateError(tabId: String, url: String, errorSummary: String) {
        updateTab(tabId) { tab ->
            tab.copy(
                isLoading = false,
                progress = 0,
                isHttps = false,
                securityAlert = SecurityAlert.SslCertificateError(url, errorSummary)
            )
        }
    }

    fun allowHttpOnce(tabId: String, insecureUrl: String) {
        val host = UrlSecurityUtils.extractRawHost(insecureUrl)
        updateTab(tabId) { tab ->
            tab.copy(
                url = insecureUrl,
                isLoading = true,
                progress = 10,
                isHttps = false,
                securityAlert = SecurityAlert.None,
                pageError = null,
                allowedHttpHostsOnce = tab.allowedHttpHostsOnce + host
            )
        }
        _commands.tryEmit(
            WebViewCommand.LoadUrl(
                tabId = tabId,
                url = insecureUrl,
                sendGpc = settings.value.sendGpcHeader
            )
        )
    }

    fun dismissSecurityAlertAndGoBack(tabId: String) {
        val tab = _tabs.value.firstOrNull { it.id == tabId } ?: return
        updateTab(tabId) { it.copy(securityAlert = SecurityAlert.None, pageError = null) }
        if (tab.canGoBack) {
            _commands.tryEmit(WebViewCommand.GoBack(tabId))
        } else {
            goHomeStartPage()
        }
    }

    fun onRequestBlocked(tabId: String, entry: BlockedRequestEntry) {
        updateTab(tabId) { tab ->
            val updatedList = if (tab.blockedRequests.size < 120) {
                tab.blockedRequests + entry
            } else {
                tab.blockedRequests
            }
            tab.copy(
                blockedRequests = updatedList,
                blockedCount = tab.blockedCount + 1
            )
        }
        viewModelScope.launch {
            repository.recordBlockedRequests(1)
        }
    }

    // --- Bookmarks & Custom Rules ---

    fun toggleBookmarkCurrentPage() {
        val tab = currentActiveTab()
        if (tab.isStartPage) return
        val existing = bookmarks.value.any { it.url == tab.url }
        viewModelScope.launch {
            if (existing) {
                repository.removeBookmarkByUrl(tab.url)
                _snackbarMessages.tryEmit("Removed bookmark")
            } else {
                repository.addBookmark(tab.title, tab.url)
                _snackbarMessages.tryEmit("Saved to private bookmarks")
            }
        }
    }

    fun addBookmark(title: String, url: String) {
        viewModelScope.launch {
            repository.addBookmark(title, url)
            _snackbarMessages.tryEmit("Saved to private bookmarks")
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmarkById(id)
        }
    }

    fun toggleSiteAllowlist(host: String) {
        val cleanHost = host.trim().lowercase().removePrefix("www.")
        if (cleanHost.isEmpty()) return
        val isCurrentlyAllowlisted = customRules.value.any { it.isAllowlist && it.domainPattern == cleanHost }
        viewModelScope.launch {
            if (isCurrentlyAllowlisted) {
                repository.removeCustomRuleByDomain(cleanHost, isAllowlist = true)
                _snackbarMessages.tryEmit("Shields restored for $cleanHost")
            } else {
                repository.addCustomRule(cleanHost, isAllowlist = true)
                _snackbarMessages.tryEmit("Shields paused for $cleanHost")
            }
            val active = currentActiveTab()
            if (!active.isStartPage) {
                _commands.tryEmit(WebViewCommand.Reload(active.id))
            }
        }
    }

    fun addCustomRule(domainPattern: String, isAllowlist: Boolean) {
        viewModelScope.launch {
            repository.addCustomRule(domainPattern, isAllowlist)
        }
    }

    fun removeCustomRule(id: Long) {
        viewModelScope.launch {
            repository.removeCustomRuleById(id)
        }
    }

    // --- Settings Updates ---

    fun setAdBlockEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setAdBlockEnabled(enabled) }
    }

    fun setCosmeticBlockingEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setCosmeticBlockingEnabled(enabled) }
    }

    fun setHttpsOnlyMode(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setHttpsOnlyMode(enabled) }
    }

    fun setBlockThirdPartyCookies(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setBlockThirdPartyCookies(enabled) }
    }

    fun setStripTrackingParams(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setStripTrackingParams(enabled) }
    }

    fun setJavascriptEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setJavascriptEnabled(enabled) }
    }

    fun setEphemeralMode(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setEphemeralMode(enabled) }
    }

    fun setSendGpcHeader(enabled: Boolean) = viewModelScope.launch {
        repository.updateSettings { setSendGpcHeader(enabled) }
    }

    fun setSearchEngine(engine: SearchEngine) = viewModelScope.launch {
        repository.updateSettings { setSearchEngine(engine) }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        repository.updateSettings { setThemeMode(mode) }
    }

    fun notifyMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    private fun updateTab(tabId: String, transform: (BrowserTab) -> BrowserTab) {
        _tabs.update { list ->
            list.map { if (it.id == tabId) transform(it) else it }
        }
    }
}
