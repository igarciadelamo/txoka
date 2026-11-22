# Card appearance and NFC style V1

Txoka renders appearance locally. NFC and QR carry known identifiers and RGB values, never
artwork, animation frames, shaders, fonts, sensor readings or calibration. The layer order is
background, mosaic, elemental effect, then name and contact text.

The [interactive design reference](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/)
is a separately hosted browser demo. Its URL is public and unlisted; knowing the link grants access.
It is not an APK, an authenticated preview or evidence of an NFC exchange. Its linked
[design plan](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/docs/plan.html)
and [NFC contract](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/docs/nfc.html)
also remain available for review. This document specifies the Android wire format.

## Local rendering

- Background: two-colour or four-corner gradient, holographic reflection, stained glass, nacre,
  brushed metal, or flat colour.
- Mosaic: six procedural geometries and 23 bundled SVG compositions. All inherit the background's
  palette and material. With four corners, the two mosaic inks are TL/BR, not TL/TR.
- Effect: none, water, earth, wind or fire, rendered after the mosaic.
- Text: plain, embossed, raised, framed, metallic or bubble; four size presets and an optional RGB
  colour, independently for the name and contact data. Metallic means coloured glyphs, with an
  outline and moving sheen, without a metallic panel behind them.
- Motion and playback are local. A device without motion sensors can still display the card and
  use manual controls. Sensor calibration, manual tilt and pause are not shared.

Stained glass, nacre and brushed metal use shared local 480 × 303 RGB assets: `glass-color.rgb`,
`nacre-color.rgb` and `metal-color.rgb`. These are direct edits of the approved reference with
text and UI removed, including near-hairline glass joints. The reference, edited PNGs and
provenance are retained in `store/material-sources/`.

The device remaps the existing palette, including optional top-right and bottom-left colours,
while preserving the texture and highlight structure. Two local unsigned eight-bit relief maps
provide glass and nacre surface detail for normals. The local 480 × 303 unsigned eight-bit
`glass-cells.u8` atlas assigns optical orientations to the 24 approved glass panes; it does not
redraw their geometry or joints. Texture coordinates remain anchored to the card; relative light
changes with both tilt axes. The three materials receive no added black legibility scrim.
At the canonical coral/cyan palette and neutral pose, base-raster RGB identity passed on
21 November 2026. Nine actual native/browser renders were compared: all three neutral reference
images were byte-identical, and every channel in tilted/reference or default-palette images
matched within one RGB value. Composited text, effects, other palettes and display scaling still
have separate visual checks. No material-specific settings or fields are added to NFC, DataStore or Room;
asset bytes and normals never enter NFC or QR. Flat remains the only background that rejects
four corners.

Production SVG masks use `ALPHA_8`. The renderer paints their coverage directly through the
palette shader, leaving uncovered material pixels unchanged, and resets the paint before later
lighting. The ARGB-mask fallback remains supported. This corrects a composition failure where
colour could cover transparent SVG regions. It changes no selector, palette or transport bytes.

Legacy cards have no appearance object. They retain automatic contrast and the historical gradient.
Explicit styles use white text by default, or `#9BA5B5` for metallic glyphs. A user-selected colour
does not imply a contrast guarantee. Check legibility over every selected mosaic and effect.

The physical reference is ISO ID-1, 85.60 × 53.98 mm, approximately 1.586:1. This is not the golden
ratio. Accessibility may require extra height when large text or long contact details need it.

## Private library presentation

Received cards reuse their stored contact and appearance in three views: Cover Flow by default,
category shelves, and a searchable list. The active Cover Flow card faces forward; neighbouring
cards are angled static previews. Swiping snaps to a card, with explicit previous/next controls as
an alternative. Only the active visible card runs motion/effects. Opening it uses the same
full-screen renderer and generates the QR from that selected contact and supported appearance.

Categories are private organisation, distinct from the card's mosaic catalogue categories. A
library category has a name, icon and flat or two-colour gradient background. Its colours drive
the Cover Flow backdrop: fractional pager movement interpolates between neighbouring categories,
with no colour change between cards in the same category. Category editing previews the draft
locally before confirmation; cards and categories can be reordered, cards moved and favourites set.

`library_organization` is a separate preferences DataStore for categories, ordering, assignments,
favourites and view mode. This revision adds no Room schema change and no profile, NFC or QR
field. Library category names, icons and colours, card order and favourites never enter STYLE,
legacy accent fields or the contact vCard. The 768-byte NDEF and 2,048-byte QR budgets are unchanged.

Portrait and landscape keep the ISO ID-1 ratio. The immersive landscape viewer fits the card to
the available height and places movement, QR and close controls in a compact side rail. Rotation
must preserve the selected card, open viewer, playback state and category draft. These are UI
requirements; native results recorded below cover the earlier materials/QR source, not this
library and immersive-viewer revision.

## Transport and byte budget

The first NDEF record remains MIME `text/vcard`, carrying vCard 3.0. The Android Application Record
for `es.igarciadelamo.txoka` follows it. `X-TXOKA-PROTOCOL:1` remains the contact protocol version.
The existing AIDs, APDUs, unlock rules, package name and NFC permissions do not change. Optional
QR scanning adds only the explicitly authorised `CAMERA` permission, requested after **Use camera**.

Both `X-TXOKA-CARD-ACCENT` and `X-TXOKA-CARD-ACCENT2` remain present. For four-corner styles they are
the top-left and bottom-right colours, providing a complete diagonal fallback to older readers.

The optional extension is one unfolded ASCII property:

```text
X-TXOKA-STYLE:1:<unpadded-base64url>\r\n
```

The complete NDEF message, including record headers and the AAR, must fit **768 bytes**. Type 4's
two-byte NLEN is additional, so the corresponding file can occupy 770 bytes. A valid card above
the NFC cap can still be saved for QR with an NFC-unavailable warning. Exceeding the independent
QR limit blocks Save. HCE fails closed and clears a previously published card. Never drop style
or truncate a field silently to fit either transport.

The extension has three fixed bytes and up to 13 optional bytes. Including its prefix and CRLF,
it occupies 22–40 bytes. A 15-byte ordinary style occupies 38 bytes; a 16-byte SVG style occupies
40. Crossing the NDEF short-record boundary can add another three bytes to the total message.
Thus the maximum marginal cost is 43 bytes. A 728-byte message plus a 40-byte property fits;
729 plus 40 does not, when record framing stays unchanged.
Choosing stained glass, nacre or brushed metal changes only the background identifier; these limits
and all optional block positions remain unchanged.

## QR transport and card back

QR carries the plain UTF-8 vCard 3.0 text with the same `X-TXOKA-STYLE` property. It contains no NDEF
record headers, MIME wrapper, Android Application Record or Type 4 NLEN. Its independent limit is
**2,048 UTF-8 bytes**, at QR error correction M with a UTF-8 ECI header. The boundary has a real
ZXing version-38 symbol round-trip test, which passed on 21 November 2026 along with exact
2,048-byte acceptance and 2,049-byte rejection. Another phone's
native camera may import the basic contact and ignore STYLE. Txoka's scanner restores supported
appearance through the same strict codec and confirmation flow as NFC.

Double-tap a card, or use the visible **Show QR** button or accessibility action, to flip it 180
degrees around its Y axis. The back retains the chosen style and centres a black-and-white QR
with a four-module quiet zone and integer pixel modules. Double-tap the back or use the button to
return. Private received-contact notes never enter the vCard.

CameraX and bundled ZXing decode locally. **Use camera** requests optional camera access; importing
an image through the system file picker needs neither camera nor storage permission. NFC and
camera hardware remain optional. Txoka does not persist or upload raw camera frames or selected
QR images, and a scanned contact is saved only after confirmation.

## Fixed header

All identifiers are explicit wire values. Do not use enum ordinals.

| Byte | Bits | Field | Values |
| --- | --- | --- | --- |
| 0 | 0–2 | Background | 0 gradient, 1 hologram, 2 stained glass, 3 nacre, 4 brushed metal, 6 flat; 5 reserved, 7 unknown |
| 0 | 3 | Four corners | 0 two colours, 1 four; flat requires 0 |
| 0 | 4–6 | Mosaic | 0 none, 1 triangles, 2 squares, 3 Cairo pentagons, 4 adapted five-arm stars, 5 hexagons, 6 dots, 7 escaped selector |
| 0 | 7 | Reserved | Must be 0 |
| 1 | 0–2 | Name finish | 0 plain, 1 embossed, 2 raised, 3 framed, 4 metallic, 5 bubble |
| 1 | 3–5 | Contact finish | Same six values |
| 1 | 6–7 | Name size | 0 S, 1 M, 2 L, 3 XL |
| 2 | 0–1 | Contact size | 0 S, 1 M, 2 L, 3 XL |
| 2 | 2–4 | Effect | 0 none, 1 water, 2 earth, 3 wind, 4 fire; others reserved |
| 2 | 5 | Name RGB present | Adds three bytes |
| 2 | 6 | Contact RGB present | Adds three bytes |
| 2 | 7 | Reserved | Must be 0 |

Optional blocks follow in this exact order:

1. One unsigned selector byte, only when mosaic = 7.
2. Top-right RGB, then bottom-left RGB: six bytes, only with four corners.
3. Name RGB: three bytes, only when its flag is set.
4. Contact RGB: three bytes, only when its flag is set.

There is no independent mosaic material, opacity, ink pair or animation block. RGB bytes are R, G,
B. An explicit text colour equal to its finish's default is encoded by omitting its flag and block.
Such a block on input is noncanonical and invalidates the extension.

## SVG selectors

Only these exact selectors are valid; gaps are not accepted. The source assets and provenance
manifest live under `app/src/main/assets/card-artwork/`. SVG paths and metadata never travel over
NFC. IDs and geometry must not be reused for unrelated artwork after a writer ships.

| Selector | Asset ID |
| ---: | --- |
| 160 | nature-fish |
| 161 | nature-birds |
| 162 | nature-reptiles |
| 192 | geo-truchet-labyrinth |
| 193 | geo-truchet-double-tracks |
| 194 | geo-orthogonal-labyrinth |
| 195 | geo-bauhaus-quarter-rounds |
| 196 | geo-sine-wave-moire |
| 197 | geo-sawtooth-waves |
| 198 | geo-herringbone-chevrons |
| 199 | geo-diamond-vibrations |
| 200 | geo-concentric-orbitals |
| 201 | geo-vesica-piscis-lattice |
| 202 | geo-honeycomb-cartography |
| 203 | geo-isometric-cube-fields |
| 204 | geo-hexagon-pinwheels |
| 205 | geo-equilateral-fractals |
| 206 | geo-triangular-turbines |
| 207 | geo-rhombus-mirrors |
| 208 | geo-eightfold-starwork |
| 209 | geo-twelvefold-zellige |
| 210 | geo-interlocking-greek-crosses |
| 211 | geo-optical-woven-grid |

The three nature compositions were generated for this project and faithfully vectorized; they
are not reproductions of Escher. The twenty geometric SVGs were supplied by the contributor.
That describes provenance, not an independently verified licence or exclusive ownership. Retired
unlicensed reproductions are excluded. See [third-party notices](THIRD_PARTY_NOTICES.md).

## Compatibility and rejection

A missing STYLE preserves the legacy foreground calculation. A writer may omit an explicit style
only if the entire result is equivalent to legacy, including the foreground. A three-byte header
must still be written when explicit white differs from legacy's automatic dark text.

Reject the whole STYLE, while retaining a valid contact and its legacy colours, for an unknown
version, duplicate property, unknown selector, reserved bit or value, invalid/noncanonical Base64url,
missing or trailing byte, flat plus four corners, or an explicit default RGB block. Length is
derived from the header; membership in a list of possible lengths is insufficient.

Earlier browser-only drafts of V1 were never production writers and are not compatibility targets.
Do not guess which draft a byte string belongs to. After the first writer ships, incompatible
changes require a new appearance version.

Background IDs 0, 1 and 6 and all existing byte positions remain immutable. IDs 2, 3 and 4 now identify
stained glass, nacre and brushed metal; they do not change the appearance or contact version. Readers
that predate these materials treat the unfamiliar background as an invalid STYLE and display the
complete contact with its legacy diagonal colours. IDs 5 and 7 still reject the whole extension,
including otherwise valid mosaic, text and corner blocks.

## Golden examples

These strings exclude the property's terminating CRLF.

| Meaning | Raw bytes (hex) | Property |
| --- | --- | --- |
| Gradient, plain M name, bubble M contact | `00 68 01` | `X-TXOKA-STYLE:1:AGgB` |
| First supplied SVG, hologram, earth | `71 68 09 C0` | `X-TXOKA-STYLE:1:cWgJwA` |
| Last supplied SVG, flat, fire | `76 68 11 D3` | `X-TXOKA-STYLE:1:dmgR0w` |
| Bubble M name and contact | `00 6D 01` | `X-TXOKA-STYLE:1:AG0B` |
| Independent name/contact styles, sizes and RGB | `00 11 23 12 34 56` | `X-TXOKA-STYLE:1:ABEjEjRW` |
| Maximum style: four-corner hologram, SVG 211, fire, custom text colours | `79 D4 73 D3 01 02 03 04 05 06 07 08 09 0A 0B 0C` | `X-TXOKA-STYLE:1:edRz0wECAwQFBgcICQoLDA` |
| Stained glass, plain M name, bubble M contact | `02 68 01` | `X-TXOKA-STYLE:1:AmgB` |
| Nacre, plain M name, bubble M contact | `03 68 01` | `X-TXOKA-STYLE:1:A2gB` |
| Brushed metal, plain M name, bubble M contact | `04 68 01` | `X-TXOKA-STYLE:1:BGgB` |
| Maximum style with stained glass | `7A D4 73 D3 01 02 03 04 05 06 07 08 09 0A 0B 0C` | `X-TXOKA-STYLE:1:etRz0wECAwQFBgcICQoLDA` |
| Maximum style with nacre | `7B D4 73 D3 01 02 03 04 05 06 07 08 09 0A 0B 0C` | `X-TXOKA-STYLE:1:e9Rz0wECAwQFBgcICQoLDA` |
| Maximum style with brushed metal | `7C D4 73 D3 01 02 03 04 05 06 07 08 09 0A 0B 0C` | `X-TXOKA-STYLE:1:fNRz0wECAwQFBgcICQoLDA` |

The codec suite enumerates all 3,801,600 valid background, corner, mosaic, effect and text
combinations, including each new material with every optional-block combination. Its twenty
independent goldens preserve the fourteen prior encodings and add minimum and maximum examples
for each new background. On 21 November 2026 the materials/QR baseline at app source `dd9c3a5`
passed all 280 JVM tests in 43 suites, with zero failures, errors or skips. This includes unsigned selectors, canonical RGB,
malformed extension fallback, old vCards, escaped/folded Unicode, NDEF framing and the exact
768/769-byte edge, real ZXing QR boundaries and QR-only save/HCE-clearing behaviour. The 21 pure
material tests are included in the 280.
Debug app/test APK compilation, Detekt, ktlint, the 95% coverage gate, debug/release lint and
R8 release APK assembly passed.

On 22 November 2026, app source `d9c1c01340a3cfba3db3a00e058a18745708d43c`, including the
library, immersive viewer and SVG-mask correction, passed all 334 JVM tests in 49 suites,
debug app/test APK compilation, Detekt, ktlint, the 95% coverage gate, both lint variants and
R8 release APK assembly. There are 19 permanent instrumented cases: eight DAO, two migration,
two mosaic, six material and one supplied-SVG/material matrix. The latter checks 20 SVGs across
three materials and three poses, including both tilt axes. All 19 permanent cases plus the
capture flow passed in API 35 run `45043218637`: **20 actual runner cases**. Capture source
`bca1af283ab273494d727859a53cb2629529fc9e` produced **22 raw screenshots**, retaining native
geometry, swipe/category behaviour, organizer edits and card/QR/pause state across rotation.
The [library evidence gallery](store/screenshots/library/README.md) records the source binding,
raw runner log, PNG hashes and layout bounds. Independent jsQR decoding of five whole screens
preserves the own-card and received-card payloads across their respective views, including their
colours and STYLE, without private library metadata. See `TESTING.md` for details and device limits.

The separate demo passes 216 Node cases in 20 files, including five regressions for bounded
Canvas-resource recovery and independent thumbnail buffers. On the published version, Chromium
also passed 120 SVG/material combinations in both selection orders, with visible thumbnails and
independent X/Y response. A controlled transient Canvas-allocation failure recovered without
reloading. These browser checks are separate from Android tests and do not validate Brave on a
physical phone.

On 21 November 2026, app source `dd9c3a58464373776391f27a49dc8a3abc522a93` passed all 16 then-current
repository instrumented tests on an Android 15 / API 35 Pixel 6 emulator, including persistence
upgrades and native Canvas composition. One separate capture flow also passed: the raw
AndroidJUnitRunner log records 17 passes, zero failures and zero errors; its XML summary is derived.
The [native evidence gallery](store/screenshots/materials-qr/README.md) retains eleven raw captures.
Independent jsQR decoding of the whole hub, portrait-viewer and landscape-viewer QR screens produced
the same 220-byte vCard with the same contact, colours and `X-TXOKA-STYLE:1:A2gB`. Manifest probes
confirm QR visibility after landscape rotation and preserved pause after rotation and return to
portrait. QA capture revision: `792d50cd50e3252f4d01283dc73ad1e12dd82240`.

Historical results at `3d7ce41` predate these materials and QR. Camera permission/denial and SAF
image import on real devices, actual sensor response, NFC interoperability and wider composed
visual/accessibility checks still require device validation.
