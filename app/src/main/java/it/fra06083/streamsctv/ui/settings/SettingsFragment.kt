package it.fra06083.streamsctv.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.ui.ProfileActivity
import it.fra06083.streamsctv.ui.profileAvatarRes

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings_tv, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val profileName = view.findViewById<TextView>(R.id.settings_profile_name)
        val profileImage = view.findViewById<ImageView>(R.id.settings_profile_image)
        val hls = view.findViewById<MaterialSwitch>(R.id.settings_prefer_hls)
        val autoplay = view.findViewById<MaterialSwitch>(R.id.settings_autoplay_next)
        val version = view.findViewById<TextView>(R.id.settings_version)

        val profile = app.profiles.activeProfile()
        profileName.text = profile?.name.orEmpty()
        if (profile != null) {
            profileImage.setImageResource(profileAvatarRes(profile.avatar))
        }

        hls.isChecked = app.providerPrefs.preferHls()
        hls.setOnCheckedChangeListener { _, checked -> app.providerPrefs.setPreferHls(checked) }
        autoplay.isChecked = app.providerPrefs.autoplayNext()
        autoplay.setOnCheckedChangeListener { _, checked -> app.providerPrefs.setAutoplayNext(checked) }

        view.findViewById<View>(R.id.settings_profile_row).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
            requireActivity().finish()
        }

        view.findViewById<View>(R.id.settings_clear_history).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.settings_clear_history_confirm)
                .setPositiveButton(R.string.delete) { _, _ ->
                    app.profiles.activeProfile()?.let { app.profiles.clearHistory(it.id) }
                    Toast.makeText(requireContext(), R.string.settings_cache_cleared, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        val versionName = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (_: Throwable) {
            "1.0.0"
        }
        version.text = "${getString(R.string.settings_version)} $versionName"
    }
}
