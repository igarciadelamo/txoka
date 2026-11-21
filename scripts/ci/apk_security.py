"""Scan a preview APK with a local, disposable and digest-pinned MobSF container."""

import argparse
import hashlib
from http.client import HTTPException
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener
import zipfile


IMAGE = (
    "opensecurity/mobile-security-framework-mobsf:v4.5.3@"
    "sha256:5113fbad0727445dda01afdccafea5e107bfa8632ead8a169492f5caa8aa1b7c"
)
EXPECTED_PACKAGE = "es.igarciadelamo.txoka.preview"
MAX_RESPONSE = 128 * 1024 * 1024
MAX_APK = 128 * 1024 * 1024
SEVERITIES = ("high", "warning", "info", "secure", "hotspot")


class NoRedirects(HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        return None


def command(*args, timeout=60, env=None, check=True, include_stderr=False):
    result = subprocess.run(
        list(args), capture_output=True, text=True, timeout=timeout, env=env,
    )
    if check and result.returncode:
        detail = result.stderr.strip()[-2000:]
        for name in ("MOBSF_API_KEY", "DJANGO_SUPERUSER_PASSWORD", "GH_TOKEN", "GITHUB_TOKEN"):
            secret = (env or os.environ).get(name)
            if secret:
                detail = detail.replace(secret, "[redacted]")
        raise RuntimeError(f"Engine command failed: {args[0]} {args[1]}: {detail}")
    return (result.stdout + (result.stderr if include_stderr else "")).strip()


class MobSF:
    def __init__(self, base_url, key):
        parsed = urlsplit(base_url)
        if (
            parsed.scheme != "http" or parsed.hostname != "127.0.0.1"
            or not parsed.port or parsed.path or parsed.username or parsed.password
            or parsed.query or parsed.fragment
        ):
            raise RuntimeError("MobSF API must use this runner's loopback interface")
        self.base_url = base_url
        self.key = key
        self.opener = build_opener(ProxyHandler({}), NoRedirects())

    def request(self, endpoint, data=None, content_type=None, timeout=120):
        headers = {"X-Mobsf-Api-Key": self.key}
        if content_type:
            headers["Content-Type"] = content_type
        request = Request(self.base_url + endpoint, data=data, headers=headers)
        try:
            with self.opener.open(request, timeout=timeout) as response:
                body = response.read(MAX_RESPONSE + 1)
        except HTTPError as error:
            raise RuntimeError(f"MobSF {endpoint}: HTTP {error.code}") from error
        except (URLError, OSError, HTTPException) as error:
            raise RuntimeError(
                f"MobSF {endpoint}: API connection failed ({type(error).__name__})"
            ) from error
        if len(body) > MAX_RESPONSE:
            raise RuntimeError(f"MobSF {endpoint}: response exceeds size limit")
        return body

    def json(self, endpoint, fields=None, timeout=120):
        data = None if fields is None else urlencode(fields).encode()
        body = self.request(
            endpoint, data,
            "application/x-www-form-urlencoded" if data is not None else None,
            timeout,
        )
        try:
            value = json.loads(body)
        except (ValueError, UnicodeError) as error:
            raise RuntimeError(f"MobSF {endpoint}: invalid JSON response") from error
        if not isinstance(value, dict) or value.get("error"):
            raise RuntimeError(f"MobSF {endpoint}: analysis/API error")
        return value

    def upload(self, apk):
        boundary = "mobsf-" + secrets.token_hex(16)
        body = (
            f"--{boundary}\r\n"
            'Content-Disposition: form-data; name="file"; filename="txoka-preview.apk"\r\n'
            "Content-Type: application/vnd.android.package-archive\r\n\r\n"
        ).encode() + apk + f"\r\n--{boundary}--\r\n".encode()
        response = self.request(
            "/api/v1/upload", body, f"multipart/form-data; boundary={boundary}",
        )
        try:
            value = json.loads(response)
        except (ValueError, UnicodeError) as error:
            raise RuntimeError("MobSF upload returned invalid JSON") from error
        if not isinstance(value, dict) or value.get("error"):
            raise RuntimeError("MobSF upload failed")
        return value


def wait_ready(client, deadline_seconds=240):
    started = time.monotonic()
    deadline = started + deadline_seconds
    next_notice = 0
    last_error = "No response received"
    while time.monotonic() < deadline:
        try:
            client.json("/api/v1/scans", timeout=5)
            print(f"MobSF API is ready after {time.monotonic() - started:.0f}s.", flush=True)
            return
        except RuntimeError as error:
            last_error = str(error)
            elapsed = time.monotonic() - started
            if elapsed >= next_notice:
                print(f"MobSF API is starting ({elapsed:.0f}s): {last_error}", flush=True)
                next_notice = elapsed + 15
            remaining = deadline - time.monotonic()
            if remaining > 0:
                time.sleep(min(3, remaining))
    raise RuntimeError(
        f"MobSF did not become ready within {deadline_seconds} seconds; last error: {last_error}"
    )


def validate_report(report, apk_md5, apk_sha256):
    if report.get("md5") != apk_md5 or report.get("sha256") != apk_sha256:
        raise RuntimeError("MobSF report does not match the uploaded APK")
    if report.get("package_name") != EXPECTED_PACKAGE:
        raise RuntimeError("MobSF report does not name the preview application")
    appsec = report.get("appsec")
    if not isinstance(appsec, dict) or not all(
        isinstance(appsec.get(severity), list) for severity in SEVERITIES
    ):
        raise RuntimeError("MobSF report is incomplete: AppSec findings are missing")
    return {severity: len(appsec[severity]) for severity in SEVERITIES}


def write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def scan(apk_path, output):
    if not 1000 <= apk_path.stat().st_size <= MAX_APK:
        raise RuntimeError("The APK is empty or exceeds the scan size limit")
    if not zipfile.is_zipfile(apk_path):
        raise RuntimeError("The input is not an APK archive")
    with zipfile.ZipFile(apk_path) as archive:
        if "AndroidManifest.xml" not in archive.namelist():
            raise RuntimeError("The APK is missing AndroidManifest.xml")
    apk = apk_path.read_bytes()
    output.mkdir(parents=True, exist_ok=True)
    apk_md5 = hashlib.md5(apk, usedforsecurity=False).hexdigest()
    apk_sha256 = hashlib.sha256(apk).hexdigest()
    metadata = {
        "engine": "MobSF", "image": IMAGE,
        "apk_sha256": apk_sha256, "apk_md5": apk_md5,
        "source_sha": os.environ.get("SOURCE_SHA", os.environ.get("GITHUB_SHA")),
        "mode": "static-report-only", "status": "running",
        "third_party_apk_uploads": False,
    }
    key = secrets.token_hex(32)
    container = "txoka-mobsf-" + secrets.token_hex(8)
    environment = {**os.environ, "MOBSF_API_KEY": key, "DJANGO_SUPERUSER_PASSWORD": key}
    container_started = False
    try:
        print("Pulling the digest-pinned MobSF 4.5.3 image...", flush=True)
        command("docker", "pull", IMAGE, timeout=480)
        metadata["image_id"] = command("docker", "image", "inspect", "--format", "{{.Id}}", IMAGE)
        print("Starting a local, disposable MobSF container...", flush=True)
        command(
            "docker", "run", "--detach", "--name", container,
            "--publish", "127.0.0.1::8000", "--cap-drop", "ALL",
            "--security-opt", "no-new-privileges", "--memory", "5g", "--cpus", "2",
            "--pids-limit", "512", "--env", "MOBSF_API_KEY",
            "--env", "DJANGO_SUPERUSER_PASSWORD", "--env", "MOBSF_ASYNC_ANALYSIS=0",
            "--env", "MOBSF_VT_ENABLED=", "--env", "MOBSF_VT_API_KEY=",
            "--env", "MOBSF_VT_UPLOAD=", "--env", "MOBSF_DOMAIN_MALWARE_SCAN=0",
            IMAGE, env=environment,
        )
        container_started = True
        address = command("docker", "port", container, "8000/tcp")
        if not re.fullmatch(r"127\.0\.0\.1:\d+", address):
            raise RuntimeError("MobSF API must bind only to the runner's loopback interface")
        client = MobSF("http://" + address, key)
        wait_ready(client)
        print("Uploading the APK to the local scanner...", flush=True)
        uploaded = client.upload(apk)
        if uploaded.get("hash") != apk_md5 or uploaded.get("scan_type") != "apk":
            raise RuntimeError("MobSF upload hash or scan type does not match the APK")
        write_json(output / "MobSF-upload.json", uploaded)
        print("Running MobSF static APK analysis...", flush=True)
        # A fresh container needs the initial scan, rather than an update-only re_scan.
        analysis = client.json("/api/v1/scan", {"hash": apk_md5}, timeout=900)
        write_json(output / "MobSF-scan.json", analysis)
        validate_report(analysis, apk_md5, apk_sha256)
        report = client.json("/api/v1/report_json", {"hash": apk_md5})
        write_json(output / "MobSF-report.json", report)
        counts = validate_report(report, apk_md5, apk_sha256)
        pdf = client.request(
            "/api/v1/download_pdf", urlencode({"hash": apk_md5}).encode(),
            "application/x-www-form-urlencoded", timeout=120,
        )
        if len(pdf) < 1000 or not pdf.startswith(b"%PDF-") or b"%%EOF" not in pdf[-1024:]:
            raise RuntimeError("MobSF did not produce a complete PDF report")
        (output / "MobSF-report.pdf").write_bytes(pdf)
        metadata.update(status="completed", package=EXPECTED_PACKAGE, findings=counts)
        summary = (
            "# MobSF static APK analysis\n\n"
            f"Package: `{EXPECTED_PACKAGE}`. Engine: MobSF 4.5.3 (digest pinned).\n\n"
            f"APK SHA-256: `{apk_sha256}`.\n\n"
            "| Classification | Findings |\n| --- | ---: |\n"
            + "".join(f"| {name} | {count} |\n" for name, count in counts.items())
            + "\nFindings are retained for review and do not block signing. Engine failures and "
            "incomplete or mismatched reports fail this job. A completed static scan does not "
            "guarantee that the APK has no vulnerabilities.\n"
        )
        (output / "MobSF-summary.md").write_text(summary, encoding="utf-8")
        if os.environ.get("GITHUB_STEP_SUMMARY"):
            with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as target:
                target.write(summary + "\n")
        print(f"MobSF completed: {counts['high']} high, {counts['warning']} warnings; reports saved.", flush=True)
    except Exception as error:
        metadata.update(status="failed", error=str(error).replace(key, "[redacted]"))
        raise
    finally:
        try:
            logs = command("docker", "logs", "--tail", "2000", container, check=False, include_stderr=True)
        except (OSError, RuntimeError, subprocess.TimeoutExpired):
            logs = "Engine logs unavailable. See the failed job step."
        (output / "MobSF-engine.log").write_text(logs.replace(key, "[redacted]"), encoding="utf-8")
        try:
            state = command("docker", "inspect", "--format", "{{json .State}}", container, check=False)
            (output / "MobSF-container-state.json").write_text(state.replace(key, "[redacted]"), encoding="utf-8")
        except (OSError, RuntimeError, subprocess.TimeoutExpired):
            pass
        cleanup_error = None
        try:
            command("docker", "rm", "--force", container, check=container_started)
            metadata["container_cleanup"] = "completed"
        except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
            cleanup_error = error
            metadata["container_cleanup"] = "failed"
        if cleanup_error and metadata["status"] == "completed":
            metadata.update(status="failed", error="Could not remove the MobSF container")
            write_json(output / "MobSF-status.json", metadata)
            raise RuntimeError("Could not remove the MobSF container") from cleanup_error
        write_json(output / "MobSF-status.json", metadata)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--output-dir", type=Path, default=Path("security-reports/apk"))
    arguments = parser.parse_args()
    try:
        scan(arguments.apk, arguments.output_dir)
    except (RuntimeError, OSError, subprocess.TimeoutExpired, zipfile.BadZipFile) as error:
        raise SystemExit(str(error)) from error


if __name__ == "__main__":
    main()
