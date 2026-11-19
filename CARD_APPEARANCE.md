# Card appearance and NFC style V1

Txoka renders appearance locally. NFC carries known identifiers and RGB values, never artwork,
animation frames, shaders, fonts, sensor readings or calibration. The layer order is background,
mosaic, elemental effect, then name and contact text.

The [interactive design reference](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/)
is a separately hosted browser demo. Its URL is public and unlisted; knowing the link grants access.
It is not an APK, an authenticated preview or evidence of an NFC exchange. Its linked
[design plan](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/docs/plan.html),
[protocol proposal](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/docs/nfc.html)
and [CI proposal](https://txoka-card-lab.olopopo.chatgpt.site/p/6744e651529f19ab8340aaf8fd7de3ef8db8/docs/ci.html)
also remain available for review. This document specifies the Android wire format.

## Local rendering

- Background: two-colour or four-corner gradient, holographic reflection, or flat colour.
- Mosaic: six procedural geometries and 23 bundled SVG compositions. All inherit the background's
  palette and material. With four corners, the two mosaic inks are TL/BR, not TL/TR.
- Effect: none, water, earth, wind or fire, rendered after the mosaic.
- Text: plain, embossed, raised, framed, metallic or bubble; four size presets and an optional RGB
  colour, independently for the name and contact data. Metallic means coloured glyphs, with an
  outline and moving sheen, without a metallic panel behind them.
- Motion and playback are local. A device without motion sensors can still display the card and
  use manual controls. Sensor calibration, manual tilt and pause are not shared.

Legacy cards have no appearance object. They retain automatic contrast and the historical gradient.
Explicit styles use white text by default, or `#9BA5B5` for metallic glyphs. A user-selected colour
does not imply a contrast guarantee. Check legibility over every selected mosaic and effect.

The physical reference is ISO ID-1, 85.60 × 53.98 mm, approximately 1.586:1. This is not the golden
ratio. Accessibility may require extra height when large text or long contact details need it.

## Transport and byte budget

The first NDEF record remains MIME `text/vcard`, carrying vCard 3.0. The Android Application Record
for `es.igarciadelamo.txoka` follows it. `X-TXOKA-PROTOCOL:1` remains the contact protocol version.
The existing AIDs, APDUs, unlock rules, package name and permissions do not change.

Both `X-TXOKA-CARD-ACCENT` and `X-TXOKA-CARD-ACCENT2` remain present. For four-corner styles they are
the top-left and bottom-right colours, providing a complete diagonal fallback to older readers.

The optional extension is one unfolded ASCII property:

```text
X-TXOKA-STYLE:1:<unpadded-base64url>\r\n
```

The complete NDEF message, including record headers and the AAR, must fit **768 bytes**. Type 4's
two-byte NLEN is additional, so the corresponding file can occupy 770 bytes. Saving must fail
when the complete message exceeds the limit; never drop style or truncate a field silently.

The extension has three fixed bytes and up to 13 optional bytes. Including its prefix and CRLF,
it occupies 22–40 bytes. A 15-byte ordinary style occupies 38 bytes; a 16-byte SVG style occupies
40. Crossing the NDEF short-record boundary can add another three bytes to the total message.
Thus the maximum marginal cost is 43 bytes. A 728-byte message plus a 40-byte property fits;
729 plus 40 does not, when record framing stays unchanged.

## Fixed header

All identifiers are explicit wire values. Do not use enum ordinals.

| Byte | Bits | Field | Values |
| --- | --- | --- | --- |
| 0 | 0–2 | Background | 0 gradient, 1 hologram, 6 flat; others reserved |
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

The test suite must cover unsigned selectors, canonical RGB handling, malformed extension fallback,
old vCards, escaped/folded Unicode fields, NDEF framing boundaries and the exact 768/769-byte edge.
Storage upgrade tests must preserve old colours, notes and contact identities. Actual sensor
response and NFC interoperability still require physical devices; JVM tests cannot establish them.
