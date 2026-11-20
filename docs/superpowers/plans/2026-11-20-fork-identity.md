# Fork Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Android UI and current documentation identify Noice-Fork as a distinct maintained project and prepare its F-Droid submission.

**Architecture:** Keep playback and stored presets intact. Separate fork navigation and GitHub preset sharing from legacy incoming links and archival provenance.

**Tech Stack:** Kotlin/Android Views, Groovy Gradle on JDK 11, Markdown, Fastlane listing and fdroiddata YAML.

**Spec:** [Fork identity and F-Droid preparation](../specs/2026-11-20-fork-identity-design.md).

## Global Constraints

- Work locally in `feat/fork-identity`; no merge, push, submission, or tag.
- No maintainer email in the tree, listing, or Git commit identity. Preserve historical attribution and license citations.
- Retain the application ID, bundled assets, original playback behavior, and ability to import older Noice V1 recipes.

## Review Focus

- Old announcement shown on a fresh app start; onboarding title or feature slides promise a service this build lacks.
- Old preset links can still be imported; arbitrary GitHub links cannot be mistaken for a preset.
- About and support links are fork-owned, but historical references are not misattributed.
- The F-Droid recipe selects the non-debug free artifact and can be pinned to a published commit later.
- Private email is absent and the unsigned APK still contains the verified soundbank.

### Task 1: Fork identity and preset transport

**Files:** `MainActivity.kt`, `AppIntroActivity.kt`, `AboutFragment.kt`, `HomeFragment.kt`, support UI, `Preset.kt`, `PresetsFragment.kt`, default string resources, and associated tests under `app/src`.

**Interfaces:** `Preset.toForkUri()` emits a GitHub fragment link; `Preset.fromSharedLink()` accepts it and retains old V1 HTTPS and `noice://preset` links.

- [ ] Exercise the existing announcement and share entry points with regression checks. Confirm the fork URL test fails on the old implementation.
- [ ] Remove the original announcement and unsupported slide; set onboarding branding and make About and support routes unambiguously identify the fork.
- [ ] Emit fork links from preset sharing without changing `toUri()`'s historic format or breaking old imports. Exercise exact route and offline round trip.
- [ ] Run the free debug unit tests and commit the Android milestone.

### Task 2: Public documentation

**Files:** `README.md`, `AGENTS.md`, `docs/CONTRIBUTORS.md`, `docs/PRIVACY.md`, `docs/CONTRIBUTING.md`, `docs/CHANGELOG.md` and other active docs found in the link audit.

- [ ] Preserve substantive upstream credit, science, and license references while replacing current project pointers. Describe fork privacy, workflow artifacts, and how the preset import works. Mark historical pages as upstream history.
- [ ] Check hyperlinks/relative targets, the report contact, and absence of the maintainer email; commit the documentation milestone.

### Task 3: F-Droid draft

**Files:** `app/build.gradle`, `fastlane/metadata/android/en-US/`, `docs/fdroid/io.github.lrq3000.noicefork.yml`, `docs/fdroid/SUBMISSION.md`.

- [ ] Verify non-debug free build requirements. Prepare the metadata, release notes, and draft, using a distinct id without `.debug`, no external source downloads at build time besides eligible tooling/dependencies, and no `AuthorEmail`.
- [ ] Build the unsigned non-debug APK where supported; verify identity, permissions, and 48 sound assets. Validate draft schema/lengths and document checks pending on fdroidserver and a final immutable source reference. Commit the packaging milestone.
