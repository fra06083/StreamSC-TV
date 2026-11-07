package it.fra06083.streamsctv.net

import it.fra06083.streamsctv.data.ExtractorLink
import org.json.JSONObject
import java.net.URLEncoder

object VixSrcResolver {

    private const val BASE = "https://vixsrc.to"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 9; AFTMM Build/PS7233) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun resolve(
        tmdbId: String,
        isSeries: Boolean,
        season: Int?,
        episode: Int?
    ): ExtractorLink? {
        return try {
            val path = if (isSeries) {
                if (season == null || episode == null) return null
                "/api/tv/$tmdbId/$season/$episode"
            } else {
                "/api/movie/$tmdbId"
            }
            val headers = mapOf("Referer" to "$BASE/", "User-Agent" to UA)
            val api = Http.fetch(BASE + path, headers = headers)
            if (api.status !in 200..299) return null
            val src = JSONObject(api.text).optString("src")
            if (src.isBlank()) return null
            val embedUrl = if (src.startsWith("http")) src else BASE + src
            val embed = Http.fetch(embedUrl, headers = headers)
            if (embed.status !in 200..299) return null
            val html = embed.text

            val blockIndex = html.indexOf("window.masterPlaylist")
            if (blockIndex < 0) return null
            val block = html.substring(blockIndex, minOf(html.length, blockIndex + 2400))

            val token = match(block, Regex("""['"]token['"]\s*:\s*['"]([^'"]+)['"]""")) ?: return null
            val expires = match(block, Regex("""['"]expires['"]\s*:\s*['"]([^'"]+)['"]""")) ?: return null
            val playlist = match(block, Regex("""url\s*:\s*['"](https?://[^'"]*?/playlist/[^'"]*)['"]"""))
                ?: return null

            val fhd = html.contains("window.canPlayFHD = true") ||
                Regex("""canPlayFHD['"]?\s*[:=]\s*['"]?1""").containsMatchIn(html)
            val finalUrl = buildString {
                append(playlist.substringBefore("?"))
                append("?ub=1&token=")
                append(URLEncoder.encode(token, "UTF-8"))
                append("&expires=")
                append(URLEncoder.encode(expires, "UTF-8"))
                if (fhd) append("&h=1")
            }
            ExtractorLink(
                name = "VixSrc",
                url = finalUrl,
                type = "M3U8",
                referer = "$BASE/",
                quality = if (fhd) 1080 else null,
                headers = mapOf("Referer" to "$BASE/", "User-Agent" to UA)
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun resolveEmbed(embedUrl: String, referer: String? = null): ExtractorLink? {
        return try {
            val ref = referer ?: "$BASE/"
            val headers = mapOf("Referer" to ref, "User-Agent" to UA)
            val embed = Http.fetch(embedUrl, headers = headers)
            if (embed.status !in 200..299) return null
            val html = embed.text
            val blockIndex = html.indexOf("window.masterPlaylist")
            if (blockIndex < 0) return null
            val block = html.substring(blockIndex, minOf(html.length, blockIndex + 2400))
            val token = match(block, Regex("""['"]token['"]\s*:\s*['"]([^'"]+)['"]""")) ?: return null
            val expires = match(block, Regex("""['"]expires['"]\s*:\s*['"]([^'"]+)['"]""")) ?: return null
            val playlist = match(block, Regex("""url\s*:\s*['"](https?://[^'"]*?/playlist/[^'"]*)['"]"""))
                ?: return null
            val fhd = html.contains("window.canPlayFHD = true")
            val finalUrl = playlist.substringBefore("?") +
                "?ub=1&token=" + URLEncoder.encode(token, "UTF-8") +
                "&expires=" + URLEncoder.encode(expires, "UTF-8") +
                if (fhd) "&h=1" else ""
            ExtractorLink(
                name = "VixCloud",
                url = finalUrl,
                type = "M3U8",
                referer = ref,
                quality = if (fhd) 1080 else null,
                headers = mapOf("Referer" to ref, "User-Agent" to UA)
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun match(text: String, regex: Regex): String? =
        regex.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
}
