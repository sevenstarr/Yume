"""Query OSV for exact Maven versions declared in Gradle catalogs, excluding transitives.

Requires Python 3.11+ and network access. Sends only public coordinates/versions.
An empty advisory list is not a guarantee that an artifact is safe or indexed.
"""

import argparse
import datetime
import json
import tomllib
import urllib.request
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path("build/quality/dependencies.json"))
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    queries = []
    skipped = []
    for catalog in sorted((root / "gradle").glob("*.versions.toml")):
        doc = tomllib.loads(catalog.read_text("utf-8"))
        for alias, lib in doc.get("libraries", {}).items():
            label = f"{catalog.name}:{alias}"
            if not isinstance(lib, dict):
                skipped.append(label)
                continue
            module = lib.get("module")
            if not module and lib.get("group") and lib.get("name"):
                module = f"{lib['group']}:{lib['name']}"
            version = lib.get("version")
            if isinstance(version, dict):
                version = doc.get("versions", {}).get(version.get("ref"))
            if not isinstance(version, str) or not version or not module:
                skipped.append(label)
                continue
            query = {"package": {"ecosystem": "Maven", "name": module}, "version": version}
            if query not in queries:
                queries.append(query)
    request = urllib.request.Request(
        "https://api.osv.dev/v1/querybatch",
        data=json.dumps({"queries": queries}).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        answers = json.load(response)["results"]
    if len(answers) != len(queries):
        raise ValueError("OSV response count differs from submitted coordinates")
    packages = [
        {"name": q["package"]["name"], "version": q["version"], "advisories": a.get("vulns", [])}
        for q, a in zip(queries, answers)
    ]
    result = {
        "dateUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "source": request.full_url,
        "scope": "Exact Maven declarations in gradle/*.versions.toml only; excludes resolved "
                 "transitives, BOM-managed artifacts, native binaries and Gradle plugins",
        "queries": len(queries),
        "omittedDeclarations": skipped,
        "packages": packages,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    affected = sum(bool(p["advisories"]) for p in packages)
    print(json.dumps({"queries": len(queries), "omitted": len(skipped), "packagesWithAdvisories": affected}))
    return 1 if affected else 0


if __name__ == "__main__":
    raise SystemExit(main())
