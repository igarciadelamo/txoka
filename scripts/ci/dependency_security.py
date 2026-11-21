"""Scan the resolved preview Maven inventory with a checksum-pinned OSV engine."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import subprocess
import tempfile
from urllib.parse import unquote
import urllib.request


VERSION = "2.7.0"
BINARY_SHA256 = "160c6253e2922e9e0a6a9771c5c4c40a4b3c6090b34a3cdd5e80c96cc2e4218b"
BINARY_URL = f"https://github.com/google/osv-scanner/releases/download/v{VERSION}/osv-scanner_linux_amd64"
MAX_BINARY_BYTES = 150 * 1024 * 1024


def download_scanner(directory):
    if platform.system() != "Linux" or platform.machine().lower() not in {"x86_64", "amd64"}:
        raise ValueError("The pinned OSV binary requires Linux AMD64.")
    binary = Path(directory) / "osv-scanner"
    digest = hashlib.sha256()
    size = 0
    with urllib.request.urlopen(BINARY_URL, timeout=60) as response, binary.open("wb") as output:
        while chunk := response.read(1024 * 1024):
            size += len(chunk)
            if size > MAX_BINARY_BYTES:
                raise ValueError("The OSV binary exceeds the expected size limit.")
            digest.update(chunk)
            output.write(chunk)
    if digest.hexdigest() != BINARY_SHA256:
        raise ValueError("OSV binary SHA-256 does not match the pinned release.")
    binary.chmod(0o700)
    return binary


def inventory(sbom):
    data = json.loads(Path(sbom).read_text())
    if not isinstance(data, dict) or data.get("bomFormat") != "CycloneDX" or data.get("specVersion") != "1.6":
        raise ValueError("The runtime inventory is not a CycloneDX 1.6 document.")
    components = data.get("components")
    if not isinstance(components, list) or not components:
        raise ValueError("The preview runtime inventory is empty or incomplete; OSV was not run.")
    metadata = data.get("metadata")
    if not isinstance(metadata, dict) or not isinstance(metadata.get("properties"), list):
        raise ValueError("The SBOM is missing its configuration metadata.")
    properties = metadata["properties"]
    if {"name": "txoka:configuration", "value": "previewRuntimeClasspath"} not in properties:
        raise ValueError("The SBOM is not identified as previewRuntimeClasspath.")
    purls = set()
    for item in components:
        if not isinstance(item, dict) or item.get("type") != "library":
            raise ValueError("The runtime inventory contains an invalid component.")
        purl = item.get("purl")
        match = re.fullmatch(r"pkg:maven/([^/@\s]+)/([^/@\s]+)@([^/@\s]+)", purl) if isinstance(purl, str) else None
        if not match or re.search(r"%(?![0-9a-fA-F]{2})", purl):
            raise ValueError("The runtime inventory contains unresolved Maven coordinates.")
        if not all(isinstance(item.get(key), str) and item[key] for key in ("group", "name", "version")):
            raise ValueError("The runtime inventory is missing component identity fields.")
        if tuple(unquote(value, errors="strict") for value in match.groups()) != (item["group"], item["name"], item["version"]):
            raise ValueError("The SBOM package URL does not match its Maven identity fields.")
        if purl in purls:
            raise ValueError("The runtime inventory contains duplicate components.")
        purls.add(purl)
    return components


def vulnerability_ids(report):
    return sorted({
        vulnerability["id"]
        for result in report["results"]
        for package in result.get("packages", [])
        for vulnerability in package.get("vulnerabilities", [])
        if isinstance(vulnerability, dict) and isinstance(vulnerability.get("id"), str)
    })


def validate_report(report, components):
    if not isinstance(report, dict) or not isinstance(report.get("results"), list):
        raise ValueError("OSV did not produce a complete JSON report.")
    if not all(isinstance(result, dict) and isinstance(result.get("packages"), list) for result in report["results"]):
        raise ValueError("OSV reported malformed result records.")
    packages = [package for result in report["results"] for package in result["packages"]]
    if not all(isinstance(item, dict) and isinstance(item.get("package"), dict) for item in packages):
        raise ValueError("OSV reported malformed package records.")
    expected = {(item["group"] + ":" + item["name"], item["version"]) for item in components}
    if not all(
        item["package"].get("ecosystem") == "Maven"
        and isinstance(item["package"].get("name"), str)
        and isinstance(item["package"].get("version"), str)
        for item in packages
    ):
        raise ValueError("OSV reported unexpected ecosystem or incomplete package identities.")
    reported = [(item["package"]["name"], item["package"]["version"]) for item in packages]
    if len(reported) != len(set(reported)):
        raise ValueError("OSV reported duplicate package identities.")
    if expected != set(reported):
        raise ValueError("OSV omitted components from the resolved runtime inventory.")


def scan(sbom, output_dir):
    sbom, output_dir = Path(sbom).resolve(), Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    components = inventory(sbom)
    with tempfile.TemporaryDirectory(prefix="txoka-osv-") as temporary:
        binary = download_scanner(temporary)
        result = subprocess.run(
            [str(binary), "scan", "source", "--format", "json", "--all-packages", "-L", str(sbom)],
            capture_output=True, text=True, timeout=600, check=False,
        )
    (output_dir / "OSV-scanner.log").write_text(result.stderr)
    if result.returncode not in {0, 1}:
        raise ValueError(f"OSV engine failed with exit code {result.returncode}.")
    report = json.loads(result.stdout)
    validate_report(report, components)
    findings = vulnerability_ids(report)
    report["scan_metadata"] = {
        "tool": "OSV-Scanner", "version": VERSION, "binary_sha256": BINARY_SHA256,
        "status": "completed", "mode": "report-only", "scanner_exit_code": result.returncode,
        "configuration": "previewRuntimeClasspath", "runtime_components": len(components),
        "sbom_sha256": hashlib.sha256(sbom.read_bytes()).hexdigest(),
    }
    (output_dir / "OSV-report.json").write_text(json.dumps(report, indent=2) + "\n")
    summary = (
        f"# Runtime dependencies: OSV-Scanner {VERSION}\n\n"
        f"Resolved preview Maven components: **{len(components)}**. Vulnerability IDs: **{len(findings)}**.\n\n"
        "Findings are report-only. Engine failures, malformed reports and omitted inventory components fail the job.\n\n"
        "Coverage: external Maven modules from `previewRuntimeClasspath`, including transitive dependencies. "
        "Android supplied by the device, app source and build tools are outside this inventory.\n"
    )
    if findings:
        summary += "\n" + "\n".join("- " + item.replace("\n", " ") for item in findings) + "\n"
    (output_dir / "OSV-summary.md").write_text(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as output:
            output.write(summary + "\n")
    print(f"OSV: {len(components)} Maven components; {len(findings)} report-only vulnerability IDs.")
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sbom", required=True, type=Path)
    parser.add_argument("--output-dir", type=Path, default=Path("security-reports/dependencies"))
    args = parser.parse_args()
    try:
        scan(args.sbom, args.output_dir)
    except (OSError, ValueError, TypeError, subprocess.SubprocessError) as error:
        args.output_dir.mkdir(parents=True, exist_ok=True)
        (args.output_dir / "OSV-error.txt").write_text("INCOMPLETE SCAN: " + str(error) + "\n")
        raise SystemExit(str(error)) from error


if __name__ == "__main__":
    main()
