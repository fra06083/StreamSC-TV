package it.fra06083.streamsctv.data

import android.content.Context

class ProviderPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("providers", Context.MODE_PRIVATE)

    fun enabledIds(): Set<String> {
        val raw = prefs.getStringSet("enabled", null) ?: return emptySet()
        return raw.toSet()
    }

    fun hasEnabledInitialized(): Boolean = prefs.contains("enabled")

    fun setEnabledIds(ids: Set<String>) {
        prefs.edit().putStringSet("enabled", ids.toMutableSet()).apply()
    }

    fun isEnabled(id: String): Boolean = enabledIds().contains(id)

    fun setEnabled(id: String, enabled: Boolean) {
        val ids = enabledIds().toMutableSet()
        if (enabled) ids.add(id) else ids.remove(id)
        setEnabledIds(ids)
    }

    fun homeProviderId(): String? = prefs.getString("home_provider", null)

    fun setHomeProviderId(id: String) {
        prefs.edit().putString("home_provider", id).apply()
    }

    fun preferHls(): Boolean = prefs.getBoolean("prefer_hls", true)
    fun setPreferHls(value: Boolean) = prefs.edit().putBoolean("prefer_hls", value).apply()

    fun autoplayNext(): Boolean = prefs.getBoolean("autoplay_next", true)
    fun setAutoplayNext(value: Boolean) = prefs.edit().putBoolean("autoplay_next", value).apply()

    fun bundledInstalledVersion(): Int = prefs.getInt("bundled_version", 0)
    fun setBundledInstalledVersion(value: Int) = prefs.edit().putInt("bundled_version", value).apply()

    fun lastSync(): Long = prefs.getLong("last_sync", 0L)
    fun setLastSync(value: Long) = prefs.edit().putLong("last_sync", value).apply()

    fun domain(providerId: String): String? = prefs.getString("domain_$providerId", null)
    fun setDomain(providerId: String, domain: String) = prefs.edit().putString("domain_$providerId", domain).apply()
    fun domainCheckedAt(providerId: String): Long = prefs.getLong("domain_time_$providerId", 0L)
    fun setDomainCheckedAt(providerId: String, value: Long) = prefs.edit().putLong("domain_time_$providerId", value).apply()
}
