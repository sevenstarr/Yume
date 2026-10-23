"""Package all five verified APKs with SHA-256 and build provenance.

Yume, 2026-10-22; assisted by Codex. Run apksigner verify --verbose
--print-certs first, saving each result as <apk>.signature.txt.
Python 3.11+, standard library only. Never reads or packages signing keys.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

ABIS = {"universal", "arm64-v8a", "armeabi-v7a", "x86", "x86_64"}


def package(root, variant, output):
    source = root / "app/build/outputs/apk" / variant
    metadata = json.loads((source / "output-metadata.json").read_text(encoding="utf-8"))
    expected_id = "io.github.sevenstarr.yume" + (".dev" if variant == "debug" else "")
    if metadata["applicationId"] != expected_id or metadata["variantName"] != variant:
        raise ValueError("Unexpected applicationId or variant")
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=no"], cwd=root, text=True).strip())
    if variant == "release" and dirty:
        raise ValueError("Release candidate requires a clean tracked working tree")
    records = []
    seen = set()
    for element in metadata["elements"]:
        filters = element["filters"]
        abi = filters[0]["value"] if len(filters) == 1 and filters[0]["filterType"] == "ABI" else "universal"
        if abi not in ABIS or abi in seen or (filters and abi == "universal"):
            raise ValueError("Unexpected or duplicate APK filters")
        seen.add(abi)
        filename = element["outputFile"]
        if Path(filename).name != filename or not filename.endswith(".apk"):
            raise ValueError("Unexpected APK filename")
        apk = source / filename
        signature = source / (filename + ".signature.txt")
        # A report is required, but apksigner's exit status in the workflow is authoritative.
        if apk.stat().st_size == 0 or not signature.read_text(encoding="utf-8").strip():
            raise ValueError("Empty APK or signature report")
        with apk.open("rb") as stream:
            digest = hashlib.file_digest(stream, "sha256").hexdigest()
        records.append({
            "file": f"Yume-{abi}-{variant}.apk",
            "source": filename,
            "abi": abi,
            "versionName": element["versionName"],
            "versionCode": element["versionCode"],
            "bytes": apk.stat().st_size,
            "sha256": digest,
        })
    if seen != ABIS:
        raise ValueError("Expected universal and four ABI APKs")
    # Refuse stale output instead of mixing artifacts from different runs.
    output.mkdir(parents=True, exist_ok=False)
    records.sort(key=lambda record: record["file"])
    for record in records:
        shutil.copyfile(source / record["source"], output / record["file"])
        shutil.copyfile(source / (record["source"] + ".signature.txt"), output / (record["file"] + ".signature.txt"))
    (output / "SHA256SUMS").write_text(
        "".join(f"{record['sha256']}  {record['file']}\n" for record in records), encoding="utf-8", newline="\n",
    )
    manifest = {"commit": revision, "trackedChanges": dirty, "variant": variant, "applicationId": expected_id, "apks": records}
    (output / "build-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n")
    return manifest


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--variant", choices=("debug", "release"), required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(package(args.root, args.variant, args.output.resolve()), indent=2))
