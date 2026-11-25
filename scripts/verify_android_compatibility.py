"""Verify the free APK's SDK floor, target and media/clock declarations."""

import argparse
import re
import subprocess
from pathlib import Path


def verify(apk: Path, aapt2: Path) -> list[str]:
    badging = subprocess.check_output([aapt2, "dump", "badging", apk], encoding="utf-8")
    manifest = subprocess.check_output(
        [aapt2, "dump", "xmltree", "--file", "AndroidManifest.xml", apk], encoding="utf-8"
    )
    expected = {
        "keep Android 5 support": r"^minSdkVersion:'21'$",
        "declare special access for the wake-up alarm": r"^uses-permission: name='android\.permission\.SCHEDULE_EXACT_ALARM'$",
        "allow the playback foreground service": r"^uses-permission: name='android\.permission\.FOREGROUND_SERVICE_MEDIA_PLAYBACK'$",
    }
    failures = [description for description, pattern in expected.items()
                if not re.search(pattern, badging, re.MULTILINE)]
    target = re.search(r"^targetSdkVersion:'(\d+)'$", badging, re.MULTILINE)
    if not target or int(target[1]) < 35:
        failures.append("target at least API 35")
    if re.search(r"^uses-permission[^:]*: name='android\.permission\.INTERNET'", badging, re.MULTILINE):
        failures.append("do not request Internet access in the offline build")
    if re.search(r"androidx\.(?:test\.core\.app\.InstrumentationActivityInvoker|fragment\.app\.testing)", manifest):
        failures.append("keep instrumentation-only activities out of the distributed APK")

    for service in re.split(r"\n\s*E: ", manifest):
        if service.startswith("service ") and 'noice.MediaPlayerService"' in service:
            declared_type = re.search(r":foregroundServiceType\([^)]*\)=(0x[0-9a-fA-F]+|\d+)", service)
            if declared_type and int(declared_type[1], 0) & 0x2:  # FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                break
    else:
        failures.append("declare mediaPlayback on MediaPlayerService")
    return failures


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--aapt2", type=Path, required=True)
    args = parser.parse_args()
    failures = verify(args.apk, args.aapt2)
    if failures:
        parser.exit(1, "Free APK requirements not met:\n" + "".join(f"- {item}\n" for item in failures))
    print("Free APK: minimum API 21, target API 35+, offline, media foreground service and exact alarms declared")


if __name__ == "__main__":
    main()
