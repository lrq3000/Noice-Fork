package com.github.ashutoshgngwr.noice.repository

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.ashutoshgngwr.noice.R

/**
 * [SettingsRepository] implements the data access layer for storing various user preferences.
 */
class SettingsRepository private constructor(private val context: Context) {

  companion object {
    const val APP_THEME_LIGHT = 0
    const val APP_THEME_DARK = 1
    const val APP_THEME_SYSTEM_DEFAULT = 2

    /**
     * Creates a new instance of [SettingsRepository]. It is needed because mockk in unable to mock
     * constructors on Android instrumented tests and there's no cleaner way to inject mocks in
     * Android components than mocking companion object methods.
     */
    fun newInstance(context: Context) = SettingsRepository(context)
  }

  private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

  /**
   * Gets user setting for current app theme.
   * Returns one of [APP_THEME_LIGHT], [APP_THEME_DARK] or [APP_THEME_SYSTEM_DEFAULT]
   */
  fun getAppTheme(): Int {
    return prefs.getInt(context.getString(R.string.app_theme_key), APP_THEME_SYSTEM_DEFAULT)
  }

  /**
   * Gets user setting for app theme and converts it into its corresponding value from
   * array ([AppCompatDelegate.MODE_NIGHT_NO], [AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM],
   * [AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM]).
   */
  fun getAppThemeAsNightMode(): Int {
    return when (getAppTheme()) {
      APP_THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
      APP_THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
      APP_THEME_SYSTEM_DEFAULT -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
    }
  }

  /**
   * Sets user setting for current app theme.
   * @param newTheme should be one of [APP_THEME_LIGHT], [APP_THEME_DARK] or [APP_THEME_SYSTEM_DEFAULT]
   */
  fun setAppTheme(newTheme: Int) {
    prefs.edit { putInt(context.getString(R.string.app_theme_key), newTheme) }
  }

  /**
   * Returns the current value of switch preference with key [R.string.presets_as_home_screen_key].
   */
  fun shouldDisplayPresetsAsHomeScreen(): Boolean {
    return prefs.getBoolean(context.getString(R.string.presets_as_home_screen_key), false)
  }

  /**
   * Returns the value of [R.string.sound_fade_in_duration_key] preference in milliseconds.
   */
  fun getSoundFadeInDurationMillis(): Long {
    return prefs.getInt(
      context.getString(R.string.sound_fade_in_duration_key),
      // inherit fallback value from the deprecated preference.
      prefs.getInt(
        context.getString(R.string.sound_fade_duration_key),
        context.resources.getInteger(R.integer.default_sound_fade_in_seconds)
      )
    ) * 1000L
  }

  internal fun migrateLegacySoundFadeInPreference() {
    val currentKey = context.getString(R.string.sound_fade_in_duration_key)
    val legacyKey = context.getString(R.string.sound_fade_duration_key)
    // The slider persists its default when first inflated, so copy a legacy choice before then.
    if (!prefs.contains(currentKey) && prefs.contains(legacyKey)) {
      prefs.edit { putInt(currentKey, prefs.getInt(legacyKey, 0)) }
    }
  }

  /**
   * Returns the value of switch preference with key [R.string.ignore_audio_focus_changes_key].
   */
  fun shouldIgnoreAudioFocusChanges(): Boolean {
    return prefs.getBoolean(context.getString(R.string.ignore_audio_focus_changes_key), false)
  }

  /**
   * Returns the value of switch preference with key [R.string.should_display_sound_icons_key].
   */
  fun shouldDisplaySoundIcons(): Boolean {
    return prefs.getBoolean(context.getString(R.string.should_display_sound_icons_key), true)
  }

  /**
   * Returns the value of switch preference with key [R.string.should_share_usage_data_key].
   */
  fun shouldShareUsageData(): Boolean {
    return prefs.getBoolean(context.getString(R.string.should_share_usage_data_key), false)
  }

  /**
   * Sets the value of switch preference with key [R.string.should_share_usage_data_key].
   */
  fun setShouldShareUsageData(enabled: Boolean) {
    prefs.edit { putBoolean(context.getString(R.string.should_share_usage_data_key), enabled) }
  }

  /**
   * Returns the value of switch preference with key [R.string.enable_media_buttons_key]
   */
  fun isMediaButtonsEnabled(): Boolean {
    return prefs.getBoolean(
      context.getString(R.string.enable_media_buttons_key), true
    )
  }
}
