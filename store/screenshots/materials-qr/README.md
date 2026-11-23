# Native materials and QR review

Eleven raw Android screenshots show the material choices, saved nacre card, QR back and portrait/landscape viewer in the existing Txoka app. Open each image for its full native PNG.

Application source: [`dd9c3a58464373776391f27a49dc8a3abc522a93`](https://github.com/igarciadelamo/txoka/commit/dd9c3a58464373776391f27a49dc8a3abc522a93).

Captured on 21 November 2026 with a Pixel 6 emulator running Android 15 (API 35), at 420 dpi. Portrait PNGs are 1080 × 2400; landscape PNGs are 2400 × 1080. `UiAutomation.takeScreenshot` supplies complete screen captures, with no cropping, image edits or compositing. Marta Ruiz, the phone number and the example.com address are fictitious. The app UI is in English for these captures; Spanish resources are also bundled. The emulator has no NFC hardware, so the NFC-unavailable notice is expected.

## Screens

| PNG | Native dimensions | Scene |
| --- | --- | --- |
| [01-hub-hologram.png](01-hub-hologram.png) | 1080 × 2400 | Hub with the holographic fish mosaic and contact card actions. |
| [02-editor-material-options.png](02-editor-material-options.png) | 1080 × 2400 | Editor showing all six background choices beneath the pinned card preview. |
| [03-editor-stained-glass.png](03-editor-stained-glass.png) | 1080 × 2400 | Stained-glass source artwork and thin joints recoloured by the existing palette. |
| [04-editor-nacre.png](04-editor-nacre.png) | 1080 × 2400 | Nacre layers and highlights in the card and matching material choice. |
| [05-editor-brushed-metal.png](05-editor-brushed-metal.png) | 1080 × 2400 | Directional brushed metal in the card and matching material choice. |
| [06-hub-material-front.png](06-hub-material-front.png) | 1080 × 2400 | Saved nacre front in the hub, with a visible Show QR action. |
| [07-hub-qr-back.png](07-hub-qr-back.png) | 1080 × 2400 | QR back in the hub, preserving nacre around the black-and-white code. |
| [08-viewer-portrait-front.png](08-viewer-portrait-front.png) | 1080 × 2400 | Portrait card viewer with contact front and motion controls. |
| [09-viewer-portrait-qr.png](09-viewer-portrait-qr.png) | 1080 × 2400 | Portrait QR viewer with the complete code and white quiet zone. |
| [10-viewer-landscape-qr.png](10-viewer-landscape-qr.png) | 2400 × 1080 | Landscape QR viewer with separate movement controls. |
| [11-viewer-landscape-front.png](11-viewer-landscape-front.png) | 2400 × 1080 | Landscape nacre front with the joystick and viewer actions beside it. |

## Provenance and checks

Keep the capture manifest beside the PNGs. Its application source SHA must identify the captured app, and each scene records the filename, native dimensions and SHA-256.

The captured source passed **17 instrumented tests**: 16 permanent repository cases and one temporary capture flow. All cases passed with zero failures or errors. The raw [instrumentation log](instrumentation.log) is retained; [test-status.json](test-status.json) is derived from its actual runner records.

An independent jsQR reader decoded the three complete screenshot PNGs `07-hub-qr-back.png`, `09-viewer-portrait-qr.png` and `10-viewer-landscape-qr.png`. Each contained the same 220-byte UTF-8 vCard with nacre STYLE `1:A2gB`; [qr-decoding.json](qr-decoding.json) records its hash and screenshot locations. QR visibility and pause survive landscape rotation, and pause survives returning to portrait. The viewer uses dark system icons over its light background.

The checks distinguish a scannable code shown by the app from a camera scanning flow. Camera access/denial, user-selected image import, physical NFC and physical motion-sensor sensitivity require their own validation. Static captures do not establish all text contrast, accessibility or animated lighting behaviour.

## Earlier review

The seven screenshots in [`../review/`](../review/README.md) belong to source `3d7ce41` from 20 November 2026. They remain historical editor/layout context and do not validate the current material or QR implementation.
