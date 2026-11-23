# Preview APKs for pull requests

Add the `build-apk` label to an open PR targeting `main` to request an installable **Txoka PR** APK. The preview installs alongside Txoka as `es.igarciadelamo.txoka.preview`, with its own profile and library. The PR supplies the app's version code. Its version name ends in `-preview`.

## Set up the signing environment

A repository administrator must configure this before enabling the signer.

1. In **Settings → Environments**, create or open **preview**. Under **Deployment branches and tags**, select **Selected branches and tags**. Add exactly one rule of type **Branch**, named `main`. Leave the tag rules and required reviewers empty. This restricts the ref while keeping signing automatic.
2. Under **Environment secrets**, set `PREVIEW_KEYSTORE_B64` to the base64-encoded preview PKCS12 keystore and `PREVIEW_KEYSTORE_PASSWORD` to its password. The expected alias is `txoka-preview`. Use the existing dedicated preview key if testers are updating an installed preview. Keep its private backup outside GitHub.
3. Before enabling builds, check **Settings → Secrets and variables → Actions → Repository secrets** and remove **both** same-named repository secrets if present. GitHub cannot display an existing secret's value: restore from the original backup and confirm the environment has both names before deleting the repository copies. While a repository copy exists, internal-branch workflows can request it. A missing environment secret can also fall back to a repository secret of the same name.
4. Verify the branch rule, the two environment secret names and the absence of their repository copies. Merge the reviewed workflows into `main`, then update the functional PR from that branch so its source contains the preview build type. Use only the dedicated test certificate. If the old key is known to have been disclosed, rotate it and reinstall the preview.

Labelling a PR authorizes building that PR's current head. The environment separately controls access to the key. Changes to trusted workflows on `main` must be reviewed with the same care as changes to the signer.

## Request, download and update

1. Apply `build-apk`. **Preview APK** checks out the exact PR head, tests it and uploads an unsigned candidate. With the label present, new code changes request another build. Changes confined to documentation skip the expensive jobs. To request a fresh build of a PR containing code after a docs-only update, remove and reapply the label.
2. Open the subsequent **Sign preview APK** run in Actions. Its summary links to the signed artifact, retained for 14 days. Check the PR, full source SHA, run ID, attempt and certificate fingerprint in `manifest.json`. Extract the ZIP and run `sha256sum --check SHA256SUMS` in its directory.
3. Open the APK on Android, or install with `adb install -r path/to/txoka-pr.apk`. Android can update a preview signed with the same key at the same or a higher version code. A lower code is a downgrade. Switching between branches can also switch database schema versions.

Removing the label or closing the PR withdraws eligibility. A new head invalidates an older candidate. To retry a failed build of the current head, rerun **all jobs** so the candidate and checks belong to the same attempt.

## What gets signed

- The producer checks the label, builds the unsigned release-derived preview, runs unit, lint and device checks, and records its APK hash and identity in `source.json`. The variant is non-debuggable, including when local release signing properties exist.
- The signer's `workflow_run` executes the trusted script at the workflow SHA on `main`. Only the `sign` job uses the `preview` environment. It never runs the PR's Gradle or scripts.
- The signer obtains the live PR, head SHA, originating workflow, run, attempt and artifact identity from GitHub. It compares the downloaded archive and APK hashes, package, app label, SDK, permissions, debug flags, version code and preview version name with policy and candidate metadata. The version code is read directly from the APK and matched against `source.json`, so a different version on `main` cannot invalidate an otherwise eligible build.
- Before upload, the signer checks that the PR, label, head and artifact are still eligible. The resulting artifact contains the signed APK, checksums, manifest and installation instructions.

## Test the preview on a phone

A fresh preview seeds eight fictional **received** cards once. Existing cards and edited notes survive an update. Removing samples does not repopulate them. The preview source set creates the fixture from the checked-out Room schema. The tester's outgoing profile and share settings are separate.

In **Settings**, the preview and debug variants expose actions to simulate one incoming card or two queued cards. They exercise the receive confirmation flow without NFC hardware. Release keeps these actions hidden.

For a physical NFC check, open the intended app on each phone and choose **Send** on one and **Receive** on the other. Preview and official builds share the protocol and HCE AIDs. The Android Application Record still names the official app, so background dispatch may open Txoka. Record physical tests in [the device plan](NFC_DEVICE_TEST_PLAN.md).

## Dependency vulnerability reports

The separate **Dependency security** workflow scans a CycloneDX inventory of the resolved preview runtime dependencies with OSV. It runs on dependency-input changes, on Mondays at 06:23 UTC, or on request. The downloadable report includes the transitive dependencies it checked and any known vulnerability IDs. Scanner failures remain visible in that workflow and do not prevent a signed preview.
