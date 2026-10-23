"""Synthetic failure cases for CI gates; these do not verify Android signing."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch


def load(name):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).resolve().parents[1] / f"{name}.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


reports = load("check-ci-reports")
apks = load("package-ci-apks")


class GatesTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)

    def write(self, path, content):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content, encoding="utf-8")

    def fixtures(self):
        for module in reports.TEST_MODULES:
            self.write(f"{module}/build/test-results/testDebugUnitTest/TEST-example.xml", '<testsuite tests="2"/>')
        for module in reports.LINT_MODULES:
            self.write(f"{module}/build/reports/lint-results-debug.xml", '<issues><issue severity="Warning"/></issues>')

    def test_missing_suite_fails(self):
        with self.assertRaisesRegex(ValueError, "Missing JUnit"):
            reports.inspect(self.root)

    def test_lint_error_fails_even_if_gradle_exited_zero(self):
        self.fixtures()
        self.write("app/build/reports/lint-results-debug.xml", '<issues><issue severity="Error"/></issues>')
        with self.assertRaisesRegex(ValueError, "Lint errors"):
            reports.inspect(self.root)

    def test_all_skipped_suite_fails(self):
        self.fixtures()
        self.write("app/build/test-results/testDebugUnitTest/TEST-example.xml", '<testsuite tests="2" skipped="2"/>')
        with self.assertRaisesRegex(ValueError, "Empty or failing"):
            reports.inspect(self.root)

    def test_failures_and_errors_fail(self):
        for attribute in ("errors", "failures"):
            with self.subTest(attribute=attribute):
                self.fixtures()
                self.write("app/build/test-results/testDebugUnitTest/TEST-example.xml", f'<testsuite tests="2" {attribute}="1"/>')
                with self.assertRaisesRegex(ValueError, "Empty or failing"):
                    reports.inspect(self.root)

    def test_passing_suites_with_warnings(self):
        self.fixtures()
        self.assertEqual(reports.inspect(self.root)["lint"]["app"], {"errors": 0, "warnings": 1})

    def apk_fixtures(self):
        elements = []
        for abi in sorted(apks.ABIS):
            name = f"app-{abi}-debug.apk"
            elements.append({"outputFile": name, "versionCode": 1, "versionName": "test",
                             "filters": [] if abi == "universal" else [{"filterType": "ABI", "value": abi}]})
            self.write(f"app/build/outputs/apk/debug/{name}", f"synthetic {abi}")
            self.write(f"app/build/outputs/apk/debug/{name}.signature.txt", "synthetic verification fixture")
        metadata = {"applicationId": "io.github.sevenstarr.yume.dev", "variantName": "debug", "elements": elements}
        self.write("app/build/outputs/apk/debug/output-metadata.json", json.dumps(metadata))
        return metadata

    def package(self):
        with patch.object(apks.subprocess, "check_output", return_value="synthetic-revision\n"):
            return apks.package(self.root, "debug", self.root / "packaged")

    def test_manifest_and_checksums_match_packaged_files(self):
        self.apk_fixtures()
        manifest = self.package()
        self.assertEqual(len(manifest["apks"]), 5)
        self.assertNotIn(b"\r", (self.root / "packaged/SHA256SUMS").read_bytes())
        for record in manifest["apks"]:
            with (self.root / "packaged" / record["file"]).open("rb") as stream:
                self.assertEqual(record["sha256"], apks.hashlib.file_digest(stream, "sha256").hexdigest())
        with self.assertRaises(FileExistsError):
            self.package()

    def test_missing_signature_report_fails_before_packaging(self):
        self.apk_fixtures()
        (self.root / "app/build/outputs/apk/debug/app-universal-debug.apk.signature.txt").unlink()
        with self.assertRaises(FileNotFoundError):
            self.package()
        self.assertFalse((self.root / "packaged").exists())

    def test_wrong_package_and_missing_abi_fail(self):
        for case in ("package", "abi"):
            with self.subTest(case=case):
                metadata = self.apk_fixtures()
                if case == "package":
                    metadata["applicationId"] = "upstream.example"
                else:
                    metadata["elements"].pop()
                self.write("app/build/outputs/apk/debug/output-metadata.json", json.dumps(metadata))
                with self.assertRaises(ValueError):
                    self.package()
                self.assertFalse((self.root / "packaged").exists())

    def test_release_with_tracked_changes_fails(self):
        metadata = {"applicationId": "io.github.sevenstarr.yume", "variantName": "release"}
        self.write("app/build/outputs/apk/release/output-metadata.json", json.dumps(metadata))
        with patch.object(apks.subprocess, "check_output", return_value="modified\n"):
            with self.assertRaisesRegex(ValueError, "clean tracked working tree"):
                apks.package(self.root, "release", self.root / "packaged")
        self.assertFalse((self.root / "packaged").exists())


if __name__ == "__main__":
    unittest.main()
