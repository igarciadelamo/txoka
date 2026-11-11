# Txoka

Share your contact card by holding two phones close together over NFC, and keep the cards you
receive. Txoka never uploads anything: no backend, no account and no `INTERNET` permission.

## Features

### Your card

- Name plus at least one of the four details: a phone number, an email address, a LinkedIn handle or
  an Instagram handle.
- Optional LinkedIn and Instagram handles, shared as bare usernames. The card shows both in one row
  with only the username, never the full link; tapping a link in the received detail opens the
  browser or the installed app. When a card has no phone or email the handles take the main slot,
  full width and at title size.
- Every value you fill in is shared. There are no per-field sharing flags.
- Seven gradient presets in a grid, or a custom colour picked on a hue wheel. Presets keep their
  curated two-colour gradient; a custom colour derives the second stop automatically. The text
  colour adapts to the background using WCAG luminance, so the card stays readable on every
  palette.
- The editor validates the phone number and the email, keeps the preview in sync and saves the
  whole card at once. Back discards the draft; the overflow menu deletes the card after a
  confirmation.

### Sharing over NFC

- The phone emulates an NFC Forum Type 4 Tag that carries the card as a vCard 3.0 NDEF record.
- Which phone acts as the card is a controller race, so the hub picks the role: **Send** advertises
  the card, **Receive** only polls. The role lives in memory and starts on Send at every launch.
- After receiving a card while in Receive, the phone offers its own card for 60 seconds so the
  other phone can read it back on a second tap. The offer is one shot, which is what stops two
  phones from bouncing cards forever.
- An optional setting requires the device to be unlocked before the card is served.
- When the device has no NFC hardware, the app explains the limitation and hides the sharing
  controls instead of failing silently.

### Receiving

- Tap a Txoka phone, or any NDEF tag that carries a `text/vcard` record, and the card opens for
  confirmation before it is stored.
- Nothing is saved until the card is accepted.
- Cards that arrive while the confirmation is open wait in a queue instead of replacing each other:
  the sheet shows the first one, says how many are behind it, and moves on as you accept or
  discard. An identical card already waiting is ignored, so a repeated tap does not stack it.
- The `NDEF_DISCOVERED` intent filter also handles a tag while Txoka is not in the foreground.

### Received contacts

- Searchable library grouped into Today and Earlier.
- Detail view with the sender's card, the date, call and email actions, a note and delete.
- LinkedIn and Instagram handles, when present, open the profile in the browser or the installed
  app. No permission is needed: the link leaves the app and Txoka never fetches anything.
- *Add to contacts* opens the system editor prefilled through `ContactsContract.Intents.Insert`,
  so Txoka never asks for `WRITE_CONTACTS`.
- Sharing a card again refreshes the stored contact instead of adding a copy: a normalised phone
  (digits only), a lowercased email or a bare social handle identifies it, the sender's fields win,
  the date moves the contact to the top and the local note is kept. Cards that only share a name are
  kept separately.
- Received cards keep the sender's gradient colours.

### Privacy and storage

- Local-first: no backend, no account and no `INTERNET` permission. The app itself never uploads
  anything.
- The profile and the sharing preferences live in DataStore; received contacts and their notes live
  in a local Room database.
- Both stay in the user's own Google backup, so a reinstall or a new phone restores the library.
  That copy belongs to the device's account, not to Txoka.
- The full wording, including the backup caveat, is in `docs/privacy-policy.md`. It is published
  with GitHub Pages at <https://igarciadelamo.github.io/txoka/privacy/>, the public URL that Play
  Console declares, and it is mirrored in the About screen.
- Deleting the card clears only the profile. Share settings and received contacts stay.

### Settings and About

- Sharing switches, the received library and an About screen with the version, a privacy summary
  (local storage plus the Android backup caveat), the author, the licence summary and the Manrope
  attribution.
- In debug builds, Settings offers *Simulate an incoming card* and *Simulate two incoming cards*.
  Both run demo cards through the real NDEF and vCard round trip; the second one queues two
  different senders, which is how the confirmation queue is exercised on emulators with no NFC.

### Languages and accessibility

- English and Spanish, in light and dark theme, following the system setting.
- Layouts are designed for 100 % and 200 % font scale.
- 48 dp touch targets, content descriptions on icon-only buttons, merged semantics on cards and
  list rows, labelled search fields and switch rows that act as a single target.
- Palette swatches expose their preset number and radio-button role, and country rows announce the
  country and dial code instead of the flag emoji.

## Platform

Kotlin and Jetpack Compose with Material 3. The app runs on Android 8.0 (API 26) and newer. NFC is
optional: without NFC hardware the card can still be created and kept, but it cannot be shared or
received.

## Design

The visual system is "editorial": a warm canvas (`#F7F5F2`, dark `#121113`), white surfaces with
20–28 dp radii, one strong colour per screen and the card as the hero. Manrope (SIL Open Font
License, see `licenses/Manrope-OFL.txt`) covers display, titles and body; Android falls back to the
system font for scripts the bundled weights do not include.

Tokens live in `ui/theme`: `Spacing`, `Corners`, `TxokaShapes`, the light and dark schemes and the
extra roles (hairline, accent-soft, success, warning) exposed through `MaterialTheme.txoka`. Shared
pieces are `ContactCard` (hero and preview layouts), `TxokaField`, `Eyebrow`, `TxokaAppBar` and the
shared buttons, so screens do not invent their own paddings.

There is a single splash: the Android one. It shows the brand mark over the brand indigo and, on
Android 12 and newer, the wordmark as a branding image at the bottom. Nothing else is drawn on top,
so no second splash appears.

## Stack

Kotlin, Jetpack Compose with Material 3, Hilt, coroutines and Flow, Room, DataStore and
libphonenumber. Single Gradle module, minSdk 26, target and compile SDK 37. Manrope is bundled as
five static weights, subset to Latin.

```
app/src/main/java/es/igarciadelamo/txoka
├── ui/        Compose screens, view models, navigation, theme
├── domain/    Models, protocol codec, repository contracts
├── data/      DataStore and Room implementations
└── nfc/       HCE service, reader mode and demo transport
```

## Architecture

```
┌───────────────────────────── Compose UI ─────────────────────────────┐
│ ShareScreen · ProfileEditor · Settings · Received list/detail        │
│ ReceiveConfirm (overlay)                                             │
└───────────────▲───────────────────────────────────▲──────────────────┘
                │ StateFlow                          │ events
        ┌───────┴────────┐                  ┌────────┴─────────┐
        │  ViewModels    │                  │  IncomingCards   │
        └───────▲────────┘                  └────────▲─────────┘
                │                                    │
   ┌────────────┴─────────────┐          ┌───────────┴──────────┐
   │ domain repositories      │          │ nfc                  │
   │ ProfileRepository        │          │ NfcReader (tags)     │
   │ ShareSettingsRepository  │          │ HceCardPublisher     │
   │ ReceivedContactRepository│          │ HceResponder/T4T     │
   └────────────▲─────────────┘          └───────────▲──────────┘
                │                                    │
   ┌────────────┴─────────────┐          ┌───────────┴──────────┐
   │ data                     │          │ domain/protocol      │
   │ DataStore profile/settings│         │ VCardSerializer      │
   │ Room received contacts   │          │ NdefMessage          │
   └──────────────────────────┘          │ Type4Tag (APDU)      │
                                         └──────────────────────┘
```

The domain layer has no Android dependencies except through interfaces, so the vCard, NDEF and
Type 4 Tag code runs as plain JVM unit tests.

**Sharing.** `HceCardPublisher` observes the profile and the share settings, builds the payload
and hands the encoded NDEF message to `HceResponder`, which keeps one `Type4Tag` instance per
card. `NdefHceService` forwards each APDU from Android to the responder and resets the selection
state when the reader goes away.

**Receiving.** Two paths feed the same confirmation overlay:

1. `NfcReader` polls tags while the activity is resumed and decodes NDEF tags itself.
2. The activity's `NDEF_DISCOVERED` filter covers the case where the system launches Txoka with a
   tag intent while the app is not in the foreground.

Both push a `VCardPayload` into `IncomingCards`, which keeps them in arrival order. The UI collects
the queue, shows the first card and offers the next once it is accepted or discarded.

**Storing.** `RoomReceivedContactRepository` maps entities to domain objects, finds the contact a
card belongs to by its keys and persists the sender's colours with the contact.

## Protocol

The card is a vCard 3.0 document carried in a single NDEF record with MIME type `text/vcard`.
Txoka adds three properties that other readers simply ignore:

| Property | Meaning |
| --- | --- |
| `X-TXOKA-PROTOCOL` | Payload version, currently `1` |
| `X-TXOKA-CARD-ACCENT` | First gradient colour, `#RRGGBB` |
| `X-TXOKA-CARD-ACCENT2` | Second gradient colour |

Social handles travel as standard `URL` properties, one per network, so any contact app can open
them:

```
URL;TYPE=LINKEDIN:https://www.linkedin.com/in/isidro-garcia
URL;TYPE=INSTAGRAM:https://www.instagram.com/isidro.png
```

Text values are escaped as vCard requires (`\n`, `\,`, `\;`, `\\`) and long lines are folded at
75 octets. Only `FN`, `TEL`, `EMAIL`, `URL`/`X-SOCIALPROFILE`, `N`, the three `X-TXOKA-*` properties
and `VERSION` are read; everything else, including photos and addresses, is ignored. A handle can
arrive as `URL` or as `X-SOCIALPROFILE` from another app, and when the type is missing the network
is recognised from the link itself. Invalid colours fall back to the default palette, so a card
written by another app still loads. The NDEF message is capped at 768 bytes: the editor limits the
name, the phone, the email and the handles, and refuses to save a card whose encoded vCard would
not fit, because HCE publishes nothing above the limit.

The emitter emulates a NFC Forum Type 4 Tag. Two AIDs are registered:

| AID | Purpose |
| --- | --- |
| `D2760000850101` | NFC Forum NDEF AID, understood by standard NDEF readers |
| `F054584F4B4101` | Txoka AID, avoids the system chooser when other NDEF apps are installed |

The reader sequence is:

1. `SELECT AID`.
2. `SELECT CC` (`E103`) and `READ BINARY` of the 15-byte capability container.
3. `SELECT NDEF` (`E104`), `READ BINARY` of the two-byte NLEN and then of the message.

The reading side deliberately uses `NfcAdapter.enableForegroundDispatch` instead of reader mode.
Reader mode limits the controller to polling, and AOSP only enables host card emulation routing
while no reader-mode request is active (`NfcService.computeDiscoveryParameters`), so a phone in
reader mode stops emulating. Foreground dispatch keeps polling and emulation on at the same time,
which is what two phones that both want to send and receive need. While the activity is visible it
also calls `CardEmulation.setPreferredService`, so Txoka answers its own AIDs before any other
installed NDEF card emulator.

Status words: `9000` success, `6A82` file or AID not found, `6A86` wrong P1/P2, `6700` wrong
length, `6986` no file selected, `6D00` unsupported instruction, `6E00` unsupported class,
`6B00` offset past the end of the file and `6985` conditions of use not satisfied (device locked
while the unlock setting is on).

## Design decisions

- **Transport abstraction.** `ContactTransport` hides whether a card arrived through real
  hardware or the demo. Only the debug demo uses the fake implementation; the product paths share
  the same codec, so the emulator exercises the real parsing logic.
- **No `INTERNET` permission.** There is nothing to sync, which makes the privacy promise easy to
  verify in the manifest and the Play listing.
- **Versioning.** The protocol version travels in the vCard, not in the NDEF structure. Absent
  property means version `0`, so payload changes never break older builds.
- **Explicit colours.** Both gradient stops are sent instead of deriving the second colour on the
  receiving side, so every device renders the same card.
- **Social handles stay bare.** The card stores `ana.ruiz`, not the full profile link, so the
  payload grows by a few bytes per network and the editor can accept either a username or a pasted
  link. `SocialHandle` normalises both into the stored form.
- **Runtime unlock rule.** The HCE service is not declared with `requireDeviceUnlock`, because the
  switch can change while the service is bound. The responder returns `6985` per APDU instead.
- **Repeated cards merge.** A received card belongs to a stored contact when the normalised phone
  (digits only), the normalised email (trimmed, lower case) or a bare social handle matches. Saving
  it again refreshes the sender's fields and bumps the received date but keeps the local note, and
  the confirm sheet says so before the user accepts. Two cards with only the same name are kept
  separately.
- **No contact write permission.** *Add to contacts* opens the system editor prefilled with
  `ContactsContract.Intents.Insert`, so the app never requests `WRITE_CONTACTS`.
- **Notes stay local.** Notes are plain text in the app database. The schema is exported to
  `app/schemas/` and committed, so every version bump must ship a `Migration`; release builds have
  no destructive fallback.

## Accessibility and localisation

- Every user-visible string lives in `res/values` and `res/values-es`; the two files have the same
  keys and the received count uses plurals. No text is hardcoded in composables.
- Icon buttons have content descriptions, and decorative icons are marked as such.
- Switch rows are single toggleable targets with `Role.Switch`, so the whole row can be activated
  by touch or screen reader, and the text wraps instead of colliding with the switch at large font
  sizes.
- Palette swatches expose "Colour preset N" with the radio button role; country rows announce the
  name and dial code instead of the flag emoji; search fields use visible labels.
- Call and email actions flow onto a second line on narrow screens or large fonts.
- Interactive elements are at least 48 dp: palette swatches, share chips, the country chip, the
  card action row and the inline NFC settings link keep a 48 dp target while the visual stays
  small.
- The contact card is one screen-reader node (`mergeDescendants`); the initials and the TXOKA
  wordmark are decorative. Received rows merge the name and the detail and expose the button role.
- Card contrast is picked from the worst gradient stop and every preset reaches 4.5:1 for body
  text, which also covers the 3:1 large-text rule. The glow, avatar and contact pills are tinted
  against the text colour, so they always add contrast instead of removing it. `CardContrastTest`
  guards the presets.

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

### Release signing

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

Then `keystore.properties` in the project root (absolute paths are accepted):

```properties
storeFile=/Users/you/keys/txoka-release.p12
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


### Unit tests and coverage

The JVM suite covers the protocol, the profile logic, the NFC responder and every view model.
Kover enforces a 95% line coverage gate over those layers; Compose screens and the Android-only
adapters are covered by manual checks instead. The HTML report is written to
`app/build/reports/kover/htmlDebug/index.html`.

```sh
./gradlew :app:koverVerifyDebug :app:koverHtmlReportDebug
```

### Instrumented tests

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

### NFC device checks

Sharing and receiving need two physical phones. [`NFC_DEVICE_TEST_PLAN.md`](NFC_DEVICE_TEST_PLAN.md)
is the checklist for them, with setup, the 32 cases and the results table to fill in.

## Known limitations

- Phone numbers typed without a country prefix use the country picked in the editor. It defaults
  to the SIM region, then the network, then the device locale and finally Spain.
- HCE only answers while the screen is on. The hub keeps the display awake while sharing is
  enabled, but a locked device with *Require device unlock* on returns `6985`.
- The NDEF AID `D2760000850101` is shared with every NDEF HCE app. When another app claims it,
  Android should show a chooser.
- Reading an emulated card needs an app that handles `NDEF_DISCOVERED` for `text/vcard`. Without
  Txoka installed the phone will not offer to save the contact on its own.
- Reader mode is never used: it disables host card emulation, so Txoka reads tags with foreground
  dispatch while it keeps emulating. Two phones find each other without a mode switch, but which
  one wins as the card is up to the controllers, so the hub role makes the exchange predictable.
- Notes are plain text, one per contact, and there is no sync or export.
- Sharing as a vCard file or a QR code is not implemented; sharing works over NFC only.

## Licence

Txoka is free software under the GNU General Public License v3.0 only (`GPL-3.0-only`). See
[`LICENSE`](LICENSE) for the full text. Third-party components and their licences are listed in
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md); the bundled Manrope font uses the SIL Open
Font License 1.1 (`licenses/Manrope-OFL.txt`). The licence texts also ship inside the APK under
`assets/`.
