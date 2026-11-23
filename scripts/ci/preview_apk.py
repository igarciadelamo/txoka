#!/usr/bin/env python3
"""Build provenance and isolated signing policy for opt-in preview APKs."""

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import zipfile


REPOSITORY = "igarciadelamo/txoka"
REPOSITORY_ID = 1436307628
WORKFLOW_PATH = ".github/workflows/preview-apk.yml"
SIGNER_WORKFLOW_REF = REPOSITORY + "/.github/workflows/sign-preview-apk.yml@refs/heads/main"
REQUIRED_JOBS = {"eligibility", "Build and verify preview", "Device persistence tests"}
APPLICATION_ID = "es.igarciadelamo.txoka.preview"
REQUIRED_PERMISSIONS = {"android.permission.NFC", APPLICATION_ID + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
OPTIONAL_PERMISSIONS = {"android.permission.CAMERA"}
LABEL = "build-apk"
MAX_ARCHIVE_BYTES = 128 * 1024 * 1024
MAX_METADATA_BYTES = 16 * 1024
SHA_PATTERN = re.compile(r"[0-9a-f]{40}\Z")
DIGEST_PATTERN = re.compile(r"[0-9a-f]{64}\Z")


class PolicyError(Exception):
    """Input or state failed the signing policy."""


def require(condition, message):
    if not condition:
        raise PolicyError(message)


def strict_json(data):
    def pairs(values):
        result = {}
        for key, value in values:
            require(key not in result, "Duplicate JSON property")
            result[key] = value
        return result

    try:
        return json.loads(data, object_pairs_hook=pairs)
    except (ValueError, UnicodeError) as error:
        raise PolicyError("Invalid JSON data") from error


def sha256_file(path):
    with Path(path).open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, target):
        return None


class GitHub:
    def __init__(self, token=None):
        self.token = token if token is not None else os.environ.get("GITHUB_TOKEN", "")
        self.opener = urllib.request.build_opener(NoRedirect())

    def request(self, path):
        require(path.startswith("/") and not path.startswith("//"), "Invalid API path")
        headers = {
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "txoka-preview-signing",
        }
        if self.token:
            headers["Authorization"] = "Bearer " + self.token
        return urllib.request.Request("https://api.github.com" + path, headers=headers)

    def get(self, path):
        try:
            with self.opener.open(self.request(path), timeout=30) as response:
                data = response.read(8 * 1024 * 1024 + 1)
            require(len(data) <= 8 * 1024 * 1024, "API response is too large")
            return strict_json(data)
        except (urllib.error.URLError, OSError) as error:
            raise PolicyError("GitHub API request failed") from error

    def pages(self, path, key=None):
        result = []
        separator = "&" if "?" in path else "?"
        for page in range(1, 21):
            payload = self.get(f"{path}{separator}per_page=100&page={page}")
            items = payload[key] if key else payload
            require(isinstance(items, list), "Invalid API collection")
            result.extend(items)
            if len(items) < 100:
                return result
        raise PolicyError("API collection exceeds the supported pagination limit")

    def download(self, artifact_id, destination):
        path = f"/repos/{REPOSITORY}/actions/artifacts/{artifact_id}/zip"
        try:
            try:
                self.opener.open(self.request(path), timeout=30)
            except urllib.error.HTTPError as response:
                require(response.code == 302, "Artifact API did not return a download redirect")
                target = response.headers.get("Location", "")
            else:
                raise PolicyError("Artifact API did not return a download redirect")
            url = urllib.parse.urlsplit(target)
            require(
                url.scheme == "https"
                and url.port in (None, 443)
                and url.username is None
                and url.password is None
                and bool(url.hostname)
                and (
                    url.hostname.endswith(".blob.core.windows.net")
                    or url.hostname.endswith(".actions.githubusercontent.com")
                ),
                "Unexpected artifact download host",
            )
            # The signed storage URL is fetched without any GitHub authorization header.
            with self.opener.open(urllib.request.Request(target), timeout=60) as response:
                require(response.status == 200, "Artifact download failed")
                size = 0
                with Path(destination).open("xb") as output:
                    while chunk := response.read(1024 * 1024):
                        size += len(chunk)
                        require(size <= MAX_ARCHIVE_BYTES, "Artifact download exceeds the size limit")
                        output.write(chunk)
        except (urllib.error.URLError, OSError, ValueError) as error:
            raise PolicyError("Artifact download failed") from error


def validate_pr(pr, sha, head_repository_id=None, head_branch=None):
    require(bool(SHA_PATTERN.fullmatch(sha)), "Invalid source SHA")
    base = pr.get("base") or {}
    head = pr.get("head") or {}
    return (
        pr.get("state") == "open"
        and not pr.get("merged_at")
        and (base.get("repo") or {}).get("id") == REPOSITORY_ID
        and base.get("ref") == "main"
        and head.get("sha") == sha
        and (head.get("repo") or {}).get("id") is not None
        and (head_repository_id is None or head["repo"]["id"] == head_repository_id)
        and (head_branch is None or head.get("ref") == head_branch)
        and LABEL in [label.get("name") for label in pr.get("labels", [])]
    )


def get_eligible_pr(api, number, sha):
    pr = api.get(f"/repos/{REPOSITORY}/pulls/{number}")
    return pr if validate_pr(pr, sha) else None


def unsigned_artifact_name(pr, sha, run_id, attempt):
    return f"preview-unsigned-pr{pr}-{sha[:12]}-run{run_id}-a{attempt}"


def signed_artifact_name(pr, sha, run_id, attempt):
    return f"txoka-pr-{pr}-{sha[:12]}-run{run_id}-a{attempt}"


def resolve(api, run_id, attempt):
    run = api.get(f"/repos/{REPOSITORY}/actions/runs/{run_id}")
    workflow = api.get(f"/repos/{REPOSITORY}/actions/workflows/preview-apk.yml")
    require(
        run.get("id") == run_id
        and (run.get("repository") or {}).get("id") == REPOSITORY_ID
        and run.get("event") == "pull_request"
        and run.get("workflow_id") == workflow.get("id")
        and workflow.get("path") == WORKFLOW_PATH
        and run.get("path") == WORKFLOW_PATH
        and run.get("status") == "completed"
        and run.get("conclusion") == "success",
        "Run is not a successful preview workflow from this repository",
    )
    require(type(attempt) is int and attempt > 0 and run.get("run_attempt") == attempt,
            "Workflow attempt changed")
    sha = run.get("head_sha", "")
    require(bool(SHA_PATTERN.fullmatch(sha)), "Run has no valid source SHA")
    source = run.get("head_repository") or {}
    owner = (source.get("owner") or {}).get("login")
    branch = run.get("head_branch")
    require(type(source.get("id")) is int and isinstance(owner, str) and owner
            and isinstance(branch, str) and branch, "Run has no source repository identity")
    query = urllib.parse.urlencode({"state": "open", "base": "main", "head": f"{owner}:{branch}"})
    # Fork workflow runs can have an empty pull_requests array. Live PR identity is authoritative.
    candidates = api.pages(f"/repos/{REPOSITORY}/pulls?{query}")
    matches = [pr for pr in candidates if validate_pr(pr, sha, source["id"], branch)]
    require(len(matches) <= 1, "More than one eligible PR matches this run")
    if not matches:
        return None
    number = matches[0].get("number")
    require(type(number) is int and number > 0, "Invalid PR number")
    pr = api.get(f"/repos/{REPOSITORY}/pulls/{number}")
    if not validate_pr(pr, sha, source["id"], branch):
        return None
    jobs = api.pages(f"/repos/{REPOSITORY}/actions/runs/{run_id}/attempts/{attempt}/jobs", "jobs")
    conclusions = {}
    for name in REQUIRED_JOBS:
        matches = [job for job in jobs if job.get("name") == name]
        require(len(matches) == 1 and matches[0].get("run_id") == run_id
                and matches[0].get("status") == "completed", "Missing or ambiguous preview job")
        conclusions[name] = matches[0].get("conclusion")
    if conclusions["Build and verify preview"] == conclusions["Device persistence tests"] == "skipped":
        return None
    require(all(result == "success" for result in conclusions.values()), "A required preview job did not succeed")
    expected_name = unsigned_artifact_name(number, sha, run_id, attempt)
    artifacts = api.pages(f"/repos/{REPOSITORY}/actions/runs/{run_id}/artifacts", "artifacts")
    matches = [artifact for artifact in artifacts if artifact.get("name") == expected_name]
    require(len(matches) == 1, "Expected exactly one unsigned preview artifact")
    artifact = matches[0]
    require(type(artifact.get("id")) is int and artifact["id"] > 0
            and artifact.get("expired") is False
            and type(artifact.get("size_in_bytes")) is int
            and 0 < artifact["size_in_bytes"] <= MAX_ARCHIVE_BYTES,
            "Artifact is expired or has an invalid identity or size")
    require((artifact.get("workflow_run") or {}).get("id") == run_id,
            "Artifact belongs to another workflow run")
    digest = artifact.get("digest", "")
    require(isinstance(digest, str) and digest.startswith("sha256:")
            and bool(DIGEST_PATTERN.fullmatch(digest[7:])), "Artifact has no SHA-256 digest")
    return {"pr": number, "sha": sha, "sha_short": sha[:12], "run_id": run_id, "attempt": attempt,
            "artifact_id": artifact["id"], "artifact_digest": digest[7:],
            "artifact_name": expected_name,
            "signed_artifact_name": signed_artifact_name(number, sha, run_id, attempt)}


def guard(api, run_id, attempt, artifact_id, artifact_digest):
    context = resolve(api, run_id, attempt)
    require(context is not None, "PR was closed, changed or lost the build-apk label")
    require(context["artifact_id"] == artifact_id and context["artifact_digest"] == artifact_digest,
            "Unsigned artifact identity changed")
    return context


def extract_candidate(archive, destination, digest):
    require(bool(DIGEST_PATTERN.fullmatch(digest)), "Invalid archive digest")
    require(sha256_file(archive) == digest, "Unsigned artifact archive digest does not match GitHub")
    target = Path(destination)
    target.mkdir(mode=0o700)
    try:
        with zipfile.ZipFile(archive) as source:
            entries = source.infolist()
            require(len(entries) == 2 and {entry.filename for entry in entries} == {"preview.apk", "source.json"},
                    "Unsigned artifact must contain only preview.apk and source.json")
            for entry in entries:
                mode = entry.external_attr >> 16
                require(not entry.is_dir() and stat.S_IFMT(mode) in (0, stat.S_IFREG)
                        and not entry.flag_bits & 1
                        and entry.compress_type in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED),
                        "Unsupported artifact entry")
                limit = MAX_METADATA_BYTES if entry.filename == "source.json" else MAX_ARCHIVE_BYTES
                require(0 < entry.file_size <= limit, "Artifact entry exceeds its size limit")
                with source.open(entry) as input_file, (target / entry.filename).open("xb") as output:
                    copied = 0
                    while chunk := input_file.read(1024 * 1024):
                        copied += len(chunk)
                        require(copied <= limit, "Expanded artifact entry exceeds its size limit")
                        output.write(chunk)
                require(copied == entry.file_size, "Artifact entry is truncated")
    except (zipfile.BadZipFile, RuntimeError, OSError) as error:
        raise PolicyError("Invalid unsigned artifact archive") from error
    return strict_json((target / "source.json").read_bytes())


def command(arguments, signing_password=False):
    environment = dict(os.environ)
    environment.pop("PREVIEW_KEYSTORE_B64", None)
    if not signing_password:
        environment.pop("PREVIEW_KEYSTORE_PASSWORD", None)
    try:
        result = subprocess.run([str(part) for part in arguments], stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=120, check=False, env=environment)
    except (OSError, subprocess.TimeoutExpired) as error:
        raise PolicyError("Android signing tool could not run") from error
    require(result.returncode == 0, "Android signing tool failed")
    require(len(result.stdout) <= 1024 * 1024, "Android signing tool output is too large")
    return result.stdout.decode("utf-8", errors="strict")


def validate_badging(text):
    packages = re.findall(r"^package: name='([^']*)' versionCode='([^']*)' versionName='([^']*)'", text, re.MULTILINE)
    require(len(packages) == 1, "APK has no unambiguous package identity")
    package, code, version = packages[0]
    require(package == APPLICATION_ID and re.fullmatch(r"[1-9][0-9]{0,9}", code) and int(code) <= 2_147_483_647
            and version.endswith("-preview") and len(version) <= 100,
            "APK does not have the expected preview identity and version")
    require(re.findall(r"^(?:minSdkVersion|sdkVersion):'([^']*)'", text, re.MULTILINE) == ["26"]
            and re.findall(r"^targetSdkVersion:'([^']*)'", text, re.MULTILINE) == ["37"],
            "APK SDK versions differ from the preview policy")
    require(re.findall(r"^application-label:'([^']*)'", text, re.MULTILINE) == ["Txoka PR"],
            "APK is not labelled Txoka PR")
    require(not re.search(r"^(?:application-(?:debuggable|testOnly)\b|testOnly=)", text, re.MULTILINE),
            "Preview APK must not be debuggable or test-only")
    permissions = re.findall(r"^uses-permission(?:-sdk-\d+)?: name='([^']*)'", text, re.MULTILINE)
    permission_set = set(permissions)
    require(REQUIRED_PERMISSIONS <= permission_set <= REQUIRED_PERMISSIONS | OPTIONAL_PERMISSIONS
            and len(permissions) == len(permission_set), "APK permissions differ from the preview policy")
    return {"application_id": package, "version_code": int(code), "version_name": version,
            "min_sdk": 26, "target_sdk": 37, "debuggable": False, "permissions": sorted(permissions)}


def inspect_apk(apk, build_tools):
    return validate_badging(command([Path(build_tools) / "aapt2", "dump", "badging", apk]))


def source_metadata(pr, sha, run_id, attempt, apk, identity):
    return {"schema_version": 1, "repository": REPOSITORY, "pull_request": pr,
            "source_sha": sha, "run_id": run_id, "run_attempt": attempt, "variant": "preview",
            "application_id": APPLICATION_ID, "version_code": identity["version_code"],
            "version_name": identity["version_name"], "sha256": sha256_file(apk)}


def validate_metadata(metadata, context, apk, identity):
    expected = source_metadata(context["pr"], context["sha"], context["run_id"], context["attempt"], apk, identity)
    require(isinstance(metadata, dict) and metadata == expected
            and all(type(metadata.get(key)) is int for key in ("schema_version", "pull_request", "run_id", "run_attempt", "version_code")),
            "Candidate metadata does not match GitHub or the APK")


def output_values(values):
    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with Path(output).open("a") as target:
            for key, value in values.items():
                rendered = str(value).lower() if type(value) is bool else str(value)
                require("\n" not in rendered and "\r" not in rendered, "Unsafe workflow output")
                target.write(f"{key}={rendered}\n")


def candidate(api, args):
    require(get_eligible_pr(api, args.pr, args.sha) is not None, "PR is no longer eligible")
    head = command(["git", "rev-parse", "HEAD"]).strip()
    require(head == args.sha, "Checkout does not match the requested source SHA")
    run_id = int(os.environ["GITHUB_RUN_ID"])
    attempt = int(os.environ["GITHUB_RUN_ATTEMPT"])
    require(run_id > 0 and attempt > 0, "Invalid build run identity")
    identity = inspect_apk(args.apk, args.build_tools)
    require(get_eligible_pr(api, args.pr, args.sha) is not None, "PR changed while building")
    target = Path(args.output)
    target.mkdir(mode=0o700)
    shutil.copyfile(args.apk, target / "preview.apk")
    (target / "source.json").write_text(json.dumps(source_metadata(args.pr, args.sha, run_id, attempt,
                                                                  target / "preview.apk", identity), indent=2) + "\n")
    output_values({"artifact_name": unsigned_artifact_name(args.pr, args.sha, run_id, attempt),
                   "sha_short": args.sha[:12]})


def keystore_fingerprint(keystore):
    text = command(["keytool", "-list", "-rfc", "-storetype", "PKCS12", "-keystore", keystore,
                    "-alias", "txoka-preview", "-storepass:env", "PREVIEW_KEYSTORE_PASSWORD"],
                   signing_password=True)
    certs = re.findall(r"-----BEGIN CERTIFICATE-----\s*([A-Za-z0-9+/=\s]+?)-----END CERTIFICATE-----", text)
    require(bool(certs), "Preview alias has no signing certificate")
    return hashlib.sha256(base64.b64decode(re.sub(r"\s", "", certs[0]), validate=True)).hexdigest()


def validate_signature(text, fingerprint):
    certificates = re.findall(
        r"^(?:Signer #\d+|V\d+(?:\.\d+)? Signer(?: #\d+)?):? certificate SHA-256 digest: ([0-9a-fA-F]{64})\s*$",
        text, re.MULTILINE,
    )
    require(re.findall(r"^Number of signers: (\d+)\s*$", text, re.MULTILINE) == ["1"]
            and len(certificates) == 1 and certificates[0].lower() == fingerprint,
            "Final APK certificate does not match the preview key")
    require("Verified using v2 scheme (APK Signature Scheme v2): true" in text,
            "Final APK has no verified v2 signature")


def sign(api, args):
    context = guard(api, args.run_id, args.attempt, args.artifact_id, args.artifact_digest)
    signer_sha = os.environ.get("SIGNER_SHA", "")
    require(bool(SHA_PATTERN.fullmatch(signer_sha)), "Missing trusted signer SHA")
    require(os.environ.get("GITHUB_REF") == "refs/heads/main", "Signing is only allowed from main")
    require(os.environ.get("GITHUB_REPOSITORY") == REPOSITORY, "Signing is only allowed in the upstream repository")
    require(os.environ.get("GITHUB_EVENT_NAME") == "workflow_run"
            and os.environ.get("GITHUB_WORKFLOW_REF") == SIGNER_WORKFLOW_REF,
            "Signing is only allowed from the trusted signer workflow")
    require(command(["git", "rev-parse", "HEAD"]).strip() == signer_sha,
            "Signer checkout does not match its trusted workflow SHA")
    root = os.environ.get("RUNNER_TEMP") or None
    with tempfile.TemporaryDirectory(prefix="txoka-preview-signing-", dir=root) as temporary:
        working = Path(temporary)
        archive = working / "candidate.zip"
        api.download(context["artifact_id"], archive)
        metadata = extract_candidate(archive, working / "candidate", context["artifact_digest"])
        apk = working / "candidate/preview.apk"
        identity = inspect_apk(apk, args.build_tools)
        validate_metadata(metadata, context, apk, identity)
        aligned = working / "aligned.apk"
        command([Path(args.build_tools) / "zipalign", "-f", "-P", "16", "4", apk, aligned])
        guard(api, args.run_id, args.attempt, args.artifact_id, args.artifact_digest)
        encoded = os.environ.get("PREVIEW_KEYSTORE_B64", "")
        require(0 < len(encoded) <= 48 * 1024 and bool(os.environ.get("PREVIEW_KEYSTORE_PASSWORD")),
                "Preview signing secrets are not configured")
        try:
            key_bytes = base64.b64decode(encoded, validate=True)
        except ValueError as error:
            raise PolicyError("Preview keystore is not valid Base64") from error
        require(0 < len(key_bytes) <= 36 * 1024, "Invalid preview keystore size")
        keystore = working / "preview.p12"
        with keystore.open("xb") as output:
            os.chmod(keystore, 0o600)
            output.write(key_bytes)
        del key_bytes
        fingerprint = keystore_fingerprint(keystore)
        signed = working / "signed.apk"
        command([Path(args.build_tools) / "apksigner", "sign", "--ks", keystore,
                 "--ks-type", "PKCS12", "--ks-key-alias", "txoka-preview",
                 "--ks-pass", "env:PREVIEW_KEYSTORE_PASSWORD", "--key-pass", "env:PREVIEW_KEYSTORE_PASSWORD",
                 "--v1-signing-enabled", "false", "--v2-signing-enabled", "true",
                 "--v3-signing-enabled", "true", "--v4-signing-enabled", "false",
                 "--out", signed, aligned], signing_password=True)
        keystore.unlink()
        verify = command([Path(args.build_tools) / "apksigner", "verify", "--verbose", "--print-certs", signed])
        validate_signature(verify, fingerprint)
        command([Path(args.build_tools) / "zipalign", "-c", "-P", "16", "4", signed])
        require(inspect_apk(signed, args.build_tools) == identity, "Signing changed APK manifest data")
        guard(api, args.run_id, args.attempt, args.artifact_id, args.artifact_digest)
        destination = Path(args.output)
        destination.mkdir(mode=0o700)
        filename = f"txoka-pr-{context['pr']}-{context['sha'][:12]}.apk"
        shutil.copyfile(signed, destination / filename)
        digest = sha256_file(destination / filename)
        manifest = {**identity, "schema_version": 1, "repository": REPOSITORY,
                    "pull_request": context["pr"], "source_sha": context["sha"],
                    "variant": "preview", "builder_run_id": args.run_id, "builder_run_attempt": args.attempt,
                    "builder_url": f"https://github.com/{REPOSITORY}/actions/runs/{args.run_id}/attempts/{args.attempt}",
                    "unsigned_artifact_id": context["artifact_id"], "unsigned_artifact_sha256": context["artifact_digest"],
                    "signer_sha": signer_sha, "signer_run_id": os.environ.get("GITHUB_RUN_ID"),
                    "signer_run_attempt": os.environ.get("GITHUB_RUN_ATTEMPT"),
                    "certificate_sha256": fingerprint, "apk": filename, "sha256": digest}
        (destination / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
        (destination / "SHA256SUMS").write_text(f"{digest}  {filename}\n")
        (destination / "README.txt").write_text(
            f"Txoka PR #{context['pr']}\nSource: {context['sha']}\n"
            f"Package: {APPLICATION_ID}\nCertificate SHA-256: {fingerprint}\n"
            "This test app installs alongside official Txoka and has separate data.\n"
            "Verify SHA256SUMS and manifest.json before installing. Keep the preview key stable\n"
            "for updates between previews. This APK cannot update the official app.\n"
            "The NFC AAR still targets official Txoka; test preview reception in foreground Receive mode.\n")
        output_values({"artifact_name": context["signed_artifact_name"],
                       "signed_artifact_name": context["signed_artifact_name"], "certificate_sha256": fingerprint,
                       "apk_sha256": digest})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)
    for name in ("gate", "candidate"):
        child = subparsers.add_parser(name)
        child.add_argument("--pr", type=int, required=True)
        child.add_argument("--sha", required=True)
        if name == "candidate":
            child.add_argument("--apk", required=True)
            child.add_argument("--output", required=True)
            child.add_argument("--build-tools", required=True)
    for name in ("resolve", "guard", "sign"):
        child = subparsers.add_parser(name)
        child.add_argument("--run-id", type=int, required=True)
        child.add_argument("--attempt", type=int, required=True)
        if name in ("guard", "sign"):
            child.add_argument("--artifact-id", type=int, required=True)
            child.add_argument("--artifact-digest", required=True)
        if name == "sign":
            child.add_argument("--output", required=True)
            child.add_argument("--build-tools", required=True)
    args = parser.parse_args()
    api = GitHub()
    try:
        if args.command == "gate":
            eligible = get_eligible_pr(api, args.pr, args.sha) is not None
            output_values({"eligible": eligible})
            print("PR is eligible." if eligible else "Preview skipped: PR is no longer eligible.")
        elif args.command == "candidate":
            candidate(api, args)
            print("Unsigned preview candidate prepared.")
        elif args.command == "resolve":
            context = resolve(api, args.run_id, args.attempt)
            output_values({"eligible": context is not None, **(context or {})})
            print("Preview candidate verified." if context else "Signing skipped: no eligible preview build.")
        elif args.command == "guard":
            guard(api, args.run_id, args.attempt, args.artifact_id, args.artifact_digest)
            print("Preview remains eligible for publication.")
        elif args.command == "sign":
            sign(api, args)
            print("Preview APK signed and verified.")
    except (PolicyError, KeyError, TypeError, ValueError, UnicodeError, OSError) as error:
        print(f"Preview policy failed: {error if isinstance(error, PolicyError) else 'invalid input or tool response'}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
