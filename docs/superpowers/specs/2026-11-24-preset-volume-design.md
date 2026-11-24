# Preset volume design

The Presets screen should change the level of a playing mix without changing its balance or requiring a visit to the Library. The active saved preset row shows a labeled horizontal slider and its current percentage. Pausing and resuming keeps this level; stopping and starting a preset again restores its saved default. A deliberately all-silent preset displays 0% with no adjustable gain.

The starting percentage equals the highest saved sound setting. On a change, each sound uses `saved sound level × selected percentage ÷ starting percentage`. For example, Beach starts at 80%; at 40% each sound uses half its saved level, and at 100% the loudest sound reaches its maximum without clipping any individual setting. Dragging to 0% mutes without stopping, and dragging back restores the balance. Percentages refer to the app’s volume control scale, not a measure of perceived loudness.

**Approaches.** Use a playback-only preset volume control keyed to the active saved preset, and apply fractional levels through the existing player volume curve. This preserves quiet sounds and avoids rounding drift as users drag. The alternatives were to repeatedly rewrite the integer player settings, which loses precision, or to change the Android device volume, which affects other audio and does not implement the requested behavior.

Changing individual sounds or repeat periods still makes the result a custom mix. The current scaled levels are retained when this happens, and the existing save action can capture the custom mix using the legacy integer format. The temporary preset volume never writes to a saved recipe or share link. Playback and the bundled soundbank remain offline and unchanged.

**Journey to verify:** Open Presets → play Beach at 80% → drag to 40%, 0%, and 100% without deactivating the row → stop → play Beach again at 80%. Also verify that an individual edit leaves the untouched sounds at their audible levels and that an old slider cannot control a new preset.
