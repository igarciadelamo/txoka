# Play Store listing

Copy-paste source for Play Console. Play limits are in the third column; the character counts
below were measured, so keep them in sync when editing.

This folder is the store kit: the listing copy and every upload asset. Nothing here is published
by GitHub Pages, which serves `docs/` only.

## Store listing

| Field | Language | Limit | Value |
| --- | --- | --- | --- |
| App name | all | 30 | `Txoka` |
| Short description | en-GB | 80 | `Share your card phone to phone. No account, no tracking, no internet.` |
| Short description | es-ES | 80 | `Comparte tu tarjeta de móvil a móvil. Sin cuenta ni internet.` |
| Full description | en-GB | 4000 | below |
| Full description | es-ES | 4000 | below |
| Release notes 1.0 | en-GB | 500 | `First release. Create your card once and share it by holding two phones together. No account, no tracking, no internet permission.` |
| Release notes 1.0 | es-ES | 500 | `Primera versión. Crea tu tarjeta una vez y compártela acercando dos móviles. Sin cuenta, sin rastreo y sin permiso de internet.` |

Category: **Tools**. Tags: `Tools`, `Productivity`.
Contact email: `isidro.garcia@gmail.com`. Website: `https://github.com/igarciadelamo/txoka`.
Privacy policy: `https://igarciadelamo.github.io/txoka/privacy/`.

### Full description (en-GB)

```
Your contact card, phone to phone.

Create your card once (name, phone, email, LinkedIn, Instagram and a colour you pick) and share it by holding two phones together. The other person sees the card and decides whether to keep it. To send theirs back, they set Txoka to Receive and you touch the phones again.

WHAT MAKES IT DIFFERENT

- No account and no sign-up.
- No ads, no analytics, no tracking and no crash reporting.
- No internet permission at all. The app cannot send anything anywhere, and the card travels phone to phone over NFC.
- Everything stays on your phone: your card, your sharing settings and the cards you receive.

YOUR CARDS

- Choose one of seven colour presets or pick any colour on a custom wheel; the text always keeps a readable contrast.
- Received cards live in a local library with their date and a note you can add, and can be added to your system contacts, called or emailed in one tap.
- Cards you already have are recognised and updated instead of duplicated.

WHAT YOU NEED

Two Android phones with NFC to exchange a card; with one phone you can still create your card and keep the ones you receive. Sharing is one to one and nothing is uploaded: there is no server behind this app.

Txoka is free software, open source under GPL-3.0: https://github.com/igarciadelamo/txoka
```

### Full description (es-ES)

```
Tu tarjeta de visita, de móvil a móvil.

Crea tu tarjeta una vez (nombre, teléfono, email, LinkedIn, Instagram y el color que elijas) y compártela acercando dos móviles. La otra persona ve la tarjeta y decide si la guarda. Para devolverte la suya, pone Txoka en Recibir y volvéis a acercar los móviles.

QUÉ LA HACE DISTINTA

- Sin cuenta y sin registro.
- Sin anuncios, sin estadísticas, sin rastreo y sin informes de fallos.
- Sin permiso de internet: la app no puede enviar nada a ningún sitio y la tarjeta viaja de móvil a móvil por NFC.
- Todo se queda en tu teléfono: tu tarjeta, tus preferencias y las tarjetas que recibes.

TUS TARJETAS

- Elige uno de los siete colores predefinidos o cualquiera en una rueda de color; el texto siempre mantiene un contraste legible.
- Las tarjetas que recibes se guardan en tu biblioteca con su fecha y una nota que puedes añadir, y puedes pasarlas a los contactos del teléfono, llamar o escribir un correo en un toque.
- Las tarjetas que ya tienes se reconocen y se actualizan en lugar de duplicarse.

QUÉ NECESITAS

Dos móviles Android con NFC para intercambiar una tarjeta; con uno solo puedes crear la tuya y guardar las que recibas. Compartir es de uno a uno y no se sube nada: detrás de esta app no hay ningún servidor.

Txoka es software libre, con licencia GPL-3.0: https://github.com/igarciadelamo/txoka
```

## App access (testing instructions for the reviewer)

```
All features are available without an account or special access.

Testing NFC needs two Android phones with NFC, both with Txoka installed and both screens on, one
in the Send role and the other in Receive. Sharing works phone to phone and there is no server, so a
single device can only exercise the card editor, the settings and the empty received library.

The app is fully usable without NFC: the hub explains that sharing needs the hardware and the card
can still be created, edited and deleted. Debug builds have two demo rows under Settings for the
receive flow; release builds do not include them.
```

## Data safety

| Question | Answer |
| --- | --- |
| Does the app collect or share any of the required user data types? | **No** |
| Is any data encrypted in transit? | Not applicable (nothing leaves the device) |
| Do you provide a way for users to request data deletion? | Not applicable (no accounts, no server) |

The card, the preferences and the received library stay in the app's local storage. The optional
Android system backup may copy them to the user's own Google account; that copy is made and
controlled by Android, not by Txoka, and it is disclosed in the privacy policy. There is no
`INTERNET` permission and no third-party SDK that collects data.

## Content rating

Category: **Other app types** (not a game, not social or communication). No violence, sexuality,
language, drugs, gambling, location sharing or public user content; no user-to-user communication
channels, so no blocking, reporting or chat moderation. Expected outcome: rated for everyone
(PEGI 3 / ESRB Everyone, USK 0). Target audience: **18+ only**, which keeps the app out of the
Families policies.

## Other app content declarations

| Section | Answer |
| --- | --- |
| Ads | No ads |
| News app | No |
| Government app | No |
| Health features | No |
| Financial features | No |
| Advertising ID | Not used |
| Sensitive permissions | None: `NFC` requires no declaration |
| App category, contact details | As above |

## Graphics

| Asset | Spec | Status |
| --- | --- | --- |
| App icon | 512×512 PNG 32-bit, no alpha | `txoka_icon_app-playstore.png` (alpha channel removed) |
| Feature graphic | 1024×500 PNG/JPEG, no alpha | `txoka_feature_graphic.jpg` (1024×500, JPEG) |
| Phone screenshots (English) | 2-8, 16:9 or 9:16, 1080-3840 px | `screenshots/en/`: hub, share back, incoming queue, received list, editor, about (light, 1080×1920 JPEG) |
| Phone screenshots (Spanish) | same | `screenshots/es/`: the same six in light mode |
| Phone screenshots (Spanish, dark) | same | `screenshots/es-dark/`: the same six in dark mode |

The captures use a fictional sample card (`Lucía Ferrer`, `example.com`) and the debug demo senders.
The emulator has no NFC, so they were taken with a temporary local patch that reports the adapter as
enabled and seeds the sample card; neither is committed. The device language came from
`cmd locale set-app-localeconfig <pkg> --locales es` plus `set-app-locales`, and the 9:16 frame from
`wm size 1080x1920`.

## Upload

```sh
./gradlew :app:bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab` to Internal testing first, choose the
default "Use Google-generated key" for Play App Signing, then promote the same build.
