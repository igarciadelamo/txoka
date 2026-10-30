# Txoka

Share your contact card by holding two phones close together over NFC, and keep the cards you
receive. Everything stays on the device: no backend, no account and no `INTERNET` permission.

## Features

- **Your card.** Name, optional phone and email, plus a two-colour gradient chosen from seven
  presets or a custom hex value. Text colour adapts to the background for contrast.
- **Sharing over NFC.** The phone emulates a NFC Forum Type 4 Tag that carries the card as a
  vCard 3.0 NDEF record. Sharing can be switched off, and an optional setting requires the device
  to be unlocked before the card is served.
- **Receiving.** Tap a Txoka phone (or any NDEF tag with a `text/vcard` record) and the card opens
  for confirmation before it is stored.
- **Received contacts.** Searchable list, editable notes, call, email, add to the system contacts
  app and delete. Duplicates are detected by normalised phone or email.
- **Debug demo.** In debug builds, Settings offers *Simulate an incoming card*, which runs a demo
  card through the real NDEF and vCard round trip. Useful on emulators, which have no NFC.

The UI is available in English and Spanish, in light and dark theme, and does not require NFC
hardware: when NFC is missing the app explains it and disables the sharing controls.

## Status

| Milestone | Scope | Status |
| --- | --- | --- |
| 0 | Project scaffolding, Hilt, navigation, lint and CI | Done |
| 1 | Own profile, card palette and onboarding | Done |
| 2 | Pure protocol: vCard, NDEF payload, T4T/APDU state machine | Done |
| 3 | HCE emitter service | Done |
| 4 | Receiving: reader mode, intent filter, confirmation | Done |
| 5 | Room, received contacts list | Done |
| 6 | Polish, accessibility, documentation, manual test matrix | Done |

## Stack

Kotlin, Jetpack Compose with Material 3, Hilt, coroutines and Flow, Room, DataStore and
libphonenumber. Single Gradle module, minSdk 26, target and compile SDK 37.

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

1. `NfcReader` runs in reader mode while the activity is resumed and decodes NDEF tags itself.
2. The activity's `NDEF_DISCOVERED` filter covers the case where the system launches Txoka with a
   tag intent while the app is not in the foreground.

Both push a `VCardPayload` into `IncomingCards`, which the UI collects.

**Storing.** `RoomReceivedContactRepository` maps entities to domain objects, keeps two indexed
keys for duplicate detection and persists the sender's colours with the contact.

## Protocol

The card is a vCard 3.0 document carried in a single NDEF record with MIME type `text/vcard`.
Txoka adds three properties that other readers simply ignore:

| Property | Meaning |
| --- | --- |
| `X-TXOKA-PROTOCOL` | Payload version, currently `1` |
| `X-TXOKA-CARD-ACCENT` | First gradient colour, `#RRGGBB` |
| `X-TXOKA-CARD-ACCENT2` | Second gradient colour |

Text values are escaped as vCard requires (`\n`, `\,`, `\;`, `\\`) and long lines are folded at
75 octets. Only `FN`, `TEL`, `EMAIL`, `N`, the three `X-TXOKA-*` properties and `VERSION` are
read; everything else, including photos and addresses, is ignored. Invalid colours fall back to
the default palette, so a card written by another app still loads. The NDEF message is capped at
768 bytes.

The emitter emulates a NFC Forum Type 4 Tag. Two AIDs are registered:

| AID | Purpose |
| --- | --- |
| `D2760000850101` | NFC Forum NDEF AID, understood by standard NDEF readers |
| `F054584F4B4101` | Txoka AID, avoids the system chooser when other NDEF apps are installed |

The reader sequence is:

1. `SELECT AID`.
2. `SELECT CC` (`E103`) and `READ BINARY` of the 15-byte capability container.
3. `SELECT NDEF` (`E104`), `READ BINARY` of the two-byte NLEN and then of the message.

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
- **Runtime unlock rule.** The HCE service is not declared with `requireDeviceUnlock`, because the
  switch can change while the service is bound. The responder returns `6985` per APDU instead.
- **Duplicate detection.** A received card is a duplicate when the normalised phone (digits only)
  or the normalised email (trimmed, lower case) matches a stored contact. Two cards with only the
  same name are kept separately.
- **No contact write permission.** *Add to contacts* opens the system editor prefilled with
  `ContactsContract.Intents.Insert`, so the app never requests `WRITE_CONTACTS`.
- **Notes stay local.** Notes are plain text in the app database; the database is version 1 with
  no migrations yet.

## Accessibility and localisation

- Every user-visible string lives in `res/values` and `res/values-es`; the two files have the same
  keys and the badge count uses plurals. No text is hardcoded in composables.
- Icon buttons have content descriptions, and decorative icons are marked as such. The received
  badge announces "1 received contact" instead of a bare number.
- Switch rows are single toggleable targets with `Role.Switch`, so the whole row can be activated
  by touch or screen reader, and the text wraps instead of colliding with the switch at large font
  sizes.
- Palette swatches expose "Colour preset N" with the radio button role; country rows announce the
  name and dial code instead of the flag emoji; search fields use visible labels.
- Call and email actions flow onto a second line on narrow screens or large fonts.
- Layouts were checked on the Pixel 9 emulator at 100% and 200% font scale, in English and
  Spanish, light and dark theme.

## API levels

`minSdk` is 26 and `targetSdk`/`compileSdk` are 37. `lintDebug` runs in CI and currently reports
no errors. Findings fixed during the audit:

- `ByteArrayOutputStream.writeBytes(ByteArray)` (API 33) was replaced with `write`, so the NDEF
  encoder works from API 26.
- `Intent.getParcelableArrayExtra` was replaced with `IntentCompat.getParcelableArrayExtra`.
- The `mipmap-anydpi-v26` folder was renamed to `mipmap-anydpi`, since the minimum SDK already
  implies the qualifier, and a redundant activity label was removed.

## Build and checks

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew detekt ktlintCheck
./gradlew :app:lintDebug
```

The Gradle daemon JVM is pinned to 21 through `gradle/gradle-daemon-jvm.properties`, because
Detekt 1.23.8 cannot parse the version string of newer JDKs. CI runs the same tasks on every push
to `main` and on every pull request.

### Instrumented tests

`ReceivedContactDaoTest` covers ordering, duplicate lookup and note/delete queries against an
in-memory Room database. Run them with the emulator or device attached:

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

## Manual test matrix

### Emulator or any device without NFC (demo mode)

| Scenario | Expected | Status |
| --- | --- | --- |
| First launch | Hub with "You don't have a card yet", sharing hint hidden, NFC banner | Pass |
| Create a card | Preview updates live, card saved, hub shows it | Pass |
| Invalid email / phone | Field error, save disabled | Pass |
| Settings | Sharing controls hidden when NFC is unavailable, demo row in debug | Pass |
| Demo card → Save contact | Contact stored, badge increments | Pass |
| Demo card → Discard | Nothing stored, overlay closes | Pass |
| Search | Filters by name, phone and email; empty state when nothing matches | Pass |
| Detail | Notes persist after force-stop, delete asks for confirmation | Pass |
| Font scale 200% | No clipped or overlapping labels, call/email wrap | Pass |
| Spanish locale | Every screen translated, badge uses Spanish plurals | Pass |

### Two NFC phones (pending device test)

| Scenario | Expected |
| --- | --- |
| Share with default settings | Reader phone shows the confirmation screen with the sender's colours |
| Save and repeat the tap | Second card is reported as a duplicate and not stored |
| Sharing switched off | Reader sees no card, sender hub shows "Sharing is off" |
| Require unlock on, phone locked | Reader gets `6985`, no card shown |
| Android Beam-era Contacts apps | Vendor behaviour when scanning a `text/vcard` NDEF tag |
| Txoka not installed on the reader | The system may offer nothing; noted as a limitation |
| Reader mode and HCE at once | Both work on the same radio |

## Known limitations

- Phone numbers typed without a country prefix use the country picked in the editor. It defaults
  to the SIM region, then the network, then the device locale and finally Spain.
- HCE only answers while the screen is on. The hub keeps the display awake while sharing is
  enabled, but a locked device with *Require device unlock* on returns `6985`.
- The NDEF AID `D2760000850101` is shared with every NDEF HCE app. When another app claims it,
  Android should show a chooser; *pending device test*. The Txoka AID is registered too, so Txoka
  to Txoka does not depend on that chooser.
- Reading an emulated card needs an app that handles `NDEF_DISCOVERED` for `text/vcard`. Without
  Txoka installed the phone will not offer to save the contact on its own.
- Reader mode and HCE run at the same time on the same device; *pending device test* to confirm
  both work on the same radio.
- Notes are plain text, one per contact, and there is no sync or export.
- Sharing as a vCard file or a QR code is not implemented; the manual test plan covers NFC only.

Everything that cannot be verified without hardware is marked *pending device test*.
