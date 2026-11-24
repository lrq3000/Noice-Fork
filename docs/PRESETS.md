# Share presets in Noice-Fork

Open **Presets**, select a preset’s menu, and tap **Share**. Share the complete link, including everything after `#`. In Noice-Fork links, that fragment carries the preset name and sound settings, not recordings. Browsers send the page address to GitHub, but not the fragment.

To import, copy the original link and open **Presets → Import preset link** in Noice-Fork. Paste it and confirm. Import works offline and does not start playback or replace your saved presets. Old Noice V1 links still work; paste them into the app rather than opening them in a browser, since V1 web links carry the recipe in the query string. Noice V2 links use a different format.

To contribute a community preset, [open an issue](https://github.com/LRQ3000/Noice-Fork/issues) with the link and a short description of the mix.

## Adjust preset volume

Play a preset and drag the **Preset volume** slider on its row. It starts at the loudest saved sound’s level and changes all the sounds together without changing their balance. Drag to 0% to mute the mix without stopping it. The adjustment is temporary; pressing Stop and playing the preset again restores its saved levels.

If you adjust an individual sound in the Library, it becomes a custom mix that you can save as a new preset. The preset slider is independent of your device’s media volume.

## Womb Simulator

The built-in mix follows [@LRQ3000’s original six-layer Noice V1 recipe](https://github.com/trynoice/android-app/issues/1055). Brown noise and gentle environmental sounds give it a low, relatively steady background. The train wheels add a heartbeat-like cadence. It is not a recording from inside the womb. A steady background may make intermittent sounds less noticeable by masking them; this is a design rationale, not a demonstrated benefit of the preset.

[Parga et al. (2018)](https://doi.org/10.1371/journal.pone.0197045) found substantial low-frequency energy in recordings collected externally from pregnant participants’ abdomens, not direct measurements of what a fetus hears. In a [small randomized trial of 40 newborns](https://doi.org/10.1136/adc.65.1.135), more fell asleep within five minutes with white noise than without it. That short-term result does not establish that brown noise, the train rhythm, or this mix improves infant sleep or helps babies stay asleep.

**Hearing safety:** This is not a medical device and cannot measure sound levels at a child’s ear. If using it near an infant, follow the [American Academy of Pediatrics’ guidance](https://www.aap.org/en/patient-care/environmental-health/promoting-healthy-environments-for-children/noise/): place the device far from the infant, use the lowest practical volume, and limit playback duration. The mix sliders are relative levels, not decibel measurements.
