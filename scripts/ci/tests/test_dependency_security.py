import contextlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock


SPEC = importlib.util.spec_from_file_location("dependency_security", Path(__file__).parents[1] / "dependency_security.py")
SCANNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCANNER)


class DependencySecurityTests(unittest.TestCase):
    COMPONENT = {"type": "library", "group": "example", "name": "library", "version": "1.2.3", "purl": "pkg:maven/example/library@1.2.3"}

    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.sbom = self.root / "runtime-sbom.cdx.json"
        self.output = self.root / "reports"

    def write_sbom(self, components=None, configuration="previewRuntimeClasspath"):
        self.sbom.write_text(json.dumps({
            "bomFormat": "CycloneDX", "specVersion": "1.6",
            "metadata": {"properties": [{"name": "txoka:configuration", "value": configuration}]},
            "components": [self.COMPONENT] if components is None else components,
        }))

    def run_scan(self, report=None, exit_code=0):
        if report is None:
            report = {"results": [{"packages": [{"package": {"name": "example:library", "version": "1.2.3", "ecosystem": "Maven"}}]}]}
        result = subprocess.CompletedProcess([], exit_code, json.dumps(report), "scanner diagnostics")
        with mock.patch.object(SCANNER, "download_scanner", return_value=Path("/fake/osv")), mock.patch.object(
            SCANNER.subprocess, "run", return_value=result
        ), mock.patch.dict("os.environ", {}, clear=True), contextlib.redirect_stdout(io.StringIO()):
            return SCANNER.scan(self.sbom, self.output)

    def test_empty_inventory_fails_instead_of_claiming_no_vulnerabilities(self):
        self.write_sbom([])
        with mock.patch.object(SCANNER, "download_scanner") as download:
            with self.assertRaises(ValueError):
                self.run_scan()
        download.assert_not_called()

    def test_findings_are_saved_report_only(self):
        self.write_sbom()
        data = {"results": [{"packages": [{"package": {"name": "example:library", "version": "1.2.3", "ecosystem": "Maven"}, "vulnerabilities": [{"id": "GHSA-fixture"}]}]}]}
        report = self.run_scan(data, 1)
        self.assertEqual(SCANNER.vulnerability_ids(report), ["GHSA-fixture"])
        self.assertEqual(report["scan_metadata"]["status"], "completed")
        self.assertTrue((self.output / "OSV-report.json").exists())

    def test_engine_error_cannot_be_reported_as_clean(self):
        self.write_sbom()
        with self.assertRaises(ValueError):
            self.run_scan(exit_code=129)
        self.assertFalse((self.output / "OSV-report.json").exists())

    def test_malformed_coordinates_are_rejected_before_download(self):
        self.write_sbom([{"name": "unresolved"}])
        with mock.patch.object(SCANNER, "download_scanner") as download:
            with self.assertRaises(ValueError):
                SCANNER.inventory(self.sbom)
        download.assert_not_called()

    def test_wrong_configuration_fails(self):
        self.write_sbom(configuration="debugRuntimeClasspath")
        with self.assertRaises(ValueError):
            SCANNER.inventory(self.sbom)

    def test_missing_osv_package_fails(self):
        self.write_sbom()
        with self.assertRaises(ValueError):
            self.run_scan({"results": []})

    def test_duplicate_components_fail(self):
        self.write_sbom([self.COMPONENT, self.COMPONENT])
        with self.assertRaises(ValueError):
            SCANNER.inventory(self.sbom)

    def test_repeated_package_cannot_hide_an_omitted_component(self):
        another = dict(self.COMPONENT, name="another", purl="pkg:maven/example/another@1.2.3")
        self.write_sbom([self.COMPONENT, another])
        package = {"package": {"name": "example:library", "version": "1.2.3", "ecosystem": "Maven"}}
        with self.assertRaises(ValueError):
            self.run_scan({"results": [{"packages": [package, package]}]})

    def test_equal_package_count_with_wrong_identity_fails(self):
        self.write_sbom()
        package = {"package": {"name": "example:unrelated", "version": "1.2.3", "ecosystem": "Maven"}}
        with self.assertRaises(ValueError):
            self.run_scan({"results": [{"packages": [package]}]})

    def test_wrong_ecosystem_is_not_counted_as_scanned(self):
        self.write_sbom()
        package = {"package": {"name": "example:library", "version": "1.2.3", "ecosystem": "npm"}}
        with self.assertRaises(ValueError):
            self.run_scan({"results": [{"packages": [package]}]})

    def test_purl_must_match_declared_coordinates(self):
        self.write_sbom([dict(self.COMPONENT, purl="pkg:maven/example/unrelated@1.2.3")])
        with self.assertRaises(ValueError):
            SCANNER.inventory(self.sbom)

    def test_encoded_purl_is_compared_using_decoded_coordinates(self):
        self.write_sbom([dict(self.COMPONENT, version="1.2.3+fixed", purl="pkg:maven/example/library@1.2.3%2Bfixed")])
        self.assertEqual(SCANNER.inventory(self.sbom)[0]["version"], "1.2.3+fixed")

    def test_wrong_binary_digest_is_never_executed(self):
        with mock.patch.object(SCANNER.urllib.request, "urlopen", return_value=io.BytesIO(b"unexpected binary")):
            with self.assertRaises(ValueError):
                SCANNER.download_scanner(self.root)


if __name__ == "__main__":
    unittest.main()
