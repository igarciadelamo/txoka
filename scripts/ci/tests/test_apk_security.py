"""Check APK scan correlation, report completeness and cleanup without Docker."""

import contextlib
import hashlib
from http.client import IncompleteRead, RemoteDisconnected
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock
import zipfile


SCRIPT = Path(__file__).resolve().parents[1] / "apk_security.py"
SPEC = importlib.util.spec_from_file_location("apk_security", SCRIPT)
SCANNER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SCANNER)


class APKSecurityTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.apk = self.root / "preview.apk"
        with zipfile.ZipFile(self.apk, "w") as archive:
            archive.writestr("AndroidManifest.xml", b"manifest" * 200)
            archive.writestr("classes.dex", b"dex" * 1000)
        self.md5 = hashlib.md5(self.apk.read_bytes(), usedforsecurity=False).hexdigest()
        self.sha256 = hashlib.sha256(self.apk.read_bytes()).hexdigest()
        self.output = self.root / "reports"
        self.commands = []
        self.client = mock.Mock()
        self.client.upload.return_value = {"hash": self.md5, "scan_type": "apk"}
        self.report = {
            "md5": self.md5, "sha256": self.sha256,
            "package_name": SCANNER.EXPECTED_PACKAGE,
            "appsec": {
                "high": [{"title": "example"}], "warning": [], "info": [],
                "secure": [], "hotspot": [],
            },
        }
        self.client.json.side_effect = lambda endpoint, *args, **kwargs: (
            self.report if endpoint in {"/api/v1/scan", "/api/v1/report_json"} else {"content": []}
        )
        self.client.request.return_value = b"%PDF-1.7\n" + b"body" * 300 + b"\n%%EOF\n"
        environment = mock.patch.dict(os.environ, {
            "GITHUB_SHA": "a" * 40,
            "SOURCE_SHA": "f" * 40,
            "GITHUB_STEP_SUMMARY": str(self.root / "summary.md"),
        }, clear=True)
        environment.start()
        self.addCleanup(environment.stop)

    def command(self, *args, **kwargs):
        self.commands.append((args, kwargs))
        if args[:3] == ("docker", "image", "inspect"):
            return "sha256:" + "b" * 64
        if args[:2] == ("docker", "port"):
            return "127.0.0.1:32768"
        return "engine diagnostics"

    def run_scan(self):
        with mock.patch.object(SCANNER, "command", side_effect=self.command), mock.patch.object(
            SCANNER, "MobSF", return_value=self.client
        ) as constructor, mock.patch.object(SCANNER, "wait_ready"), contextlib.redirect_stdout(io.StringIO()):
            SCANNER.scan(self.apk, self.output)
            return constructor

    def status(self):
        return json.loads((self.output / "MobSF-status.json").read_text())

    def test_findings_are_report_only_and_match_both_input_checksums(self):
        constructor = self.run_scan()
        metadata = self.status()
        self.assertEqual(metadata["status"], "completed")
        self.assertEqual(metadata["findings"]["high"], 1)
        self.assertEqual(metadata["apk_sha256"], self.sha256)
        self.assertEqual(metadata["apk_md5"], self.md5)
        self.assertEqual(metadata["source_sha"], "f" * 40)
        self.assertEqual(metadata["container_cleanup"], "completed")
        self.assertFalse(metadata["third_party_apk_uploads"])
        self.assertIn("do not block signing", (self.output / "MobSF-summary.md").read_text())
        self.assertEqual(constructor.call_args.args[0], "http://127.0.0.1:32768")
        key = constructor.call_args.args[1]
        run_args, run_options = next(call for call in self.commands if call[0][:2] == ("docker", "run"))
        self.assertNotIn(key, " ".join(run_args))
        self.assertEqual(run_options["env"]["MOBSF_API_KEY"], key)
        self.assertIn("127.0.0.1::8000", run_args)
        self.assertIn("MOBSF_VT_ENABLED=", run_args)
        self.assertIn("MOBSF_VT_UPLOAD=", run_args)
        self.assertIn("--cap-drop", run_args)
        self.assertTrue(any(args[:2] == ("docker", "rm") for args, _ in self.commands))

    def test_image_pull_and_run_use_the_pinned_digest(self):
        self.run_scan()
        self.assertRegex(SCANNER.IMAGE, r":v4\.5\.3@sha256:[0-9a-f]{64}$")
        for prefix in [("docker", "pull"), ("docker", "run")]:
            args, _ = next(call for call in self.commands if call[0][:2] == prefix)
            self.assertIn(SCANNER.IMAGE, args)

    def test_mismatched_md5_or_sha256_report_fails_and_removes_container(self):
        for field, incorrect in [("md5", "c" * 32), ("sha256", "d" * 64)]:
            with self.subTest(field=field):
                saved = self.report[field]
                self.report[field] = incorrect
                with self.assertRaisesRegex(RuntimeError, "does not match"):
                    self.run_scan()
                self.assertEqual(self.status()["status"], "failed")
                self.assertEqual(self.status()["container_cleanup"], "completed")
                self.report[field] = saved

    def test_production_package_cannot_be_presented_as_a_preview_scan(self):
        self.report["package_name"] = "es.igarciadelamo.txoka"
        with self.assertRaisesRegex(RuntimeError, "preview application"):
            self.run_scan()
        self.assertEqual(self.status()["status"], "failed")

    def test_fresh_container_uses_initial_scan_without_update_only_rescan(self):
        self.run_scan()
        scans = [call for call in self.client.json.call_args_list if call.args[0] == "/api/v1/scan"]
        self.assertEqual(len(scans), 1)
        self.assertEqual(scans[0].args[1], {"hash": self.md5})
        self.assertNotIn("re_scan", scans[0].args[1])

    def test_upload_hash_or_type_mismatch_stops_before_analysis(self):
        for value in [{"hash": "d" * 32, "scan_type": "apk"}, {"hash": self.md5, "scan_type": "ipa"}]:
            with self.subTest(value=value):
                self.client.upload.return_value = value
                with self.assertRaisesRegex(RuntimeError, "upload hash or scan type"):
                    self.run_scan()
                self.client.json.assert_not_called()
                self.assertEqual(self.status()["status"], "failed")

    def test_incomplete_findings_are_an_engine_failure(self):
        self.report["appsec"] = {"high": []}
        with self.assertRaisesRegex(RuntimeError, "incomplete"):
            self.run_scan()
        self.assertEqual(self.status()["status"], "failed")

    def test_json_report_is_validated_independently_of_initial_scan(self):
        wrong_report = {**self.report, "sha256": "e" * 64}
        self.client.json.side_effect = lambda endpoint, *args, **kwargs: (
            wrong_report if endpoint == "/api/v1/report_json" else self.report
        )
        with self.assertRaisesRegex(RuntimeError, "does not match"):
            self.run_scan()
        self.assertEqual(self.status()["status"], "failed")

    def test_invalid_or_truncated_pdf_is_an_engine_failure(self):
        for payload in [b'{"error":"PDF unavailable"}', b"%PDF-1.7\nshort\n%%EOF", b"%PDF-1.7\n" + b"x" * 2000]:
            with self.subTest(payload=payload[:20]):
                self.client.request.return_value = payload
                with self.assertRaisesRegex(RuntimeError, "complete PDF"):
                    self.run_scan()
                self.assertEqual(self.status()["status"], "failed")

    def test_non_loopback_api_addresses_are_rejected(self):
        for url in ["http://example.com:8000", "http://0.0.0.0:8000", "https://127.0.0.1:8000", "http://127.0.0.1:8000/evil"]:
            with self.subTest(url=url), self.assertRaisesRegex(RuntimeError, "loopback"):
                SCANNER.MobSF(url, "ephemeral-key")

    def test_redirects_cannot_forward_the_api_key_or_apk_to_an_external_host(self):
        handler = SCANNER.NoRedirects()
        self.assertIsNone(handler.redirect_request(None, None, 302, "redirect", {}, "https://example.com"))

    def test_oversized_api_response_is_an_engine_failure(self):
        client = SCANNER.MobSF("http://127.0.0.1:8000", "ephemeral-key")
        response = mock.MagicMock()
        response.__enter__.return_value.read.return_value = b"12345"
        with mock.patch.object(SCANNER, "MAX_RESPONSE", 4), mock.patch.object(
            client.opener, "open", return_value=response
        ):
            with self.assertRaisesRegex(RuntimeError, "size limit"):
                client.request("/api/v1/report_json")

    def test_engine_errors_redact_environment_credentials(self):
        result = subprocess.CompletedProcess(["docker", "run"], 1, "", "api-secret github-secret")
        with mock.patch.object(SCANNER.subprocess, "run", return_value=result):
            with self.assertRaisesRegex(RuntimeError, "Engine command failed") as raised:
                SCANNER.command("docker", "run", env={
                    "MOBSF_API_KEY": "api-secret", "GITHUB_TOKEN": "github-secret",
                })
        self.assertNotIn("api-secret", str(raised.exception))
        self.assertNotIn("github-secret", str(raised.exception))

    def test_json_api_errors_are_not_clean_reports(self):
        client = SCANNER.MobSF("http://127.0.0.1:8000", "ephemeral-key")
        for payload in [b'{"error":"analysis failed"}', b"invalid JSON", b"[]"]:
            with self.subTest(payload=payload), mock.patch.object(client, "request", return_value=payload):
                with self.assertRaises(RuntimeError):
                    client.json("/api/v1/scan", {"hash": self.md5})

    def test_readiness_retries_transient_disconnects(self):
        client = SCANNER.MobSF("http://127.0.0.1:8000", "ephemeral-key")
        response = mock.MagicMock()
        response.__enter__.return_value.read.return_value = b'{"content":[]}'
        client.opener = mock.Mock()
        client.opener.open.side_effect = [ConnectionResetError(104, "reset"), RemoteDisconnected("closed"), response]
        with mock.patch.object(SCANNER.time, "sleep") as sleep, contextlib.redirect_stdout(io.StringIO()) as output:
            SCANNER.wait_ready(client)
        self.assertEqual(client.opener.open.call_count, 3)
        self.assertEqual(sleep.call_count, 2)
        self.assertNotIn("ephemeral-key", output.getvalue())

    def test_analysis_network_failure_does_not_retry_into_a_clean_result(self):
        client = SCANNER.MobSF("http://127.0.0.1:8000", "ephemeral-key")
        for failure in [ConnectionResetError(104, "reset"), IncompleteRead(b"partial")]:
            with self.subTest(failure=type(failure).__name__), mock.patch.object(
                client.opener, "open", side_effect=failure
            ) as opener, mock.patch.object(SCANNER.time, "sleep") as sleep:
                with self.assertRaisesRegex(RuntimeError, "API connection failed"):
                    client.json("/api/v1/scan", {"hash": self.md5})
                opener.assert_called_once()
                sleep.assert_not_called()

    def test_readiness_timeout_reports_last_failure(self):
        client = mock.Mock()
        client.json.side_effect = RuntimeError("MobSF /api/v1/scans: HTTP 503")
        clock = [0]
        with mock.patch.object(SCANNER.time, "monotonic", side_effect=lambda: clock[0]), mock.patch.object(
            SCANNER.time, "sleep", side_effect=lambda seconds: clock.__setitem__(0, clock[0] + seconds)
        ), contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaisesRegex(RuntimeError, "within 6 seconds; last error: .*HTTP 503"):
                SCANNER.wait_ready(client, deadline_seconds=6)
        self.assertEqual(client.json.call_count, 2)

    def test_cleanup_failure_is_not_reported_as_success(self):
        original = self.command
        def failing_cleanup(*args, **kwargs):
            if args[:2] == ("docker", "rm"):
                raise RuntimeError("cannot remove")
            return original(*args, **kwargs)
        with mock.patch.object(self, "command", side_effect=failing_cleanup):
            with self.assertRaisesRegex(RuntimeError, "Could not remove"):
                self.run_scan()
        self.assertEqual(self.status()["status"], "failed")
        self.assertEqual(self.status()["container_cleanup"], "failed")

    def test_engine_error_keeps_a_failed_status_and_cleans_up(self):
        self.client.json.side_effect = RuntimeError("analysis failed")
        with self.assertRaisesRegex(RuntimeError, "analysis failed"):
            self.run_scan()
        self.assertEqual(self.status()["status"], "failed")
        self.assertTrue(any(args[:2] == ("docker", "rm") for args, _ in self.commands))

    def test_input_must_be_a_bounded_apk_with_manifest(self):
        for payload in [b"", b"PK" + b"not a zip" * 300]:
            self.apk.write_bytes(payload)
            with self.assertRaises(RuntimeError), mock.patch.object(SCANNER, "command") as engine:
                SCANNER.scan(self.apk, self.output)
            engine.assert_not_called()


if __name__ == "__main__":
    unittest.main()
