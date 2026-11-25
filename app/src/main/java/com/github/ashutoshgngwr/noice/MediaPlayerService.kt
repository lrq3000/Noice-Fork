package com.github.ashutoshgngwr.noice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.getSystemService
import androidx.media.AudioAttributesCompat
import androidx.mediarouter.media.MediaRouter
import com.github.ashutoshgngwr.noice.activity.MainActivity
import com.github.ashutoshgngwr.noice.model.Preset
import com.github.ashutoshgngwr.noice.playback.PlaybackController
import com.github.ashutoshgngwr.noice.playback.Player
import com.github.ashutoshgngwr.noice.playback.PlayerManager
import com.github.ashutoshgngwr.noice.playback.PlayerNotificationManager
import com.github.ashutoshgngwr.noice.playback.PresetVolume
import com.github.ashutoshgngwr.noice.repository.PresetRepository
import com.github.ashutoshgngwr.noice.repository.SettingsRepository
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class MediaPlayerService : Service() {
  /**
   * [MediaPlayerService] publishes [PlaybackUpdateEvent] whenever there's an update to
   * [PlayerManager].
   *
   * @param state [PlaybackStateCompat] representing the current player state
   * @param players the currently playing sound [Players][Player]
   * @param presetVolume the live volume control when a saved preset is active
   */
  data class PlaybackUpdateEvent(
    val state: Int, val players: Map<String, Player>, val presetVolume: PresetVolume? = null
  )

  companion object {
    private val TAG = MediaPlayerService::class.java.simpleName
    private val WAKELOCK_TIMEOUT = TimeUnit.DAYS.toMillis(1)

    private const val FOREGROUND_ID = 0x29
    private const val ALARM_FOREGROUND_ID = 0x2A
    private const val RC_MAIN_ACTIVITY = 0x28
  }

  private lateinit var wakeLock: PowerManager.WakeLock
  private lateinit var mediaSession: MediaSessionCompat
  private lateinit var playerManager: PlayerManager
  private lateinit var presetRepository: PresetRepository
  private lateinit var settingsRepository: SettingsRepository
  private var inForeground = false
  private var wakeUpPreset: Preset? = null

  private val becomingNoisyReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
        Log.i(TAG, "Becoming noisy... Pause playback!")
        playerManager.pause()
      }
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    wakeLock = requireNotNull(getSystemService<PowerManager>()).run {
      newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, packageName).apply {
        setReferenceCounted(false)
      }
    }

    mediaSession = MediaSessionCompat(this, "$TAG.mediaSession").also {
      var piFlags = PendingIntent.FLAG_UPDATE_CURRENT
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        piFlags = piFlags or PendingIntent.FLAG_IMMUTABLE
      }

      it.setSessionActivity(
        PendingIntent.getActivity(
          this, RC_MAIN_ACTIVITY, Intent(this, MainActivity::class.java), piFlags
        )
      )

      it.setPlaybackToLocal(AudioManager.STREAM_MUSIC)
      it.isActive = true
      MediaRouter.getInstance(this).setMediaSessionCompat(it)
    }

    playerManager = PlayerManager(this, mediaSession)
    playerManager.setPlaybackUpdateListener(this::onPlaybackUpdate)
    playerManager.setWakeUpDismissAction(this::dismissWakeUpAlarm)

    presetRepository = PresetRepository.newInstance(this)
    settingsRepository = SettingsRepository.newInstance(this)
    PlayerNotificationManager.createChannel(this)
    registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
  }

  private fun onPlaybackUpdate(state: Int, players: Map<String, Player>) {
    val presetVolume = playerManager.currentPresetVolume()
    EventBus.getDefault().postSticky(PlaybackUpdateEvent(state, players, presetVolume))

    val currentPreset = when {
      presetVolume != null -> presetRepository.get(presetVolume.presetID)
      players.isNotEmpty() -> presetRepository.list().firstOrNull { it.matchesPlayback(players) }
      else -> null
    }
    val title = currentPreset?.name ?: getString(R.string.unsaved_preset)
    mediaSession.setMetadata(
      MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .build()
    )

    if (state == PlaybackStateCompat.STATE_STOPPED) {
      if (wakeUpPreset != null) {
        wakeUpPreset = null
        playerManager.setAudioUsage(AudioAttributesCompat.USAGE_MEDIA)
      }
      leaveForeground()
      if (wakeLock.isHeld) wakeLock.release()
    } else {
      wakeLock.acquire(WAKELOCK_TIMEOUT)
      val alarm = wakeUpPreset
      if (alarm == null) {
        enterForeground(PlayerNotificationManager.createNotification(this, mediaSession, title, currentPreset != null))
      }
      // Keep the original alarm notification while it rings instead of retriggering the full-screen action.
    }
  }

  private fun enterForeground(notification: Notification, id: Int = FOREGROUND_ID) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
    } else {
      startForeground(id, notification)
    }
    inForeground = true
  }

  private fun leaveForeground() {
    @Suppress("DEPRECATION") // This overload also exists on Android 5.
    stopForeground(true)
    inForeground = false
  }

  private fun startWakeUpAlarm(intent: Intent, startId: Int) {
    val preset = presetRepository.get(intent.getStringExtra(PlaybackController.EXTRA_PRESET_ID))
    if (preset == null || preset.playerStates.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      if (!inForeground) {
        // Satisfy the pending foreground-service start even if the scheduled preset was removed.
        enterForeground(PlayerNotificationManager.createNotification(this, mediaSession, getString(R.string.wake_up_timer), false))
        leaveForeground()
        stopSelf(startId)
      }
      return
    }
    wakeUpPreset = preset
    // On Android 15, the service must be foreground before requesting audio focus from the background.
    // A notification update cannot open the full-screen ringer; post a distinct alarm notification.
    enterForeground(
      PlayerNotificationManager.createWakeUpNotification(this, mediaSession, preset.id, preset.name),
      ALARM_FOREGROUND_ID
    )
    playerManager.setAudioUsage(AudioAttributesCompat.USAGE_ALARM)
    playerManager.playPreset(preset, saved = true)
  }

  private fun dismissWakeUpAlarm(): Boolean {
    if (wakeUpPreset == null) return false
    playerManager.stop()
    return true
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null) return START_STICKY
    when (intent.action) {
      PlaybackController.ACTION_RING_WAKE_UP -> {
        startWakeUpAlarm(intent, startId)
        return START_NOT_STICKY
      }
      PlaybackController.ACTION_DISMISS_WAKE_UP -> {
        if (!dismissWakeUpAlarm() && !inForeground) stopSelf(startId)
        return START_NOT_STICKY
      }
      else -> {
        if (wakeUpPreset != null && intent.action == PlaybackController.ACTION_PAUSE_PLAYBACK) {
          dismissWakeUpAlarm()
        } else {
          if (intent.action == PlaybackController.ACTION_PLAY_SOUND ||
            intent.action == PlaybackController.ACTION_PLAY_PRESET ||
            intent.action == PlaybackController.ACTION_PLAY_RANDOM_PRESET) {
            dismissWakeUpAlarm()
          }
          PlaybackController.handleServiceIntent(this, playerManager, intent)
        }
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    unregisterReceiver(becomingNoisyReceiver)
    playerManager.cleanup()
    mediaSession.release()
    PlaybackController.clearAutoStopCallback()
    if (wakeLock.isHeld) wakeLock.release()
    super.onDestroy()
  }
}
