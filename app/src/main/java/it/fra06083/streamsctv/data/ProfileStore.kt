package it.fra06083.streamsctv.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProfileStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    val avatars = listOf(
        "profile_bg_blue",
        "profile_bg_dark_blue",
        "profile_bg_orange",
        "profile_bg_pink",
        "profile_bg_purple",
        "profile_bg_red",
        "profile_bg_teal"
    )

    init {
        if (profiles().isEmpty()) {
            create("Profilo 1", 0)
        }
    }

    fun profiles(): List<Profile> {
        val raw = prefs.getString("list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                Profile(
                    id = o.optString("id"),
                    name = o.optString("name", "Profilo"),
                    avatar = o.optInt("avatar", 0)
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun activeProfile(): Profile? {
        val id = prefs.getString("active", null) ?: return null
        return profiles().firstOrNull { it.id == id }
    }

    fun setActive(id: String) {
        prefs.edit().putString("active", id).apply()
    }

    fun create(name: String, avatar: Int): Profile {
        val profile = Profile(UUID.randomUUID().toString().take(8), name.ifBlank { "Profilo" }, avatar)
        val list = profiles().toMutableList()
        list.add(profile)
        save(list)
        return profile
    }

    fun update(profile: Profile) {
        val list = profiles().toMutableList()
        val index = list.indexOfFirst { it.id == profile.id }
        if (index >= 0) list[index] = profile
        save(list)
    }

    fun delete(id: String): Boolean {
        val list = profiles().toMutableList()
        if (list.size <= 1) return false
        list.removeAll { it.id == id }
        save(list)
        if (prefs.getString("active", null) == id) {
            prefs.edit().putString("active", list.first().id).apply()
        }
        profilePrefs(id).edit().clear().apply()
        return true
    }

    private fun save(list: List<Profile>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("avatar", it.avatar)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }

    // ── Per-profile data ────────────────────────────────────────────────────

    private fun profilePrefs(id: String): SharedPreferences =
        appContext.getSharedPreferences("profile_$id", Context.MODE_PRIVATE)

    fun history(profileId: String): List<HistoryEntry> {
        val raw = profilePrefs(profileId).getString("history", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                HistoryEntry(
                    providerId = o.optString("providerId"),
                    mediaId = o.optString("mediaId"),
                    title = o.optString("title"),
                    poster = o.optString("poster").ifBlank { null },
                    type = o.optString("type").ifBlank { null },
                    url = o.optString("url"),
                    season = if (o.isNull("season")) null else o.optInt("season"),
                    episode = if (o.isNull("episode")) null else o.optInt("episode"),
                    episodeTitle = o.optString("episodeTitle").ifBlank { null },
                    positionMs = o.optLong("positionMs"),
                    durationMs = o.optLong("durationMs"),
                    updatedAt = o.optLong("updatedAt"),
                    loadData = o.optString("loadData").ifBlank { null },
                    tmdbId = o.optString("tmdbId").ifBlank { null }
                )
            }.sortedByDescending { it.updatedAt }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun upsertHistory(profileId: String, entry: HistoryEntry) {
        val prefs = profilePrefs(profileId)
        val list = history(profileId).toMutableList()
        list.removeAll { it.key == entry.key }
        list.add(0, entry.copy(updatedAt = System.currentTimeMillis()))
        saveHistory(prefs, list)
    }

    fun removeHistory(profileId: String, key: String) {
        val prefs = profilePrefs(profileId)
        saveHistory(prefs, history(profileId).filterNot { it.key == key })
    }

    fun clearHistory(profileId: String) {
        profilePrefs(profileId).edit().remove("history").apply()
    }

    private fun saveHistory(prefs: SharedPreferences, list: List<HistoryEntry>) {
        val arr = JSONArray()
        list.take(60).forEach {
            arr.put(
                JSONObject()
                    .put("providerId", it.providerId)
                    .put("mediaId", it.mediaId)
                    .put("title", it.title)
                    .put("poster", it.poster ?: "")
                    .put("type", it.type ?: "")
                    .put("url", it.url)
                    .put("season", it.season ?: JSONObject.NULL)
                    .put("episode", it.episode ?: JSONObject.NULL)
                    .put("episodeTitle", it.episodeTitle ?: "")
                    .put("positionMs", it.positionMs)
                    .put("durationMs", it.durationMs)
                    .put("updatedAt", it.updatedAt)
                    .put("loadData", it.loadData ?: "")
                    .put("tmdbId", it.tmdbId ?: "")
            )
        }
        prefs.edit().putString("history", arr.toString()).apply()
    }

    fun favorites(profileId: String): List<FavoriteEntry> {
        val raw = profilePrefs(profileId).getString("favorites", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                FavoriteEntry(
                    providerId = o.optString("providerId"),
                    mediaId = o.optString("mediaId"),
                    title = o.optString("title"),
                    poster = o.optString("poster").ifBlank { null },
                    type = o.optString("type").ifBlank { null },
                    year = if (o.isNull("year")) null else o.optInt("year"),
                    url = o.optString("url").ifBlank { null },
                    addedAt = o.optLong("addedAt")
                )
            }.sortedByDescending { it.addedAt }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun isFavorite(profileId: String, key: String): Boolean =
        favorites(profileId).any { it.key == key }

    fun toggleFavorite(profileId: String, item: MediaItem): Boolean {
        val prefs = profilePrefs(profileId)
        val list = favorites(profileId).toMutableList()
        val key = item.key
        val existing = list.firstOrNull { it.key == key }
        return if (existing != null) {
            list.remove(existing)
            saveFavorites(prefs, list)
            false
        } else {
            list.add(
                0,
                FavoriteEntry(
                    providerId = item.providerId,
                    mediaId = item.id,
                    title = item.title,
                    poster = item.poster,
                    type = item.type,
                    year = item.year,
                    url = item.url,
                    addedAt = System.currentTimeMillis()
                )
            )
            saveFavorites(prefs, list)
            true
        }
    }

    private fun saveFavorites(prefs: SharedPreferences, list: List<FavoriteEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("providerId", it.providerId)
                    .put("mediaId", it.mediaId)
                    .put("title", it.title)
                    .put("poster", it.poster ?: "")
                    .put("type", it.type ?: "")
                    .put("year", it.year ?: JSONObject.NULL)
                    .put("url", it.url ?: "")
                    .put("addedAt", it.addedAt)
            )
        }
        prefs.edit().putString("favorites", arr.toString()).apply()
    }
}
