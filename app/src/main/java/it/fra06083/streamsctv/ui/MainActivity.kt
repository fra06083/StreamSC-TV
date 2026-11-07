package it.fra06083.streamsctv.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.navigationrail.NavigationRailView
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app
import it.fra06083.streamsctv.ui.detail.DetailFragment
import it.fra06083.streamsctv.ui.extensions.ExtensionsFragment
import it.fra06083.streamsctv.ui.home.HomeFragment
import it.fra06083.streamsctv.ui.library.LibraryFragment
import it.fra06083.streamsctv.ui.search.SearchFragment
import it.fra06083.streamsctv.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var rail: NavigationRailView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_tv)

        val container = findViewById<View>(R.id.fragment_container)
        (container.layoutParams as ViewGroup.MarginLayoutParams).marginStart =
            resources.getDimensionPixelSize(R.dimen.nav_rail_view_width)

        rail = findViewById(R.id.nav_rail_view)
        rail.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> showTab(HomeFragment(), "home")
                R.id.navigation_search -> showTab(SearchFragment(), "search")
                R.id.navigation_library -> showTab(LibraryFragment(), "library")
                R.id.navigation_downloads -> showTab(ExtensionsFragment(), "extensions")
                R.id.navigation_settings -> showTab(SettingsFragment(), "settings")
                else -> return@setOnItemSelectedListener false
            }
            true
        }
        rail.selectedItemId = R.id.navigation_home
        rail.findViewById<View>(R.id.nav_footer_profile_card)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        updateProfileImage()
    }

    override fun onResume() {
        super.onResume()
        updateProfileImage()
    }

    private fun updateProfileImage() {
        val profile = app.profiles.activeProfile() ?: return
        rail.findViewById<ImageView>(R.id.nav_footer_profile_pic)
            ?.setImageResource(profileAvatarRes(profile.avatar))
    }

    private fun showTab(fragment: Fragment, tag: String) {
        supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }

    fun openDetail(providerId: String, url: String?) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, DetailFragment.newInstance(providerId, url))
            .addToBackStack("detail")
            .commit()
    }

    fun openSearch() {
        rail.selectedItemId = R.id.navigation_search
    }
}
