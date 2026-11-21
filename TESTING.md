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
in-memory Room database. It also checks transactional clear/restore with the original contact IDs,
preservation of a contact arriving after clear, and complete batch rollback on an ID conflict.
`TxokaMigrationTest` exercises upgrades from versions 1 and 2 to version 3,
checking that fields, colours and notes survive and that appearance can then be stored.
`CardMosaicRenderingTest` checks that all 29 procedural and bundled SVG mosaics retain both inks
and that mask resolution stays bounded without changing the card aspect ratio.
`CardMaterialRenderingTest` adds six native material checks, including the 24-pane optical atlas,
source RGB identity, both tilt axes and separate mosaic/effect composition. The repository now
has **18 instrumented tests**: eight DAO, two migration, two mosaic and six material tests. Their
execution against the new library and immersive-viewer revision is pending.

For the preceding material/QR revision on 21 November 2026, all 16 tests then present passed on an
Android 15 / API 35 Pixel 6 emulator at app source
`dd9c3a5`, together with one separate screenshot-capture flow. The raw AndroidJUnitRunner log
records 17 passes, zero failures and zero errors; the XML summary is derived from that log.
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

## Library and immersive-viewer verification — 21 November 2026

The latest local JVM reports for the library revision contain **334 tests in 49 suites**, with
zero failures, errors or skips. The 95% Kover gate, debug app and instrumented-test APK compilation,
debug/release lint, R8 release assembly, Detekt and ktlint passed in the final combined gate.
These local results do not establish that the new Compose flows work on an Android device.

The new regression coverage exercises:

- Private category metadata surviving a reopened DataStore, category and card ordering, moves,
  favorites, view mode, orphan pruning and concurrent edits without lost updates.
- A bounded, versioned metadata codec: malformed and oversized input, UTF-8 byte limits, nesting,
  invalid IDs and dangling assignments. Organization changes leave profiles, private notes and
  NFC/QR payloads intact; Room stays at schema version 3.
- Transactional clear and atomic batch restore with stable contact IDs. ViewModel cases cover
  two queued Undo requests, retrying metadata after contacts have already been restored, and
  retaining a recoverable Undo when metadata removal and contact rollback both fail.
- Search, favorites/recent filters, sender updates, category order and deletion cleanup without
  losing notes or unrelated received cards.

The separate browser demo passes **211 Node cases in 19 test files**, including model and simulated
DOM interactions and actual Canvas comparisons for all 29 mosaics over the three new materials.
Published version 20 was checked by comparing eight public resources byte for
byte with its source. This is source/publication verification, not a visual browser review or an
Android test run.

Execution of all 18 repository instrumented cases and the new native library/viewer capture flow
remains pending. The eleven images and 17 runner passes below belong to `dd9c3a5`; they do not
validate the later CoverFlow, organizer, rotation-retention or immersive-layout changes.

## Material and QR baseline verification — 21 November 2026

At app source `dd9c3a58464373776391f27a49dc8a3abc522a93`, the recorded
`app/build/test-results/testDebugUnitTest/TEST-*.xml` reports contained **280 tests in 43 suites**,
with zero failures, errors or skips. The 21 pure material tests are included in that
total: seven artwork, four pane-atlas, three relief and seven texture tests.

| Area | Verified coverage at `dd9c3a5` | Recorded result |
| --- | --- | --- |
| Debug build and static checks | Debug app and instrumented-test APK compilation, Detekt, ktlint, 95% Kover gate and debug lint | Passed |
| Appearance codec | Twenty independent goldens, all 3,801,600 valid combinations, atomic fallback and unchanged 768/769-byte NDEF boundary | Passed in JVM suite |
| Material fields | Canonical 480 × 303 RGB identity at neutral coral/cyan, 24-pane atlas, bounded textures and anchored two-axis light | Passed in the 21 included material tests |
| QR vCard | Real ZXing version-38 UTF-8 symbol round trips at correction M/ECI 26, Unicode and STYLE, exact 2,048-byte cap and 2,049-byte rejection | Passed in JVM suite |
| QR-only profile | Save above the NFC cap, warning state and clearing a previously published HCE card | Passed in JVM suite |
| Browser demo | All 191 tests across 16 test files, including independent pixel QR decoding | Passed; separate from native tests |
| Native/browser material parity | Nine actual 480 × 303 renders: three neutral reference images byte-identical; all remaining tilt/default-palette channel differences at most 1 | Passed |
| Release build and lint | R8 release APK assembly and release lint | Passed |
| Android instrumentation | Six DAO, two migration, two mosaic and six material cases, plus one separate capture flow | Passed: 17 actual runner cases on Android 15 / API 35, Pixel 6 emulator |
| Native QR screenshots | Independent jsQR decoding of the raw hub, portrait-viewer and landscape-viewer screens | Passed: the same 220-byte vCard, contact, colours and `X-TXOKA-STYLE:1:A2gB` in all three |

The [material and QR evidence gallery](store/screenshots/materials-qr/README.md) contains eleven
unedited full-resolution Android screenshots and the raw runner evidence. Its manifest pins the
app source above and QA capture revision `792d50cd50e3252f4d01283dc73ad1e12dd82240`. Recorded probes
confirm that the QR remains visible after landscape rotation and pause survives both that rotation
and the return to portrait. Screenshot QR decoding used the whole raw screens, without cropping,
resizing or replacing the displayed code.

Camera permission/denial and SAF image-import flows on real devices, physical NFC interoperability,
physical sensor response and wider composed visual/accessibility checks remain pending. The emulator
run and its captured scenes do not establish these remaining device results.

## Library and landscape regression checks

Record device, Android version, theme, font scale and animation setting for the following checks.
Use fictional received cards with different materials and at least two cards per category.

- Swipe CoverFlow in both orientations, including a fast swipe, then use Previous/Next. Confirm
  snapping, visible angled neighbors, the centered card and its accessible name. The backdrop
  must interpolate between different category colors and stay stable within one category.
- Switch between CoverFlow, category shelves and list. Search and favorites/recent filters must
  retain the complete underlying library. Open the active card and verify its original material,
  mosaic, text and contact details; neighboring cards and thumbnails must remain static.
- Create and edit a category's name, icon, flat color and two-color gradient. Reorder categories,
  move cards and reorder them with both drag handles and buttons. Deleting a category must leave
  its cards available without a category. Restart and verify all confirmed organization changes.
- Receive an update to an existing contact and edit its note. Its ID, category, manual position
  and favorite state must remain attached. Clear the library and Undo; repeat with a new arrival
  after clear and confirm neither that card nor restored notes/appearance are lost. Exercise
  storage-failure and Undo-retry paths without duplicate restored IDs.
- Rotate with a received-card viewer open on its front and QR back, while paused. The selected
  card, visible face and pause must survive. Rotate with an unsaved category draft and custom
  color selection; preserve the draft without committing it on cancellation.
- In landscape, the immersive viewer must use the largest card that fits the available height
  and width while retaining the physical width/height ratio, approximately 1.586. Its 112 dp
  control rail contains QR, Close and movement controls; neither controls nor joystick may cover
  the card. Verify display cutouts, transient system bars, small windows and restoration of
  system-bar behavior after closing. CoverFlow uses an inset-aware lateral rail in landscape.
- Check 48 dp targets, TalkBack descriptions and ordering, light/dark themes, 200% font scale and
  disabled system animations. Only the active, uncovered card should animate or read sensors;
  shelves, neighbors and organizer thumbnails stay still. Test real sensor sensitivity separately.

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
keeps unedited full-resolution Android captures of the newer card UI. The
[materials and QR evidence gallery](store/screenshots/materials-qr/README.md) records the historical
`dd9c3a5` emulator run and its eleven raw captures. `docs/` is published by GitHub Pages and is for
the privacy policy only.
