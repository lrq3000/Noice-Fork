# Noice-Fork

Noice-Fork is an independent, offline-first ambient sound mixer for Android based on [Noice v1.3.3](https://github.com/trynoice/android-app/releases/tag/1.3.3). Mix sounds at independent volume levels, save them as presets, and share the recipes. The soundbank is included in the app, so playback does not require an account, a subscription, or a network connection. [Stephen Karl Larroque (@LRQ3000)](docs/CONTRIBUTORS.md) maintains the fork.

## Why fork Noice V1?

Noice V1 could play two short samples of different lengths together. The individual loops repeat, but their starts do not usually line up again until much later. Without conspicuous sounds that mark a loop boundary, the mixture can feel like one much longer recording. That is a perceptual effect, not a claim that the samples never repeat. This technique lets a phone carry a varied sound library while using much less storage than a library of long recordings.

[Noice V2](https://trynoice.com/news/v2-announcement) chose a different design: longer, more varied recordings delivered over the Internet, with full offline playback offered as a premium feature. Streaming makes it easier to expand the library, but depends on the delivery service and can incur recurring bandwidth costs; a bundled soundbank can keep working even when the original service no longer runs. This fork narrows the scope to reliable offline playback and community presets. It does not maintain V2.

## Womb Simulator by @LRQ3000

The built-in mix follows the [original six-layer V1 recipe](https://github.com/trynoice/android-app/issues/1055). Brown noise and gentle environmental sounds give it a low, relatively steady background. The train wheels add a heartbeat-like cadence, not a literal fetal recording. A steady background may make intermittent household sounds less noticeable by masking them. These are design hypotheses, not proven mechanisms for this preset.

[Parga et al. (2018)](https://doi.org/10.1371/journal.pone.0197045) found substantial low-frequency energy in recordings collected externally from pregnant participants' abdomens. Those recordings are not direct measurements of what a fetus hears. In a [small randomized trial of 40 newborns](https://doi.org/10.1136/adc.65.1.135), more fell asleep within five minutes with white noise than without it. That short-term result does not establish that brown noise, the train rhythm, or this mix improves infant sleep or helps babies stay asleep.

**Hearing safety:** This is not a medical device and cannot measure the sound level at a child's ear. If using it near an infant, follow the [American Academy of Pediatrics' guidance](https://www.aap.org/en/patient-care/environmental-health/promoting-healthy-environments-for-children/noise/): place the device far from the infant, use the lowest practical volume and limit playback duration. The mix sliders are relative levels, not decibel measurements.

## Debug APKs

Open [Actions → Debug APK](https://github.com/LRQ3000/Noice-Fork/actions/workflows/debug-apk.yml), select a successful run on `main`, and download the `Noice-Fork-debug` artifact. Unzip the download to get the APK. This is a testing build, not a store release; different runner-generated debug keys may prevent one APK from updating another. Export your presets before uninstalling an older build.

To build locally with JDK 11 and Android SDK 30, run `./gradlew :app:testFreeDebugUnitTest :app:assembleFreeDebug` (use `gradlew.bat` on Windows). The free variant has no Google services configuration requirement.

Noice-Fork is not listed on F-Droid or Google Play. The [F-Droid submission draft](docs/fdroid/SUBMISSION.md) describes the separate, non-debug package.

## Community presets and credits

Use a preset's **Share** action to copy or send its recipe, not any sound files. Copy the entire link and use **Import preset link** in the app. [The preset guide](docs/PRESETS.md) explains the format and compatibility. Report issues or share presets in the [fork’s issue tracker](https://github.com/LRQ3000/Noice-Fork/issues), or start with the [contribution guide](docs/CONTRIBUTING.md).

Ashutosh Gangwar and the [original Noice contributors](docs/CONTRIBUTORS.md) deserve particular credit for making a small offline app sound so convincing. The inventive dual-sample engine and the care taken to choose and blend the original recordings make Noice V1 worth preserving. The software is licensed under the [GNU GPL v3](LICENSE). Individual sound clips have separate credits and licenses in [the soundbank manifest](docs/audio-sources.json); the two licenses should not be conflated.

The manifest names the creator, original source, edits, and SHA-256 for each audio file. License references: [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/), [CC BY 3.0](https://creativecommons.org/licenses/by/3.0/), and [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Replacements preserve the original preset's sound keys and slider settings, but can sound different.
