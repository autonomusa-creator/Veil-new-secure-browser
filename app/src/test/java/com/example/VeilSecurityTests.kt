package com.example

import com.example.core.AdBlockEngine
import com.example.core.BlockCategory
import com.example.core.SearchEngine
import com.example.core.UrlSecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VeilSecurityTests {

    @Test
    fun adBlockEngine_blocksKnownAdNetworkSubdomain() {
        val decision = AdBlockEngine.evaluateRequest(
            requestUrl = "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
            pageHost = "example.com",
            adBlockEnabled = true,
            customBlockedPatterns = emptySet(),
            allowlistedHosts = emptySet()
        )
        assertTrue(decision.shouldBlock)
        assertEquals(BlockCategory.AD_NETWORK, decision.category)
    }

    @Test
    fun adBlockEngine_blocksSocialPixelAndAnalytics() {
        val fb = AdBlockEngine.evaluateRequest(
            requestUrl = "https://connect.facebook.net/en_US/fbevents.js",
            pageHost = "news.example.org",
            adBlockEnabled = true,
            customBlockedPatterns = emptySet(),
            allowlistedHosts = emptySet()
        )
        assertTrue(fb.shouldBlock)
        assertEquals(BlockCategory.SOCIAL_PIXEL, fb.category)

        val ga = AdBlockEngine.evaluateRequest(
            requestUrl = "https://www.google-analytics.com/collect?v=1",
            pageHost = "news.example.org",
            adBlockEnabled = true,
            customBlockedPatterns = emptySet(),
            allowlistedHosts = emptySet()
        )
        assertTrue(ga.shouldBlock)
        assertEquals(BlockCategory.TRACKER, ga.category)
    }

    @Test
    fun adBlockEngine_respectsAllowlistedSiteAndCustomFilters() {
        val allowlisted = AdBlockEngine.evaluateRequest(
            requestUrl = "https://doubleclick.net/ad.js",
            pageHost = "trusted.org",
            adBlockEnabled = true,
            customBlockedPatterns = emptySet(),
            allowlistedHosts = setOf("trusted.org")
        )
        assertFalse(allowlisted.shouldBlock)

        val customBlocked = AdBlockEngine.evaluateRequest(
            requestUrl = "https://cdn.customtracker.io/sdk.js",
            pageHost = "example.com",
            adBlockEnabled = true,
            customBlockedPatterns = setOf("customtracker.io"),
            allowlistedHosts = emptySet()
        )
        assertTrue(customBlocked.shouldBlock)
        assertEquals(BlockCategory.CUSTOM, customBlocked.category)
    }

    @Test
    fun urlSecurityUtils_upgradesHttpAndStripsTrackingParams() {
        val normalized = UrlSecurityUtils.normalizeInputToUrl(
            rawInput = "http://example.org/article?id=42&utm_source=x&fbclid=12345",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsOnly = true,
            stripTrackingParams = true
        )
        assertEquals("https://example.org/article?id=42", normalized)
    }

    @Test
    fun urlSecurityUtils_routesSearchQueriesToSelectedEngine() {
        val searchUrl = UrlSecurityUtils.normalizeInputToUrl(
            rawInput = "privacy focused browsers",
            searchEngine = SearchEngine.DUCKDUCKGO,
            httpsOnly = true,
            stripTrackingParams = true
        )
        assertEquals("https://duckduckgo.com/?q=privacy+focused+browsers", searchUrl)
    }
}
