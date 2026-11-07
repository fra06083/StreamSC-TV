package it.fra06083.streamsctv.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.ext.InstalledExtension
import it.fra06083.streamsctv.ext.ManifestSection
import it.fra06083.streamsctv.ui.MainActivity
import it.fra06083.streamsctv.ui.common.CardItem
import it.fra06083.streamsctv.ui.common.ProviderSelectAdapter
import it.fra06083.streamsctv.ui.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class HomeFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var loading: View
    private lateinit var errorBox: View
    private lateinit var errorText: TextView
    private lateinit var emptyText: TextView
    private lateinit var providerButton: MaterialButton
    private lateinit var adapter: HomeSectionAdapter
    private var loadJob: Job? = null
    private var providerId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home_tv, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.home_master_recycler)
        shimmer = view.findViewById(R.id.home_loading_shimmer)
        loading = view.findViewById(R.id.home_loading)
        errorBox = view.findViewById(R.id.home_loading_error)
        errorText = view.findViewById(R.id.home_error_text)
        emptyText = view.findViewById(R.id.home_empty_text)
        providerButton = view.findViewById(R.id.home_change_api)

        adapter = HomeSectionAdapter(emptyList()) { item ->
            (activity as? MainActivity)?.openDetail(item.providerId, item.url ?: item.id)
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        providerButton.setOnClickListener { showProviderDialog() }
        view.findViewById<View>(R.id.home_preview_reload_provider).setOnClickListener { load() }
        view.findViewById<View>(R.id.home_preview_search_button).setOnClickListener {
            (activity as? MainActivity)?.openSearch()
        }
        view.findViewById<View>(R.id.home_reload_connectionerror).setOnClickListener { load() }
        load()
    }

    private fun showProviderDialog() {
        val enabled = app.extensions.enabledExtensions()
        if (enabled.isEmpty()) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_list, null)
        val list = dialogView.findViewById<RecyclerView>(R.id.dialog_list)
        list.layoutManager = LinearLayoutManager(requireContext())
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_provider)
            .setView(dialogView)
            .setNegativeButton(R.string.cancel, null)
            .create()
        list.adapter = ProviderSelectAdapter(enabled, providerId) { selected ->
            providerId = selected.manifest.id
            app.providerPrefs.setHomeProviderId(selected.manifest.id)
            providerButton.text = selected.manifest.name
            dialog.dismiss()
            load()
        }
        dialog.show()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val enabled = app.extensions.enabledExtensions()
            if (enabled.isEmpty()) {
                showError(getString(R.string.no_providers))
                return@launch
            }
            val provider = enabled.firstOrNull {
                it.manifest.id == app.providerPrefs.homeProviderId()
            } ?: enabled.first()
            providerId = provider.manifest.id
            providerButton.text = provider.manifest.name
            showLoading()

            val sections = provider.manifest.sections.ifEmpty {
                listOf(ManifestSection("", provider.manifest.name))
            }
            val loaded = supervisorScope {
                sections.map { section ->
                    async {
                        try {
                            val result = app.repository.home(provider, section.key, 1)
                            HomeSection(
                                label = section.label.ifBlank { provider.manifest.name },
                                cards = result.items.map { CardItem(it) }
                            )
                        } catch (_: Throwable) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            val rows = mutableListOf<HomeSection>()
            val profile = app.profiles.activeProfile()
            if (profile != null) {
                val history = app.profiles.history(profile.id).take(20)
                if (history.isNotEmpty()) {
                    rows.add(
                        HomeSection(
                            label = getString(R.string.continue_watching),
                            cards = history.map { entry ->
                                CardItem(
                                    item = entry.toMediaItem(app.repository.providerName(entry.providerId)),
                                    progress = entry.progress,
                                    episodeLabel = entry.episode?.let { episode ->
                                        buildString {
                                            entry.season?.let { append("S$it ") }
                                            append("E$episode")
                                        }
                                    }
                                )
                            }
                        )
                    )
                }
            }
            rows.addAll(loaded.filter { it.cards.isNotEmpty() })

            if (rows.isEmpty()) {
                showError(getString(R.string.error_network))
            } else {
                adapter.submit(rows)
                loading.visible(false)
                errorBox.visible(false)
                emptyText.visible(false)
                recycler.visible(true)
                recycler.scrollToPosition(0)
            }
        }
    }

    private fun showLoading() {
        loading.visible(true)
        shimmer.startShimmer()
        errorBox.visible(false)
        emptyText.visible(false)
        recycler.visible(false)
    }

    private fun showError(message: String) {
        shimmer.stopShimmer()
        loading.visible(false)
        recycler.visible(false)
        emptyText.visible(false)
        errorBox.visible(true)
        errorText.text = message
    }

    override fun onDestroyView() {
        loadJob?.cancel()
        shimmer.stopShimmer()
        super.onDestroyView()
    }
}
