# AGENTS.md

Context and working agreement for AI agents contributing to Txoka. Read this before touching code.
If something here disagrees with the repository, trust the repository and update this file.

## What this is

Txoka is a native Android app that shares a contact card over NFC or QR and keeps the cards
received. It is local-first: no backend, no account and no `INTERNET` permission.

- Kotlin + Jetpack Compose + Material 3, single Gradle module.
- Hilt for DI, coroutines/Flow, Room for received contacts, DataStore for profile/settings and
  private library organization, libphonenumber for phone handling, Manrope as the bundled font.
- Packages: `ui/`, `domain/`, `data/`, `nfc/`, `di/`.
- Version 1.0 (`versionCode = 1`). Room database version 3; the schema is exported to
  `app/schemas/` and committed, and every version bump must ship a real `Migration` (release builds
  have no destructive fallback).

## Current status (November 2026)

Milestones 0–6 and the polish phase are merged. Historical results for the preceding appearance
baseline at `3d7ce41` are recorded in `TESTING.md`.

- Unified app bar, hub/card actions, accessibility pass, string audit and licensing are done.
- The old `NoSuchMethodError runBlockingK` was a version split: `kotlinx-coroutines-test` came in at
  1.11.0 while the app runtime resolved `kotlinx-coroutines-core` 1.9.0 (Room's BOM), and AGP's
  consistent resolution pushed the app version into the test APK. `kotlinx-coroutines-core` is
  now declared explicitly at the catalog version.
- Card appearance V1 is described in `CARD_APPEARANCE.md`; physical device validation remains
  required before release.
- The material/QR baseline at `dd9c3a5` adds stained glass, nacre and brushed metal plus offline QR
  sharing, camera scanning and QR image import. On 21 November 2026, all 280 JVM tests in 43 suites
  passed, including 21 material tests and real ZXing QR boundary tests. Debug app/test APK compilation,
  Detekt, ktlint, the 95% Kover gate, debug/release lint and R8 release APK assembly passed.
  On the same date, all 16 repository instrumented tests plus one capture flow passed on an
  Android 15 / API 35 Pixel 6 emulator at app source `dd9c3a5`. Eleven raw screenshots and
  independent decoding of the hub and both viewer QR backs are recorded in the
  [native evidence gallery](store/screenshots/materials-qr/README.md). Camera permission/denial
  and SAF image import on real devices, physical NFC and sensors, and wider visual/accessibility
  checks remain pending. Historical results in `TESTING.md` identify the earlier revision they cover.
- The library adds Cover Flow, category shelves and a list fallback; categories, card order,
  favorites and view mode live in a separate private DataStore. The viewer maximizes landscape
  card height with immersive system bars and side controls. At `d9c1c01` on 22 November 2026,
  all 334 JVM tests in 49 suites, the 95% Kover gate, debug app/test APK compilation, both lint
  variants, R8 release assembly, Detekt and ktlint passed.
- `d9c1c01` also corrects production `ALPHA_8` SVG composition: the palette shader is painted
  through mask coverage, leaving uncovered material pixels untouched. The ARGB path remains
  available and the painter state resets before subsequent layers. The repository has 19 permanent
  instrumented cases, including a 20 SVG × 3 material × 3 pose matrix. All 19 plus the separate
  capture flow passed on API 35 in run `45043218637`: 20 cases, 22 raw screenshots and five
  independently decoded QR screens. The [library evidence gallery](store/screenshots/library/README.md)
  pins app source `d9c1c01` and capture source `bca1af283ab273494d727859a53cb2629529fc9e`.
  The correction changes rendering only, without NFC/QR fields or bytes. Physical NFC, sensors,
  camera/import flows and wider visual/accessibility checks remain separate device work.
- The separate demo passes 216 Node cases in 20 files, including five Canvas resource/recovery
  regressions. On published version 22, Chromium passed 120 SVG/material/selection-order
  combinations and both tilt axes; a controlled Canvas allocation failure recovered without a
  page reload. These browser checks do not establish behaviour in Brave on a physical phone.
- Remaining backlog: vCard-file sharing and SAF export/import of the received library.

## Non-negotiable rules

1. Do not add `INTERNET` or any further permission without asking the user first. The user explicitly
   authorised `CAMERA` for optional QR scanning; request it only after **Use camera**. QR image
   import through the system file picker requires no camera or storage permission.
2. Do not invent Android/NFC behaviour. Anything that needs real hardware must be verified on a
   device before it is claimed as working.
3. One branch per feature/fix. Never commit directly to `main` and never merge PRs yourself.
4. Commit author must be the project owner. Use the identity already in the history:
   `git -c user.name="Isidro Garcia" -c user.email="$(git log -1 --format=%ae)" commit ...`
5. Code, comments, commit messages, README and this file are in English. UI strings are Spanish
   and English (`values/` + `values-es/`).
6. Minimise comments; prefer clear identifiers. Use Conventional Commits (`feat:`, `fix:`,
   `chore:`, `refactor:`, `test:`, `docs:`), scoped when useful (`feat(profile): ...`).
7. Ask one crisp question when a design decision is ambiguous, then implement end to end.

## Commands

    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

    ./gradlew :app:assembleDebug :app:testDebugUnitTest detekt ktlintCheck
    ./gradlew :app:installDebug
    ./gradlew :app:lintDebug
    ./gradlew :app:assembleRelease :app:lintRelease   # R8 path; unsigned without keystore
    ./gradlew :app:bundleRelease                     # what gets published, if signed
    ./gradlew :app:connectedDebugAndroidTest   # uninstalls the app and wipes its data

Instrumented tests without wiping the installed app:

    ./gradlew :app:installDebugAndroidTest
    "$HOME/Library/Android/sdk/platform-tools/adb" shell am instrument -w \
      -e class es.igarciadelamo.txoka.data.received.ReceivedContactDaoTest \
      es.igarciadelamo.txoka.test/androidx.test.runner.AndroidJUnitRunner

Run `assembleDebug + testDebugUnitTest + detekt + ktlintCheck` before every commit.

## Architecture

Context lives in three documents: `README.md` is the summary, `ARCHITECTURE.md` the design, the
protocol and the accessibility rules, and `TESTING.md` the build, signing and test commands.

- `ui/share/ShareScreen.kt` + ViewModel — hub: app bar actions, hero card with edit/expand actions above it, NFC
  module, privacy footer.
- `ui/profile/ProfileEditorScreen.kt` + ViewModel — create/edit contact fields and layered appearance,
  with a pinned preview, motion controls, mosaic grid and independent text settings.
- `ui/profile/CardColourPicker.kt` — preset colours commit immediately; custom HSV/HEX previews stay
  local until confirmed. `ColourDraft` preserves hue through grayscale and black.
- `ui/received/` — list, detail, notes, delete, add to system contacts, and "empty library" with a
  confirmation dialog and an undo snackbar; ViewModels combine contacts with private organization
  and serialize library edits and recoverable clear/undo phases.
- `ui/library/` — Cover Flow, category shelves, list controls and the category/card organizer;
  only the centered card animates and uses motion sensors. Neighbor cards and thumbnails are static.
- `ui/receive/ReceiveConfirmScreen.kt` + ViewModel — confirmation bottom sheet after a tag is read.
- `ui/settings/` — device unlock setting, library, About, no separate licences screen.
- `ui/components/Layout.kt` — `Eyebrow`, `TxokaAppBar`, `ScreenColumn`, shared buttons.
- `ui/components/ContactCard.kt` — hero/preview card and `Profile.cardContentColor()`.
- `ui/components/CardMaterialLayer.kt`, `CardMotion.kt`, `CardViewer.kt` and `artwork/` — native
  appearance, lifecycle-aware sensors/manual tilt, full-screen viewing and local vector masks.
- `ui/components/CardQrFlip.kt` — a 180-degree Y-axis card flip, retaining the chosen style on the
  back and centring a black-and-white QR code with a four-module quiet zone and integer pixels.
- `ui/qr/` — optional CameraX scanning and system-file-picker image import, decoded locally with
  bundled ZXing and passed to the existing receive confirmation queue.
- `ui/components/Fields.kt` — `TxokaField`, `CountryChip`.
- `ui/theme/` — `Color.kt`, `Theme.kt`, `Tokens.kt` (`Spacing`, `Corners`), `Type.kt`.
- `ui/navigation/` — serializable routes and `TxokaNavHost`.
- `domain/protocol/` — pure vCard 3.0, QR vCard and NDEF encoding, Type 4 Tag APDU state machine.
- `domain/profile/` — `Profile`, palettes, phone helpers, contrast helpers, repository interfaces.
- `domain/received/` — received contact model and repository interface.
- `domain/library/` — private categories, organization, view modes, restore snapshots and repository interface.
- `nfc/` — `HceCardPublisher`, `HceResponder`, `NdefHceService`, `NfcReader`, `IncomingCards`,
  `FakeTransport`.
- `data/profile/` — DataStore profile and share-settings repositories.
- `data/received/` — Room entity, DAO, database, migrations and repository.
- `data/library/` — bounded versioned organization codec and atomic DataStore updates in the separate
  `library_organization` preferences file; no Room schema change.
- `di/` — Hilt modules.

## Data model and protocol

- `Profile(name, phone?, email?, linkedin?, instagram?, card)`. The name and at least one of the
  four detail fields (phone, email, LinkedIn, Instagram) are mandatory in the editor. **Every field
  that is present is shared**: there are no per-field sharing flags. Removed
  `sharePhone`/`shareEmail` in PR #16. Social handles are stored bare (`ana.ruiz`) and travel as
  standard `URL` properties.
- Received contacts store the sender's name, phone, email, LinkedIn and Instagram handles, palette,
  note and timestamp.
- Categories (name, icon, flat/two-colour background), category/card ordering, assignments, favorites
  and the chosen library view are private local metadata keyed by received-contact ID. They never
  enter profiles, STYLE, NFC or QR. Existing contacts start uncategorized and `COVER_FLOW` is the
  default view. Deleting a category unassigns its cards without deleting them. Pruning removes only
  orphaned contact metadata. Room remains version 3 with no organization migration.
- Empty-library reads and deletes contacts in one Room transaction; Undo restores original IDs in
  an atomic batch so metadata remains attached. Serialized ViewModel operations track contact and
  metadata restoration separately, allowing a metadata retry without inserting the contacts twice.
- A repeated card matches on normalised phone (digits only), lowercased email or lowercased social
  handle. Saving it refreshes the sender's fields and bumps `receivedAt`, keeps the local note and
  returns `SaveResult.Updated`; the confirm sheet shows "You already have …" and "Update contact".
  Same-name-only cards are stored separately. Merging needs exactly one match: if the phone points
  at one stored contact and the email at another, the card is stored as a new one instead of
  overwriting either, and `match()` reports no owner.
- `ProfileRepository.delete()` clears only the `profile.*` DataStore keys; it keeps share settings
  and received contacts. The hub falls back to its empty state and HCE stops publishing.
- Payload: vCard 3.0 plus `X-TXOKA-PROTOCOL`, `X-TXOKA-CARD-ACCENT`, `X-TXOKA-CARD-ACCENT2`, and a
  trailing Android Application Record naming the package so the reader opens Txoka instead of a
  chooser. `X-TXOKA-STYLE` optionally carries appearance. A missing protocol means version 0. The
  complete NDEF message is capped at 768 bytes, including MIME framing and AAR; Type 4's two-byte
  NLEN is outside that cap.
- QR carries the plain UTF-8 vCard and its STYLE, without NDEF or AAR. Its independent cap is
  2,048 UTF-8 bytes at error correction M. Save can retain a QR-only profile above the NFC cap;
  the editor warns, and HCE clears any previous card instead of publishing stale contact data.
  Exceeding the QR cap blocks Save. Never truncate fields or drop style to fit either transport.
- QR scanning asks for camera access only after **Use camera**, or reads a user-selected image
  through the system file picker. Frames and images are decoded on device, without raw-image
  persistence or upload. Saving a decoded contact still requires confirmation; private received
  notes are never included in the shared vCard.
- HCE AIDs: NDEF Forum `D2760000850101` and Txoka `F054584F4B4101`.
- `IncomingCards` is a queue, oldest first: the confirmation sheet shows the head, adds how many
  cards wait behind it, and ignores an identical card already queued. `save()`/`discard()` remove
  exactly the card the user acted on, so a stale tap cannot swallow the next one.
- Phone region fallback: SIM → network → locale → ES.

## UI and design system

- Theme follows the system light/dark mode. Forcing light mode was explicitly rejected; do not
  reintroduce it without asking.
- One `TxokaAppBar`:
  - `title == null` → "Txoka" wordmark (hub).
  - `title != null` → back arrow + screen title (secondary screens).
  - Surface background, 1 dp hairline (`HairlineLight = 0x3D171614`,
    `HairlineDark = 0x33FFFFFF`) and an 8 dp, 7%-black shadow.
- Hub: no bottom quick actions. Edit and expand actions sit in the card section heading, leaving
  its initials and content unobstructed. A contacts icon (no numeric badge) sits in the app bar left of Settings.
- The hub sharing module is a two-state control (Send / Receive), not an on/off switch, because
  "stop sending" means "start receiving". The role also picks the NFC mechanism: Receive reads with
  `enableReaderMode` (needed on vendor builds that never dispatch NDEF intents to third-party apps),
  Send emulates with foreground dispatch and the preferred HCE service. The role is session only, in
  memory, and resets to Send.
  After a card arrives while receiving, the phone offers its own for 60 s with a Cancel action and a
  live countdown. The window runs on the real clock, so it keeps counting while the app is in the
  background and the countdown only shows the time that is actually left.
- Editor: back discards (there is no Discard button), the footer is a single full-width Save, and
  "Delete card" lives in the app-bar overflow menu with a confirmation dialog.
- Legacy cards use a two-colour gradient and WCAG foreground. Appearance V1 adds background,
  mosaic, effect and independent name/contact text layers. Keep assets offline and STYLE compact;
  never send artwork, sensor values or renderer settings. See `CARD_APPEARANCE.md`.
- Background wire IDs are fixed: 0 gradient, 1 hologram, 2 stained glass, 3 nacre, 4 brushed metal,
  6 flat. IDs 5 and 7 reject STYLE atomically and retain the complete contact with legacy colours.
  The three materials use shared local 480 × 303 RGB assets (`glass-color.rgb`, `nacre-color.rgb`,
  `metal-color.rgb`) from direct edits of the approved reference, with text/UI removed and glass
  joints close to hairline width. Palette remapping preserves texture and highlight structure. Two local
  unsigned eight-bit relief maps support normals; `glass-cells.u8` assigns the 24 canonical panes
  their optical orientations without redrawing joints. Texture coordinates stay anchored while
  relative lighting responds to both tilt axes. These materials have no added black legibility scrim. The
  reference, edited PNGs and provenance live in `store/material-sources/`. Asset bytes, normals
  and lighting stay local; the fixed header, optional blocks, NFC permissions, AIDs, APDUs and
  package name remain unchanged.
- Double-tap a card to show its QR back or return to its front. Keep the visible **Show QR** button
  and accessibility action so the gesture is optional. NFC and camera hardware are optional;
  library and QR image import must remain usable without them.
- Library views are Cover Flow, category shelves and list. Cover Flow interpolates the category
  background between neighboring cards during a swipe; cards in one category keep that background
  stable. The organizer edits category names/icons/colours, orders categories and cards, moves cards
  and toggles favorites. Drag handles have button alternatives. Keep the selected viewer card, QR,
  pause state and category draft across rotation. Pause covered cards and honor reduced motion.
- Accessibility baseline: 48 dp targets, content descriptions on icon-only buttons, merged
  semantics on cards and list rows, contrast checked by tests.
- Only `material-icons-core` is available; `material-icons-extended` is not a dependency.
- Landscape and wide windows: ordinary content uses `Modifier.contentWidth()` (`ContentMaxWidth` = 560 dp)
  inside a horizontally centred column. The library stage and full-screen viewer are deliberate
  exceptions: maximize available card height while keeping the physical ID-1 ratio (about 1.586).
  Landscape Cover Flow places controls in a side rail. The landscape viewer hides system bars,
  restores their previous state on dismissal and places actions/motion feedback in a 112 dp side
  rail; nothing may cover the card. Keep 48 dp controls and scrolling access when height is limited.
  Sheets scroll and pin their actions, because a bottom sheet is shorter than its content in
  landscape. The hub remains
  a single scrollable column. The appearance editor adapts to two columns in landscape
  and scales its complete preview when height is limited, leaving room for controls and fields.

## Licensing and About

- Project licence: `GPL-3.0-only`. Full text in `LICENSE`; third-party notices in
  `THIRD_PARTY_NOTICES.md` and `licenses/`.
- Licence texts also ship in the APK under `app/src/main/assets/`; the Manrope OFL text is in
  `app/src/main/res/raw/manrope_ofl.txt` (kept intentionally, ignored by `app/lint.xml`).
- About shows the author section, the licence summary and the Manrope attribution. There is no
  licences screen; do not reintroduce one.
- `docs/privacy-policy.md` is the policy Play Console declares, published with GitHub Pages at
  <https://igarciadelamo.github.io/txoka/privacy/> (source: `main`, folder `/docs`). Moving, renaming
  or deleting that file breaks a URL outside this repository: keep the path and the `permalink`
  front matter, and bump "Last updated" when the wording changes.
- `app_name` and `about_author` are `translatable="false"`.

## Strings

- Every user-facing string lives in `values/strings.xml` and `values-es/strings.xml`.
- `StringResourcesTest` enforces translation parity, placeholder parity and plural quantities.
  Add strings to both files in the same commit.
- No hardcoded user-facing text in Compose; previews may use sample data but should prefer
  `stringResource` when practical.

## Testing

- JVM tests live in `app/src/test/`; instrumented DAO tests in `app/src/androidTest/`.
- Pure protocol, profile, contrast, ViewModel and state logic are unit-tested.
- Compose screens are verified manually on the emulator; add Compose previews for new components.
- `FakeProfileRepository`, `FakeReceivedContactRepository`, `FakeShareSettingsRepository`,
  `FakeLibraryOrganizationRepository`, `FakeNfcAvailability` and `FakeSimRegionSource` live in
  `app/src/test/.../testing/`.
- Library regressions cover codec bounds/corruption, private metadata persistence and pruning,
  category/card order, favorites, transactional clear/batch restore and retry-safe Undo.
- Kover enforces 95% coverage for the included logic classes (see `app/build.gradle.kts`).

## Emulator and manual verification

- Emulator: `Pixel_9` API 37, no NFC hardware. Use the debug-only Settings row
  "Simulate an incoming card" for receive flows.
- Never claim NFC works from emulator testing.
- Helpers:

      ADB="$HOME/Library/Android/sdk/platform-tools/adb"
      "$ADB" shell cmd uimode night no     # light
      "$ADB" shell cmd uimode night yes    # dark
      "$ADB" shell uiautomator dump /sdcard/ui.xml   # inspect text/bounds
      "$ADB" exec-out screencap -p > /tmp/screen.png

- Deleting the card in testing wipes only the profile. Back it up first if needed:

      "$ADB" exec-out run-as es.igarciadelamo.txoka cat files/datastore/profile.preferences_pb > /tmp/p.pb
      "$ADB" exec-in run-as es.igarciadelamo.txoka sh -c 'cat > files/datastore/profile.preferences_pb' < /tmp/p.pb

## Known pitfalls

- Gradle is pinned to JDK 21. Detekt 1.23.8 breaks on JDK 25; always export `JAVA_HOME`.
- Room 2.6.1 fails under KSP1 with "unexpected jvm signature V". Room 2.8.5 is pinned.
- `kotlinx-serialization-json` (1.12.0) and the Kotlin compiler (2.4.21) are bumped together.
- `connectedDebugAndroidTest` uninstalls the app and clears its data; reinstall afterwards.
- GitHub publication depends on the current connection's write permissions. Check the actual
  tool result; metadata reporting push access does not prove the integration can create refs or
  commits. Never claim a PR exists without a successful creation and canonical link.
- Detekt limits functions to 80 lines and flags magic numbers outside `@Composable`/`@Preview`.
  Ktlint wants single-expression bodies on one line when they fit (120 columns max).
- Disabling an HCE service component survives reinstalls, so `TxokaApplication` re-enables
  `NdefHceService` on every start. A disabled service makes `setPreferredService` fail with
  "unknown component" and the phone silently stops sending.
- `docs/` is documentation only; only files under `app/src/main/res/` and `app/src/main/assets/`
  are packaged in the APK. Everything in `docs/` is also published by GitHub Pages, so internal
  notes, store assets and anything not meant to be public belong in `store/` or the repository
  root.
- Do not commit `local.properties`, `keystore.properties`, keystores or build outputs. Release
  signing reads `keystore.properties` from the project root; without it the APK stays unsigned.

## Roadmap

1. 1.0 ships what is already built plus bug fixes and small improvements. Blocking work is release
   signing and publication.
2. Post-1.0: vCard-file sharing and SAF export/import of the received library, without `INTERNET`.

## Workflow checklist

1. `git switch main && git pull --ff-only`.
2. Create a branch: `feat/...`, `fix/...`, `chore/...`.
3. Implement end to end, with tests where testable.
4. Run `./gradlew :app:assembleDebug :app:testDebugUnitTest detekt ktlintCheck`.
5. Commit with a Conventional Commit and the required author.
6. Push the branch and send the user the compare/PR link. Do not merge.
