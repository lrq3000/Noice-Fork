"""Build the offline library from explicitly licensed recordings, not a blind copy of the v1 assets."""

import argparse
from hashlib import sha256
import json
from pathlib import Path
import re
import subprocess
from urllib.request import Request, urlopen
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SOURCES = ROOT / "docs/audio-sources.json"
BLANKET = "https://raw.githubusercontent.com/rafaelmardojai/blanket/2eaca9ba5a1b624adf90b6ccee1008efc46f0273/data/resources/sounds/"
NOICE_SEGMENTS = "https://cdn.trynoice.com/library/segments/"


class Soundbank:
    def __init__(self, archive: Path, cache: Path, ffmpeg: str):
        self.archive = archive
        self.cache = cache
        self.ffmpeg = ffmpeg
        self.records = json.loads(SOURCES.read_text(encoding="utf-8")) if SOURCES.exists() else {}
        ASSETS.mkdir(parents=True, exist_ok=True)
        self.cache.mkdir(parents=True, exist_ok=True)

    def keep(self, name: str, source_id: int, author: str, license_id: str) -> None:
        output = ASSETS / f"{name}.mp3"
        with ZipFile(self.archive) as archive:
            output.write_bytes(archive.read(f"app/src/main/assets/{output.name}"))
        self.record(output, f"https://freesound.org/s/{source_id}/", author, license_id, "Unchanged from the Noice v1.3.3 bundle; upstream shortened/edited the licensed source.")

    def fetch(self, url: str) -> Path:
        target = self.cache / (sha256(url.encode()).hexdigest() + ".media")
        if not target.exists():
            with urlopen(Request(url, headers={"User-Agent": "Noice-Fork asset preparation/1.0 (github.com/LRQ3000/Noice-Fork)"}), timeout=25) as response:
                data = response.read(48_000_001)
            if not data or len(data) > 48_000_000:
                raise ValueError(f"Unexpected source media size for {url}")
            target.write_bytes(data)
        return target

    def replace(self, name: str, media: str, source: str, author: str, license_id: str, start: int, duration: int, cutoff: int = 0, gain_db: int = 0) -> None:
        output = ASSETS / f"{name}.mp3"
        audio = self.fetch(media)
        probe = subprocess.run([self.ffmpeg, "-hide_banner", "-i", str(audio)], capture_output=True, text=True, errors="replace")
        timing = re.search(r"Duration: (\d+):(\d+):(\d+(?:\.\d+)?)", probe.stderr)
        if timing is None:
            raise ValueError(f"Not a readable audio clip: {source}")
        hours, minutes, seconds = map(float, timing.groups())
        length = 3600 * hours + 60 * minutes + seconds
        # Some source files are short, already-edited loops. Use separate overlapping excerpts when a second full window does not fit.
        if start + duration > length:
            start = min(start, int(length * 0.4))
            duration = min(duration, round(length - start, 2))
        fade = min(0.08, duration / 4)
        filters = [f"afade=t=in:st=0:d={fade}", f"afade=t=out:st={duration - fade}:d={fade}"]
        if cutoff:
            filters.insert(0, f"lowpass=f={cutoff}")
        if gain_db:
            filters += [f"volume={gain_db}dB", "alimiter=limit=0.5:level=false"]
        subprocess.run([self.ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-ss", str(start), "-i", str(audio), "-t", str(duration), "-af", ",".join(filters), "-map_metadata", "-1", "-ar", "44100", "-c:a", "libmp3lame", "-b:a", "128k", str(output)], check=True)
        edits = f"Replacement; excerpt from {start}s for {duration}s, short edge fades and MP3 encoding."
        if cutoff:
            edits += f" Low-pass filtered at {cutoff} Hz."
        if gain_db:
            edits += f" Applied {gain_db} dB digital gain and a −6 dBFS peak limiter to compensate for quiet source recordings."
        self.record(output, source, author, license_id, edits)

    def record(self, output: Path, source: str, author: str, license_id: str, edits: str) -> None:
        data = output.read_bytes()
        if len(data) < 2_000:
            raise ValueError(f"Audio excerpt {output.name} is empty or too short")
        self.records[output.name] = {"source": source, "author": author, "license": license_id, "edits": edits, "sha256": sha256(data).hexdigest()}
        SOURCES.write_text(json.dumps(self.records, indent=2, sort_keys=True) + "\n", encoding="utf-8")

    def noice_excerpt(self, name: str, segment: str, source_id: int, author: str, start: int, duration: int, cutoff: int = 0, license_id: str = "CC-BY-3.0", gain_db: int = 0) -> None:
        """Use only a free public segment for which the Noice sound manifest names one licensed recording."""
        self.replace(name, NOICE_SEGMENTS + segment + "/128k.mp3", f"https://freesound.org/s/{source_id}/", f"{author} (Noice segment edit)", license_id, start, duration, cutoff, gain_db)

    def freesound_excerpt(self, name: str, source_id: int, uploader_id: int, author: str, license_id: str, start: int, duration: int) -> None:
        media = f"https://cdn.freesound.org/previews/{source_id // 1000}/{source_id}_{uploader_id}-hq.mp3"
        self.replace(name, media, f"https://freesound.org/s/{source_id}/", author, license_id, start, duration)

    def prepare_recipe(self) -> None:
        self.keep("brownian_noise", 171552, "georgedyer", "CC0-1.0")
        self.keep("moving_train", 170866, "YOH", "CC-BY-4.0")
        for slot in (0, 1):
            self.keep(f"water_stream_{slot}", 415151, "BurghRecords", "CC0-1.0")
        car_recording = "https://cdn.freesound.org/previews/174/174845_213393-hq.mp3"
        wind_recording = BLANKET + "wind.ogg"
        for slot, start, duration in ((0, 1, 17), (1, 20, 23)):
            self.replace(f"electric_car_{slot}", car_recording, "https://freesound.org/s/174845/", "jakobthiesen", "CC-BY-3.0", start, duration, 750, 14)
            self.replace(f"soft_wind_{slot}", wind_recording, "https://freesound.org/s/217506/", "Félix Blume (edited for Blanket by Porrumentzio)", "CC0-1.0", (0, 7)[slot], (9, 11)[slot], 1400, 5)
            self.noice_excerpt(f"wind_in_palm_trees_{slot}", "wind_through_palm_trees/wind_through_palm_trees_0", 346106, "StrangeAcoustics", start, duration, 1700, "CC-BY-4.0", 10)

    def prepare_library(self) -> None:
        self.keep("airplane_inflight", 47254, "digifishmusic", "CC-BY-4.0")
        self.keep("human_heartbeat", 216219, "RSilveira_88", "CC-BY-4.0")
        self.keep("purring_cat", 332274, "nebulousflynn", "CC-BY-3.0")
        self.keep("train_horn", 248229, "CouleurCasquette", "CC-BY-3.0")
        self.keep("birds_0", 471891, "hargissssound", "CC0-1.0")
        self.keep("creaking_ship_1", 478667, "DBlover", "CC0-1.0")
        self.keep("airplane_seatbelt_beeps", 244583, "nachosose", "CC0-1.0")
        self.keep("public_library_1", 250017, "launemax", "CC0-1.0")
        self.keep("howling_wolf", 500646, "betchkal", "CC0-1.0")
        self.keep("moderate_rain", 518863, "idomusics", "CC0-1.0")
        self.keep("thunder_crack", 213015, "Taira Komori", "CC-BY-4.0")

        for slot, start, duration in ((0, 1, 17), (1, 20, 23)):
            self.keep(f"night_{slot}", 424763, "jaime_enrique", "CC0-1.0")
            self.noice_excerpt(f"office_{slot}", "office/office", 211945, "qubodup", start, duration)
            self.keep(f"quiet_conversation_{slot}", 82171, "tfsound", "CC0-1.0")
        self.noice_excerpt("birds_1", "birds/birds_3", 120905, "Kyster", 20, 23)
        self.noice_excerpt("morning_in_a_village", "village_morning/village_morning_0", 170930, "klankbeeld", 1, 31)
        self.noice_excerpt("screeching_seagulls", "seagulls/seagulls_0", 197743, "beejeeb1314", 0, 6)
        self.noice_excerpt("rolling_thunder", "thunder/thunder_1", 529535, "straget", 0, 15)

        self.freesound_excerpt("public_library_0", 620683, 13504080, "xkeril", "CC0-1.0", 2, 29)
        self.freesound_excerpt("water_hose_0", 684884, 14079947, "DrinkingPeter", "CC0-1.0", 0, 17)
        self.freesound_excerpt("water_hose_1", 684884, 14079947, "DrinkingPeter", "CC0-1.0", 4, 13)
        self.freesound_excerpt("wind_chimes_of_shells", 329858, 1540977, "Martineerok", "CC-BY-4.0", 0, 37)

        for name, file, source, author, license_id, start, duration in (
            ("bonfire_0", "fireplace", "https://soundbible.com/1543-Fireplace.html", "ezwa", "Public Domain", 0, 17),
            ("bonfire_1", "fireplace", "https://soundbible.com/1543-Fireplace.html", "ezwa", "Public Domain", 18, 23),
            ("coffee_shop_0", "coffee-shop", "https://soundbible.com/1664-Restaurant-Ambiance.html", "stephan", "Public Domain", 0, 17),
            ("coffee_shop_1", "coffee-shop", "https://soundbible.com/1664-Restaurant-Ambiance.html", "stephan", "Public Domain", 18, 23),
            ("creaking_ship_0", "boat", "https://freesound.org/s/439365/", "Falcet (edited for Blanket by Porrumentzio)", "CC0-1.0", 0, 17),
            ("crickets", "summer-night", "https://soundbible.com/2083-Crickets-Chirping-At-Night.html", "Lisa Redfern", "Public Domain", 0, 21),
            ("seaside", "waves", "https://freesound.org/s/48412/", "Luftrum (edited for Blanket by Porrumentzio)", "CC-BY-3.0", 0, 33),
        ):
            self.replace(name, BLANKET + file + ".ogg", source, author, license_id, start, duration)

        for name, slug, source_id, author, duration in (
            ("light_rain", "2065-Rain-Inside-House", 2065, "Mark DiAngelo", 28),
            ("heavy_rain", "2217-Heavy-Rain-Wind", 2217, "Daniel Simion", 33),
            ("distant_thunder", "907-Distant-Thunder", 907, "Mike Koenig", 12),
            ("walking_through_the_snow", "660-Walking-In-Snow", 660, "Mike Koenig", 15),
        ):
            self.replace(name, f"https://soundbible.com/grab.php?id={source_id}&type=mp3", f"https://soundbible.com/{slug}.html", author, "CC-BY-3.0", 0, duration)
        for color, seed in (("white", 1709), ("pink", 2183)):
            output = ASSETS / f"{color}_noise.mp3"
            generator = f"anoisesrc=color={color}:amplitude=0.16:seed={seed}:sample_rate=44100:duration=37"
            subprocess.run([self.ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi", "-i", generator, "-c:a", "libmp3lame", "-b:a", "128k", str(output)], check=True)
            self.record(output, "scripts/prepare_audio.py", "Noice-Fork contributors", "CC0-1.0", f"New seeded synthesis: {generator}.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archive", type=Path, help="Local git archive of the exact Noice v1.3.3 tag")
    parser.add_argument("cache", type=Path, help="Temporary directory for original source downloads")
    parser.add_argument("--ffmpeg", default="ffmpeg")
    args = parser.parse_args()
    soundbank = Soundbank(args.archive, args.cache, args.ffmpeg)
    soundbank.prepare_recipe()
    soundbank.prepare_library()
