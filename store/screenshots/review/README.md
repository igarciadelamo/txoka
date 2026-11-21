# Native appearance review

These screenshots show the card appearance controls in the existing Android app, including the
hub, pinned editor preview and full-screen landscape viewer. Open an image for the original PNG.

Captured on a Pixel 6 emulator running Android 15 (API 35), at its native 1080 × 2400 resolution
and 420 dpi. The landscape capture is 2400 × 1080. The images are raw device captures, without
compositing or image edits. Marta Ruiz, the phone number and the example.com address are fictitious.
The emulator has no NFC hardware, so its NFC-unavailable message is expected.
The application sources match
[`3d7ce41`](https://github.com/igarciadelamo/txoka/commit/3d7ce41692e3cc064164e026289ed627ef9178da).

## Card and editor

<p>
  <a href="01-hub-hologram.png"><img src="01-hub-hologram.png" width="220" alt="Holographic fish mosaic on the card in the Txoka hub"></a>
  <a href="02-editor-background.png"><img src="02-editor-background.png" width="220" alt="Pinned card preview and background controls in the native editor"></a>
  <a href="03-editor-mosaics.png"><img src="03-editor-mosaics.png" width="220" alt="Mosaic categories and coloured previews below the card"></a>
</p>

- **Hub:** the personalised card in the existing hub, with the emulator's actual NFC availability.
- **Background:** the card stays visible while its palette and material are changed.
- **Mosaic:** the catalogue shows local designs as coloured previews, grouped by category.

## Lettering, colour and effects

<p>
  <a href="04-editor-lettering.png"><img src="04-editor-lettering.png" width="220" alt="Lettering controls for the name and contact details, including metallic glyphs"></a>
  <a href="05-editor-custom-colour.png"><img src="05-editor-custom-colour.png" width="220" alt="Custom colour controls with hue, saturation, brightness and HEX input"></a>
  <a href="07-editor-fire.png"><img src="07-editor-fire.png" width="220" alt="Fire as an independent effect over the gradient card"></a>
</p>

- **Lettering:** separate name and contact targets, with six finishes and metallic glyphs in the preview.
- **Custom colour:** hue, saturation, brightness and HEX controls below the pinned preview.
- **Fire:** an elemental effect selected separately from the background and mosaic.

## Landscape viewer

<a href="06-viewer-landscape.png"><img src="06-viewer-landscape.png" width="720" alt="Full-screen card viewer in landscape with movement controls and joystick feedback"></a>

The full-screen viewer places the card beside its motion controls in landscape. Static captures
show the layout; sensor response and NFC interoperability still require physical-device validation.
