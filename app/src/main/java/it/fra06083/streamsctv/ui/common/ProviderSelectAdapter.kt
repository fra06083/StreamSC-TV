package it.fra06083.streamsctv.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.ext.InstalledExtension
import it.fra06083.streamsctv.ui.loadPoster

class ProviderSelectAdapter(
    private val providers: List<InstalledExtension>,
    private val selectedId: String?,
    private val onSelect: (InstalledExtension) -> Unit
) : RecyclerView.Adapter<ProviderSelectAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.provider_name)
        val status: TextView = view.findViewById(R.id.provider_status)
        val icon: ImageView = view.findViewById(R.id.provider_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_select, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val provider = providers[position]
        holder.name.text = provider.manifest.name
        holder.status.text = if (provider.manifest.id == selectedId) {
            "● ${provider.manifest.statusLabel}"
        } else {
            provider.manifest.statusLabel
        }
        provider.iconFile?.let { holder.icon.setImageURI(android.net.Uri.fromFile(it)) }
            ?: holder.icon.setImageResource(R.drawable.app_icon)
        holder.itemView.setOnClickListener { onSelect(provider) }
    }

    override fun getItemCount(): Int = providers.size
}
