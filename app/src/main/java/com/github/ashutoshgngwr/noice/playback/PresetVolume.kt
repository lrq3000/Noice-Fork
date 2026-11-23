package com.github.ashutoshgngwr.noice.playback

import androidx.annotation.MainThread
import com.github.ashutoshgngwr.noice.model.Preset

/** Playback-only volume for one activation of a saved preset. */
class PresetVolume internal constructor(
  preset: Preset,
  private val players: Map<String, Player>,
  private val isCurrent: (PresetVolume) -> Boolean
) {
  val presetID = preset.id
  private val savedMix = preset.playerStates.copyOf()
  private val defaultPercent = (savedMix.maxOfOrNull { it.volume } ?: 0) * 100 / Player.MAX_VOLUME
  val canAdjust = defaultPercent > 0
  var percent = defaultPercent
    private set

  @MainThread
  fun setPercent(percent: Int) {
    val target = percent.coerceIn(0, 100)
    if (!canAdjust || target == this.percent || !isCurrent(this)) return

    this.percent = target
    for (sound in savedMix) {
      players.getValue(sound.soundKey).setPlaybackVolume(levelFor(sound))
    }
  }

  // Starting from the saved mix on every drag preserves quiet sounds through mute and avoids rounding drift.
  private fun levelFor(sound: Preset.PlayerState): Float =
    if (!canAdjust || percent == defaultPercent) sound.volume.toFloat()
    else sound.volume * percent.toFloat() / defaultPercent

  internal fun matchesPlayback(): Boolean =
    players.size == savedMix.size && savedMix.all { sound ->
      val player = players[sound.soundKey]
      player != null && player.playbackVolume == levelFor(sound) && player.timePeriod == sound.timePeriod
    }
}
