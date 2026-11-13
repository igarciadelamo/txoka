# NFC device test plan

Manual validation of the NFC paths on **two physical phones**. The protocol, the responder and the
view models are covered by unit tests, and the debug demo exercises the same codec, but nothing here
can be reproduced on an emulator, so these cases only pass on real hardware.

The hub role also picks the NFC mechanism: **Receive** reads with `enableReaderMode` and does not
emulate, **Send** emulates with foreground dispatch and the preferred HCE service. The reader-mode
callback bypasses NDEF intent dispatch on purpose, because some vendor builds never deliver those
intents to third-party apps.

## Setup

| Item | How it must be |
|---|---|
| Phones | **A** ("sender") and **B** ("receiver"), both with NFC. Record model, Android version and skin. Ideally one OEM skin and one stock build. |
| Build | Same signed APK (or `./gradlew :app:installDebug` while there is no keystore) on both, same `versionCode`. |
| Data | A: full card (name, phone, email, LinkedIn, Instagram, custom colour). B: a profile with name and phone, **empty** received library. |
| Case | No case. Find the antenna area once and keep it as the reference position. |
| Radio | NFC on, airplane mode off. |
| HCE | No payment app owning the tag: system Settings → Apps → Default apps → NFC/payments. |
| Logs | On both phones: `adb logcat -c` then `adb logcat \| grep -iE "nfc\|apdu\|HostApduService\|txoka"`. Snapshot first with `adb shell dumpsys nfc > nfc_before.txt`. |
| Evidence | A case only counts as passed with a log excerpt or screenshot in the results table. |

Bring B against the back of A, on the antenna area, and hold for one to two seconds. Screens stay
on except in the lock tests.

## A. Basic send and receive

**1. Send A→B (happy path)**
- A: role **Send**. B: role **Receive**.
- Tap B against A's back and hold briefly.
- Expected on B: confirmation sheet for A's card, "Shared over NFC" chip, the card in its own
  gradient, **Save contact** (not **Update contact**), **Discard**, and no "more cards waiting"
  line. Saving adds one row to the library with phone, email, LinkedIn and Instagram unchanged.
- No system chooser and no other app opened.

**2. Nothing is stored without a tap**
- Repeat 1 and leave the sheet alone for 30 s. The library does not change until **Save contact**.

**3. Discard**
- Repeat 1 and discard. The sheet closes and the library stays empty.

**4. Same contact twice**
- Repeat 1 once the card is stored. Expected: "You already have …" and **Update contact**. Saving
  refreshes the sender's fields, keeps the local note and bumps the date; no second row appears.

**5. Send back B→A (the whole round trip)**
- After 1, B shows "Hold again to share yours back · N s" counting down from 60, plus a cancel ✕.
- Tap again **inside the window**.
- Expected: **one** direction per tap. Acceptable: A shows the sheet with B's card and B's window
  ends, leaving B on **Receive**. Not acceptable: cards bouncing between the phones, duplicated
  sheets, or B still counting down after having sent.
- Save on A and check both libraries hold the other person.

## B. The return window

**6. Cancel**
- After B receives, tap the ✕. The module goes back to **Receive** and the countdown disappears.

**7. Expiry after 60 s**
- After B receives, note the wall clock and wait 60 s without touching anything. The counter must
  reach 0 and the module return to **Receive** ("Hold near a phone to receive a card").

**8. Countdown runs on the real clock**
- After B receives, press Home, wait ~30 s, come back: the counter shows ~30 s, not 56.
- Variant: Home, wait 70 s, come back: **Receive**.

**9. The return works with the app in the background**
- After B receives, press Home on B and tap A within the 60 s. A must get B's card even though B is
  not on screen, because HCE still answers off-screen.

**10. No offer unless the phone was receiving**
- A on **Send**, tap with B on **Send**. Neither phone should end up offering a return unless it was
  receiving when the card arrived.

## C. Lock screen and the unlock setting

**11. Setting off (default): send while the phone is locked**
- A on the hub in Send, screen **on and locked** (lock screen visible). B on Receive. Tap.
- Record what happens on B. Some OEMs stop card emulation behind the lock screen; a failure here is
  a finding, not necessarily a bug.
- Variant: screen fully off on A. The README currently claims HCE only answers while the screen is
  on, so record whether this matches.

**12. Setting on in A: lock must block the read**
- A: Settings → Sharing → turn on require unlock. The hub must hint "Unlock the phone to share".
- Lock A, keep B on Receive, tap.
- Expected: B shows no card and no error, both apps stay stable, and the retry after unlocking A
  works. The responder answers `6985` (conditions not satisfied) while locked.

**13. Setting on with A unlocked**
- A unlocked (hub open, or in the background via Home), B on Receive, tap: it works.

**14. The switch follows NFC**
- Turn NFC off on A → in Settings the unlock switch is disabled. Turn NFC back on → enabled again
  with its value kept.

## D. NFC off and unavailable

**15. NFC off when the app opens**
- Turn NFC off on B and open Txoka. Hub reads "NFC is off", the Send/Receive selector is hidden and
  **Open NFC settings** opens the system screen. Settings shows the warning card and the unlock
  switch is disabled.

**16. Turning NFC on with the app open**
- Toggle it in the system settings and come back: the module shows the healthy state and the
  selector works, without restarting the app.

**17. Turning NFC off mid-flow**
- B on Receive, turn NFC off, return to the app: the off warning appears. Tap A: nothing, no crash.
  Turn NFC on and retry: it works.

## E. Foreign tags and card size

**18. NDEF tag from another app**
- Using a physical tag or another app, write a `text/plain` record and an arbitrary `text/vcard`
  record, then read them with Txoka in the foreground and in the background.
- Expected: anything that does not decode to a parseable vCard is ignored. No sheet, no crash. Note
  whether the system shows a chooser because another app also handles `text/vcard`.

**19. The largest card the editor allows**
- On A, create a card at the field caps: name 64 characters, phone 32, email 120, LinkedIn 60 and
  Instagram 60, with accents and at least one emoji.
- Expected: A sends without the "too long" warning and B receives every field intact, no truncation
  and no broken characters.
- Note: the "This card is too long to share" warning is **not** reproducible over NFC with two
  phones from the current UI, because the editor caps stop a card from exceeding the 768-byte NDEF
  budget. That path is covered by unit tests and the emulator only; do not claim it as
  hardware-validated.

**20. Phone formatting**
- After saving on B, check the number keeps the received formatting and that Call and Email open
  the right intents.

## F. Queue and duplicates

**21. Repeated taps with the same card**
- B on Receive, tap A five times in a row. Expected: one sheet and no "more cards waiting", because
  an identical card already in the queue is ignored.

**22. Two different senders with the sheet open**
- B receives from A (Ana). Without closing the sheet, edit A's profile to another name and another
  phone and tap again.
- Expected: the sheet still shows the first card and adds "1 more card is waiting". Saving shows
  the second card in the same sheet, which stays open. Discarding the last one closes it.

**23. Acting on a queued card**
- In 22, saving or discarding must remove exactly the card that was on screen and reveal the next
  one.

## G. Background receiver and dead process

**24. Receiving with the app in the background**
- B with Txoka in the background (Home), A on Send, tap.
- Expected: B comes to the front with the confirmation sheet and exactly one card, not a duplicate
  from a double intent delivery. No system chooser.

**25. Receiving while locked**
- B locked, tap. Either the activity asks to unlock and then shows the card, or nothing happens.
  Never a crash and never a card accepted silently.

**26. Sender process gone on A**
- Close Txoka on A from recents. B on Receive, tap. Expected: A still sends, because the process is
  started again to bind the HCE service; check `HostApduService` in logcat.
- Variant: `adb shell am force-stop es.igarciadelamo.txoka`. Not sending until the app is reopened
  again is expected; record the difference.

## H. Role combinations

**27. Both on Receive**
- Tap: nothing should happen, since neither phone publishes a card. No crash. Setting one to Send
  makes it work.

**28. Both on Send**
- Tap: at most one sheet, never a loop of cards. Document what actually happens.
- Restart the app on both: the role must be back to **Send**, because it is per session.

## I. Robustness

**29. Rotation and back with the sheet open**: rotate to landscape (the actions stay pinned and
  visible) and press back (the card is discarded, not saved).

**30. Screen stays awake**: the hub keeps the display on while NFC is enabled (leave it untouched
  for two minutes). Turning NFC off stops it. Informational.

## Vendor findings

Measured on the pair below while running these cases. They are platform behaviour, not app bugs, and
they shape the design:

- **A phone cannot poll and emulate at the same time.** `enableReaderMode` disables card emulation
  for the whole adapter, which is why one tap can only carry one card and the hub needs a role.
  Documented in `NfcAdapter.enableReaderMode`.
- **Two Android phones talk peer to peer by default**, so a tap does not expose the emulated card
  until one side enters reader mode with an Activity in the foreground. Receiving in the background
  is therefore impossible on the receiving phone; sending is not.
- **The vivo (OriginOS) does not deliver `NDEF_DISCOVERED` intents to third party apps** and
  restarts the Activity when one reaches it, tearing the RF session down. That is why the app reads
  with reader mode instead of intent dispatch, and why exchanging automatically in a single contact
  was abandoned.
- **The Samsung ships its own "share your card over NFC" service.** Without an Android Application
  Record it wins the tag or forces an app chooser, so the card never reaches Txoka. The AAR in the
  NDEF message fixes it: the tag now opens Txoka directly, in the foreground and from the
  background.
- **The vivo filters application `Log` output** (only warnings reach logcat), which is why the
  diagnostics of the receiver side are the ones worth collecting.

## Results

Pair under test: **A** vivo V40 SE 5G (V2337), Android 16 / OriginOS 6 · **B** Samsung Galaxy A36
5G (SM-A366B), Android 16 / One UI.

| # | Case | Result | Evidence |
|---|---|---|---|
| 1 | Send A→B | Pass | Sheet with the NFC chip, `Save contact`, no queue line, no chooser; library gains one row with phone, email, LinkedIn and Instagram |
| 2 | Nothing stored without a tap | Pass | 30 s with the sheet open, then discard: library unchanged |
| 3 | Discard | Pass | Sheet closes, library unchanged |
| 4 | Same contact twice | Pass | "You already have …" and `Update contact` |
| 5 | Send back B→A | Pass with precondition | A must be on **Receive**; on the vivo a phone on Send cannot read. A shows the sheet with B's card |
| 6 | Cancel | Pass | ✕ returns the module to Receive and clears the countdown |
| 7 | Expiry after 60 s | Pass | Counter reaches 0 and the module returns to Receive |
| 8 | Countdown on the real clock | Pass | 48 s, 33 s in the background, back at 12 s |
| 9 | Return with the app in the background | Not executed after the AAR fix | Needs a rerun; before the AAR the system chooser stole the tag |
| 10 | No offer unless receiving | Pass | Both on Send: no sheet, no offer |
| 14 | The unlock switch follows NFC | Pass | Disabled with NFC off, enabled with its value kept when it comes back |
| 15 | NFC off when the app opens | Pass | Off warning, role selector hidden, `Open NFC settings` opens the system screen |
| 16 | Turning NFC on with the app open | Pass | Module recovers without restarting |
| 17 | Turning NFC off mid-flow | Pass | Off warning, tap does nothing, no crash, works again once NFC is back |
| 20 | Phone formatting and intents | Pass | Number kept as sent; `Call` opens the dialer, `Email` opens Gmail compose |
| 24 | Receiving in the background | Not executed after the AAR fix | Before it, the Samsung chooser won the tag; after it the app opens on its own |
| 25 | Receiving while locked | Pass | Nothing accepted silently, no crash; the system chooser appeared while locked |
| 26 | Sender process gone | Pass | Removing Txoka from recents on A still sends, the HCE service restarts the process |
| 27 | Both on Receive | Pass | Nothing happens, no crash |
| 28 | Both on Send | Pass | No sheet and no offer on either side |
| 29 | Rotation and back with the sheet open | Pass | Landscape keeps both actions pinned; back discards and the library keeps one row |
| 30 | Screen stays awake | Pass | Still awake and focused 95 s after a 60 s screen timeout |
| 11-13, 18, 19, 21-23 | Not executed | Pending | Lock tests need a manual lock; 18 needs a physical tag; 19 and 22 need editing the profile; 21 is not reachable because receiving switches the phone to Send |

Diagnostics when something fails:

```sh
adb shell dumpsys nfc > nfc_after.txt   # compare AID routing with nfc_before.txt
adb logcat -d > full_logcat.txt
```

## Definition of done

The plan counts as done when the table is filled in on **two different phone pairs** (ideally one
OEM skin and one stock build) and the negative results are documented. Then:

- record the phone models and Android versions next to the result;
- open a separate issue for anything that only fails on one vendor, attaching the logcat, instead of
  changing the protocol blindly.
