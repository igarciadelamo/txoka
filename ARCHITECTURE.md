# Architecture

How the app is put together, what travels over the tag and why the design looks like this. The
summary is in the [`README`](README.md) and the checks are in [`TESTING.md`](TESTING.md).

## Features in detail

### Your card

- Name plus at least one of the four details: a phone number, an email address, a LinkedIn handle or
  an Instagram handle.
- Optional LinkedIn and Instagram handles, shared as bare usernames. The card shows both in one row
  with only the username, never the full link; tapping a link in the received detail opens the
  browser or the installed app. When a card has no phone or email the handles take the main slot,
  full width and at title size.
- Every value you fill in is shared. There are no per-field sharing flags.
- Appearance is edited in background, mosaic, effect and text layers. Gradients, holograms,
  stained glass, nacre and brushed metal support two colours or four corners; a flat background
  uses two mosaic inks. The three materials use bundled reference-derived textures with local
  palette remapping and two-axis lighting. Six procedural geometries and 23 local SVGs require
  no network access. Name and contact data each support six lettering finishes, four sizes and an
  optional RGB colour. Null appearance preserves
  the legacy gradient and WCAG foreground calculation. Explicit text colours remain the user's
  choice.
- The editor validates the phone number and the email, keeps the preview in sync and saves the
  whole card at once. Back discards the draft; the overflow menu deletes the card after a
  confirmation.

The material bases are shared 480 × 303 RGB assets, `glass-color.rgb`, `nacre-color.rgb` and
`metal-color.rgb`, made by directly editing the approved reference to remove text and UI. Glass
joints remain near hairline width. Palette remapping uses the chosen two colours or four corners
while preserving the source texture and highlight structure. Glass and nacre also use local
unsigned eight-bit relief maps for surface normals. Texture coordinates stay anchored to the
card, and changing relative light responds to both tilt axes. The material surfaces receive no
added black legibility scrim. The reference, edited PNGs and provenance are kept in `store/material-sources/`.

### Sharing over NFC

- The phone emulates an NFC Forum Type 4 Tag that carries the card as a vCard 3.0 NDEF record.
- Which phone acts as the card is a controller race, so the hub picks the role: **Send** advertises
  the card with host card emulation, **Receive** only listens through reader mode. The role lives in
  memory and starts on Send at every launch.
- After receiving a card while in Receive, the phone offers its own card for 60 seconds so the
  other phone can read it back on a second tap. The offer is one shot, which is what stops two
  phones from bouncing cards forever.
- An optional setting requires the device to be unlocked before the card is served.
- When the device has no NFC hardware, the app explains the limitation and hides the sharing
  controls instead of failing silently. QR sharing and image import remain available.

### Sharing and reading QR

- Double-tapping a card flips it 180 degrees around its Y axis. The back keeps the chosen style
  and centres a black-and-white QR code with a four-module quiet zone, drawn at integer pixel
  sizes. A visible **Show QR** button and accessibility action provide the same operation;
  double-tapping the back or using the button returns to the front.
- The QR contains the UTF-8 vCard 3.0 text, including the compact STYLE extension, with no NDEF
  framing or Android Application Record. Bundled ZXing generates and decodes it offline at error
  correction M. Txoka restores the supported appearance; a native camera or another contact
  scanner may import the basic contact and ignore the style.
- The scanner offers **Use camera** and image import through Android's system file picker.
  CameraX supplies frames to bundled ZXing only after the user chooses **Use camera** and grants
  `CAMERA`. Image import needs neither camera nor storage permission. Both paths decode on the
  device and send the result to the existing confirmation queue.
- NFC and camera features are optional. A device without either can still create cards, use the
  library and import a QR image. Private received-contact notes are excluded from shared vCards.

### Receiving

- Tap a Txoka phone, or any NDEF tag that carries a `text/vcard` record, and the card opens for
  confirmation before it is stored.
- Nothing is saved until the card is accepted.
- Cards that arrive while the confirmation is open wait in a queue instead of replacing each other:
  the sheet shows the first one, says how many are behind it, and moves on as you accept or
  discard. An identical card already waiting is ignored, so a repeated tap does not stack it.
- The `NDEF_DISCOVERED` intent filter also handles a tag while Txoka is not in the foreground.
- QR camera and image results use the same confirmation and duplicate-handling path. A successful
  scan does not itself save a contact.

### Received contacts

- Cover Flow is the default library view, with category shelves and a searchable list as alternatives.
  All, favourites and recent filters use the same received contacts and private organisation.
- Cover Flow keeps the active card frontal, angles its neighbours and snaps after a swipe or
  explicit previous/next action. Only the active visible card runs motion and effects; neighbouring
  previews stay static. Tapping the active card opens its full-screen viewer and contact QR.
- Each private category has a name, icon, first RGB colour and optional second RGB colour. A missing
  second colour means a flat background. The Cover Flow backdrop interpolates those colours with
  the pager position, remaining stable while adjacent cards belong to the same category.
- The organiser edits category appearance with a local preview, reorders categories and card
  thumbnails, moves cards between categories and manages favourites. Deleting a category leaves
  its contacts available without that category. These actions never edit the sender's appearance.
- Detail view with the sender's card, the date, call and email actions, a note and delete.
- LinkedIn and Instagram handles, when present, open the profile in the browser or the installed
  app. No permission is needed: the link leaves the app and Txoka never fetches anything.
- *Add to contacts* opens the system editor prefilled through `ContactsContract.Intents.Insert`,
  so Txoka never asks for `WRITE_CONTACTS`.
- Sharing a card again refreshes the stored contact instead of adding a copy: a normalised phone
  (digits only), a lowercased email or a bare social handle identifies it, the sender's fields win,
  the received date is refreshed and the local note is kept. The recent filter follows that date;
  manual library organisation remains separate. Cards that only share a name are kept separately.
- Received cards keep the sender's palette and supported appearance. Unknown or malformed style
  extensions fall back to the palette while preserving the contact.
- Rotation must preserve the selected card, open viewer and in-progress category editor rather
  than dismissing the current task. Persisted organisation is distinct from this transient UI state.

### Privacy and storage

- Local-first: no backend, no account and no `INTERNET` permission. The app itself never uploads
  anything.
- The profile and the sharing preferences live in DataStore; received contacts and their notes live
  in a local Room database. Room schema 3 adds a nullable encoded appearance column through a real
  migration from schema 2. Existing colours, notes and identities are retained.
- Library organisation has its own `library_organization` preferences DataStore and
  `LibraryOrganizationRepository`. It stores category definitions and order, contact-ID assignments,
  per-category card order, favourite IDs and the selected view mode. It does not add a Room column
  or migration, change `Profile` or `ReceivedContact`, or change any NFC/QR field or byte budget.
  Deleted contacts have their associated organisation metadata removed; empty-library undo restores
  the saved assignments, order and favourites alongside the contacts.
- Camera frames and selected QR images are decoded on device. Txoka does not persist or upload
  their raw images; only a contact the user confirms is stored. Its supported appearance is kept,
  while private notes, category names/icons/colours, ordering and favourites never enter NFC or QR.
- With Android backup enabled, the system may copy the Room database and DataStore preferences,
  including private library organisation, to the user's own Google backup for restoration.
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
optional: QR provides another way to share and receive. Camera hardware is also optional because
the QR scanner can import an image through the system file picker.

## Design

The visual system is "editorial": a warm canvas (`#F7F5F2`, dark `#121113`), white surfaces with
20–28 dp radii, one strong colour per screen and the card as the hero. Manrope (SIL Open Font
License, see `licenses/Manrope-OFL.txt`) covers display, titles and body; Android falls back to the
system font for scripts the bundled weights do not include.

Tokens live in `ui/theme`: `Spacing`, `Corners`, `TxokaShapes`, the light and dark schemes and the
extra roles (hairline, accent-soft, success, warning) exposed through `MaterialTheme.txoka`. Shared
pieces are `ContactCard` (hero and preview layouts), `TxokaField`, `Eyebrow`, `TxokaAppBar` and the
shared buttons, so screens do not invent their own paddings.

The library and full-screen card viewer adapt to the current window orientation. In landscape,
the immersive viewer hides system bars, fits the card to the available height at the physical
ISO ID-1 ratio and keeps movement, QR and close controls in a compact side rail. It does not
stretch the card to fill a wider screen or place those controls over either face. The library's
landscape controls likewise sit beside its card area. Sensor and animation work pauses while
another viewer or editor covers the previews.

There is a single splash: the Android one. It shows the brand mark over the brand indigo and, on
Android 12 and newer, the wordmark as a branding image at the bottom. Nothing else is drawn on top,
so no second splash appears.

## Stack

Kotlin, Jetpack Compose with Material 3, Hilt, coroutines and Flow, Room, DataStore and
libphonenumber, with CameraX and bundled ZXing for offline QR scanning. Single Gradle module,
minSdk 26, target and compile SDK 37. Manrope is bundled as five static weights, subset to Latin.

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
│ ShareScreen · ProfileEditor · QR scan · Settings · Received detail   │
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
state when the reader goes away. A profile above the NFC limit clears the previously published
card, so a QR-only profile cannot leave an older contact available through HCE. `QrVCardCodec`
serializes the same profile to bounded UTF-8 text independently of the NDEF transport.

**Receiving.** Three paths feed the same confirmation overlay:

1. While the hub is on screen, `NfcReader` reads the tag itself: reader mode on **Receive** and
   foreground dispatch on **Send**. Both decode the NDEF message and hand the card over.
2. The activity's `NDEF_DISCOVERED` filter covers the case where the system launches Txoka with a
   tag intent while the app is not in the foreground.
3. The QR screen decodes camera frames or a selected image with bundled ZXing, validates the
   bounded vCard text through `QrVCardCodec` and submits the parsed contact.

All push a `VCardPayload` into `IncomingCards`, which keeps them in arrival order. The UI collects
the queue, shows the first card and offers the next once it is accepted or discarded.

**Storing.** `RoomReceivedContactRepository` maps entities to domain objects, finds the contact a
card belongs to by its keys and persists the sender's colours with the contact.

## Protocol

The card is a vCard 3.0 document carried in a single NDEF record with MIME type `text/vcard`,
followed by an Android Application Record for `es.igarciadelamo.txoka`. The AAR is what makes the
receiving phone open Txoka instead of showing an app chooser or letting another NFC app claim the
tag; it adds 40 bytes to the message and readers ignore it. Txoka adds properties to the
vCard that other readers simply ignore:

| Property | Meaning |
| --- | --- |
| `X-TXOKA-PROTOCOL` | Payload version, currently `1` |
| `X-TXOKA-CARD-ACCENT` | First gradient colour, `#RRGGBB` |
| `X-TXOKA-CARD-ACCENT2` | Second gradient colour |
| `X-TXOKA-STYLE` | Optional versioned compact appearance; see [the V1 contract](CARD_APPEARANCE.md) |

Social handles travel as standard `URL` properties, one per network, so any contact app can open
them:

```
URL;TYPE=LINKEDIN:https://www.linkedin.com/in/isidro-garcia
URL;TYPE=INSTAGRAM:https://www.instagram.com/isidro.png
```

Text values are escaped as vCard requires (`\n`, `\,`, `\;`, `\\`) and long lines are folded at
75 octets. Only `FN`, `TEL`, `EMAIL`, `URL`/`X-SOCIALPROFILE`, `N`, the supported `X-TXOKA-*` properties
and `VERSION` are read; everything else, including photos and addresses, is ignored. A handle can
arrive as `URL` or as `X-SOCIALPROFILE` from another app, and when the type is missing the network
is recognised from the link itself. Invalid colours fall back to the default palette, so a card
written by another app still loads. The complete NDEF message is capped at 768 bytes. HCE publishes
nothing above the limit and clears any previous card. The editor can save a larger QR-only card
if its serialized UTF-8 vCard fits the independent 2,048-byte QR cap, and warns that NFC is
unavailable. Exceeding the QR cap blocks Save. Neither transport truncates contact fields or
silently removes STYLE.

The appearance property adds at most 40 bytes, or 43 marginal NDEF bytes if a short-record length
expands. The 768-byte limit includes the MIME and AAR records; Type 4's two-byte NLEN is additional.
STYLE has strict version, selector, length and canonical-value checks. A rejected extension never
partially changes the palette or contact. Neither sensor values nor SVG paths are transmitted.
The three-byte fixed header and maximum 16-byte style remain unchanged. Background IDs 0, 1 and 6
retain their meanings; 2, 3 and 4 identify stained glass, nacre and brushed metal. IDs 5 and 7 reject
the entire style and preserve the contact and legacy palette. Bundled material RGB and relief
samples, surface normals and lighting never enter the payload or profile storage model.

QR uses the same vCard text and extension without MIME/NDEF framing, AAR or NLEN. Its cap measures
UTF-8 bytes rather than characters. Error correction M and a UTF-8 ECI header are used; the
2,048-byte boundary is covered by a real ZXing symbol round trip. The NFC permission, package,
AIDs and APDU behavior are unchanged; `CAMERA` is the sole added permission for optional scanning.

The emitter emulates a NFC Forum Type 4 Tag. Two AIDs are registered:

| AID | Purpose |
| --- | --- |
| `D2760000850101` | NFC Forum NDEF AID, understood by standard NDEF readers |
| `F054584F4B4101` | Txoka AID, avoids the system chooser when other NDEF apps are installed |

The reader sequence is:

1. `SELECT AID`.
2. `SELECT CC` (`E103`) and `READ BINARY` of the 15-byte capability container.
3. `SELECT NDEF` (`E104`), `READ BINARY` of the two-byte NLEN and then of the message.

The reading side switches mechanism with the hub role. On **Receive** it uses
`NfcAdapter.enableReaderMode`, which hands the tag straight to the app: several vendor builds never
deliver `NDEF_DISCOVERED` intents to third-party apps, and the reader-mode callback bypasses that
dispatch, the activity chooser and the tag-scan allowlist. On **Send** it keeps
`enableForegroundDispatch` plus `CardEmulation.setPreferredService`, because reader mode turns host
card emulation off (`NfcService.computeDiscoveryParameters`) and the phone has to keep answering
tags. The two modes are exclusive, which is exactly what the role means.

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
- **Camera access is optional.** Request `CAMERA` only after **Use camera**. The system file
  picker grants access to a selected QR image without a broad storage permission.
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
- QR flipping has a visible button and a named accessibility action in addition to the double-tap
  gesture. The QR has an accessible description, and the flip respects disabled system animation.
- Legacy card contrast is picked from the worst gradient stop and every legacy preset reaches
  4.5:1 for body text. `CardContrastTest` guards those presets. Promoting a legacy card into an
  explicit appearance preserves that foreground; a separately selected text colour, mosaic or
  effect requires a new visual legibility check and has no automatic contrast guarantee.
