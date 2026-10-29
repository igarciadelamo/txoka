# Txoka

Share your contact card by holding two phones close together over NFC, and keep the cards
you receive. Everything stays on the device: no backend, no internet permission.

> Work in progress. This README is completed milestone by milestone.

## Status

| Milestone | Scope | Status |
| --- | --- | --- |
| 0 | Project scaffolding, Hilt, navigation, lint and CI | Done |
| 1 | Own profile, card palette and onboarding | Done |
| 2 | Pure protocol: vCard, NDEF payload, T4T/APDU state machine | Done |
| 3 | HCE emitter service | Done |
| 4 | Receiving: reader mode, intent filter, confirmation | Done |
| 5 | Room, received contacts list | Done |
| 6 | Polish, documentation, manual test matrix | Planned |

## Stack

Kotlin, Jetpack Compose with Material 3, Hilt, coroutines and Flow, Room, DataStore and
libphonenumber. Single Gradle module with packages split by layer:

```
es.igarciadelamo.txoka
├── ui/      Compose screens, view models, navigation, theme
├── domain/  Models, use cases, transport contract
├── data/    DataStore and Room implementations
└── nfc/     HCE service and NFC readers
```

## Build and checks

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew detekt ktlintCheck
```

The Gradle daemon JVM is pinned to 21 through `gradle/gradle-daemon-jvm.properties`, because
Detekt 1.23.8 cannot parse the version string of newer JDKs.

## Architecture

Pending: diagram and layer responsibilities.

## Protocol

The card is a vCard 3.0 document carried in a single NDEF record with MIME type `text/vcard`.
Txoka adds three properties that other readers simply ignore:

| Property | Meaning |
| --- | --- |
| `X-TXOKA-PROTOCOL` | Payload version, currently `1` |
| `X-TXOKA-CARD-ACCENT` | First gradient colour, `#RRGGBB` |
| `X-TXOKA-CARD-ACCENT2` | Second gradient colour |

Text values are escaped as vCard requires (`\n`, `\,`, `\;`, `\\`) and long lines are folded at
75 octets. Unknown properties are skipped and invalid colours fall back to the default palette, so a
card written by another app still loads. The NDEF message is capped at 768 bytes.

The emitter emulates a NFC Forum Type 4 Tag. The reader sequence is:

1. `SELECT AID` (`D2760000850101` by default, plus any extra AID we register).
2. `SELECT CC` (`E103`) and `READ BINARY` of the 15-byte capability container.
3. `SELECT NDEF` (`E104`), `READ BINARY` of the two-byte NLEN and then of the message.

Status words: `9000` success, `6A82` file or AID not found, `6A86` wrong P1/P2, `6700` wrong
length, `6986` no file selected, `6D00` unsupported instruction, `6E00` unsupported class and
`6B00` offset past the end of the file.

## Design decisions

Pending: transport abstraction, protocol versioning, payload allowlist, card theming.

## Known limitations

- Phone numbers typed without a country prefix use the country picked in the editor. It defaults
  to the SIM region, then the network, then the device locale and finally Spain.
- HCE only answers while the screen is on. The hub keeps the display awake while sharing is
  enabled, but a locked device with `requireDeviceUnlock` on returns `6985`.
- The NDEF AID `D2760000850101` is shared with every NDEF HCE app. When another app claims it,
  Android should show a chooser; *pending device test*. A Txoka specific AID is registered too, so
  Txoka to Txoka does not depend on that chooser.
- The NDEF AID is not registered as `requireDeviceUnlock` in the manifest; the unlock rule is
  enforced per APDU instead, because the switch can change at runtime.
- Reading an emulated card needs an app that handles `NDEF_DISCOVERED` for `text/vcard`. Without
  Txoka installed the phone will not offer to save the contact on its own; *pending device test* for
  vendor Contacts apps that still handle the legacy Beam filter.
- Reader mode and HCE run at the same time on the same device; *pending device test* to confirm both
  work on the same radio.
- Duplicate detection matches a stored contact when the normalised phone or the normalised email is
  equal; two cards sharing only a name are stored separately.
- Notes are plain text, one per contact. The database is version 1 with no migrations yet.
- Pending: Tap to Share interactions and vendor specific behaviour.

Items that cannot be verified without hardware are listed here as *pending device test*.

## Manual test plan

Pending: device matrix and scenarios.
