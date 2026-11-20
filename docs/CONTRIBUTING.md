# Contributing to Noice-Fork

Noice-Fork is maintained by Stephen Karl Larroque (@LRQ3000). Use the [fork’s issue tracker](https://github.com/LRQ3000/Noice-Fork/issues) to report bugs, discuss changes, or share a preset. Include the build version and Android version in bug reports, and omit logs or presets that disclose personal information.

Keep playback offline and preserve compatibility with the existing preset keys and old V1 links. For new audio, record the author, source, license, edits, and SHA-256 in [the audio manifest](audio-sources.json). Do not copy unverified recordings from upstream history.

Build with JDK 11 and Android SDK 30: `./gradlew :app:testFreeDebugUnitTest :app:assembleFreeDebug`. Use `gradlew.bat` on Windows. Verify the bundled soundbank with `python3 scripts/verify_audio.py --apk app/build/outputs/apk/free/debug/app-free-debug.apk`. [GitHub Actions](https://github.com/LRQ3000/Noice-Fork/actions/workflows/debug-apk.yml) builds a debug APK on every push to `main`.

Keep changes focused and preserve attribution to [the original Noice contributors](CONTRIBUTORS.md). See [the preset guide](PRESETS.md) and [F-Droid packaging notes](fdroid/SUBMISSION.md) for those contribution paths.
