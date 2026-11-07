package it.fra06083.streamsctv.data

data class Profile(
    val id: String,
    val name: String,
    val avatar: Int
)

data class MediaItem(
    val id: String,
    val title: String,
    val type: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val url: String? = null,
    val year: Int? = null,
    val quality: String? = null,
    val providerId: String,
    val providerName: String
) {
    val key: String get() = "$providerId:$id"
}

data class Episode(
    val id: String,
    val number: Int,
    val season: Int? = null,
    val title: String? = null,
    val plot: String? = null,
    val duration: Int? = null,
    val poster: String? = null,
    val loadData: String
)

data class Season(
    val number: Int,
    val episodes: List<Episode>
)

data class MediaDetail(
    val id: String,
    val title: String,
    val type: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val plot: String? = null,
    val year: Int? = null,
    val genres: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val score: Double? = null,
    val contentRating: String? = null,
    val duration: Int? = null,
    val seasons: List<Season> = emptyList(),
    val loadData: String,
    val providerId: String,
    val providerName: String
) {
    val isSeries: Boolean get() = seasons.isNotEmpty() || type == "TvSeries" || type == "Anime"
    val key: String get() = "$providerId:$id"
    fun toMediaItem(): MediaItem = MediaItem(
        id = id,
        title = title,
        type = type,
        poster = poster,
        backdrop = background,
        url = null,
        year = year,
        quality = null,
        providerId = providerId,
        providerName = providerName
    )
}

data class ExtractorLink(
    val name: String,
    val url: String,
    val type: String? = null,
    val referer: String? = null,
    val quality: Int? = null,
    val headers: Map<String, String> = emptyMap()
)

data class HistoryEntry(
    val providerId: String,
    val mediaId: String,
    val title: String,
    val poster: String? = null,
    val type: String? = null,
    val url: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = 0L,
    val loadData: String? = null,
    val tmdbId: String? = null
) {
    val key: String get() = "$providerId:$mediaId"
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    fun toMediaItem(providerName: String): MediaItem = MediaItem(
        id = mediaId,
        title = title,
        type = type,
        poster = poster,
        url = url,
        providerId = providerId,
        providerName = providerName
    )
}

data class FavoriteEntry(
    val providerId: String,
    val mediaId: String,
    val title: String,
    val poster: String? = null,
    val type: String? = null,
    val year: Int? = null,
    val url: String? = null,
    val addedAt: Long = 0L
) {
    val key: String get() = "$providerId:$mediaId"
    fun toMediaItem(providerName: String): MediaItem = MediaItem(
        id = mediaId,
        title = title,
        type = type,
        poster = poster,
        url = url,
        providerId = providerId,
        providerName = providerName
    )
}

data class MainPageResult(
    val items: List<MediaItem>,
    val hasNextPage: Boolean = false
)
