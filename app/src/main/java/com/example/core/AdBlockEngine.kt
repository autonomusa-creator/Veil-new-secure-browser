package com.example.core

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

enum class BlockCategory(val label: String) {
    AD_NETWORK("Ad Network"),
    TRACKER("Analytics & Tracker"),
    SOCIAL_PIXEL("Social Pixel"),
    TELEMETRY("Telemetry & Crash"),
    CUSTOM("Custom Filter")
}

data class BlockDecision(
    val shouldBlock: Boolean,
    val domain: String = "",
    val category: BlockCategory = BlockCategory.AD_NETWORK
)

object AdBlockEngine {

    private val AD_NETWORK_DOMAINS = hashSetOf(
        "doubleclick.net",
        "g.doubleclick.net",
        "googleads.g.doubleclick.net",
        "ad.doubleclick.net",
        "stats.g.doubleclick.net",
        "googlesyndication.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "googleadservices.com",
        "www.googleadservices.com",
        "adservice.google.com",
        "imasdk.googleapis.com",
        "fundingchoicesmessages.google.com",
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",
        "c.amazon-adsystem.com",
        "s.amazon-adsystem.com",
        "adnxs.com",
        "ib.adnxs.com",
        "acdn.adnxs.com",
        "taboola.com",
        "cdn.taboola.com",
        "trc.taboola.com",
        "outbrain.com",
        "widgets.outbrain.com",
        "criteo.com",
        "criteo.net",
        "static.criteo.net",
        "bidder.criteo.com",
        "pubmatic.com",
        "ads.pubmatic.com",
        "rubiconproject.com",
        "fastlane.rubiconproject.com",
        "openx.net",
        "us-u.openx.net",
        "casalemedia.com",
        "indexexchange.com",
        "htlb.casalemedia.com",
        "3lift.com",
        "eb2.3lift.com",
        "triplelift.com",
        "sharethrough.com",
        "teads.tv",
        "a.teads.tv",
        "media.net",
        "contextual.media.net",
        "moatads.com",
        "z.moatads.com",
        "adform.net",
        "track.adform.net",
        "bidswitch.net",
        "mathtag.com",
        "pixel.mathtag.com",
        "flashtalking.com",
        "serving-sys.com",
        "innovid.com",
        "adroll.com",
        "s.adroll.com",
        "d.adroll.com",
        "perfectaudience.com",
        "revcontent.com",
        "trends.revcontent.com",
        "mgid.com",
        "jsc.mgid.com",
        "propellerads.com",
        "popads.net",
        "popcash.net",
        "zedo.com",
        "advertising.com",
        "ads.yahoo.com",
        "gemini.yahoo.com",
        "adcolony.com",
        "applovin.com",
        "vungle.com",
        "inmobi.com",
        "smaato.net",
        "smartadserver.com",
        "yieldmo.com",
        "sovrn.com",
        "lijit.com",
        "kargo.com",
        "gumgum.com",
        "spotxchange.com",
        "springserve.com",
        "connatix.com",
        "nativo.com",
        "adserver.org",
        "carbonads.com",
        "srv.carbonads.net",
        "buysellads.com",
        "ethicalads.io",
        "media.ethicalads.io",
        "adsrvr.org",
        "match.adsrvr.org",
        "turn.com",
        "rhythmone.com",
        "sonobi.com",
        "undertone.com",
        "zemanta.com",
        "lkqd.net",
        "tremorhub.com",
        "stickyadstv.com"
    )

    private val TRACKER_DOMAINS = hashSetOf(
        "google-analytics.com",
        "www.google-analytics.com",
        "ssl.google-analytics.com",
        "analytics.google.com",
        "googletagmanager.com",
        "www.googletagmanager.com",
        "googletagservices.com",
        "scorecardresearch.com",
        "sb.scorecardresearch.com",
        "quantserve.com",
        "pixel.quantserve.com",
        "hotjar.com",
        "static.hotjar.com",
        "script.hotjar.com",
        "vars.hotjar.com",
        "mixpanel.com",
        "api.mixpanel.com",
        "cdn.mxpnl.com",
        "amplitude.com",
        "api.amplitude.com",
        "api2.amplitude.com",
        "cdn.amplitude.com",
        "segment.io",
        "segment.com",
        "cdn.segment.com",
        "api.segment.io",
        "chartbeat.com",
        "static.chartbeat.com",
        "ping.chartbeat.net",
        "clarity.ms",
        "www.clarity.ms",
        "bat.bing.com",
        "mc.yandex.ru",
        "an.yandex.ru",
        "omtrdc.net",
        "2o7.net",
        "demdex.net",
        "dpm.demdex.net",
        "krxd.net",
        "beacon.krxd.net",
        "bluekai.com",
        "tags.bluekai.com",
        "rlcdn.com",
        "exelator.com",
        "eyeota.net",
        "lotame.com",
        "crwdcntrl.net",
        "tapad.com",
        "liveramp.com",
        "pippio.com",
        "everesttech.net",
        "bounceexchange.com",
        "fullstory.com",
        "rs.fullstory.com",
        "logrocket.com",
        "heap.io",
        "heapanalytics.com",
        "cdn.heapanalytics.com",
        "kissmetrics.com",
        "kissmetrics.io",
        "optimizely.com",
        "cdn.optimizely.com",
        "crazyegg.com",
        "mouseflow.com",
        "inspectlet.com",
        "luckyorange.com",
        "pendo.io",
        "appcues.com",
        "intercom.io",
        "widget.intercom.io",
        "drift.com",
        "js.driftt.com",
        "hubspot.com",
        "js.hs-scripts.com",
        "js.hs-analytics.net",
        "track.hubspot.com",
        "mktoresp.com",
        "munchkin.marketo.net",
        "pardot.com",
        "pi.pardot.com",
        "branch.io",
        "app.link",
        "adjust.com",
        "appsflyer.com",
        "kochava.com",
        "singular.net",
        "onesignal.com",
        "cdn.onesignal.com",
        "pushwoosh.com",
        "permutive.com",
        "parsely.com",
        "pixel.wp.com",
        "stats.wp.com",
        "skimresources.com",
        "viglink.com",
        "narrativ.com"
    )

    private val SOCIAL_PIXEL_DOMAINS = hashSetOf(
        "connect.facebook.net",
        "pixel.facebook.com",
        "an.facebook.com",
        "ads.twitter.com",
        "static.ads-twitter.com",
        "analytics.twitter.com",
        "ads-api.twitter.com",
        "t.co",
        "ads.linkedin.com",
        "px.ads.linkedin.com",
        "snap.licdn.com",
        "ads.pinterest.com",
        "ct.pinterest.com",
        "log.pinterest.com",
        "analytics.tiktok.com",
        "ads.tiktok.com",
        "sc-static.net",
        "tr.snapchat.com",
        "alb.reddit.com",
        "events.reddit.com",
        "pixel.redditmedia.com"
    )

    private val TELEMETRY_DOMAINS = hashSetOf(
        "sentry.io",
        "browser.sentry-cdn.com",
        "bugsnag.com",
        "notify.bugsnag.com",
        "newrelic.com",
        "js-agent.newrelic.com",
        "bam.nr-data.net",
        "datadoghq.com",
        "browser-intake-datadoghq.com",
        "rollbar.com",
        "raygun.io",
        "trackjs.com",
        "app-measurement.com",
        "firebaseinstallations.googleapis.com"
    )

    private val AD_PATH_HINTS = listOf(
        "/pagead/",
        "/adserver/",
        "/adsystem/",
        "/ads/banner",
        "/ad-banner",
        "/prebid",
        "/gpt/pubads",
        "/pubads_impl",
        "/fbevents.js",
        "/gtag/js",
        "/gtm.js",
        "/analytics.js",
        "/matomo.js",
        "/piwik.js"
    )

    private val AD_HOST_PREFIXES = listOf(
        "ads.",
        "ad.",
        "adserver.",
        "adservice.",
        "adsystem.",
        "pixel.",
        "pixels.",
        "tracker.",
        "tracking.",
        "telemetry.",
        "metrics.",
        "beacon.",
        "beacons."
    )

    fun evaluateRequest(
        requestUrl: String,
        pageHost: String?,
        adBlockEnabled: Boolean,
        customBlockedPatterns: Set<String>,
        allowlistedHosts: Set<String>
    ): BlockDecision {
        if (!adBlockEnabled) return BlockDecision(shouldBlock = false)

        val currentSite = pageHost?.lowercase()?.removePrefix("www.") ?: ""
        if (currentSite.isNotEmpty() && isHostOrSuffixInSet(currentSite, allowlistedHosts)) {
            return BlockDecision(shouldBlock = false)
        }

        val uri = runCatching { Uri.parse(requestUrl) }.getOrNull()
            ?: return BlockDecision(shouldBlock = false)
        val rawHost = uri.host?.lowercase() ?: return BlockDecision(shouldBlock = false)
        val normalizedHost = rawHost.removePrefix("www.")

        // Never block same-origin root resources unless matching an explicit ad path
        val isSameOrigin = currentSite.isNotEmpty() &&
            (normalizedHost == currentSite || normalizedHost.endsWith(".$currentSite"))

        // 1. User custom filters
        if (customBlockedPatterns.isNotEmpty()) {
            val lowerUrl = requestUrl.lowercase()
            for (pattern in customBlockedPatterns) {
                val clean = pattern.trim().lowercase()
                if (clean.isNotEmpty() && (normalizedHost == clean || normalizedHost.endsWith(".$clean") || lowerUrl.contains(clean))) {
                    return BlockDecision(true, normalizedHost, BlockCategory.CUSTOM)
                }
            }
        }

        // 2. Domain sets (exact + parent domain walk)
        findMatchingCategory(rawHost)?.let { category ->
            return BlockDecision(true, normalizedHost, category)
        }

        // 3. Heuristic third-party ad/tracker subdomain prefixes
        if (!isSameOrigin) {
            for (prefix in AD_HOST_PREFIXES) {
                if (rawHost.startsWith(prefix)) {
                    val cat = if (prefix.startsWith("ad")) BlockCategory.AD_NETWORK else BlockCategory.TRACKER
                    return BlockDecision(true, normalizedHost, cat)
                }
            }
        }

        // 4. Known ad/tracker script path hints
        val path = uri.path?.lowercase().orEmpty()
        if (path.isNotEmpty()) {
            for (hint in AD_PATH_HINTS) {
                if (path.contains(hint)) {
                    val cat = if (hint.contains("fb") || hint.contains("gt") || hint.contains("analytics") || hint.contains("matomo") || hint.contains("piwik")) {
                        BlockCategory.TRACKER
                    } else {
                        BlockCategory.AD_NETWORK
                    }
                    return BlockDecision(true, normalizedHost, cat)
                }
            }
        }

        return BlockDecision(shouldBlock = false)
    }

    private fun findMatchingCategory(host: String): BlockCategory? {
        var candidate = host
        while (candidate.contains(".")) {
            when {
                candidate in AD_NETWORK_DOMAINS -> return BlockCategory.AD_NETWORK
                candidate in SOCIAL_PIXEL_DOMAINS -> return BlockCategory.SOCIAL_PIXEL
                candidate in TRACKER_DOMAINS -> return BlockCategory.TRACKER
                candidate in TELEMETRY_DOMAINS -> return BlockCategory.TELEMETRY
            }
            candidate = candidate.substringAfter(".", "")
        }
        return null
    }

    private fun isHostOrSuffixInSet(host: String, set: Set<String>): Boolean {
        if (set.isEmpty()) return false
        var candidate = host
        while (candidate.isNotEmpty()) {
            if (candidate in set) return true
            val dot = candidate.indexOf('.')
            if (dot == -1) break
            candidate = candidate.substring(dot + 1)
        }
        return false
    }

    fun emptyResponseForUrl(url: String): WebResourceResponse {
        val lowerPath = url.substringBefore("?").lowercase()
        return when {
            lowerPath.endsWith(".js") -> WebResourceResponse(
                "application/javascript",
                "utf-8",
                ByteArrayInputStream("/* blocked by Veil */".toByteArray())
            )
            lowerPath.endsWith(".css") -> WebResourceResponse(
                "text/css",
                "utf-8",
                ByteArrayInputStream("/* blocked by Veil */".toByteArray())
            )
            else -> WebResourceResponse(
                "text/plain",
                "utf-8",
                ByteArrayInputStream(ByteArray(0))
            )
        }
    }

    /**
     * Injects lightweight cosmetic CSS to collapse empty ad containers, sponsored blocks,
     * and common intrusive overlays without altering organic article layout.
     */
    fun buildCosmeticHideScript(): String = """
        (function() {
            var id = 'veil-cosmetic-style';
            if (document.getElementById(id)) return;
            var css = `
                ins.adsbygoogle,
                iframe[src*="doubleclick.net"],
                iframe[src*="googlesyndication.com"],
                iframe[src*="amazon-adsystem.com"],
                iframe[src*="taboola.com"],
                iframe[src*="outbrain.com"],
                [id^="google_ads_iframe"],
                [id^="div-gpt-ad"],
                [data-ad-slot],
                [data-ad-client],
                [data-google-query-id],
                .adsbygoogle,
                .ad-banner,
                .ad-container,
                .ad-wrapper,
                .ad-slot,
                .sponsored-content,
                .outbrain-widget,
                .taboola-widget,
                #taboola-below-article-thumbnails,
                #outbrain_widget_0 {
                    display: none !important;
                    visibility: hidden !important;
                    height: 0 !important;
                    min-height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                }
            `;
            var style = document.createElement('style');
            style.id = id;
            style.type = 'text/css';
            style.appendChild(document.createTextNode(css));
            (document.head || document.documentElement).appendChild(style);
        })();
    """.trimIndent()

    /**
     * Toggles Distraction-Free Focus Mode CSS on the active page:
     * hides sticky headers, sidebars, floating widgets, newsletter banners, and social bars.
     */
    fun buildFocusModeScript(enable: Boolean): String = if (enable) {
        """
        (function() {
            var id = 'veil-focus-style';
            if (document.getElementById(id)) return;
            var css = `
                aside,
                [role="complementary"],
                [class*="newsletter"],
                [class*="subscribe-banner"],
                [class*="cookie-banner"],
                [class*="consent-banner"],
                [id*="cookie-notice"],
                [id*="onetrust"],
                [class*="social-share"],
                [class*="share-bar"],
                [class*="floating-chat"],
                [class*="intercom"],
                [class*="popup-modal"],
                [class*="paywall-banner"],
                [class*="sticky-promo"] {
                    display: none !important;
                }
                body {
                    line-height: 1.65 !important;
                    overflow-x: hidden !important;
                }
                article, main, [role="main"] {
                    max-width: 720px !important;
                    margin-left: auto !important;
                    margin-right: auto !important;
                }
            `;
            var style = document.createElement('style');
            style.id = id;
            style.type = 'text/css';
            style.appendChild(document.createTextNode(css));
            (document.head || document.documentElement).appendChild(style);
        })();
        """.trimIndent()
    } else {
        """
        (function() {
            var el = document.getElementById('veil-focus-style');
            if (el && el.parentNode) el.parentNode.removeChild(el);
        })();
        """.trimIndent()
    }
}
