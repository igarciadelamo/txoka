# Preview APKs for pull requests

The `build-apk` pull request label requests an installable **Txoka PR** build before merge. The
configured producer does not pass signing secrets to its build steps; a separate job signs the
unsigned APK. Repository-secret access relies on trusted collaborators, as described below.
This is an additional test app; production release signing is unchanged.

## Install identity and updates

| Property | Preview | Official release |
| --- | --- | --- |
| Application ID | `es.igarciadelamo.txoka.preview` | `es.igarciadelamo.txoka` |
| Launcher name | `Txoka PR` | `Txoka` |
| Version name | `1.0-preview` | `1.0` |
| Version code | `1` | `1` |
| Signing key | Dedicated preview certificate | Existing release certificate |
| Local profile and received cards | Preview app storage | Official app storage |

The preview build type inherits release optimisation and stays non-debuggable. Gradle always
leaves its APK unsigned, even if a developer has local release signing properties. A trusted job
signs it afterwards with the preview certificate.

Keep the same preview keystore for every build. Android can then replace an earlier preview,
including another APK with the same version code, while retaining its local data. The source
commit and run identity live in the artifact manifest rather than in an ever-increasing version
code. A newly generated preview key cannot update an installation signed with the old key.

The same version code does not order previews: choose the intended PR and source SHA explicitly.
Updates still require compatible app data. In particular, a preview from an older database schema
may not open data written by a newer PR; signing compatibility does not supply a reverse migration.

Installing a preview does not update, uninstall or migrate the official app. The two app IDs give
them separate Android storage. Do not supply the official release keystore or its password to the
preview workflows.

## Fictional received library

A fresh preview installation starts with eight fictional **received** cards. Their names identify
them as previews or examples, their email addresses use the reserved `example.com` domain, and
their notes say `Fictional preview contact`. They contain no phone numbers. Your own card stays
empty: the installer never writes the profile or share-settings DataStore and cannot turn a sample
into an outgoing NFC card.

The sample installer belongs only to the `preview` source set. A nonexported provider runs before
`Application.onCreate`, without accessing Hilt members, repositories or DataStore. It copies a
packaged Room database only when the received database, its SQLite sidecars and the one-time marker
are absent. Existing preview databases and their notes are always preserved, including when the
installer is first added by an update.

The marker lives outside the database in `noBackupFilesDir`. Deleting individual samples, emptying
the library or deleting its database does not repopulate it on the next launch. An interrupted or
failed installation also keeps the marker and skips later attempts. The complete copied file is
flushed and published with an atomic hard link that refuses an existing destination; an incomplete
copy never replaces user data. Uninstalling the preview or clearing all its app data resets this
one-time state.

`generatePreviewSamples` creates the fixture under `app/build/generated/previewSamples/assets`.
It reads the version declared by the checked-out `TxokaDatabase` and selects that exact exported
schema and Room identity hash, even when higher numbered schema files exist. The CI branch's Room
version 2 produces eight legacy palettes without an appearance dependency. Room version 3 adds
seven styled examples and one legacy card; stained glass, nacre and brushed metal are used only
when their immutable IDs 2, 3 and 4 exist in the compiled background enum. All geometry and material
settings remain local. Unknown schema versions fail fixture generation instead of guessing.

Neither the provider nor the fixture is included in official debug or release app APKs. The existing
device test command compiles the installer and generated fixture into the **instrumented test APK**
to check Room opening, repeat startup, edited notes, deletion and existing-database preservation in
isolated directories. Preview JVM tests also exercise interrupted copies, SQLite sidecars and a
destination appearing during publication:

```sh
python3 -m unittest discover -s scripts/ci/tests -p test_preview_samples.py -v
./gradlew :app:testPreviewUnitTest :app:connectedDebugAndroidTest
```

Local validation of this installer revision passed 187 debug JVM tests and 196 preview JVM tests
(including the nine installer tests), the required build/style gate, debug/release lint and debug
coverage verification. All 76 Python policy/generator tests passed with two expected skips for the
unavailable local Semgrep executable. Inspection of the built APKs confirmed that official debug
and release contain neither sample assets nor installer code, while preview and the instrumented
test APK contain the fixture. The three Room device lifecycle tests compile; execution is recorded
separately from these local results.

## One-time repository setup

Complete this in `igarciadelamo/txoka`. The workflows and signing script must be reviewed and merged
into `main` before they can sign a functional pull request.

A collaborator can add repository secrets to this personal repository without configuring an
environment. See GitHub's [secret permissions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets).

1. Generate and retain one dedicated PKCS12 preview keystore, using the alias `txoka-preview`.
2. In **Settings → Secrets and variables → Actions**, choose **New repository secret** for each:

   | Repository secret | Value |
   | --- | --- |
   | `PREVIEW_KEYSTORE_B64` | Base64 encoding of the complete preview PKCS12 file |
   | `PREVIEW_KEYSTORE_PASSWORD` | Password shared by its store and `txoka-preview` private key |

Repository secrets can also be requested by other workflows, including workflows changed in an
internal PR. Keeping this signer on `main` does not prevent another workflow from requesting them.
This setup assumes trusted collaborators with write access and uses a key dedicated to previews.
Never put the official release key or its password in these secrets. Fork PRs normally do not
receive repository secrets directly; the default-branch signer handles their eligible artifacts.

Back up the preview keystore and password securely so later previews can update previous
installations. Store neither in the repository, artifacts, issues, pull request descriptions nor
chat messages.

### Generate and upload the preview key

Run these commands on your trusted computer with JDK 21 and an authenticated `gh` CLI. The
commands upload secret values over standard input; they do not print the keystore or password.
The first `keytool` command asks for a password interactively. Use that same password when asked
below; PKCS12 uses it for both the store and key.

```sh
umask 077
mkdir -p "$HOME/keys"
keytool -genkeypair -storetype PKCS12 \
  -keystore "$HOME/keys/txoka-preview.p12" \
  -alias txoka-preview -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Txoka Preview, O=Txoka, C=ES"
```

Check the alias and save the **public certificate SHA-256 fingerprint** for comparison with future
artifacts. `keytool` asks for the store password; its certificate output is public metadata.

```sh
keytool -list -v -storetype PKCS12 \
  -keystore "$HOME/keys/txoka-preview.p12" -alias txoka-preview
```

Upload both repository secrets:

```sh
base64 < "$HOME/keys/txoka-preview.p12" | tr -d '\n' | \
  gh secret set PREVIEW_KEYSTORE_B64 --repo igarciadelamo/txoka

read -r -s -p 'Preview keystore password: ' txoka_preview_password
printf '\n'
printf '%s' "$txoka_preview_password" | \
  gh secret set PREVIEW_KEYSTORE_PASSWORD --repo igarciadelamo/txoka
unset txoka_preview_password
```

The password prompt uses Bash's `read -s`; launch Bash first if the local shell does not support it.
Keep shell tracing (`set -x`) disabled. No GitHub environment or deployment approval is required.

## Request and download a build

1. Merge the CI-only pull request into `main` first.
2. Update the functional pull request, including card appearance PR [#57](https://github.com/igarciadelamo/txoka/pull/57),
   from that `main` so its exact head contains the preview build type.
3. Add the `build-apk` label to that open pull request. Further commits request a new preview while
   the label remains present. The ordinary CI checks continue to run independently.
4. Open **Actions** and inspect the preview build. It checks out the full pull request head SHA,
   runs the quality and security gates and builds an unsigned APK without signing secrets. This
   run is named **Preview APK**.
5. After that run succeeds, open the following **Sign preview APK** run. It signs automatically
   after validating the current PR and candidate; its summary links to the signed artifact.
6. Download the signed artifact from that signing run. Check that its manifest names the expected
   pull request, full source SHA, build run and attempt before installing it.

Removing the label or closing the pull request makes it ineligible for signing. A later commit
makes an older build stale: the signer must validate the current head again and reject the older
artifact. Use the latest eligible run rather than assuming an earlier successful run represents
current source.

For a manual retry, choose **Re-run all jobs**, or remove and reapply `build-apk` to start a new
run. Re-running only failed jobs can leave an earlier attempt's candidate alongside a later
attempt's checks; the signer intentionally rejects that combination.

Each signed artifact is named `txoka-pr-<N>-<SHA12>-run<runID>-a<attempt>`, retained for **14 days**,
and contains four files:

| File | Purpose |
| --- | --- |
| `txoka-pr-<N>-<SHA12>.apk` | Installable Txoka PR build |
| `SHA256SUMS` | SHA-256 digest for the signed APK |
| `manifest.json` | Source repository, PR/head SHA, originating run/attempt and signing certificate identity |
| `README.txt` | Installation and verification instructions for that build |

Keep the four files together. Verify the APK checksum after extracting the artifact:

```sh
sha256sum --check SHA256SUMS
```

The checksum detects a mismatch between these downloaded files. It does not independently
establish that the code is safe: identify the signing run, reviewed source SHA and preview
certificate as well. Artifacts expire; a stable preview key permits a later build to update a
previous installation even after its original artifact has expired.

On Android, allow installation from the app used to open the APK when prompted. With Android
platform tools available, install or replace the preview without clearing its data:

```sh
adb install -r path/to/the-signed-preview.apk
```

Uninstalling `Txoka PR` removes its local data. Installing the official app does not import preview
profiles or received contacts.

## Security reports

The labelled **Preview APK** run includes the three security checks adapted from Prebostes Taraos:

| Scanner | Scope and report |
| --- | --- |
| Semgrep 1.182.0 | Local rules for native Kotlin/Java under `app/src`, excluding test source sets and generated code; the adapter checks that every selected file was scanned |
| OSV-Scanner 2.7.0 | Known dependency vulnerabilities against a CycloneDX inventory of the resolved `previewRuntimeClasspath`, including transitive dependencies; the downloaded scanner binary is SHA-256 checked |
| MobSF 4.5.3 | Static analysis of the same unsigned APK, with JSON, AppSec data, PDF and a summary; the uploaded and reported APK hashes must match the local input |

Findings are **report-only**, matching the initial Prebostes Taraos policy. Read the reports before
installing a build: a successful job means the scanner completed its expected scope, not that it
found no vulnerabilities. Engine failures, omitted required Kotlin files, empty or malformed
dependency inventories, APK hash/package mismatches and incomplete reports fail the run and block
signing. No scanner step uses `continue-on-error` to turn a failed analysis into a clean result.

MobSF runs in a temporary container on the GitHub runner, using
`opensecurity/mobile-security-framework-mobsf:v4.5.3` pinned to the image digest in the adapter.
Its API is bound to loopback and uses an ephemeral API key; third-party APK uploads are disabled.
The container is removed after the scan. This is static APK analysis; there is no dynamic Android
emulator analysis or claim of NFC testing in this job.

Download the security report artifacts from the **Preview APK** run and keep the run/source SHA
with them. Source reports are written under `security-reports/source/` (Semgrep JSON/SARIF,
summary, engine log and source/rule hashes). Dependency reports are under
`security-reports/dependencies/` (OSV JSON, log and summary); the CycloneDX inventory is
`app/build/reports/security/preview-sbom.cdx.json`. MobSF reports are under
`security-reports/apk/`, including `MobSF-report.json`, `MobSF-report.pdf` and `MobSF-summary.md`.

The source rules inspect selected native code for weak cryptography, process execution, JavaScript
bridges, raw file URIs, XML parser review points and embedded secret patterns. They do not provide a
repository-wide secret scan. OSV covers resolved Maven runtime components, including transitives;
the Android OS, Gradle/build tools and application source are outside that dependency inventory.

The existing JVM tests, 95% coverage gate, Detekt, ktlint and debug/release Android lint still run.
A separate **Device persistence tests** job creates a fresh API 35 emulator and runs
`connectedDebugAndroidTest`, including the DAO and database migration tests. Any additional
instrumented tests in the requested PR run there too. This command removes app data only from the
disposable CI emulator; it never targets a tester's installed app. Its reports are uploaded as
`preview-device-tests-<runID>-a<attempt>`. All six producer jobs must succeed before signing.
The signing adapter independently checks the final package ID, SDK levels, non-debuggable status,
allowed permissions, version, certificate and APK checksum.

The signer's fixed permission policy requires NFC and the preview app's generated
`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. It additionally allows `android.permission.CAMERA`
for pull requests that include the QR scanner; camera is optional, so the CI branch's NFC-only
preview remains valid. Missing required permissions, duplicate declarations, `INTERNET` and every
other permission are rejected. This allowlist comes from trusted signing code and cannot be expanded
by a pull request's manifest.

Its policy tests and the scanner adapter tests run locally without production keys:

```sh
python3 -m unittest discover -s scripts/ci/tests -v
```

To reproduce the actual scanners, use a Linux x86-64 host with Python, JDK 21, the Android SDK and
a working Docker daemon. Install the pinned Semgrep engine into a virtual environment outside the
repository, then run:

```sh
python3 -m venv "$HOME/.venvs/txoka-security"
. "$HOME/.venvs/txoka-security/bin/activate"
python3 -m pip install semgrep==1.182.0

python3 scripts/ci/source_security.py --output-dir security-reports/source
./gradlew --no-configuration-cache -I scripts/ci/dependencies.init.gradle :app:writePreviewSbom
python3 scripts/ci/dependency_security.py \
  --sbom app/build/reports/security/preview-sbom.cdx.json \
  --output-dir security-reports/dependencies
./gradlew :app:assemblePreview
python3 scripts/ci/apk_security.py \
  app/build/outputs/apk/preview/app-preview-unsigned.apk \
  --output-dir security-reports/apk
```

The OSV adapter downloads and checksum-verifies its pinned Linux AMD64 binary. The MobSF adapter
uses the pinned container image. These runs require network access for tool/dependency downloads
and vulnerability queries; the Txoka application itself still has no `INTERNET` permission.

## NFC during preview testing

The preview application ID is intentionally separate; the wire protocol is not. Its vCard fields,
NDEF MIME type, Android Application Record and HCE AIDs retain the official Txoka values. In
particular, the AAR still names `es.igarciadelamo.txoka`. Background dispatch may therefore open the
official app rather than Txoka PR.

For preview NFC checks, keep the intended app in the foreground on both phones and explicitly
choose **Send** on one and **Receive** on the other. Both installations advertise the same HCE
AIDs, so foreground service selection and each phone's NFC controller still need verification.
Co-installation is not proof that the intended HCE service won the routing decision.

NFC exchange requires two physical phones. An APK build, emulator run, manifest check or passing
protocol unit test does not establish that preview-to-preview or preview-to-official exchange
works. Record those device results in [`NFC_DEVICE_TEST_PLAN.md`](NFC_DEVICE_TEST_PLAN.md), including
which app was open, each direction and the receiving app. No official app data or NFC contract is
changed to make preview builds coexist.

## Signing workflow and repository trust

- `.github/workflows/preview-apk.yml` runs pull request code with read-only token permissions and
  does not map preview signing secrets to its build steps. It associates the unsigned candidate
  with the exact checked-out head and records the source SHA, run ID and attempt.
- `.github/workflows/sign-preview-apk.yml` is triggered by `workflow_run` from the trusted default
  branch. Its signing code comes from `main`, not the pull request.
- The signer validates the originating workflow/run/attempt, current open PR/head and label, and
  the APK's expected preview identity before using the repository secrets. A stale or unrelated
  artifact must fail closed.
- Fork PRs are supported. The signer resolves the current PR using GitHub's live repository,
  branch and exact-head metadata; the candidate's JSON does not decide which PR is authorized.
- Removing `build-apk` stops future eligible signing, and the signer checks the live state again
  before using the key and before publication. GitHub has no atomic label-check-and-upload
  transaction; withdrawing a label also cannot revoke an APK someone has already downloaded.
- The signing job does not check out pull request source, run Gradle from it, execute artifact
  contents or restore a pull request-controlled build cache. Downloaded APKs and manifests are
  treated as untrusted input.
- `scripts/ci/preview_apk.py` contains the validation and signing support. Keep changes to this
  trusted path under the same review discipline as workflows and repository-secret access.
- Build and signing jobs never use the official release key. Missing repository secrets must stop
  signing rather than silently use a different certificate.

The label authorizes preview builds for later commits while it remains attached. Source review
still matters: a collaborator can change a workflow to request repository secrets, and the
checks cannot prove that arbitrary code in a labelled PR produces a benign APK. Passing the
automated checks does not replace that review or the device plan.
