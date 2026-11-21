<p align="center">
  <img src="store/txoka_icon_app-playstore.png" width="104" alt="">
</p>

<h1 align="center">Txoka</h1>

<p align="center">Your contact card, phone to phone. No account, no internet permission.</p>

<p align="center">
  <img alt="Android 8.0 or newer" src="https://img.shields.io/badge/Android-8.0%2B-3ddc84?logo=android&logoColor=white">
  <a href="LICENSE"><img alt="Licence: GPL-3.0-only" src="https://img.shields.io/badge/licence-GPL--3.0--only-blue"></a>
  <img alt="Internet permission not requested" src="https://img.shields.io/badge/INTERNET-not%20requested-critical">
  <img alt="Kotlin and Jetpack Compose" src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?logo=kotlin&logoColor=white">
</p>

<p align="center">
  <img src="store/screenshots/readme/01-hub.jpg" width="180" alt="The hub: your card and the Send / Receive control">
  <img src="store/screenshots/readme/02-incoming.jpg" width="180" alt="A card that arrived, waiting to be saved">
  <img src="store/screenshots/readme/03-library.jpg" width="180" alt="The library of received cards">
  <img src="store/screenshots/readme/04-editor.jpg" width="180" alt="The card editor">
</p>

Share a contact card by holding two NFC phones together or showing its QR code. The other person
decides whether to keep it; over NFC, they can send theirs back on a second tap. Sharing is direct:
there is no backend, no account and no `INTERNET` permission.

## What it does

- **Your card.** Name, phone, email, LinkedIn and Instagram. Choose a gradient, holographic,
  stained-glass, nacre, brushed-metal or flat background; a local vector mosaic; and a water,
  earth, wind or fire effect. Name and contact data have independent lettering finishes, four size
  presets and colours. The three material backgrounds use bundled textures edited from the
  approved reference, with palette and lighting adjustments on the device. Older cards keep their
  original gradient and automatic text contrast.
- **Share.** The phone emulates an NFC Forum Type 4 Tag carrying a vCard 3.0 record. **Send**
  advertises the card, **Receive** listens. Double-tap a card, or use **Show QR**, to flip it over
  and show a QR code on the same chosen background. Double-tap again or use the button to return.
- **QR fallback.** Scan with the optional camera or import an image through the system file
  picker. Txoka restores the contact and its supported style; another phone's camera may offer to
  import the basic contact without the style. Scanning works offline. Camera permission is asked
  only after **Use camera**; image import needs neither camera nor storage permission.
- **Receive.** Nothing is stored until you accept it. Cards that arrive while the sheet is open wait
  in a queue instead of overwriting each other, and a card you already have is updated rather than
  duplicated.
- **Keep.** Browse received cards in Cover Flow, category shelves or a searchable list. Cover Flow
  keeps the active card facing forward and angles its neighbours; its background follows the active
  category and blends while swiping between categories. Open a card full screen, or use its date,
  private note, call/email actions and *Add to contacts* without asking for `WRITE_CONTACTS`.
- **Organise.** Create categories with a name, icon and flat or two-colour gradient background;
  reorder categories and cards, move cards between categories and mark favourites. This organisation
  is private device metadata, stored separately from the received contacts and never shared by NFC or QR.
- **Local and private.** No ads, no analytics, no crash reporting and no network access. NFC and QR
  share the contact fields and appearance you choose; private notes and library organisation are
  excluded. Links to a profile open in the browser: the app never fetches them.

## Requirements

Android 8.0 (API 26) or newer. NFC exchange needs two phones with NFC. QR provides another way to
share: display the code for another phone to scan, or import a QR image in Txoka. NFC and camera
hardware are optional; the card editor, library and image import work without them.

## Build and run

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest detekt ktlintCheck
./gradlew :app:installDebug
```

Release signing, coverage, instrumented tests and the NFC device plan are in
[`TESTING.md`](TESTING.md). How the app is put together, what travels over the tag and why it looks
the way it does are in [`ARCHITECTURE.md`](ARCHITECTURE.md).

The [interactive appearance demo](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/)
is a public, unlisted browser design reference, with links to the plan and NFC contract.
The native implementation and compact wire format are documented in
[`CARD_APPEARANCE.md`](CARD_APPEARANCE.md).

The full-screen viewer keeps the card's ISO ID-1 proportions. In landscape it uses the available
height in an immersive window, with movement, QR and close controls in a side rail.

## Appearance review

The [native screenshot gallery](store/screenshots/review/README.md) shows the card in the hub,
its layered editor and the landscape viewer. These are unedited Android emulator captures with
fictitious contact details.

<p align="center">
  <a href="store/screenshots/review/01-hub-hologram.png"><img src="store/screenshots/review/01-hub-hologram.png" width="180" alt="Holographic card in the native Txoka hub"></a>
  <a href="store/screenshots/review/03-editor-mosaics.png"><img src="store/screenshots/review/03-editor-mosaics.png" width="180" alt="Pinned card preview above the native mosaic catalogue"></a>
  <a href="store/screenshots/review/04-editor-lettering.png"><img src="store/screenshots/review/04-editor-lettering.png" width="180" alt="Independent name and contact lettering finishes in the native editor"></a>
</p>

## Known limitations

- The hub role decides the NFC mode, so one tap carries one card and the phone that listens cannot
  also advertise. Which phone ends up as the card is still up to the controllers.
- HCE answers only while the screen is on, and a locked phone with *Require device unlock* on
  answers `6985` instead.
- The NDEF AID `D2760000850101` is shared with every NDEF HCE app, so another app can claim the tag.
- Reading an emulated card needs an app that handles `NDEF_DISCOVERED` for `text/vcard`; without
  Txoka installed the phone will not offer to save the contact on its own.
- Phone numbers typed without a prefix use the country picked in the editor, which defaults to the
  SIM region, then the network, then the device locale and finally Spain.
- No sync or vCard-file/library export: notes are plain text, one per contact, and stay private.
- The NFC message is limited to 768 bytes, including its record framing and Android Application
  Record. A larger card can be saved for QR sharing if its UTF-8 vCard fits the separate 2,048-byte
  QR limit; the editor warns that NFC is unavailable for that card.
- Large QR codes need enough screen space and a clear view. Physical camera interoperability
  still needs device checks; other scanners may ignore Txoka's appearance extension.
- Appearance and NFC interoperability need validation on physical phones. A sensorless device can
  display designs and use manual movement controls. User-selected text colours may need adjustment
  for contrast.

## How it was built

Txoka was built with an AI coding agent (DeepSeek V4.1 Flash 1017,
`deepseek/deepseek-v4.1-flash-1017` on OpenRouter) following the rules in `AGENTS.md`: one branch
per change, no self-merged pull requests, and build, tests, detekt and ktlint before every commit.
The first working version took 43 pull requests; fixing what the phone tests found brought it to
51.

The emulator has no NFC, so the card exchange was debugged by hand on two phones. The results are
in [`NFC_DEVICE_TEST_PLAN.md`](NFC_DEVICE_TEST_PLAN.md).

## Licence

Txoka is free software under the GNU General Public License v3.0 only (`GPL-3.0-only`). See
[`LICENSE`](LICENSE) for the full text. Third-party components and their licences are listed in
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md); the bundled Manrope font uses the SIL Open
Font License 1.1 (`licenses/Manrope-OFL.txt`). The licence texts also ship inside the APK under
`assets/`.

Release history: [`CHANGELOG.md`](CHANGELOG.md).
