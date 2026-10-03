# Verification

## iOS — tasks 1.1–1.7 (2026-10-03)

### Devices and builds

| Device | Use | State |
|---|---|---|
| Samsung Galaxy S8+ SM-G955F, serial `ce0417143a6ab8130c`, Android 9 (API 28), es-CO, `com.felipearpa.fortuna` (prod, signed in) | ARTEMIS prerequisite exploration (read-only) | `mobile_diagnose` verdict `ready` (5/5 required checks); ARTEMIS accessibility helper v6 |
| iPhone 16 Pro (iOS 18.1) `A2F5EF6F-BA25-4035-9494-F6ACE0144047` | Baseline and redesigned History, light/dark, AX5 text, Spanish, refresh, append, row activation, timeline | Signed in (Felipe's account), production backend |
| iPad Air 11-inch (M2) iOS 18.1 `F45A74E8-D2F2-4DF8-B5D3-DFFD4144E27D` | Wide window (820 pt portrait), light/dark | Signed in |
| tmp-drawer-iPhone17-26.5 (iOS 26.5) `48AD28C3-2C4E-406C-95FD-46563B66EA63` | iOS 26+ check | **Not signed in** — the app opens on the Fortuna sign-in screen; nothing observed beyond it |

Builds: scheme `Tyche` (production, not `Tyche Local`) through `iOS/Tyche.xcworkspace`, Xcode 27.0 (27A266a), Debug. History is read-only; no bet, score, or account data was written.

### ARTEMIS exploration (Android, reusable by task 2.1)

Trace `529e0b89-32fc-4652-8b58-90b90e163a3f` (ARTEMIS Pro, `verification_level=final`, 5/5 checks passed, 37 steps, ~10 min). Coordinates are ARTEMIS normalized `[x, y]` on a 0–1000 grid. Captures: `verification/android-baseline-*.jpg`.

| Step | Interaction (verified) | Observation |
|---|---|---|
| Open pool | Tap pool row `Copa Mundial de la FIFA 2026, Puesto 8, 589 puntos, 13 jugadores, Puesto sin cambios` at `[500, 154]` | Pool home: compact top app bar with avatar (left, `[70, 66]`), the selected tab's title, and the ⇄ change-pool icon (right, `[930, 66]`). Tabs are a **bottom** bar: `Puntajes` `[167, 910]` → `Apuestas` `[500, 910]` → `Historia` `[833, 910]`. |
| History | Tap `Historia` `[832, 910]`, wait ~2.5 s | Title `Historia`. Each match: date `DD/MM/YY` (e.g. `19/07/26`) above a time `2:00 p. m.`, then home and away rows (flag, name, bold final score, lighter prediction), and the award `+0` / `+2` at the bottom right; dividers between matches. First rows: España 0–0 Argentina (bet 3–2, +0); Francia 4–6 Inglaterra (bet 3–1, +2); Inglaterra 1–2 Argentina (bet 2–1, +2). |
| Pull to refresh | Swipe `[500, 250]` → `[500, 750]` over 800 ms | Material circular refresh indicator at the top; rows stay visible, no skeletons. |
| Pagination | 14 upward swipes (`[600, 700]` → `[600, 300]`, 800 ms; faster flicks `[500, 850]` → `[500, 150]`, 200–400 ms) | ~104 rows (11/06/26–19/07/26). No append placeholder or spinner was seen; pages arrived before the end was reached. Last row: México 2–0 Sudáfrica, 11/06/26 2:00 p. m., bet 2–0, +10. Returning to the top: switch to `Puntajes` and back to `Historia`. |
| Match activation | Tap first row (España vs Argentina) at `[500, 130]` | Destination `Marcador` (back arrow `[69, 66]`, home icon `[930, 66]`). **First open showed `Error inesperado` with `Reintentar`** (tapped at `[500, 630]`), after which the match header (`España 0 - Argentina 0`, `19/07/26, 2:00 p. m.`) and every gambler's bet/award loaded. This is pre-existing behavior outside this change. Back with the toolbar arrow, not system BACK. |
| Another gambler's timeline | `Puntajes` `[167, 910]` → tap `El mono` (rank 1) at `[500, 90]` | Destination `Línea de tiempo` with the gambler's name as a large heading; rows grouped under date headers with time, two team rows (flag, name, bold score, lighter prediction), and award. Back with the arrow `[69, 66]`. |

Hazards observed: BACK on pool home exits the app; never tap `Cerrar sesión`. The run stayed signed in and wrote nothing.

### iOS baseline (before any code change)

Same account and pool on the iPhone 16 Pro (iOS 18.1). Interaction path (points; taps right after a launch or a scroll are sometimes swallowed, so repeat once): pool row ≈ (180, 188); tabs at y ≈ 832 (Scores 115, Bets 201, History 334); first History row ≈ (200, 320); match screen's home button ≈ (373, 79); tap the status bar to scroll to the top.

- `ios18-baseline-history-light.png`: native large title `History`, avatar and ⇄ toolbar items, rows grouped under `19/07/26`-style headers, scores and bets in two columns, `+0` award.
- `ios18-baseline-history-sticky-header.png`: the pinned date header overlaps the previous row's award while scrolling.
- `ios18-baseline-history-refreshing.png`: native refresh control; rows stay visible.
- `ios18-baseline-history-no-prediction.png`: Estados Unidos 4–1 Paraguay has no bet and shows `+0`.
- `ios18-baseline-history-last-row.png`: list ends at México–Sudáfrica above the tab bar.
- `ios18-baseline-match-destination.png`: row activation opens `Score` with a `History` back button.
- `ios18-baseline-timeline.png`: another gambler's `Timeline` with date headers and `FinishedBetItem` rows.

### What changed (iOS)

- `Bet` depends on `DataPool`; `FinishedBetListViewModel` takes `GetPoolGamblerScoreUseCase` (resolved from the existing `PoolAssembly` registration in `PoolHomeView`; the PoolHome preview assembler registers a fake). No new API contract, repository, or Pool presentation dependency.
- History-owned total (`HistoryPointsSummaryState`): loads on entry, on pull to refresh, and on summary retry; keeps confirmed points during a refresh and after a failed one; discards responses from superseded requests. `PoolHomeView` keys History on pool + gambler so another context gets a new view model.
- `HistoryPointsSummary` (production component with `historyPointsPlaceholderModel` + `isPlaceholder`) heads every list state (loading, loaded, empty, error); `HistoryPointsHeader` adds the compact error/retry and refresh status.
- History uses the plain `LazyPagingVStack` with its own pull to refresh, which awaits the pager refresh and the total together. No shared paging container changed. Append failures now show the app's existing `LazyPagingVStackConcatenateError` retry (History previously showed nothing).
- `HistoryBetItem` replaces History's sticky date groups: per-row date/time, flags and names around a large centered result, `Your bet` and an award pill. Rows are `Button`s with `InteractiveRowButtonStyle` and the existing `invokeMatchOpen`. `FinishedBetItem` and the timeline are untouched.
- English/Spanish (`es`, `es-ES`) strings with plural variants for visible and spoken points.

### Results

| Check | Result | Evidence |
|---|---|---|
| App builds (iOS 18.1 and 26.5 simulators), previews compile | Pass | `xcodebuild build -scheme Tyche -workspace iOS/Tyche.xcworkspace` → BUILD SUCCEEDED (Debug compiles every `#Preview`) |
| Focused tests | Pass, 27/27 (21 new) | `xcodebuild test -scheme Bet -parallel-testing-enabled NO` on the iOS 18.1 iPhone, from `iOS/Bet` |
| Total for the current pool, not summed from rows | Pass | `ios18-after-history-light.png` shows `+589 pts earned`, matching the pool list's 589 pts while only the first page is loaded |
| Context change shows the new pool's total | Pass | ⇄ → `Prueba` → History: `+4 pts ganados` (`ios18-after-history-other-pool-es.png`) |
| Positive / zero / no-bet rows | Pass | green `+2 pts` pills, neutral `0 pts`, `No bet` + `0 pts` for Estados Unidos–Paraguay (`ios18-after-history-last-row-no-bet.png`), `Sin apuesta` in Spanish |
| Reference-like layout, light and dark | Pass | `ios18-after-history-light.png`, `ios18-after-history-dark.png`, compared with `reference/history-reference.png`; the native large title and tab bar are unchanged |
| Pull to refresh keeps rows and total visible | Pass | `ios18-after-history-refreshing.png` |
| Append | Pass | Scrolling loaded rows down to 11 Jun (all ~104 matches, three pages); the total stayed `+589` |
| Last row reachable above the tab bar | Pass | `ios18-after-history-last-row-no-bet.png` |
| Row activation reaches the existing destination once | Pass | `ios18-after-row-activation-match.png` (`Score`, back button `History`) |
| Timeline, Scores, Bets unchanged | Pass | `ios18-after-timeline-unchanged.png`, `ios18-after-scores-tab-light.png`, `ios18-after-bets-tab-dark-es.png`, `ipad18-after-scores-tab-light.png` |
| Long names wrap | Pass | `República Democrática del Congo`, `Bosnia y Herzegovina` wrap within their columns (`ios18-after-history-appended-long-names.png`) |
| Largest text size (AX5) | Pass | Teams stack with the result between them; date/time and bet/pill reflow vertically (`ios18-after-history-dark-ax5*.png`, captured before flags were capped at 52 pt) |
| Spanish | Pass | `Historial`, `ganados`, `Tu apuesta`, `domingo, 19 de jul.`, `2:00 p.m.` (`ios18-after-history-dark-es.png`) |
| Wide window | Pass | iPad 820 pt portrait: content in a 640 pt column under the large title (`ipad18-after-history-portrait-*.png`) |
| Contrast (measured) | Pass | secondary text 5.9:1 light / 7.7:1 dark; green value 6.6:1 / 12.5:1; green pill text 5.5:1 / 7.3:1; neutral pill text 5.6:1 / 6.4:1 |

### Not observed on screen, and the tests that cover it

Real data and the available simulators cannot stage these:

- **Summary-only failure, summary/list partial failure, retry independence**: the production endpoints did not fail. Covered by `aFailedFirstLoadShowsTheErrorAndRetryShowsThePlaceholderAgain`, `aRefreshKeepsTheTotalVisibleAndAFailedRefreshKeepsItWithTheFailure`, `summaryRetryDoesNotReloadTheRows`, `aFailedTotalDuringPullToRefreshKeepsTheRefreshedRows` (`FinishedBetListViewModelPointsSummaryTests`). Illustrated by the `Summary failed, rows loaded` and `Total loaded, rows failed` previews in `FinishedBetList.swift`.
- **Out-of-order completion**: `anOlderResponseFinishingLastDoesNotReplaceTheNewerOne`, `anOlderFailureFinishingLastDoesNotMarkTheNewerTotalAsFailed`.
- **Context change while a request is pending**: `eachContextRequestsItsOwnTotalAndNeverShowsAnotherContextsTotal` (the on-screen pool switch above was not mid-request).
- **Append never reloads the total; refresh feedback lasts for the slower request**: `appendingAPageNeitherRequestsNorChangesTheTotal`, `pullToRefreshReloadsBothAndWaitsForTheSlowerTotal`.
- **Unavailable total, unavailable result/award**: no real match lacks them. Covered by `aTotalMissingFromASuccessfulResponseReplacesThePreviousNumber`, `unavailableValuesAreDescribedRatherThanReadAsZero`, `positivePointsAreSignedAndZeroOrUnavailableNeverShowAPlus`; preview `Loaded and placeholder` in `HistoryBetItem.swift`.
- **Initial loading placeholders and Reduce Motion**: responses arrived before a capture could be taken. The rows and summary use the module's existing `PulsingPlaceholderContent`, which holds the static midpoint under Reduce Motion; placeholders announce nothing (`placeholdersAnnounceNothing`). Previews `Loading` and `Positive, zero, unavailable`.
- **Append placeholder row and append-failure retry**: pages loaded before the end was reached; the endpoint did not fail.
- **VoiceOver**: not run on a simulator. The exact announcements are asserted by `aRowAnnouncesDateResultBetAndPointsInOrder`, `theSummaryAnnouncesCurrentPoolPointsWithSingularAndPluralWords`, `placeholdersAnnounceNothing`.
- **24-hour clock and earlier-year dates**: no such match in the data. `theTimeFollowsTheClockPreference`, `aMatchFromAnotherYearIncludesIt`, `theDateFollowsTheLocaleOrderAndLanguage`.
- **iOS 26+**: the iOS 26.5 simulator is not signed in, so History was not observed there; the app builds and launches to the sign-in screen.
- **Landscape and Split View resizing**: rotating the simulator requires assistive access for System Events, which is not granted; only portrait iPhone and iPad widths were observed.

### Settings changed and restored

- iPhone 16 Pro (iOS 18.1): appearance light → dark → light; text size `large` → `accessibility-extra-extra-extra-large` → `large`. Spanish came from launch arguments (`-AppleLanguages (es) -AppleLocale es_CO`) and was not persisted.
- iPad Air (M2): appearance light → dark → light.
- The three simulators were shut down afterwards, as they were before.
- The working-tree Debug build replaced the installed app on the iPhone 16 Pro and the iPad Air, and was newly installed on the iOS 26.5 simulator.

### Follow-up: full-width rows and iOS 26+ (2026-10-03)

- **Full-width rows.** Following the updated design (Decision 1 and Risks), I removed the 640 pt column cap and its leading-alignment wrapper (`historyReadableColumn` became `historyHorizontalGutter`, a 16 pt horizontal gutter). On the iPad Air (M2) iOS 18.1 in portrait (820 pt), rows now span the full content width under the large title, with balanced team columns around the centered result, in light (`ipad18-fullwidth-history-portrait-light.png`) and dark (`ipad18-fullwidth-history-portrait-dark.png`). Another gambler's timeline was unchanged there (`ipad18-fullwidth-timeline-unchanged.png`). The earlier `ipad18-after-history-portrait-*.png` captures show the superseded 640 pt column.
- **Builds and tests after the change.** `xcodebuild build -scheme Tyche -workspace iOS/Tyche.xcworkspace` (iOS 26.5 destination) → BUILD SUCCEEDED. `xcodebuild test -scheme Bet -parallel-testing-enabled NO` (iOS 18.1 iPhone) → 27/27 passed. No `Package.resolved` changed.
- **iOS 26.5 (`tmp-drawer-iPhone17-26.5`).** It is signed in. I installed the updated build over the existing app (`simctl install`, no uninstall), and after relaunch it stayed signed in on `My pools`. Appearance stayed `light`, text size `large`. **History was not verified there:** the simulator tool refused every tap and screenshot because access to this simulator has not been granted ("Let Claude use it" pending or declined). Liquid Glass navigation and tab bar, large-title collapse, light/dark, summary and rows, refresh, append, row activation, the timeline, and Scores/Bets remain unobserved on iOS 26+.
- **Simulator state.** The iPad Air (M2) was booted for this check (appearance light → dark → light) and shut down again. The iOS 26.5 simulator was already booted and was left booted with Fortuna running.
- **iOS 26.5 checklist, after access was granted.** Run on `tmp-drawer-iPhone17-26.5` with the full-width build, signed in, production data. Taps right after a scroll are sometimes swallowed here too, so I repeated them.
  - *Liquid Glass shell*: the floating glass tab bar (Scores, Bets, History) and the glass ⇄ toolbar button are native. Content scrolls beneath the tab bar (`ios26-history-light.png`, `ios26-scores-tab-light.png`).
  - *Large-title collapse*: scrolling collapses `History` into the inline title over the scroll-edge effect, and the summary scrolls away with the content (`ios26-history-title-collapsed-light.png`).
  - *Light and dark*: summary `+589 pts earned`, rows, and the green and neutral pills (`ios26-history-light.png`, `ios26-history-dark.png`).
  - *Refresh*: pull to refresh kept the summary and rows visible (`ios26-history-refresh-dark.png`, captured as it settled).
  - *Append to the last row*: flicked down to México 2–0 Sudáfrica (11 Jun). The row and its `+10 pts` pill sit fully above the glass tab bar, and the no-bet row (Estados Unidos–Paraguay, `No bet`, `0 pts`) appears just above it (`ios26-history-last-row-dark.png`).
  - *Row activation*: the first row opened `Score` for España–Argentina once, with the glass back button and the tab bar hidden (`ios26-row-activation-match-dark.png`).
  - *Another gambler's timeline*: Scores → El mono opened `Timeline` with date headers and the unchanged `FinishedBetItem` rows (`ios26-timeline-unchanged-dark.png`).
  - *Scores and Bets intact*: `ios26-scores-tab-light.png`, `ios26-bets-tab-dark.png` (Bets shows its existing empty state).
  - *Observation outside this change*: on the `Score` screen, tapping the Santiago Arcila and Juano rows did not open their timelines on iOS 26.5 (three attempts). I did not change `MatchBetList`, and I did not check this screen on iOS 26.5 before the change, so the cause is unconfirmed.
  - *Settings*: appearance was light → dark → light (restored; text size stayed `large`). The simulator was left booted with Fortuna running and signed in.

Accepted without on-screen observation of summary-only failure, summary/list partial failure, out-of-order completion, context change mid-request, and append-failure retry (task 1.4) by the user on 2026-10-03; covered by the focused tests listed above.
Accepted without live VoiceOver (task 1.6) by the user on 2026-10-03; covered by the announcement tests listed above.
Accepted without landscape/Split View resizing and without on-screen initial loading placeholders under Reduce Motion (task 1.7) by the user on 2026-10-03.

## Android — tasks 2.1–2.8 (2026-10-03)

### Devices and builds

| Device | Use | State |
|---|---|---|
| Samsung Galaxy S8+ SM-G955F, serial `ce0417143a6ab8130c`, Android 9 (API 28), es-CO, three-button navigation | ARTEMIS re-check (2.1), hand checks, ARTEMIS integrated run (2.8) | `mobile_diagnose` verdict `ready` (5/5); signed in; `adb install -r -t` kept the session |
| AVD `medium_phone`, API 36 (google_apis_playstore arm64), en-US, gesture navigation | Light/dark, font scale 2.0, landscape, ARTEMIS integrated run (2.8) | Booted with `mobile_diagnose(launch_avd="medium_phone")`; it was **already signed in** as Felipe, so no sign-in was needed; `adb install -r -t` kept the session |
| AVD `Pixel_7_API_36_no_account`, API 36 (signed out) | Instrumented (connected) tests only | Booted with `mobile_diagnose(launch_avd=…)`, then stopped before `medium_phone` started; one AVD at a time |

Builds: `./gradlew :app:assembleProdDebug` (Prod flavor, production backend), Debug. History is read-only; no bet, score, or account data was written.

### 2.1 ARTEMIS re-check of the recorded exploration

Trace `3692c9da-448e-4367-9142-24e18b743489` (ARTEMIS Pro, `verification_level=final`, 3/3 checks passed, 23 steps) on the phone with the build installed before this change. The recorded exploration above still applies: same pool row (`[500, 154]`), bottom tabs `Puntajes`/`Apuestas`/`Historia` (`[167, 915]`/`[500, 915]`/`[832, 915]`), first rows España 0–0 Argentina (bet 3–2, +0), Francia 4–6 Inglaterra (3–1, +2), Inglaterra 1–2 Argentina (2–1, +2) under `DD/MM/YY` date headers, match destination `Marcador` (this time it loaded without `Error inesperado`), back arrow `[68, 65]`, timeline `Línea de tiempo` grouped under date headers. Captures: `android28-recheck-baseline-history.jpg`, `android28-recheck-baseline-timeline.jpg`.

New observation (before the change, outside this change): the pull to refresh in that run failed and the shared list replaced every row with the full-screen `Error inesperado` state, which has no retry button (`android28-recheck-baseline-refresh-error.jpg`). Switching tabs recovered it. Interaction paths reused for the tests below: tap by text through `uiautomator dump` (`Copa Mundial de la FIFA 2026`, `Historia`/`History`, `Puntajes`/`Scores`, `El mono`), toolbar back arrow at (73, 144) px on the phone.

### What changed (Android)

- `:bet` depends on `:data:pool`; `BetViewModelModule` passes the existing `GetPoolGamblerScore` (already registered by `PoolProvider`) to `FinishedBetListViewModel`. No new repository, endpoint, or backend change. The History view model is keyed by pool and gambler in `finishedBetListViewModel`.
- History-owned total (`HistoryPointsSummaryState`): requested when the view model is created (History opens), on pull to refresh, and on summary retry; keeps confirmed points during a refresh and after a failed one; cancels the previous request and discards any response from a superseded request.
- `HistoryPointsSummary` (production component with `historyPointsPlaceholderModel` + `isPlaceholder`) heads every list state (initial loading, loaded, empty, error) through one header item; `HistoryPointsHeader` adds the compact error/retry, the refresh failure, and inline progress for a summary retry.
- `RefreshableLazyPagingColumn` gained two optional parameters with list-only defaults: `onRefresh` (also called by the pull) and `isCompanionRefreshing` (keeps the pull indicator while that work is pending). No other caller passes them.
- `HistoryBetItem` replaces History's sticky date headers: per-row date/time, flags and names around a large centered result, `Your bet`/`Tu apuesta` and an award pill; teams stack around the result at font scale 1.5 and above. Rows are clickable only when real. `FinishedBetItem` and the timeline are untouched.
- Pool home: History uses a `LargeTopAppBar` with `exitUntilCollapsedScrollBehavior` (same avatar and ⇄ actions); Scores and Bets keep the compact bar. Switching tabs re-expands the large title.
- English, Spanish (`values-es`, "polla") and Spain Spanish (`values-es-rES`, "quiniela") strings with plural forms for visible and spoken points, matching the iOS wording.

### Results

| Check | Result | Evidence |
|---|---|---|
| Dependency resolution and compilation (2.2) | Pass | `./gradlew :app:compileProdDebugKotlin`, `:app:compileLocalDebugKotlin`, `:app:assembleProdDebug` |
| Unit tests | Pass | `./gradlew :bet:testDebugUnitTest` 16/16 (14 new), `:app:testProdDebugUnitTest` 60/60, `:ui:testDebugUnitTest` 44/44, `:pool:testDebugUnitTest` 2/2 |
| Instrumented tests | Pass | On `Pixel_7_API_36_no_account`: `:ui:connectedDebugAndroidTest` (`RefreshableLazyPagingColumnCompanionRefreshTest` 3 new + `RefreshableLazyPagingColumnInsetsTest` 4) 7/7; `:bet:connectedDebugAndroidTest` (`HistoryPresentationTest` 9 new + `BetPlaceholderAccessibilityTest` 5) 14/14 |
| Total for the current pool, not summed from rows | Pass | `+589 pts ganados` (pool list: 589 pts) while one page is loaded (`android28-after-history-light-es.png`, `android36-after-history-light-en.png`) |
| Context change shows the new pool's total | Pass | ⇄ → `Prueba` → Historia: `+4 pts ganados`; back to the World Cup pool: `+589` (`android28-after-history-other-pool-es.jpg`, ARTEMIS trace below) |
| Positive / zero / no-bet rows | Pass | green `+2 pts`, neutral `0 pts`, `Sin apuesta` + `0 pts` for Estados Unidos–Paraguay (`android28-after-history-last-row.png`) |
| Initial loading placeholders | Pass | Summary loaded first; row placeholders are the production row with the shared pulse (`android28-after-history-rows-loading.png`) |
| Large title once, collapses on scroll, compact Scores/Bets | Pass | `android28-after-history-title-collapsed.png`, `android36-after-history-dark-collapsed.png`; re-expanded after a tab switch (`android28-after-history-after-tab-switch.png`) |
| Pull to refresh keeps rows and total visible | Pass | `android28-after-history-refreshing.png` |
| Append to the last row; last row clear of the tab bar | Pass | Phone (three-button): México 2–0 Sudáfrica, `+10 pts` (`android28-after-history-last-row.png`); emulator (gesture): `android36-after-history-last-row-gesture.jpg`; total stayed `+589` |
| Row activation reaches the existing destination once | Pass | `Marcador`/`Score` for España–Argentina; one back press returns to History (`android28-after-row-activation-match.png`) |
| Timeline, Scores, Bets unchanged | Pass | `android28-after-timeline-unchanged.png`; ARTEMIS runs below |
| Long names wrap | Pass | `Bosnia y Herzegovina`, `República Checa` (`android28-after-history-last-row.png`) |
| Font scale | Pass | 1.3 on the phone (`android28-after-history-font130*.png`); 2.0 on API 36, teams stacked around the result (`android36-after-history-dark-font200*.png`) |
| Light and dark | Pass | `android36-after-history-light-en.png`, `android36-after-history-dark-en.png` |
| Landscape | Pass | Phone (three-button bar on the side) `android28-after-history-landscape-*.png`; API 36 `android36-after-history-landscape-dark.png`; rows span the full width |
| Spanish and English formatting | Pass | `domingo, 19 de jul.`, `2:00 p. m.`, `Tu apuesta`, `ganados`; `Sunday, Jul 19`, `2:00 PM`, `Your bet`, `earned` |
| TalkBack nodes on the device | Pass | `uiautomator dump` on the phone: one node per summary (`589 puntos ganados en esta polla`) and per row (`domingo, 19 de jul., 2:00 p. m. Resultado final: España 0, Argentina 0. Tu apuesta: 3 a 2. 0 puntos`); no separate flag, pill, or text nodes |
| Contrast (measured, WCAG) | Pass | secondary text 5.95:1 light / 7.36:1 dark; green value 6.56:1 / 11.14:1; green pill text 5.52:1 / 7.34:1; neutral pill text 5.31:1 / 6.09:1 |
| Comparison with the reference and iOS | See note | `compare-reference-ios-android-dark.png` (reference, iOS 18.1 dark, Android API 36 dark) |

ARTEMIS integrated runs (2.8), animations off on both devices:

- Phone, API 28, three-button, Spanish: trace `9dbb37bf-8e4d-4d3b-88da-97e26a99c440` (Pro, `final`, 7/7 checks passed). Layout, pull to refresh (rows and total stayed), scroll to the last row (no `Reintentar`), tab switch re-expands the title with the total unchanged, row → `Marcador` → back, `Apuestas`/`Puntajes` compact bars, `El mono` → unchanged `Línea de tiempo`, ⇄ → `Prueba` (`+4 pts ganados`) → back to `+589`.
- Emulator, API 36, gesture navigation, English: trace `154e4386-cbf9-4c79-b290-5b299119a351` (Pro, `final`, 11/11 checks passed). The same path except the pool switch; the last row cleared the tab bar and gesture handle.

Comparison note: Android matches the reference's hierarchy (summary, per-row date/time, centered result, `Your bet`, pills). Visible differences are platform chrome: Material's large app bar sets `History` in regular weight (the reference and iOS use bold), and Android keeps its bottom tab row and `#121212` dark canvas.

### Not observed on screen, and the tests that cover it

- **Summary-only failure, summary/list partial failure, retry independence**: the production endpoints did not fail during the checks. Covered by `given a failed first request when the summary is retried then the placeholder returns and the total loads`, `given a known total when a refresh runs and fails then the total stays with the failure`, `given a failed total when the summary is retried then the rows are not requested again` (`FinishedBetListViewModelPointsSummaryTest`). Previews `FinishedBetListSummaryFailedPreview`, `FinishedBetListRowsFailedPreview`.
- **Out-of-order completion**: `given two refreshes when the older response finishes last then the newer total stays`, `given two refreshes when an older failure finishes last then the newer total is not marked failed`.
- **Context change while a request is pending**: `given a pending request for one pool when another pool's History opens then each shows only its own total` (the on-screen pool switch was not mid-request).
- **Append never reloads the total**: `given loaded rows when further pages load then the total is neither requested again nor changed`. On screen the total stayed `+589` while pages appended, but requests were not counted (the app no longer logs Ktor traffic).
- **One pull refreshes both; feedback lasts for the slower request; list-only default unchanged**: `onePullRefreshesTheListAndStartsTheCompanionRefreshOnce`, `theIndicatorStaysUntilTheCompanionRefreshFinishesAfterTheList`, `withoutACompanionThePullRefreshesOnlyTheListAndTheIndicatorEndsWithIt` (`RefreshableLazyPagingColumnCompanionRefreshTest`).
- **Unavailable total, result, or award; positive/zero text**: no real match lacks them. `given a response without a total when History opens then the summary is unavailable, not zero`, `given a known total when a refresh returns no total then the previous number is replaced`, `unavailableValuesAreDescribedRatherThanReadAsZero`, `positivePointsAreSignedAndZeroOrUnavailableNeverShowAPlus`.
- **Append placeholder row and append-failure retry**: pages arrived before the end was reached and the endpoint did not fail. Append failure uses the shared `lazyPagingConcatenateError` retry, unchanged.
- **Empty and initial-error list states with the summary**: not stageable with this account. Previews only.
- **Live TalkBack**: not run. The exact announcements and the absence of placeholder/decorative stops are asserted by `aRowAnnouncesDateResultBetAndPointsInOrderAndOpensOnce`, `aSinglePointIsSpokenInTheSingularInSpanish`, `theSummaryAnnouncesCurrentPoolPointsWithSingularAndPluralWords`, `placeholdersExposeNoValuesOrActions`, plus the device node dump above.
- **24-hour clock and earlier-year dates**: no such match in the data. `theTimeFollowsTheClockPreference`, `aMatchFromAnotherYearIncludesIt`, `theDateFollowsTheLocaleOrderAndLanguage`.
- **Disabled-animation placeholders**: the ARTEMIS runs used disabled animations, but responses arrived before a placeholder capture; the rows use the shared `LocalLoadingPlaceholderPulse`, whose disabled-animation behavior is covered by the existing `LoadingPlaceholderPulseMotionTest`.
- **Dark theme on the API 28 phone**: `cmd uimode night yes` has no effect on Android 9 outside car mode; dark was checked on API 36 only.

### Settings changed and restored

- Phone: font scale 1.0 → 1.3 → 1.0; rotation (auto-rotate 1, user rotation 0) → fixed landscape → auto-rotate 1, user rotation 0; animator/transition/window scales 1.0 → 0 → 1.0. `cmd uimode night yes` was attempted and had no effect; night mode reads `no`, `ui_night_mode` 1, as before. The working-tree prodDebug build replaced the installed app (`adb install -r -t`); still signed in.
- `medium_phone`: night mode no → yes → no; font scale 1.0 → 2.0 → 1.0; rotation auto → fixed landscape → auto; animator scale unset → 0 → unset (`settings delete`), transition/window 1.0 → 0 → 1.0. The working-tree build replaced the installed app; still signed in. Shut down afterwards, as it was before.
- `Pixel_7_API_36_no_account`: booted for the `:ui` and `:bet` connected tests (library test APKs, installed and removed by Gradle; Fortuna there is signed out) and shut down.

Accepted without on-screen observation of context change mid-request (2.3), summary-only failure, summary/list partial failure, append-failure retry and request-counted "append does not reload the total" (2.4), and unavailable result/award values (2.5), closed on the focused tests above, by the user on 2026-10-03.
Accepted without live TalkBack (task 2.7), closed on tests and the accessibility dump, by the user on 2026-10-03.
Accepted without dark theme on API 28 (task 2.7; Android 9 ignores `cmd uimode night`), checked on API 36 only, by the user on 2026-10-03.
Accepted without on-screen empty and initial-error states with the total and without the on-screen append placeholder row (task 2.8), covered by tests, by the user on 2026-10-03.

### Follow-up: compact History app bar (task 2.6, 2026-10-03)

The user dropped the large History title on Android (design Decision 4 and task 2.6 updated). `PoolHomeView.kt` is back to its committed version. The History-only `LargeTopAppBar`, its `exitUntilCollapsedScrollBehavior` and state, and the re-expand-on-tab-switch logic are gone. History now uses the same compact `TopAppBar` and `enterAlwaysScrollBehavior` as Scores and Bets, with the avatar and ⇄ callbacks and the tab shell unchanged. The History content, summary, and paging hooks are unchanged.

- **Builds and tests**: `./gradlew :app:assembleProdDebug` passed; `:app:testProdDebugUnitTest` 60/60; `:bet:testDebugUnitTest` 16/16.
- **Phone** (SM-G955F, API 28, three-button, es-CO): installed with `adb install -r -t` and still signed in.
  - *Same compact bar, title once*: History, Puntajes, and Apuestas each show avatar, title, and ⇄ in the same compact bar, and no title appears in the History content (`android28-compact-history-top.png`, `android28-compact-scores.png`, `android28-compact-bets.png`).
  - *Avatar*: opens the drawer (`android28-compact-history-drawer-open.png`); system Back closed it and stayed on History.
  - *⇄*: opens `Mis pollas`; reopening the World Cup pool returned to History with `+589 pts ganados`.
  - *First and last content, portrait*: after scrolling to the end, México 2–0 Sudáfrica (`+10 pts`) sits fully above the tab bar while the compact bar is hidden by its scroll behavior, as on the other tabs (`android28-compact-history-last-row.png`). Scrolling back up brings the bar back with the summary fully below it (`android28-compact-history-first-content.png`).
  - *Landscape*: the summary is clear of the bar at the top and the last row is above the tab bar at the bottom, with the navigation buttons on the side (`android28-compact-history-landscape-top.png`, `android28-compact-history-landscape-last-row.png`).
- **Superseded captures**: the large-title captures above are superseded: `android28-after-history-light-es.png`, `android28-after-history-rows-loading.png`, `android28-after-history-title-collapsed.png`, `android28-after-history-after-tab-switch.png`, `android28-after-history-refreshing.png`, `android28-after-history-font130*.png`, `android28-after-history-landscape-top.png`, `android36-after-history-light-en.png`, `android36-after-history-dark-en.png`, `android36-after-history-dark-font200.png`, and the Android panel of `compare-reference-ios-android-dark.png`. Their list content is unchanged; only the title presentation differs.
- **Not rechecked after this change**: the API 36 gesture-navigation emulator, dark theme, and large font scale. The app bar change does not touch the History content.
- **Settings**: phone rotation went to fixed landscape and back (auto-rotate 1, user rotation 0). Font and animation scales were not changed (1.0).

### Follow-up: tab-switch app bar tint (task 2.9, 2026-10-03)

**Reproduced first** on the phone (SM-G955F, API 28) with the compact-bar build.
- Scrolling History hid the bar. Tapping Puntajes then opened Scores with the bar still hidden and the status-bar area tinted (`android28-tint-before-history-to-scores-bar-hidden.png`).
- Dragging down brought the bar back gray-tinted at the top of the leaderboard (`android28-tint-before-scores-top-tinted.png`, bar pixel `#F3EDF7`).
- Apuestas → Puntajes kept the tint, although Apuestas had not scrolled (`android28-tint-before-bets-to-scores-still-tinted.png`).

**Diagnosis confirmed and extended.** All tabs share one `TopAppBarState`, so both its `contentOffset` (the tint) and its `heightOffset` (bar hidden) carry over to the next tab. The new tab's list opens at its top, but nothing resets the state.

**Fix.** In `PoolHomeContent` (`PoolHomeView.kt`), the tab bar's callback resets the shared state's `heightOffset` and `contentOffset` to 0 when the selected tab changes, then forwards the change. Re-tapping the current tab changes nothing. Scroll behavior within a tab, the avatar and ⇄ callbacks, and the tab shell are unchanged.

**Test.** `PoolHomeTabSwitchAppBarTest` (app instrumented, on the signed-out `Pixel_7_API_36_no_account`) drives `PoolHomeContent` with one plain list per tab. For each of the six switches among History, Scores and Bets it checks:
- the bar title is shown and the bar's pixel matches the unscrolled color after the switch;
- scrolling the new tab hides the bar;
- dragging the bar back over the scrolled list tints it.

It passed with the fix (1/1). With the reset temporarily removed it failed: Scores' bar title was not displayed after History → Scores. The source was restored from a backup (same SHA-1).

**Device check after the fix** (phone, `adb install -r -t`, still signed in). For all six switches the bar is shown and untinted at the top of the new tab (`android28-tint-after-all-tab-switches.png`). Within a tab it still hides on scroll and comes back tinted over scrolled content (`android28-tint-after-scroll-still-hides-and-tints.png`). The avatar opened the drawer, and Back closed it. ⇄ opened `Mis pollas`, and reopening the pool worked. Apuestas shows its empty state for this account, so it can't be scrolled as the source tab; its row in the matrix starts unscrolled.

**Builds and tests.** `:app:assembleProdDebug` passed; `:app:testProdDebugUnitTest` 60/60; `:bet:testDebugUnitTest` 16/16; `:app:connectedProdDebugAndroidTest` (`PoolHomeTabSwitchAppBarTest`) 1/1.

**Not rechecked:** the API 36 gesture-navigation emulator and dark theme on screen. The instrumented test ran on API 36 in light theme.

**Settings:** no phone setting was changed. The test AVD was rebooted once after a `PixelCopy` timeout and an `adb` disconnect, then shut down.
Accepted without on-screen observation of switching away from a scrolled Bets tab (task 2.9; Bets is empty for this account), covered by `PoolHomeTabSwitchAppBarTest` on API 36, by the user on 2026-10-03.

### Follow-up: API 36 recheck and comparison (2026-10-03)

Rechecked the compact app bar (2.6) and the tab-switch reset (2.9) on AVD `medium_phone`: API 36, gesture navigation, en-US. The AVD was booted with `mobile_diagnose(launch_avd="medium_phone")`, and the current working-tree prodDebug build was installed with `adb install -r -t`; it stayed signed in. No code changed. Checks were driven with adb and the UI tree; ARTEMIS was not used.

| Check | Dark | Light |
|---|---|---|
| History, Scores and Bets share the same compact bar; title shown once | Pass (`android36-compact-dark-history-top.png`, `-scores.png`, `-bets.png`) | Pass (`android36-compact-light-history-top.png`, `-scores.png`) |
| All six tab switches after scrolling the source tab: untinted bar at the top of the new tab | Pass (`android36-compact-dark-all-tab-switches.png`) | Pass (`android36-compact-light-all-tab-switches.png`) |
| Bar hides on scroll; dragging it back over scrolled content tints it | Pass (`android36-compact-dark-bar-hides-and-tints.png`) | Pass (`android36-compact-light-bar-hides-and-tints.png`) |
| First content: summary below the bar at the top | Pass (history-top captures) | Pass |
| Last content: México 2–0 Sudáfrica fully above the tab bar and gesture handle | Pass (`android36-compact-dark-history-last-row.png`) | Pass (`android36-compact-light-history-last-row.png`) |
| Avatar opens the drawer, Back closes it; ⇄ opens My pools and reopening the pool returns to History (`589 points earned in this pool`) | Pass (`android36-compact-dark-drawer-open.png`) | Pass |

Bets shows its empty state for this account (in light it briefly showed its loading placeholders), so it cannot be scrolled as a source tab with real data. That case is covered by `PoolHomeTabSwitchAppBarTest`.

Test-driver observation: on this software-rendered, heavily loaded AVD (load average about 9), fast scripted flings (120–400 ms) were sometimes delivered as taps. Twice they opened a match's `Score` screen and, once, another gambler's timeline. Slower 700 ms swipes did not do this, and it never happened on the phone. I treat it as emulator input timing, not an app defect, but did not confirm the cause. I left those screens with system Back, checking after each step that pool home was reached.

**Comparison.** `compare-reference-ios-android-dark-compact.png` shows the reference, iOS 18.1 dark, and Android API 36 dark with the compact app bar. It supersedes `compare-reference-ios-android-dark.png`, which shows the dropped large title. The History content matches the reference hierarchy. On Android the title sits in the compact app bar rather than as a large heading, by design (Decision 4).

**Settings.** Night mode went no → yes → no. Animation, font, rotation and navigation settings were not changed (animator scale unset, transition and window 1.0, font 1.0, auto-rotate 1, gesture navigation). The AVD was shut down afterwards.
