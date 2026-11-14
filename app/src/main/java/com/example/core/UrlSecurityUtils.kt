package com.example.core

import android.net.Uri
import android.util.Patterns
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class SearchEngine(
    val displayName: String,
    val shortName: String,
    val searchUrlTemplate: String,
    val homeUrl: String
) {
    DUCKDUCKGO(
        displayName = "DuckDuckGo",
        shortName = "DDG",
        searchUrlTemplate = "https://duckduckgo.com/?q=%s",
        homeUrl = "https://duckduckgo.com"
    ),
    STARTPAGE(
        displayName = "Startpage",
        shortName = "Startpage",
        searchUrlTemplate = "https://www.startpage.com/sp/search?query=%s",
        homeUrl = "https://www.startpage.com"
    ),
    BRAVE(
        displayName = "Brave Search",
        shortName = "Brave",
        searchUrlTemplate = "https://search.brave.com/search?q=%s",
        homeUrl = "https://search.brave.com"
    ),
    WIKIPEDIA(
        displayName = "Wikipedia",
        shortName = "Wiki",
        searchUrlTemplate = "https://en.wikipedia.org/w/index.php?search=%s",
        homeUrl = "https://en.wikipedia.org"
    ),
    GOOGLE(
        displayName = "Google",
        shortName = "Google",
        searchUrlTemplate = "https://www.google.com/search?q=%s",
        homeUrl = "https://www.google.com"
    )
}

object UrlSecurityUtils {

    private val TRACKING_QUERY_PARAMS = setOf(
        "fbclid", "gclid", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
        "mc_cid", "mc_eid", "igshid", "igsh", "_hsenc", "_hsmi", "vero_id",
        "wickedid", "oly_anon_id", "oly_enc_id", "rb_clickid", "s_cid",
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "utm_id", "utm_name", "utm_reader", "utm_viz_id", "utm_pubreferrer",
        "ref_src", "ref_url", "mkt_tok", "trk", "trkInfo"
    )

    private val ALLOWED_WEB_SCHEMES = setOf("http", "https")

    fun isAllowedWebScheme(url: String): Boolean {
        val scheme = runCatching { Uri.parse(url).scheme?.lowercase() }.getOrNull()
        return scheme in ALLOWED_WEB_SCHEMES
    }

    fun isSecureUrl(url: String): Boolean {
        if (url.isBlank() || url == "about:blank") return true
        return url.startsWith("https://", ignoreCase = true)
    }

    fun extractHost(url: String): String {
        if (url.isBlank()) return ""
        return runCatching {
            val host = Uri.parse(url).host.orEmpty().lowercase()
            host.removePrefix("www.")
        }.getOrDefault("")
    }

    fun extractRawHost(url: String): String {
        if (url.isBlank()) return ""
        return runCatching {
            Uri.parse(url).host.orEmpty().lowercase()
        }.getOrDefault("")
    }

    fun normalizeInputToUrl(
        rawInput: String,
        searchEngine: SearchEngine,
        httpsOnly: Boolean,
        stripTrackingParams: Boolean
    ): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""

        val candidateUrl = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> {
                if (httpsOnly && !isLocalHost(trimmed)) {
                    "https://" + trimmed.substring(7)
                } else {
                    trimmed
                }
            }
            looksLikeDomainOrUrl(trimmed) -> {
                val scheme = if (isLocalHost("http://$trimmed") && !httpsOnly) "http://" else "https://"
                scheme + trimmed
            }
            else -> {
                val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.toString())
                searchEngine.searchUrlTemplate.replace("%s", encoded)
            }
        }

        return if (stripTrackingParams) stripTrackingParameters(candidateUrl) else candidateUrl
    }

    fun stripTrackingParameters(url: String): String {
        if (!url.contains("?")) return url
        return runCatching {
            val uri = Uri.parse(url)
            val queryNames = uri.queryParameterNames
            if (queryNames.isEmpty()) return url

            val hasTrackingParam = queryNames.any { name ->
                val lower = name.lowercase()
                lower in TRACKING_QUERY_PARAMS || lower.startsWith("utm_")
            }
            if (!hasTrackingParam) return url

            val builder = uri.buildUpon().clearQuery()
            for (name in queryNames) {
                val lower = name.lowercase()
                if (lower !in TRACKING_QUERY_PARAMS && !lower.startsWith("utm_")) {
                    for (value in uri.getQueryParameters(name)) {
                        builder.appendQueryParameter(name, value)
                    }
                }
            }
            builder.build().toString()
        }.getOrDefault(url)
    }

    fun upgradeToHttps(httpUrl: String): String {
        return if (httpUrl.startsWith("http://", ignoreCase = true)) {
            "https://" + httpUrl.substring(7)
        } else {
            httpUrl
        }
    }

    private fun isLocalHost(url: String): Boolean {
        val host = extractRawHost(url)
        return host == "localhost" ||
            host == "127.0.0.1" ||
            host.startsWith("192.168.") ||
            host.startsWith("10.")
    }

    private fun looksLikeDomainOrUrl(input: String): Boolean {
        if (input.contains(" ")) return false
        if (input.startsWith("localhost", ignoreCase = true)) return true
        val hostPart = input.substringBefore("/").substringBefore("?").substringBefore(":")
        if (Patterns.IP_ADDRESS.matcher(hostPart).matches()) return true
        return hostPart.contains(".") &&
            !hostPart.startsWith(".") &&
            !hostPart.endsWith(".") &&
            hostPart.substringAfterLast(".").length >= 2
    }
}
