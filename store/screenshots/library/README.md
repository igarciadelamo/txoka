# Native visual library and landscape review

Twenty-two raw Android screenshots show the card materials, QR, visual library, category organizer
and maximum-height landscape viewer in Txoka. Open each PNG for its full native screen.

Application source: [`d9c1c01340a3cfba3db3a00e058a18745708d43c`](https://github.com/igarciadelamo/txoka/commit/d9c1c01340a3cfba3db3a00e058a18745708d43c).
QA capture source: `bca1af283ab273494d727859a53cb2629529fc9e`; actual Android run: `45043218637`.

Captured on 22 November 2026 with a Pixel 6 emulator running Android 15 / API 35 at 420 dpi.
Portrait screenshots are 1080 × 2400; landscape screenshots are 2400 × 1080.
`UiAutomation.takeScreenshot` supplies complete native PNGs without cropping, resizing, image edits
or compositing. Marta Ruiz and the seven received contacts use fictional details and example.com
addresses. The interface is English; Spanish resources also ship in the app. The emulator has no NFC
hardware. Test fixtures are installed only by the temporary QA helper and do not seed production.

## Screens

| PNG | Native dimensions | Scene |
| --- | --- | --- |
| [01-hub-hologram.png](01-hub-hologram.png) | 1080 × 2400 | Native hub with a holographic fish mosaic and sample contact details |
| [02-editor-material-options.png](02-editor-material-options.png) | 1080 × 2400 | All six background choices in the actual native editor |
| [03-editor-stained-glass.png](03-editor-stained-glass.png) | 1080 × 2400 | Bundled glass relief and native stained-glass lighting |
| [04-editor-nacre.png](04-editor-nacre.png) | 1080 × 2400 | Native nacre material with the complete card preview |
| [05-editor-brushed-metal.png](05-editor-brushed-metal.png) | 1080 × 2400 | Native directional brushed metal and its WYSIWYG choice |
| [06-hub-material-front.png](06-hub-material-front.png) | 1080 × 2400 | Saved nacre card and offline QR controls in the hub |
| [07-hub-qr-back.png](07-hub-qr-back.png) | 1080 × 2400 | The actual QR back of the saved native contact card |
| [08-viewer-portrait-front.png](08-viewer-portrait-front.png) | 1080 × 2400 | Portrait full-screen native card and movement controls |
| [09-viewer-portrait-qr.png](09-viewer-portrait-qr.png) | 1080 × 2400 | Portrait native viewer showing a complete scannable QR |
| [10-viewer-landscape-qr.png](10-viewer-landscape-qr.png) | 2400 × 1080 | Landscape QR back with persistent pause and movement feedback |
| [11-viewer-landscape-front.png](11-viewer-landscape-front.png) | 2400 × 1080 | Landscape native nacre card with separate controls |
| [12-library-coverflow-work.png](12-library-coverflow-work.png) | 1080 × 2400 | Native CoverFlow with the complete stained-glass card and a Work category backdrop |
| [13-library-coverflow-friends.png](13-library-coverflow-friends.png) | 1080 × 2400 | Centering a Friends card changes the actual carousel background to its category color |
| [14-library-coverflow-landscape.png](14-library-coverflow-landscape.png) | 2400 × 1080 | Landscape CoverFlow preserves the selected card and moves controls into a side rail |
| [15-library-viewer-landscape-front.png](15-library-viewer-landscape-front.png) | 2400 × 1080 | The selected native nacre card uses almost the entire landscape height |
| [16-library-viewer-landscape-qr.png](16-library-viewer-landscape-qr.png) | 2400 × 1080 | The selected received card shares its own offline QR with a complete quiet zone |
| [17-library-viewer-portrait-qr.png](17-library-viewer-portrait-qr.png) | 1080 × 2400 | A received-card viewer retains its QR back and paused state through rotation |
| [18-library-shelves.png](18-library-shelves.png) | 1080 × 2400 | Colored category shelves show native static card thumbnails |
| [19-library-organizer.png](19-library-organizer.png) | 1080 × 2400 | Native library organizer with card thumbnails, drag handles and accessible order buttons |
| [20-library-category-editor.png](20-library-category-editor.png) | 1080 × 2400 | Native category editor with name, icons, background modes and a live category preview |
| [21-library-edited-category.png](21-library-edited-category.png) | 1080 × 2400 | Saved category metadata and card ordering are reflected by the native library |
| [22-library-search.png](22-library-search.png) | 1080 × 2400 | Search filters the real native library while keeping category styling and full-size card rendering |

## Provenance and checks

The [capture manifest](capture-manifest.json) pins the captured application and QA sources and
records native dimensions and SHA-256 for each raw PNG. All **20 instrumented cases** passed:
19 permanent repository cases (eight DAO, two migration, two mosaic, six material and one SVG/material
matrix case) plus
the temporary native capture flow. [test-status.json](test-status.json) and
[junit-derived.xml](junit-derived.xml) are derived from the retained raw
[instrumentation log](instrumentation.log); the XML is not a runner-produced JUnit report.

The actual UI flow exercised swipe and snapping, previous/next controls, category backdrop changes,
card ordering, favorites, search, editing and saving a category, and rotation with an unsaved category
draft. It checked that cards in one category retain the same backdrop and that both the selected
received card and its QR/pause state survive rotation.

Accessibility bounds recorded in the manifest retain the physical ID-1 ratio (about 1.586).
The landscape library retains the system bars. Its card uses **97.8%**
of the available native stage height and **100.0%** of the maximum physical
card height allowed there, including the intended 8 dp vertical margin. That is
**86.4%** of the complete screen height. The native accessibility snapshot
independently identifies the stage beside its full-height control rail; the verifier recomputes the
maximum fit from those bounds and the captured display density, with a two-pixel tolerance.
The immersive viewer uses **98.0%** of the complete screen height. The capture
flow also rejects overlap of the viewer controls or permanent joystick with the card.

An independent jsQR reader decoded **five whole raw screenshots**, without cropping, resizing or
replacing the rendered code. The three hub/own-card viewer QR screens contain the same 220-byte
Marta Ruiz vCard; the two received-card viewer screens contain the same **193-byte**
Nerea Vidal vCard. Both retain the contact's own colours and nacre STYLE `1:A2gB`.
[qr-decoding.json](qr-decoding.json) records screenshot hashes, payload hashes and QR locations.
Category names, assignments, order, favorites and private notes are absent from the shared payloads.

These checks validate the captured emulator flows. They do not establish physical NFC exchange,
camera scanning/permission or SAF import flows on real devices, physical motion-sensor response,
touch feel, or a complete visual/accessibility audit.

## Earlier evidence

The [materials and QR review](../materials-qr/README.md) belongs to app source `dd9c3a5` and remains
a separate historical result. The [earlier editor review](../review/README.md) belongs to `3d7ce41`.
