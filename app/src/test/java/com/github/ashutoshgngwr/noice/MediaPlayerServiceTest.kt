package com.github.ashutoshgngwr.noice

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.AudioManagerCompat
import androidx.mediarouter.media.MediaRouter
import androidx.test.core.app.ApplicationProvider
import com.github.ashutoshgngwr.noice.playback.PlaybackController
import com.github.ashutoshgngwr.noice.playback.PlaybackUpdateListener
import com.github.ashutoshgngwr.noice.playback.Player
import com.github.ashutoshgngwr.noice.playback.PlayerManager
import com.github.ashutoshgngwr.noice.repository.PresetRepository
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.greenrobot.eventbus.EventBus
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPowerManager

@RunWith(RobolectricTestRunner::class)
class MediaPlayerServiceTest {

  private lateinit var serviceController: ServiceController<MediaPlayerService>
  private lateinit var mockEventBus: EventBus
  private lateinit var mockServiceIntent: Intent

  @Before
  fun setup() {
    // mock so that service is created (onCreate calls CastAPIProvider constructor via PlayerManager).
    val application = ApplicationProvider.getApplicationContext<NoiceApplication>()
    application.castAPIProvider = mockk(relaxed = true)
    shadowOf(application).removeSystemService(Context.WIFI_SERVICE)
    // No cast devices are involved; leave the real player and media session under test.
    mockkStatic(MediaRouter::class)
    every { MediaRouter.getInstance(any()) } returns mockk(relaxed = true)

    mockkStatic(EventBus::class)
    mockEventBus = mockk(relaxed = true)
    every { EventBus.getDefault() } returns mockEventBus

    mockServiceIntent = mockk(relaxed = true)
    serviceController = Robolectric.buildService(MediaPlayerService::class.java, mockServiceIntent)
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  @Test
  fun testOnPlayerUpdateListener_onOngoingPlayback() {
    mockkConstructor(PlayerManager::class)

    val listenerSlot = slot<PlaybackUpdateListener>()
    every {
      anyConstructed<PlayerManager>().setPlaybackUpdateListener(capture(listenerSlot))
    } returns Unit

    serviceController.create() // will register the listener\

    val wakeLock = ShadowPowerManager.getLatestWakeLock()
    assertFalse(wakeLock.isHeld) // should not be held by default

    val players = hashMapOf<String, Player>()
    listenerSlot.invoke(PlaybackStateCompat.STATE_PLAYING, players) // invoke the listener

    assertTrue(wakeLock.isHeld)
    val eventSlot = slot<MediaPlayerService.PlaybackUpdateEvent>()
    verify(exactly = 1) { mockEventBus.postSticky(capture(eventSlot)) }
    assertEquals(PlaybackStateCompat.STATE_PLAYING, eventSlot.captured.state)
    assertEquals(players, eventSlot.captured.players)

    clearMocks(mockEventBus)
    listenerSlot.invoke(PlaybackStateCompat.STATE_PAUSED, players)
    assertTrue(wakeLock.isHeld) // should not release the wakelock
    verify(exactly = 1) { mockEventBus.postSticky(capture(eventSlot)) }
    assertEquals(PlaybackStateCompat.STATE_PAUSED, eventSlot.captured.state)

    clearMocks(mockEventBus)
    listenerSlot.invoke(PlaybackStateCompat.STATE_STOPPED, players)
    assertFalse(wakeLock.isHeld) // should release the wakelock
    verify(exactly = 1) { mockEventBus.postSticky(capture(eventSlot)) }
    assertEquals(PlaybackStateCompat.STATE_STOPPED, eventSlot.captured.state)
  }

  @Test
  fun testWakeLock_onServiceDestroy() {
    mockkObject(PlaybackController)
    serviceController.create().destroy()
    ShadowLooper.idleMainLooper()
    assertFalse(ShadowPowerManager.getLatestWakeLock().isHeld)
    verify(exactly = 1) { PlaybackController.clearAutoStopCallback() }
  }

  @Test
  @Config(sdk = [31])
  fun aBackgroundWakeUpAlarmEntersTheMediaForegroundBeforeRequestingAudioFocus() {
    val service = serviceController.create().get()
    val preset = PresetRepository.newInstance(service).list().first()
    val shadow = shadowOf(service)
    mockkStatic(AudioManagerCompat::class)
    every { AudioManagerCompat.requestAudioFocus(any(), any()) } answers {
      assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, service.foregroundServiceType)
      assertEquals(Notification.CATEGORY_ALARM, shadow.lastForegroundNotification.category)
      AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    every { AudioManagerCompat.abandonAudioFocusRequest(any(), any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    service.onStartCommand(
      Intent(PlaybackController.ACTION_RING_WAKE_UP).putExtra(PlaybackController.EXTRA_PRESET_ID, preset.id),
      0,
      1
    )
    verify { AudioManagerCompat.requestAudioFocus(any(), any()) }
    assertTrue(shadow.isLastForegroundNotificationAttached)

    service.onStartCommand(Intent(PlaybackController.ACTION_DISMISS_WAKE_UP), 0, 2)
    assertTrue(shadow.isForegroundStopped)
    assertFalse(shadow.isLastForegroundNotificationAttached)
    serviceController.destroy()
  }

  @Test
  @Config(sdk = [31])
  fun anAlarmDuringExistingPlaybackPostsADistinctForegroundNotification() {
    val service = serviceController.create().get()
    val preset = PresetRepository.newInstance(service).list().first()
    val shadow = shadowOf(service)
    mockkStatic(AudioManagerCompat::class)
    every { AudioManagerCompat.requestAudioFocus(any(), any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    every { AudioManagerCompat.abandonAudioFocusRequest(any(), any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    fun send(action: String, id: Int) {
      service.onStartCommand(Intent(action).putExtra(PlaybackController.EXTRA_PRESET_ID, preset.id), 0, id)
    }
    send(PlaybackController.ACTION_PLAY_PRESET, 1)
    val playbackNotificationId = shadow.lastForegroundNotificationId

    send(PlaybackController.ACTION_RING_WAKE_UP, 2)

    assertNotEquals(playbackNotificationId, shadow.lastForegroundNotificationId)
    assertEquals(Notification.CATEGORY_ALARM, shadow.lastForegroundNotification.category)
    serviceController.destroy()
  }

  @Test
  fun testOnStartCommand() {
    mockkObject(PlaybackController)
    serviceController.create().startCommand(0, 0)
    verify(exactly = 1) {
      PlaybackController.handleServiceIntent(any(), any(), mockServiceIntent)
    }
  }
}
