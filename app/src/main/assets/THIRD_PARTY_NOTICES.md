# Third-party notices

Txoka is licensed under GPL-3.0-only. See `LICENSE` for the full text.

The app links or bundles the following third-party components:

| Component | Licence |
| --- | --- |
| AndroidX: Activity, Compose, Lifecycle, Navigation, Room, SQLite, DataStore | Apache-2.0 |
| Kotlin, kotlinx.coroutines, kotlinx.serialization | Apache-2.0 |
| Okio | Apache-2.0 |
| Dagger Hilt, Jakarta Dependency Injection, javax.inject | Apache-2.0 |
| libphonenumber | Apache-2.0 |
| Manrope font | SIL Open Font License 1.1 |

Full licence texts:

- Apache-2.0: `licenses/Apache-2.0.txt`
- Manrope OFL 1.1: `licenses/Manrope-OFL.txt`

The canonical copies live at the repository root. The same files are bundled under
`app/src/main/assets/` so the notices travel inside the APK, and the Manrope OFL text is also kept
in `app/src/main/res/raw/manrope_ofl.txt` so it stays attached to the font resource.
