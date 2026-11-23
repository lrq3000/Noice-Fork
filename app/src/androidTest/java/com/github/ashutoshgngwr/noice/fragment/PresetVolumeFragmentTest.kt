package com.github.ashutoshgngwr.noice.fragment

import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.view.MotionEvent
import android.widget.SeekBar
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.ashutoshgngwr.noice.MediaPlayerService
import com.github.ashutoshgngwr.noice.R
import com.github.ashutoshgngwr.noice.model.Preset
import com.github.ashutoshgngwr.noice.playback.Player
import com.github.ashutoshgngwr.noice.playback.PresetVolume
import com.github.ashutoshgngwr.noice.playback.strategy.PlaybackStrategyFactory
import com.github.ashutoshgngwr.noice.repository.PresetRepository
import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.endsWith
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresetVolumeFragmentTest {
  private lateinit var scenario: FragmentScenario<PresetsFragment>
  private lateinit var preset: Preset
  private lateinit var players: Map<String, Player>
  private var presetPosition = -1
  private var volume: PresetVolume? = null

  @Before
  fun setup() {
    scenario = launchFragmentInContainer(themeResId = R.style.Theme_App)
    scenario.onFragment { fragment ->
      val presets = PresetRepository.newInstance(fragment.requireContext()).list()
      presetPosition = presets.indexOfFirst { it.playerStates.any { state -> state.soundKey == "seaside" } }
      preset = presets[presetPosition]
      val factory = mockk<PlaybackStrategyFactory> {
        every { newInstance(any()) } returns mockk(relaxed = true)
      }
      players = preset.playerStates.associate { state ->
        state.soundKey to Player(state.soundKey, factory).apply { setVolume(state.volume) }
      }
    }
  }

  @After
  fun teardown() {
    scenario.moveToState(Lifecycle.State.DESTROYED)
  }

  @Test
  fun activeRowShowsPresetVolumeAndSwipingScalesTheLiveMix() {
    val slider = onView(allOf(withId(R.id.preset_volume_slider), isDisplayed()))
    slider.check(doesNotExist())
    scenario.onFragment(::startPreset)
    onView(withId(R.id.list))
      .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(presetPosition))
    onView(allOf(withId(R.id.preset_volume_label), isDisplayed()))
      .check(matches(withText(endsWith("80%"))))

    slider.perform(GeneralSwipeAction(
      Swipe.SLOW, { pointAt(it as SeekBar, 0.8f) }, { pointAt(it as SeekBar, 0.4f) }, Press.FINGER
    ))

    assertTrue(requireNotNull(volume).percent in 35..45)
    assertEquals(
      0.75f * players.getValue("seaside").playbackVolume,
      players.getValue("wind_in_palm_trees").playbackVolume,
      0.00001f
    )
    slider.check(matches(isDisplayed()))
  }

  @Test
  fun anInFlightDragCannotChangeAReplacementActivation() {
    scenario.onFragment(::startPreset)
    onView(allOf(withId(R.id.preset_volume_slider), isDisplayed())).check(matches(isDisplayed()))

    scenario.onFragment { fragment ->
      val list = fragment.requireView().findViewById<RecyclerView>(R.id.list)
      val slider = requireNotNull(list.findViewHolderForAdapterPosition(presetPosition)).itemView
        .findViewById<SeekBar>(R.id.preset_volume_slider)
      val downTime = SystemClock.uptimeMillis()
      fun send(action: Int, fraction: Float) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
          slider.width * fraction, slider.height / 2f, 0)
        slider.onTouchEvent(event)
        event.recycle()
      }

      send(MotionEvent.ACTION_DOWN, 0.8f)
      send(MotionEvent.ACTION_MOVE, 0.6f)
      assertTrue(requireNotNull(volume).percent < 80)
      startPreset(fragment)
      send(MotionEvent.ACTION_MOVE, 0.4f)
      send(MotionEvent.ACTION_UP, 0.4f)

      assertEquals(80, requireNotNull(volume).percent)
    }
  }

  private fun startPreset(fragment: PresetsFragment) {
    preset.playerStates.forEach { players.getValue(it.soundKey).setVolume(it.volume) }
    volume = PresetVolume(preset, players) { it === volume }
    fragment.onPlayerManagerUpdate(
      MediaPlayerService.PlaybackUpdateEvent(PlaybackStateCompat.STATE_PLAYING, players, volume)
    )
  }

  private fun pointAt(view: SeekBar, fraction: Float): FloatArray {
    val xy = IntArray(2)
    view.getLocationOnScreen(xy)
    val trackWidth = view.width - view.paddingLeft - view.paddingRight
    return floatArrayOf(xy[0] + view.paddingLeft + trackWidth * fraction, xy[1] + view.height / 2f)
  }
}
