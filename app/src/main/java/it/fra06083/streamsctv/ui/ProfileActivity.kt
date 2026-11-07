package it.fra06083.streamsctv.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.data.Profile

class ProfileActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_select)
        recycler = findViewById(R.id.profile_recycler)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = ProfileAdapter()
        recycler.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.reload()
    }

    private fun openMain(profile: Profile) {
        app.profiles.setActive(profile.id)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showOptions(profile: Profile) {
        val options = arrayOf(getString(R.string.edit_profile), getString(R.string.delete_profile))
        MaterialAlertDialogBuilder(this)
            .setTitle(profile.name)
            .setItems(options) { _, which ->
                if (which == 0) showEditDialog(profile) else confirmDelete(profile)
            }
            .show()
    }

    private fun confirmDelete(profile: Profile) {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.profile_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (!app.profiles.delete(profile.id)) {
                    Toast.makeText(this, R.string.profile_last_one, Toast.LENGTH_SHORT).show()
                }
                adapter.reload()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEditDialog(existing: Profile?) {
        val view = layoutInflater.inflate(R.layout.dialog_profile_edit, null)
        val nameInput = view.findViewById<EditText>(R.id.profile_name_input)
        val avatarRecycler = view.findViewById<RecyclerView>(R.id.profile_avatar_recycler)
        var selectedAvatar = existing?.avatar ?: 0
        nameInput.setText(existing?.name.orEmpty())

        val avatarAdapter = AvatarAdapter(selectedAvatar) { index ->
            selectedAvatar = index
        }
        avatarRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        avatarRecycler.adapter = avatarAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.add_profile else R.string.edit_profile)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isBlank()) {
                    nameInput.error = getString(R.string.profile_name_required)
                    return@setOnClickListener
                }
                if (existing == null) {
                    app.profiles.create(name, selectedAvatar)
                } else {
                    app.profiles.update(existing.copy(name = name, avatar = selectedAvatar))
                }
                adapter.reload()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private inner class ProfileAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items: List<Profile> = emptyList()

        init {
            reload()
        }

        fun reload() {
            items = app.profiles.profiles()
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size + 1

        override fun getItemViewType(position: Int): Int = if (position < items.size) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layout = if (viewType == 0) R.layout.account_list_item else R.layout.account_list_item_add
            val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val view = holder.itemView
            if (position >= items.size) {
                view.setOnClickListener { showEditDialog(null) }
                return
            }
            val profile = items[position]
            view.findViewById<TextView>(R.id.account_name).text = profile.name
            view.findViewById<ImageView>(R.id.account_image)
                .setImageResource(profileAvatarRes(profile.avatar))
            view.setOnClickListener { openMain(profile) }
            view.setOnLongClickListener {
                showOptions(profile)
                true
            }
        }
    }

    private class AvatarAdapter(
        private var selected: Int,
        private val onSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<AvatarAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val card: View = view.findViewById(R.id.avatar_card)
            val image: ImageView = view.findViewById(R.id.avatar_image)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile_avatar, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int = PROFILE_AVATARS.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.image.setImageResource(holder.itemView.context.profileAvatarRes(position))
            holder.card.scaleX = if (position == selected) 1.12f else 1f
            holder.card.scaleY = holder.card.scaleX
            holder.itemView.setOnClickListener {
                val previous = selected
                selected = position
                notifyItemChanged(previous)
                notifyItemChanged(position)
                onSelected(position)
            }
        }
    }
}
