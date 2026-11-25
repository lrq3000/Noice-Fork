package com.github.ashutoshgngwr.noice.fragment

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.test.core.app.ApplicationProvider
import com.github.ashutoshgngwr.noice.R
import com.github.ashutoshgngwr.noice.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FadeInPreferenceTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
  private val settings = SettingsRepository.newInstance(context)
  private val currentKey = context.getString(R.string.sound_fade_in_duration_key)
  private val legacyKey = context.getString(R.string.sound_fade_duration_key)

  @Test
  fun unsetFadeInIsFiveSecondsBeforeOpeningSettings() {
    assertEquals(5000L, settings.getSoundFadeInDurationMillis())
  }

  @Test
  fun settingsShowsFiveSecondsOnFirstVisit() {
    assertEquals(5, openSettingsAndReadFadeIn())
    assertEquals(5000L, settings.getSoundFadeInDurationMillis())
  }

  @Test
  fun openingSettingsKeepsTheLegacyFadeInWhenNoNewValueExists() {
    prefs.edit { putInt(legacyKey, 7) }

    assertEquals(7000L, settings.getSoundFadeInDurationMillis())
    assertEquals(7, openSettingsAndReadFadeIn())
    assertEquals(7000L, settings.getSoundFadeInDurationMillis())
  }

  @Test
  fun aSavedOneSecondFadeInOverridesTheLegacyValue() {
    prefs.edit {
      putInt(legacyKey, 7)
      putInt(currentKey, 1)
    }

    assertEquals(1, openSettingsAndReadFadeIn())
    assertEquals(1000L, settings.getSoundFadeInDurationMillis())
  }

  @Test
  fun aSavedZeroStillDisablesFadeIn() {
    prefs.edit { putInt(currentKey, 0) }

    assertEquals(0, openSettingsAndReadFadeIn())
    assertEquals(0L, settings.getSoundFadeInDurationMillis())
  }

  private fun openSettingsAndReadFadeIn(): Int {
    val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
    controller.get().setTheme(R.style.Theme_App)
    controller.setup()
    try {
      val fragment = SettingsFragment()
      controller.get().supportFragmentManager.beginTransaction().add(fragment, null).commitNow()
      return requireNotNull(fragment.findPreference<SeekBarPreference>(currentKey)).value
    } finally {
      controller.pause().stop().destroy()
    }
  }
}
