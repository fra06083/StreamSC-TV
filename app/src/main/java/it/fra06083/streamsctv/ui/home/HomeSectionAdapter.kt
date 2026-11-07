package it.fra06083.streamsctv.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.data.MediaItem
import it.fra06083.streamsctv.ui.common.CardItem
import it.fra06083.streamsctv.ui.common.MediaCardAdapter

data class HomeSection(
    val label: String,
    val cards: List<CardItem>
)

class HomeSectionAdapter(
    private var sections: List<HomeSection>,
    private val onClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<HomeSectionAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.home_child_more_info)
        val recycler: RecyclerView = view.findViewById(R.id.home_child_recyclerview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.homepage_parent_tv, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val section = sections[position]
        holder.title.text = section.label
        holder.recycler.layoutManager =
            LinearLayoutManager(holder.recycler.context, LinearLayoutManager.HORIZONTAL, false)
        holder.recycler.adapter = MediaCardAdapter(section.cards.toMutableList(), onClick = onClick)
        holder.recycler.setHasFixedSize(true)
    }

    override fun getItemCount(): Int = sections.size

    fun submit(list: List<HomeSection>) {
        sections = list
        notifyDataSetChanged()
    }
}
