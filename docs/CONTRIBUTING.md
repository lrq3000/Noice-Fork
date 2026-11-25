# Contributing to Noice-Fork

Noice-Fork is maintained by Stephen Karl Larroque (@LRQ3000). Use the [fork’s issue tracker](https://github.com/LRQ3000/Noice-Fork/issues) to report bugs, discuss changes, or share a preset. Include the build version and Android version in bug reports, and omit logs or presets that disclose personal information.

Keep playback offline and preserve compatibility with the existing preset keys and old V1 links. For new audio, record the author, source, license, edits, and SHA-256 in [the audio manifest](audio-sources.json). Do not copy unverified recordings from upstream history.

Build with JDK 17, Android SDK 35 and Build-Tools 35.0.0: `./gradlew :app:testFreeDebugUnitTest :app:assembleFreeDebug`. Use `gradlew.bat` on Windows. The [build instructions](../README.md#build-and-contribute) also show how to verify the APK's soundbank and Android compatibility declarations. [GitHub Actions](https://github.com/LRQ3000/Noice-Fork/actions/workflows/debug-apk.yml) builds a debug APK on every push to `main`.

For on-device instrumentation tests, use `./gradlew -PdeviceTests=true :app:connectedFreeDebugAndroidTest`. The opt-in supplies the test-only activities that FragmentScenario needs; do not distribute that APK.

Keep changes focused and preserve attribution to [the original Noice contributors](CONTRIBUTORS.md). See [the preset guide](PRESETS.md) and [F-Droid packaging notes](fdroid/SUBMISSION.md) for those contribution paths.
