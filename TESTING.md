# Testing and building

The commands, the signing setup and the manual checks the emulator cannot cover.

## Build and checks

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew detekt ktlintCheck
./gradlew :app:lintDebug
./gradlew :app:koverVerifyDebug
./gradlew :app:assembleRelease :app:lintRelease
```

The Gradle daemon JVM is pinned to 21 through `gradle/gradle-daemon-jvm.properties`, because
Detekt 1.23.8 cannot parse the version string of newer JDKs. CI runs the same tasks on every push
to `main` and on every pull request, including the release build so R8 stays verified.

## Preview builds and CI security checks

The `preview` build type uses release optimisation, stays non-debuggable and always leaves signing
to a separate trusted job. Its application ID is `es.igarciadelamo.txoka.preview` and its launcher
name is **Txoka PR**, so it installs alongside the official app with separate local data.

```sh
./gradlew :app:assemblePreview :app:testPreviewUnitTest :app:lintPreview
python3 -m unittest discover -s scripts/ci/tests -v
```

The Python suite exercises the signing policy and scanner failure handling without real signing
secrets or a running scanner. Its fixtures cover stale heads, missing labels, incorrect artifacts,
archive and identity validation, incomplete scans and malformed reports. The scanner jobs also
run the actual engines when an open PR has the `build-apk` label:

| Check | Input | Policy |
| --- | --- | --- |
| Semgrep 1.182.0 | Native Kotlin/Java source sets under `app/src`, excluding tests and generated code, with local rules | Findings are reported; engine errors and omitted selected source files fail the job |
| OSV-Scanner 2.7.0 | CycloneDX inventory of `previewRuntimeClasspath` | Findings are reported; an empty or malformed inventory and scanner errors fail the job |
| MobSF 4.5.3 | The unsigned preview APK in a local ephemeral container | Findings are reported; upload/report hash mismatches, incomplete reports and engine errors fail the job |

This initially follows the report-only finding policy used in Prebostes Taraos. Scanner completion
is required before signing; the presence of findings is visible in the reports and does not itself
block the preview. A completed scan does not establish that an APK is free of vulnerabilities.

The labelled workflow also executes the instrumented DAO and migration suite on a fresh API 35
emulator. `connectedDebugAndroidTest` removes app data only on that disposable runner. This job
does not test physical NFC exchange. Retry with **Re-run all jobs** so the candidate and its
checks share the same run attempt.

The ordinary **Build, test and lint** check remains separate. Preview request, repository-secret
setup, certificate persistence, report locations and the download manifest are documented in
[`PREVIEW_APK.md`](PREVIEW_APK.md). The configured build steps do not receive the preview key;
other workflows, including internal PR workflow changes, can request repository secrets. This
setup relies on trusted collaborators. Neither these jobs nor their tests use the official release key.

## Release signing

The release build reads `keystore.properties` from the project root. Neither that file nor the
keystore is committed, and neither should live inside the repository: keep both elsewhere and backed
up twice offline, because every future upload to Play has to be signed with the same upload key.

Create the upload key once (PKCS12, RSA 4096, valid well past the 2033 Play requirement):

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
mkdir -p "$HOME/keys"
"$JAVA_HOME/bin/keytool" -genkeypair -v -storetype PKCS12 \
  -keystore "$HOME/keys/txoka-release.p12" -alias txoka \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Isidro Garcia, O=Txoka, C=ES"
```

Then `keystore.properties` in the project root. The value is a literal path: `.properties` files do
not expand `~` or `$HOME`, so write the absolute path, or a path relative to the project root:

```properties
storeFile=/absolute/path/to/txoka-release.p12
storePassword=…
keyAlias=txoka
keyPassword=…
```

Record the fingerprints once, in case a future integration needs the certificate:

```sh
"$JAVA_HOME/bin/keytool" -list -v -keystore "$HOME/keys/txoka-release.p12" -alias txoka
```

With Play App Signing this key is only the *upload* key: Google keeps the app signing key, so a lost
upload key costs a reset request, not the listing.

Publish the bundle instead of the APK:

```sh
./gradlew :app:bundleRelease
"$JAVA_HOME/bin/jarsigner" -verify app/build/outputs/bundle/release/app-release.aab
```

The signed bundle is `app/build/outputs/bundle/release/app-release.aab`. Without
`keystore.properties` the release build still runs and writes an unsigned bundle, which is what CI
checks.

## Unit tests and coverage

The JVM suite covers the protocol, the profile logic, the NFC responder and every view model.
Kover enforces a 95% line coverage gate over those layers; Compose screens and the Android-only
adapters are covered by manual checks instead. The HTML report is written to
`app/build/reports/kover/htmlDebug/index.html`.

```sh
./gradlew :app:koverVerifyDebug :app:koverHtmlReportDebug
```

## Instrumented tests

`ReceivedContactDaoTest` covers ordering, match lookup and note/delete queries against an
in-memory Room database. `TxokaMigrationTest` writes a version 1 database by hand, opens it with
version 2 and checks that the stored cards survive the migration. Run them with the emulator or
device attached:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Gradle uninstalls the app and its data when the run finishes. Reinstall with
`./gradlew :app:installDebug` afterwards, or run the tests without touching the installed app:

```sh
./gradlew :app:installDebugAndroidTest
adb shell am instrument -w \
  -e class es.igarciadelamo.txoka.data.received.ReceivedContactDaoTest \
  es.igarciadelamo.txoka.test/androidx.test.runner.AndroidJUnitRunner
```

## NFC device checks

Sharing and receiving need two physical phones. [`NFC_DEVICE_TEST_PLAN.md`](NFC_DEVICE_TEST_PLAN.md)
is the checklist for them, with setup, the 30 cases and the results table to fill in.

## Store kit

`store/` holds what Play Console asks for: the 512 px icon, the feature graphic, the phone
screenshots per language and [`store/play-listing.md`](store/play-listing.md), with the listing
copy, the release notes and the App content answers. `store/screenshots/readme/` keeps the
downsized copies the README embeds, so the project page does not load the full-size ones. `docs/`
is published by GitHub Pages and is for the privacy policy only.
