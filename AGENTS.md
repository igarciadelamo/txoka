# AGENTS.md

Context and working agreement for AI agents contributing to Txoka. Read this before touching code.
If something here disagrees with the repository, trust the repository and update this file.

## What this is

Txoka is a native Android app that shares a contact card between two phones over NFC and keeps the
cards received. It is local-first: no backend, no account and no `INTERNET` permission.

- Kotlin + Jetpack Compose + Material 3, single Gradle module.
- Hilt for DI, coroutines/Flow, Room for received contacts, DataStore for profile/settings,
  libphonenumber for phone handling, Manrope as the bundled font.
- Packages: `ui/`, `domain/`, `data/`, `nfc/`, `di/`.
- Version 1.0 (`versionCode = 1`). Room database version 2; the schema is exported to
  `app/schemas/` and committed, and every version bump must ship a real `Migration` (release builds
  have no destructive fallback).

## Current status (November 2026)

`main` is green. Milestones 0–6 and the polish phase are merged.

- Unified app bar, hub/card actions, accessibility pass, string audit and licensing are done.
- 167 JVM unit tests + 5 instrumented DAO tests (the DAO tests currently fail to start on the
  emulator with `NoSuchMethodError runBlockingK`; it reproduces on `main`).
- Remaining backlog: optional vCard/QR sharing fallback, real two-phone NFC validation.

## Non-negotiable rules

1. Do not add `INTERNET` or any new permission without asking the user first.
2. Do not invent Android/NFC behaviour. Anything that needs real hardware must be written as
   "pending device test", never asserted as working.
3. One branch per feature/fix. Never commit directly to `main` and never merge PRs yourself.
4. Commit author must be `isidro.garcia@gmail.com`:
   `git -c user.name="Isidro Garcia" -c user.email="isidro.garcia@gmail.com" commit ...`
5. Code, comments, commit messages, README and this file are in English. UI strings are Spanish
   and English (`values/` + `values-es/`).
6. Minimise comments; prefer clear identifiers. Use Conventional Commits (`feat:`, `fix:`,
   `chore:`, `refactor:`, `test:`, `docs:`), scoped when useful (`feat(profile): ...`).
7. Ask one crisp question when a design decision is ambiguous, then implement end to end. The user
   is cost-sensitive: do not explore for long without a deliverable.

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

- `ui/share/ShareScreen.kt` + ViewModel — hub: app bar actions, hero card with edit overlay, NFC
  module, privacy footer.
- `ui/profile/ProfileEditorScreen.kt` + ViewModel — create/edit the card: name, phone with country
  picker, email, colour grid + wheel sheet, delete in the overflow menu.
- `ui/profile/ColourPickerSheet.kt` — custom colour: hue wheel, saturation/value square and live
  card preview.
- `ui/received/` — list, detail, notes, delete, add to system contacts, and "empty library" with a
  confirmation dialog and an undo snackbar.
- `ui/receive/ReceiveConfirmScreen.kt` + ViewModel — confirmation bottom sheet after a tag is read.
- `ui/settings/` — device unlock setting, library, About, no separate licences screen.
- `ui/components/Layout.kt` — `Eyebrow`, `TxokaAppBar`, `ScreenColumn`, shared buttons.
- `ui/components/ContactCard.kt` — hero/preview card and `Profile.cardContentColor()`.
- `ui/components/Fields.kt` — `TxokaField`, `CountryChip`.
- `ui/theme/` — `Color.kt`, `Theme.kt`, `Tokens.kt` (`Spacing`, `Corners`), `Type.kt`.
- `ui/navigation/` — serializable routes and `TxokaNavHost`.
- `domain/protocol/` — pure vCard 3.0 and NDEF encoding, Type 4 Tag APDU state machine.
- `domain/profile/` — `Profile`, palettes, phone helpers, contrast helpers, repository interfaces.
- `domain/received/` — received contact model and repository interface.
- `nfc/` — `HceCardPublisher`, `HceResponder`, `NdefHceService`, `NfcReader`, `IncomingCards`,
  `FakeTransport`.
- `data/profile/` — DataStore profile and share-settings repositories.
- `data/received/` — Room entity, DAO, database and repository.
- `di/` — Hilt modules.

## Data model and protocol

- `Profile(name, phone?, email?, linkedin?, instagram?, card)`. The name and at least one of the
  four detail fields (phone, email, LinkedIn, Instagram) are mandatory in the editor. **Every field
  that is present is shared**: there are no per-field sharing flags. Removed
  `sharePhone`/`shareEmail` in PR #16. Social handles are stored bare (`ana.ruiz`) and travel as
  standard `URL` properties.
- Received contacts store the sender's name, phone, email, LinkedIn and Instagram handles, palette,
  note and timestamp.
- A repeated card matches on normalised phone (digits only), lowercased email or lowercased social
  handle. Saving it refreshes the sender's fields and bumps `receivedAt`, keeps the local note and
  returns `SaveResult.Updated`; the confirm sheet shows "You already have …" and "Update contact".
  Same-name-only cards are stored separately. Merging needs exactly one match: if the phone points
  at one stored contact and the email at another, the card is stored as a new one instead of
  overwriting either, and `match()` reports no owner.
- `ProfileRepository.delete()` clears only the `profile.*` DataStore keys; it keeps share settings
  and received contacts. The hub falls back to its empty state and HCE stops publishing.
- Payload: vCard 3.0 plus `X-TXOKA-PROTOCOL`, `X-TXOKA-CARD-ACCENT`, `X-TXOKA-CARD-ACCENT2`.
  A missing protocol means version 0. NDEF payload cap: 768 bytes.
- HCE AIDs: NDEF Forum `D2760000850101` and Txoka `F054584F4B4101`.
- Phone region fallback: SIM → network → locale → ES.

## UI and design system

- Theme follows the system light/dark mode. Forcing light mode was explicitly rejected; do not
  reintroduce it without asking.
- One `TxokaAppBar`:
  - `title == null` → "Txoka" wordmark (hub).
  - `title != null` → back arrow + screen title (secondary screens).
  - Surface background, 1 dp hairline (`HairlineLight = 0x3D171614`,
    `HairlineDark = 0x33FFFFFF`) and an 8 dp, 7%-black shadow.
- Hub: no bottom quick actions. A pencil overlay on the card opens the editor; a contacts icon
  (no numeric badge) sits in the app bar left of Settings.
- The hub sharing module is a two-state control (Send / Receive), not an on/off switch, because
  "stop sending" means "start receiving". The role is session only, in memory, and resets to Send.
  After a card arrives while receiving, the phone offers its own for 60 s with a Cancel action.
- Editor: back discards (there is no Discard button), the footer is a single full-width Save, and
  "Delete card" lives in the app-bar overflow menu with a confirmation dialog.
- Cards always use a two-colour gradient; 7 presets in a 4×2 grid plus a custom colour picked on a
  hue wheel. Text colour comes from `Profile.cardContentColor()` using WCAG luminance.
- Accessibility baseline: 48 dp targets, content descriptions on icon-only buttons, merged
  semantics on cards and list rows, contrast checked by tests.
- Only `material-icons-core` is available; `material-icons-extended` is not a dependency.
- Landscape and wide windows: content uses `Modifier.contentWidth()` (`ContentMaxWidth` = 560 dp)
  inside a horizontally centred column, so nothing stretches edge to edge. Sheets scroll and pin
  their actions, because a bottom sheet is shorter than its content in landscape. The hub and the
  editor are still single columns in landscape: the card scrolls out of the fold, and a two-column
  layout for them is the known follow-up.

## Licensing and About

- Project licence: `GPL-3.0-only`. Full text in `LICENSE`; third-party notices in
  `THIRD_PARTY_NOTICES.md` and `licenses/`.
- Licence texts also ship in the APK under `app/src/main/assets/`; the Manrope OFL text is in
  `app/src/main/res/raw/manrope_ofl.txt` (kept intentionally, ignored by `app/lint.xml`).
- About shows the author section, the licence summary and the Manrope attribution. There is no
  licences screen; do not reintroduce one.
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
  `FakeNfcAvailability` and `FakeSimRegionSource` live in `app/src/test/.../testing/`.
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
- `gh` is not installed and there are no GitHub API credentials. You can push branches but not
  open PRs. Push and give the user:
  `https://github.com/igarciadelamo/txoka/compare/main...<branch>?expand=1`
- Detekt limits functions to 80 lines and flags magic numbers outside `@Composable`/`@Preview`.
  Ktlint wants single-expression bodies on one line when they fit (120 columns max).
- `docs/` is documentation only; only files under `app/src/main/res/` and `app/src/main/assets/`
  are packaged in the APK.
- Do not commit `local.properties`, `keystore.properties`, keystores or build outputs. Release
  signing reads `keystore.properties` from the project root; without it the APK stays unsigned.

## Roadmap

1. Optional sharing fallback: export the card as a vCard file or QR code, without `INTERNET`.
2. Real NFC validation: reader + HCE need two physical phones. Keep it marked
   "pending device test" until then.

## Workflow checklist

1. `git switch main && git pull --ff-only`.
2. Create a branch: `feat/...`, `fix/...`, `chore/...`.
3. Implement end to end, with tests where testable.
4. Run `./gradlew :app:assembleDebug :app:testDebugUnitTest detekt ktlintCheck`.
5. Commit with a Conventional Commit and the required author.
6. Push the branch and send the user the compare/PR link. Do not merge.
7. If hardware verification is needed, say "pending device test" in the PR and README.
