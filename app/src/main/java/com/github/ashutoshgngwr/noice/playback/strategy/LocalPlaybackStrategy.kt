package com.github.ashutoshgngwr.noice.playback.strategy

import android.content.Context
import android.net.Uri
import com.github.ashutoshgngwr.noice.ext.cancelFade
import androidx.media.AudioAttributesCompat
import com.github.ashutoshgngwr.noice.ext.fade
import com.github.ashutoshgngwr.noice.ext.setAudioAttributesCompat
import com.github.ashutoshgngwr.noice.model.Sound
import com.github.ashutoshgngwr.noice.repository.SettingsRepository
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

/**
 * [LocalPlaybackStrategy] implements [PlaybackStrategy] which plays the media locally
 * on device using the [SimpleExoPlayer] implementation.
 */
class LocalPlaybackStrategy(
  context: Context,
  audioAttributes: AudioAttributesCompat,
  sound: Sound
) : PlaybackStrategy {

  companion object {
    // a smaller default used when changing volume of an active player.
    internal const val DEFAULT_FADE_DURATION = 1000L
  }

  private val players = sound.src.map { initPlayer(context, it, sound.isLooping) }
  private val settingsRepository = SettingsRepository.newInstance(context)
  private var volume: Float = 0f
  private var paused = false

  init {
    setAudioAttributes(audioAttributes)
  }

  private fun initPlayer(context: Context, src: String, isLooping: Boolean): SimpleExoPlayer {
    return SimpleExoPlayer.Builder(context)
      .build()
      .apply {
        repeatMode = if (isLooping) {
          ExoPlayer.REPEAT_MODE_ONE
        } else {
          ExoPlayer.REPEAT_MODE_OFF
        }

        prepare(
          Util.getUserAgent(context, context.packageName).let { userAgent ->
            ProgressiveMediaSource.Factory(DefaultDataSourceFactory(context, userAgent))
              .createMediaSource(Uri.parse("asset:///$src"))
          }
        )
      }
  }

  override fun setVolume(volume: Float) {
    this.volume = volume
    players.forEach { it.fade(it.volume, volume, duration = DEFAULT_FADE_DURATION) }
  }

  override fun play() {
    val wasPaused = paused
    paused = false
    for (player in players) {
      val wasPlaying = player.isPlaying
      val looping = player.repeatMode == ExoPlayer.REPEAT_MODE_ONE
      if (!looping && !wasPlaying) {
        player.seekTo(0)
      }

      if (!looping && wasPaused) {
        player.cancelFade()
        player.volume = volume
      }
      player.playWhenReady = true
      // A quick resume must replace the pending fade-out even if ExoPlayer is still playing.
      if (looping && (!wasPlaying || wasPaused)) {
        val start = if (wasPlaying) player.volume else 0f
        player.fade(start, volume, settingsRepository.getSoundFadeInDurationMillis())
      }
    }
  }

  override fun pause() {
    paused = true
    players.forEach {
      it.fade(it.volume, 0f, duration = DEFAULT_FADE_DURATION) {
        it.playWhenReady = false
      }
    }
  }

  override fun stop() {
    for (player in players) {
      if (!player.playWhenReady) {
        continue
      }

      player.fade(player.volume, 0f, DEFAULT_FADE_DURATION) {
        player.playWhenReady = false
        player.release()
      }
    }
  }

  override fun setAudioAttributes(attrs: AudioAttributesCompat) {
    players.forEach { it.setAudioAttributesCompat(attrs) }
  }
}
