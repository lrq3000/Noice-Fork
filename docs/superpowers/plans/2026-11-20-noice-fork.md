# Noice-Fork Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish an attributable, first-launch-offline Noice v1.3.3 derivative with the Womb Simulator preset and automatic debug APKs.

**Architecture:** An independent `main` starts from a sanitized source snapshot of v1.3.3. An independent local archive retains upstream history and the v2 reference branch. The existing Android playback and preset engines stay intact.

**Tech Stack:** Kotlin, Android Views, Groovy Gradle, Gradle 6.7.1/AGP 4.2.2 as the initial baseline, JDK 11, Python 3 standard library for asset validation, and GitHub Actions.

**Spec:** [`2026-11-20-noice-fork-design.md`](../specs/2026-11-20-noice-fork-design.md)

## Global Constraints

- Public source: `LRQ3000/Noice-Fork`, only `main`; local directory: `C:\git\Noice-Fork`. Exact upstream history lives in `C:\git\Noice-Fork-upstream`, never a push source.
- Preserve all sound keys/scheduling and the mix levels; ship only sources and media confirmed eligible for public redistribution. Keep original attribution.
- No account or Internet connection for playback on a fresh install. No signing secrets, backend or release publication. Existing fork and upstream stay untouched.
- Keep legacy share format; offer an in-app manual import when Android does not dispatch the web link.

## Review Focus

- Missing, unauthorized, or unreferenced audio in an APK: compare declared samples, tracked inventory, manifest hashes and archive contents.
- Preset does not appear on first launch or a layer is missing: exercise a fresh repository and check all six exact states.
- A malformed paste changes state or unexpectedly starts playback: test invalid input and assert no writes/launch; verify a valid round trip.
- Legacy Firebase/settings or permissions silently break the new package: check merged manifest and exercise a clean offline launch on a modern Android device.
- A `main` push does not produce a downloadable artifact: compare exact commit SHA in the workflow run and inspect the resulting APK.

---

### Task 1: Snapshot and audio provenance

**Files:** `app/src/main/assets/`, `docs/audio-sources.json`, `scripts/verify_audio.py`, upstream `LICENSE` and credits.

**Interfaces:** consumes `Sound.kt` sample filenames and upstream source licensing evidence; produces one manifest record per shipped asset with path, SHA-256, original/replacement status, source, author and license.

- [ ] Work from `git -C C:\git\Noice-Fork-upstream archive 1.3.3`; copy source without media pending review, upstream automation/secrets config, or `.git` metadata. Audit other bundled third-party material before bringing it into the public tree.
- [ ] Run the inventory verifier against the unpopulated snapshot. Expect failure for missing samples. Add verified original clips and attributable replacements for unclear clips; retain both slots for dual-sampled entries and keep them sonically distinct. Do not copy an unclear file even as a temporary committed intermediate.
- [ ] Run `python scripts/verify_audio.py`. Expect paths and hashes to match the full catalog with nothing missing or extra. Inspect staged media and verify no upstream commit is reachable. Commit the attributable v1 snapshot.

### Task 2: Standalone free debug build

**Files:** `build.gradle`, `app/build.gradle`, `app/src/main/res/values/strings.xml` and related labels, `.github/workflows/debug-apk.yml`.

**Interfaces:** `:app:assembleFreeDebug` produces `app/build/outputs/apk/free/debug/app-free-debug.apk`, application ID `io.github.lrq3000.noicefork.debug`, display name `Noice-Fork`. Retain the upstream namespace for source code and a separate debug application ID suffix.

- [ ] Check a baseline with a scoped JDK 11 and Android SDK 30. Capture legacy dependency or plugin failures before changing their declarations.
- [ ] Remove inherited Google build-plugin requirements for the offline build and adapt only obsolete dependencies needed by that build. Replace inherited Android automation with `Debug APK`, running on any push to `main`; configure JDK 11, SDK 30, read-only token permissions, tests, APK assembly and an artifact with failure on a missing file. Do not enable other upstream publishing workflows.
- [ ] Run `gradlew.bat :app:testFreeDebugUnitTest :app:assembleFreeDebug`. Expected: tests pass and an APK with the fork's package, label and bundled samples exists. Commit the build/branding milestone.

### Task 3: Default preset and offline import

**Files:** `app/src/main/java/com/github/ashutoshgngwr/noice/repository/PresetRepository.kt`, `model/Preset.kt` and `fragment/PresetsFragment.kt` in the same package, `app/src/main/res/layout/presets_fragment.xml`, `app/src/main/res/values/strings.xml`, `app/src/test/java/com/github/ashutoshgngwr/noice/{repository/PresetRepositoryTest,model/PresetTest}.kt`.

**Interfaces:** `PresetRepository.list()` includes the built-in on first use. `Preset.fromSharedLink(text: String): Preset` accepts v1 HTTPS/`noice://preset` formats after exact URL/payload validation and rejects invalid input before any repo write. User-confirmed import adds a `Preset` through existing storage, or names an error without data changes.

- [ ] Add and run a failing test for fresh defaults containing `Womb Simulator by @LRQ3000`, with states `{brownian_noise:25, electric_car:13, moving_train:22, soft_wind:25, water_stream:6, wind_in_palm_trees:15}` at `timePeriod = 300`. Add exact round-trip and malformed/unknown sound/invalid volume/duplicate assertions for paste parsing.
- [ ] Add the next record with a stable UUID in `DEFAULT_PRESETS`, validate paste inputs using the current player bounds and sound catalog, and wire a labeled dialog under Presets. Keep existing legacy links and share encoding.
- [ ] Run `gradlew.bat :app:testFreeDebugUnitTest :app:assembleFreeDebug`. On an isolated emulator, confirm first-launch playback without network and a copy/paste/import round trip; no production app, phone controls, or user data touched. Commit the preset/import milestone.

### Task 4: README, final check and publish

**Files:** `README.md`, existing contribution and legal references where needed.

**Interfaces:** the repo README links to `Debug APK` Actions and explains downloads, limitations, provenance, rationale, scientific hypotheses and contributors.

- [ ] Explain the shift from v1 to v2 using the v2 announcement, then cite Spencer et al. (1990), Parga et al. (2018) and AAP noise guidance for distinct, limited claims. Avoid a clinical efficacy or SPL guarantee and unsupported business claims.
- [ ] Verify the final code, asset manifest, APK and Git reachability. Obtain fresh-context whole-branch review and fix material findings before proceeding. Commit the documentation/check milestone.
- [ ] Verify `gh api user --jq .login` resolves to `lrq3000`, verify the expected destination is unoccupied, create only `gh repo create LRQ3000/Noice-Fork --public`, add `https://github.com/LRQ3000/Noice-Fork.git` as `origin`, and `git push -u origin main` from the sanitized repository. No archive refs or tags in this push. Compare `git ls-remote origin refs/heads/main` and `main`, and inspect the completed `Debug APK` run and downloadable artifact.
