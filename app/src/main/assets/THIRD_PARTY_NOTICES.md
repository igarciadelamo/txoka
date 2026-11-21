# Third-party notices

Txoka is licensed under GPL-3.0-only. See `LICENSE` for the full text.

The app links or bundles the following third-party components:

| Component | Licence |
| --- | --- |
| AndroidX: Activity, CameraX, Compose, Lifecycle, Navigation, Room, SQLite, DataStore | Apache-2.0 |
| Kotlin, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 |
| Okio | Apache-2.0 |
| Dagger Hilt, Jakarta Dependency Injection, javax.inject | Apache-2.0 |
| libphonenumber | Apache-2.0 |
| ZXing Core | Apache-2.0 |
| Manrope font | SIL Open Font License 1.1 |

Full licence texts:

- Apache-2.0: `licenses/Apache-2.0.txt`
- Manrope OFL 1.1: `licenses/Manrope-OFL.txt`

The canonical copies live at the repository root. The same files are bundled under
`app/src/main/assets/` so the notices travel inside the APK, and the Manrope OFL text is also kept
in `app/src/main/res/raw/manrope_ofl.txt` so it stays attached to the font resource.

## Card artwork

The APK also bundles 23 self-contained SVG files under `assets/card-artwork/`:

- Three original Txoka nature compositions (fish, birds and reptiles), generated for the project
  and faithfully vectorized from the approved compositions. These are not reproductions of
  M. C. Escher's works. Generation and tracing provenance is recorded in
  `card-artwork/original-sources.json`; generated artwork is not a claim of exclusive copyright.
- Twenty geometric two-ink SVGs supplied by the contributor in
  `20_mosaicos_geometricos_bicolor_svg.zip`. Their geometry and source bytes are preserved.
  The archive's README states no separate licence. The contribution is included at the
  contributor's request; that provenance is not independent verification of rights or a new
  licence grant. See `card-artwork/geometric-sources.json` and `card-artwork/README.md`.

The SVGs require no network requests or external resources. Retired unlicensed Escher reproductions
are not bundled. The Potrace tool used during preparation is not a runtime dependency.
