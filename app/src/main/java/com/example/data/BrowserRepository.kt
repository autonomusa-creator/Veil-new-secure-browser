package com.example.data

import com.example.core.UrlSecurityUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BrowserRepository(
    private val dao: BrowserDao,
    private val prefs: PrivacyPreferencesRepository
) {
    val settingsFlow: Flow<PrivacySettings> = prefs.settingsFlow
    val bookmarksFlow: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
    val customRulesFlow: Flow<List<CustomRuleEntity>> = dao.observeCustomRules()

    suspend fun ensureStarterBookmarksSeeded() {
        val currentSettings = prefs.settingsFlow.first()
        if (!currentSettings.hasSeededDefaults) {
            val starterPins = listOf(
                BookmarkEntity(
                    title = "Wikipedia",
                    url = "https://en.wikipedia.org",
                    host = "en.wikipedia.org"
                ),
                BookmarkEntity(
                    title = "DuckDuckGo",
                    url = "https://duckduckgo.com",
                    host = "duckduckgo.com"
                ),
                BookmarkEntity(
                    title = "EFF Privacy",
                    url = "https://www.eff.org",
                    host = "eff.org"
                ),
                BookmarkEntity(
                    title = "Hacker News",
                    url = "https://news.ycombinator.com",
                    host = "news.ycombinator.com"
                )
            )
            dao.insertBookmarksIgnore(starterPins)
            prefs.markDefaultsSeeded()
        }
    }

    suspend fun addBookmark(title: String, url: String) {
        val cleanTitle = title.ifBlank { UrlSecurityUtils.extractHost(url).ifBlank { "Untitled" } }
        dao.insertBookmark(
            BookmarkEntity(
                title = cleanTitle,
                url = url,
                host = UrlSecurityUtils.extractHost(url)
            )
        )
    }

    suspend fun removeBookmarkByUrl(url: String) = dao.deleteBookmarkByUrl(url)
    suspend fun removeBookmarkById(id: Long) = dao.deleteBookmarkById(id)

    suspend fun addCustomRule(domainPattern: String, isAllowlist: Boolean) {
        val normalized = domainPattern.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
        if (normalized.isNotEmpty()) {
            dao.insertCustomRule(
                CustomRuleEntity(
                    domainPattern = normalized,
                    isAllowlist = isAllowlist
                )
            )
        }
    }

    suspend fun removeCustomRuleById(id: Long) = dao.deleteCustomRuleById(id)

    suspend fun removeCustomRuleByDomain(domainPattern: String, isAllowlist: Boolean) {
        dao.deleteCustomRuleByDomain(domainPattern.lowercase().removePrefix("www."), isAllowlist)
    }

    suspend fun updateSettings(action: suspend PrivacyPreferencesRepository.() -> Unit) {
        prefs.action()
    }

    suspend fun recordBlockedRequests(count: Int) {
        prefs.incrementLifetimeBlocked(count)
    }
}
