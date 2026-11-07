package it.fra06083.streamsctv.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.data.MediaItem
import it.fra06083.streamsctv.ui.MainActivity
import it.fra06083.streamsctv.ui.common.CardItem
import it.fra06083.streamsctv.ui.common.MediaCardAdapter
import it.fra06083.streamsctv.ui.visible
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class SearchFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var recycler: RecyclerView
    private lateinit var status: TextView
    private lateinit var loading: ProgressBar
    private lateinit var empty: TextView
    private lateinit var adapter: MediaCardAdapter
    private var searchJob: Job? = null
    private var allResults: List<MediaItem> = emptyList()
    private var filter = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search_tv, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchView = view.findViewById(R.id.main_search)
        recycler = view.findViewById(R.id.search_autofit_results)
        status = view.findViewById(R.id.search_status)
        loading = view.findViewById(R.id.search_loading)
        empty = view.findViewById(R.id.search_empty)

        adapter = MediaCardAdapter(mutableListOf(), R.layout.search_result_grid) { item ->
            (activity as? MainActivity)?.openDetail(item.providerId, item.url ?: item.id)
        }
        recycler.layoutManager = GridLayoutManager(requireContext(), 7)
        recycler.adapter = adapter

        view.findViewById<Chip>(R.id.search_type_all).setOnCheckedChangeListener { _, checked ->
            if (checked) { filter = 0; applyFilter() }
        }
        view.findViewById<Chip>(R.id.search_type_movies).setOnCheckedChangeListener { _, checked ->
            if (checked) { filter = 1; applyFilter() }
        }
        view.findViewById<Chip>(R.id.search_type_tv).setOnCheckedChangeListener { _, checked ->
            if (checked) { filter = 2; applyFilter() }
        }
        view.findViewById<Chip>(R.id.search_type_anime).setOnCheckedChangeListener { _, checked ->
            if (checked) { filter = 3; applyFilter() }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    doSearch(query.orEmpty())
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                scheduleSearch(newText.orEmpty())
                return true
            }
        })
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            allResults = emptyList()
            adapter.submit(emptyList())
            recycler.visible(false)
            empty.visible(false)
            loading.visible(false)
            status.visible(false)
            return
        }
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            doSearch(query)
        }
    }

    private suspend fun doSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val providers = app.extensions.enabledExtensions().filter { it.manifest.hasSearch }
        if (providers.isEmpty()) {
            status.visible(true)
            status.text = getString(R.string.no_providers)
            return
        }
        status.visible(true)
        status.text = getString(R.string.search_all) + " (${providers.size})"
        loading.visible(true)
        empty.visible(false)
        recycler.visible(false)

        val results = supervisorScope {
            providers.map { provider ->
                async {
                    try {
                        app.repository.search(provider, trimmed, 1)
                    } catch (_: Throwable) {
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
        if (!isAdded) return
        val seen = HashSet<String>()
        allResults = results.filter { seen.add(it.key) }
        loading.visible(false)
        status.visible(true)
        status.text = if (allResults.isNotEmpty()) {
            "${allResults.size} risultati"
        } else {
            getString(R.string.no_results)
        }
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (filter) {
            1 -> allResults.filter { it.type == "Movie" }
            2 -> allResults.filter { it.type == "TvSeries" }
            3 -> allResults.filter { it.type == "Anime" || it.type == "Cartoon" }
            else -> allResults
        }
        adapter.submit(filtered.map { CardItem(it) })
        recycler.visible(filtered.isNotEmpty())
        empty.visible(filtered.isEmpty() && allResults.isNotEmpty())
    }

    override fun onDestroyView() {
        searchJob?.cancel()
        super.onDestroyView()
    }
}
