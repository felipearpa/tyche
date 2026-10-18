# Verification

## Android — task 3.1 (2026-10-18)

Setup: emulator-5554 (AVD Pixel_10_Pro_XL, API 37, en-US, light mode, software rendering), Fortuna 2.0.0 prodDebug installed with `./gradlew :app:installProdDebug` at 16:07 over the existing install, still signed in as felipearpa against prod. Pool "QA gamblers" (`01M55B9QZDA7725BDEB0PWA1EP`), 49 members at the start. Failures were staged with airplane mode (`cmd connectivity airplane-mode enable`), rotation with `settings put system user_rotation` (auto-rotate off during the walk), and the transient removing state was caught with `screenrecord` frames. Requests were checked in logcat (`Ktor Client` request lines only). Screenshots are in `verification/`.

Gamblers removed from QA gamblers: btester13, btester14, btester15 (49 → 46 members). btester16 and btester17 were used only for failures and are still members.

### Build checks

- `./gradlew :app:testProdDebugUnitTest` (from `Android/`): BUILD SUCCESSFUL, 60 tests, 0 failures.
- `./gradlew :pool:testDebugUnitTest` (from `Android/`): BUILD SUCCESSFUL, up to date with the group 1–2 run, 40 tests, 0 failures.

### Scenarios

| Scenario | Result | Screenshots |
|---|---|---|
| Swipe past the threshold: "Remove" background, dialog "Remove @btester13 from this pool?" opens | Pass | `01-gamblers-opened.png`, `02-swipe-past-threshold.png`, `03-confirm-dialog.png` |
| Cancel leaves the row unchanged, count 49 | Pass | `04-after-cancel.png` |
| Confirm (btester13): row leaves, count 49 → 48 | Pass. The removing frame was not captured for this row: the screenshot after the tap showed the dialog still fading out. | `05-removed.png` |
| Confirm (btester14): row dims with the ball spinner in place of the avatar, then animates out, count 48 → 47 | Pass (screen-recording frames). The count dropped while the dimmed row started its exit, and the row then left without flicker. | `06-removing.png`, `07-removing-exit.png`, `08-removed-count-47.png` |
| Removed gamblers stay gone after paging and pull-to-refresh | Pass. Scrolled to the last member (tester58) and back, and pulled to refresh online: btester13–15 never reappeared and the server count read 46. The members endpoint returns every member in one response, so there is no second page to load. | `12-scrolled-away.png`, `15-after-refresh-online.png` |
| Two failures in airplane mode (btester16, btester17): each row shows "Couldn't remove" with Retry and ✕, count unchanged at 46, no banner | Pass, staged twice | `11-two-failures-offline.png` |
| Retry re-enters removing without a dialog | Partly observed. Each Retry tap on btester16 sent a DELETE for that gambler at once (logcat), and no dialog appeared (UI dump). Offline, the request failed in 0.1–0.5 s and the row showed "Couldn't remove" again. The dimmed removing state was not captured. | `16-failures-after-scroll-back.png` (state before and after a Retry) |
| ✕ returns the row to idle | Pass. btester17 shows its email again, count 46, and no DELETE was logged. | `19-dismissed-btester17.png` |
| Swipe on a failed row opens the dialog; Cancel keeps the failure | Pass (btester16) | `20-swipe-failed-row-dialog.png` |
| Pull-to-refresh returns a failed row to idle | Pass. Online pull-to-refresh (GET pool and members, both 200) showed btester16 with its email again. An earlier pull-to-refresh while offline showed the load error, and after Retry online both failed rows were idle. | `21-refresh-releases-failed-row.png`, `13-offline-refresh-error.png`, `14-reloaded-failures-released.png` |
| Failed rows' state survives scroll-away | Pass. With both rows failed, the list was scrolled about ten rows past them and back: both still showed "Couldn't remove" with Retry and ✕. | `16-failures-after-scroll-back.png` |
| Failed rows' state survives rotation | Pass. Both failures stayed in landscape and after rotating back. | `17-failures-after-rotation.png`, `18-failures-after-rotating-back.png` |
| In-flight removal survives a rotation | Partly observed. btester15 was confirmed and rotation was requested at once. The row was dimmed and removing just before the rotation took effect. The rotation applied about 1.5 s later, when the request had already finished: the recreated screen showed btester15 gone and the count 46, with no second request. | `09-removing-before-rotation.png`, `10-after-rotation-removed.png` |
| In-flight removal survives scroll-away | Not observed. The scroll swipes sent right after confirming btester14 did not move the list until the request had finished. | none |

### Not verified

- A removal visibly in flight across scroll-away and a rotation: online removals finish in about 2 s, and the emulator's console `network delay` and `network speed` settings did not slow the app's traffic (a refresh still finished in under 4 s). Offline removals fail in 0.1–0.5 s, and the image is not rooted, so traffic could not be stalled with iptables. The holders' survival was shown with failed rows (scroll-away and rotation) and with an outcome observed after a rotation.
- The dimmed removing state after Retry: offline retries fail too quickly to capture, and a capture online would have needed a fourth removal. The DELETE being sent without a dialog was observed in logcat.
- Error haptic: the emulator has no haptic feedback.

### Observations outside this change

- The emulator was heavily loaded during the walk (load average up to 30): `uiautomator dump` sometimes lagged behind the screen, and `screencap` hung during rotations (one capture came back black and was retaken).
- After an offline pull-to-refresh and Retry online, the heading had no count line until the next pull-to-refresh. This matches the earlier walk's note that the count is re-requested only on pull-to-refresh.

## Android — task 3.1 follow-up (2026-10-18)

Setup: same emulator, build, account, and pool as above, 16:38–17:00. The user approved one more successful removal, of btester16 only. btester16's failure was staged in airplane mode. After airplane mode was turned off, the emulator's DNS stopped resolving the API host: each DELETE stayed in flight for 6–19 s and then failed with `UnknownHostException` (logcat), although the host machine resolved the name. That stall gave an in-flight window that the earlier pass could not stage, so the Retry, rotation, and scroll-away checks below ran during real in-flight requests. Request timings are from logcat `Ktor Client` request lines. Screenshots are in `verification/`.

No gambler was removed in this follow-up. btester16 is still a member and its row was left in "Couldn't remove", which is in-memory state only. The pool still has 46 members.

### Scenarios

| Scenario | Result | Screenshots |
|---|---|---|
| Retry re-enters removing without a dialog | Pass. Retry on the failed btester16 row sent a DELETE (logcat) with no dialog. The row dimmed with the ball spinner in place of the avatar while the request was pending. The first capture, about 1 s after the tap, showed the row's email with the avatar not yet dimmed. This is most likely the first frame of the animated change, as in the earlier walk's two-removals capture. | `22-followup-btester16-failed.png`, `25-followup-retry-first-frame.png`, `26-followup-retry-removing-after-rotation.png`, `29-followup-retry-removing-after-scroll-back.png` |
| In-flight removal survives a rotation | Pass. DELETE in flight from 16:53:38.5 to 16:53:44.5. Rotation was requested right after the Retry tap. The recreated landscape screen at about 16:53:43 showed btester16 dimmed with the spinner and the count at 46. When the request failed, the same row showed "Couldn't remove" with Retry and ✕. A first attempt at 16:40 had its DELETE in flight for 19 s across the rotation, and its failure also landed on the recreated landscape row; its recording kept only the frame before the tap. | `23-followup-before-retry-and-rotation.png`, `24-followup-inflight-retry-outcome-after-rotation.png`, `26-followup-retry-removing-after-rotation.png`, `27-followup-retry-outcome-after-rotation.png` |
| In-flight removal survives scroll-away | Pass (screen-recording frames). DELETE in flight from 16:57:03.7 to 16:57:16.7. Right after the Retry tap, the list was scrolled until btester22 was the top row, with btester16 several rows above the viewport. When the list was scrolled back, btester16 was still dimmed with the spinner. When the request failed, the row showed "Couldn't remove" with Retry and ✕. | `28-followup-retry-row-scrolled-away.png`, `29-followup-retry-removing-after-scroll-back.png`, `30-followup-retry-outcome-after-scroll-back.png` |

### Not verified

- A successful removal after Retry (removing → exit animation → count 46 → 45): the emulator's DNS did not recover after the airplane-mode cycles. Turning Wi-Fi off and on did not fix it either. btester16 was therefore not removed. A successful removal after confirming was observed in the first pass (btester14, `06`–`08`). The earlier record notes that restarting the emulator restores its DNS.
- Error haptic: the emulator has no haptic feedback.

### Observations outside this change

- On this emulator, DNS for the API host stopped resolving after airplane mode was turned off, while the host machine still resolved the name. Requests then waited 6–19 s before failing. This is an emulator network problem, not app behavior.
- `screenrecord` stops producing frames once the display rotates, and `screencap` can hang during a rotation. The rotation evidence here therefore comes from separate captures taken before and after the rotation settled.

## Android — task 3.1 follow-up, Samsung SM-G955F (2026-10-18)

Setup: Samsung SM-G955F (`ce0417143a6ab8130c`, Android 9, es-CO). This worktree's prodDebug (Fortuna 2.0.0, built 16:06) was installed with `adb install -r` over the existing app at 17:06, without uninstalling or clearing data. The app opened signed in as felipearpa against prod. Jugadores in "QA gamblers" showed "46 jugadores en esta polla", with btester16 first.

Result: blocked before the success-after-Retry check could run. The first swipe on btester16 in airplane mode opened "¿Eliminar a @btester16 de esta polla?". The confirming tap missed: `android screen resolve` returned coordinates outside the dialog, so the dialog closed and no DELETE was sent (logcat). The phone then timed out its screen and went to its secure lock screen (`deviceLocked=1`), and it cannot be unlocked from a tool. Airplane mode was turned off again. No gambler was removed: btester16 is still a member and the pool still has 46 members.

### Not verified

- A successful removal after Retry (removing → exit animation → count 46 → 45): blocked by the phone's lock screen. It needs the phone unlocked and kept awake for the walk.

### Rerun

At 17:10 the phone was unlocked and Fortuna was on Jugadores ("46 jugadores en esta polla"). The phone still had `stay_on_while_plugged_in` set to 0 and a 30 s screen timeout while USB-powered, so "Permanecer activo" was not in effect.

- Airplane mode on, then a swipe on btester16 opened "¿Eliminar a @btester16 de esta polla?". The UI dump placed "Eliminar" at [709,1226][865,1269]. By the time the tap reached the phone at 17:10:47, the dialog's window had already been removed (17:10:43, SurfaceFlinger) as the screen timed out. The tap went to the activity window, no DELETE was sent (logcat), and the row stayed idle.
- On a second swipe, the dialog never showed in UI dumps, and the phone was then dozing on its secure lock screen (`deviceLocked=1`). The walk stopped there and airplane mode was turned off again.

No gambler was removed: btester16 is still a member and the pool still has 46 members. No screenshots were kept from this rerun.

#### Not verified

- A successful removal after Retry (removing → exit animation → count 46 → 45): blocked again by the phone's 30 s screen timeout and secure lock. It needs the screen kept on during the walk, for example Developer options "Stay awake" (`stay_on_while_plugged_in`) while on USB, or a longer screen timeout.

### Rerun 2

Setup: 17:13–17:16, `stay_on_while_plugged_in` = 7 and `mStayOn=true` (checked before starting), USB connected, unlocked throughout. Same prodDebug build as above, still signed in as felipearpa. "QA gamblers" → Jugadores showed "46 jugadores en esta polla". Taps used bounds from a fresh `uiautomator dump` taken right before each tap. The Retry sequence was recorded with `screenrecord` on the phone, and the frames below come from that recording.

Gambler removed: btester16 (46 → 45). No other member was touched.

| Scenario | Result | Screenshots |
|---|---|---|
| Offline confirm fails: dialog "¿Eliminar a @btester16 de esta polla?" ("Eliminar" at [709,1226][865,1269]), then the row shows "No se pudo eliminar" with "Reintentar" and ✕, count 46 | Pass. DELETE logged and failed offline at 17:14:57. | `31-samsung-confirm-dialog.png`, `32-samsung-btester16-failed-offline.png` |
| Back online: connectivity confirmed without refreshing the list (a refresh would clear the failed row) | Pass. The phone resolved and pinged the API host. | `33-samsung-before-retry-online.png` |
| Retry enters removing without a dialog | Pass. Tapping "Reintentar" sent a DELETE at 17:15:34.8 and no dialog appeared. In the first frame after the tap, the failure line had already been replaced by the email, with the avatar not yet dimmed. Over the next ~150 ms the row dimmed and the ball spinner replaced the avatar. This confirms that the emulator's "email with undimmed avatar" capture was the start of the animated change. | `34-samsung-retry-first-frame.png`, `35-samsung-retry-removing.png` |
| Removal succeeds: the row animates out and the count drops by one | Pass. Response 204 at 17:15:36.8. The count line changed to "45 jugadores en esta polla" while the dimmed row was still shown. The row then shrank and faded out over about 100 ms, and btester17 moved up. | `36-samsung-count-45-exit-starting.png`, `37-samsung-row-animating-out.png`, `38-samsung-removed-count-45.png` |

#### Not verified

- Error haptic on the offline failure: haptics cannot be observed through adb.
