package it.fra06083.streamsctv.ext

import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import it.fra06083.streamsctv.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.Closeable

class ProviderRuntime(
    private val extension: InstalledExtension,
    private val baseUrlOverride: String? = null
) : Closeable {

    companion object {
        private const val TAG = "ProviderRuntime"
        private const val CALL_TIMEOUT_MS = 60_000L
    }

    private val mutex = Mutex()
    private var js: QuickJs? = null

    val manifest: ProviderManifest get() = extension.manifest

    private suspend fun ensureStarted() {
        if (js != null) return
        val runtime = withContext(Dispatchers.IO) { QuickJs.create(Dispatchers.IO) }
        try {
            runtime.function("__nativeLog") { args ->
                val message = args.joinToString(" ") { it?.toString() ?: "null" }
                Log.d("JS:${manifest.id}", message)
                null
            }

            runtime.function("__nativeParseHTML") { args ->
                val html = args.getOrNull(0)?.toString() ?: ""
                val selector = args.getOrNull(1)?.toString() ?: ""
                val array = JSONArray()
                try {
                    val doc = Jsoup.parse(html)
                    val elements = doc.select(selector)
                    elements.forEach { element ->
                        array.put(
                            JSONObject()
                                .put("text", element.text())
                                .put("outerHTML", element.outerHtml())
                                .put("href", element.attr("href"))
                                .put("src", element.attr("src"))
                                .put("dataSrc", element.attr("data-src"))
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "parseHTML failed", t)
                }
                array.toString()
            }

            runtime.asyncFunction("__nativeFetch") { args ->
                val url = args.getOrNull(0)?.toString() ?: ""
                val method = args.getOrNull(1)?.toString() ?: "GET"
                val headersJson = args.getOrNull(2)?.toString() ?: "{}"
                val body = args.getOrNull(3)?.toString() ?: ""
                val headers = mutableMapOf<String, String>()
                try {
                    val obj = JSONObject(headersJson)
                    obj.keys().forEach { key -> headers[key] = obj.optString(key) }
                } catch (_: Throwable) {
                }
                try {
                    val result = Http.fetch(url, method, headers, body)
                    JSONObject()
                        .put("status", result.status)
                        .put("text", result.text)
                        .put("url", result.finalUrl)
                        .toString()
                } catch (t: Throwable) {
                    Log.w(TAG, "fetch failed: $url", t)
                    JSONObject()
                        .put("status", 599)
                        .put("text", "")
                        .put("url", url)
                        .put("error", t.message ?: "network error")
                        .toString()
                }
            }

            val bootstrap = """
                var console = {
                    log: function() { __nativeLog(Array.prototype.slice.call(arguments).join(" ")); },
                    warn: function() { __nativeLog(Array.prototype.slice.call(arguments).join(" ")); },
                    error: function() { __nativeLog(Array.prototype.slice.call(arguments).join(" ")); },
                    debug: function() { __nativeLog(Array.prototype.slice.call(arguments).join(" ")); }
                };
                async function fetch(url, options) {
                    var opts = options || {};
                    var headers = {};
                    try { headers = opts.headers || {}; } catch (e) {}
                    var result = await __nativeFetch(
                        String(url || ""),
                        opts.method ? String(opts.method) : "GET",
                        JSON.stringify(headers),
                        (opts.body !== undefined && opts.body !== null) ? String(opts.body) : ""
                    );
                    var parsed = JSON.parse(result);
                    return {
                        status: parsed.status,
                        ok: parsed.status >= 200 && parsed.status < 300,
                        url: parsed.url,
                        text: async function() { return parsed.text; },
                        json: async function() { return JSON.parse(parsed.text); }
                    };
                }
                function parseHTML(html, selector) {
                    try {
                        return JSON.parse(__nativeParseHTML(String(html || ""), String(selector || "")));
                    } catch (e) {
                        return [];
                    }
                }
                async function webViewScrape(url, selector) {
                    throw new Error("webViewScrape non supportato su StreamSC TV");
                }
                async function __callJson(name, argsJson) {
                    try {
                        var fn = globalThis[name];
                        if (typeof fn !== "function") {
                            throw new Error("Funzione provider mancante: " + name);
                        }
                        var args = JSON.parse(argsJson);
                        var result = await fn.apply(null, args);
                        return JSON.stringify({ ok: true, data: result === undefined ? null : result });
                    } catch (e) {
                        return JSON.stringify({ ok: false, error: String(e && e.message ? e.message : e) });
                    }
                }
            """.trimIndent()

            runtime.evaluate<Any?>(bootstrap, "bootstrap.js")

            var script = extension.scriptFile.readText()
            if (!baseUrlOverride.isNullOrBlank()) {
                script = script.replace(
                    Regex("""(?:const|let|var)\s+BASE_URL\s*=\s*["'][^"']*["']"""),
                    "const BASE_URL = \"${baseUrlOverride.replace("\"", "\\\"")}\""
                )
                val host = try {
                    java.net.URI(baseUrlOverride).host
                } catch (_: Throwable) {
                    null
                }
                if (!host.isNullOrBlank()) {
                    script = script.replace(
                        Regex("""(?:const|let|var)\s+DEFAULT_CDN\s*=\s*["'][^"']*["']"""),
                        "const DEFAULT_CDN = \"https://cdn.$host\""
                    )
                }
            }
            runtime.evaluate<Any?>(script, "provider.js")
            js = runtime
        } catch (t: Throwable) {
            runtime.close()
            throw t
        }
    }

    suspend fun call(functionName: String, args: JSONArray): JSONObject {
        return mutex.withLock {
            ensureStarted()
            withTimeout(CALL_TIMEOUT_MS) {
                val runtime = js ?: error("Runtime non inizializzato")
                val argsJson = args.toString()
                val expression = "__callJson(${JSONObject.quote(functionName)}, ${JSONObject.quote(argsJson)})"
                val raw = runtime.evaluate<String>(expression, "call.js")
                JSONObject(raw)
            }
        }
    }

    override fun close() {
        try {
            js?.close()
        } catch (_: Throwable) {
        }
        js = null
    }
}
