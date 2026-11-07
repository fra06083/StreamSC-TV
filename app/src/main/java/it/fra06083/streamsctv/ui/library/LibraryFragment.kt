package it.fra06083.streamsctv.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.ui.MainActivity
import it.fra06083.streamsctv.ui.common.CardItem
import it.fra06083.streamsctv.ui.common.MediaCardAdapter
import it.fra06083.streamsctv.ui.visible

class LibraryFragment : Fragment() {

    private lateinit var continueHeader: TextView
    private lateinit var continueRecycler: RecyclerView
    private lateinit var favoritesHeader: TextView
    private lateinit var favoritesRecycler: RecyclerView
    private lateinit var empty: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_library_tv, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        continueHeader = view.findViewById(R.id.library_continue_header)
        continueRecycler = view.findViewById(R.id.library_continue_recycler)
        favoritesHeader = view.findViewById(R.id.library_favorites_header)
        favoritesRecycler = view.findViewById(R.id.library_favorites_recycler)
        empty = view.findViewById(R.id.library_empty_text)

        continueRecycler.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        favoritesRecycler.layoutManager = GridLayoutManager(requireContext(), 7)
        reload()
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        if (!isAdded) return
        val profile = app.profiles.activeProfile() ?: return
        val history = app.profiles.history(profile.id)
        val favorites = app.profiles.favorites(profile.id)

        val continueCards = history.map { entry ->
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
        val favoriteCards = favorites.map { favorite ->
            CardItem(favorite.toMediaItem(app.repository.providerName(favorite.providerId)))
        }

        continueRecycler.adapter = MediaCardAdapter(continueCards) { item ->
            (activity as? MainActivity)?.openDetail(item.providerId, item.url ?: item.id)
        }
        favoritesRecycler.adapter = MediaCardAdapter(favoriteCards) { item ->
            (activity as? MainActivity)?.openDetail(item.providerId, item.url ?: item.id)
        }

        continueHeader.visible(continueCards.isNotEmpty())
        continueRecycler.visible(continueCards.isNotEmpty())
        favoritesHeader.visible(favoriteCards.isNotEmpty())
        favoritesRecycler.visible(favoriteCards.isNotEmpty())
        empty.visible(continueCards.isEmpty() && favoriteCards.isEmpty())
    }
}
