package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.SearchEngine
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "veil_privacy_prefs")

data class PrivacySettings(
    val adBlockEnabled: Boolean = true,
    val cosmeticBlockingEnabled: Boolean = true,
    val httpsOnlyMode: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val stripTrackingParams: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val ephemeralMode: Boolean = true,
    val sendGpcHeader: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lifetimeBlockedCount: Long = 0L,
    val hasSeededDefaults: Boolean = false
)

class PrivacyPreferencesRepository(private val context: Context) {

    private object Keys {
        val AD_BLOCK = booleanPreferencesKey("ad_block_enabled")
        val COSMETIC_BLOCK = booleanPreferencesKey("cosmetic_blocking_enabled")
        val HTTPS_ONLY = booleanPreferencesKey("https_only_mode")
        val BLOCK_3P_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val STRIP_TRACKERS = booleanPreferencesKey("strip_tracking_params")
        val JS_ENABLED = booleanPreferencesKey("javascript_enabled")
        val EPHEMERAL = booleanPreferencesKey("ephemeral_mode")
        val SEND_GPC = booleanPreferencesKey("send_gpc_header")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LIFETIME_BLOCKED = longPreferencesKey("lifetime_blocked_count")
        val SEEDED_DEFAULTS = booleanPreferencesKey("has_seeded_defaults")
    }

    val settingsFlow: Flow<PrivacySettings> = context.privacyDataStore.data.map { prefs ->
        PrivacySettings(
            adBlockEnabled = prefs[Keys.AD_BLOCK] ?: true,
            cosmeticBlockingEnabled = prefs[Keys.COSMETIC_BLOCK] ?: true,
            httpsOnlyMode = prefs[Keys.HTTPS_ONLY] ?: true,
            blockThirdPartyCookies = prefs[Keys.BLOCK_3P_COOKIES] ?: true,
            stripTrackingParams = prefs[Keys.STRIP_TRACKERS] ?: true,
            javascriptEnabled = prefs[Keys.JS_ENABLED] ?: true,
            ephemeralMode = prefs[Keys.EPHEMERAL] ?: true,
            sendGpcHeader = prefs[Keys.SEND_GPC] ?: true,
            searchEngine = prefs[Keys.SEARCH_ENGINE]
                ?.let { name -> SearchEngine.entries.firstOrNull { it.name == name } }
                ?: SearchEngine.DUCKDUCKGO,
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                ?: ThemeMode.SYSTEM,
            lifetimeBlockedCount = prefs[Keys.LIFETIME_BLOCKED] ?: 0L,
            hasSeededDefaults = prefs[Keys.SEEDED_DEFAULTS] ?: false
        )
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) = updateBool(Keys.AD_BLOCK, enabled)
    suspend fun setCosmeticBlockingEnabled(enabled: Boolean) = updateBool(Keys.COSMETIC_BLOCK, enabled)
    suspend fun setHttpsOnlyMode(enabled: Boolean) = updateBool(Keys.HTTPS_ONLY, enabled)
    suspend fun setBlockThirdPartyCookies(enabled: Boolean) = updateBool(Keys.BLOCK_3P_COOKIES, enabled)
    suspend fun setStripTrackingParams(enabled: Boolean) = updateBool(Keys.STRIP_TRACKERS, enabled)
    suspend fun setJavascriptEnabled(enabled: Boolean) = updateBool(Keys.JS_ENABLED, enabled)
    suspend fun setEphemeralMode(enabled: Boolean) = updateBool(Keys.EPHEMERAL, enabled)
    suspend fun setSendGpcHeader(enabled: Boolean) = updateBool(Keys.SEND_GPC, enabled)

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.privacyDataStore.edit { it[Keys.SEARCH_ENGINE] = engine.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.privacyDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun incrementLifetimeBlocked(delta: Int = 1) {
        if (delta <= 0) return
        context.privacyDataStore.edit { prefs ->
            val current = prefs[Keys.LIFETIME_BLOCKED] ?: 0L
            prefs[Keys.LIFETIME_BLOCKED] = current + delta
        }
    }

    suspend fun markDefaultsSeeded() = updateBool(Keys.SEEDED_DEFAULTS, true)

    private suspend fun updateBool(key: Preferences.Key<Boolean>, value: Boolean) {
        context.privacyDataStore.edit { it[key] = value }
    }
}
