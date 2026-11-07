package it.fra06083.streamsctv.ui.player

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.data.ExtractorLink
import it.fra06083.streamsctv.data.HistoryEntry
import it.fra06083.streamsctv.net.Http
import it.fra06083.streamsctv.ui.detail.SourceAdapter
import it.fra06083.streamsctv.ui.visible
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PROVIDER_ID = "provider_id"
        const val EXTRA_MEDIA_ID = "media_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_POSTER = "poster"
        const val EXTRA_TYPE = "type"
        const val EXTRA_PAGE_URL = "page_url"
        const val EXTRA_IS_SERIES = "is_series"
        const val EXTRA_SEASON = "season"
        const val EXTRA_EPISODE = "episode"
        const val EXTRA_EPISODE_TITLE = "episode_title"
        const val EXTRA_TMDB = "tmdb"
        const val EXTRA_LOAD_DATA = "load_data"
        const val EXTRA_LINK_URL = "link_url"
        const val EXTRA_LINK_NAME = "link_name"
        const val EXTRA_LINK_TYPE = "link_type"
        const val EXTRA_LINK_REFERER = "link_referer"
        const val EXTRA_LINK_HEADERS = "link_headers"
        const val EXTRA_EPISODES = "episodes"
        const val EXTRA_EPISODE_INDEX = "episode_index"
        const val EXTRA_RESUME_MS = "resume_ms"
    }

    private data class PlayerEpisode(
        val number: Int,
        val season: Int,
        val title: String,
        val loadData: String
    )

    private lateinit var playerView: PlayerView
    private lateinit var loadingOverlay: View
    private lateinit var loadingText: TextView
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var playPause: MaterialButton
    private lateinit var rewind: MaterialButton
    private lateinit var forward: MaterialButton
    private lateinit var positionView: TextView
    private lateinit var durationView: TextView
    private lateinit var timeBar: DefaultTimeBar
    private lateinit var sourcesButton: MaterialButton
    private lateinit var nextButton: MaterialButton

    private var player: ExoPlayer? = null
    private var controlsVisible = true
    private val handler = Handler(Looper.getMainLooper())
    private var pendingResumeMs = 0L
    private var lastSavedAt = 0L
    private var sourcesDialog: AlertDialog? = null

    private var providerId = ""
    private var mediaId = ""
    private var mediaTitle = ""
    private var poster: String? = null
    private var type: String? = null
    private var pageUrl: String? = null
    private var isSeries = false
    private var season = 0
    private var episode = 0
    private var episodeTitle: String? = null
    private var tmdbId: String? = null
    private var currentLoadData: String? = null
    private var episodes: List<PlayerEpisode> = emptyList()
    private var episodeIndex = -1
    private var currentLinks: List<ExtractorLink> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_tv)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        readIntent(intent)
        bindViews()
        setupControls()
        setupPlayer()
        updateEpisodeUi()
        playLink(
            ExtractorLink(
                name = intent.getStringExtra(EXTRA_LINK_NAME) ?: mediaTitle,
                url = intent.getStringExtra(EXTRA_LINK_URL).orEmpty(),
                type = intent.getStringExtra(EXTRA_LINK_TYPE),
                referer = intent.getStringExtra(EXTRA_LINK_REFERER),
                headers = parseHeaders(intent.getStringExtra(EXTRA_LINK_HEADERS))
            ),
            pendingResumeMs
        )
    }

    private fun readIntent(intent: Intent) {
        providerId = intent.getStringExtra(EXTRA_PROVIDER_ID).orEmpty()
        mediaId = intent.getStringExtra(EXTRA_MEDIA_ID).orEmpty()
        mediaTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        poster = intent.getStringExtra(EXTRA_POSTER)
        type = intent.getStringExtra(EXTRA_TYPE)
        pageUrl = intent.getStringExtra(EXTRA_PAGE_URL)
        isSeries = intent.getBooleanExtra(EXTRA_IS_SERIES, false)
        season = intent.getIntExtra(EXTRA_SEASON, 0)
        episode = intent.getIntExtra(EXTRA_EPISODE, 0)
        episodeTitle = intent.getStringExtra(EXTRA_EPISODE_TITLE)
        tmdbId = intent.getStringExtra(EXTRA_TMDB)
        currentLoadData = intent.getStringExtra(EXTRA_LOAD_DATA)
        pendingResumeMs = intent.getLongExtra(EXTRA_RESUME_MS, 0L)
        episodeIndex = intent.getIntExtra(EXTRA_EPISODE_INDEX, -1)
        episodes = parseEpisodes(intent.getStringExtra(EXTRA_EPISODES))
        if (currentLoadData == null && episodeIndex in episodes.indices) {
            currentLoadData = episodes[episodeIndex].loadData
        }
    }

    private fun bindViews() {
        playerView = findViewById(R.id.player_view)
        loadingOverlay = findViewById(R.id.player_loading_overlay)
        loadingText = findViewById(R.id.player_loading_text)
        topBar = findViewById(R.id.player_top_bar)
        bottomBar = findViewById(R.id.player_bottom_bar)
        titleView = findViewById(R.id.player_title)
        subtitleView = findViewById(R.id.player_subtitle)
        playPause = findViewById(R.id.player_play_pause)
        rewind = findViewById(R.id.player_rewind)
        forward = findViewById(R.id.player_forward)
        positionView = findViewById(R.id.player_position)
        durationView = findViewById(R.id.player_duration)
        timeBar = findViewById(R.id.player_progress)
        sourcesButton = findViewById(R.id.player_sources)
        nextButton = findViewById(R.id.player_next)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupControls() {
        titleView.text = mediaTitle
        playPause.setOnClickListener { togglePlayPause() }
        rewind.setOnClickListener { seekBy(-10_000) }
        forward.setOnClickListener { seekBy(10_000) }
        sourcesButton.setOnClickListener { showSources() }
        nextButton.setOnClickListener { playNext() }
        playerView.setOnClickListener { if (controlsVisible) hideControls() else showControls() }

        timeBar.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                handler.removeCallbacks(hideControlsRunnable)
            }

            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                player?.seekTo(position)
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                scheduleHideControls()
            }
        })
        showControls()
    }

    private fun setupPlayer() {
        val player = ExoPlayer.Builder(this).build()
        this.player = player
        playerView.player = player
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    loadingOverlay.visible(false)
                    if (pendingResumeMs > 0) {
                        player.seekTo(pendingResumeMs)
                        pendingResumeMs = 0
                    }
                    updateProgress()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon()
            }

            override fun onPlayerError(error: PlaybackException) {
                loadingOverlay.visible(false)
                showControls()
                Toast.makeText(this@PlayerActivity, R.string.player_error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun playLink(link: ExtractorLink, startPositionMs: Long) {
        val player = player ?: return
        currentLinks = if (currentLinks.any { it.url == link.url }) {
            currentLinks
        } else {
            currentLinks + link
        }
        loadingOverlay.visible(true)
        loadingText.text = link.name
        val headers = buildMap {
            put("User-Agent", Http.DEFAULT_UA)
            putAll(link.headers)
            link.referer?.takeIf { it.isNotBlank() }?.let { put("Referer", it) }
        }
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)

        val url = link.url
        val isHls = link.type?.contains("m3u8", true) == true ||
            link.type?.contains("hls", true) == true ||
            url.contains(".m3u8", true)
        val mediaItem = MediaItem.fromUri(url)
        val mediaSource = if (isHls) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
        player.setMediaSource(mediaSource, startPositionMs.coerceAtLeast(0))
        player.prepare()
        player.playWhenReady = true
        updatePlayPauseIcon()
        scheduleHideControls()
    }

    private fun togglePlayPause() {
        val player = player ?: return
        if (player.isPlaying) player.pause() else player.play()
        updatePlayPauseIcon()
        scheduleHideControls()
    }

    private fun updatePlayPauseIcon() {
        val playing = player?.isPlaying == true
        playPause.setIconResource(
            if (playing) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
        )
    }

    private fun seekBy(deltaMs: Long) {
        val player = player ?: return
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0)
        player.seekTo(target)
        updateProgress()
        showControls()
    }

    private fun updateProgress() {
        val player = player ?: return
        val duration = player.duration
        val position = player.currentPosition
        positionView.text = formatTime(position)
        durationView.text = if (duration > 0) formatTime(duration) else "--:--"
        if (duration > 0) {
            timeBar.setDuration(duration)
            timeBar.setPosition(position)
            timeBar.setBufferedPosition(player.bufferedPosition)
        }
        if (System.currentTimeMillis() - lastSavedAt > 10_000) {
            lastSavedAt = System.currentTimeMillis()
            saveHistory()
        }
    }

    private val progressTicker = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }

    private val hideControlsRunnable = Runnable {
        if (sourcesDialog?.isShowing == true) return@Runnable
        hideControls()
    }

    private fun showControls() {
        controlsVisible = true
        topBar.visible(true)
        bottomBar.visible(true)
        if (window.currentFocus == null || window.currentFocus === playerView) {
            playPause.requestFocus()
        }
        scheduleHideControls()
    }

    private fun hideControls() {
        controlsVisible = false
        topBar.visible(false)
        bottomBar.visible(false)
        playerView.requestFocus()
    }

    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 5000)
    }

    private fun updateEpisodeUi() {
        subtitleView.text = if (isSeries && episode > 0) {
            buildString {
                if (season > 0) append("S$season ")
                append("E$episode")
                episodeTitle?.takeIf { it.isNotBlank() }?.let { append(" · $it") }
            }
        } else {
            app.repository.providerName(providerId)
        }
        nextButton.visible(hasNext())
    }

    private fun hasNext(): Boolean = episodeIndex >= 0 && episodeIndex + 1 < episodes.size

    private fun playNext() {
        if (!hasNext()) return
        val nextIndex = episodeIndex + 1
        val next = episodes[nextIndex]
        val provider = app.extensions.findById(providerId) ?: return
        loadingOverlay.visible(true)
        loadingText.text = getString(R.string.loading)
        viewLifecycleScope.launch {
            val links = try {
                app.repository.loadLinks(
                    ext = provider,
                    loadData = next.loadData,
                    isSeries = true,
                    tmdbId = tmdbId,
                    season = next.season,
                    episode = next.number
                )
            } catch (_: Throwable) {
                emptyList()
            }
            if (links.isEmpty()) {
                loadingOverlay.visible(false)
                Toast.makeText(this@PlayerActivity, R.string.player_error, Toast.LENGTH_SHORT).show()
                return@launch
            }
            episodeIndex = nextIndex
            season = next.season
            episode = next.number
            episodeTitle = next.title
            currentLoadData = next.loadData
            currentLinks = links
            updateEpisodeUi()
            saveHistory()
            playLink(links.first(), 0)
        }
    }

    private val viewLifecycleScope get() = lifecycleScope

    private fun showSources() {
        val links = currentLinks
        if (links.isEmpty()) return
        val view = layoutInflater.inflate(R.layout.dialog_list, null)
        val list = view.findViewById<RecyclerView>(R.id.dialog_list)
        list.layoutManager = LinearLayoutManager(this)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pick_source)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .create()
        list.adapter = SourceAdapter(links) { source ->
            dialog.dismiss()
            playLink(source, player?.currentPosition ?: 0L)
        }
        dialog.setOnDismissListener {
            sourcesDialog = null
            scheduleHideControls()
        }
        sourcesDialog = dialog
        dialog.show()
    }

    private fun saveHistory() {
        val player = player ?: return
        val position = player.currentPosition
        if (position < 5_000) return
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val profile = app.profiles.activeProfile() ?: return
        app.profiles.upsertHistory(
            profile.id,
            HistoryEntry(
                providerId = providerId,
                mediaId = mediaId.ifBlank { mediaTitle },
                title = mediaTitle,
                poster = poster,
                type = type,
                url = pageUrl.orEmpty(),
                season = if (isSeries && season > 0) season else null,
                episode = if (isSeries && episode > 0) episode else null,
                episodeTitle = episodeTitle,
                positionMs = position,
                durationMs = duration,
                loadData = currentLoadData,
                tmdbId = tmdbId
            )
        )
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.ITALY, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ITALY, "%02d:%02d", minutes, seconds)
        }
    }

    private fun parseEpisodes(json: String?): List<PlayerEpisode> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                PlayerEpisode(
                    number = obj.optInt("number"),
                    season = obj.optInt("season"),
                    title = obj.optString("title"),
                    loadData = obj.optString("loadData")
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun parseHeaders(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { key -> map[key] = obj.optString(key) }
            map
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> if (!controlsVisible) {
                    seekBy(-10_000)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (!controlsVisible) {
                    seekBy(10_000)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> if (!controlsVisible) {
                    showControls()
                    return true
                }
            }
            if (controlsVisible) scheduleHideControls()
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        handler.post(progressTicker)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        saveHistory()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(progressTicker)
        handler.removeCallbacks(hideControlsRunnable)
    }

    override fun onDestroy() {
        saveHistory()
        sourcesDialog?.dismiss()
        handler.removeCallbacksAndMessages(null)
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
