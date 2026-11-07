package it.fra06083.streamsctv.ext

import org.json.JSONObject

data class ManifestSection(
    val key: String,
    val label: String
)

data class ProviderManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val language: String,
    val baseUrl: String,
    val types: List<String>,
    val status: Int,
    val author: String,
    val iconUrl: String?,
    val hasMainPage: Boolean,
    val hasSearch: Boolean,
    val hasLoadLinks: Boolean,
    val sections: List<ManifestSection>
) {
    val statusLabel: String
        get() = when (status) {
            0 -> "Non disponibile"
            2 -> "Lento"
            3 -> "Beta"
            else -> "OK"
        }

    companion object {
        fun fromJson(json: JSONObject): ProviderManifest {
            val caps = json.optJSONObject("capabilities")
            val sectionsArr = json.optJSONArray("mainPageSections")
            val sections = mutableListOf<ManifestSection>()
            if (sectionsArr != null) {
                for (i in 0 until sectionsArr.length()) {
                    val o = sectionsArr.optJSONObject(i) ?: continue
                    sections.add(
                        ManifestSection(
                            key = o.optString("key"),
                            label = o.optString("label", o.optString("key"))
                        )
                    )
                }
            }
            val typesArr = json.optJSONArray("types")
            val types = mutableListOf<String>()
            if (typesArr != null) {
                for (i in 0 until typesArr.length()) types.add(typesArr.optString(i))
            }
            return ProviderManifest(
                id = json.optString("id"),
                name = json.optString("name", json.optString("id")),
                version = json.opt("version")?.toString() ?: "0",
                description = json.optString("description"),
                language = json.optString("language", "it"),
                baseUrl = json.optString("baseUrl"),
                types = types,
                status = json.optInt("status", 1),
                author = json.optString("author"),
                iconUrl = json.optString("iconUrl").ifBlank { null },
                hasMainPage = caps?.optBoolean("hasMainPage", true) ?: true,
                hasSearch = caps?.optBoolean("hasSearch", true) ?: true,
                hasLoadLinks = caps?.optBoolean("hasLoadLinks", true) ?: true,
                sections = sections
            )
        }
    }
}

data class InstalledExtension(
    val manifest: ProviderManifest,
    val directory: java.io.File
) {
    val iconFile: java.io.File? get() = java.io.File(directory, "icon.png").takeIf { it.isFile }
    val scriptFile: java.io.File get() = java.io.File(directory, "provider.js")
}
