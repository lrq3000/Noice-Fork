package com.github.ashutoshgngwr.noice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.github.ashutoshgngwr.noice.activity.AlarmRingerActivity
import com.github.ashutoshgngwr.noice.activity.MainActivity
import com.github.ashutoshgngwr.noice.playback.PlaybackController
import com.github.ashutoshgngwr.noice.repository.PresetRepository
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose

object WakeUpTimerManager {

  /**
   * [Timer] declares fields necessary to schedule a Wake-up timer.
   */
  data class Timer(@Expose var presetID: String, @Expose var atMillis: Long)

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  const val PREF_WAKE_UP_TIMER = "wake_up_timer"

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  const val PREF_LAST_USED_PRESET_ID = "last_used_preset_id"

  private const val RC_MAIN_ACTIVITY = 0x40

  private val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()

  private inline fun <T> withAlarmManager(ctx: Context, crossinline block: (AlarmManager) -> T): T {
    return with(requireNotNull(ctx.getSystemService()), block)
  }

  private fun getPendingIntentForActivity(context: Context) =
    Intent(context, MainActivity::class.java).let {
      it.putExtra(MainActivity.EXTRA_NAV_DESTINATION, R.id.wake_up_timer)
      var piFlags = PendingIntent.FLAG_UPDATE_CURRENT
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        piFlags = piFlags or PendingIntent.FLAG_IMMUTABLE
      }

      PendingIntent.getActivity(context, RC_MAIN_ACTIVITY, it, piFlags)
    }

  fun canScheduleExactAlarm(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
      withAlarmManager(context) { it.canScheduleExactAlarms() }

  private fun getAlarmOperation(context: Context, presetID: String?): PendingIntent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // An exact alarm can start a foreground service even if Android blocks an activity launch.
      PlaybackController.buildWakeUpAlarmPendingIntent(context, presetID)
    } else {
      AlarmRingerActivity.getPendingIntent(context, presetID)
    }

  /**
   * [set] schedules the given [Timer] using Android's [AlarmManager]. It also persists the [Timer]
   * instance onto device storage to be able return it later upon querying.
   */
  fun set(context: Context, timer: Timer): Boolean {
    if (timer.atMillis <= System.currentTimeMillis() || !canScheduleExactAlarm(context)) {
      return false
    }

    if (PresetRepository.newInstance(context).get(timer.presetID) == null) {
      return false
    }

    try {
      withAlarmManager(context) {
        it.setAlarmClock(
          AlarmManager.AlarmClockInfo(timer.atMillis, getPendingIntentForActivity(context)),
          getAlarmOperation(context, timer.presetID)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          // Cancel the old activity-based operation when upgrading an existing alarm.
          it.cancel(AlarmRingerActivity.getPendingIntent(context, null))
        }
      }
    } catch (e: SecurityException) {
      // Special access can change between the check and scheduling.
      Log.w("WakeUpTimerManager", "Unable to schedule an exact wake-up alarm", e)
      return false
    }

    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putString(PREF_WAKE_UP_TIMER, gson.toJson(timer))
    }
    return true
  }

  /**
   * [cancel] cancels the last scheduled timer.
   */
  fun cancel(context: Context) {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      remove(PREF_WAKE_UP_TIMER)
    }

    withAlarmManager(context) {
      // don't need concrete timer value for cancelling the alarm.
      it.cancel(getAlarmOperation(context, null))
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        it.cancel(AlarmRingerActivity.getPendingIntent(context, null))
      }
    }
  }

  /**
   * [get] returns the last scheduled timer if found in the persisted state. Returns null otherwise.
   */
  fun get(context: Context): Timer? {
    val timer = PreferenceManager.getDefaultSharedPreferences(context)
      .getString(PREF_WAKE_UP_TIMER, null)

    return gson.fromJson(timer, Timer::class.java)
  }

  /**
   * [saveLastUsedPresetID] saves the ID of the preset last used by the user in a Wake-up timer.
   */
  fun saveLastUsedPresetID(context: Context, selectedPresetID: String?) {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    with(sharedPrefs.edit()) {
      putString(PREF_LAST_USED_PRESET_ID, selectedPresetID)
      apply()
    }
  }

  /**
   * [getLastUsedPresetID] return the saved ID of the last selected preset. Returns null otherwise.
   */
  fun getLastUsedPresetID(context: Context): String? {
    return with(PreferenceManager.getDefaultSharedPreferences(context)) {
      val lastSelectedPresetID = getString(PREF_LAST_USED_PRESET_ID, null)

      // ensure that preset with given ID exists in preferences.
      PresetRepository.newInstance(context).get(lastSelectedPresetID)?.id
    }
  }

  /**
   * Restores a future timer after reboot, app update or Android regranting exact-alarm access.
   */
  class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
      when (intent?.action) {
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> Unit
        else -> return
      }

      val timer = get(context) ?: return
      if (timer.atMillis > System.currentTimeMillis()) {
        set(context, timer)
      }
    }
  }
}
