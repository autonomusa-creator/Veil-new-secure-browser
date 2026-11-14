package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppBad
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.core.AdBlockEngine
import com.example.core.UrlSecurityUtils
import com.example.data.PrivacySettings
import com.example.ui.AdBlockSnapshot
import com.example.ui.BlockedRequestEntry
import com.example.ui.BrowserTab
import com.example.ui.SecurityAlert
import com.example.ui.SslCertificateInfo
import com.example.ui.WebViewCommand
import kotlinx.coroutines.flow.SharedFlow
import java.text.DateFormat
import java.util.concurrent.ConcurrentHashMap

class WebViewPool {
    private val views = mutableMapOf<String, WebView>()
    private val autoUpgradedTimestamps = ConcurrentHashMap<String, Long>()

    @Volatile
    var adBlockSnapshot: AdBlockSnapshot = AdBlockSnapshot()

    @Volatile
    var privacySettings: PrivacySettings = PrivacySettings()

    @Volatile
    var tabsSnapshot: Map<String, BrowserTab> = emptyMap()

    fun getOrCreate(
        context: Context,
        tabId: String,
        onPageStarted: (String, String, Boolean, Boolean) -> Unit,
        onProgressChanged: (String, Int) -> Unit,
        onTitleChanged: (String, String) -> Unit,
        onPageFinished: (String, String, String, Boolean, Boolean, SslCertificateInfo?) -> Unit,
        onPageError: (String, String, String) -> Unit,
        onHttpsUpgradeBlocked: (String, String, String) -> Unit,
        onSslCertificateError: (String, String, String) -> Unit,
        onRequestBlocked: (String, BlockedRequestEntry) -> Unit,
        onOpenNewTab: (String) -> Unit,
        onLongPressLink: (String) -> Unit
    ): WebView {
        views[tabId]?.let { existing ->
            applyDynamicSettings(existing, tabsSnapshot[tabId], privacySettings)
            return existing
        }

        val webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureHardenedSettings(this, privacySettings)

            setOnLongClickListener { view ->
                val hitResult = (view as? WebView)?.hitTestResult
                val extra = hitResult?.extra
                if (!extra.isNullOrBlank() &&
                    (hitResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE ||
                        hitResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)
                ) {
                    onLongPressLink(extra)
                    true
                } else {
                    false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    onProgressChanged(tabId, newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) {
                        onTitleChanged(tabId, title)
                    }
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    // Block non-user-gesture popups; open user-initiated target=_blank in a new Veil tab
                    if (!isUserGesture) return false
                    val href = view?.hitTestResult?.extra
                    if (!href.isNullOrBlank() && UrlSecurityUtils.isAllowedWebScheme(href)) {
                        onOpenNewTab(href)
                    }
                    return false
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null
                    if (request.isForMainFrame) return null

                    val currentTab = tabsSnapshot[tabId]
                    val pageHost = currentTab?.url?.let { UrlSecurityUtils.extractHost(it) }
                    val snapshot = adBlockSnapshot

                    val decision = AdBlockEngine.evaluateRequest(
                        requestUrl = reqUrl,
                        pageHost = pageHost,
                        adBlockEnabled = snapshot.adBlockEnabled,
                        customBlockedPatterns = snapshot.customBlockedPatterns,
                        allowlistedHosts = snapshot.allowlistedHosts
                    )

                    if (decision.shouldBlock) {
                        onRequestBlocked(
                            tabId,
                            BlockedRequestEntry(
                                domain = decision.domain,
                                category = decision.category,
                                fullUrl = reqUrl
                            )
                        )
                        return AdBlockEngine.emptyResponseForUrl(reqUrl)
                    }
                    return null
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val reqUrl = request?.url?.toString() ?: return false
                    val settings = privacySettings

                    if (!UrlSecurityUtils.isAllowedWebScheme(reqUrl)) {
                        val scheme = Uri.parse(reqUrl).scheme?.lowercase()
                        if (scheme == "mailto" || scheme == "tel") {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }
                        return true
                    }

                    if (!request.isForMainFrame) return false

                    val cleanedUrl = if (settings.stripTrackingParams) {
                        UrlSecurityUtils.stripTrackingParameters(reqUrl)
                    } else {
                        reqUrl
                    }

                    val rawHost = UrlSecurityUtils.extractRawHost(cleanedUrl)
                    val isLocal = rawHost == "localhost" ||
                        rawHost == "127.0.0.1" ||
                        rawHost.startsWith("192.168.") ||
                        rawHost.startsWith("10.")

                    val tab = tabsSnapshot[tabId]
                    val allowedOnce = tab?.allowedHttpHostsOnce?.contains(rawHost) == true

                    if (settings.httpsOnlyMode &&
                        cleanedUrl.startsWith("http://", ignoreCase = true) &&
                        !isLocal &&
                        !allowedOnce
                    ) {
                        val key = "$tabId:$rawHost"
                        val lastUpgrade = autoUpgradedTimestamps[key] ?: 0L
                        val now = System.currentTimeMillis()
                        if (now - lastUpgrade < 6_000L) {
                            onHttpsUpgradeBlocked(
                                tabId,
                                cleanedUrl,
                                "This site redirected back to an unencrypted HTTP connection."
                            )
                        } else {
                            autoUpgradedTimestamps[key] = now
                            val upgraded = UrlSecurityUtils.upgradeToHttps(cleanedUrl)
                            view?.loadUrl(upgraded, buildPrivacyHeaders(settings.sendGpcHeader))
                        }
                        return true
                    }

                    if (cleanedUrl != reqUrl) {
                        view?.loadUrl(cleanedUrl, buildPrivacyHeaders(settings.sendGpcHeader))
                        return true
                    }

                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    val loadedUrl = url ?: return
                    if (loadedUrl == "about:blank") return
                    onPageStarted(
                        tabId,
                        loadedUrl,
                        view?.canGoBack() == true,
                        view?.canGoForward() == true
                    )
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    injectScriptsIfEnabled(view, tabId)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val loadedUrl = url ?: return
                    if (loadedUrl == "about:blank") return
                    injectScriptsIfEnabled(view, tabId)

                    val sslInfo = view?.certificate?.let { cert ->
                        val fmt = DateFormat.getDateInstance(DateFormat.MEDIUM)
                        SslCertificateInfo(
                            issuedTo = cert.issuedTo?.cName?.ifBlank { cert.issuedTo?.oName } ?: "Verified Host",
                            issuedBy = cert.issuedBy?.oName?.ifBlank { cert.issuedBy?.cName } ?: "Trusted Authority",
                            validFrom = cert.validNotBeforeDate?.let { fmt.format(it) } ?: "—",
                            validUntil = cert.validNotAfterDate?.let { fmt.format(it) } ?: "—",
                            isSecure = UrlSecurityUtils.isSecureUrl(loadedUrl)
                        )
                    }

                    onPageFinished(
                        tabId,
                        loadedUrl,
                        view?.title.orEmpty(),
                        view?.canGoBack() == true,
                        view?.canGoForward() == true,
                        sslInfo
                    )
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame != true) return
                    val failingUrl = request.url?.toString().orEmpty()
                    val desc = error?.description?.toString().orEmpty().ifBlank { "Unable to reach site" }
                    onPageError(tabId, failingUrl, desc)
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: SslErrorHandler?,
                    error: SslError?
                ) {
                    // Strictly cancel untrusted certificates; never bypass silently
                    handler?.cancel()
                    val url = error?.url.orEmpty()
                    val summary = when (error?.primaryError) {
                        SslError.SSL_EXPIRED -> "Certificate has expired."
                        SslError.SSL_IDMISMATCH -> "Certificate hostname does not match this site."
                        SslError.SSL_NOTYETVALID -> "Certificate is not yet valid."
                        SslError.SSL_UNTRUSTED -> "Certificate authority is not trusted."
                        else -> "Invalid or untrusted TLS certificate."
                    }
                    onSslCertificateError(tabId, url, summary)
                }
            }
        }

        views[tabId] = webView
        return webView
    }

    private fun injectScriptsIfEnabled(view: WebView?, tabId: String) {
        if (view == null) return
        val settings = privacySettings
        if (!settings.javascriptEnabled) return

        val snapshot = adBlockSnapshot
        val tab = tabsSnapshot[tabId]
        val host = tab?.url?.let { UrlSecurityUtils.extractHost(it) }.orEmpty()
        val isAllowlisted = host.isNotEmpty() && host in snapshot.allowlistedHosts

        if (snapshot.adBlockEnabled && settings.cosmeticBlockingEnabled && !isAllowlisted) {
            view.evaluateJavascript(AdBlockEngine.buildCosmeticHideScript(), null)
        }
        if (tab?.isFocusMode == true) {
            view.evaluateJavascript(AdBlockEngine.buildFocusModeScript(true), null)
        }
    }

    fun executeCommand(command: WebViewCommand) {
        when (command) {
            is WebViewCommand.LoadUrl -> {
                views[command.tabId]?.loadUrl(
                    command.url,
                    buildPrivacyHeaders(command.sendGpc)
                )
            }
            is WebViewCommand.GoBack -> {
                views[command.tabId]?.let { if (it.canGoBack()) it.goBack() }
            }
            is WebViewCommand.GoForward -> {
                views[command.tabId]?.let { if (it.canGoForward()) it.goForward() }
            }
            is WebViewCommand.Reload -> {
                views[command.tabId]?.reload()
            }
            is WebViewCommand.StopLoading -> {
                views[command.tabId]?.stopLoading()
            }
            is WebViewCommand.SetFocusMode -> {
                views[command.tabId]?.evaluateJavascript(
                    AdBlockEngine.buildFocusModeScript(command.enabled),
                    null
                )
            }
            is WebViewCommand.SetDesktopMode -> {
                views[command.tabId]?.let { wv ->
                    wv.settings.userAgentString = if (command.enabled) DESKTOP_USER_AGENT else null
                    wv.settings.useWideViewPort = true
                    wv.reload()
                }
            }
            is WebViewCommand.DestroyTab -> {
                destroySingle(command.tabId)
            }
            WebViewCommand.ShredAll -> {
                shredEverything()
            }
        }
    }

    fun syncAllSettings(settings: PrivacySettings, tabs: List<BrowserTab>) {
        privacySettings = settings
        tabsSnapshot = tabs.associateBy { it.id }
        views.forEach { (id, wv) ->
            applyDynamicSettings(wv, tabsSnapshot[id], settings)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureHardenedSettings(webView: WebView, settings: PrivacySettings) {
        val ws = webView.settings
        ws.javaScriptEnabled = settings.javascriptEnabled
        ws.domStorageEnabled = true
        ws.databaseEnabled = !settings.ephemeralMode
        ws.allowFileAccess = false
        ws.allowContentAccess = false
        @Suppress("DEPRECATION")
        ws.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        ws.allowUniversalAccessFromFileURLs = false
        ws.setGeolocationEnabled(false)
        ws.setSupportMultipleWindows(true)
        ws.javaScriptCanOpenWindowsAutomatically = false
        ws.builtInZoomControls = true
        ws.displayZoomControls = false
        ws.useWideViewPort = true
        ws.loadWithOverviewMode = true
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        ws.cacheMode = if (settings.ephemeralMode) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ws.safeBrowsingEnabled = true
        }

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(webView, !settings.blockThirdPartyCookies)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun applyDynamicSettings(webView: WebView, tab: BrowserTab?, settings: PrivacySettings) {
        val ws = webView.settings
        if (ws.javaScriptEnabled != settings.javascriptEnabled) {
            ws.javaScriptEnabled = settings.javascriptEnabled
        }
        ws.databaseEnabled = !settings.ephemeralMode
        ws.cacheMode = if (settings.ephemeralMode) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, !settings.blockThirdPartyCookies)
        val wantDesktop = tab?.isDesktopMode == true
        val hasDesktop = ws.userAgentString == DESKTOP_USER_AGENT
        if (wantDesktop != hasDesktop) {
            ws.userAgentString = if (wantDesktop) DESKTOP_USER_AGENT else null
        }
    }

    private fun destroySingle(tabId: String) {
        views.remove(tabId)?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.stopLoading()
            wv.loadUrl("about:blank")
            wv.clearHistory()
            wv.removeAllViews()
            wv.destroy()
        }
    }

    fun shredEverything() {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
        }
        val keys = views.keys.toList()
        for (key in keys) {
            views.remove(key)?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                wv.clearFormData()
                wv.clearCache(true)
                wv.loadUrl("about:blank")
                wv.removeAllViews()
                wv.destroy()
            }
        }
        autoUpgradedTimestamps.clear()
    }

    fun destroyAllOnDispose() {
        val keys = views.keys.toList()
        for (key in keys) {
            destroySingle(key)
        }
    }

    private fun buildPrivacyHeaders(sendGpc: Boolean): Map<String, String> {
        return if (sendGpc) {
            mapOf("Sec-GPC" to "1", "DNT" to "1")
        } else {
            emptyMap()
        }
    }

    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}

@Composable
fun VeilWebViewHost(
    activeTab: BrowserTab,
    allTabs: List<BrowserTab>,
    settings: PrivacySettings,
    adBlockSnapshot: AdBlockSnapshot,
    commands: SharedFlow<WebViewCommand>,
    onPageStarted: (String, String, Boolean, Boolean) -> Unit,
    onProgressChanged: (String, Int) -> Unit,
    onTitleChanged: (String, String) -> Unit,
    onPageFinished: (String, String, String, Boolean, Boolean, SslCertificateInfo?) -> Unit,
    onPageError: (String, String, String) -> Unit,
    onHttpsUpgradeBlocked: (String, String, String) -> Unit,
    onSslCertificateError: (String, String, String) -> Unit,
    onRequestBlocked: (String, BlockedRequestEntry) -> Unit,
    onOpenNewTab: (String) -> Unit,
    onLongPressLink: (String) -> Unit,
    onAllowHttpOnce: (String, String) -> Unit,
    onDismissAlertBack: (String) -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pool = remember { WebViewPool() }

    val latestOnPageStarted by rememberUpdatedState(onPageStarted)
    val latestOnProgressChanged by rememberUpdatedState(onProgressChanged)
    val latestOnTitleChanged by rememberUpdatedState(onTitleChanged)
    val latestOnPageFinished by rememberUpdatedState(onPageFinished)
    val latestOnPageError by rememberUpdatedState(onPageError)
    val latestOnHttpsUpgradeBlocked by rememberUpdatedState(onHttpsUpgradeBlocked)
    val latestOnSslCertificateError by rememberUpdatedState(onSslCertificateError)
    val latestOnRequestBlocked by rememberUpdatedState(onRequestBlocked)
    val latestOnOpenNewTab by rememberUpdatedState(onOpenNewTab)
    val latestOnLongPressLink by rememberUpdatedState(onLongPressLink)

    pool.adBlockSnapshot = adBlockSnapshot
    pool.syncAllSettings(settings, allTabs)

    LaunchedEffect(commands) {
        commands.collect { cmd ->
            if (cmd is WebViewCommand.LoadUrl) {
                // Ensure the WebView exists before calling loadUrl
                pool.getOrCreate(
                    context = context,
                    tabId = cmd.tabId,
                    onPageStarted = { id, u, b, f -> latestOnPageStarted(id, u, b, f) },
                    onProgressChanged = { id, p -> latestOnProgressChanged(id, p) },
                    onTitleChanged = { id, t -> latestOnTitleChanged(id, t) },
                    onPageFinished = { id, u, t, b, f, s -> latestOnPageFinished(id, u, t, b, f, s) },
                    onPageError = { id, u, d -> latestOnPageError(id, u, d) },
                    onHttpsUpgradeBlocked = { id, u, r -> latestOnHttpsUpgradeBlocked(id, u, r) },
                    onSslCertificateError = { id, u, e -> latestOnSslCertificateError(id, u, e) },
                    onRequestBlocked = { id, entry -> latestOnRequestBlocked(id, entry) },
                    onOpenNewTab = { u -> latestOnOpenNewTab(u) },
                    onLongPressLink = { u -> latestOnLongPressLink(u) }
                )
            }
            pool.executeCommand(cmd)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pool.destroyAllOnDispose()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!activeTab.isStartPage) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("browser_webview_container"),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { container ->
                    val wv = pool.getOrCreate(
                        context = container.context,
                        tabId = activeTab.id,
                        onPageStarted = { id, u, b, f -> latestOnPageStarted(id, u, b, f) },
                        onProgressChanged = { id, p -> latestOnProgressChanged(id, p) },
                        onTitleChanged = { id, t -> latestOnTitleChanged(id, t) },
                        onPageFinished = { id, u, t, b, f, s -> latestOnPageFinished(id, u, t, b, f, s) },
                        onPageError = { id, u, d -> latestOnPageError(id, u, d) },
                        onHttpsUpgradeBlocked = { id, u, r -> latestOnHttpsUpgradeBlocked(id, u, r) },
                        onSslCertificateError = { id, u, e -> latestOnSslCertificateError(id, u, e) },
                        onRequestBlocked = { id, entry -> latestOnRequestBlocked(id, entry) },
                        onOpenNewTab = { u -> latestOnOpenNewTab(u) },
                        onLongPressLink = { u -> latestOnLongPressLink(u) }
                    )
                    if (container.childCount == 0 || container.getChildAt(0) !== wv) {
                        container.removeAllViews()
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        container.addView(
                            wv,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                    if (wv.url.isNullOrBlank() && activeTab.url.isNotBlank()) {
                        wv.loadUrl(
                            activeTab.url,
                            if (settings.sendGpcHeader) mapOf("Sec-GPC" to "1", "DNT" to "1") else emptyMap()
                        )
                    }
                }
            )
        }

        when (val alert = activeTab.securityAlert) {
            is SecurityAlert.HttpsUpgradePrompt -> {
                SecurityInterstitialOverlay(
                    isCriticalSsl = false,
                    title = stringResource(R.string.https_only_title),
                    message = "${alert.reason}\n\n${stringResource(R.string.https_only_message)}",
                    targetUrl = alert.insecureUrl,
                    onGoBack = { onDismissAlertBack(activeTab.id) },
                    onProceedOnce = { onAllowHttpOnce(activeTab.id, alert.insecureUrl) }
                )
            }
            is SecurityAlert.SslCertificateError -> {
                SecurityInterstitialOverlay(
                    isCriticalSsl = true,
                    title = stringResource(R.string.ssl_error_title),
                    message = "${alert.errorSummary}\n\n${stringResource(R.string.ssl_error_message)}",
                    targetUrl = alert.url,
                    onGoBack = { onDismissAlertBack(activeTab.id) },
                    onProceedOnce = null
                )
            }
            SecurityAlert.None -> {
                activeTab.pageError?.let { errorMsg ->
                    PageErrorOverlay(
                        message = errorMsg,
                        onRetry = onReload,
                        onGoBack = { onDismissAlertBack(activeTab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityInterstitialOverlay(
    isCriticalSsl: Boolean,
    title: String,
    message: String,
    targetUrl: String,
    onGoBack: () -> Unit,
    onProceedOnce: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.widthIn(max = 460.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isCriticalSsl) Icons.Outlined.GppBad else Icons.Outlined.LockOpen,
                        contentDescription = title,
                        tint = if (isCriticalSsl) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (targetUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = UrlSecurityUtils.extractHost(targetUrl).ifBlank { targetUrl },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onGoBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_go_back_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.go_back_safely))
                    }
                    if (onProceedOnce != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onProceedOnce,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_proceed_http_button")
                        ) {
                            Text(stringResource(R.string.proceed_http_once))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageErrorOverlay(
    message: String,
    onRetry: () -> Unit,
    onGoBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Page unavailable",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onGoBack) {
                        Text("Back")
                    }
                    Button(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}
