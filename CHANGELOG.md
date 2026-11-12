# Changelog

All notable changes to Txoka are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). The version here matches `versionName` in
`app/build.gradle.kts`; `versionCode` follows the Play upload order.

## [1.0.0] - 2026-11-12

First public release. `versionCode` 1, minimum Android 8.0 (API 26), Room schema version 2.

### Added

- **Card editor.** Name, phone with a country picker for national numbers, email, LinkedIn and
  Instagram. The name and at least one of the four details are required, and every field that is
  present is shared. Per-field limits keep a card inside the NDEF budget, and the encoded size is
  checked before saving.
- **Card design.** Seven two-colour gradients in a 4×2 grid plus a custom colour picked on a hue
  wheel, with a live preview and a text colour chosen by WCAG contrast.
- **Sharing over NFC.** The phone emulates an NFC Forum Type 4 Tag that carries the card as a vCard
  3.0 NDEF record, under the NDEF Forum AID and a Txoka AID. Reading uses foreground dispatch, so
  one phone can send and receive at the same time even with other NDEF apps installed.
- **Send and Receive roles.** A two-state control instead of a switch, because "stop sending" means
  "start receiving". The role lasts for the session and resets to Send.
- **Return offer.** After a card arrives while receiving, the phone offers its own for 60 seconds,
  with a live countdown and a Cancel action. The window runs on the real clock, so it keeps counting
  while the app is in the background.
- **Incoming card sheet.** Shows the sender, the card, "Shared over NFC" and reasons to save or
  discard. Cards queue up in arrival order with "1 more card is waiting" and an identical card
  already queued is ignored.
- **Repeated cards update instead of duplicating.** A normalised phone, email or social handle
  matches an existing contact: saving refreshes the sender's fields and the date, keeps the local
  note and says "You already have …" before accepting. Ambiguous matches are stored as new cards.
- **Received library.** List with search, detail with date and note, call and email actions,
  *Add to contacts* prefilled in the system editor, delete one card, and *Empty library* with a
  confirmation dialog and an undo snackbar.
- **Settings.** Require device unlock before sharing, an NFC-off warning that opens the system
  settings, the received library and About. Debug builds add two rows that simulate incoming cards
  through the real NDEF and vCard round trip.
- **About.** Version, a privacy summary covering local storage and the Android system backup, the
  author section, the GPL-3.0-only summary and the Manrope attribution.
- **Interface.** English and Spanish with a test that enforces parity and placeholder parity, the
  bundled Manrope font, light and dark themes that follow the system, a single system splash, and
  predictive back. Content is capped at 560 dp in landscape and wide windows.
- **Accessibility.** 48 dp touch targets, content descriptions on icon-only buttons, merged
  semantics on cards and rows, and card contrast checked by tests.

### Notes

- Sharing and receiving need NFC hardware; without it the app still creates, edits and keeps cards
  and explains the limitation.
- There is no `INTERNET` permission and no analytics, advertising, crash reporting or tracking. See
  the [privacy policy](https://igarciadelamo.github.io/txoka/privacy/).
- With Android backup enabled, the system may copy the card and the library to the user's own
  Google account. That copy belongs to the user's account, not to Txoka.
- Phone numbers typed without a country prefix use the country picked in the editor, which defaults
  to the SIM region, then the network, then the device locale and finally Spain.
- Open-source licence: GPL-3.0-only, with third-party notices in `THIRD_PARTY_NOTICES.md`.

[1.0.0]: https://github.com/igarciadelamo/txoka/releases/tag/v1.0.0
