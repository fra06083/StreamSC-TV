package it.fra06083.streamsctv.ui

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import it.fra06083.streamsctv.R
import it.fra06083.streamsctv.app

val PROFILE_AVATARS = listOf(
    "profile_bg_blue",
    "profile_bg_dark_blue",
    "profile_bg_orange",
    "profile_bg_pink",
    "profile_bg_purple",
    "profile_bg_red",
    "profile_bg_teal"
)

fun ImageView.loadPoster(url: String?) {
    if (url.isNullOrBlank()) {
        setImageDrawable(null)
        return
    }
    load(url) {
        crossfade(120)
    }
}

val AVATAR_DRAWABLES = listOf(
    R.drawable.profile_bg_blue,
    R.drawable.profile_bg_dark_blue,
    R.drawable.profile_bg_orange,
    R.drawable.profile_bg_pink,
    R.drawable.profile_bg_purple,
    R.drawable.profile_bg_red,
    R.drawable.profile_bg_teal
)

fun profileAvatarRes(index: Int): Int =
    AVATAR_DRAWABLES.getOrElse(index) { R.drawable.profile_bg_blue }

fun Context.profileAvatarRes(index: Int): Int = it.fra06083.streamsctv.ui.profileAvatarRes(index)

fun View.visible(show: Boolean) {
    visibility = if (show) View.VISIBLE else View.GONE
}

fun RecyclerView.setHorizontal() {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
}

fun Fragment.activeProfileId(): String? = context?.app?.profiles?.activeProfile()?.id
