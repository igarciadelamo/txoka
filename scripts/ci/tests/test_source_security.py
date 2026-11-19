import contextlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock


SPEC = importlib.util.spec_from_file_location("source_security", Path(__file__).parents[1] / "source_security.py")
SCANNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCANNER)


class SourceSecurityTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        for name in [
            "app/src/main/java/example/Card.kt", "app/src/preview/kotlin/Preview.kt",
            "app/src/debug/java/Debug.java", "app/src/test/java/Test.kt",
            "app/src/androidTest/java/Instrumented.kt", "app/build/generated/Generated.kt",
        ]:
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("// native fixture\n")
        rules = self.root / SCANNER.RULES
        rules.parent.mkdir(parents=True, exist_ok=True)
        rules.write_text("rules: []\n")
        self.selected = [path.relative_to(self.root).as_posix() for path in SCANNER.targets(self.root)]
        self.output = self.root / "reports"

    def run_scan(self, findings=(), scanned=None, errors=(), exit_code=0, version=SCANNER.VERSION):
        def engine(command, **kwargs):
            report = {
                "version": version, "results": list(findings), "errors": list(errors),
                "paths": {"scanned": self.selected if scanned is None else scanned},
            }
            Path(command[command.index("--json-output") + 1]).write_text(json.dumps(report))
            Path(command[command.index("--sarif-output") + 1]).write_text(json.dumps({"version": "2.1.0", "runs": []}))
            self.assertNotIn("SEMGREP_APP_TOKEN", kwargs["env"])
            self.assertEqual(kwargs["env"]["SEMGREP_SEND_METRICS"], "off")
            self.assertIn("--x-ignore-semgrepignore-files", command)
            self.assertNotIn("auto", command)
            return subprocess.CompletedProcess(command, exit_code)
        with mock.patch.object(SCANNER.subprocess, "run", engine), mock.patch.dict(
            SCANNER.os.environ, {"SEMGREP_APP_TOKEN": "do-not-use"}
        ), contextlib.redirect_stdout(io.StringIO()):
            return SCANNER.scan(self.root, self.output)

    def test_runtime_kotlin_java_and_preview_selected_but_not_generated_or_tests(self):
        self.assertEqual(set(self.selected), {
            "app/src/main/java/example/Card.kt", "app/src/preview/kotlin/Preview.kt",
            "app/src/debug/java/Debug.java",
        })

    def test_findings_are_report_only_with_coverage_and_raw_reports(self):
        finding = {"check_id": "fixture", "extra": {"severity": "ERROR"}}
        self.assertEqual(self.run_scan(findings=[finding], exit_code=1), 0)
        evidence = json.loads((self.output / "source-coverage.json").read_text())
        self.assertEqual(evidence["status"], "completed")
        self.assertEqual(evidence["findings"], 1)
        self.assertEqual(len(evidence["files"]), 3)
        self.assertTrue((self.output / "Semgrep-report.sarif").exists())

    def test_engine_failure_is_not_a_clean_scan(self):
        self.assertEqual(self.run_scan(exit_code=2), 1)
        self.assertIn("INCOMPLETE SCAN", (self.output / "Semgrep-summary.md").read_text())

    def test_missing_selected_kotlin_file_fails(self):
        scanned = [path for path in self.selected if not path.endswith("Card.kt")]
        self.assertEqual(self.run_scan(scanned=scanned), 1)
        self.assertIn("Card.kt", (self.output / "Semgrep-summary.md").read_text())

    def test_new_native_file_cannot_be_silently_omitted(self):
        (self.root / "app/src/main/java/example/New.kt").write_text("class New\n")
        self.assertEqual(self.run_scan(scanned=self.selected), 1)

    def test_parser_errors_fail_even_when_exit_code_is_zero(self):
        self.assertEqual(self.run_scan(errors=[{"type": "ParseError"}]), 1)

    def test_engine_version_mismatch_fails(self):
        self.assertEqual(self.run_scan(version="0.0.0"), 1)

    def test_scanned_path_outside_checkout_fails(self):
        self.assertEqual(self.run_scan(scanned=self.selected + ["../../outside.kt"]), 1)


if __name__ == "__main__":
    unittest.main()
