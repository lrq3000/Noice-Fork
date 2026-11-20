"""Check that the catalog, bundled assets, and attributed sources agree."""

import argparse
from hashlib import sha256
import json
from pathlib import Path
import re
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
CATALOG = ROOT / "app/src/main/java/com/github/ashutoshgngwr/noice/model/Sound.kt"
MANIFEST = ROOT / "docs/audio-sources.json"
LICENSES = {"CC0-1.0", "CC-BY-3.0", "CC-BY-4.0", "Public Domain", "GPL-3.0-only"}


def verify(apk: Path | None) -> None:
    expected = set(re.findall(r'"([a-z0-9_]+\.mp3)"', CATALOG.read_text(encoding="utf-8")))
    actual = {path.name for path in ASSETS.iterdir() if path.is_file()} if ASSETS.exists() else set()
    if not expected or actual != expected:
        raise ValueError(f"Asset inventory: missing {sorted(expected - actual)}; unexpected {sorted(actual - expected)}")
    sources = json.loads(MANIFEST.read_text(encoding="utf-8"))
    if set(sources) != expected:
        raise ValueError("Sound catalog and attribution manifest do not describe the same files")
    for name, source in sources.items():
        if source["license"] not in LICENSES or not all(source[key] for key in ("author", "source", "edits")):
            raise ValueError(f"Missing provenance for {name}")
        data = (ASSETS / name).read_bytes()
        if len(data) < 2_000:
            raise ValueError(f"Audio is empty or suspiciously short: {name}")
        if source["sha256"] != sha256(data).hexdigest():
            raise ValueError(f"SHA-256 mismatch for {name}")
    if apk is not None:
        with ZipFile(apk) as archive:
            packaged = {path.removeprefix("assets/") for path in archive.namelist() if re.fullmatch(r"assets/[^/]+\.mp3", path)}
            if packaged != expected:
                raise ValueError("The APK does not contain exactly the declared soundbank")
            for name, source in sources.items():
                if source["sha256"] != sha256(archive.read(f"assets/{name}")).hexdigest():
                    raise ValueError(f"APK audio differs from the source manifest: {name}")
    print(f"Verified inventory and SHA-256 for {len(expected)} bundled clips")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path)
    try:
        verify(parser.parse_args().apk)
    except (KeyError, OSError, ValueError) as error:
        parser.exit(status=1, message=f"{error}\n")
