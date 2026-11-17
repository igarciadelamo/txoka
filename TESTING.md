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
