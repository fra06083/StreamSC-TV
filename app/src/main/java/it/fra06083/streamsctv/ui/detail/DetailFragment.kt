package it.fra06083.streamsctv.ui.detail

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.data.Episode
import it.fra06083.streamsctv.data.ExtractorLink
import it.fra06083.streamsctv.data.MediaDetail
import it.fra06083.streamsctv.net.Http
import it.fra06083.streamsctv.ui.loadPoster
import it.fra06083.streamsctv.ui.player.PlayerActivity
import it.fra06083.streamsctv.ui.visible
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class DetailFragment : Fragment() {

    companion object {
        private const val ARG_PROVIDER = "provider"
        private const val ARG_URL = "url"

        fun newInstance(providerId: String, url: String?): DetailFragment {
            val fragment = DetailFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_PROVIDER, providerId)
                putString(ARG_URL, url)
            }
            return fragment
        }
    }

    private lateinit var background: ImageView
    private lateinit var loading: ShimmerFrameLayout
    private lateinit var errorBox: View
    private lateinit var errorText: TextView
    private lateinit var content: View
    private lateinit var titleView: TextView
    private lateinit var episodesText: TextView
    private lateinit var playMovie: View
    private lateinit var playMovieButton: MaterialButton
    private lateinit var playSeries: View
    private lateinit var playSeriesButton: MaterialButton
    private lateinit var resumeSeries: View
    private lateinit var resumeSeriesButton: MaterialButton
    private lateinit var favorite: View
    private lateinit var favoriteButton: MaterialButton
    private lateinit var favoriteText: TextView
    private lateinit var episodesShow: View
    private lateinit var episodesShowButton: MaterialButton
    private lateinit var metaSite: MaterialButton
    private lateinit var metaType: TextView
    private lateinit var metaYear: TextView
    private lateinit var metaRating: TextView
    private lateinit var metaDuration: TextView
    private lateinit var description: TextView
    private lateinit var castText: TextView
    private lateinit var tags: ChipGroup
    private lateinit var episodesShadow: View
    private lateinit var episodeHolder: View
    private lateinit var seasonRecycler: RecyclerView
    private lateinit var episodeRecycler: RecyclerView

    private var detail: MediaDetail? = null
    private var providerId: String = ""
    private var pageUrl: String? = null
    private var seasonAdapter: SeasonAdapter? = null
    private var episodeAdapter: EpisodeAdapter? = null
    private var seasonIndex = 0
    private var loadingDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_result_tv, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        providerId = requireArguments().getString(ARG_PROVIDER).orEmpty()
        pageUrl = requireArguments().getString(ARG_URL)

        background = view.findViewById(R.id.background_poster)
        loading = view.findViewById(R.id.result_loading)
        errorBox = view.findViewById(R.id.result_loading_error)
        errorText = view.findViewById(R.id.result_error_text)
        content = view.findViewById(R.id.result_finish_loading)
        titleView = view.findViewById(R.id.result_title)
        episodesText = view.findViewById(R.id.result_episodes_text)
        playMovie = view.findViewById(R.id.result_play_movie)
        playMovieButton = view.findViewById(R.id.result_play_movie_button)
        playSeries = view.findViewById(R.id.result_play_series)
        playSeriesButton = view.findViewById(R.id.result_play_series_button)
        resumeSeries = view.findViewById(R.id.result_resume_series)
        resumeSeriesButton = view.findViewById(R.id.result_resume_series_button)
        favorite = view.findViewById(R.id.result_favorite)
        favoriteButton = view.findViewById(R.id.result_favorite_Button)
        favoriteText = view.findViewById(R.id.result_favorite_Text)
        episodesShow = view.findViewById(R.id.result_episodes_show)
        episodesShowButton = view.findViewById(R.id.result_episodes_show_button)
        metaSite = view.findViewById(R.id.result_meta_site)
        metaType = view.findViewById(R.id.result_meta_type)
        metaYear = view.findViewById(R.id.result_meta_year)
        metaRating = view.findViewById(R.id.result_meta_rating)
        metaDuration = view.findViewById(R.id.result_meta_duration)
        description = view.findViewById(R.id.result_description)
        castText = view.findViewById(R.id.result_cast_text)
        tags = view.findViewById(R.id.result_tag)
        episodesShadow = view.findViewById(R.id.episodes_shadow)
        episodeHolder = view.findViewById(R.id.episode_holder_tv)
        seasonRecycler = view.findViewById(R.id.result_season_selection)
        episodeRecycler = view.findViewById(R.id.result_episodes)

        seasonRecycler.layoutManager = LinearLayoutManager(requireContext())
        episodeRecycler.layoutManager = LinearLayoutManager(requireContext())
        episodesShadow.isClickable = true
        episodesShadow.setOnClickListener { closeEpisodes() }

        view.findViewById<View>(R.id.result_reload_connectionerror).setOnClickListener { load() }
        playMovieButton.setOnClickListener { playMovie() }
        playSeriesButton.setOnClickListener { openEpisodes() }
        episodesShowButton.setOnClickListener { openEpisodes() }
        favoriteButton.setOnClickListener { toggleFavorite() }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (episodesShadow.visibility == View.VISIBLE) {
                        closeEpisodes()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

        load()
    }

    private fun load() {
        val url = pageUrl
        if (url.isNullOrBlank()) {
            showError(getString(R.string.error_generic))
            return
        }
        val provider = app.extensions.findById(providerId)
        if (provider == null) {
            showError(getString(R.string.no_providers))
            return
        }
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val loaded = app.repository.load(provider, url)
                if (!isAdded) return@launch
                bind(loaded)
            } catch (t: Throwable) {
                if (!isAdded) return@launch
                showError(t.message ?: getString(R.string.error_network))
            }
        }
    }

    private fun bind(media: MediaDetail) {
        detail = media
        loading.visible(false)
        loading.stopShimmer()
        errorBox.visible(false)
        content.visible(true)

        background.loadPoster(media.background ?: media.poster)
        titleView.text = media.title
        metaSite.text = media.providerName
        metaType.text = typeLabel(media.type)
        metaYear.text = media.year?.toString().orEmpty()
        metaYear.visible(media.year != null)
        metaRating.text = media.score?.let { String.format(Locale.ITALY, "%.1f/10", it) }.orEmpty()
        metaRating.visible(media.score != null)
        metaDuration.text = media.duration?.let { "$it min" }.orEmpty()
        metaDuration.visible(media.duration != null)
        description.text = media.plot.orEmpty()
        description.visible(!media.plot.isNullOrBlank())
        castText.text = if (media.cast.isNotEmpty()) {
            "Cast: ${media.cast.take(8).joinToString(", ")}"
        } else {
            ""
        }
        castText.visible(media.cast.isNotEmpty())

        tags.removeAllViews()
        media.genres.take(10).forEach { genre ->
            val chip = Chip(requireContext())
            chip.text = genre
            chip.isClickable = false
            chip.isFocusable = false
            chip.setChipBackgroundColorResource(R.color.colorSearch)
            chip.setTextColor(requireContext().getColor(R.color.textColor))
            tags.addView(chip)
        }

        val series = media.seasons.any { it.episodes.isNotEmpty() }
        val totalEpisodes = media.seasons.sumOf { it.episodes.size }
        if (series) {
            episodesText.text = buildString {
                if (media.seasons.size > 1) append("${media.seasons.size} stagioni · ")
                append("$totalEpisodes episodi")
            }
            episodesText.visible(true)
            playMovie.visible(false)
            playSeries.visible(true)
            episodesShow.visible(true)
            seasonAdapter = SeasonAdapter(media.seasons) { index -> selectSeason(index) }
            seasonRecycler.adapter = seasonAdapter
            seasonRecycler.visible(media.seasons.size > 1)
            selectSeason(0)
        } else {
            episodesText.visible(false)
            playMovie.visible(true)
            playSeries.visible(false)
            episodesShow.visible(false)
        }

        updateFavorite()
        updateResume()
    }

    private fun typeLabel(type: String?): String = when (type) {
        "Movie" -> getString(R.string.content_movie)
        "TvSeries" -> getString(R.string.content_tv)
        "Anime" -> getString(R.string.content_anime)
        "Cartoon" -> "Animazione"
        "Documentary" -> "Documentario"
        else -> ""
    }

    private fun selectSeason(index: Int) {
        val media = detail ?: return
        if (index !in media.seasons.indices) return
        seasonIndex = index
        seasonAdapter?.select(index)
        val episodes = media.seasons[index].episodes
        episodeAdapter = EpisodeAdapter(episodes) { episode -> playEpisode(episode) }
        episodeRecycler.adapter = episodeAdapter
    }

    private fun openEpisodes() {
        episodesShadow.visible(true)
        episodeHolder.visible(true)
        episodesShadow.post {
            episodeRecycler.requestFocus()
        }
    }

    private fun closeEpisodes() {
        episodesShadow.visible(false)
        episodeHolder.visible(false)
        episodesShowButton.requestFocus()
    }

    private fun toggleFavorite() {
        val media = detail ?: return
        val profile = app.profiles.activeProfile() ?: return
        val item = media.toMediaItem().copy(url = pageUrl)
        val added = app.profiles.toggleFavorite(profile.id, item)
        updateFavorite()
        Toast.makeText(
            requireContext(),
            if (added) R.string.add_favorite else R.string.remove_favorite,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateFavorite() {
        val media = detail ?: return
        val profile = app.profiles.activeProfile() ?: return
        val isFavorite = app.profiles.isFavorite(profile.id, media.key)
        favoriteButton.setIconResource(
            if (isFavorite) R.drawable.ic_baseline_favorite_24
            else R.drawable.ic_baseline_favorite_border_24
        )
        favoriteText.text = getString(
            if (isFavorite) R.string.remove_favorite else R.string.favorite
        )
    }

    private fun updateResume() {
        val media = detail ?: return
        val profile = app.profiles.activeProfile() ?: return
        val entry = app.profiles.history(profile.id).firstOrNull { it.key == media.key }
        if (entry != null && entry.progress in 0.01f..0.95f) {
            resumeSeries.visible(true)
            resumeSeriesButton.setOnClickListener {
                if (media.seasons.any { it.episodes.isNotEmpty() } &&
                    entry.season != null && entry.episode != null
                ) {
                    val episode = media.seasons
                        .flatMap { it.episodes }
                        .firstOrNull { it.season == entry.season && it.number == entry.episode }
                    if (episode != null) playEpisode(episode, entry.positionMs)
                } else {
                    playMovie(entry.positionMs)
                }
            }
        } else {
            resumeSeries.visible(false)
        }
    }

    private fun playMovie(resumeMs: Long = 0L) {
        val media = detail ?: return
        resolveAndPlay(
            loadData = media.loadData,
            isSeries = false,
            season = null,
            episode = null,
            episodeTitle = null,
            resumeMs = resumeMs
        )
    }

    private fun playEpisode(episode: Episode, resumeMs: Long = 0L) {
        val media = detail ?: return
        val season = episode.season ?: media.seasons.getOrNull(seasonIndex)?.number
        resolveAndPlay(
            loadData = episode.loadData,
            isSeries = true,
            season = season,
            episode = episode.number,
            episodeTitle = episode.title,
            resumeMs = resumeMs
        )
    }

    private fun resolveAndPlay(
        loadData: String,
        isSeries: Boolean,
        season: Int?,
        episode: Int?,
        episodeTitle: String?,
        resumeMs: Long
    ) {
        val media = detail ?: return
        val provider = app.extensions.findById(providerId) ?: return
        showLoadingDialog()
        viewLifecycleOwner.lifecycleScope.launch {
            val links = try {
                app.repository.loadLinks(
                    ext = provider,
                    loadData = loadData,
                    isSeries = isSeries,
                    tmdbId = tmdbId(loadData),
                    season = season,
                    episode = episode
                )
            } catch (_: Throwable) {
                emptyList()
            }
            dismissLoadingDialog()
            if (!isAdded) return@launch
            if (links.isEmpty()) {
                Toast.makeText(requireContext(), R.string.player_error, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (links.size == 1) {
                startPlayer(links.first(), loadData, season, episode, episodeTitle, resumeMs)
            } else {
                showSources(links, loadData, season, episode, episodeTitle, resumeMs)
            }
        }
    }

    private fun showSources(
        links: List<ExtractorLink>,
        loadData: String,
        season: Int?,
        episode: Int?,
        episodeTitle: String?,
        resumeMs: Long
    ) {
        val view = layoutInflater.inflate(R.layout.dialog_list, null)
        val list = view.findViewById<RecyclerView>(R.id.dialog_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pick_source)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .create()
        list.adapter = SourceAdapter(links) { source ->
            dialog.dismiss()
            startPlayer(source, loadData, season, episode, episodeTitle, resumeMs)
        }
        dialog.show()
    }

    private fun startPlayer(
        link: ExtractorLink,
        loadData: String,
        season: Int?,
        episode: Int?,
        episodeTitle: String?,
        resumeMs: Long
    ) {
        val media = detail ?: return
        val episodes = media.seasons.flatMap { it.episodes }
        val episodesJson = JSONArray()
        episodes.forEach { item ->
            episodesJson.put(
                JSONObject()
                    .put("number", item.number)
                    .put("season", item.season ?: 0)
                    .put("title", item.title ?: "")
                    .put("loadData", item.loadData)
            )
        }
        val currentIndex = episodes.indexOfFirst {
            it.number == episode && (season == null || it.season == season)
        }
        val headersJson = JSONObject()
        link.headers.forEach { (key, value) -> headersJson.put(key, value) }

        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_PROVIDER_ID, providerId)
            putExtra(PlayerActivity.EXTRA_MEDIA_ID, media.id)
            putExtra(PlayerActivity.EXTRA_TITLE, media.title)
            putExtra(PlayerActivity.EXTRA_POSTER, media.poster)
            putExtra(PlayerActivity.EXTRA_TYPE, media.type)
            putExtra(PlayerActivity.EXTRA_PAGE_URL, pageUrl)
            putExtra(PlayerActivity.EXTRA_IS_SERIES, media.seasons.any { it.episodes.isNotEmpty() })
            putExtra(PlayerActivity.EXTRA_SEASON, season ?: 0)
            putExtra(PlayerActivity.EXTRA_EPISODE, episode ?: 0)
            putExtra(PlayerActivity.EXTRA_EPISODE_TITLE, episodeTitle)
            putExtra(PlayerActivity.EXTRA_TMDB, tmdbId(media.loadData))
            putExtra(PlayerActivity.EXTRA_LINK_URL, link.url)
            putExtra(PlayerActivity.EXTRA_LINK_NAME, link.name)
            putExtra(PlayerActivity.EXTRA_LINK_TYPE, link.type)
            putExtra(PlayerActivity.EXTRA_LINK_REFERER, link.referer)
            putExtra(PlayerActivity.EXTRA_LINK_HEADERS, headersJson.toString())
            putExtra(PlayerActivity.EXTRA_EPISODES, episodesJson.toString())
            putExtra(PlayerActivity.EXTRA_EPISODE_INDEX, currentIndex)
            putExtra(PlayerActivity.EXTRA_RESUME_MS, resumeMs)
            putExtra(PlayerActivity.EXTRA_LOAD_DATA, loadData)
        }
        startActivity(intent)
    }

    private fun tmdbId(loadData: String?): String? {
        if (loadData.isNullOrBlank()) return null
        return try {
            JSONObject(loadData).optString("tmdb_id").takeIf { it.isNotBlank() && it != "null" }
        } catch (_: Throwable) {
            null
        }
    }

    private fun showLoading() {
        loading.visible(true)
        loading.startShimmer()
        errorBox.visible(false)
        content.visible(false)
    }

    private fun showError(message: String) {
        loading.visible(false)
        loading.stopShimmer()
        content.visible(false)
        errorBox.visible(true)
        errorText.text = message
    }

    private fun showLoadingDialog() {
        dismissLoadingDialog()
        val view = layoutInflater.inflate(R.layout.dialog_loading, null)
        loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    override fun onResume() {
        super.onResume()
        if (detail != null) {
            updateFavorite()
            updateResume()
        }
    }

    override fun onDestroyView() {
        dismissLoadingDialog()
        loading.stopShimmer()
        super.onDestroyView()
    }
}
