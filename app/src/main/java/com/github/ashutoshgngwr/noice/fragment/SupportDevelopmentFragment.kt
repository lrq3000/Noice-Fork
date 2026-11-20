package com.github.ashutoshgngwr.noice.fragment

import android.os.Bundle
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.github.ashutoshgngwr.noice.NoiceApplication
import com.github.ashutoshgngwr.noice.R
import com.github.ashutoshgngwr.noice.databinding.SupportDevelopmentFragmentBinding
import com.github.ashutoshgngwr.noice.ext.launchInCustomTab

class SupportDevelopmentFragment : Fragment() {

  private lateinit var binding: SupportDevelopmentFragmentBinding
  private lateinit var app: NoiceApplication

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = SupportDevelopmentFragmentBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    app = NoiceApplication.of(requireContext())
    binding.contributeButton.setOnClickListener {
      Uri.parse(getString(R.string.app_contributing_url))
        .launchInCustomTab(requireContext())
    }

    binding.shareButton.setOnClickListener {
      val text = getString(R.string.app_description)
      ShareCompat.IntentBuilder(requireActivity())
        .setChooserTitle(R.string.support_development__share)
        .setType("text/plain")
        .setText("$text\n\n${getString(R.string.app_github_url)}")
        .startChooser()

      app.analyticsProvider.logEvent("share_app_with_friends", bundleOf())
    }

    app.analyticsProvider.setCurrentScreen("support_development", SupportDevelopmentFragment::class)
  }
}
