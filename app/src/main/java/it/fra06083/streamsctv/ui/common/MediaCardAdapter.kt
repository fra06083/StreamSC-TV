package it.fra06083.streamsctv.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.data.MediaItem
import it.fra06083.streamsctv.ui.loadPoster

data class CardItem(
    val item: MediaItem,
    val progress: Float = 0f,
    val episodeLabel: String? = null
)

class MediaCardAdapter(
    private var items: List<CardItem>,
    private val layoutRes: Int = R.layout.home_result_grid,
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<MediaCardAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view
        val image: ImageView = view.findViewById(R.id.imageView)
        val title: TextView = view.findViewById(R.id.imageText)
        val quality: TextView? = view.findViewById(R.id.text_quality)
        val rating: TextView? = view.findViewById(R.id.text_rating)
        val dub: TextView? = view.findViewById(R.id.text_is_dub)
        val sub: TextView? = view.findViewById(R.id.text_is_sub)
        val episode: TextView? = view.findViewById(R.id.episode_text)
        val progressContainer: View? = view.findViewById(R.id.watch_progress_container)
        val circularProgress: CircularProgressIndicator? = view.findViewById(R.id.watchProgress)
        val linearProgress: android.widget.ProgressBar? = view.findViewById(R.id.watchProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val model = items[position]
        val item = model.item
        holder.title.text = item.title
        holder.image.loadPoster(item.poster)
        holder.quality?.apply {
            text = item.quality ?: "HD"
            isVisible = !item.quality.isNullOrBlank()
        }
        holder.rating?.isVisible = false
        holder.dub?.isVisible = false
        holder.sub?.isVisible = false
        holder.episode?.apply {
            text = model.episodeLabel.orEmpty()
            isVisible = !model.episodeLabel.isNullOrBlank()
        }
        if (model.progress > 0f) {
            holder.progressContainer?.isVisible = true
            holder.circularProgress?.apply {
                isVisible = true
                setProgressCompat((model.progress * 100).toInt(), true)
            }
            holder.linearProgress?.apply {
                isVisible = holder.circularProgress == null
                progress = (model.progress * 100).toInt()
            }
        } else {
            holder.progressContainer?.isVisible = false
            holder.circularProgress?.isVisible = false
            holder.linearProgress?.isVisible = false
        }
        holder.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<CardItem>) {
        items = list
        notifyDataSetChanged()
    }
}
