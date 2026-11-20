# Noice-Fork design

**Intent:** Preserve the last verified bundled Noice release, v1.3.3 (`b50c5148cfe4f525bf1a3aaad823a7d5751d2482`), as a maintainable local-first Android app with community presets and builds from `main`.

**Approach:** A byte-for-byte fork would keep the original recordings but would also republish blobs whose reuse rights could not be established. The approved approach is an attributable v1.3.3 source snapshot in a standalone public repository, with independently cleared or original replacement audio. An exact upstream clone with `archive/upstream-v2` stays local and has no maintenance obligations. The existing GitHub fork is not changed.

**User journey:** Install the fork alongside upstream Noice, open Presets and play `Womb Simulator by @LRQ3000` without a network on the first launch. Share the existing v1 preset link; on devices that route it to a browser, users can paste the link into the app instead. Pasting is explicit, validates the mix and does not overwrite other presets or start playback automatically.

**Audio:** Keep the scheduling, dual-sampling, original sound keys, and the six-layer v1 recipe unchanged. Record license/source and hash for every shipped clip. Do not include blobs with unclear rights in any public commit or artifact. A mismatched or unavailable replacement for a preset layer must be disclosed and addressed before publication.

**Distribution:** Build the FOSS `free` debug APK on every push to `main` and upload it as an Actions artifact. No Google service account, release signing, streaming service, new hosting, force-push, or signed release. Explain debug signing differences between runs. Keep LGPL/GPL and other upstream notices and record separately licensed media.

**README:** Credit Noice and the audio sources. Explain v1 dual sampling as a way for two short parallel loops to have a longer joint repetition period, and v2 streaming as a different design with ongoing delivery costs and premium offline playback, without attributing project inactivity to an unproven cause. Link the preset discussion, distinguish spectral/rhythm/masking hypotheses from evidence, and explain safe low-volume, distant, time-limited use with AAP guidance. Welcome community presets and maintenance contributions.

**Verification:** Check asset provenance and archive isolation; build and test the free variant; check a fresh offline device journey including import/share; verify the GitHub Actions run/artifact after publishing only `main` to `LRQ3000/Noice-Fork`.
