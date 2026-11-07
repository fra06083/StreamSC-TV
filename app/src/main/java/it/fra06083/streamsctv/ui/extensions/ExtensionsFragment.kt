package it.fra06083.streamsctv.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.ext.InstalledExtension
import it.fra06083.streamsctv.ui.visible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtensionsFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var empty: TextView
    private lateinit var loading: ProgressBar
    private lateinit var adapter: ExtensionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_extensions_tv, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recycler = view.findViewById(R.id.extensions_recycler)
        empty = view.findViewById(R.id.extensions_empty)
        loading = view.findViewById(R.id.extensions_loading)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = ExtensionAdapter(
            items = emptyList(),
            isEnabled = { app.providerPrefs.isEnabled(it) },
            onToggle = { ext, enabled ->
                app.extensions.setEnabled(ext.manifest.id, enabled)
            },
            onDelete = { ext -> confirmDelete(ext) }
        )
        recycler.adapter = adapter

        view.findViewById<View>(R.id.extensions_sync_button).setOnClickListener { sync() }
        view.findViewById<View>(R.id.extensions_url_button).setOnClickListener { installFromUrl() }
        reload()
    }

    private fun reload() {
        val list = app.extensions.listInstalled()
        adapter.submit(list)
        empty.visible(list.isEmpty())
        recycler.visible(list.isNotEmpty())
    }

    private fun confirmDelete(ext: InstalledExtension) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(ext.manifest.name)
            .setMessage(R.string.extension_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                app.extensions.delete(ext.manifest.id)
                reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sync() {
        if (!isAdded) return
        loading.visible(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Result.success(app.extensions.syncFromGithub())
                } catch (t: Throwable) {
                    Result.failure(t)
                }
            }
            loading.visible(false)
            if (!isAdded) return@launch
            result.onSuccess { count ->
                Toast.makeText(
                    requireContext(),
                    if (count > 0) "${getString(R.string.extensions_updated)} ($count)" else getString(R.string.extensions_updated),
                    Toast.LENGTH_SHORT
                ).show()
                reload()
            }.onFailure {
                Toast.makeText(requireContext(), R.string.extensions_install_fail, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun installFromUrl() {
        val view = layoutInflater.inflate(R.layout.dialog_text_input, null)
        val input = view.findViewById<EditText>(R.id.extension_url_input)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.extensions_install_url)
            .setView(view)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val url = input.text.toString().trim()
                if (url.isBlank()) return@setOnClickListener
                dialog.dismiss()
                loading.visible(true)
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        try {
                            Result.success(app.extensions.installFromUrl(url))
                        } catch (t: Throwable) {
                            Result.failure(t)
                        }
                    }
                    loading.visible(false)
                    if (!isAdded) return@launch
                    result.onSuccess {
                        Toast.makeText(requireContext(), R.string.extensions_install_ok, Toast.LENGTH_SHORT).show()
                        reload()
                    }.onFailure {
                        Toast.makeText(requireContext(), R.string.extensions_install_fail, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show()
    }
}
