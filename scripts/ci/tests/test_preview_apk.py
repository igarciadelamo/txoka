import argparse
import base64
import copy
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import stat
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import zipfile


SPEC = importlib.util.spec_from_file_location("preview_apk", Path(__file__).parents[1] / "preview_apk.py")
preview = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(preview)

SHA = "a" * 40
OTHER_SHA = "b" * 40
DIGEST = "c" * 64
RUN_ID = 1234
ATTEMPT = 2
PR_NUMBER = 55
SOURCE_ID = 1463922408


def pull_request():
    return {
        "number": PR_NUMBER, "state": "open", "merged_at": None,
        "base": {"ref": "main", "repo": {"id": preview.REPOSITORY_ID}},
        "head": {"ref": "feature/improve-cards", "sha": SHA,
                 "repo": {"id": SOURCE_ID, "owner": {"login": "ml0renz0"}}},
        "labels": [{"name": preview.LABEL}],
    }


class FakeGitHub:
    def __init__(self):
        self.run = {"id": RUN_ID, "repository": {"id": preview.REPOSITORY_ID},
                    "event": "pull_request", "workflow_id": 88, "path": preview.WORKFLOW_PATH,
                    "status": "completed", "conclusion": "success", "run_attempt": ATTEMPT,
                    "head_sha": SHA, "head_branch": "feature/improve-cards", "pull_requests": [],
                    "head_repository": {"id": SOURCE_ID, "owner": {"login": "ml0renz0"}}}
        self.workflow = {"id": 88, "path": preview.WORKFLOW_PATH}
        self.pr = pull_request()
        self.candidates = [self.pr]
        self.jobs = [{"name": name, "run_id": RUN_ID, "status": "completed", "conclusion": "success"}
                     for name in preview.REQUIRED_JOBS]
        self.artifacts = [{"id": 4567, "name": preview.unsigned_artifact_name(PR_NUMBER, SHA, RUN_ID, ATTEMPT),
                           "expired": False, "size_in_bytes": 2048, "workflow_run": {"id": RUN_ID},
                           "digest": "sha256:" + DIGEST}]
        self.requests = []

    def get(self, path):
        self.requests.append(path)
        if path.endswith("/actions/workflows/preview-apk.yml"):
            return copy.deepcopy(self.workflow)
        if path.endswith(f"/actions/runs/{RUN_ID}"):
            return copy.deepcopy(self.run)
        if path.endswith(f"/pulls/{PR_NUMBER}"):
            return copy.deepcopy(self.pr)
        raise AssertionError("Unexpected API request: " + path)

    def pages(self, path, key=None):
        self.requests.append(path)
        if path.endswith(f"/attempts/{ATTEMPT}/jobs") and key == "jobs":
            return copy.deepcopy(self.jobs)
        if path.endswith("/artifacts") and key == "artifacts":
            return copy.deepcopy(self.artifacts)
        if "/pulls?" in path and key is None:
            return copy.deepcopy(self.candidates)
        raise AssertionError("Unexpected API collection: " + path)


class PreviewEligibilityTests(unittest.TestCase):
    def test_fork_run_with_empty_pr_associations_is_resolved_from_live_api(self):
        api = FakeGitHub()
        context = preview.resolve(api, RUN_ID, ATTEMPT)
        self.assertEqual(PR_NUMBER, context["pr"])
        self.assertEqual(SHA, context["sha"])
        self.assertEqual(4567, context["artifact_id"])
        self.assertEqual(DIGEST, context["artifact_digest"])
        self.assertIn("head=ml0renz0%3Afeature%2Fimprove-cards", " ".join(api.requests))

    def test_eligible_pr_is_rechecked_after_the_list_response(self):
        api = FakeGitHub()
        api.candidates = [copy.deepcopy(api.pr)]
        api.pr["head"]["sha"] = OTHER_SHA
        self.assertIsNone(preview.resolve(api, RUN_ID, ATTEMPT))

    def test_no_label_closed_wrong_base_old_sha_and_other_fork_do_not_match(self):
        changes = [
            lambda pr: pr.update(labels=[]),
            lambda pr: pr.update(state="closed"),
            lambda pr: pr["base"].update(ref="release"),
            lambda pr: pr["base"]["repo"].update(id=999),
            lambda pr: pr["head"].update(sha=OTHER_SHA),
            lambda pr: pr["head"]["repo"].update(id=999),
            lambda pr: pr["head"].update(ref="another-branch"),
        ]
        for change in changes:
            with self.subTest(change=change):
                api = FakeGitHub()
                change(api.pr)
                self.assertIsNone(preview.resolve(api, RUN_ID, ATTEMPT))

    def test_unlabelled_run_with_skipped_jobs_is_a_clean_skip(self):
        api = FakeGitHub()
        api.pr["labels"] = []
        for job in api.jobs:
            job["conclusion"] = "skipped"
        api.artifacts = []
        self.assertIsNone(preview.resolve(api, RUN_ID, ATTEMPT))

    def test_multiple_current_eligible_prs_are_rejected(self):
        api = FakeGitHub()
        api.candidates.append(copy.deepcopy(api.pr))
        with self.assertRaises(preview.PolicyError):
            preview.resolve(api, RUN_ID, ATTEMPT)

    def test_wrong_run_workflow_event_repository_or_conclusion_is_rejected(self):
        changes = [
            {"id": RUN_ID + 1}, {"workflow_id": 89}, {"path": ".github/workflows/fake.yml"},
            {"event": "push"}, {"repository": {"id": 999}}, {"status": "in_progress"},
            {"conclusion": "failure"}, {"head_sha": "not-a-sha"},
            {"head_repository": {"id": 12}},
        ]
        for change in changes:
            with self.subTest(change=change):
                api = FakeGitHub()
                api.run.update(change)
                with self.assertRaises(preview.PolicyError):
                    preview.resolve(api, RUN_ID, ATTEMPT)

    def test_wrong_workflow_path_and_newer_attempt_are_rejected(self):
        api = FakeGitHub()
        api.workflow["path"] = ".github/workflows/elsewhere.yml"
        with self.assertRaises(preview.PolicyError):
            preview.resolve(api, RUN_ID, ATTEMPT)
        api = FakeGitHub()
        api.run["run_attempt"] += 1
        with self.assertRaisesRegex(preview.PolicyError, "attempt changed"):
            preview.resolve(api, RUN_ID, ATTEMPT)

    def test_every_required_build_and_security_job_must_run_successfully(self):
        for name in preview.REQUIRED_JOBS:
            for conclusion in ("skipped", "failure", "cancelled", None):
                with self.subTest(job=name, conclusion=conclusion):
                    api = FakeGitHub()
                    next(job for job in api.jobs if job["name"] == name)["conclusion"] = conclusion
                    with self.assertRaises(preview.PolicyError):
                        preview.resolve(api, RUN_ID, ATTEMPT)
        for transform in (lambda jobs: jobs.pop(), lambda jobs: jobs.append(copy.deepcopy(jobs[0]))):
            api = FakeGitHub()
            transform(api.jobs)
            with self.assertRaises(preview.PolicyError):
                preview.resolve(api, RUN_ID, ATTEMPT)

    def test_missing_ambiguous_expired_wrong_run_and_unhashed_artifacts_are_rejected(self):
        changes = [
            lambda artifacts: artifacts.clear(),
            lambda artifacts: artifacts.append(copy.deepcopy(artifacts[0])),
            lambda artifacts: artifacts[0].update(expired=True),
            lambda artifacts: artifacts[0].update(name="preview-latest"),
            lambda artifacts: artifacts[0].update(size_in_bytes=preview.MAX_ARCHIVE_BYTES + 1),
            lambda artifacts: artifacts[0].update(workflow_run={"id": RUN_ID + 1}),
            lambda artifacts: artifacts[0].update(digest=None),
            lambda artifacts: artifacts[0].update(digest="md5:" + "a" * 32),
        ]
        for change in changes:
            with self.subTest(change=change):
                api = FakeGitHub()
                change(api.artifacts)
                with self.assertRaises(preview.PolicyError):
                    preview.resolve(api, RUN_ID, ATTEMPT)

    def test_guard_refuses_replacement_artifacts_and_withdrawn_authorization(self):
        api = FakeGitHub()
        with self.assertRaises(preview.PolicyError):
            preview.guard(api, RUN_ID, ATTEMPT, 999, DIGEST)
        with self.assertRaises(preview.PolicyError):
            preview.guard(api, RUN_ID, ATTEMPT, 4567, "e" * 64)
        api.pr["labels"] = []
        with self.assertRaises(preview.PolicyError):
            preview.guard(api, RUN_ID, ATTEMPT, 4567, DIGEST)


def archive_at(path, entries):
    with zipfile.ZipFile(path, "w") as archive:
        for name, data in entries:
            archive.writestr(name, data)
    return preview.sha256_file(path)


class PreviewArchiveTests(unittest.TestCase):
    def test_archive_extracts_only_two_expected_files_after_digest_verification(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "candidate.zip"
            digest = archive_at(archive, [("preview.apk", b"apk"), ("source.json", b'{"ok":true}')])
            self.assertEqual({"ok": True}, preview.extract_candidate(archive, root / "out", digest))
            self.assertEqual(["preview.apk", "source.json"], sorted(path.name for path in (root / "out").iterdir()))
            with self.assertRaises(preview.PolicyError):
                preview.extract_candidate(archive, root / "other", "a" * 64)

    def test_traversal_absolute_paths_duplicates_and_unexpected_files_are_rejected(self):
        invalid = ["../preview.apk", "/preview.apk", "sub/preview.apk", "source.json", "private.p12"]
        for name in invalid:
            with self.subTest(name=name), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                archive = root / "candidate.zip"
                with self.assertWarns(UserWarning) if name == "source.json" else _no_warning():
                    digest = archive_at(archive, [(name, b"apk"), ("source.json", b"{}")])
                with self.assertRaises(preview.PolicyError):
                    preview.extract_candidate(archive, root / "out", digest)

    def test_symlinks_and_oversized_metadata_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "candidate.zip"
            symlink = zipfile.ZipInfo("preview.apk")
            symlink.create_system = 3
            symlink.external_attr = (stat.S_IFLNK | 0o777) << 16
            digest = archive_at(archive, [(symlink, b"/etc/passwd"), ("source.json", b"{}")])
            with self.assertRaises(preview.PolicyError):
                preview.extract_candidate(archive, root / "out", digest)
            digest = archive_at(archive, [("preview.apk", b"apk"), ("source.json", b" " * (preview.MAX_METADATA_BYTES + 1))])
            with self.assertRaises(preview.PolicyError):
                preview.extract_candidate(archive, root / "out2", digest)

    def test_invalid_archive_and_duplicate_json_keys_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            archive = root / "invalid.zip"
            archive.write_bytes(b"not a zip")
            with self.assertRaises(preview.PolicyError):
                preview.extract_candidate(archive, root / "out", preview.sha256_file(archive))
        with self.assertRaises(preview.PolicyError):
            preview.strict_json(b'{"source_sha":"a","source_sha":"b"}')


class _no_warning:
    def __enter__(self):
        return self

    def __exit__(self, *args):
        return False


BADGING = f"""package: name='{preview.APPLICATION_ID}' versionCode='1' versionName='1.0-preview'
minSdkVersion:'26'
targetSdkVersion:'37'
uses-permission: name='android.permission.NFC'
uses-permission: name='{preview.APPLICATION_ID}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
application-label:'Txoka PR'
"""


class PreviewApkIdentityTests(unittest.TestCase):
    def test_camera_is_the_only_optional_permission_and_baseline_remains_accepted(self):
        baseline = preview.validate_badging(BADGING, 1)
        required = {"android.permission.NFC", preview.APPLICATION_ID + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
        self.assertEqual(required, set(baseline["permissions"]))
        for declaration in ("uses-permission", "uses-permission-sdk-23"):
            camera = BADGING + f"{declaration}: name='android.permission.CAMERA'\n"
            with self.subTest(declaration=declaration):
                identity = preview.validate_badging(camera, 1)
                self.assertEqual(sorted(required | {"android.permission.CAMERA"}), identity["permissions"])

    def test_camera_cannot_authorize_additional_permissions_or_duplicate_declarations(self):
        camera = BADGING + "uses-permission: name='android.permission.CAMERA'\n"
        extra_permissions = (
            "uses-permission: name='android.permission.INTERNET'\n",
            "uses-permission: name='android.permission.RECORD_AUDIO'\n",
            "uses-permission: name='android.permission.UNKNOWN'\n",
            "uses-permission: name='android.permission.CAMERA'\n",
            "uses-permission-sdk-23: name='android.permission.CAMERA'\n",
        )
        for extra in extra_permissions:
            with self.subTest(extra=extra), self.assertRaises(preview.PolicyError):
                preview.validate_badging(camera + extra, 1)

    def test_camera_does_not_replace_either_required_permission(self):
        camera = BADGING + "uses-permission: name='android.permission.CAMERA'\n"
        for permission in ("android.permission.NFC", preview.APPLICATION_ID + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"):
            missing_required = camera.replace(f"uses-permission: name='{permission}'\n", "")
            with self.subTest(permission=permission), self.assertRaises(preview.PolicyError):
                preview.validate_badging(missing_required, 1)

    def test_actual_apk_manifest_fields_are_required(self):
        identity = preview.validate_badging(BADGING, 1)
        self.assertEqual(preview.APPLICATION_ID, identity["application_id"])
        self.assertFalse(identity["debuggable"])
        modifications = [
            BADGING.replace(preview.APPLICATION_ID, "es.igarciadelamo.txoka"),
            BADGING.replace("versionCode='1'", "versionCode='999999'"),
            BADGING.replace("1.0-preview", "1.0"),
            BADGING.replace("minSdkVersion:'26'", "minSdkVersion:'27'"),
            BADGING.replace("targetSdkVersion:'37'", "targetSdkVersion:'36'"),
            BADGING.replace("Txoka PR", "Txoka"),
            BADGING + "application-debuggable\n", BADGING + "application-testOnly\n",
            BADGING + "testOnly='-1'\n",
            BADGING + "uses-permission: name='android.permission.INTERNET'\n",
            BADGING + "uses-permission-sdk-23: name='android.permission.NFC'\n",
            BADGING.replace("uses-permission: name='android.permission.NFC'\n", ""),
        ]
        for text in modifications:
            with self.subTest(text=text), self.assertRaises(preview.PolicyError):
                preview.validate_badging(text, 1)

    def test_metadata_is_only_accepted_when_it_matches_api_and_apk(self):
        identity = preview.validate_badging(BADGING, 1)
        context = {"pr": PR_NUMBER, "sha": SHA, "run_id": RUN_ID, "attempt": ATTEMPT}
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / "preview.apk"
            apk.write_bytes(b"unsigned apk")
            metadata = preview.source_metadata(PR_NUMBER, SHA, RUN_ID, ATTEMPT, apk, identity)
            preview.validate_metadata(metadata, context, apk, identity)
            for key, value in [("source_sha", OTHER_SHA), ("run_attempt", ATTEMPT + 1),
                               ("sha256", "a" * 64), ("version_code", True), ("extra", "value")]:
                altered = {**metadata, key: value}
                with self.subTest(key=key), self.assertRaises(preview.PolicyError):
                    preview.validate_metadata(altered, context, apk, identity)

    def test_certificate_and_v2_signature_are_both_verified(self):
        text = ("Verified using v2 scheme (APK Signature Scheme v2): true\nNumber of signers: 1\n"
                f"Signer #1 certificate SHA-256 digest: {DIGEST}\n")
        preview.validate_signature(text, DIGEST)
        preview.validate_signature(text.replace("Signer #1 certificate", "V3.0 Signer: certificate"), DIGEST)
        for altered in (text.replace("true", "false"), text.replace(DIGEST, "d" * 64),
                        text.replace("Number of signers: 1", "Number of signers: 2"),
                        text + f"Signer #2 certificate SHA-256 digest: {DIGEST}\n"):
            with self.assertRaises(preview.PolicyError):
                preview.validate_signature(altered, DIGEST)


class PreviewSigningFlowTests(unittest.TestCase):
    def run_signing(self, directory, fail_sign=False, environment_override=None):
        root = Path(directory)
        runner = root / "runner"
        runner.mkdir()
        apk = root / "unsigned.apk"
        apk.write_bytes(b"test apk bytes")
        identity = preview.validate_badging(BADGING, 1)
        metadata = preview.source_metadata(PR_NUMBER, SHA, RUN_ID, ATTEMPT, apk, identity)
        archive = root / "source.zip"
        digest = archive_at(archive, [("preview.apk", apk.read_bytes()),
                                      ("source.json", json.dumps(metadata).encode())])
        context = {"pr": PR_NUMBER, "sha": SHA, "run_id": RUN_ID, "attempt": ATTEMPT,
                   "artifact_id": 4567, "artifact_digest": digest,
                   "signed_artifact_name": preview.signed_artifact_name(PR_NUMBER, SHA, RUN_ID, ATTEMPT)}
        args = argparse.Namespace(run_id=RUN_ID, attempt=ATTEMPT, artifact_id=4567,
                                  artifact_digest=digest, output=str(root / "final"), build_tools="/tools")
        environment = {"SIGNER_SHA": OTHER_SHA, "GITHUB_REF": "refs/heads/main",
                       "GITHUB_REPOSITORY": preview.REPOSITORY, "GITHUB_EVENT_NAME": "workflow_run",
                       "GITHUB_WORKFLOW_REF": preview.SIGNER_WORKFLOW_REF, "RUNNER_TEMP": str(runner),
                       "PREVIEW_KEYSTORE_B64": base64.b64encode(b"fixture private key bytes").decode(),
                       "PREVIEW_KEYSTORE_PASSWORD": "fixture password", "GITHUB_RUN_ID": "9999",
                       "GITHUB_RUN_ATTEMPT": "1"}
        environment.update(environment_override or {})
        api = FakeGitHub()
        api.download = lambda artifact_id, target: shutil.copyfile(archive, target)
        seen_key = []

        def tool(arguments, signing_password=False):
            parts = [str(value) for value in arguments]
            if parts == ["git", "rev-parse", "HEAD"]:
                return OTHER_SHA + "\n"
            if parts[0] == "/tools/zipalign" and "-f" in parts:
                shutil.copyfile(parts[-2], parts[-1])
                return ""
            if parts[0] == "/tools/apksigner" and parts[1] == "sign":
                key = Path(parts[parts.index("--ks") + 1])
                seen_key.append((key, stat.S_IMODE(key.stat().st_mode)))
                if fail_sign:
                    raise preview.PolicyError("fixture signing failure")
                shutil.copyfile(parts[-1], parts[parts.index("--out") + 1])
                return ""
            if parts[0] == "/tools/apksigner" and parts[1] == "verify":
                return f"Verified using v2 scheme (APK Signature Scheme v2): true\nNumber of signers: 1\nSigner #1 certificate SHA-256 digest: {DIGEST}\n"
            if parts[0] == "/tools/zipalign" and "-c" in parts:
                return ""
            raise AssertionError(parts)

        with patch.dict(os.environ, environment, clear=True), patch.object(preview, "guard", return_value=context), \
                patch.object(preview, "inspect_apk", return_value=identity), \
                patch.object(preview, "command", side_effect=tool), \
                patch.object(preview, "keystore_fingerprint", return_value=DIGEST):
            if fail_sign or environment_override:
                with self.assertRaises(preview.PolicyError):
                    preview.sign(api, args)
            else:
                preview.sign(api, args)
        self.assertEqual([], list(runner.iterdir()))
        self.assertTrue(all(mode == 0o600 and not key.exists() for key, mode in seen_key))
        return root / "final"

    def test_only_the_four_public_output_files_survive_successful_signing(self):
        with tempfile.TemporaryDirectory() as directory:
            final = self.run_signing(directory)
            apk_name = f"txoka-pr-{PR_NUMBER}-{SHA[:12]}.apk"
            self.assertEqual({apk_name, "SHA256SUMS", "manifest.json", "README.txt"},
                             {path.name for path in final.iterdir()})
            metadata = json.loads((final / "manifest.json").read_text())
            self.assertEqual(preview.sha256_file(final / apk_name), metadata["sha256"])
            self.assertEqual(DIGEST, metadata["certificate_sha256"])

    def test_private_key_is_removed_when_signing_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            final = self.run_signing(directory, fail_sign=True)
            self.assertFalse(final.exists())

    def test_wrong_workflow_or_non_main_ref_cannot_reach_the_key(self):
        for change in ({"GITHUB_REF": "refs/pull/55/merge"},
                       {"GITHUB_WORKFLOW_REF": preview.SIGNER_WORKFLOW_REF.replace("sign-preview-apk", "fake")},
                       {"GITHUB_EVENT_NAME": "pull_request"}, {"GITHUB_REPOSITORY": "ml0renz0/txoka"}):
            with self.subTest(change=change), tempfile.TemporaryDirectory() as directory:
                final = self.run_signing(directory, environment_override=change)
                self.assertFalse(final.exists())


class PreviewDownloadTests(unittest.TestCase):
    def test_download_does_not_forward_github_token_to_blob_storage(self):
        target = "https://productionresultssa1.blob.core.windows.net/artifact?token=storage-token"

        class Storage(io.BytesIO):
            status = 200

        class Opener:
            requests = []

            def open(self, request, timeout):
                self.requests.append(request)
                if len(self.requests) == 1:
                    raise urllib.error.HTTPError(request.full_url, 302, "redirect", {"Location": target}, None)
                return Storage(b"zip data")

        api = preview.GitHub("private-github-token")
        api.opener = Opener()
        with tempfile.TemporaryDirectory() as directory:
            api.download(4567, Path(directory) / "candidate.zip")
        self.assertEqual("Bearer private-github-token", api.opener.requests[0].get_header("Authorization"))
        self.assertIsNone(api.opener.requests[1].get_header("Authorization"))

    def test_non_storage_or_http_redirects_are_rejected(self):
        for target in ("https://attacker.invalid/file", "http://example.blob.core.windows.net/file",
                       "https://user:pass@example.blob.core.windows.net/file"):
            class Opener:
                def open(self, request, timeout):
                    raise urllib.error.HTTPError(request.full_url, 302, "redirect", {"Location": target}, None)
            api = preview.GitHub("secret")
            api.opener = Opener()
            with self.subTest(target=target), tempfile.TemporaryDirectory() as directory:
                with self.assertRaises(preview.PolicyError):
                    api.download(4567, Path(directory) / "candidate.zip")


class PreviewToolEnvironmentTests(unittest.TestCase):
    def test_keystore_is_never_in_a_child_environment_and_password_is_explicit(self):
        environment = {"PREVIEW_KEYSTORE_B64": "private encoded key", "PREVIEW_KEYSTORE_PASSWORD": "password"}
        result = argparse.Namespace(returncode=0, stdout=b"ok")
        with patch.dict(os.environ, environment), patch.object(preview.subprocess, "run", return_value=result) as run:
            preview.command(["aapt2", "dump"])
            self.assertNotIn("PREVIEW_KEYSTORE_B64", run.call_args.kwargs["env"])
            self.assertNotIn("PREVIEW_KEYSTORE_PASSWORD", run.call_args.kwargs["env"])
            preview.command(["keytool"], signing_password=True)
            self.assertNotIn("PREVIEW_KEYSTORE_B64", run.call_args.kwargs["env"])
            self.assertEqual("password", run.call_args.kwargs["env"]["PREVIEW_KEYSTORE_PASSWORD"])


if __name__ == "__main__":
    unittest.main()
