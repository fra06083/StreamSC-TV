package it.fra06083.streamsctv.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.data.Episode
import it.fra06083.streamsctv.data.Season

class SeasonAdapter(
    private val seasons: List<Season>,
    private val onSelect: (Int) -> Unit
) : RecyclerView.Adapter<SeasonAdapter.Holder>() {

    private var selected = 0

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val button: MaterialButton = view as MaterialButton
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.result_selection, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val season = seasons[position]
        holder.button.text = "Stagione ${season.number}"
        holder.button.alpha = if (position == selected) 1f else 0.6f
        holder.button.setOnClickListener {
            val previous = selected
            selected = position
            notifyItemChanged(previous)
            notifyItemChanged(position)
            onSelect(position)
        }
    }

    override fun getItemCount(): Int = seasons.size

    fun select(position: Int) {
        if (position !in seasons.indices) return
        val previous = selected
        selected = position
        notifyItemChanged(previous)
        notifyItemChanged(position)
    }
}

class EpisodeAdapter(
    private var episodes: List<Episode>,
    private val onClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.episode_text)
        val progress: View? = view.findViewById(R.id.episode_progress)
        val progressContainer: View? = view.findViewById(R.id.watch_progress_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.result_episode, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val episode = episodes[position]
        val title = episode.title?.takeIf { it.isNotBlank() }
        holder.text.text = if (title != null) {
            "Episodio ${episode.number} · $title"
        } else {
            "Episodio ${episode.number}"
        }
        holder.progressContainer?.visibility = View.GONE
        holder.progress?.visibility = View.GONE
        holder.itemView.setOnClickListener { onClick(episode) }
    }

    override fun getItemCount(): Int = episodes.size

    fun submit(list: List<Episode>) {
        episodes = list
        notifyDataSetChanged()
    }
}
