# NFC device test plan

Manual validation of the NFC paths on **two physical phones**. The protocol, the responder and the
view models are covered by unit tests, and the debug demo exercises the same codec, but nothing here
can be reproduced on an emulator, so these cases only pass on real hardware.

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

**29. Pulling the phones apart mid-read**: no crash, no half card, and a normal retry works.

**30. Misalignment**: tap offset by one or two centimetres. The expected outcome is simply nothing,
  without visible errors.

**31. Rotation and back with the sheet open**: rotate to landscape (the actions stay pinned and
  visible) and press back (the card is discarded, not saved).

**32. Screen stays awake**: the hub keeps the display on while NFC is enabled (leave it untouched
  for two minutes). Turning NFC off stops it. Informational.

## Results

| # | Case | A model / Android | B model / Android | Result | Evidence |
|---|---|---|---|---|---|
| 1 | Send A→B | | | | |
| 5 | Send back B→A | | | | |
| 6-9 | Return window, background | | | | |
| 11-13 | Lock screen, require unlock | | | | |
| 18 | Foreign tag | | | | |
| 21-22 | Queue | | | | |
| 24-26 | Background, dead process | | | | |
| 27-28 | Role combinations | | | | |

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
