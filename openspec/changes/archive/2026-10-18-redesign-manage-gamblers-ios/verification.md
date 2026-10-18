# Verification

## iOS — task 4.1 (2026-10-17)

Setup: the walk ran on the "iPhone 18 Pro Max" simulator (iOS 27.0, `C0B22275-991E-465C-9394-6349500609D3`, light mode). It had Fortuna 2.0.0 (12) installed, a Debug build of the working tree installed at 19:26, after the last change to any source file (19:13). The app was signed in as felipearpa against prod. Pools: "QA gamblers" (`01M55B9QZDA7725BDEB0PWA1EP`, 53 members at the start) and "QA solo" (`01M55EBNFB5DZNX85NSA4BAE23`, owner only). Locales were switched by relaunching the app with launch arguments (`xcrun simctl launch … -AppleLanguages "(es-CO)" -AppleLocale es_CO`, and the same for es-ES). The simulator's system settings were not changed. The app was relaunched in English at the end. Input was injected with the iOS Simulator tool. Screenshots were taken with `xcrun simctl io … screenshot`; for transient states, a loop captured frames continuously while the input ran. Screenshots are in `verification/`.

Gamblers removed from QA gamblers: btester09, btester10, btester11 (en) and btester12 (es-ES). The count went 53 → 49. The count line read 52, 51, 50 and 49 after each removal; the server confirmed 50 after the es-CO pull-to-refresh, and the pool list showed "50 members".

### Build checks

The package tests ran on the "iPhone 17 Pro Max" simulator (iOS 26.5, `CC5B4940-D918-4DE6-95EE-A1844A2AAC1E`), the closest local runtime to CI, not on iOS 18.1 as the task text says. The user chose this simulator. Results were read with `xcrun xcresulttool get test-results summary`.

- `xcodebuild test -scheme Pool -destination 'id=CC5B4940-…' -parallel-testing-enabled NO` (from `iOS/Pool`): Passed, 79 tests, 0 failures, 0 skipped.
- `xcodebuild test -scheme UI -destination 'id=CC5B4940-…' -parallel-testing-enabled NO` (from `iOS/UI`): Passed, 119 tests: 115 passed, 0 failures, 4 skipped. The skipped tests are the Liquid Glass legibility and presentation tests, which are outside this change.
- `xcodebuild -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator' build` (from `iOS/`): BUILD SUCCEEDED.
- xcodebuild rewrote `iOS/Pool/Package.resolved` and `iOS/UI/Package.resolved`. Both were restored with `git checkout`.

### Scenarios

| Scenario | Locale | Result | Screenshots |
|---|---|---|---|
| Owner opens Gamblers from the drawer: "Gamblers" heading and "53 gamblers in this pool" above the rows; top bar shows only Back | en | Pass | `en-01-drawer.png`, `en-02-gamblers-opened.png` |
| Same ("Jugadores", "50 jugadores en esta polla") | es-CO | Pass | `es-CO-03-drawer.png`, `es-CO-05-gamblers-loaded.png` |
| Same ("50 jugadores en esta quiniela") | es-ES | Pass | `es-ES-02-drawer.png`, `es-ES-04-gamblers-loaded.png` |
| Back returns to the pool home | en | Pass | `en-10-back-to-pool-home.png` |
| Heading scrolls with the rows | en | Pass. The heading had scrolled off while the owner's row was in view. | `en-09-owner-row-heading-scrolled.png` |
| Swipe past the threshold (about 100 pt, held): error background with the person-remove glyph and "Remove"; on release the alert opens and the row is back at rest behind it | en (btester09) | Pass | `en-03-swipe-past-threshold.png`, `en-04-confirm-dialog.png` |
| Same, with "Eliminar" and "¿Eliminar a @btester12 de esta polla?" | es-CO | Pass | `es-CO-06-swipe-background.png`, `es-CO-07-confirm-dialog.png` |
| Same, with "¿Eliminar a @btester12 de esta quiniela?" | es-ES | Pass | `es-ES-05-swipe-background.png`, `es-ES-06-confirm-dialog.png` |
| Swipe released early (40 pt): row settles, no alert | en | Pass. Seen in a screenshot after release; no file saved. | none |
| Swipe past the threshold, then dragged back almost to the start before release: row settles, no alert | en (btester10) | Pass | `en-06-drag-back-no-dialog.png` |
| Owner cancels: the row is unchanged | en, es-CO | Pass | `en-05-after-cancel.png`, `es-CO-08-after-cancel.png` |
| Owner confirms: the alert closes and the row dims in place with the ball spinner instead of the avatar; username and email stay; no overlay | en (btester09), es-ES (btester12) | Pass | `en-07-removing.png`, `es-ES-07-removing.png` |
| Removal succeeds: the row leaves and the count drops (53 → 52; 50 → 49) | en, es-ES | Pass | `en-08-removed.png`, `es-ES-08-removed.png` |
| Two removals at once: btester10 and btester11 swiped and confirmed back to back | en | Not observed. Each request finished in about 1 s. The tool needs 2–3 s between confirming one alert and confirming the next, so the two removals ran one after the other. Burst frames show each row removing on its own and each row leaving (count 52 → 51 → 50). No frame has both in flight. | none saved (burst frames only) |
| Removed gamblers stay gone after pull-to-refresh; the count stays 50 and comes from the server | es-CO | Pass. The refresh spinner showed above the heading. | `es-CO-09-pull-to-refresh.png`, `es-CO-10-after-refresh.png` |
| Owner's row: current-user highlight, "You" beside the username, "Owner" capsule, in its natural position (after btester30, not first) | en | Pass | `en-09-owner-row-heading-scrolled.png` |
| Same ("Tú", "Dueño") | es-CO | Pass | `es-CO-11-owner-row.png` |
| Swiping the owner's row does nothing | en | Pass. The row did not move in 12 frames captured during a 110 pt held drag, and no alert opened. | none (identical to `en-09`) |
| Only the owner: owner's row followed by "No other gamblers yet" with the group glyph; "1 gambler in this pool" | en | Pass | `en-14-only-owner-empty.png` |
| Same ("Aún no hay otros jugadores", "1 jugador en esta polla") | es-CO | Pass | `es-CO-01-drawer-qa-solo.png`, `es-CO-02-only-owner-empty.png` |
| Same ("1 jugador en esta quiniela", "Cuando alguien se una a esta quiniela…") | es-ES | Pass | `es-ES-01-only-owner-empty.png` |
| First load: eight placeholder rows and a placeholder count line, then the count arrives before the rows | en | Pass | `en-11-first-load-placeholders.png`, `en-12-count-before-rows.png`, `en-13-loaded-after-removals.png` |
| First load: eight placeholder rows and a placeholder count line (frame captured during the push transition) | es-CO | Pass | `es-CO-04-first-load-placeholders.png` |
| First load: eight placeholder rows; the count line had already loaded | es-ES | Pass (rows only) | `es-ES-03-placeholder-rows.png` |
| Removal fails: "Couldn't remove", Retry and ✕ in the row; two failures at once; Retry restarts without an alert; ✕ returns to idle with the count unchanged; swiping a failed row; pull-to-refresh clears failures | — | Not verified on the simulator: there was no way to make requests fail (see below) | none |
| Load error: error with Retry, no empty state | — | Not verified on the simulator: there was no way to make requests fail (see below) | none |

The cosmetic clipping noted in group 3 (the first letter of "Remove" clipped at a drag of about 58 pt) was not seen. The captured drags were about 100 pt and showed "Remove"/"Eliminar" in full.

### Not verified

- **Failure, Retry, dismiss, two failures, swiping a failed row, and pull-to-refresh with a failed row.** The simulator cannot go offline, and the user declined blocking the API host. These tests cover the behavior; all passed in the Pool run above:
  - `ManageGamblerRowTests.holderStateChangeRedrawsTheRow`, `.dismissReturnsTheFailedRowToIdle`, `.failedControlsNameTheGambler`
  - `GamblerRowHolderTests.failureShowsTheFailureAndReportsIt`, `.retryRestartsTheRemovalWithoutConfirmation`, `.dismissReturnsToIdleWithoutARequest`
  - `ManageGamblersListViewModelTests.dismissingAFailureDropsTheHolderAndKeepsTheCount`, `.refreshDropsFailedHoldersAndKeepsRemovalsInFlight`, `.aFailedCountIsAFailure`

  None of these tests renders two failed rows on screen at once.
- **Load error without the empty state.** This could not be staged on the simulator, for the same reason. The guard, `membersHoldOnlyTheOwner` (called by `ManageGamblersList.holdsOnlyTheOwner`), returns false unless the refresh is not loading and pagination has reached its end. `ManageGamblersEmptyStateTests` (added 2026-10-17 after the walk; Pool 88/88 on the iPhone 17 Pro Max, iOS 26.5) covers it: `firstPageLoadErrorShowsNoEmptyState`, `firstPageLoadingShowsNoEmptyState`, `nextPageLoadingOrFailedShowsNoEmptyState`, `morePagesToLoadShowsNoEmptyState`, and the owner-only, other-gamblers, removed-gamblers and no-members cases. `PoolPlaceholderSurfacesTests.failedOrMissingCountLeavesTheLineOut` covers the count line after a failed count request.
- **Two removals in flight at the same time:** see the table. Each removal and its outcome was observed.
- **Error haptic and the VoiceOver announcement on failure.** The simulator has no haptics, failures could not be staged, and VoiceOver was not driven.
- **VoiceOver "Remove" action and the "Removing" value.** VoiceOver was not driven. `ManageGamblerRowTests.ownerRowHasNoSwipeAndNoRemoveAction`, `.otherRowsOfferTheRemoveAction`, `.removingRowOffersNoRemoveAction` and `.removingRowIsDescribedAsRemoving` cover them.
- **A removing row scrolled off screen and back.** Requests finished in about 1 s, before a scroll could be made.
- **Package tests on iOS 18.1.** They ran on iOS 26.5 by the user's choice, as noted under Build checks.

Accepted without the on-screen failure walkthrough for task 3.4 (two failed rows, Retry, ✕, pull-to-refresh), closed on the cited tests, by the user on 2026-10-17.
Accepted without package tests on the iOS 18.1 simulator (run on iOS 26.5, the closest local runtime to CI) by the user on 2026-10-17.
