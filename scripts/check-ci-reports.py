"""Fail CI on missing/empty JUnit suites or lint errors, including reused reports.

Yume, 2026-10-22; assisted by Codex. Python 3.11+, standard library only.
This checks reports, not whether Gradle executed or reused each task.
"""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

TEST_MODULES = ("app", "domain", "data", "source-local")
LINT_MODULES = ("app", "data", "source-local", "presentation-widget")


def inspect(root):
    results = {"tests": {}, "lint": {}}
    for module in TEST_MODULES:
        reports = sorted((root / module / "build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
        if not reports:
            raise ValueError(f"Missing JUnit reports: {module}")
        counts = dict.fromkeys(("tests", "failures", "errors", "skipped"), 0)
        for report in reports:
            suite = ET.parse(report).getroot()
            for key in counts:
                counts[key] += int(suite.get(key, "0"))
        if counts["tests"] <= counts["skipped"] or counts["failures"] or counts["errors"]:
            raise ValueError(f"Empty or failing JUnit suite: {module}: {counts}")
        results["tests"][module] = counts
    for module in LINT_MODULES:
        report = root / module / "build/reports/lint-results-debug.xml"
        issues = ET.parse(report).getroot().findall("issue")
        counts = {"errors": 0, "warnings": 0}
        for issue in issues:
            severity = issue.get("severity", "").lower()
            if severity in ("error", "fatal"):
                counts["errors"] += 1
            elif severity == "warning":
                counts["warnings"] += 1
        if counts["errors"]:
            raise ValueError(f"Lint errors: {module}: {counts}")
        results["lint"][module] = counts
    return results


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    print(json.dumps(inspect(args.root), indent=2))
