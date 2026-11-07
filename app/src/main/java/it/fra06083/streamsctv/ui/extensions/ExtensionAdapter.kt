package it.fra06083.streamsctv.ui.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.ext.InstalledExtension
import it.fra06083.streamsctv.ui.visible

class ExtensionAdapter(
    private var items: List<InstalledExtension>,
    private val isEnabled: (String) -> Boolean,
    private val onToggle: (InstalledExtension, Boolean) -> Unit,
    private val onDelete: (InstalledExtension) -> Unit
) : RecyclerView.Adapter<ExtensionAdapter.Holder>() {

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.extension_icon)
        val name: TextView = view.findViewById(R.id.extension_name)
        val description: TextView = view.findViewById(R.id.extension_description)
        val version: TextView = view.findViewById(R.id.extension_version)
        val status: TextView = view.findViewById(R.id.extension_status)
        val language: TextView = view.findViewById(R.id.extension_language)
        val enabled: MaterialSwitch = view.findViewById(R.id.extension_enabled)
        val delete: View = view.findViewById(R.id.extension_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.extension_item_tv, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val ext = items[position]
        val manifest = ext.manifest
        holder.name.text = manifest.name
        holder.description.text = manifest.description
        holder.description.visible(manifest.description.isNotBlank())
        holder.version.text = "v${manifest.version}"
        holder.status.text = manifest.statusLabel
        holder.language.text = manifest.language
        val icon = ext.iconFile
        if (icon != null) {
            holder.icon.setImageURI(android.net.Uri.fromFile(icon))
        } else {
            holder.icon.setImageResource(R.drawable.app_icon)
        }

        holder.enabled.setOnCheckedChangeListener(null)
        holder.enabled.isChecked = isEnabled(manifest.id)
        holder.enabled.setOnCheckedChangeListener { _, checked ->
            onToggle(ext, checked)
        }
        holder.itemView.setOnClickListener {
            holder.enabled.isChecked = !holder.enabled.isChecked
        }
        holder.delete.setOnClickListener { onDelete(ext) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<InstalledExtension>) {
        items = list
        notifyDataSetChanged()
    }
}
