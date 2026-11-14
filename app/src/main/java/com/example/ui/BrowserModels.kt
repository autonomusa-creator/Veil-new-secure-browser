package com.example.ui

import com.example.core.BlockCategory

data class BlockedRequestEntry(
    val domain: String,
    val category: BlockCategory,
    val fullUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SslCertificateInfo(
    val issuedTo: String,
    val issuedBy: String,
    val validFrom: String,
    val validUntil: String,
    val isSecure: Boolean
)

sealed interface SecurityAlert {
    data object None : SecurityAlert
    data class HttpsUpgradePrompt(
        val insecureUrl: String,
        val reason: String
    ) : SecurityAlert

    data class SslCertificateError(
        val url: String,
        val errorSummary: String
    ) : SecurityAlert
}

data class BrowserTab(
    val id: String,
    val title: String = "New Private Tab",
    val url: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isFocusMode: Boolean = false,
    val isDesktopMode: Boolean = false,
    val isHttps: Boolean = true,
    val sslInfo: SslCertificateInfo? = null,
    val blockedRequests: List<BlockedRequestEntry> = emptyList(),
    val blockedCount: Int = 0,
    val securityAlert: SecurityAlert = SecurityAlert.None,
    val pageError: String? = null,
    val allowedHttpHostsOnce: Set<String> = emptySet()
) {
    val isStartPage: Boolean get() = url.isBlank()
}

enum class ActiveOverlay {
    NONE,
    PRIVACY_INSPECTOR,
    SECURITY_DETAILS,
    TAB_SWITCHER,
    BOOKMARKS,
    SETTINGS
}

data class LinkContextMenuState(
    val url: String,
    val title: String = ""
)

sealed interface WebViewCommand {
    data class LoadUrl(val tabId: String, val url: String, val sendGpc: Boolean) : WebViewCommand
    data class GoBack(val tabId: String) : WebViewCommand
    data class GoForward(val tabId: String) : WebViewCommand
    data class Reload(val tabId: String) : WebViewCommand
    data class StopLoading(val tabId: String) : WebViewCommand
    data class SetFocusMode(val tabId: String, val enabled: Boolean) : WebViewCommand
    data class SetDesktopMode(val tabId: String, val enabled: Boolean) : WebViewCommand
    data class DestroyTab(val tabId: String) : WebViewCommand
    data object ShredAll : WebViewCommand
}

data class AdBlockSnapshot(
    val adBlockEnabled: Boolean = true,
    val customBlockedPatterns: Set<String> = emptySet(),
    val allowlistedHosts: Set<String> = emptySet()
)
