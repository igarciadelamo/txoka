# Bundled card artwork

All 23 SVG files are bundled locally and contain only vector geometry. No artwork is downloaded at
runtime. Their exact SHA-256 digests and stable NFC selectors are recorded in the adjacent source
manifests and checked by `CardArtworkCatalogTest`.

- `geometric/`: the twenty original, unmodified SVG files supplied by the user in
  `20_mosaicos_geometricos_bicolor_svg.zip`. The supplied README describes their two colours and
  periodic SVG patterns but does not state a separate licence. See `geometric-sources.json` for
  the recorded provenance and rights note.
- `original/`: faithful vector traces of the three previously approved original Txoka compositions
  (fish, birds and reptiles). Their composition and internal detail are preserved. They do not claim
  to be mathematical tessellations or reproductions of Escher's drawings. See
  `original-sources.json` for the recorded source and generation history.

The renderer interprets the two source inks as antialiased coverage masks. Both displayed colours
come from the selected card palette; the same geometry can also receive the card's gradient or foil
material. Source SVG colours are not shown as fixed black-and-white thumbnails.

No unlicensed Escher reproductions, external SVG resources, embedded raster images, scripts or fonts
are included. Only a stable one-byte artwork selector travels over NFC; the SVGs and masks remain
on the device.
