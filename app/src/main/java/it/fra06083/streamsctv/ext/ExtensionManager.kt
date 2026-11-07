package it.fra06083.streamsctv.ext

import android.content.Context
import android.util.Log
import it.fra06083.streamsctv.data.ProviderPrefs
import it.fra06083.streamsctv.net.Http
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class ExtensionManager(private val context: Context) {

    companion object {
        private const val TAG = "ExtensionManager"
        private const val BUNDLED_VERSION = 1
        private const val GITHUB_ZIP =
            "https://github.com/fra06083/StreamSC-Extensions/archive/refs/heads/main.zip"
    }

    private val prefs = ProviderPrefs(context)
    val extensionsDir: File = File(context.filesDir, "extensions").also { it.mkdirs() }

    @Volatile
    private var installed: List<InstalledExtension>? = null

    var onChange: (() -> Unit)? = null

    @Synchronized
    fun ensureBundledInstalled() {
        if (prefs.bundledInstalledVersion() >= BUNDLED_VERSION) {
            initializeDefaultEnabled()
            return
        }
        try {
            val assetNames = context.assets.list("extensions") ?: emptyArray()
            for (name in assetNames) {
                if (!name.endsWith(".scext")) continue
                val manifestText = try {
                    context.assets.open("extensions/$name/manifest.json").bufferedReader().use { it.readText() }
                } catch (_: Throwable) {
                    continue
                }
                val manifest = ProviderManifest.fromJson(JSONObject(manifestText))
                val existing = findById(manifest.id)
                if (existing == null || versionCode(existing.manifest.version) < versionCode(manifest.version)) {
                    val target = File(extensionsDir, name)
                    if (target.exists()) target.deleteRecursively()
                    copyAssetDir("extensions/$name", target)
                }
            }
            prefs.setBundledInstalledVersion(BUNDLED_VERSION)
        } catch (t: Throwable) {
            Log.e(TAG, "Bundled install failed", t)
        }
        installed = null
        initializeDefaultEnabled()
    }

    private fun initializeDefaultEnabled() {
        if (prefs.hasEnabledInitialized()) return
        val ids = listInstalled().filter { it.manifest.status == 1 }.map { it.manifest.id }.toSet()
        prefs.setEnabledIds(ids.ifEmpty { listInstalled().map { it.manifest.id }.toSet() })
    }

    private fun copyAssetDir(assetPath: String, target: File) {
        val children = context.assets.list(assetPath) ?: emptyArray()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
        } else {
            target.mkdirs()
            for (child in children) copyAssetDir("$assetPath/$child", File(target, child))
        }
    }

    @Synchronized
    fun listInstalled(): List<InstalledExtension> {
        installed?.let { return it }
        val result = mutableListOf<InstalledExtension>()
        val dirs = extensionsDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
        for (dir in dirs) {
            val manifestFile = File(dir, "manifest.json")
            if (!manifestFile.isFile) continue
            try {
                val manifest = ProviderManifest.fromJson(
                    JSONObject(manifestFile.readText())
                )
                if (manifest.id.isBlank()) continue
                result.add(InstalledExtension(manifest, dir))
            } catch (t: Throwable) {
                Log.w(TAG, "Bad manifest in ${dir.name}", t)
            }
        }
        val sorted = result.sortedBy { it.manifest.name.lowercase() }
        installed = sorted
        return sorted
    }

    fun findById(id: String): InstalledExtension? = listInstalled().firstOrNull { it.manifest.id == id }

    fun enabledExtensions(): List<InstalledExtension> {
        val ids = prefs.enabledIds()
        return listInstalled().filter { ids.contains(it.manifest.id) }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        prefs.setEnabled(id, enabled)
        onChange?.invoke()
    }

    @Synchronized
    fun delete(id: String): Boolean {
        val ext = findById(id) ?: return false
        ext.directory.deleteRecursively()
        installed = null
        val ids = prefs.enabledIds().toMutableSet()
        ids.remove(id)
        prefs.setEnabledIds(ids)
        onChange?.invoke()
        return true
    }

    @Synchronized
    fun installZip(bytes: ByteArray): InstalledExtension {
        val temp = File(context.cacheDir, "ext_install_${System.currentTimeMillis()}")
        temp.mkdirs()
        try {
            unzip(bytes, temp)
            val manifestFile = findFile(temp, "manifest.json")
                ?: error("manifest.json non trovato nello ZIP")
            val providerDir = manifestFile.parentFile ?: error("Cartella provider non valida")
            val manifest = ProviderManifest.fromJson(JSONObject(manifestFile.readText()))
            if (manifest.id.isBlank()) error("ID provider non valido")
            val target = File(extensionsDir, "${manifest.id}.scext")
            if (target.exists()) target.deleteRecursively()
            providerDir.copyRecursively(target, overwrite = true)
            installed = null
            if (!prefs.hasEnabledInitialized()) {
                prefs.setEnabledIds(listInstalled().filter { it.manifest.status == 1 }.map { it.manifest.id }.toSet())
            }
            onChange?.invoke()
            return findById(manifest.id) ?: error("Installazione non riuscita")
        } finally {
            temp.deleteRecursively()
        }
    }

    fun installFromUrl(url: String): InstalledExtension {
        val bytes = Http.bytes(url)
        return installZip(bytes)
    }

    @Synchronized
    fun syncFromGithub(): Int {
        val bytes = Http.bytes(GITHUB_ZIP)
        val temp = File(context.cacheDir, "ext_sync_${System.currentTimeMillis()}")
        temp.mkdirs()
        var updated = 0
        try {
            unzip(bytes, temp)
            val providerDirs = mutableListOf<File>()
            collectProviderDirs(temp, providerDirs)
            for (dir in providerDirs) {
                val manifestFile = File(dir, "manifest.json")
                if (!manifestFile.isFile) continue
                try {
                    val manifest = ProviderManifest.fromJson(JSONObject(manifestFile.readText()))
                    if (manifest.id.isBlank()) continue
                    val existing = findById(manifest.id)
                    if (existing == null ||
                        versionCode(manifest.version) > versionCode(existing.manifest.version)
                    ) {
                        val target = File(extensionsDir, "${manifest.id}.scext")
                        if (target.exists()) target.deleteRecursively()
                        dir.copyRecursively(target, overwrite = true)
                        updated++
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Sync failed for ${dir.name}", t)
                }
            }
            prefs.setLastSync(System.currentTimeMillis())
            if (updated > 0) {
                installed = null
                if (!prefs.hasEnabledInitialized()) {
                    prefs.setEnabledIds(listInstalled().filter { it.manifest.status == 1 }.map { it.manifest.id }.toSet())
                }
                onChange?.invoke()
            }
        } finally {
            temp.deleteRecursively()
        }
        return updated
    }

    private fun collectProviderDirs(dir: File, out: MutableList<File>) {
        if (dir.name.endsWith(".scext") && File(dir, "manifest.json").isFile) {
            out.add(dir)
            return
        }
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory && child.name != "__MACOSX") collectProviderDirs(child, out)
        }
    }

    private fun findFile(dir: File, name: String): File? {
        if (dir.isFile) return if (dir.name == name) dir else null
        dir.listFiles()?.forEach { child ->
            val found = findFile(child, name)
            if (found != null) return found
        }
        return null
    }

    private fun unzip(bytes: ByteArray, targetDir: File) {
        ZipInputStream(bytes.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (name.contains("..") || name.startsWith("/")) {
                    zip.closeEntry()
                    continue
                }
                val outFile = File(targetDir, name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { output -> zip.copyTo(output) }
                }
                zip.closeEntry()
            }
        }
    }

    fun versionCode(version: String): Long =
        version.filter { it.isDigit() }.toLongOrNull() ?: 0L
}
