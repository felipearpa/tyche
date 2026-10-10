# Verification

## iOS — tasks 1.1–1.8 (2026-10-03)

Environment: Xcode 27.0 (27A266a), Debug build of the `Tyche` scheme from `iOS/Tyche.xcworkspace` against the production backend, signed in as the user's own account (the account was already signed in on all three simulators; no sign-in was performed). Pool used: "Copa Mundial de la FIFA 2026" (13 members). No production data was written.

| Simulator | Runtime | Use |
| --- | --- | --- |
| iPhone 16 Pro (iOS 18.1), A2F5EF6F-BA25-4035-9494-F6ACE0144047 | iOS 18.1 | Exploration before the change, main after-change checks, Spanish, dark, largest text |
| tmp-drawer-iPhone17-26.5, 48AD28C3-2C4E-406C-95FD-46563B66EA63 | iOS 26.5 | iOS 26+ (Liquid Glass chrome), light/dark, Reduce Motion |
| iPad Air 11-inch (M4), 838A5F55-ED10-4BE0-BF6A-9B7413F93AE2 | iOS 27.0 | Wide window (portrait) |
| tmp-bettests-18.1 (throwaway iPhone 16 Pro, deleted afterwards) | iOS 18.1 | Unit test runs, without Fortuna data |

Captures are in `verification/` with the prefix `ios-`. `ios-render-*` images were rendered from the production components with SwiftUI `ImageRenderer` (sample data, not real accounts), because the real feed has no uncomputed entries and failures cannot be staged on the simulators. In the rendered summary states the refresh `ProgressView` appears as a placeholder glyph; that is an `ImageRenderer` limitation, not the app.

### 1.1 Exploration of the existing Timeline (before any code change)

The interactions were driven on the iOS 18.1 simulator by tapping and swiping, and each step was confirmed with a screenshot.

- **Entry from Scores:** My pools → pool row → Scores tab → tap "El mono" (rank 1) pushes Timeline: inline title "Timeline", Back "Scores", Home icon on the right. Capture: `ios-1.1-before-timeline-from-scores-ios18.png`.
- **Current presentation:** the gambler name was a bold heading outside the list. Rows were grouped under sticky `dd/MM/yy` date headers. Each row showed the time, two team lines with the match score and a small prediction column, and a large `+N` with no unit; zero showed as `+0`. There was no avatar and no points total.
- **Row activation:** tapping the Inglaterra–Argentina row opened the existing match screen ("Score", Back "Timeline"). Capture: `ios-1.1-before-row-opens-match-ios18.png`.
- **Entry from a match's gambler list:** on the match screen, tapping "Santiago Arcila" pushed that gambler's Timeline (Back "Score"). Capture: `ios-1.1-before-timeline-from-match-ios18.png`.
- **Home:** tapping Home returned to the pool's Scores root. **Back:** returned to Scores.
- **Refresh:** a pull-down gesture was accepted and the content stayed on screen. No indicator was captured.
- **Pagination:** the page size is 50. Scrolling reached the opening match (11 Jun, México–Sudáfrica), which is more than 50 rows down for this World Cup pool, so at least one append followed the cursor. The last row was fully reachable. Capture: `ios-1.1-before-last-page-end-ios18.png`.
- **Data available:** the tournament is over. Every entry observed for El mono, Santiago Arcila, Juano, Abuela, and giraldocano517 was computed, so uncomputed (pending) entries cannot be observed with real data.
- **Personal History baseline:** `ios-1.1-before-personal-history-ios18.png` (+589 pts earned, "Your bet" rows).

### Implementation summary

- The shared `HistoryBetItem` and `HistoryPointsSummary`/`HistoryPointsHeader` take a `HistoryOwner` (`signedInGambler` by default, or `selectedGambler(name:)`). The owner selects the visible bet label ("Your bet" or "Bet"), the spoken bet and no-bet text, the summary announcement, and the summary's load and refresh failure messages.
- An uncomputed entry (`isComputed == false`) uses the same row. It shows the available match score or dashes, plus a neutral "Points pending" pill. Its announcement says "Match score: …" or "… match score unavailable" and "Points pending". A computed entry keeps History's "Final score" and award text.
- Timeline's view model gets `GetPoolGamblerScoreUseCase` and adds the summary load, generation-guarded stale-response rejection, and pull to refresh of rows and total together. These mirror History's view model.
- `BetTimelineList` uses the plain paging stack with a scrolling header: `TimelineGamblerHeading` (shared `AccountAvatar` plus the name as a heading) and the shared points header. Rows are native buttons (`InteractiveRowButtonStyle`) around the shared row and divider, with production placeholder rows for initial and append loading. Sticky date sections were removed.
- `BetTimelineItem`, `FinishedBetItem`, and `LiveBetItem` are deleted. Timeline was their only consumer.
- Home has the localized accessible name "Home" / "Inicio" (`go_home_action` in the UI package).
- The Timeline destination is identified by the signed-in account and route (`.id(TimelineContext)`), so another gambler, pool, or account starts a new view model.

### 1.2 Shared row: context and pending presentation

- `TimelinePresentationTextTests` (10 tests, passed) checks the spoken output for the selected gambler:
  - Settled rows: "Final score: Inglaterra 1, Argentina 2. Bet: 2 to 1. 2 points"; single and zero awards ("1 point", "0 points"); and missing result, bet, and award ("final score unavailable. No bet placed. Points unavailable").
  - Pending rows with a score ("Match score: Inglaterra 1, Argentina 0. … Points pending") and without one ("match score unavailable … Points pending").
  - A pending entry with a populated `score` is never announced or shown as an award (`HistoryRowPoints == .pending`).
- Personal defaults: the existing `HistoryPresentationTextTests` (9 tests, unchanged) still pass, including "Your bet: 2 to 1". A new test also confirms that the default owner produces the same label as the explicit personal owner.
- Visual: `ios-render-pending-and-settled-rows-light.png` and `-dark.png` show a pending row without a score (dashes), a pending row with a score of 1–0 and No bet, and settled rows with positive and zero awards.

### 1.3 Shared summary/header copy

- Spoken: "El mono earned 1 point in this pool", "… 120 points …", "… 0 points …", and "Points earned by El mono in this pool unavailable". Personal History's announcements ("120 points earned in this pool") are unchanged (existing tests).
- Visible, selected gambler: "Couldn't load points." with Retry, "Couldn't update points. Showing the last total." with Retry under the retained total, "— pts Total unavailable", and "Updating points…". Capture: `ios-render-summary-states-selected-gambler.png`. Personal History keeps "Couldn't load your points." (unchanged key).
- Placeholders are silent: summary and row placeholders return an empty label for both owners (tests).
- Initial failure and retry, refresh failure that keeps the prior total, and a successful unavailable total are covered by the view-model tests under 1.4.

### 1.4 Timeline state

`BetTimelineListViewModelTests` (10 tests, all passed) covers:

- The rows and total use the routed pool/gambler IDs (`pool-a` / `gambler-7`).
- The total is the server total (120) and not the sum of loaded awards (15).
- An append follows the cursor (`nil` → `page-2`), keeps the pending entries before the settled ones, and makes no second total request.
- Failed rows leave the loaded total intact.
- When the total fails, the rows stay usable and retry requests only the total (one row request in total).
- A failed refresh keeps the previous total marked as failed.
- A successful refresh without a total replaces the number with "unavailable".
- An older response that finishes last cannot overwrite the newer one.
- Two gamblers' view models each show only their own total when responses arrive out of order.
- A refresh that returns a pending entry as computed changes it to an award (pending to settled).

On the simulator, the Timeline total matched the leaderboard for every gambler opened: El mono 676, Juano 654, Abuela 592, giraldocano517 0.

### 1.5 Composition

- Reference hierarchy, light and dark: avatar and name, then "+676 pts earned", then flat rows. Each row has its own date and time, the result between the teams, "Bet x – y", a points pill, and a thin divider. There are no sticky date headers, and identity and total scroll away with the rows (`ios-1.8-last-content-reachable-ios18.png`). Captures: `ios-1.5-timeline-from-scores-light-ios18.png`, `ios-1.8-timeline-dark-ios18.png`, `ios-1.8-timeline-light-ios26.png`, `ios-1.8-timeline-dark-ios26.png`.
- During loading the identity stays real: the name and letter avatar are visible while the rows show production placeholder rows and the total has already loaded (`ios-1.5-loading-identity-kept-ios18.png`).
- Avatar fallback: the shared letter avatar uses the same letter and color as the leaderboard (El mono brown "E", Juano blue "J", giraldocano517 red "G").
- Full width: on the iPad (820 pt portrait), rows span the content width with team columns balanced around the result (`ios-1.8-timeline-ipad-portrait-ios27.png`).
- No Timeline-only row or skeleton remains. Real and placeholder rows are `HistoryBetItem`, and the placeholder rows (`HistoryBetPlaceholderRow`/`List`) are shared with History.

### 1.6 Integration

- Entry from Scores (El mono, Juano, Abuela) and from a match's gambler list (giraldocano517, Back "Score") both work. Capture: `ios-1.6-timeline-from-match-gamblers-ios18.png`.
- A row opens the existing match screen exactly once: one Back returns to Timeline (`ios-1.6-row-opens-match-ios18.png`).
- Tapping the identity or summary area does nothing.
- Home returns to the Scores root, and Back returns to the previous screen.
- After pull to refresh, the rows and total stayed on screen, with no second summary indicator (`ios-1.6-after-pull-to-refresh-ios18.png`).
- Identity and total stay usable when the rows fail or are empty: the empty/error states are rendered after the header in every list state (code, plus the "Total loaded, rows failed" preview). The rows stay usable when the total fails (view-model test, plus the summary render).
- Placeholder rows are not wrapped in a button and the row disables hit testing when it is a placeholder, so placeholders cannot activate.
- Home has the accessible name "Home" / "Inicio". No VoiceOver check was possible (see Not verified).

### 1.7 Localization and accessibility

- English, Spanish (`es`), and Spanish (Spain) (`es-ES`) are added for:
  - the bet label ("Bet", "Apuesta"),
  - "Points pending" ("Puntos pendientes"),
  - the match-score announcements ("Marcador: …", "… marcador no disponible"),
  - the selected gambler's bet and no-bet announcements ("Apuesta: 2 a 1", "No hay apuesta registrada"),
  - the selected gambler's summary announcements, with plural substitution ("El mono ganó 1 punto en esta polla"; es-ES "… quiniela"),
  - the neutral load and refresh failures,
  - the Home name.
- A Spanish test (es, es-ES) checks the singular and plural forms.
- Spanish UI on the simulator (`-AppleLanguages (es)`, es_CO): "Línea de tiempo", "+676 pts ganados", "Apuesta 1 – 2", and "domingo, 19 de jul." (`ios-1.7-timeline-spanish-es-CO-ios18.png`).
- Semantics in code: the name has the header trait, and the avatar, flags, and pill backgrounds are hidden from accessibility. Each row is one combined element with its label in the order date/time, result or score, bet, points, inside a native `Button`. The total is a separate element.
- Largest Dynamic Type (AX5, dark): the name and the total value and label reflow onto separate lines. Rows stack home, result, and away, and the footer keeps "Bet 2 – 1" together beside or under the pill (`ios-1.7-timeline-ax5-dark-ios18.png`, `ios-1.7-timeline-ax5-footer-ios18.png`). The "Points pending" pill wraps under the bet at AX3 (`ios-render-pending-rows-ax3.png`).
- A long name ("giraldocano517@gmail.com") wraps to two lines in the heading.

### 1.8 Builds, tests, and regression

- `xcodebuild build -scheme Tyche -workspace iOS/Tyche.xcworkspace -destination 'id=<sim>'` succeeded for iOS 18.1 and iOS 26.5. The iPad ran the same simulator build.
- `xcodebuild test -scheme Tyche … -only-testing:BetTests -parallel-testing-enabled NO` on the throwaway iOS 18.1 simulator: 48 passed, 0 failed. The run includes the existing History, points-summary, match, and pending suites.
- `xcodebuild test -scheme Tyche … -only-testing:AccountTests -only-testing:PoolTests -only-testing:UITests -only-testing:TycheTests` on the same simulator: 204 passed, 2 skipped, 0 failed. This covers the shared placeholder and avatar checks.
- No `Package.resolved` change after any run.
- Regression:
  - Personal History is identical to the baseline: "+589 pts earned", "Your bet" rows (`ios-1.8-personal-history-unchanged-ios18.png`).
  - A History row opens the match screen (Back "History") (`ios-1.8-history-row-opens-match-ios18.png`).
  - Scores is unchanged.
  - Bets shows its existing empty state, because no match is open (`ios-1.8-bets-tab-ios18.png`).
- First and last content: the first row sits below the header under the navigation bar, and the opening match is fully visible above the home indicator at the end of the list after the append (`ios-1.8-last-content-reachable-ios18.png`).
- Reduce Motion (iOS 26.5) was turned on, Timeline was opened, and the setting was restored. The rows loaded within the push transition, so a static placeholder frame could not be compared. The placeholder treatment is the unchanged shared `PulsingPlaceholderContent`, which holds a static midpoint under Reduce Motion (`ios-1.8-reduce-motion-loading-ios26.png`).
- Mixed pending/settled pages, pending to settled, and partial failures are covered by tests and renders only. See Not verified.

Settings changed and restored: appearance (dark, then back to light) on iOS 18.1 and iOS 26.5; content size (AX5, then back to large) on iOS 18.1; Reduce Motion (on, then back to off) on iOS 26.5. All three simulators are shut down again. The throwaway test simulator was deleted.

### Not verified

- **VoiceOver:** reading order, ownership in speech, focus stops, and the Home name. The Simulator has no VoiceOver. The labels and grouping are covered by unit tests and code; check them on a device.
- **Landscape and iPad resized windows:** the Simulator could not be rotated or resized here, because UI scripting is not permitted.
- **Real pending entries, a real pending-to-settled transition, mixed pending/settled pages, and real partial failures:** the tournament is over, there are no uncomputed entries, and the simulators share the Mac's network. These are covered by view-model tests and renders. No production write was made to stage them.
- **An uploaded photo on another gambler's Timeline:** no other member of the pool has a photo, so only the letter fallback was observed.
- **Pull-to-refresh indicator:** it was not captured. The refresh finished before the screenshot.

Accepted without real pending entries (covered by tests and renders) by the user on 2026-10-03.
Accepted without an uploaded photo on another gambler's Timeline by the user on 2026-10-03.
Accepted without VoiceOver verification (covered by tests and code) by the user on 2026-10-03.
Accepted without a real pending-to-settled transition, mixed pending/settled pages, real partial failures, the Reduce Motion placeholder frame, or the pull-to-refresh indicator (covered by tests and renders) by the user on 2026-10-03.

### Follow-up: landscape and resized windows (2026-10-03)

I didn't use osascript or macOS Accessibility for these checks, and no layout defect was found, so no code changed.

- **Resized windows** (iPad Air 11-inch (M4), iOS 27.0). The iPad's Multitasking & Gestures setting was already "Windowed Apps", so no setting was changed. I resized Fortuna's window by dragging its corner grabber with the iOS Simulator tool's `touch_path`, while Timeline (El mono) was open.
  - **Narrow window, about 375 × 870 pt:**
    - Rows span the window width, with team columns balanced around the result.
    - The heading and the "+676 pts earned" total fit.
    - Long team names ("Estados Unidos", "Bosnia y Herzegovina", "República Checa") wrap under their flags.
    - The last row (11 Jun, México–Sudáfrica, "+10 pts") is fully reachable at the bottom of the window.
    - Captures: `ios-1.8-followup-ipad-narrow-window-top-ios27.png`, `ios-1.8-followup-ipad-narrow-window-end-ios27.png`.
  - **Wide, short window, about 690 × 485 pt:**
    - Reopening Timeline from Scores shows the first content (identity, total, first row) below the window's navigation bar.
    - Rows use the full width, and the last row and its footer are fully reachable.
    - Captures: `ios-1.8-followup-ipad-wide-short-window-top-ios27.png`, `ios-1.8-followup-ipad-wide-short-window-end-ios27.png`.
  - The window was returned to full screen with its maximize control.
- **Landscape.** `simctl` has no rotation command. A temporary XCUITest (`XCUIDevice.shared.orientation = .landscapeLeft`, no app launch, run with `-parallel-testing-enabled NO` on the target simulator) rotated the device. The file was deleted after use. While rotated, the app was driven with the iOS Simulator tool, which takes portrait-native coordinates.
  - **iPad landscape (1180 × 820 pt):** identity, total and rows fill the width, with the centered result and balanced teams. The last row is fully reachable above the bottom edge. Captures: `ios-1.8-followup-ipad-landscape-top-ios27.png`, `ios-1.8-followup-ipad-landscape-end-ios27.png`.
  - **iPhone 16 Pro landscape** (iOS 18.1, 874 × 402 pt, the most constrained height):
    - Content stays inside the leading and trailing safe areas, clear of the Dynamic Island side.
    - Identity and total precede the first row.
    - The last row's footer sits fully above the home indicator.
    - Captures: `ios-1.8-followup-iphone-landscape-top-ios18.png`, `ios-1.8-followup-iphone-landscape-end-ios18.png`.
- **Restored:**
  - Both simulators were rotated back to portrait with the same helper, and portrait was confirmed from the screenshot dimensions.
  - The iPad window is full screen again.
  - Both simulators are shut down.
  - No `Package.resolved` changed, and the temporary UI test file is gone from the working tree.

## Android — tasks 2.1–2.8 (2026-10-03)

Environment: `:app` Prod debug build (`./gradlew :app:assembleProdDebug`) against the production backend, signed in as the user's own account on both devices (no sign-in was performed). Pool: "Copa Mundial de la FIFA 2026" (13 members). No production data was written.

| Device | API | Navigation | Locale | Use |
| --- | --- | --- | --- | --- |
| Galaxy S8+ (SM-G955F), `ce0417143a6ab8130c` | 28 (Android 9) | Three-button | es-CO | Exploration before the change (ARTEMIS Pro), after-change flows (ARTEMIS Pro), Spanish, semantics dump |
| `medium_phone` AVD, `emulator-5554` | 36 | Gesture | en-US | English, dark, 2× font, landscape, disabled animations, long name |
| `Pixel_7_API_36_no_account` AVD | 36 | Gesture | en-US | Instrumented tests only (Fortuna signed out there) |

The new build was installed over the existing app on both signed-in devices (`adb install -r`), which kept the sign-in. Captures are in `verification/` with the prefix `android-`. ARTEMIS traces: `cb5e4e16-477d-4a1e-9f25-b41cca4a8ed0` (2.1), `bc654b0d-75a0-45c5-a81d-95a430d0f6c0` and `1058b610-4826-4906-87c9-5a7aa188839b` (API 28 after the change), `6e237783-8845-487c-a809-7a69dbde59d1` (API 36).

### 2.1 Exploration of the existing Timeline (before any code change)

ARTEMIS Pro drove the installed build on the S8 (API 28, es-CO, three-button navigation); every step was confirmed from its screenshots and notes.

- **Entry from Scores:** pool row → "Puntajes" tab → "El mono" (rank 1, 676) pushes Timeline: title "Línea de tiempo", back arrow on the left, Home icon on the right. Capture: `android-2.1-before-timeline-from-scores-api28.jpg`.
- **Current presentation:** the gambler name was a bold heading fixed above the list. Rows were grouped under sticky `dd/MM/yy` date headers; each row showed the time, two team lines with the match score and a small prediction column, and `+N` with no unit (`+0` for zero). There was no avatar and no points total.
- **Pull to refresh:** accepted; content stayed on screen.
- **Pagination:** 17 flings reached the opening match (11/06/26, México 2–0 Sudáfrica, +10). The page size is 50, so at least one append followed the cursor. The last row was fully visible above the navigation buttons. Capture: `android-2.1-before-last-page-end-api28.jpg`.
- **Row activation:** tapping that row opened "Marcador" (the match screen); the top-left back arrow returned to Timeline. Capture: `android-2.1-before-row-opens-match-api28.jpg`.
- **Entry from a match's gambler list:** on "Marcador", tapping "Santiago Arcila" pushed his Timeline. Capture: `android-2.1-before-timeline-from-match-api28.jpg`.
- **Home:** returned to the pool home on "Puntajes". Capture: `android-2.1-before-home-returns-to-scores-api28.jpg`.
- **Back/Home names:** both icon buttons had an empty content description.
- **Data:** as on iOS, every entry observed was computed, so pending entries cannot be observed with real data.

### Implementation summary

- The shared `HistoryBetItem`, `HistoryPointsSummary`, and `HistoryPointsHeader` take a `HistoryOwner` (`SignedInGambler` by default, or `SelectedGambler(name)`), matching iOS. The owner selects the bet label ("Your bet" / "Bet"), the spoken bet and no-bet text, the summary announcement, and the load and refresh failure messages.
- `HistoryRowPoints` (`Awarded(HistoryPoints)` or `Pending`) comes from `isComputed`. A pending row uses the same layout with the available match score or dashes and a neutral "Points pending" pill (it may wrap at large font scales; a numeric award stays on one line). Its announcement says "Match score: …" or "… match score unavailable" and "Points pending".
- History's summary request logic moved into `HistoryPointsSummaryLoader` (generation-guarded, cancels the previous request). `FinishedBetListViewModel` and `BetTimelineListViewModel` both use it; Timeline gets `GetPoolGamblerScore` through Koin with the routed pool and gambler IDs.
- `BetTimelineList` now mirrors History's list: `TimelineGamblerHeading` (shared `AccountAvatar` with the leaderboard's fallback, decorative; name as a heading) and the shared points header are the first items of every list state, followed by shared rows and dividers. Initial and append loading use the shared production placeholder rows. Sticky date headers and the fixed name heading are gone, and the list takes the full Scaffold padding.
- `BetTimeLineItem`, `FinishedBetItem`, and `LiveBetItem` are deleted; Timeline was their only production consumer.
- Back and Home have the localized names "Back"/"Atrás" and "Home"/"Inicio" (`back_action`, `go_home_action` in `:ui`).
- Back, Home, and row activation go through `runIfStarted` on the route's back stack entry, so a double activation leaves the route once.
- The Timeline view model is keyed by `poolId:gamblerId`, like History's. Its back stack entry is the presentation's lifetime; sign-out navigates to Home with `popUpTo(initialRoute, inclusive)`, which clears the entry and its view model.
- `:bet` depends on `:account` for `AccountAvatar`.

### 2.2 Shared row: context and pending presentation

- `HistoryPresentationTest` (instrumented, 20 tests, all passed), new cases for the selected gambler:
  - Settled: "Sunday, Jul 19, 2:00 PM. Final score: Inglaterra 1, Argentina 2. Bet: 2 to 1. 2 points"; single and zero awards ("1 point", "0 points"); missing result, bet, and award ("final score unavailable. No bet placed. Points unavailable", no "0 points", no "Your").
  - Pending without a score: "Inglaterra versus Argentina, match score unavailable. Bet: 2 to 1. Points pending"; never "Final", and a populated `score` is not announced as an award.
  - Pending with a score: "Match score: Inglaterra 1, Argentina 0. No bet placed. Points pending".
  - Spanish (es-CO and es-ES): "Apuesta: 2 a 1", "1 punto", "No hay apuesta registrada", "Marcador: Inglaterra 1, Argentina 0", "Puntos pendientes"; never "Tu apuesta" or "hiciste".
- Personal defaults: the existing cases ("Your bet: 2 to 1", "Tu apuesta", "No bet placed") still pass unchanged.
- `HistoryRowPointsTest` (JVM, 3 tests, passed): computed awards including a confirmed zero, a computed entry without an award is unavailable, and an uncomputed entry is pending even with a score.
- On device the selected gambler's rows show "Bet 1 – 2" / "Apuesta 1 – 2" with the unchanged score hierarchy and pills (`android-2.5-timeline-light-english-api36.png`, `android-2.5-timeline-from-scores-light-api28.png`).

### 2.3 Shared summary/header copy

- Spoken (instrumented tests): "El mono earned 1 point in this pool", "… 120 points …", "… 0 points …", "Points earned by El mono in this pool unavailable", "El mono ganó 1 punto en esta polla", "El mono ganó 2 puntos en esta polla", "El mono ganó 1 punto en esta quiniela", "Puntos ganados por El mono en esta quiniela no disponibles". Personal History's announcements are unchanged (existing test).
- Visible: initial failure "Couldn't load points." with Retry (one retry per tap); refresh failure "Couldn't update points. Showing the last total." under the retained "+676 pts" total; personal History keeps "Couldn't load your points.".
- Placeholders are silent for the selected owner: no "X", no "pt", no content description, no click action.
- On the S8 the total is announced as "El mono ganó 676 puntos en esta polla" (uiautomator dump of the running app).

### 2.4 Timeline state

`BetTimelineListViewModelTest` (JVM, 10 tests, all passed; `./gradlew :bet:testDebugUnitTest`):

- Rows and total are requested with the routed `pool-a` / `gambler-7`, and never with another ID.
- The total is the server total (120), not the sum of loaded awards (15).
- An append follows the cursor (`null` → `page-2`), keeps the pending entries before the settled ones, and makes no second total request.
- Failed rows leave the loaded total intact.
- A failed total leaves the rows usable; retry requests only the total (one row request overall).
- A failed pull refresh keeps the previous total marked as failed and ends the pull state.
- A successful refresh without a total replaces the number with unavailable.
- An older failure that finishes last cannot overwrite the newer total.
- Two gamblers' view models each show only their own total when responses arrive out of order.
- A refresh that returns a pending entry as computed changes it to an award (pending to settled).

The existing `FinishedBetListViewModelPointsSummaryTest` (12 tests) passes unchanged against the shared loader.

On device the Timeline totals matched the leaderboard: El mono 676, Santiago Arcila 616, juandahoyos28@gmail.com 607.

### 2.5 Composition

- Reference hierarchy: avatar and name, then "+676 pts earned" / "ganados", then flat rows with their own date and time, the result between the teams, "Bet x – y", a points pill, and a divider. No sticky date headers; identity and total scroll away (the app bar also collapses on scroll as before). Captures: `android-2.5-timeline-from-scores-light-api28.png`, `android-2.5-timeline-light-english-api36.png`, `android-2.8-timeline-dark-api36.png`.
- Loading keeps the real identity: with animations off, the name and letter avatar stay real while rows show static production placeholder rows (`android-2.8-loading-animations-off-identity-kept-api36.png`).
- Avatar fallback: the shared letter avatar uses the leaderboard's letter and color (El mono brown "E", Santiago Arcila green "S", juandahoyos28 blue "J").
- Full width: rows span the content width with balanced team columns, also in landscape (2400 px wide).
- No Timeline-only row or skeleton remains; `BetTimelineListTest` checks that the loading state exposes no filler text, content descriptions, or click actions.

### 2.6 Integration

ARTEMIS Pro on the S8 (API 28, three-button navigation), all steps passed:

- Pull to refresh kept identity, total, and rows on screen (`android-2.6-after-pull-to-refresh-api28.jpg`).
- Flinging appended pages to the opening match (11 Jun, México 2–0 Sudáfrica, "Apuesta 2 – 0", "+10 pts"), fully visible above the navigation buttons (`android-2.8-last-content-reachable-api28.jpg`).
- Tapping that row opened "Marcador"; one tap on the back arrow returned to El mono's Timeline, so one activation opened exactly one screen (`android-2.6-row-opens-match-api28.jpg`).
- From "Marcador", "Santiago Arcila" opened his Timeline with "+616 pts ganados" (`android-2.6-timeline-from-match-gamblers-api28.jpg`).
- Home returned to the pool home on "Puntajes".

Instrumented `BetTimelineListTest` (5 tests, all passed):

- The name is a heading and the total is announced for the selected gambler.
- A row opens its match exactly once; the heading and total have no click action.
- When rows fail, identity and total stay above the list's existing failure ("Unexpected error"; pull to refresh remains its recovery).
- When the total fails, Retry calls only the summary retry and rows still open their match.
- While rows and total load, the identity is real and placeholders expose nothing and cannot be activated.

Back and Home announce "Atrás" and "Inicio" on the S8 (uiautomator dump).

### 2.7 Localization and accessibility

- English, Spanish (`values-es`), and Spanish (Spain) (`values-es-rES`) strings were added for the bet label, "Points pending", the match-score announcements, the selected gambler's bet and no-bet announcements, the summary announcements (plurals with `one`/`many`/`other`; es "polla", es-ES "quiniela"), the neutral load and refresh failures, and the Back/Home names.
- Semantics in the running app (S8, uiautomator dump, top to bottom): "El mono" (text; the avatar has no node), "El mono ganó 676 puntos en esta polla", then one clickable node per row such as "domingo, 19 de jul., 2:00 p. m. Resultado final: España 0, Argentina 0. Apuesta: 1 a 2. 0 puntos"; flags, avatar, dividers, and pill backgrounds add no nodes. The heading role is covered by `BetTimelineListTest`.
- 2× font (API 36): the name and the total value and label reflow; teams stack around the result; "Bet 1 – 2" stays together beside the pill (`android-2.7-timeline-font-2x-top-api36.png`).
- Long name: "juandahoyos28@gmail.com" fits on one line at 1× and wraps to two lines at 2× without clipping (`android-2.7-long-name-timeline-api36.png`, `android-2.7-long-name-font-2x-api36.png`).

### 2.8 Builds, tests, and regression

- `./gradlew :app:assembleProdDebug`: succeeded.
- `./gradlew :bet:testDebugUnitTest`: 29 passed, 0 failed (new: `BetTimelineListViewModelTest` 10, `HistoryRowPointsTest` 3).
- `./gradlew :app:testProdDebugUnitTest :ui:testDebugUnitTest :account:testDebugUnitTest`: 60, 44, and 85 passed, 0 failed.
- `ANDROID_SERIAL=<Pixel_7_API_36_no_account> ./gradlew :bet:connectedDebugAndroidTest`: `HistoryPresentationTest` 20/20, `BetTimelineListTest` 5/5, `BetPlaceholderAccessibilityTest` 3/3 passed; `BetTextFieldTest` 3 failed (known, pre-existing). The two placeholder cases for the deleted `LiveBetItem`/`FinishedBetItem` were removed; the shared row's placeholder is covered by `HistoryPresentationTest`.
- `ANDROID_SERIAL=<Pixel_7_API_36_no_account> ./gradlew :ui:connectedDebugAndroidTest`: 77/77 passed, including the shared placeholder motion and `RefreshableLazyPagingColumn` companion-refresh and inset tests.
- API levels and navigation: API 28 three-button (S8) and API 36 gesture (`medium_phone`).
- Light and dark: `android-2.5-timeline-light-english-api36.png`, `android-2.8-timeline-dark-api36.png`.
- Landscape (API 36): first content below the app bar, rows full width, and the opening match fully reachable above the gesture handle after the appends (`android-2.8-timeline-landscape-top-api36.png`, `android-2.8-timeline-landscape-end-api36.png`).
- Disabled animations: placeholder rows are static and only rows are masked; identity and the loaded total stay real (`android-2.8-loading-animations-off-identity-kept-api36.png`).
- Regression:
  - Personal History: "+589 pts ganados" with "Tu apuesta" rows; its first row opens "Marcador" and the back arrow returns to the pool home (`android-2.8-personal-history-unchanged-api28.jpg`).
  - Scores unchanged (`android-2.8-scores-unchanged-api36.png`).
  - Bets shows its existing empty state "Nada para mostrar", because no match is open (`android-2.8-bets-tab-api28.jpg`).
- Mixed pending/settled pages, pending to settled, and partial failures are covered by the view-model and instrumented tests only. See Not verified.

Settings changed and restored on `medium_phone`: dark mode (on, then off); font scale (2.0, then 1.0, twice); rotation (auto-rotate off with landscape, then portrait and auto-rotate on); animator, transition, and window animation scales (0, then back to the defaults: animator unset, transition and window 1.0). No settings were changed on the S8. Emulators were swapped through ARTEMIS (`mobile_diagnose(launch_avd=…)`) and stopped with `adb emu kill`; `medium_phone` is running again with Fortuna installed.

### Not verified

- **TalkBack speech:** TalkBack was not enabled on either device; it is a system accessibility setting outside the settings allowed for this check. Reading order, ownership, grouped rows, and the absence of decorative nodes were checked from the uiautomator tree of the running app and the instrumented semantics tests, not by listening to TalkBack.
- **Real pending entries, a real pending-to-settled transition, mixed pending/settled pages, and real partial failures:** the tournament is over, every entry is computed, and no production write was made. Covered by `BetTimelineListViewModelTest`, `HistoryPresentationTest`, and `BetTimelineListTest`. A real network failure (airplane mode) was not requested.
- **An uploaded photo on another gambler's Timeline:** no other member of the pool has a photo, and Timeline does not open for the signed-in gambler, so only the letter fallback was observed.
- **Dark mode, large font, landscape, and disabled animations on API 28:** checked on API 36 only; on the S8 only light, 1× font, and portrait were observed.
- **Pull-to-refresh indicator:** the refresh finished before a screenshot was taken; only that content stayed was observed.
- **Narrow windows (split screen / freeform) and resized windows:** not exercised; the narrowest window observed is the S8's 411 dp portrait width.

Accepted without real pending entries (covered by tests) by the user on 2026-10-03.
Accepted without an uploaded photo on another gambler's Timeline by the user on 2026-10-03.
Accepted without real row or total failures on a device (covered by tests) by the user on 2026-10-03.
Accepted without a real pending-to-settled transition, mixed pending/settled pages, real partial failures, the pull-to-refresh indicator, API 28 dark/large-font/landscape/animations-off checks, or windows narrower than 411 dp by the user on 2026-10-03.

### Follow-up: TalkBack (2026-10-03)

TalkBack 16.0 (Android Accessibility Suite, already installed) on `medium_phone` (API 36, en-US, light, 1× font), Fortuna Prod debug build from this change, El mono's Timeline opened from Scores. No app code changed.

- **Setup:** original values were `enabled_accessibility_services=com.artemis.helper/.ArtemisAccessibilityService`, `accessibility_enabled=1`, `touch_exploration_enabled=0`. TalkBack was enabled by appending `com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService` to `enabled_accessibility_services`.
- **Input method:** `adb shell input` events bypass touch exploration (an injected swipe opened a row as a plain tap) and `sendevent` is not permitted. Touches were sent through the emulator console (`adb emu event send` on the virtual multi-touch screen), which TalkBack handles as real touches.
- **Evidence:** TalkBack's green focus outline in each capture, plus the focused node's content description and text from `uiautomator dump`. TalkBack's spoken text is not written to logcat in this release build (the TTS engine logged only that synthesis ran), so the exact utterance, including role words such as "Heading" or "Button", was not captured.

| Focus stop (swipe order) | Focused node's text / description | Capture |
| --- | --- | --- |
| Back | "Back" | `android-2.7-talkback-06-back-focused-api36.png` |
| Title | "Timeline" | `android-2.7-talkback-05a-title-focused-api36.png` |
| Home | "Home" | `android-2.7-talkback-05-home-focused-api36.png` |
| Gambler name | "El mono" (the avatar is not a stop) | `android-2.7-talkback-01-name-focused-api36.png` |
| Total | "El mono earned 676 points in this pool" | `android-2.7-talkback-02-total-focused-api36.png` |
| Row 1 | "Sunday, Jul 19, 2:00 PM. Final score: España 0, Argentina 0. Bet: 1 to 2. 0 points" | `android-2.7-talkback-03-first-row-focused-api36.png` |
| Row 2 | "Saturday, Jul 18, 4:00 PM. Final score: Francia 4, Inglaterra 6. Bet: 2 to 1. 0 points" | `android-2.7-talkback-04-second-row-focused-api36.png` |

- **Order and stops:** swipe right went name → total → row 1 → row 2; swipe left from row 2 went back through row 1, total, and name to Home, title, and Back. Each row is one focus outline around the whole row. The avatar, flags, score digits, and points pill were never separate stops; a touch on row 1's flag focused the whole row.
- **Wording:** the focused rows say "Bet" with full point units ("0 points"). The other loaded rows in the tree read the same way (for example "… Bet: 1 to 2. 20 points"), and none say "Your bet".
- **Heading:** the name's heading role is not exposed in `uiautomator dump`, and the speech was not captured, so "Heading" was not observed from TalkBack. It remains covered by `BetTimelineListTest`. One attempt to switch TalkBack's reading control to Headings by gesture only cleared focus and was not repeated.
- **Activation once:** with row 1 focused, TalkBack activation opened the existing "Score" screen for España–Argentina (`…-07-row-focused-before-activation-…`, `…-08-row-activation-opens-match-…`). One hardware Back returned to El mono's Timeline (`…-09-one-back-returns-to-timeline-…`), so the row opened the match once. Two activation inputs were sent (a console double-tap, then the Alt+Enter keyboard shortcut); which one TalkBack acted on was not determined. The shell Back sent between them was swallowed under TalkBack, which a later shell Back on Timeline confirmed.
- **Personal History:** the focused first row reads "Sunday, Jul 19, 2:00 PM. Final score: España 0, Argentina 0. Your bet: 3 to 2. 0 points", and the total reads "589 points earned in this pool" (`android-2.7-talkback-10-personal-history-row-focused-api36.png`).
- **ANR during the session:** one "Fortuna isn't responding" dialog appeared at 19:35 while History loaded. It recorded input dispatch timed out, CPU pressure 80%, system_server 79%, and TTS 27%; its stack dump timed out. Each tap on "Wait" brought the dialog back, so the app was closed from the dialog and relaunched; that ended the process only, and the sign-in stayed. A live `dumpsys activity top` beforehand returned the app's view hierarchy, so its main thread was responsive. The ANR is attributed to emulator load from TalkBack, speech synthesis, and repeated `uiautomator` dumps, not to Timeline or History code.
- **Restored:** `enabled_accessibility_services=com.artemis.helper/.ArtemisAccessibilityService`, `accessibility_enabled=1`, `touch_exploration_enabled=0` (read back, and `dumpsys accessibility` lists only the Artemis helper). No other setting changed. The S8 was not touched.

Not verified: TalkBack's spoken output itself, including the role words "Heading" and "Button" (only focus outlines and node text were observed), and TalkBack in Spanish (the emulator is en-US).

Accepted without capturing TalkBack's spoken output (including "Heading"/"Button" role words) or TalkBack in Spanish by the user on 2026-10-03.

## iOS — tasks 3.1–3.3 (2026-10-04)

Environment: Xcode 27.0, Debug build of the `Tyche` scheme from `iOS/Tyche.xcworkspace` against the production backend (read-only), on `iPhone 16 Pro (iOS 18.1)` A2F5EF6F-BA25-4035-9494-F6ACE0144047, already signed in to the user's account. No sign-in was performed and no production data was written. Unit tests ran on a throwaway iPhone 16 Pro (iOS 18.1) without Fortuna data, deleted afterwards. Captures are in `verification/` with the prefix `ios-3.3-`.

Failures were staged with a temporary, debug-only switch, as approved by the user: `LazyPagingCursorSource.load` failed every first-page request, and `GetPoolGamblerScoreUseCase.execute` failed every total request, while a marker file (`Documents/debug-fail-lists` or `Documents/debug-fail-totals`) existed in the app's data container. Creating and deleting the markers with the app running made a request fail or succeed without rebuilding. Both files were restored afterwards (`shasum` before and after: `7da83ddc…` and `0e357c69…`), the real build was rebuilt and reinstalled, and the installed app contains no trace of the switch.

### 3.1 Retry on the shared list error

- `LazyPagingVStackError` now takes a `retry` action and shows a localized Retry button beneath the error, with the same label (`SharedStringResource.retryAction`, "Retry" / "Reintentar") and style (`.standardProminent`) as the append error.
- The defaults in `LazyPagingVStack` and all three `RefreshableLazyPagingVStack` initializers pass the list's own reload. History and Timeline pass it in their error content, so Retry reloads only the rows and never the points total.
- Retry calls `LazyPagingItems.refresh()`, not `retry()`. The library's `retry()` replays the last load it attempted. When a pull to refresh over loaded rows fails, the visible rows have already asked for the next page during that refresh, so `retry()` replays that skipped append and does nothing. On the simulator, Retry on Scores did nothing after a failed pull to refresh until this was changed (`ios-3.3-scores-retry-after-failed-pull-to-refresh-ios18.png` shows the fixed result).
- Screens that render the shared error, confirmed in code:
  - through the defaults: Scores (`GamblerScoreList`), Bets (`PendingBetList`), the match's bettors (`MatchBetList`), and Pool from layout (`PoolFromLayoutCreatorList`);
  - explicitly: History (`FinishedBetList`), Timeline (`BetTimelineList`), Manage gamblers (`ManageGamblersList`, which passed the shared error itself), My pools (`PoolScoreErrorList` inside `PoolScoreList`), and the templates section of the My pools empty state.
- Tests (Swift Testing):
  - `LazyPagingVStackErrorTests` (UITests, 6): the button is labelled "Retry" and runs its action once. Through each of the four default initializers, Retry requests the list exactly once more and the refresh that pull to refresh runs still requests it again. A regression test fails a pull to refresh over loaded rows while a row asks for the next page, then expects Retry to request the list again. It failed with the previous `retry()` wiring (timeout waiting for the request) and passes now.
  - `BetTimelineListViewModelTests` and `FinishedBetListViewModelPointsSummaryTests` each gained `listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth`: with the total loaded and the rows failed, tapping the list's Retry requests the rows once (2 requests in total), leaves the total request count at 1 with the total unchanged, never calls the summary retry, and a later pull to refresh reloads both.

### 3.2 Summary retry as an inline icon button

- Canonical source: `assets/icons/refresh.svg`, Material Symbols "refresh", outlined, fill 0, weight 400, grade 0, optical size 24, unmodified from material-design-icons commit `bd8cb85b…` (the commit already recorded for `add.svg`). SHA-256 `e56d7ee3…3c1f7c`, identical to the Google Fonts glyph. Source, license, and the iOS derivation are recorded in `assets/icons/README.md`.
- iOS asset: `refresh.imageset/refresh.svg` in the UI package, a byte-identical copy rendered as a template with vector data preserved (`SharedImageResource.refresh`). The Android asset is not part of this group.
- `HistoryPointsHeader` shows the initial-failure and refresh-failure text with `HistoryPointsRetryButton` beside it. The button is borderless and icon-only, with a 22-point glyph that scales with Dynamic Type (`subheadline`) inside a 44 × 44-point minimum target that takes taps over its whole area. Its accessible name is "Retry" / "Reintentar", and it still calls only the summary retry.
- Measured glyph contrast against the background: 5.13:1 in light (#2E7D32 on white) and 7.56:1 in dark (#4CAF50 on black).
- `HistoryPointsRetryTests` (BetTests, 4 tests, two parameterized over History and Timeline × initial and refresh failure):
  - the failure text and the retry button share one row;
  - the button's accessibility label is "Retry", its label is the image alone, and the image's frame minimum is 44 × 44;
  - tapping runs the summary retry exactly once;
  - a refresh failure keeps the last total above the retry;
  - a current total shows no retry.
- BetTests now depends on ViewInspector 0.10.3, the version the UI and Pool test targets already use.

### 3.3 Builds, tests, and simulator checks

- `xcodebuild test -scheme Tyche -workspace iOS/Tyche.xcworkspace -destination 'id=<throwaway iOS 18.1>' -only-testing:BetTests -only-testing:UITests -only-testing:PoolTests -only-testing:AccountTests -only-testing:TycheTests -parallel-testing-enabled NO`: 264 passed, 2 skipped, 0 failed.
- `xcodebuild build` of `Tyche` for the iOS 18.1 simulator succeeded with and without the temporary switch. No `Package.resolved` changed.
- Initial list failure and Retry, on every screen that renders the shared error:
  - My pools: `ios-3.3-my-pools-list-error-retry-ios18.png`, then `-after-retry`.
  - Scores: `ios-3.3-scores-list-error-retry-ios18.png`. After a failed pull to refresh, Retry reloads the leaderboard: `ios-3.3-scores-retry-after-failed-pull-to-refresh-ios18.png`.
  - Bets: `ios-3.3-bets-list-error-retry-ios18.png`, then the empty state after Retry, because no match is open (`-after-retry-empty`).
  - Pool from layout: `ios-3.3-pool-from-layout-list-error-retry-ios18.png`, then the template list (`-after-retry`). Nothing was created.
  - Manage gamblers ("Prueba", which the user owns): `ios-3.3-manage-gamblers-list-error-retry-ios18.png`. Retry loaded the three members. No capture was kept of the loaded list, because it shows other members' email addresses. No member was removed.
  - Match bettors: `ios-3.3-match-bettors-list-error-retry-ios18.png`. Retry started the reload, but each time the request burst described under Not verified followed and the app was terminated, so the reloaded list was not captured.
  - History: the list failed with the total loaded (`ios-3.3-history-list-error-total-kept-ios18.png`). Before Retry was tapped, the total request was made to fail. Retry loaded the rows and the total stayed "+589 pts earned", so the total was not requested again (`ios-3.3-history-after-list-retry-total-not-reloaded-ios18.png`).
  - Timeline (El mono): the rows and the total both failed (`ios-3.3-timeline-list-and-summary-initial-failure-ios18.png`). Retry loaded the rows while the summary still read "Couldn't load points." (`ios-3.3-timeline-after-list-retry-summary-still-failed-ios18.png`).
- Summary retry, with list requests made to fail first so a row reload would have shown the list error:
  - History initial failure: "Couldn't load your points." with the icon beside it (`ios-3.3-history-summary-initial-failure-icon-retry-ios18.png`).
  - History refresh failure: "+589 pts earned", then "Couldn't update your points. Showing the last total." with the icon (`ios-3.3-history-summary-refresh-failure-icon-retry-ios18.png`). The icon dimmed while pressed. It loaded the total and the rows stayed (`ios-3.3-history-summary-retry-total-only-ios18.png`).
  - Timeline initial failure, then total-only retry: "+676 pts earned" with the rows kept (`ios-3.3-timeline-summary-retry-total-only-ios18.png`).
  - Timeline refresh failure: "Couldn't update points. Showing the last total." under "+676 pts" (`ios-3.3-timeline-summary-refresh-failure-icon-retry-ios18.png`).
- Largest Dynamic Type (AX5) in dark appearance on Timeline:
  - The refresh-failure text wraps over four lines with the scaled icon beside it (`ios-3.3-timeline-refresh-failure-ax5-dark-ios18.png`).
  - The list error's Retry is reachable after scrolling, and tapping it loaded the rows (`ios-3.3-timeline-list-error-retry-ax5-dark-ios18.png`).
- Light appearance is shown by every other capture.
- VoiceOver-facing names come from the tests above: "Retry" for the list button through its visible label, and "Retry" for the summary icon through `accessibilityLabel`. The Simulator has no VoiceOver.

Settings changed and restored: appearance (dark, then back to light) and content size (AX5, then back to large) on the iOS 18.1 simulator. The marker files were deleted. The simulator is shut down with the real build installed. The throwaway test simulator was deleted.

### Not verified

- **Request bursts against production.** Twice in this session the debug build sent about 100–130 requests per second to the production API (503 responses appeared) and to the avatar bucket, until the app was terminated:
  - from 09:16:25 to 09:17:52, about 10,000 requests, starting when Timeline was opened after several retries;
  - three short bursts after that, each stopped within about two seconds by a watchdog that terminated the app above 40 requests in 3 seconds.
  - The bursts are reproducible on the match bettors screen. Its list fails, the list is reloaded, and the burst starts as soon as the rows return. The reload can be the new Retry or the existing pull to refresh: pull to refresh alone, without Retry, also caused a burst. This points to an existing loop on that screen that Retry now reaches with one tap.
  - The first burst (Timeline) could not be reproduced with the same steps.
  - A render loop (continuous frame invalidations) started about 15 seconds before each of the first two bursts.
  - The root cause was not found, and no fix was attempted because it is outside these tasks. It needs investigation before release.
- **VoiceOver** reading of the two Retry controls: the Simulator has no VoiceOver. Covered by tests and code.
- **iOS 26+ and iPad** were not rerun for these controls: only the iOS 18.1 simulator was used.

Accepted without VoiceOver verification (labels covered by tests) by the user on 2026-10-04.
Accepted without iOS 26+ and iPad checks of the list and summary retry by the user on 2026-10-04.
Accepted without the match bettors list loading after Retry (blocked by the request burst noted above) by the user on 2026-10-04.

## Request loop — tasks 4.1–4.2 (2026-10-04)

Environment: Xcode 27.0, Debug builds of the `Tyche` scheme on `iPhone 16 Pro (iOS 18.1)` A2F5EF6F-BA25-4035-9494-F6ACE0144047 and `tmp-drawer-iPhone17-26.5` 48AD28C3-2C4E-406C-95FD-46563B66EA63; Android `:app:assembleProdDebug` on the `medium_phone` emulator (emulator-5554, API 36). All against the production backend, read-only, already signed in. No sign-in, sign-out, or production write. Captures are in `verification/` with the prefix `loop-4.`.

Temporary, debug-only edits (all reverted, see the end of this section):

- iOS: `LazyPagingCursorSource.load` failed the first page while `Documents/debug-fail-lists` existed (every list) or `Documents/debug-fail-<ItemType>` existed (one list type), and logged each load and whether its task was cancelled. `AvatarImageStore` and `AccountAvatar` logged avatar fetches. The Scores list (`GamblerScoreList`, `GamblerScoreListView`, `GamblerScoreListViewModel`) logged its body, appearance, and view model, and for one experiment used the plain column with a logged pull-to-refresh action. `PoolHomeRouter` was built once with `stabilizesNavigationLayout: false`.
- Android: `CursorPagingSource.load` failed the first page while `files/debug-fail-lists` existed and logged each load; `AvatarOriginNetworkClient` logged each avatar request.
- Watchdogs: on iOS, a log stream of CFNetwork task summaries (status, protocol, connection, byte counts only) and the debug lines, terminating the app above 40 requests, or 40 failed list loads, in 3 seconds. On Android, logcat filtered to the debug lines and Ktor request paths without query strings, force-stopping Fortuna above 40 in 3 seconds.

### 4.1 Reproduction and root cause

- The loop does not need a failure. On iOS 18.1, opening the match bettors screen from History started it twice, and switching from Scores to History once. The list that reloaded was the Scores leaderboard in the non-selected Scores tab (3,239-byte responses), not the match bettors list. Each time, the watchdog terminated the app within 2 seconds, by which point about 70 (10:13:07–10:13:09, 17 of them answered 503) and about 50 (10:15:18–10:15:20) requests had reached production. The 3.3 bursts started "when rows came back" because failing loads never reach the network; with the failure still in place the same loop ran offline at one load every 16.7 ms (10:10:47–10:11:02, 1,269 loads).
- With the Scores list failing offline (`debug-fail-PoolGamblerScoreModel`), the loop reproduced with no production traffic. Every load in it ran in an already-cancelled task, and the error view inside the hidden column logged an appearance and a disappearance in the same update.
- The loop continued with the pull-to-refresh action removed (no pull-to-refresh calls were logged), with the drawer's hosting controller disabled, and with the Scores view model and its `LazyPagingItems` unchanged throughout.
- The new Retry and pull to refresh on the match screen each issued one request; the burst that followed them in 3.3 came from the hidden Scores tab.
- The Timeline path that triggered the first burst was retried once: opening El mono's Timeline from Scores puts Scores in the same stack, which disappears normally and does not loop. The first burst most likely came from History or Scores in a non-selected tab while Timeline was pushed.
- iOS 26.5 did not reproduce it with the same steps: the hidden Scores column did not reload when the match screen opened.

Root cause: on iOS 18, SwiftUI starts and cancels a view's `task` within one update for a view in a non-selected tab, each time that view updates. The paging library's column (`LazyPagingVStack` in lazy-paging-swift 0.0.3) refreshes from that `task` without checking for cancellation, and the refresh publishes a loading state at once. That publish updates the hidden column, which starts and cancels the next `task`, which refreshes again: one reload per frame, each with its rows' avatars, for as long as the tab stays hidden. Any update to the hidden column starts it, for example pool home re-rendering when the selected tab pushes the match screen. Since pool home's tabs became separate navigation stacks (#27), the other tabs stay alive and hidden while a destination is shown.

### Library fix (outside this change)

The fix belongs in the paging library, which owns the column's appearance load. It was made and tested in a clone of lazy-paging-swift at v0.0.3 (`loop-4.2-lazy-paging-swift-v0.0.3-fix.patch`):

- `LazyPagingItems.refreshOnAppear()` (internal) returns without loading when its task is already cancelled, and otherwise calls `refresh()`. `LazyPagingVStack`'s appearance task calls it instead of `refresh()`.
- `refresh()` is unchanged. A first version that guarded `refresh()` itself stopped the loop but broke History's pull to refresh: SwiftUI had already cancelled the refreshable action's task when it reached the list, so only the total reloaded. Pull to refresh and Retry call `refresh()` directly and still reload.
- No throttling or debouncing.

Regression tests (Swift Testing, `LazyPagingItemsLoadTests`):

- A cancelled appearance loads nothing and leaves the load state unchanged; 60 cancelled appearances load nothing. Both failed before the guard (the page loaded each time) and pass after it.
- A live appearance loads the first page once, and `refresh()` from a cancelled task still loads. Both pass before and after.
- `swift test` in the clone: 119 tests in 22 suites passed.

On the iOS 18.1 simulator, the app was built against the fixed clone through a temporary local package path in `UI`, `Bet`, and `Pool`:

| Step | Before the fix | With the fix |
|---|---|---|
| Open the match screen from History (hidden Scores) | about 60 Scores reloads per second until terminated | no Scores reload |
| Match bettors, Retry after a failed list | not observable (loop) | 1 list request, then none |
| Match bettors, pull to refresh after a failed pull | not observable (loop) | 1 list request, then none |
| History, Retry after a failed list | — | 1 list request, no total request |
| History, pull to refresh after a failed list | — | 1 list and 1 total request |
| History, plain pull to refresh | — | 1 list and 1 total request |
| Timeline (El mono), Retry after a failed list | — | 1 row request, no total request |
| Timeline (El mono), pull to refresh after a failed list | — | 1 row and 1 total request |

Rows on these three screens have no avatar requests of their own (the Timeline heading's avatar was already loaded), so no recovery issued an avatar request. The watchdog never fired with the fix. Captures: `loop-4.2-match-bettors-list-error-before-retry-ios18.png`, `-after-retry-`, `-after-pull-to-refresh-`, `loop-4.2-history-list-error-total-kept-ios18.png`, `loop-4.2-history-after-retry-ios18.png`, `loop-4.2-timeline-list-error-ios18.png`, `loop-4.2-timeline-after-retry-ios18.png`.

The repository still depends on lazy-paging-swift 0.0.3, so the shipped app keeps the loop until the fix is released in the library and `UI`, `Bet`, and `Pool` move to that version.

Observed while measuring, not changed:

- Opening the match screen requests its list twice: the list is rendered in a different branch while the header loads and again once it has loaded, so the first column's task is cancelled and a second one starts.
- Opening History for the first time requested the total twice.

### 4.2 Android

Android has no equivalent path. Its columns use AndroidX Paging with `cachedIn` in each view model; neither the app's `RefreshableLazyPagingColumn` nor lazy-paging-kmp 0.0.2 refreshes on composition. Only pull to refresh and explicit retries call `refresh()`. Android has no list Retry yet (section 5), so recovery used pull to refresh. The same steps were run on emulator-5554 with the watchdog:

- Opening the pool, switching from Scores to History, and opening the match from History: one request per screen and nothing from the hidden tabs.
- Match bettors, a failed pull to refresh and then a pull to refresh: 1 list request.
- History, a failed pull to refresh and then a pull to refresh: 1 list and 1 total request.
- Timeline (El mono, opened from Scores), a failed pull to refresh and then a pull to refresh: 1 row and 1 total request.
- No avatar requests during any recovery, and the watchdog never fired.

Captures: `loop-4.3-android-match-bettors-list-error-api36.png`, `loop-4.3-android-match-bettors-after-pull-to-refresh-api36.png`, `loop-4.3-android-history-list-error-api36.png`, `loop-4.3-android-timeline-list-error-api36.png`, `loop-4.3-android-timeline-after-pull-to-refresh-api36.png`. No Android fix or test was needed.

### Reverting the temporary edits

`shasum` before and after:

- iOS: `LazyPagingCursorSource.swift` 7da83ddc…, `AvatarImageStore.swift` e4d8d95e…, `AccountAvatar.swift` ae2ca121…, `GamblerScoreListView.swift` 6341e4a9…, `GamblerScoreListViewModel.swift` 8f194601…, `GamblerScoreList.swift` 5fa73709…, `PoolHomeRouter.swift` 91347b02…, `UI/Package.swift` 244511ff…, `Bet/Package.swift` 1640cd47…, `Pool/Package.swift` 9bc7a1fc…, and every `Package.resolved` (the workspace's 2296f36f… included) all match.
- Android: `CursorPagingSource.kt` 61c9e715…, `AvatarOriginNetworkClient.kt` 0886f89f… match.
- The real builds were rebuilt and reinstalled on both simulators and on emulator-5554; neither contains the debug strings. The marker files were deleted. Both simulators are shut down.

Accepted without checking the Android loop on the Galaxy S8+ (API 28) by the user on 2026-10-04.

## Android — tasks 5.1–5.3 (2026-10-04)

Environment: `:app` Prod debug build (`./gradlew :app:assembleProdDebug`) against the production backend (read-only), on the `medium_phone` AVD (`emulator-5554`, API 36, 1080 × 2400, gesture navigation, en-US), already signed in to the user's account. No sign-in, sign-out, or production write. Instrumented tests ran on `Pixel_7_API_36_no_account` (`emulator-5556`), with medium_phone shut down while they ran. The Galaxy S8+ was not used. Captures are in `verification/` with the prefix `android-5.3-`.

Failures were staged with a temporary, debug-only switch, as approved by the user: `CursorPagingSource.load` failed every first-page load while `files/debug-fail-lists` existed in the app's data directory, and `GetPoolGamblerScore.execute` failed every total request while `files/debug-fail-totals` existed; both logged each load under the tag `DebugFail`. Markers were created and deleted with `adb shell run-as` while the app ran. A watchdog read logcat (Ktor request paths without query strings, response codes, and the `DebugFail` lines only) and would force-stop Fortuna above 40 requests or loads in 3 seconds. It never fired. Avatar requests (Coil) were not counted, because no avatar logging was added.

### 5.1 Retry on the shared list error

Exploration first, on the unchanged code with both markers present (ARTEMIS Flash, trace `c05395c3-bd17-4fba-a39f-ac7dba6a3834`): History showed "Couldn't load your points." with a filled "Retry" button beneath it, and "Unexpected error" with no button under the list's error. Scores showed the same list error with no button. Pull to refresh was the only recovery for the rows.

- `lazyPagingColumnError(exception, onRetry)` now shows a filled `Button` labelled with the shared `retry_action` ("Retry" / "Reintentar") beneath the error, in a centred column with medium spacing, as `lazyPagingConcatenateError` does. `onRetry` is required.
- `RefreshableLazyPagingColumn`'s default error content passes `lazyPagingItems::refresh`. History (`FinishedBetList`) and Timeline (`BetTimelineList`) pass their rows' `refresh` in their error content, so Retry reloads only the rows. The page errors keep `retry()`.
- Full-list Retry uses `LazyPagingItems.refresh()`, as on iOS. A throwaway instrumented test checked whether Paging 3 has the iOS problem: with endless pages, a failed pull to refresh over loaded rows, then `retry()`, the list was requested again and the rows returned. Paging 3 does not show the iOS behaviour, so no test of `retry()` was kept. A test that Retry recovers after a failed pull over loaded rows is kept (below).
- Screens that render the shared error, confirmed in code:
  - through the default: Scores (`GamblerScoreList`), match bettors (`MatchBetList`), Pool from layout (`PoolFromLayoutCreatorList`), Bets (`PendingBetList`), and Manage gamblers (`ManageGamblersList`). Bets and Manage gamblers each had a private copy of the shared error without an action; both copies were removed so the screens use the default. Manage gamblers' copy added 16 dp of padding around the error item, which is gone.
  - explicitly: History and Timeline.
  - My pools (`PoolScoreList`) keeps its own error with its existing Retry; that Retry now calls `refresh()` instead of `retry()`.
- Tests (instrumented, Compose):
  - `LazyPagingColumnErrorRetryTest` (ui, 3): the button is labelled "Retry", has the Button role, and runs its action once. Through the default, after a failed first load Retry requests the list exactly once more and the rows appear, and a later pull to refresh requests it again. After a failed pull to refresh over loaded rows, Retry requests the list once more and the rows return.
  - `HistoryListRetryTest` (bet, 2), for History and Timeline: with the total loaded and the rows failed, Retry requests the rows once (2 loads in total), never calls the summary retry or the pull's total refresh, and keeps the total visible. A later pull to refresh requests the rows once more and the total once.
  - `PoolScoreListInsetsTest.aFailedFirstLoadScrollsToItsRetryActionAndRetryingRecoversTheList` was adjusted: its paging source factory returned the same `PagingSource` instance, which `refresh()` rejects ("An instance of PagingSource was re-used"). It now creates a source per generation; the assertions (2 loads, rows shown) are unchanged.
  - `RefreshableLazyPagingColumnInsetsTest` and `BetTimelineListTest` were updated for the new signature and the list's Retry.

### 5.2 Summary retry as an inline icon button

- Android asset: `Android/ui/src/main/res/drawable/refresh.xml`, derived from `assets/icons/refresh.svg` as `add.xml` was from `add.svg`: path data copied unchanged (checked by comparing the two strings), a 960 × 960 viewport and `translateY="960"` for the SVG's viewBox. `assets/icons/README.md` lists it.
- `HistoryPointsHeader` shows the initial-failure and refresh-failure text in a row with `HistoryPointsRetryButton` after it, 4 dp apart. The text takes the remaining width and wraps; the button stays beside it. The button is a borderless Material 3 `IconButton` (48 × 48 dp touch target) tinted with the theme's primary green, showing the 24 dp refresh icon with the content description `retry_action` ("Retry" / "Reintentar"). It calls only the summary retry.
- Icon contrast against the screen background: 5.13:1 in light (#2E7D32 on #FFFFFF) and 6.74:1 in dark (#4CAF50 on #121212).
- `HistoryPointsRetryTest` (bet, 6): for History and Timeline, each with an initial and a refresh failure, the retry has the content description "Retry" and the Button role, a 48 × 48 dp touch area, no visible "Retry" text, starts after the failure text and overlaps its line, and runs the summary retry exactly once. The refresh failures keep the last total. The Spanish name is "Reintentar", and a current total shows no retry. The summary retry in `HistoryPresentationTest` and `BetTimelineListTest` is now found by its content description.

### 5.3 Builds, tests, and emulator checks

Commands (from the brief; `ANDROID_SERIAL=emulator-5556` for instrumented runs):

- `./gradlew :ui:testDebugUnitTest :bet:testDebugUnitTest :pool:testDebugUnitTest :app:testProdDebugUnitTest :app:compileProdDebugAndroidTestKotlin`: passed (ui 44, bet 29, pool 2, app 60 tests). A first run with the failure switch in place failed two `CursorPagingSourceTest` cases on the unmocked `Log.d`; the switch was removed for every test run.
- `:ui:connectedDebugAndroidTest`: 81 passed, then the three paging test classes again after the throwaway test was removed: 10 passed.
- `:bet:connectedDebugAndroidTest`: 39 run, 36 passed; the 3 failures are the known `BetTextFieldTest` cases ("Failed to perform text input").
- `:pool:connectedDebugAndroidTest`: 37 passed (after the source fix above).
- `:app:assembleProdDebug` succeeded with and without the switch.

On the emulator, through ARTEMIS Flash for each Retry and summary-icon tap (traces `7cf7364d-…`, `95efa6aa-…`, `8c4385b4-…`, `43ddee23-…`, `767ef3ed-…`, `18dda83a-…`, `047e2e01-…`, `5d9a4469-…`, `097d1141-…`, `7d9b6496-…`, `bc76eea8-…`, `fba885aa-…`, `865ce5f7-…`, `65834293-…`, `90d1a02f-…`). Request counts come from the watchdog log:

| Screen | Failure | Recovery | Requests after the tap | Captures |
|---|---|---|---|---|
| My pools | first load | Retry | 1 list request; pools shown | `my-pools-list-error-retry` |
| Timeline (El mono, from Scores) | rows and total | Retry | 1 row request, no total request; summary still "Couldn't load points." | `timeline-list-error-and-summary-initial-failure`, `timeline-after-list-retry-summary-still-failed` |
| Timeline | total (initial) | summary icon | 1 total request; "+676 pts earned", rows kept | `timeline-summary-retry-total-only` |
| Timeline | total (refresh) | pull to refresh | 1 row and 1 total request; "+676 pts", "Couldn't update points. Showing the last total." with the icon | `timeline-summary-refresh-failure-icon-retry` |
| History | total (initial) | — | rows loaded; "Couldn't load your points." with the icon | `history-summary-initial-failure-icon-retry` |
| History | total | summary icon (adb tap) | 1 total request | — |
| History | rows, by a pull with the total succeeding | Retry | 1 row request, no total request; "+589 pts earned" kept | `history-list-error-total-kept`, `history-after-list-retry-total-not-reloaded` |
| History | total (refresh) | pull to refresh | 1 row and 1 total request; last total with "Couldn't update your points. Showing the last total." and the icon | `history-summary-refresh-failure-icon-retry` |
| Scores | failed pull over loaded rows | Retry | 1 list request; leaderboard shown | `scores-list-error-retry`, `scores-after-retry` |
| Bets | failed pull | Retry | 1 list request; empty state (no open match) | `bets-list-error-retry`, `bets-after-retry` |
| Match bettors (España vs Argentina, from History) | first load | Retry | 1 list request; bettors shown | `match-bettors-list-error-retry`, `match-bettors-after-retry` |
| Manage gamblers ("Prueba") | first load | Retry | 1 list request; 3 members | `manage-gamblers-list-error-retry` (no capture of the loaded list, which shows email addresses; no member was removed) |
| Pool from layout | first load | Retry | 1 list request; one template | `pool-from-layout-list-error-retry`, `pool-from-layout-after-retry` (nothing was created) |

No recovery issued a second list request, and nothing was requested afterwards until the next action.

- Font scale 2.0 in dark mode, Timeline (El mono, opened from the match bettors): the list error's Retry ends at y = 2198 px, above the gesture handle, and is reachable without scrolling (`timeline-list-error-font-2x-dark-top`). Retry loaded the rows with the total kept (`timeline-after-list-retry-font-2x-dark`). The refresh-failure text wraps to two lines with the icon beside it (`timeline-refresh-failure-font-2x-dark`). The icon keeps its 24 dp size at 2.0, as Material icons do; the text scales.
- Light mode is shown by the other captures.
- Edge-to-edge: on every screen above, the Retry button and the summary icon lay inside the window above the 24 dp gesture area, and the Timeline and History headers stayed below the app bar.
- TalkBack-facing names, from the accessibility node tree (`uiautomator dump`): the list button is a clickable node with the text "Retry"; the summary icon is a clickable 126 × 126 px node (48 dp) whose child carries the content description "Retry", which TalkBack reads on the clickable node. The Compose tests above confirm the merged node has the Button role. TalkBack speech was not run.

Settings changed and restored on emulator-5554: dark mode (`cmd uimode night yes`, back to `no`) and font scale (2.0, back to 1.0).

Reverting the temporary edits: `CursorPagingSource.kt` 61c9e715… and `GetPoolGamblerScore.kt` 3bb2ffd0… match their hashes from before the change (`shasum -c`), and `git status` shows neither as modified. The real build was rebuilt and reinstalled on emulator-5554 (`adb install -r`, still signed in); none of its 31 dex files contains `debug-fail` or `DebugFail`. The marker files were deleted. Pixel_7_API_36_no_account is shut down.

### Not verified

- **TalkBack speech** for the two Retry controls: names checked through the accessibility node tree and Compose semantics tests only.
- **Galaxy S8+ (API 28, es-CO)**: not used; Spanish names are covered by `HistoryPointsRetryTest.theRetryIsNamedInSpanish` and the shared `retry_action` translations.
- **Avatar requests during recovery**: not counted (no avatar logging was added). The API request counts above do not include them.
- **My pools' templates section** (empty state, no pools): its own template error has no Retry on Android, while iOS's uses the shared error with Retry. It could not be staged with this account, and it was not changed.

Observed, not changed: the Manage gamblers app-bar back button has no accessible name (uiautomator reports it as `NAF="true"`, no content description).

Accepted without TalkBack speech, the Galaxy S8+ (API 28), avatar request counts during Retry, or a staged My pools templates error by the user on 2026-10-04.

### Follow-up: My pools templates Retry (2026-10-04)

When the gambler has no pools, My pools shows popular templates. On Android, a failed templates load showed only the error, while iOS shows the shared list error with Retry.

- `PoolScoreList`'s templates section now shows a Retry button (shared `retry_action`, "Retry" / "Reintentar") beneath its error. The button calls `lazyPoolLayouts.refresh()`, so it reloads only the templates. The shared `lazyPagingColumnError` was not used, because it fills the whole list viewport and this error sits inside the empty state below its heading. The section keeps its inline error, now in a centred column with the same Failure, button style, and spacing as the shared error.
- `PoolScoreTemplatesRetryTest` (pool, instrumented): with no pools and a first templates load that fails, Retry is shown and clickable. Activating it loads the templates and shows them, after exactly 2 template loads in total (the failed one and the retry).
- `ANDROID_SERIAL=emulator-5556 ./gradlew :pool:connectedDebugAndroidTest` on `Pixel_7_API_36_no_account`: 38 passed. `:pool:testDebugUnitTest` and `:pool:compileDebugAndroidTestKotlin` passed, and `:app:assembleProdDebug` succeeded. medium_phone was shut down while the tests ran. Afterwards it was restarted with the rebuilt real build reinstalled, still signed in.
- Not verified on a device: the account has pools, so My pools never shows the templates section. No temporary edits were made for this follow-up.

## Full-list state centering — tasks 6.1–6.2 (2026-10-04)

Environment: iOS Debug builds of the `Tyche` scheme from `iOS/Tyche.xcworkspace` on `iPhone 16 Pro (iOS 18.1)` A2F5EF6F-BA25-4035-9494-F6ACE0144047; Android `:app:assembleProdDebug` on the `medium_phone` AVD (`emulator-5554`, API 36, 1080 × 2400, gesture navigation). Both against the production backend, read-only, already signed in. No sign-in, sign-out, or production write. Captures are in `verification/` with the prefixes `ios-6.1-` and `android-6.2-`; none shows another member's email address.

Failure and empty states were staged with the same temporary, debug-only switches as in 3.3 and 5.3, plus an empty-list marker: `LazyPagingCursorSource.load` (iOS) and `CursorPagingSource.load` (Android) failed the first page while `debug-fail-lists` existed and returned an empty last page while `debug-empty-lists` existed; `GetPoolGamblerScoreUseCase.execute` / `GetPoolGamblerScore.execute` failed while `debug-fail-totals` existed. Request watchdogs (iOS: CFNetwork task summaries and the debug lines from `log stream`; Android: Ktor request paths without query strings and the debug lines from logcat) would have terminated the app above 40 events in 3 seconds. Neither fired.

### 6.1 iOS

- `LazyPagingVStackError` and `LazyPagingVStackEmpty` take an optional `header`: list content that stays above the state. A new internal `FullListStateLayout` stacks the header and gives the state the rest of the existing minimum height (the root geometry's height minus its safe-area insets), centering the state there. The minimum is a minimum: when the header and the state's ideal height exceed it, the layout grows and the list scrolls. Without a header (the default initializers), the state fills the same minimum as before.
- The remaining height comes from the header's measured height in the same layout pass, so it does not depend on the scroll position. iOS 16 has no scroll-content coordinate space or scroll-geometry API, so the header is passed into the state view rather than measured as a separate row.
- History (`FinishedBetList`) and Timeline (`BetTimelineList`) pass their identity and points header into the error and empty states instead of rendering it as a separate row before them.
- Centered text: `ErrorView` centers its failure reason and recovery suggestion (the title was already centered); `MessageView` centers its message.
- The paging stacks' default-content constraints now name `LazyPagingVStackError<EmptyView>` and `LazyPagingVStackEmpty<EmptyView>`.
- Tests (Swift Testing): `FullListStateLayoutTests` (UITests, 5): without a header the layout fills the minimum; a header takes its height from the state so the total stays the minimum; a state taller than the space left grows the layout; a header taller than the minimum leaves the state its ideal height; the state is centered in the space below the header. `LazyPagingVStackErrorTests` finds `LazyPagingVStackError<EmptyView>`; the History and Timeline Retry tests find the list's Retry by its "Retry" label, because the error now contains the header.

Simulator captures (light, Large text unless noted):

| State | Capture | Observation |
|---|---|---|
| Timeline (El mono), rows and total failed | `ios-6.1-timeline-list-and-summary-initial-failure-ios18.png` | Same state as `ios-3.3-timeline-list-and-summary-initial-failure-ios18.png`. The error block moved up about 56 pt; its center is about 12 pt above the middle of the space between "Couldn't load points." and the home indicator. All description lines are centered. |
| Timeline, AX5 | `ios-6.1-timeline-list-error-ax5-top-ios18.png`, `-ax5-scrolled-to-retry-` | The error starts right below the header and the list scrolls; Retry is reachable at the end. |
| Timeline, empty rows, total loaded | `ios-6.1-timeline-empty-total-kept-ios18.png` | "+676 pts earned" stays above "Nothing to show"; no zero total. |
| History, list failed, total kept | `ios-6.1-history-list-error-total-kept-ios18.png` | Compared with `ios-3.3-history-list-error-total-kept-ios18.png`, the block moved up about 39 pt (half the header height, as expected). |
| History, empty rows, total loaded | `ios-6.1-history-empty-total-kept-ios18.png`, AX5: `ios-6.1-history-empty-ax5-ios18.png` | Message below the total; at AX5 it still fits below the wrapped total. |
| Scores, list failed (no header) | `ios-6.1-scores-list-error-unchanged-ios18.png` | Icon, title, and Retry at the same positions as `ios-3.3-scores-list-error-retry-ios18.png`; only the description lines are now centered. |

On History and Scores the state sits about 35 pt below the middle of the visible space, because the existing minimum height is taken from the root geometry, which includes the tab bar. That estimate is unchanged here (header-less screens must look as before); it is an open question for the user.

Builds and tests:

- `xcodebuild build -scheme Tyche -workspace iOS/Tyche.xcworkspace -destination 'id=A2F5EF6F-…'`: succeeded with and without the temporary switch.
- `xcodebuild test … -destination 'id=<throwaway iPhone 16 Pro, iOS 18.1>' -only-testing:UITests -only-testing:BetTests -only-testing:PoolTests -parallel-testing-enabled NO`: passed (Swift Testing runs of 53, 32, and 91 tests, plus 12 XCTest cases). The throwaway simulator was deleted. No `Package.resolved` changed (hashes compared before and after).

### 6.2 Android

- `ViewportFillingItem` (a composable used inside `item { }`) is replaced by `LazyListScope.viewportFillingItem { }`, which declares the item itself with a fixed key. The item measures the padded viewport height (`fillParentMaxHeight`, as before) and subtracts the height of the items before it, read from the list's `LazyListState.layoutInfo` as the item's offset from the first item. That offset does not change while the list scrolls; once the first item has scrolled out, the content is already taller than the viewport and no minimum applies.
- The list state reaches the item through an internal composition local that `RefreshableLazyPagingColumn` provides (not for a reversed layout). In any other list the item assumes nothing is above it, so the previews keep their old look.
- The height is a minimum: content taller than it makes the item taller and the list scrolls. The item no longer scrolls internally (`CenteredScrollableColumn` is no longer used here; the match screen still uses it).
- Callers migrated: `lazyPagingColumnError`, `lazyPagingColumnEmpty`, Bets' empty state (`PendingBetList`), and My pools' list error (`PoolScoreList`, whose 16 dp padding moved onto its content column). History and Timeline keep their header items and need no change.
- Centered text: `ExceptionView` centers its failure-reason text (the title and the empty message were already centered).
- Tests (instrumented, Compose), `RefreshableLazyPagingColumnInsetsTest`: an empty state below a 120 dp header is centered in the padded space left below it, with the header at the top padding; the shared empty state below a header fits between the header and the bottom padding; a state taller than the space below a header grows the list, and scrolled to its end Retry lies within the padded area. The two existing tall-state tests (`aTallErrorStateScrollsItsRecoveryActionIntoTheSafeArea` here and `PoolScoreListInsetsTest.aFailedFirstLoadScrollsToItsRetryActionAndRetryingRecoversTheList`) failed with `performScrollTo`: the list's semantic scroll-to stops Retry at the viewport edge (480 dp, inside the 48 dp bottom padding), whereas the old inner scroll stopped it at the padded edge. They now scroll the list to its end, as a swipe or a screen reader's scroll forward does, and pass with the same bounds assertions.

Emulator captures (through ARTEMIS Flash, traces `765509e6-…`, `db69d31b-…`, `70775b3c-…`, `3546e2ce-…`, `d7effc17-…`, `8ac719b1-…`, `b25bc53d-…`, `2884cec3-…`; screenshots with `adb exec-out screencap`):

| State | Capture | Observation |
|---|---|---|
| Timeline (El mono, from Scores), rows and total failed | `android-6.2-timeline-list-error-and-summary-initial-failure-api36.png` | Same state as `android-5.3-timeline-list-error-and-summary-initial-failure-api36.png`. The error block moved up about 176 px (67 dp) and is centered in the space between the points row and the gesture area. The description wraps over three centered lines. |
| Timeline, font scale 2.0 | `android-6.2-timeline-list-error-font-2x-api36.png` | Fits without scrolling; the Retry label ends at y = 1913 px. |
| Timeline, font scale 2.0, landscape | `android-6.2-timeline-list-error-font-2x-landscape-top-api36.png`, `-scrolled-to-retry-` | The error does not fit; the list scrolls and Retry is fully visible above the gesture handle. |
| Timeline, empty rows, total loaded | `android-6.2-timeline-empty-total-kept-api36.png` | "+676 pts earned" stays above "Nothing to show". |
| History, rows failed, total kept | `android-6.2-history-list-error-total-kept-api36.png` | Centered between "+589 pts earned" and the bottom bar. |
| History, empty rows, total loaded | `android-6.2-history-empty-total-kept-api36.png` | One pull: 1 row load and 1 total request. |
| Scores, failed pull (no header) | `android-6.2-scores-list-error-unchanged-api36.png` | Icon, title, and Retry at the same pixels as `android-5.3-scores-list-error-retry-api36.png`; only the description is now centered. |
| Bets, empty (no header) | `android-6.2-bets-empty-unchanged-api36.png` | Centered as before. |

Builds and tests:

- `./gradlew :ui:testDebugUnitTest :bet:testDebugUnitTest :pool:testDebugUnitTest`: passed.
- `ANDROID_SERIAL=emulator-5556` on `Pixel_7_API_36_no_account`, medium_phone shut down: `:ui:connectedDebugAndroidTest` 83 passed; `:pool:connectedDebugAndroidTest` 38 passed (both after the test change above); `:bet:connectedDebugAndroidTest` 39 run, 36 passed, the 3 failures are the known `BetTextFieldTest` cases.
- `:app:assembleProdDebug` succeeded with and without the switch.

Screens whose shared text is now centered: every screen that shows `ErrorView` (iOS) or `ExceptionView` (Android): all shared list errors (Scores, Bets, History, Timeline, match bettors, Pool from layout, Manage gamblers, My pools and its templates section), the pool drawer's score error, the match screen's error, the email-link sign-in error, and the pool join errors (including "sign in required"). The iOS empty message (`MessageView`) is used only by the shared empty state.

Settings changed and restored: Android font scale (2.0, back to 1.0) and rotation (landscape with auto-rotate off, back to portrait with auto-rotate on); iOS content size (AX5, back to Large). Appearance was not changed.

Reverting the temporary edits: `LazyPagingCursorSource.swift` 7da83ddc…, `GetPoolGamblerScoreUseCase.swift` 0e357c69…, `CursorPagingSource.kt` 61c9e715…, and `GetPoolGamblerScore.kt` 3bb2ffd0… match their hashes from before (`shasum -c`). The real builds were rebuilt and reinstalled: on emulator-5554 none of the APK's 31 dex files contains the debug strings; the iOS app bundle contains none. The marker files were deleted. The iOS simulator is shut down; medium_phone was restarted with the real build.

### Not verified

- **Dark appearance** was not captured; the change affects layout and text alignment only.
- **TalkBack and VoiceOver** were not run; no semantics changed. On Android, a screen reader's scroll-to of a Retry taller than the space left now stops it at the viewport edge rather than the padded edge (see the test change above).
- **iOS 26+ and iPad**: only the iOS 18.1 simulator was used.

Accepted without dark mode, VoiceOver/TalkBack, or iOS 26+/iPad checks of the full-list state centering by the user on 2026-10-04.
Accepted the Android scroll-to change (a Retry taller than the remaining space stops at the viewport edge) by the user on 2026-10-04.

### Follow-up: iOS visible-height centering (2026-10-04)

The user asked to remove the remaining iOS offset in this change. The full-list error and empty states now take their minimum height from the list's own visible height instead of the root geometry minus its safe areas.

- `fullListStateViewport(contentInsets:)` (new, in `FullListStateLayout.swift`) is applied to each paging stack. A background `GeometryReader` measures the scroll view's height excluding the safe areas it extends under: the navigation bar, the tab bar or the home indicator. The modifier stores that height and passes it to the states, less the stack's top and bottom content insets. It works on iOS 16; there are no scroll-geometry APIs. `FullListStateLayout`, the header handling, and the minimum-only behavior are unchanged.
- Applied to all eight paging lists: Scores (`GamblerScoreList`), Bets (`PendingBetList`), History (`FinishedBetList`), Timeline (`BetTimelineList`), match bettors (`MatchBetList`), Pool from layout (`PoolFromLayoutCreatorList`), Manage gamblers (`ManageGamblersList`), and My pools (`PoolScoreList`). Lists with content insets pass the same value they give the stack. My pools' list error kept only its horizontal 8 pt padding, because vertical padding around the state made the list 16 pt taller than the visible height.
- A list that does not apply the modifier gets no minimum: the state takes its natural height. The root `parentSize` environment, which `withParentGeometryProxy()` still sets in `PoolHomeRouter`, `PoolScoreListRouter`, and a `PoolScoreList` preview, no longer has a reader.
- A first version passed the height through a preference key. In the app the preference reported only its initial zero, so the states were top-aligned (seen on My pools and Scores). The height is now stored from the geometry reader's `onAppear` and `onChange(of:)`.
- Tests: `FullListStateLayoutTests` gained two tests (7 in total). In a 700-point window, a scroll view with 8-point content insets has a 100-point bottom safe-area inset standing in for a tab bar. Its header and 100-point state lay out with the state starting at 300 pt: the visible 600 pt less the insets leaves the state's area from 108 to 592 pt. The second test checks that, without a measured viewport, the state takes its ideal height.

Measured offsets, iPhone 16 Pro (iOS 18.1), Large text, light. Values are the vertical center of the state's visible block (sad face or search icon down to Retry or message), in points. They are compared with the middle of the visible empty area: from the list's top edge (152.3 pt, below the large title) or from the bottom of the header's visible text, down to the tab bar's top edge (790.7 pt) or the home-indicator edge (840 pt) where the tab bar is hidden. Positive means below the middle.

| Screen and state | Capture | Before (6.1) | After |
|---|---|---|---|
| Scores, list error (no header, tab bar) | `ios-6.1b-scores-list-error-ios18.png` | +32.3 | +5.3 |
| Bets, list error (no header, tab bar) | `ios-6.1b-bets-list-error-ios18.png` | — | +2.7 |
| History, list error with "+589 pts earned" kept (tab bar) | `ios-6.1b-history-list-error-total-kept-ios18.png` | +38.5 | +8.8 |
| Timeline (El mono), rows and total failed (no tab bar) | `ios-6.1b-timeline-list-and-summary-initial-failure-ios18.png` | −7.2 | +13.7 |
| Timeline, empty rows with "+676 pts earned" kept | `ios-6.1b-timeline-empty-total-kept-ios18.png` | −12.9 | +7.9 |

On History and Timeline the remaining offset is the header's own space below its visible text: 16 pt of bottom padding, and on Timeline also the lower half of the 44-point summary retry target. Measured from the bottom of the header's frame instead, every state is within 3 pt of the middle (Timeline error +2.4, Timeline empty about +2, History about +3). The few points left on Scores, Bets, and the header frames come from the block's own asymmetry (icon and button) and pixel measurement.

- Largest text (AX5), Timeline error: the state starts right below the header and the list scrolls. Scrolled to the end, Retry is fully visible above the home indicator (`ios-6.1b-timeline-list-error-ax5-scrolled-to-retry-ios18.png`).
- The request watchdog ran throughout and never fired. The leaderboard captured mid-session shows other members' email addresses and was not saved. A capture that showed the open pool drawer was deleted. The drawer had been opened by a delayed tap on the Scores avatar; nothing in it was tapped, and the app was terminated to close it.

Builds and tests:

- `xcodebuild build` of `Tyche` for `iPhone 16 Pro (iOS 18.1)`: succeeded with and without the temporary switch.
- `xcodebuild test … -only-testing:UITests -only-testing:BetTests -only-testing:PoolTests -parallel-testing-enabled NO` on a throwaway iPhone 16 Pro (iOS 18.1): passed (Swift Testing runs of 53, 32, and 93 tests; the XCTest cases had 0 failures). The throwaway simulator was deleted.
- No `Package.resolved` changed (hashes compared before and after).

Settings changed and restored: content size (AX5, back to Large). Reverting the temporary edits: `LazyPagingCursorSource.swift` 7da83ddc… and `GetPoolGamblerScoreUseCase.swift` 0e357c69… match their hashes from before (`shasum -c`). Temporary diagnostic logging in `FullListStateLayout.swift` and `LazyPagingVStackError.swift` was removed (no `DebugVP` or `DEBUG-TEMP` left in the sources). The real build was rebuilt and installed and contains none of the debug strings. The marker files were deleted and the simulator is shut down. Android was not changed.

Not verified: dark appearance, VoiceOver, iOS 26+ and iPad. The user accepted these.
