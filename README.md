# Txoka

Share your contact card by holding two phones close together over NFC, and keep the cards
you receive. Everything stays on the device: no backend, no internet permission.

> Work in progress. This README is completed milestone by milestone.

## Status

| Milestone | Scope | Status |
| --- | --- | --- |
| 0 | Project scaffolding, Hilt, navigation, lint and CI | Done |
| 1 | Own profile, card palette and onboarding | Done |
| 2 | Pure protocol: vCard, NDEF payload, T4T/APDU state machine | Planned |
| 3 | HCE emitter service | Planned |
| 4 | Receiving: reader mode, intent filter, confirmation | Planned |
| 5 | Room, received contacts list | Planned |
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

Pending: vCard 3.0 payload, NDEF `text/vcard` record and the Type 4 Tag APDU sequence.

## Design decisions

Pending: transport abstraction, protocol versioning, payload allowlist, card theming.

## Known limitations

- Phone numbers typed without a country prefix are parsed against the device locale region
  (`Locale.getDefault().country`). Reading the SIM region is pending.
- Pending: AID conflicts with other NDEF apps, screen-on requirement, Tap to Share, vendor
  behaviours.

Items that cannot be verified without hardware are listed here as *pending device test*.

## Manual test plan

Pending: device matrix and scenarios.
