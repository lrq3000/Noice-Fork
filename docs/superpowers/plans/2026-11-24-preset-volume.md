# Preset Volume Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` for inline work or `subagent-driven-development` if delegation is authorized. Steps use checkbox syntax for tracking.

**Goal:** Give the active saved preset an inline horizontal slider that scales its mix proportionally.

**Architecture:** `PresetVolume` owns one activation’s baseline and target percentage. `PlayerManager` controls its lifetime and publishes it through the existing in-process playback event. Players accept fractional playback levels without changing the saved format.

**Tech Stack:** Kotlin, Android XML views, AppCompat SeekBar, EventBus, Robolectric and Espresso.

**Spec:** `docs/superpowers/specs/2026-11-24-preset-volume-design.md`.

## Global constraints

- Keep playback offline and preserve v1 sound keys, the legacy integer recipe format, and existing audio assets.
- Build the Free Debug variant with JDK 11 and Android SDK 30. Keep `main` unchanged.

## Review focus

- Mute must not discard the reference mix; verify `80 → 0 → 100 → 80`.
- Pause, resume, and restart have different lifetimes; verify that pause retains the level and reactivation resets.
- Manual edits end the volume session but do not change untouched sounds; verify mixed playback after an edit.
- A recycled row or replaced session must not adjust the new preset; verify stale control rejection and row state.
- All-zero recipes must remain finite and silent; verify that the control cannot raise them.

## One user-visible slice: scale and control a running preset

- [x] Add regression tests in `app/src/test/java/com/github/ashutoshgngwr/noice/playback/PlayerManagerTest.kt` for initial 80%, fractional scaling at 40% and 100%, mute, pause/resume, fresh activation, all-zero presets, and detachment after individual edits. Run `:app:testFreeDebugUnitTest --tests '*PlayerManagerTest'` to observe the missing behavior.
- [x] Add `PresetVolume` in the playback package (`presetID: String`, `percent: Int`, `canAdjust: Boolean`, `setPercent(percent: Int)`). Add `Player.playbackVolume: Float` and `setPlaybackVolume(volume: Float)`, retaining the existing integer `volume`/`setVolume` interface for recipes and per-sound controls. In `PlayerManager.currentPresetVolume(): PresetVolume?`, reject stale sessions and mismatched playback.
- [x] Include `presetVolume: PresetVolume?` in `MediaPlayerService.PlaybackUpdateEvent`; use it for the current preset name, skipping, and suppression of Library’s Save button while the mix still belongs to a saved preset.
- [x] Update `presets_list_item.xml` and `PresetsFragment.kt` to show a labeled 0–100 slider only for the active playback session. Wire user-generated changes and update the percentage without replacing the row during a drag.
- [x] Add regressions in `app/src/androidTest/java/com/github/ashutoshgngwr/noice/fragment/PresetVolumeFragmentTest.kt` for the inline slider’s visibility, proportional scaling, and a drag across two activations of the same preset. Run the exact free unit-test/build commands and `scripts/verify_audio.py`; compile the instrumentation APK and run the core gesture on an isolated Android target if available.
- [x] Inspect the final diff, commit the coherent feature and tests locally, and record the commit and any verification limits.

**Checkpoint:** The core witness is the active row’s gesture and proportional live levels, not merely a successful APK build. Reassess after the red/green cycle or after 30 active minutes. Do not change the toolchain or app dependencies to unblock a device test.

**Verification:** Free Debug (JDK 11, Android SDK 30): 125 JVM tests passed, both APKs built, and all 48 bundled audio hashes matched. On an isolated Android 9 (API 28) emulator, the two Espresso tests passed and the app-level journey verified 80% → 40% → pause/resume at 40% → 0% → 100% → stop/replay at 80%. No physical speaker measurement or TalkBack session was performed.
