package it.fra06083.streamsctv.net

import android.content.Context
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class HttpResult(
    val status: Int,
    val text: String,
    val finalUrl: String
)

object Http {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val list = cookieStore.getOrPut(url.host) { mutableListOf() }
            synchronized(list) {
                cookies.forEach { newCookie ->
                    list.removeAll { it.name == newCookie.name && it.path == newCookie.path }
                    list.add(newCookie)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val list = cookieStore[url.host] ?: return emptyList()
            val now = System.currentTimeMillis()
            synchronized(list) {
                list.removeAll { it.expiresAt < now }
                return list.toList()
            }
        }
    }

    private var cacheDir: File? = null

    val client: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
        cacheDir?.let { builder.cache(Cache(it, 30L * 1024 * 1024)) }
        builder.build()
    }

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "http_cache").also { it.mkdirs() }
    }

    const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 9; AFTMM Build/PS7233) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun fetch(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String = ""
    ): HttpResult {
        val builder = Request.Builder().url(url)
        var hasUserAgent = false
        var hasAcceptLanguage = false
        headers.forEach { (key, value) ->
            if (key.equals("User-Agent", true)) hasUserAgent = true
            if (key.equals("Accept-Language", true)) hasAcceptLanguage = true
            try {
                builder.header(key, value)
            } catch (_: Throwable) {
            }
        }
        if (!hasUserAgent) builder.header("User-Agent", DEFAULT_UA)
        if (!hasAcceptLanguage) builder.header("Accept-Language", "it-IT,it;q=0.9,en;q=0.7")

        val requestBody = when {
            method.equals("POST", true) -> body.toRequestBody(
                (headers["Content-Type"] ?: "application/x-www-form-urlencoded").toMediaType()
            )
            method.equals("PUT", true) -> body.toRequestBody(
                (headers["Content-Type"] ?: "application/json").toMediaType()
            )
            else -> null
        }
        builder.method(method.uppercase(), requestBody)

        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string() ?: ""
            return HttpResult(
                status = response.code,
                text = text,
                finalUrl = response.request.url.toString()
            )
        }
    }

    fun bytes(url: String, headers: Map<String, String> = emptyMap()): ByteArray {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> try { builder.header(k, v) } catch (_: Throwable) {} }
        if (headers.keys.none { it.equals("User-Agent", true) }) {
            builder.header("User-Agent", DEFAULT_UA)
        }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.bytes() ?: ByteArray(0)
        }
    }

    fun clearCookies() {
        cookieStore.clear()
    }
}
