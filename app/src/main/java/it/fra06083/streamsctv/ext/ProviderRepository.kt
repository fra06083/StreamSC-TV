package it.fra06083.streamsctv.ext

import android.content.Context
import android.util.Log
import it.fra06083.streamsctv.data.ExtractorLink
import it.fra06083.streamsctv.data.MainPageResult
import it.fra06083.streamsctv.data.MediaDetail
import it.fra06083.streamsctv.data.MediaItem
import it.fra06083.streamsctv.data.ProviderPrefs
import it.fra06083.streamsctv.data.Season
import it.fra06083.streamsctv.data.Episode
import it.fra06083.streamsctv.net.Http
import it.fra06083.streamsctv.net.VixSrcResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

class ProviderException(message: String) : Exception(message)

class ProviderRepository(
    private val context: Context,
    private val extensions: ExtensionManager
) {

    companion object {
        private const val TAG = "ProviderRepository"
    }

    private val prefs = ProviderPrefs(context)
    private val runtimes = ConcurrentHashMap<String, RuntimeHolder>()
    private val runtimeMutex = Mutex()

    private data class RuntimeHolder(
        val version: String,
        val baseUrl: String?,
        val runtime: ProviderRuntime
    )

    private val homeCache = ConcurrentHashMap<String, CachedPage>()
    private data class CachedPage(val at: Long, val result: MainPageResult)

    fun providerName(id: String): String =
        extensions.findById(id)?.manifest?.name ?: id

    private suspend fun runtimeFor(ext: InstalledExtension): ProviderRuntime {
        val baseUrl = resolveBaseUrl(ext)
        val key = ext.manifest.id
        runtimes[key]?.let { holder ->
            if (holder.version == ext.manifest.version && holder.baseUrl == baseUrl) return holder.runtime
        }
        return runtimeMutex.withLock {
            runtimes[key]?.let { holder ->
                if (holder.version == ext.manifest.version && holder.baseUrl == baseUrl) return@withLock holder.runtime
                try { holder.runtime.close() } catch (_: Throwable) {}
                runtimes.remove(key)
            }
            val runtime = ProviderRuntime(ext, baseUrl)
            runtimes[key] = RuntimeHolder(ext.manifest.version, baseUrl, runtime)
            runtime
        }
    }

    private fun resolveBaseUrl(ext: InstalledExtension): String? {
        if (ext.manifest.id != "streamingcommunity") return null
        val now = System.currentTimeMillis()
        val cached = prefs.domain(ext.manifest.id)
        if (!cached.isNullOrBlank() && now - prefs.domainCheckedAt(ext.manifest.id) < 6 * 60 * 60 * 1000L) {
            return cached
        }
        val candidates = linkedSetOf(
            ext.manifest.baseUrl,
            "https://streamingcommunityz.boo",
            "https://streamingcommunityz.bar",
            "https://streaming-communityz.autos"
        ).filter { it.isNotBlank() }
        for (candidate in candidates) {
            try {
                val result = Http.fetch(candidate.trimEnd('/') + "/it")
                if (result.status in 200..399) {
                    val uri = URI(result.finalUrl)
                    val host = uri.host
                    if (!host.isNullOrBlank()) {
                        val base = "${uri.scheme ?: "https"}://$host"
                        prefs.setDomain(ext.manifest.id, base)
                        prefs.setDomainCheckedAt(ext.manifest.id, now)
                        return base
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return cached ?: ext.manifest.baseUrl.ifBlank { null }
    }

    suspend fun home(ext: InstalledExtension, sectionKey: String, page: Int = 1): MainPageResult =
        withContext(Dispatchers.IO) { homeInternal(ext, sectionKey, page) }

    private suspend fun homeInternal(ext: InstalledExtension, sectionKey: String, page: Int): MainPageResult {
        val cacheKey = "${ext.manifest.id}:$sectionKey:$page"
        homeCache[cacheKey]?.let {
            if (System.currentTimeMillis() - it.at < 10 * 60 * 1000L) return it.result
        }
        val runtime = runtimeFor(ext)
        val args = JSONArray().put(page).put(sectionKey)
        val response = runtime.call("getMainPage", args)
        if (!response.optBoolean("ok", false)) {
            throw ProviderException(response.optString("error", "Errore provider"))
        }
        val data = response.opt("data")
        val result = parseMainPage(data, ext.manifest)
        homeCache[cacheKey] = CachedPage(System.currentTimeMillis(), result)
        return result
    }

    suspend fun search(ext: InstalledExtension, query: String, page: Int = 1): List<MediaItem> =
        withContext(Dispatchers.IO) { searchInternal(ext, query, page) }

    private suspend fun searchInternal(ext: InstalledExtension, query: String, page: Int): List<MediaItem> {
        val runtime = runtimeFor(ext)
        val args = JSONArray().put(query).put(page)
        val response = runtime.call("search", args)
        if (!response.optBoolean("ok", false)) {
            throw ProviderException(response.optString("error", "Errore ricerca"))
        }
        val data = response.opt("data") as? JSONArray ?: return emptyList()
        val result = mutableListOf<MediaItem>()
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            result.add(parseMediaItem(o, ext.manifest))
        }
        return result
    }

    suspend fun load(ext: InstalledExtension, url: String): MediaDetail =
        withContext(Dispatchers.IO) { loadInternal(ext, url) }

    private suspend fun loadInternal(ext: InstalledExtension, url: String): MediaDetail {
        val runtime = runtimeFor(ext)
        val args = JSONArray().put(url)
        val response = runtime.call("load", args)
        if (!response.optBoolean("ok", false)) {
            throw ProviderException(response.optString("error", "Impossibile caricare il titolo"))
        }
        val data = response.opt("data") as? JSONObject
            ?: throw ProviderException("Risposta provider non valida")
        return parseDetail(data, ext.manifest)
    }

    suspend fun loadLinks(
        ext: InstalledExtension,
        loadData: String,
        isSeries: Boolean,
        tmdbId: String?,
        season: Int?,
        episode: Int?
    ): List<ExtractorLink> = withContext(Dispatchers.IO) {
        val links = mutableListOf<ExtractorLink>()
        try {
            val runtime = runtimeFor(ext)
            val args = JSONArray().put(loadData)
            val response = runtime.call("loadLinks", args)
            if (response.optBoolean("ok", false)) {
                val data = response.opt("data")
                if (data is JSONArray) {
                    for (i in 0 until data.length()) {
                        val o = data.optJSONObject(i) ?: continue
                        val url = o.optString("url")
                        if (url.isBlank()) continue
                        val headersObj = o.optJSONObject("headers")
                        val headers = mutableMapOf<String, String>()
                        headersObj?.keys()?.forEach { key -> headers[key] = headersObj.optString(key) }
                        val referer = o.optString("referer").ifBlank { headers["Referer"] ?: "" }
                        if (referer.isNotBlank() && headers.keys.none { it.equals("Referer", true) }) {
                            headers["Referer"] = referer
                        }
                        links.add(
                            ExtractorLink(
                                name = o.optString("name", ext.manifest.name),
                                url = url,
                                type = o.optString("type").ifBlank { null },
                                referer = referer.ifBlank { null },
                                quality = if (o.has("quality") && !o.isNull("quality")) o.optInt("quality") else null,
                                headers = headers
                            )
                        )
                    }
                }
            } else {
                Log.w(TAG, "loadLinks error: ${response.optString("error")}")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "loadLinks exception", t)
        }

        if (links.isEmpty()) {
            val fallback = fallbackLinks(loadData, isSeries, tmdbId, season, episode)
            if (fallback != null) links.add(fallback)
        }
        links.filter { it.url.isNotBlank() }
    }

    private fun fallbackLinks(
        loadData: String,
        isSeries: Boolean,
        tmdbId: String?,
        season: Int?,
        episode: Int?
    ): ExtractorLink? {
        var tmdb = tmdbId
        var seasonNumber = season
        var episodeNumber = episode
        var embedUrl: String? = null
        try {
            val obj = JSONObject(loadData)
            if (tmdb.isNullOrBlank()) {
                val raw = obj.opt("tmdb_id")
                if (raw != null && raw != JSONObject.NULL) tmdb = raw.toString()
            }
            if (seasonNumber == null && obj.has("season")) seasonNumber = obj.optInt("season")
            if (episodeNumber == null && obj.has("episode")) episodeNumber = obj.optInt("episode")
            val url = obj.optString("url")
            if (url.contains("vixcloud.cc/embed/")) embedUrl = url
        } catch (_: Throwable) {
        }

        if (!tmdb.isNullOrBlank() && tmdb != "null") {
            VixSrcResolver.resolve(tmdb, isSeries, seasonNumber, episodeNumber)?.let { return it }
        }
        if (embedUrl != null) {
            VixSrcResolver.resolveEmbed(embedUrl)?.let { return it }
        }
        return null
    }

    private fun parseMainPage(data: Any?, manifest: ProviderManifest): MainPageResult {
        val items = mutableListOf<MediaItem>()
        var hasNext = false
        when (data) {
            is JSONObject -> {
                val arr = data.optJSONArray("items") ?: data.optJSONArray("data")
                hasNext = data.optBoolean("hasNextPage", false)
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        items.add(parseMediaItem(o, manifest))
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until data.length()) {
                    val o = data.optJSONObject(i) ?: continue
                    items.add(parseMediaItem(o, manifest))
                }
            }
        }
        return MainPageResult(items, hasNext)
    }

    private fun parseMediaItem(o: JSONObject, manifest: ProviderManifest): MediaItem {
        val url = o.str("url")
        val id = o.str("id") ?: url ?: o.str("title").orEmpty()
        return MediaItem(
            id = id,
            title = o.str("title") ?: o.str("name") ?: "",
            type = o.str("type"),
            poster = o.str("poster") ?: o.str("image") ?: o.str("cover"),
            backdrop = o.str("backdrop") ?: o.str("background") ?: o.str("banner"),
            url = url,
            year = o.intOrNull("year"),
            quality = o.str("quality"),
            providerId = manifest.id,
            providerName = manifest.name
        )
    }

    private fun parseDetail(o: JSONObject, manifest: ProviderManifest): MediaDetail {
        val seasonsArr = o.optJSONArray("seasons")
        val seasons = mutableListOf<Season>()
        if (seasonsArr != null) {
            for (i in 0 until seasonsArr.length()) {
                val s = seasonsArr.optJSONObject(i) ?: continue
                val number = s.intOrNull("number") ?: (i + 1)
                val epsArr = s.optJSONArray("episodes")
                val episodes = mutableListOf<Episode>()
                if (epsArr != null) {
                    for (j in 0 until epsArr.length()) {
                        val e = epsArr.optJSONObject(j) ?: continue
                        episodes.add(parseEpisode(e, number))
                    }
                }
                seasons.add(Season(number, episodes))
            }
        }
        val episodesArr = o.optJSONArray("episodes")
        if (seasons.isEmpty() && episodesArr != null) {
            val episodes = mutableListOf<Episode>()
            for (j in 0 until episodesArr.length()) {
                val e = episodesArr.optJSONObject(j) ?: continue
                episodes.add(parseEpisode(e, null))
            }
            if (episodes.isNotEmpty()) seasons.add(Season(1, episodes))
        }

        val genres = mutableListOf<String>()
        o.optJSONArray("genres")?.let { arr ->
            for (i in 0 until arr.length()) {
                val value = arr.opt(i)
                if (value is String) genres.add(value)
                else (value as? JSONObject)?.str("name")?.let { genres.add(it) }
            }
        }
        val cast = mutableListOf<String>()
        o.optJSONArray("cast")?.let { arr ->
            for (i in 0 until arr.length()) {
                val value = arr.opt(i)
                if (value is String) cast.add(value)
                else (value as? JSONObject)?.str("name")?.let { cast.add(it) }
            }
        }

        return MediaDetail(
            id = o.str("id") ?: o.str("url").orEmpty(),
            title = o.str("title") ?: "",
            type = o.str("type"),
            poster = o.str("poster") ?: o.str("image") ?: o.str("cover"),
            background = o.str("background") ?: o.str("backdrop") ?: o.str("banner"),
            plot = o.str("plot") ?: o.str("description"),
            year = o.intOrNull("year"),
            genres = genres,
            cast = cast,
            score = o.doubleOrNull("score") ?: o.doubleOrNull("rating"),
            contentRating = o.str("contentRating"),
            duration = o.intOrNull("duration"),
            seasons = seasons,
            loadData = o.str("loadData") ?: "{}",
            providerId = manifest.id,
            providerName = manifest.name
        )
    }

    private fun parseEpisode(o: JSONObject, season: Int?): Episode {
        val loadDataRaw = o.opt("loadData")
        val loadData = when (loadDataRaw) {
            is String -> loadDataRaw
            is JSONObject -> loadDataRaw.toString()
            null -> "{}"
            else -> loadDataRaw.toString()
        }
        return Episode(
            id = o.str("id") ?: "",
            number = o.intOrNull("number") ?: 0,
            season = o.intOrNull("season") ?: season,
            title = o.str("title") ?: o.str("name"),
            plot = o.str("plot") ?: o.str("description"),
            duration = o.intOrNull("duration"),
            poster = o.str("poster") ?: o.str("image"),
            loadData = loadData
        )
    }

    fun closeAll() {
        runtimes.values.forEach { try { it.runtime.close() } catch (_: Throwable) {} }
        runtimes.clear()
        homeCache.clear()
    }

    private fun JSONObject.str(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is String -> value.ifBlank { null }
            is Number -> value.toString()
            else -> value?.toString()?.ifBlank { null }
        }
    }

    private fun JSONObject.intOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun JSONObject.doubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }
}
