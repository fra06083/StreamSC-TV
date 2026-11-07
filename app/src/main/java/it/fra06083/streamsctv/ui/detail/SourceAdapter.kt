package it.fra06083.streamsctv.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.data.ExtractorLink

class SourceAdapter(
    private val sources: List<ExtractorLink>,
    private val onSelect: (ExtractorLink) -> Unit
) : RecyclerView.Adapter<SourceAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.source_name)
        val quality: TextView = view.findViewById(R.id.source_quality)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_source, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val source = sources[position]
        holder.name.text = source.name
        val meta = buildList {
            source.quality?.let { add("${it}p") }
            source.type?.let { add(it) }
        }.joinToString(" · ")
        holder.quality.text = meta
        holder.quality.visibility = if (meta.isBlank()) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener { onSelect(source) }
    }

    override fun getItemCount(): Int = sources.size
}
