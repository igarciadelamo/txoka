"""Scan native application sources with local rules and audit Semgrep's coverage."""

import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[2]
VERSION = "1.182.0"
RULES = Path("scripts/ci/source-security-rules.yml")


def targets(root):
    source = root / "app/src"
    return sorted(
        path for path in source.rglob("*")
        if path.is_file() and path.suffix in {".kt", ".java"}
        and not path.relative_to(source).parts[0].startswith(("test", "androidTest"))
    )


def validate_report(report, selected, root):
    if not isinstance(report, dict) or not isinstance(report.get("results"), list):
        raise ValueError("Semgrep did not produce a valid findings list.")
    if report.get("version") != VERSION:
        raise ValueError("Semgrep's reported version does not match the pinned engine.")
    if not isinstance(report.get("errors"), list):
        raise ValueError("Semgrep did not report its analysis errors.")
    if report["errors"]:
        raise ValueError("Semgrep reported analysis errors; inspect Semgrep-report.json.")
    paths = report.get("paths")
    if not isinstance(paths, dict) or not isinstance(paths.get("scanned"), list):
        raise ValueError("Semgrep did not report its scanned files.")
    scanned = {(root / path).resolve().relative_to(root).as_posix() for path in paths["scanned"]}
    missing = {path.relative_to(root).as_posix() for path in selected} - scanned
    if missing:
        raise ValueError("Semgrep omitted selected source files: " + ", ".join(sorted(missing)))


def scan(root, output_dir):
    root = Path(root).resolve()
    output_dir = Path(output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)
    report = {}
    failure = None
    selected = targets(root)
    try:
        if not selected or not any(path.suffix == ".kt" for path in selected):
            raise ValueError("No native Kotlin application source files were selected.")
        environment = os.environ.copy()
        for key in ("SEMGREP_APP_TOKEN", "SEMGREP_APP_URL"):
            environment.pop(key, None)
        environment["SEMGREP_SEND_METRICS"] = "off"
        environment["SEMGREP_ENABLE_VERSION_CHECK"] = "0"
        environment["SEMGREP_SETTINGS_FILE"] = str(output_dir / ".semgrep-settings.yml")
        environment["SEMGREP_LOG_FILE"] = str(output_dir / "Semgrep-internal.log")
        command = [
            "semgrep", "scan", "--config", str(RULES), "--metrics", "off",
            "--disable-version-check", "--no-error", "--quiet", "--strict",
            "--no-git-ignore", "--x-ignore-semgrepignore-files",
            "--json-output", str(output_dir / "Semgrep-report.json"),
            "--sarif-output", str(output_dir / "Semgrep-report.sarif"),
            *[path.relative_to(root).as_posix() for path in selected],
        ]
        with (output_dir / "Semgrep-engine.log").open("w") as log:
            process = subprocess.run(
                command, cwd=root, env=environment, stdout=log,
                stderr=subprocess.STDOUT, timeout=600, check=False,
            )
        if process.returncode not in {0, 1}:
            raise ValueError(f"Semgrep engine failed with exit code {process.returncode}.")
        report = json.loads((output_dir / "Semgrep-report.json").read_text())
        validate_report(report, selected, root)
        sarif = json.loads((output_dir / "Semgrep-report.sarif").read_text())
        if not isinstance(sarif, dict) or sarif.get("version") != "2.1.0" or not isinstance(sarif.get("runs"), list):
            raise ValueError("Semgrep did not produce a complete SARIF report.")
    except (OSError, ValueError, TypeError, subprocess.SubprocessError) as error:
        failure = str(error)
    findings = report.get("results", []) if isinstance(report, dict) else []
    findings = findings if isinstance(findings, list) else []
    counts = Counter(item.get("extra", {}).get("severity", "UNKNOWN") for item in findings if isinstance(item, dict))
    evidence = {
        "tool": "Semgrep", "version": VERSION, "mode": "report-only",
        "status": "failed" if failure else "completed", "failure": failure,
        "rules_sha256": hashlib.sha256((root / RULES).read_bytes()).hexdigest(),
        "files": {path.relative_to(root).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest() for path in selected},
        "findings": len(findings), "severities": dict(sorted(counts.items())),
    }
    (output_dir / "source-coverage.json").write_text(json.dumps(evidence, indent=2) + "\n")
    summary = (
        f"# Source security: Semgrep {VERSION}\n\n"
        f"Status: **{evidence['status']}**. Native Kotlin/Java files selected: **{len(selected)}**. "
        f"Findings: **{len(findings)}**.\n\n"
        "Findings are report-only. Engine errors, parser errors and omitted source files fail the job. "
        "Local rules inspect selected native source patterns and potential embedded secrets; they are not a complete security audit.\n"
    )
    if failure:
        summary += "\n**INCOMPLETE SCAN:** " + failure.replace("\n", " ").replace("`", "") + "\n"
    (output_dir / "Semgrep-summary.md").write_text(summary)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as output:
            output.write(summary + "\n")
    print(f"Semgrep: {evidence['status']}; {len(selected)} native files, {len(findings)} report-only findings.")
    return 1 if failure else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=Path("security-reports/source"))
    args = parser.parse_args()
    return scan(ROOT, args.output_dir)


if __name__ == "__main__":
    raise SystemExit(main())
