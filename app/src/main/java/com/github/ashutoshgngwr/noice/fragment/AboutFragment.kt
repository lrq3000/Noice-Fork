package com.github.ashutoshgngwr.noice.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.github.ashutoshgngwr.noice.BuildConfig
import com.github.ashutoshgngwr.noice.NoiceApplication
import com.github.ashutoshgngwr.noice.R
import com.github.ashutoshgngwr.noice.ext.launchInCustomTab
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

class AboutFragment : Fragment() {

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return AboutPage(context).run {
      setImage(R.drawable.ic_launcher_24dp)
      setDescription("${getString(R.string.app_name)}\n\n${getString(R.string.fork_about_description)}")
      addItem(
        buildElement(
          R.drawable.ic_about_version,
          "v${BuildConfig.VERSION_NAME}",
          getString(R.string.app_changelog_url)
        )
      )

      addItem(
        buildElement(
          R.drawable.ic_about_copyright,
          R.string.app_copyright,
          R.string.app_license_url
        )
      )

      addItem(
        buildElement(
          R.drawable.ic_about_group,
          R.string.app_authors,
          R.string.app_authors_url
        )
      )

      addItem(
        buildElement(
          R.drawable.about_icon_github,
          R.string.about_github,
          R.string.app_github_url
        )
      )

      addItem(
        buildElement(
          R.drawable.about_icon_link,
          R.string.fork_privacy,
          R.string.app_privacy_url
        )
      )

      addGroup(getString(R.string.fork_maintainer_label))
      addItem(buildElement(R.drawable.ic_about_group, R.string.fork_maintainer_name, R.string.app_authors_url))
      addGroup(getString(R.string.fork_original_creator_label))
      addItem(buildElement(R.drawable.ic_about_group, R.string.fork_original_creator_name, R.string.app_authors_url))
      create()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    NoiceApplication.of(requireContext())
      .analyticsProvider
      .setCurrentScreen("about", AboutFragment::class)
  }

  private fun buildElement(
    @DrawableRes iconId: Int,
    @StringRes titleResId: Int,
    @StringRes urlResId: Int
  ): Element {
    return buildElement(iconId, getString(titleResId), getString(urlResId))
  }

  private fun buildElement(@DrawableRes iconId: Int, title: String, url: String): Element {
    return Element(title, iconId)
      .setAutoApplyIconTint(true)
      .setOnClickListener { Uri.parse(url).launchInCustomTab(requireContext()) }
  }

}
