# Noice-Fork maintenance

- `main` is the offline Android fork of upstream Noice v1.3.3. V2 is for reference only. Do not import upstream Git history wholesale: old Git objects contain recordings that are not all cleared for redistribution.
- Keep playback independent of the network, including on the first launch. Keep v1 preset sound keys and the legacy share format compatible. The bundled soundbank and individual asset licenses are documented in `docs/audio-sources.json`; a changed recording must include provenance, license, edits, and SHA-256.
- Verify the free build with JDK 11 and Android SDK 30: `./gradlew :app:testFreeDebugUnitTest :app:assembleFreeDebug`, then `python3 scripts/verify_audio.py --apk app/build/outputs/apk/free/debug/app-free-debug.apk`.
- Keep the Actions build limited to an unsigned-for-release, debug-signed free APK on pushes to `main`. Do not introduce production signing secrets or backend requirements for core playback.
- Route public contact through the fork's GitHub issue tracker. Do not add the maintainer's email to docs, app source, store metadata, or commit identity unless a verified publishing requirement makes it necessary and the user authorizes the disclosure.
