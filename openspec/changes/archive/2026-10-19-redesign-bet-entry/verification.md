# Verification — redesign-bet-entry

Record of the task 6.1 integration check of the `bet-entry` scenarios (and the restated `android-edge-to-edge` and `ios-liquid-glass` scenarios) on iOS and Android, in English and Spanish. Captures live in `verification/`; files named `g6-*` were taken on 2026-10-19, the others in earlier groups.

Result values: **observed** (seen in the running app during this check, or in an earlier capture where noted), **covered by test** (not staged by hand; the named automated test exercises it), **not verified** (with the reason).

## Earlier groups (coordinator record)

- Felipe accepted these as unverified:
  - iOS airplane-mode Retry on a simulator (the sim shares the Mac's network)
  - 3.2: typing on an empty card
  - 3.3: away `+`, and the failure banner/Retry on a device
  - 3.4: the No bet count dropping as a bet saves (observed later), VoiceOver "at least N", the day header exactly at the real page-1/page-2 boundary, and the empty-message timing on real pages
  - 3.5/3.6: VoiceOver/Accessibility Inspector, Reduce Motion, and the neighbour slide mid-list
  - iOS 16–17 keep the end-of-list jump; the iOS 18+ bottom-anchor fix was accepted
- Observed on device:
  - the forbidden save on match B (2026-10-18 19:45Z): the boxes animated back to 1–0, then Locked (ios-06, ios-07)
  - "Locks in N min" counting down (ios-01)
  - real server paging in "QA paging", 3,000 matches over 3 pages (ios-30-*)
  - the "+" counts and the empty message waiting for the last page (ios-25..28)
  - the iOS bugs found and fixed: a save overwritten by a later page, and a 13 s freeze with hidden rows
- Android group 5: all 65 `:bet` connected tests pass. `PoolHomePendingBetKeyboardTest` passed 4/4 once, then flaked on the overloaded emulator. 5.2/5.5/5.6 are still pending Felipe's acceptance (keyboard test stability, TalkBack actually speaking, Accessibility Scanner, animation smoothness, `animateContentSize()` left off the card).

## Automated checks (2026-10-19)

| Suite | Command | Result |
|---|---|---|
| iOS `BetTests` + `UITests` (iOS 18.1 sim `B5FD97E9…`) | `xcodebuild test -workspace iOS/Tyche.xcworkspace -scheme Tyche -destination 'id=B5FD97E9-BEC1-4F7A-8503-06A6A3EB4E96' -parallel-testing-enabled NO -only-testing:BetTests -only-testing:UITests` | 290 tests: 289 passed, 1 failed. `BetTests` 171/171. `UITests` 118/119: `TrendColorTests.testTrendTokensMeetNormalTextContrastOnEverySurfaceInBothAppearances` (6 dark-mode contrast assertions, e.g. positive on canvas 3.18:1 < 4.5:1) |
| `TrendColorTests` on HEAD `9e2957b1` (temporary worktree, `UI` scheme, same sim) | `xcodebuild test -scheme UI -only-testing:UITests/TrendColorTests` | Fails the same way on HEAD (same 6 assertions, same ratios): pre-existing, not caused by this change |
| Android `:bet:testDebugUnitTest` | `./gradlew :bet:testDebugUnitTest :app:testProdDebugUnitTest --continue` | 133/133 passed |
| Android `:app:testProdDebugUnitTest` | (same run) | 60/60 passed |
| `PoolHomePendingBetKeyboardTest` (Pixel_10_Pro_XL `emulator-5554`) | `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedProdDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.PoolHomePendingBetKeyboardTest` | 4/4 passed on the first attempt (2m 45s, with the keyboard fix). The run uninstalled Fortuna, which wiped the emulator's sign-in; the Prod debug build was reinstalled afterwards |

## Fix made during this check

- **Android, `android-edge-to-edge` "Editing a score near the list end" failed by hand.** Focusing a score box on the last two cards of QA solo left the card's lower half (and on the last card the focused box itself) behind the keyboard for more than 10 s, twice out of two (`g6-android-09`, `g6-android-12`). `KeepVisibleAboveKeyboard` asked for `bringIntoView()` in the frame that reported the finished keyboard inset, but the list is sized by `fitInside(WindowInsetsRulers.Ime)` during layout, so that request could arrive before the viewport had shrunk and then never repeat. The fix waits one frame (`withFrameNanos {}`) before `bringIntoView()` (`Android/bet/src/main/java/com/felipearpa/tyche/bet/pending/KeepVisibleAboveKeyboard.kt`). After reinstalling, the whole card came above the keyboard on both cards and stayed visible when focus moved to the away box (`g6-android-13`, `-14`, `-15`).

## iOS — task 6.1 (2026-10-19)

Device: iPhone 18 Pro Max simulator (iOS 27, `C0B22275…`, signed in, light appearance), `Tyche` scheme Debug build of the working tree against production. Spanish: relaunched with `-AppleLanguages (es) -AppleLocale es_CO`, then relaunched in English. Pools: QA solo (7 matches), QA paging (3,000 open matches, 3 pages).

| Scenario | Locale | Result | Evidence |
|---|---|---|---|
| Opening the Bets tab | en, es | observed: cards under date headers in kickoff order, flags, names, time, pill, two boxes | g6-ios-01, g6-ios-02, g6-ios-26, g6-ios-27 |
| Large text | en | observed at AX5 after a fresh launch: names stack and wrap above the scores, filter row scrolls sideways, steppers reachable. Finding: changing the text size while the tab was open left the Saved pill clipped ("ved") until relaunch | g6-ios-24, g6-ios-25 (clipped: g6-ios-23) |
| Matches today and later this week | en, es | observed "Yesterday", "Wednesday", "Friday" / "Ayer", "Miércoles", "Viernes" and short dates ("Thu, 15 Oct" / "Jue, 15 de oct."); no match today or tomorrow in the data, so "Today"/"Tomorrow" from earlier ios-08/ios-17 and `PendingBetDayLabelTests` | g6-ios-02, g6-ios-27, ios-08, ios-17 |
| A match in another year | en, es | observed "Mon, 4 Jan 2027" / "Lun, 4 ene. 2027" | g6-ios-11, g6-ios-31 |
| Midnight passes | en | not verified: the simulator clock cannot be moved past midnight during a session; labels from an injected now are covered by `PendingBetDayLabelTests`, the live flip is not | — |
| Typing a prediction | en | observed: typed 2 → focus moved to away and Incomplete; typed 1 → saved 2–1 and the card left No bet | g6-ios-13, g6-ios-14 |
| Adjusting with steppers | en | observed: 2–1 → `+` away → spinner → Saved 2–2; `−` on a 0 stays 0 with no spinner | g6-ios-03, g6-ios-04, g6-ios-05 |
| First tap on an empty card | en | observed: `+` away → 0–1, saved | g6-ios-15, g6-ios-18 (in Placed) |
| Quick pick | en | observed: 1–1 chip → both boxes 1, Saved, chips gone | g6-ios-16 |
| Rapid stepper taps | en | observed: three `+` home taps → one spinner, Saved 3–3; the single submission is covered by `threeStepperTapsWithin300MillisecondsMakeOneSubmission` (tool taps are slower than 0.5 s) | g6-ios-06 |
| Edit while saving | en | covered by test `anEditDuringTheFlightResavesTheNewestScoresOnce`; by hand only the end state (Saved 2–3) was seen, the second tap's timing relative to the flight could not be controlled | g6-ios-21, g6-ios-22 |
| One side typed | en, es | observed: amber dashed box, Incomplete/Incompleta pill, "Add Austria's score to save" / "Agrega los goles de Alemania para guardar", no spinner | g6-ios-17, g6-ios-28 |
| Completing the draft later | en | observed: Incomplete 2–? kept while Placed loaded every page (counts became exact) and back; `+` away → 2–1 submitted and left No bet | g6-ios-19, g6-ios-20 |
| Clearing a saved prediction | en | observed: both boxes cleared → still Saved; after the keyboard closed the boxes showed 2–2 again. Finding: with only one box cleared, the saved card shows the hint and the quick-pick chips | g6-ios-09, g6-ios-10 (one side: g6-ios-08) |
| Network failure | en | not verified on the simulator (shares the Mac's network; accepted earlier); covered by `aFailedSaveKeepsTheDraftAndReportsTheFailure`, `retryResubmitsTheSameScoresAtOnce` | — |
| Forbidden save | en | not staged (a real lock passing needs new matches); observed earlier on match B, 2026-10-18 19:45Z; tests `aForbiddenSaveLocksTheRowWithTheLastSavedPrediction`, `aForbiddenFirstSaveLocksTheRowWithEmptyScores` | ios-06, ios-07 |
| Locked card | en, es | observed: Locked/Cerrada pill, muted 1–0, no controls; boxes taking no input covered by `aLockedMatchIsLockedAndIgnoresEdits` | g6-ios-02, g6-ios-27 |
| Locked card without a prediction | en, es | observed: muted dashes, no `?` boxes | g6-ios-01, g6-ios-26 |
| Lock approaching | en | not staged today (no open match within an hour of its lock); observed earlier; tests `theLockNoteCountsWholeMinutesDuringTheLastHourBeforeTheLock`, `theLockNoteUpdatesAtEachWholeMinuteBeforeTheLockAndAtTheLock` | ios-01 |
| Jumping to missing bets | en, es | observed on QA paging: No bet shows only `?` cards; a save drops the card and No bet 1,117+ → 1,116+ → 1,115+ | g6-ios-12, g6-ios-14, g6-ios-32 |
| Filter with nothing left | es | observed: "Por ahora no te queda nada por apostar." on QA solo | g6-ios-29 |
| Missing bets only on a later page | en | not restaged; observed earlier; test `noBetWaitsForALaterPageThatHoldsAnUnbetMatch` | ios-27-* |
| Counts while more pages remain | en, es | observed "1.123+ / 1.117+ / 6+" and plain "3000 / 2991 / 9" after the last page. Finding: the exact count drops the grouping separator ("3000") that the partial one uses ("1.123+") | g6-ios-11, g6-ios-18, g6-ios-31 |
| Placed filter with nothing placed | en | not restaged (every pool now holds a bet; staging needs deleting bets); observed earlier on real pages | ios-30-real-server-pages-3 |
| Pill becomes spinner and back | en | observed the end states (spinner, Saved) with nothing moving; frame-level crossfade earlier; test `theStatusSlotKeepsItsHeightBetweenSavingAndSaved` | g6-ios-03, g6-ios-04, ios-11, ios-pill-save.gif |
| Chips leave after the first save | en | observed the chip row collapsing with an animated height. Note: it collapses as soon as both scores are set, before the save returns | g6-ios-15, ios-28 |
| Save rejected because the match locked | en | not staged (see Forbidden save); observed earlier; announcement covered by `savesAndFailuresAreAnnouncedOnce`; Reduce Motion part not verified | ios-06 |
| Reduce Motion is on | en | not verified (accepted in 3.5/3.6) | — |
| Screen reader places a bet | en | not verified (VoiceOver accepted in 3.5); labels and announcement covered by `steppersAndChipsNameTheirTeams`, `savesAndFailuresAreAnnouncedOnce` | — |
| First page loading | es | observed: production cards with placeholder shapes, no controls; test `aPlaceholderCardExposesNoControls` | g6-ios-30, ios-09 |
| Status glyphs on both platforms | en | observed the check and lock glyphs on both platforms; `add`, `remove`, `lock`, `error` path strings are identical between the Android drawables and the iOS SVGs; `done` is the same shape written in the other viewBox convention (pre-existing asset) | g6-ios-02, g6-android-03 |

Restated scenarios on iOS:

| Scenario | Locale | Result | Evidence |
|---|---|---|---|
| ios-liquid-glass: Editing a prediction within a list | en | observed: boxes, steppers, chips and filter chips use standard non-glass styling | g6-ios-07, g6-ios-12 |
| ios-liquid-glass: Retrying a failed prediction | en | not verified on the simulator (no offline staging; accepted) | — |

## Android — task 6.1 (2026-10-19)

Device: Pixel_10_Pro_XL emulator `emulator-5554` (Android 16, software rendering), Prod debug build of the working tree installed with `adb install -r`. Spanish: `cmd locale set-app-locales com.felipearpa.fortuna --locales es-CO`, cleared afterwards. The emulator was heavily loaded (frames took 1.6–2.7 s), so no timing claims are made from it.

| Scenario | Locale | Result | Evidence |
|---|---|---|---|
| Opening the Bets tab | en, es | observed | g6-android-02, g6-android-03, g6-android-20 |
| Large text | es | not restaged; observed earlier at 200 % font scale | android-26 |
| Matches today and later this week | en, es | observed "Yesterday", "Wednesday", "Friday"; Spanish relative days earlier; tests `PendingBetDayTest`, `PendingBetDayLabelFormatTest` | g6-android-03, android-23, android-24 |
| A match in another year | en, es | observed "Mon, Jan 4, 2027" / "Lun, 4 ene 2027" | g6-android-17, g6-android-20 |
| Midnight passes | en | covered by test `given midnight passes in the device time zone then tomorrow becomes today`; not staged live | — |
| Typing a prediction | en | observed: typed 2 → focus to away, Incomplete; typed 1 → spinner, exactly one PATCH with 2–1 | g6-android-18, g6-android-19 |
| Adjusting with steppers | en | observed: `+` away → Saved 2–2; `−` on 0 stays 0 | g6-android-04, g6-android-05 |
| First tap on an empty card | es | observed: `+` away → one PATCH 0–1, card shown Guardada 0–1 under Con apuesta | g6-android-22, g6-android-24 |
| Quick pick | en | not restaged; observed earlier | android-05 |
| Rapid stepper taps | en | observed: three `+` home taps sent together → exactly one PATCH with 3–3, Saved | g6-android-06 |
| Edit while saving | en | covered by test `given a save in flight when the draft changes then the newest scores are resaved once`; the hand attempt produced two sequential saves (3–4, then 3–5) because the second tap arrived after the first response | g6-android-07, g6-android-08 |
| One side typed | en, es | observed: Incomplete/Incompleta, amber dashed box after the keyboard closed, hint names the missing team | g6-android-18, g6-android-23 |
| Completing the draft later | es | partly observed: the Incompleta draft survived switching to Con apuesta and back, and `+` away submitted 2–1 (one PATCH); further pages did not finish loading on the loaded emulator, so retention across a page append is covered by `given a draft when a page is appended then the row keeps its holder and draft` | g6-android-25, g6-android-26 |
| Clearing a saved prediction | en | observed: both boxes cleared → still Saved; after the keyboard closed, 2–2 again | g6-android-10, g6-android-11 |
| Network failure | en | not restaged; observed earlier with airplane mode | android-17, android-18 |
| Forbidden save | en | not staged (real lock); tests `given a forbidden save then the row locks with the last saved prediction`, `aRefusedSaveTurnsTheCardIntoTheLockedSavedPrediction` | — |
| Locked card | en | observed: Locked, muted 1–0, no controls; no input covered by `aLockedCardWithoutAPredictionShowsDashesAndTakesNoInput` | g6-android-03 |
| Locked card without a prediction | en | observed: muted dashes | g6-android-02 |
| Lock approaching | en | not staged (no match within the hour); covered by `PendingBetClockTest` and `theLockNoteJoinsTheSummary` | — |
| Jumping to missing bets | en, es | observed: No bet/Sin apuesta shows only `?` cards; saves drop the card, 1,113+ → 1,112+ → 1,111+ | g6-android-21, g6-android-22, android-09, android-10 |
| Filter with nothing left | en, es | observed "Nothing left to bet on right now." (en); Spanish earlier | g6-android-16, android-25 |
| Missing bets only on a later page | en | covered by tests `whileMorePagesRemainCountsCarryAPlusAndTheEmptyMessageWaits` and the list view-model tests | — |
| Counts while more pages remain | en, es | observed "1123+ / 1113+ / 10+"; exact counts earlier | g6-android-17, g6-android-20, android-08 |
| Placed filter with nothing placed | en | covered by test `given no loaded match has a prediction when Placed is selected then the filter is empty`; not staged | — |
| Pill becomes spinner and back | en | observed the spinner in the pill slot and Saved with nothing moving; 5× frames earlier; smoothness pending (5.6) | g6-android-19, android-04 |
| Chips leave after the first save | en | observed the chip row gone once both scores were set (as on iOS, before the response); frames earlier | g6-android-19, android-05 |
| Save rejected because the match locked | en | not staged; tests as for Forbidden save and `given a save is refused because betting closed then the lock is announced` | — |
| Reduce Motion is on | en | observed earlier at 0× animator scale; pending acceptance (5.6) | android-06 |
| Screen reader places a bet | en | partly observed: the accessibility tree names "One more goal for México", "México goals", "One less goal for Japón"; stepper targets 52×48 dp; TalkBack speech not verified (5.5 pending) | uiautomator dump, 2026-10-19 |
| First page loading | en | observed: production cards with placeholder shapes | g6-android-01, android-28 |
| Status glyphs on both platforms | en | observed (see iOS row) | g6-android-03 |

Restated scenarios on Android:

| Scenario | Locale | Result | Evidence |
|---|---|---|---|
| android-edge-to-edge: Editing a score near the list end | en | failed on the first build, fixed (see above), then observed: whole card above the keyboard on the last two cards, still visible after focus moved to the away box; Retry above the keyboard covered by `aFailedSavesRetryIsReachableAboveTheKeyboard` | g6-android-09, g6-android-12 (fail); g6-android-13, -14, -15 (after fix) |
| android-edge-to-edge: Returning from the keyboard | en, es | observed: closing the keyboard kept the draft and restored the list height; test `closingTheKeyboardRestoresTheListHeightAndKeepsTheDraft` | g6-android-11, g6-android-23 |

## Not staged during this session

- A real lock passing while the tab is open (needs new matches); the forbidden save observed on 2026-10-18 (ios-06, ios-07) stands for it.
- "Locks in N min" today: no open match was within an hour of its lock.
- Midnight passing, Reduce Motion, VoiceOver/TalkBack speech, iOS offline Retry.

## Coverage summary

Per platform, over the 31 `bet-entry` scenarios:

| Platform | Observed this session | Observed in an earlier capture only | Covered by tests only | Not verified |
|---|---|---|---|---|
| iOS | 21 | 5 (forbidden save, its animation, lock note, later-page No bet, empty Placed) | 1 (edit while saving) | 4 (midnight, offline Retry, Reduce Motion, VoiceOver) |
| Android | 20 (two of them partly: completing a draft after a page append, TalkBack) | 4 (large text, quick pick, offline Retry, 0× animator scale) | 7 (midnight, edit while saving, forbidden save, its animation, lock note, later-page No bet, empty Placed) | 0 (TalkBack speech itself not heard) |

Findings that did not fail a scenario, for a decision:

1. iOS (Android not checked): on a card with a saved prediction, clearing one box brings back the quick-pick chips and the hint (g6-ios-08); the spec says chips show "while the match has no saved prediction", task 3.3 says "only in `empty`/`incomplete`".
2. iOS and Android: the chip row collapses as soon as both scores are set, before the save returns (g6-ios-15, g6-android-19); the "Chips leave after the first save" scenario says after the save completes.
3. iOS: an exact filter count has no grouping separator ("3000") while a partial one has ("1.123+") (g6-ios-18 vs g6-ios-11); Android shows neither ("1123+").
4. iOS: changing Dynamic Type while the tab is open leaves the status pill at its old width, clipping it ("ved"), until the state changes or the app relaunches (g6-ios-23).
5. iOS: with the keyboard up, a focused card can stay partly covered (lower steppers, hint), and in one case the first scroll after typing did not dismiss the keyboard; the restated keyboard requirement covers Android only.
6. Android: typing over a selected value with the same digit (2 over 2) does not move focus to the away box, so the next digit replaced the home score (México went 1–2 by accident, then was restored).

## Production writes (2026-10-19)

All are bet saves by felipearpa on open matches in Felipe's pools. No layouts, matches, pools or gamblers were added or removed.

- QA solo, México–Japón (2026-10-21): 2–1 → 2–2 (iOS) → 2–1 (iOS) → 2–2 (Android) → 1–2 → 2–1 (Android). Ends at its starting 2–1.
- QA solo, Alemania–Canadá (2026-10-23): 0–3 → 3–3 → 0–3 (iOS, several saves) → 3–3 → 3–4 → 3–5 → 2–5 (a mis-tap) → 0–5 → 0–3 (Android). Ends at its starting 0–3.
- QA paging, Egipto–Argelia (2027-01-04): 2–1 → 2–2 → 2–3 (iOS). Left at 2–3.
- QA paging, new bets: Nueva Zelanda–Brasil 2–1, Paraguay–Jordania 0–1, Alemania–Países Bajos 1–1, Catar–Austria 2–1 (iOS); Australia–Austria 2–1, Brasil–República Checa 0–1, Panamá–Curazao 2–1 (Android).

## Fixes after task 6.1 (2026-10-19)

How the six findings above were resolved. Captures named `g6b-*` are in `verification/`.

- **Findings 1 and 2 (quick-pick chips):** fixed on both platforms. Chips show only while the card is editable and nothing is saved. They stay through the first save and collapse when it succeeds. A failed first save keeps them, and a saved card never shows them.
  - Android, observed: g6b-android-02, g6b-android-03, g6b-android-04, g6b-android-06 (the first save fails in airplane mode and the chips stay; Retry then saves).
  - iOS, observed: g6b-ios-03, g6b-ios-04, g6b-ios-08 (en, es).
  - iOS, not verified:
    - A failed first save keeping the chips: the simulator shares the Mac's network. Covered by the unit test `aFailedFirstSaveKeepsTheQuickPicks`.
    - The chip row collapsing in place on the All filter: seen only as the card leaving No bet.
- **Finding 3 (count format):** both platforms format exact counts, partial counts and the "at least N" text with the locale's number format.
  - iOS, observed: g6b-ios-01, g6b-ios-05, g6b-ios-09. The device locale is en_CO, so it shows "3.000"; the comma form ("3,000") was not observed on iOS.
  - Android, observed: g6b-android-01, g6b-android-07, g6b-android-08, g6b-android-09 ("3,000" in English, "3.000" in Spanish).
- **Finding 4 (iOS pill clipped after a live Dynamic Type change):** fixed; the pill re-measures when the text size changes. Observed: g6b-ios-06, g6b-ios-07, g6b-ios-10.
- **Finding 5 (iOS keyboard covering the steppers):** fixed; the focused card scrolls fully above the keyboard, and a spec line was added for it. Observed: g6b-ios-02, g6b-ios-03.
- **Finding 6 (Android focus with the same digit):** fixed with an `InputTransformation`, and a Compose test was added. Observed: g6b-android-05.

### Tests after the fixes

| Suite | Result |
|---|---|
| iOS `BetTests` (iOS 18.1 simulator) | 175/175 passed |
| Android `:bet` unit tests | 138/138 passed |
| Android `:app` unit tests | 60/60 passed |
| Android `:bet` pending-bet Compose UI tests | 17/17 passed |
| `PoolHomePendingBetKeyboardTest` | 4/4 result from task 6.1, before these fixes. They change nothing on that test's path except the chips; not re-run |

### Production writes in this round

All are bet saves on open matches.

- Android (QA paging): Uruguay–Senegal 2–1, Estados Unidos–Nueva Zelanda 1–1, Irán–Alemania 2–1 (saved on Retry after a failed attempt).
- iOS: Austria–Sudáfrica saved again at 1–1, Japón–Paraguay 2–1, Egipto–Argentina 1–1.

Accepted without TalkBack/VoiceOver actually speaking the announcements and without an Accessibility Scanner pass by the user on 2026-10-19.

Accepted without staging a real lock passing during the session, the "Locks in N min" note on the day of the check, midnight passing, an iOS failed save (offline), iOS Reduce Motion, the iOS chip row collapsing in place under All, and the en_US "3,000" form by the user on 2026-10-19.

### Untouched `:app` connected tests, with and without this change

Run on `emulator-5554` only, one attempt per class per side: first in a temporary worktree of HEAD `9e2957b1` (load average 6.5 at the start), then in the main checkout with this change (load average 59.7 at the start, raised by other work on the host). Command: `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedProdDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.<class>`.

| Test | HEAD `9e2957b1` | With this change |
|---|---|---|
| `HomeGoogleSignInAttemptTest.recreatingTheActivityWhileGoogleIsOpenStartsNoSecondRequest` | fails ("is not enabled") | fails ("is not enabled") |
| `HomeGoogleSignInAttemptTest.recreatingTheActivityAfterSuccessDoesNotNavigateAgain` | fails ("is not enabled") | fails ("is not enabled") |
| `HomeGoogleSignInAttemptTest.anAttemptWaitingForGoogleDisablesEverySignInAction` | fails ("is not enabled") | fails ("is not enabled") |
| `PoolHomeTabSwitchAppBarTest.everyTabSwitchShowsTheBarUnscrolledAndScrollingStillTintsAndHidesIt` | passes | passes (re-run alone at load average 9.8, since the first run with the change never started: Gradle timed out reading the emulator's properties) |
| `UsernameEditorKeyboardTest.closingAndReopeningTheKeyboardKeepsTheDraftAndSelectionWithoutAGap` | passes | passes |
| `UsernameEditorKeyboardTest.saveStaysBelowTheFieldGuidanceAndScrollsWithTheContent` | passes | passes |

The other tests in these classes passed on both sides.

Verdict: the three `HomeGoogleSignInAttemptTest` failures also occur on HEAD, so this change did not cause them. The `PoolHomeTabSwitchAppBarTest` and two `UsernameEditorKeyboardTest` failures from group 5 did not recur on either side.

Task 7.2: accepted without TalkBack actually speaking the missing-box text and without an Accessibility Scanner pass, under the screen-reader acceptance given by the user on 2026-10-19.

Task 7.1: accepted without a real hardware keyboard (HID key events used instead), the score boxes as drawn in rendered previews, and the live-paging list preview, by the user on 2026-10-19; all three are covered on the device.

## Fixed-size cards and end-of-list stillness (2026-10-19)

Captures named `g7-*` and `g6d-*` are in `verification/`.

### 7.1 iOS

- The quick-pick chips and the Incomplete hint are removed. The missing box now speaks "Add [team]'s score to save".
- No-movement frames: g7-ios-01 to g7-ios-04; first bet in Spanish: g7-ios-06.
- A fast-typing race was found and fixed: the home box keeps the first typed digit and the rest goes to away (g7-ios-05).
- Previews rendered: g7-ios-preview-item-states-dark, g7-ios-preview-item-states-light, g7-ios-preview-item-states-es, g7-ios-preview-list-placeholders.
- `BetTests`: 175/175 passed.

### 7.2 Android

- The same removals. The missing box's state description is `bet_score_box_missing_accessibility_value`.
- No-movement frames on the S8, in es-CO: g7-android-01 to g7-android-03.
- Unit tests passed: 135/135 in `:bet`, 60/60 in `:app`. Pending-bet Compose UI tests: 19/19, then 21/21 after the end-of-list work.
- The fast-typing race does not reproduce on Android: `input text 21` and bursts of key events saved 2–1.

### 7.3 Reference boards

- `reference/bets-list.png` and `reference/bet-row-states.png` were re-exported from the canvas (version 15). Neither shows chips or the hint, open cards have equal heights, and the Row states board now runs through Loading.

### End-of-list stillness

- iOS 18 and later keep the cards above still through `keepsBottomAnchoredOnSizeChanges`; iOS 16–17 keep the end-of-list jump, as accepted earlier.
- Android now does the same with `EndOfListStillness.kt`. When the last card shrinks at the bottom of the list, it keeps its old height as empty space and drops that space once it is off screen. Test: `EndOfListStillnessTest`.
- S8 evidence:
  - g7-android-end-of-list-01: the banner leaves, and Alemania and Haití stay still in every frame.
  - g7-android-end-of-list-02: scrolling up and back shows no jump or gap.
- Not verified on a device: a card shrinking at the end of the list because its match locks, which can't be staged; `EndOfListStillnessTest` covers it.

### Animation parity

- The S8 and iOS pairs in `g6d-anim-*-ios.png` / `g6d-anim-*-android.png` match after the fixes: one shared 250 ms ease-in-out curve, 40/60 fades that never overlap, and neighbouring cards moving in step.

### Decisions recorded

- Quick-pick chips are removed.
- Incomplete is shown only by the amber dashed box and the amber pill.
- The failure banner stays deliberately prominent; the Retry-in-pill mockups were rejected (`mock-failure-now-vs-proposal-c.png`).
- An open card keeps one height; only the failure banner and locking change it.

### Production writes in these rounds

All are bet saves on open matches.

- iOS: Haití–Egipto 2–1 (after an accidental 21–1), Alemania–Canadá 1–0, Ghana–Catar 2–1, Haití–Brasil 1–0, Ecuador–Australia 0–1 (accidental).
- Android: España–Brasil 1–0, Túnez–Alemania 2–1, Jordania–Paraguay 2–1, Panamá–España 2–1, Suecia–Arabia Saudita 2–1, Haití–Brasil 0–0 (accidental; if it is the same match as the iOS Haití–Brasil save, it overwrote that 1–0).

Task 5.6: accepted without animateContentSize() on the card (AnimatedVisibility alone animates the height; S8 recordings match iOS) by the user on 2026-10-19.

Task 6.1: accepted without observing the end-of-list stillness on a device when a match locks (covered by EndOfListStillnessTest and the observed banner case) by the user on 2026-10-19.

Task 8.1: iOS stepper fill set to 8 % light / 19.5 % dark (1.09:1 / 1.25:1 against the card), not Android's 1.14:1 in light, so the steppers stay softer than the iOS score boxes; approved by the user on 2026-10-19. Captures: g8-ios-real-light.png, g8-ios-real-dark.png. Android: g8-android-real-midpoint-{light,dark}.png.
