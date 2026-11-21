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
Kover enforces a 95% line coverage gate over those layers; Compose screen flows and Android-only
adapters need manual checks, with focused rendering tests for mosaics and materials. The HTML
report is written to `app/build/reports/kover/htmlDebug/index.html`.

```sh
./gradlew :app:koverVerifyDebug :app:koverHtmlReportDebug
```

## Instrumented tests

`ReceivedContactDaoTest` covers ordering, match lookup and note/delete queries against an
in-memory Room database. `TxokaMigrationTest` exercises upgrades from versions 1 and 2 to version 3,
checking that fields, colours and notes survive and that appearance can then be stored.
`CardMosaicRenderingTest` checks that all 29 procedural and bundled SVG mosaics retain both inks
and that mask resolution stays bounded without changing the card aspect ratio.
`CardMaterialRenderingTest` adds six native material checks, including the 24-pane optical atlas,
source RGB identity, both tilt axes and separate mosaic/effect composition. The current repository
has 16 instrumented tests in total: six DAO, two migration, two mosaic and six material tests.
Their execution for this revision remains pending; compiling the test APK is not a device run.
Run them with a disposable emulator or device attached:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Gradle uninstalls the app and its data when the run finishes. Reinstall with
`./gradlew :app:installDebug` afterwards, or run the tests without touching the installed app:

```sh
./gradlew :app:installDebugAndroidTest
adb shell am instrument -w \
  es.igarciadelamo.txoka.test/androidx.test.runner.AndroidJUnitRunner
```

On 20 November 2026, all ten repository instrumented tests passed on an Android 15 (API 35)
Pixel 6 emulator using application sources at
[`3d7ce41`](https://github.com/igarciadelamo/txoka/commit/3d7ce41692e3cc064164e026289ed627ef9178da):
six DAO tests, two migrations to Room version 3, and two mosaic rendering tests covering all
29 designs and bounded mask resolution. A separate emulator flow confirmed that the full-screen
viewer stays open and paused after rotation. Physical NFC and motion response remain separate
device checks.

That dated result predates the new material and QR changes and is not a passing result for the
current revision. The historical JVM result was 248 passing tests in 38 suites at `3d7ce41`.

## Current verification — 21 November 2026

The current `app/build/test-results/testDebugUnitTest/TEST-*.xml` reports contain **280 tests in
43 suites**, with zero failures, errors or skips. The 21 pure material tests are included in that
total: seven artwork, four pane-atlas, three relief and seven texture tests.

| Area | Verified coverage | Current result |
| --- | --- | --- |
| Debug build and static checks | Debug app and instrumented-test APK compilation, Detekt, ktlint, 95% Kover gate and debug lint | Passed |
| Appearance codec | Twenty independent goldens, all 3,801,600 valid combinations, atomic fallback and unchanged 768/769-byte NDEF boundary | Passed in JVM suite |
| Material fields | Canonical 480 × 303 RGB identity at neutral coral/cyan, 24-pane atlas, bounded textures and anchored two-axis light | Passed in the 21 included material tests |
| QR vCard | Real ZXing version-38 UTF-8 symbol round trips at correction M/ECI 26, Unicode and STYLE, exact 2,048-byte cap and 2,049-byte rejection | Passed in JVM suite |
| QR-only profile | Save above the NFC cap, warning state and clearing a previously published HCE card | Passed in JVM suite |
| Browser demo | All 191 tests across 16 test files, including independent pixel QR decoding | Passed; separate from native tests |
| Native/browser material parity | Nine actual 480 × 303 renders: three neutral reference images byte-identical; all remaining tilt/default-palette channel differences at most 1 | Passed |
| Release build and lint | R8 release APK assembly and release lint | Passed |
| Android instrumentation | All 16 current repository cases | Pending device run |

Camera permission/denial and image-import flows, physical NFC interoperability, physical sensor
response and composed Android visual/accessibility checks remain pending. Neither JVM/Canvas
comparisons nor APK compilation establish these device results.

## NFC device checks

Sharing and receiving need two physical phones. [`NFC_DEVICE_TEST_PLAN.md`](NFC_DEVICE_TEST_PLAN.md)
is the checklist for them, with setup, the 30 cases and the results table to fill in.

### Appearance regression checks

The [V1 appearance contract](CARD_APPEARANCE.md) defines the golden bytes, strict rejection rules
and 768-byte budget. The JVM suite exercises these separately from the Android renderer. In
addition to the existing device checklist, verify the following before releasing this feature:

- New-to-new NFC exchange, old-to-new reception and new-to-old gradient fallback, including a
  maximum-sized style and long Unicode contact data.
- Save, restart, receive, update, delete/undo and database upgrade without losing appearance or notes.
- Both motion axes, calibration, portrait/landscape rotation, background/resume, sensor toggle and
  manual joystick. Open the viewer from the hub, received-card detail and editor; rotate while
  paused and confirm it remains open and paused. Compare small tilts on two physical phones;
  an emulator cannot prove sensitivity.
- All backgrounds and mosaics in airplane mode; no missing external assets. Confirm both flat
  inks, gradient visibility, holographic reflection and elemental effects. Check stained-glass
  texture with near-hairline joints, nacre's layered highlights and brushed metal's directional
  finish against the approved edited sources. Check the canonical coral/cyan neutral base raster
  against its bundled RGB asset, then review palette remapping with two colours and four corners.
  Tilt each axis and confirm changing light while texture coordinates remain anchored to the
  card. The three material backgrounds must have no added black legibility scrim. Review composed text,
  mosaics, effects and scaled screenshots separately from base-raster RGB identity.
- Both text targets, all finishes and sizes, long names/social-only cards, 200% font scale, TalkBack,
  light/dark themes and a system-disabled animation scale. Check contrast for chosen colours.
- Repeated colour-picker sessions: presets commit immediately; custom previews require Select;
  Cancel restores the opening colour; grayscale/black do not destroy the hue setting.
- Mosaic categories wrap and filter strictly, previews scroll vertically, the card stays visible
  while editing and the full-screen viewer works in landscape.

Do not infer physical NFC interoperability or visual quality from codec tests. Record device,
Android version and results rather than treating the browser demo as a native test run.

### QR regression checks

- Double-tap cards in the hub and received detail, and open the full-screen viewer. Confirm the
  180-degree Y-axis flip, the same selected style on the back, a centred black-and-white QR code,
  a clear four-module quiet zone and integer-size modules. Return with a double-tap or the visible
  button. Check TalkBack, 200% font scale, rotation and disabled system animation.
- Decode displayed QR codes on physical phones, including long Unicode fields and a maximum-size
  styled card. Confirm Txoka restores style; separately record what a native camera or other
  contact scanner imports, since it may ignore the appearance extension.
- Opening the scanner must not request camera permission. Choose **Use camera**, allow access and
  scan; repeat after denial. Image import through the system file picker must work without camera
  or storage permission, including on a device without a camera or NFC.
- Camera scanning and image import must work in airplane mode and reach confirmation before any
  contact is stored. Cancel and invalid/non-vCard images must not create contacts. Confirm queued
  cards and repeated-card updates behave like NFC, preserving private local notes.
- Save a card above the 768-byte NDEF cap but within the 2,048-byte UTF-8 QR cap. Confirm the editor
  warns about NFC, the previous HCE card is no longer served, and QR preserves every field and
  STYLE. A card above the QR cap must fail Save without truncation.
- Confirm the QR contains the contact and supported appearance but no received-contact note, NDEF
  wrapper or Android Application Record. Camera frames and imported raw images must not be saved
  or uploaded by the app.

## Store kit

`store/` holds what Play Console asks for: the 512 px icon, the feature graphic, the phone
screenshots per language and [`store/play-listing.md`](store/play-listing.md), with the listing
copy, the release notes and the App content answers. `store/screenshots/readme/` keeps the
downsized copies of the original overview images. The [appearance review gallery](store/screenshots/review/README.md)
keeps unedited full-resolution Android captures of the newer card UI. `docs/` is published by GitHub
Pages and is for the privacy policy only.
