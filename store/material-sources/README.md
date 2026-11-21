# Material sources

These material sources were generated for Txoka on 20 November 2026 with OpenAI ImageGen.
The three colour sources are direct edits of the approved design reference, with typography
and UI removed. Glass leading was reduced to a hairline at the user's request. The two neutral
relief sources supply additional local surface normals. They do not reproduce third-party
artwork. All source PNG files are preserved unchanged here and are not packaged in the app.

`provenance.json` records the source and runtime SHA-256 digests. The two `*-relief.u8` files in
`app/src/main/assets/card-materials/` are 256 × 256 unsigned eight-bit luminance samples,
65,536 bytes each, without a header. Each sample is the rounded box average of the
corresponding source rectangle, using Rec.709 RGB luminance weights
`0.2126 R + 0.7152 G + 0.0722 B`. The web demo uses identical scalar data.

The three `*-color.rgb` files retain the approved texture and highlights at 480 × 303 pixels,
436,320 bytes each, with interleaved unsigned eight-bit sRGB channels and no header. Each
channel is box-averaged independently from its source image and rounded to the nearest
integer. Both renderers consume identical RGB data. The reference palette at neutral pose
reproduces these canonical pixels; other palettes recolour the existing surface.

The renderer computes palette transfer and two-axis light changes on the device. The artwork
stays attached to the card, while glass, nacre and directional brushed-metal reflections respond
to tilt. No map, normal, image, light position or sensor reading travels through NFC or QR:
the existing STYLE background identifiers select the local material.

`glass-cells.u8` is a 480 × 303 unsigned label atlas, with every pixel assigned to one of
the 24 visible source panes (IDs 0–23), including partial panes at the crop edges. Existing
source seam junctions guide local shortest paths along the dark hairline joints; this only
records optical identity. It does not redraw geometry or alter the RGB artwork. The immutable
label map gives each source pane its own fixed normal orientation for two-axis relighting.
