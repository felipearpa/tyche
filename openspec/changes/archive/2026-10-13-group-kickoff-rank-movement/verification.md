# Verification

## iOS — tasks 3.1–3.5 (2026-10-12)

Toolchain: Xcode 27.0 (`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`) on the iPhone 18 Pro Max simulator (iOS 27.0, `D8F12B93-3319-437A-8CE2-59C620CC8A29`). Xcode 26.5 has no iOS 26.5 platform installed and lists no eligible destination, so nothing was built with it. No live backend: every feed and score response comes from unit-test doubles or JSON fixtures shaped like `KickoffGroupResponse.fs` and `PoolGamblerScoreResponse.fs`.

Command (all locally runnable suites; ViewInspector cases excluded, see below):

```bash
cd iOS
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild test \
  -workspace Tyche.xcworkspace -scheme Tyche \
  -destination 'platform=iOS Simulator,id=D8F12B93-3319-437A-8CE2-59C620CC8A29' \
  -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO -parallel-testing-enabled NO \
  -only-testing:CoreTests -only-testing:DataBetTests -only-testing:DataPoolTests -only-testing:BetTests \
  -skip-testing:BetTests/HistoryPointsRetryTests \
  '-skip-testing:BetTests/FinishedBetListViewModelPointsSummaryTests/listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth()' \
  '-skip-testing:BetTests/BetTimelineListViewModelTests/listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth()' \
  -only-testing:PoolTests/GamblerScoreItemAccessibilityTests -only-testing:PoolTests/PoolScoreItemAccessibilityTests \
  -only-testing:PoolTests/GamblerScorePlaceholderTests \
  -only-testing:UITests -skip-testing:UITests/DrawerInteractionTests -skip-testing:UITests/LazyPagingVStackErrorTests \
  -only-testing:TycheTests
```

Result: passed, 295 tests. `BetTests/KickoffGroupPagingTests` also passed three consecutive iterations (`-test-iterations 3`). `UITests/TrendIndicatorTests` (ViewInspector) passed 5/5 when run alone.

| Task | Evidence |
| --- | --- |
| 3.1 | `CoreTests/RankMovementTests` (pending, unavailable, numeric, partial for score rows and feed groups); `DataBetTests/KickoffGroupFeedTests` (page decoding, partial counts, `movement: null`, a group with no computed match, `INVALID_CURSOR`/`CURSOR_CONTEXT_MISMATCH` mapping, first-page request without a cursor); `DataPoolTests/PoolGamblerScoreResponseTests` (`trendGroup` object, JSON null, absent, missing `beforePosition`). |
| 3.2 | `assets/icons/arrow_upward.svg`, `arrow_downward.svg`, `horizontal_rule.svg` are byte-identical to the iOS image-set files (`cmp`); SHA-256 recorded in `assets/icons/README.md`. Not compared with the upstream Material Symbols files (no download in this round). |
| 3.3 | `BetTests/KickoffGroupPresentationTests`; captures below. |
| 3.4 | `BetTests/KickoffGroupPagingTests` (append order, server counts and movement, a stale page rejected as superseded, a refresh during an append, one automatic first-page restart with groups kept visible, a second rejection shown with retry, ordinary append retry); `BetTests/BetTimelineListViewModelTests` (phase order across pages, partial-to-finished on refresh, pending versus unavailable). |
| 3.5 | `PoolTests/GamblerScoreItemAccessibilityTests` and `PoolScoreItemAccessibilityTests` (announcement order with the partial qualifier last, pending, unavailable, singular and plural, inert placeholders); `UITests/RankMovementAccessibilityTests`; `BetTests/KickoffGroupPresentationTests` (header announcement, a row focused directly keeps its own date and time without the group count or movement). |

Captures in [verification/ios](verification/ios), rendered with `ImageRenderer` at 2× from the production components and preview data (a temporary test, since deleted): `history-groups-{dark,light}.png`, `history-groups-dark-320.png`, `history-groups-dark-744.png`, `history-partial-unavailable-{dark,light}.png`, `history-partial-ax5-dark-320.png`, `group-sizes-1-to-4-{dark,light}.png`, `timeline-pending-partial-{dark,light}.png`, `timeline-pending-partial-ax3-dark.png`, `history-placeholder-{dark,light}.png`, `leaderboard-trend-states-{dark,light}.png`, `leaderboard-trend-states-dark-320.png`, `leaderboard-trend-states-ax3-dark.png`, `my-pools-trend-states-{dark,light}.png`, `my-pools-trend-states-ax3-dark.png`. They are component compositions, not app screenshots: no navigation bar, tab bar, or scrolling.

Measured contrast (WCAG): qualifier and state labels (`onSurface` at 70%) 5.9:1 / 7.7:1 on the light / dark canvas, 5.4:1 / 6.4:1 on the badge fill, 5.5:1 / 5.4:1 on the current-user row. Trend colors on the dark badge fill: positive 7.2:1, negative 7.0:1, steady 12.4:1. On the light badge fill: positive 2.6:1, negative 2.9:1, steady 1.4:1; on the light canvas they are 3.2:1, 3.5:1, and 1.7:1, the existing token values the leaderboard already used.

Not verified locally:

- ViewInspector suites crash with `Can't unsafeBitCast between types of different sizes` or fail with "does not have 'accessibilityLabel'/'accessibilityHidden' modifier" on iOS 27: `PoolTests/PoolTests`, `GamblerScoreItemRankPreviewTests`, `PoolPlaceholderSurfacesTests`, `GamblerScoreItemPlaceholderBehaviorTests`, `BetTests/HistoryPointsRetryTests`, and the two `listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth()` tests. CI only.
- VoiceOver on a device or simulator, the app's real screens with navigation and tab bars, pull to refresh, and iPad and landscape layouts (task 3.6).

Accepted without comparing the committed trend SVGs against upstream Material Symbols by the user on 2026-10-12.
Accepted without VoiceOver on a running app and the real app screens with navigation and tab bars by the user on 2026-10-12; these checks move to task 3.6.

## iOS — follow-ups to 3.1–3.5 (2026-10-12)

Same toolchain and simulator as the previous section; no live backend.

### Light-mode trend tokens

The light values of the shared trend tokens in `iOS/UI/Sources/UI/Assets/Assets.xcassets/Colors` changed; the dark values did not. Android adopts the same light values.

| Token | Before (light) | After (light) | White canvas | Badge fill (`secondarySystemFill` over white) | Current-user row `#D4F3DE` |
| --- | --- | --- | --- | --- | --- |
| `PositiveColor` | `#689F38` | `#33691E` | 6.60:1 | 5.45:1 | 5.56:1 |
| `NegativeColor` | `#EF5350` | `#B71C1C` | 6.57:1 | 5.42:1 | 5.53:1 |
| `NeutralColor` (steady) | `#FBC02D` | `#8C5E00` | 5.65:1 | 4.66:1 | 4.76:1 |

The badge-fill column composites `secondarySystemFill` as `#E9E9EB`. Dark values are unchanged: `#8BC34A`, `#EF9A9A`, `#FFEB3B`, at 5.7:1 or higher on black, the dark badge fill, and the dark current-user row `#143C24`. `UITests/TrendColorTests` resolves every token in both appearances against the canvas, the composited badge fill, and the current-user row, and requires 4.5:1. The before values fail it at 2.6:1, 2.9:1, and 1.4:1 on the light badge fill.

### Leaderboard at accessibility text sizes

At accessibility sizes `GamblerScoreItem` reflows. The rank rail, avatar, and score share the first line; the score moves below them when they don't fit. The username then wraps across the full width. At ordinary sizes it stays one line with an ellipsis, as the pool-leaderboard "Long username" scenario allows. A plain rank rail is now at least as wide as its arrow and number, so "↑3" no longer clips. The row keeps its single announcement and actions. `PoolTests/GamblerScoreItemLargeTextTests` uses `UIHostingController.sizeThatFits` to check three things: a long username makes the row taller at AX3 and AX5, it does not at the default size, and the plain rail is never narrower than its movement at default, AX3, and AX5.

### Append retry

History, Timeline, and the leaderboard now render through `UI/Paging/AppendPagingVStack`. Its rows call only `LazyPagingItems.appendIfNeeded(currentIndex:)`, never `onRowAccess(index:)`. A prepend is therefore never armed, and `retry()` repeats the failed append. `UITests/AppendPagingVStackTests` covers this in two tests. A one-row list whose append fails is retried with the same cursor. The library's `onRowAccess` path documents the original bug: there, `retry()` repeats a prepend and no request goes out. `BetTests/KickoffGroupPagingTests.anOrdinaryAppendFailureRetriesTheSameContinuation` covers a one-group History list. The lazy-paging library is unchanged.

### Checks

The same command as the previous section, plus `-only-testing:PoolTests/GamblerScoreItemLargeTextTests`. Result: passed, 302 tests (4 skipped).

Captures (temporary `ImageRenderer` test, since deleted): `followup-leaderboard-light.png`, `followup-my-pools-light.png`, `followup-history-light.png`, `followup-leaderboard-ax3-dark.png`, `followup-leaderboard-ax5-dark.png`, `followup-leaderboard-ax5-light-320.png`. Earlier light captures in this folder show the previous token values.

Not verified locally:

- The ViewInspector suites that inspect the changed leaderboard row and paging error view: `GamblerScoreItemRankPreviewTests`, `GamblerScoreItemPlaceholderBehaviorTests`, `PoolTests/PoolTests`, and `UITests/LazyPagingVStackErrorTests`. They crash or fail on iOS 27 as before. CI only.
- Pull to refresh and the append Retry in the running app.

## Android — tasks 4.2–4.5 (2026-10-12)

JVM checks only: no device, emulator, Robolectric, or screenshot tooling was used, and there is no live backend. Feed and score responses come from Ktor `MockEngine` bodies and JSON strings shaped like `KickoffGroupResponse.fs` and `PoolGamblerScoreResponse.fs`. The project has no Compose preview-rendering task, so no Android captures were saved under `verification/android`; the previews named below render in Android Studio only.

Commands, from `Android/`:

```bash
./gradlew testDebugUnitTest :app:testProdDebugUnitTest --offline   # the CI command
./gradlew :ui:compileDebugAndroidTestKotlin :bet:compileDebugAndroidTestKotlin \
  :pool:compileDebugAndroidTestKotlin :app:compileProdDebugAndroidTestKotlin :app:assembleProdDebug --offline
./gradlew :ui:lintDebug :bet:lintDebug :pool:lintDebug
```

Result: 715 unit tests, 0 failures; instrumented sources compile; the app assembles. Lint reports one error, `NonObservableLocale` at `rememberHistoryMatchDateFormat`'s `Locale.getDefault()` fallback, which predates this change, and no missing translations or plural quantities in the new strings.

| Task | Evidence |
| --- | --- |
| 4.2 | `core` `RankMovementTest` (pending, unavailable, numeric, partial for score rows and feed groups); `data:bet` `KickoffGroupFeedRequestTest` (grouped page decoding, partial counts, `movement: null`, a group with no computed match, two instants that read 01:30 in New York on 2026-11-01 staying two groups, an offset instant equal to its UTC form, a malformed kickoff failing, `INVALID_CURSOR` and `CURSOR_CONTEXT_MISMATCH` on both feeds, no `searchText` or cursor on a first History page); `data:pool` `PoolGamblerScoreResponseTest` (`trendGroup` object, JSON null, absent field, missing `beforePosition`); `pool` `PoolGamblerScoreModelTest`. |
| 4.3 | `ui` drawables `arrow_upward.xml`, `arrow_downward.xml`, `horizontal_rule.xml` carry the canonical SVG path data unchanged (compared by script); `assets/icons/README.md` documents them. Previews: `TrendIndicator` (plain and badge states), `KickoffGroupHeader`, `KickoffGroupSection` (one to four matches; Timeline pending and partial with `Bet` ownership; placeholder), `FinishedBetList`, `BetTimelineList`. |
| 4.4 | `bet` `KickoffGroupFeedTest` (append order with server counts and movement, distinct keys for DST-colliding labels, a group repeated in a later page shown once, partial-to-finished on refresh, one automatic first-page restart after a rejected continuation, a second rejection shown as "The list changed" with no further requests, a viewer restart re-arming the automatic restart, and an ordinary failed append retried with the same continuation with one group loaded); `BetTimelineListViewModelTest` and `FinishedBetListViewModelPointsSummaryTest` on grouped pages. |
| 4.5 | `ui` `TrendColorContrastTest` (trend colors and labels at 4.5:1 or more on canvas, badge fill, and current-user row; with the previous values it fails, for example light gain 4.12:1 on the canvas). Instrumented expectations updated, not run: `PoolScoreItemAccessibilityTest` (pending, unavailable, partial `Up 3 places, 2 of 4 matches computed`), `ProgressIndicatorTest` (pending and unavailable labels, partial qualifier). Previews: `GamblerScoreItem` and `PoolScoreItem` trend states at default, `fontScale = 2f` at 320 dp, and Spanish. |

Append retry: AndroidX Paging 3.5's `retry()` repeats only load types in error. The feed's pages have no `prevKey`, so prepend is complete and never retried in place of the append; `KickoffGroupFeedTest` covers this with one group loaded. No paging wrapper change was needed.

Trend colors (WCAG, measured by `contrastRatio`):

| Token | Light | Canvas `#FFFFFF` | Badge fill `surfaceVariant` `#F5F5F5` | Current-user row `#D4F3DE` |
| --- | --- | --- | --- | --- |
| gain | `#388E3C` → `#33691E` | 6.60 | 6.06 | 5.56 |
| drop | `#D32F2F` → `#B71C1C` | 6.57 | 6.03 | 5.53 |
| steady | `#FBC02D` → `#8C5E00` | 5.65 | 5.18 | 4.76 |

| Token | Dark | Canvas `#121212` | Badge fill `#1E1E1E` | Current-user row `#143C24` |
| --- | --- | --- | --- | --- |
| gain | `#A5D6A7` (unchanged) | 11.40 | 10.14 | 7.51 |
| drop | `#F44336` → `#EF9A9A` | 8.71 | 7.75 | 5.74 |
| steady | `#FFEB3B` (unchanged) | 15.34 | 13.65 | 10.11 |

The previous dark drop `#F44336` measured 5.09:1 on the canvas but 3.35:1 on the dark current-user row, so it changed to iOS's dark negative value. The badge uses `surfaceVariant`: Android's `onSurface` at 12% over white (`#E4E4E4`) left light steady at 4.44:1. Qualifier and state labels (`onSurface` at 70%) measure 5.92 / 5.68 / 5.48 light and 7.34 / 6.85 / 5.44 dark on canvas, badge fill, and current-user row.

Not verified:

- Instrumented tests (`connectedAndroidTest`), TalkBack, rendered previews, large font scales, reduced motion, light and dark on a device, and edge-to-edge scroll limits (task 4.6).
- Real History, Timeline, leaderboard, and My pools screens against the deployed grouped endpoints (tasks 4.1, 4.6, 5.x).

## iOS — repeated kickoff groups (2026-10-12)

`KickoffGroupFeed` (`iOS/Bet/Sources/Bet/KickoffGroup/KickoffGroupFeed.swift`) shows each kickoff once per generation, matching Android. A group whose kickoff an earlier page of the same list already showed is dropped. That happens when a Timeline group settles between pages and comes back in the finished phase. Its settled copy appears on the next refresh, and every first page starts a new generation with no seen kickoffs. When dropping leaves a page empty and the page has a continuation, the feed fetches the next page at once, up to three more times, so the list does not stall.

Tests in `BetTests/KickoffGroupPagingTests`:

- `aTimelineGroupThatSettlesBetweenPagesAppearsOnceAndItsSettledCopyAfterARefresh`: the group appears once, with its first, partial copy and unique identities; after a first-page reload it shows the settled movement.
- `aPageLeftEmptyByARepeatedGroupContinuesToTheNextPage`.

Bet tests ran with the earlier command restricted to `-only-testing:BetTests` and the same ViewInspector skips: passed, 98 tests. Not verified: the scenario in the running app against a live backend.

## Android — renders and checks for 4.3–4.5 (2026-10-13)

Device: the Pixel_10_Pro_XL emulator (API 37.2, `emulator-5554`), booted through ARTEMIS and shut down afterwards. Only the `:ui`, `:bet`, and `:pool` test APKs were installed (`installDebugAndroidTest`), run with `adb -s emulator-5554 shell am instrument -w -e class …`, and uninstalled at the end; the Fortuna app was not touched. Global animation scales and the font scale were recorded first (`animator_duration_scale` unset, transition and window 1.0, font 1.0) and match those values at the end. No live backend.

### Renders

[verification/android](verification/android): 84 PNGs from the production components (`GamblerScoreItem`, `PoolScoreItem`, `KickoffGroupSection` with its header and rows), each in light and dark, at font scale 1 and 2, and at 320 dp and 600 dp; the leaderboard also at font scale 3. Scenes: leaderboard and My pools rows in every trend state (up, down, steady, partial for the current user, pending, unavailable) with a placeholder row; History groups of one to four matches, a down and a partial group, a steady group with long names, an unavailable group from an earlier year; Timeline pending and partial groups with `Bet`; the group placeholder. File names are `<scene>-<theme>-fs<scale>-<width>dp.png`. A temporary harness drew each scene into an offscreen graphics layer with a fixed density and font scale; it had no assertions and was deleted. The font scale is applied linearly, not through Android 14's non-linear font scaling.

Fixes made because of the renders:

- Rail labels broke mid-word ("Unavailab le", "Pendi ng"). The leaderboard and My pools rails are now at least as wide as the label's widest word (`RankMovement.labelWordWidth`), and labels drop `labelSmall`'s letter spacing, as iOS's `caption2` has none.
- Team names broke mid-word in History rows at 320 dp ("Paragua y"). `HistoryBetItem` stacks the teams around the result when a name's widest word cannot fit its column.
- My pools squeezed the pool name at font scale 2 ("Campe ones", "membe rs"). At font scale 1.5 or more the row reflows: rank rail and invite control on the first line, name and counts at full width; points and member count wrap as whole items.

Compared with `reference/history-grouped-icons.png`, the History hierarchy matches: date over time and count, trailing trend pill, rail, dividers, centered result, `Your bet` footer, and points pill.

Differences from the iOS captures in [verification/ios](verification/ios):

- At 320 dp, a row whose result is unavailable (dashes) stacks its teams on Android because the dashes are wider than digits; iOS keeps three columns at its capture width. At 360 dp and wider, Android keeps three columns too.
- At font scale 2 the History header's badge moves below the date and time on Android, trailing-aligned with its qualifier; iOS puts it on the leading edge under the metadata at accessibility sizes.
- The light badge fill is `surfaceVariant` `#F5F5F5`, slightly lighter than iOS's `secondarySystemFill`.
- My pools rows have no dividers and the invite control has no filled circle on Android; both predate this change (the iOS circle comes from `ios-liquid-glass`).
- A single-token name (an email address) still wraps inside the token at large text on both platforms.

### Instrumented tests, class by class

| Class | Result |
| --- | --- |
| `pool` `PoolScoreItemAccessibilityTest` | 11 passed (pending, unavailable, partial `Up 3 places, 2 of 4 matches computed`, singular and plural) |
| `pool` `PoolScoreListAccessibilityTest` | 3 passed |
| `pool` `GamblerScoreListTest` | 3 passed (expectation updated to end with "Movement unavailable") |
| `pool` `GamblerScorePlaceholderTest` | 6 passed |
| `bet` `HistoryPresentationTest` | 20 passed |
| `bet` `BetTimelineListTest` | 5 passed |
| `bet` `HistoryListRetryTest` | 2 passed |
| `bet` `BetPlaceholderAccessibilityTest` | 3 passed |
| `ui` `LoadingPlaceholderPulseMotionTest` | 2 passed |
| `ui` `ProgressIndicatorTest` (earlier full `:ui` run) | 6 passed |

These cover one announcement per row and per group header, the absence of separate trend or qualifier stops, and inert placeholders (no text, description, or click action).

Failures seen in the earlier full-module runs, all in code this change does not touch, so classified as unrelated (not re-checked against the committed code): `PushDrawerAccessibilityTest` ended the `:ui` run with an ANR (`keyDispatchingTimedOut`); `PoolNameKeyboardTest` (six cases, the keyboard never became visible) and `PoolJoinSafeBoundsTest`; `GamblerScorePlaceholderTest`'s pixel comparison once failed with "Failed waiting for PixelCopy!" and passed on the class run.

### Reduced motion

With `animator_duration_scale` set to 0: `LoadingPlaceholderPulseMotionTest` (2), `GamblerScorePlaceholderTest` (6), `BetPlaceholderAccessibilityTest` (3), and `BetTimelineListTest` (5) passed. The setting was then deleted, restoring its unset value.

### Feed parity check

A page whose groups were all already shown, but which carries a continuation, does not stall: AndroidX Paging keeps loading while the loaded items stay within the prefetch distance, so it requests the next key at once. `KickoffGroupFeedTest` covers it (`null`, `page-2`, `page-3` requested; both groups shown). No bounded refill was needed on Android.

### Unit tests

`./gradlew testDebugUnitTest :app:testProdDebugUnitTest -Dorg.gradle.jvmargs="-Xmx4g -Dfile.encoding=UTF-8"`: 716 tests, 0 failures, after these fixes. On this machine the default 2 GB Gradle heap once deadlocked in dex merging; 4 GB worked.

Not verified:

- TalkBack spoken on a device; the semantics are checked by the Compose tests above.
- The real app screens with navigation and tab bars, pull to refresh, edge-to-edge scroll limits, and the grouped endpoints (tasks 4.1, 4.6, and 5.x).

Accepted without TalkBack speech on a device, the real app screens, and Android 14 non-linear font scaling by the user on 2026-10-13; these checks move to task 4.6.

## iOS — task 3.6 live checks (2026-10-13)

The `Tyche` scheme (production, `https://tyche-api.felipearpa.com`) was built with Xcode 27 and installed over the existing app. The user signed in by hand. Devices:

- iPhone SE (3rd generation), iOS 27.0, `4D1E3B5D-B498-4B1C-82D9-965242CF0E88`, 375 × 667 pt, no home indicator.
- iPad Pro 13-inch (M5), iOS 27.0, `2CADE6B5-934E-42A0-99A1-1BD92D9C83E8`, portrait 1032 × 1376 pt.

```bash
cd iOS
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild build -workspace Tyche.xcworkspace -scheme Tyche \
  -configuration Debug -destination 'platform=iOS Simulator,id=D8F12B93-3319-437A-8CE2-59C620CC8A29' -disableAutomaticPackageResolution
xcrun simctl install <udid> ~/Library/Developer/Xcode/DerivedData/Tyche-*/Build/Products/Debug-iphonesimulator/Tyche.app
```

Taps and swipes were sent through the Claude iOS simulator tool. Appearance and text size were set with `xcrun simctl ui <udid> appearance light|dark` and `content_size large|accessibility-extra-large`. Captures were taken with `xcrun simctl io <udid> screenshot`. Network requests were counted from the app's CFNetwork log (`simctl spawn <udid> log stream --predicate 'process == "Tyche" AND subsystem == "com.apple.CFNetwork"'`, one `summary for task` line per request, with status and size; URLs are redacted in the log). Both devices ended in light appearance at the `large` text size, as they started.

The data is real production data: a finished 2026 World Cup pool of 13 gamblers, a one-member pool, and a three-member pool. Every match has a result, so no group is partial or pending.

| Check | Result |
| --- | --- |
| Leaderboard and My pools trend states | Every row in all three pools shows the steady symbol. The signed-in gambler's newest History group (Sunday 19 Jul, España – Argentina) is steady too, so the two surfaces agree. Partial and pending states cannot occur in this data. |
| History grouped headers, rail, counts, movement | Date, time, and a trailing trend badge per group. "2 matches" after the time on two-match group-stage kickoffs. Rail and internal divider present. Movement shown as up, down, steady, and "Unavailable": the opening match (Thursday 11 Jun 2:00 PM) has no snapshot. Rows say `Your bet`. |
| Another gambler's Timeline | Opened from the leaderboard (C.A.Vélez; Juan Pablo Rojas). Same grouping, with `Bet` and `No bet` ownership. The newest groups match the leaderboard's steady state. |
| Paging | Not observable with this data: one request returned the whole Timeline (about 63 KB), and scrolling a Timeline into the group stage made no further feed request. The server's pages are DynamoDB pages, and a full tournament fits in one. History was scrolled to its last group (the opening match), and no kickoff header repeated in the captured screens. Multi-page behavior stays covered by `BetTests/KickoffGroupPagingTests`. |
| Pull to refresh | History, iPhone and iPad: two requests (total and first page), both 200. Leaderboard: one request. My pools: one request. |
| Summary-only and list-only retry, empty and error states | Not staged live; see Not verified. |
| Safe-area reachability | The first content sits below the navigation bar, and the last row or group scrolls fully above the floating tab bar, at the default text size and at accessibility-extra-large: iPhone leaderboard, History end, and My pools end; iPad leaderboard end. Pushed Timeline screens have no tab bar; their back and home buttons stay reachable. |
| Real screens with navigation and tab bars | iPhone: bottom floating tab bar (Scores, Bets, History), which minimizes on scroll. iPad: top segmented tab bar. Large titles collapse into the navigation bar on scroll. |
| Themes and text size | Light and dark on both devices. At accessibility-extra-large, History and Timeline headers move the badge below the date and time, rows stack the teams, the leaderboard row reflows (rank rail, avatar, and score on the first line, then the wrapped username), and My pools moves name and counts below the rank rail. Nothing was clipped. |

Captures in [verification/ios/live](verification/ios/live) (real account data): `se-my-pools-light.png`, `se-my-pools-dark-axxl{,-end}.png`, `se-leaderboard-light-{top,bottom}.png`, `se-leaderboard-dark-top.png`, `se-leaderboard-dark-axxl{,-top}.png`, `se-history-light-{top,end}.png`, `se-history-dark-top.png`, `se-history-dark-axxl-{top,end}.png`, `se-timeline-light-{top,two-match-groups}.png`, `se-timeline-dark-two-match-groups.png`, `se-timeline-dark-axxl.png`, `ipad-my-pools-light-portrait.png`, `ipad-leaderboard-light-portrait.png`, `ipad-leaderboard-dark-axxl{,-end}.png`, `ipad-history-{light,dark}-portrait.png`, `ipad-history-dark-axxl.png`, `ipad-timeline-{light,dark}-portrait.png`, `ipad-timeline-dark-two-match-groups.png`, `ipad-timeline-dark-axxl.png`.

Observations, not changed:

- On the 1032-pt iPad, History and Timeline rows span the full width: the teams sit far apart, and the trend badge and points pill sit at the trailing edge. The leaderboard and My pools do the same. No screen has a readable-width limit, and this change did not introduce one.
- At accessibility sizes the iPad stacks History and Timeline teams and drops the header badge below the metadata even though the width would allow one line, because the layout switches on text size, not width.
- Opening a leaderboard returns 403 for every member without an uploaded avatar (the avatar bucket's response for a missing object), and the row falls back to the initial. This predates this change.

### Tests

`BetTests/FinishedBetListViewModelPointsSummaryTests.firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal` and `BetTests/BetTimelineListViewModelTests.firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal` drive the list's first-page Retry through the view model. They check that it requests the first page again without a cursor, never requests the total, and keeps the confirmed total. They wait with the files' poll-budgeted `waitUntil`. The existing `listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth()` tests check the same thing through ViewInspector and crash on local iOS 27 runtimes, so until now list-only retry was covered only on CI. No UI test was added: XCUITest would reinstall the app and drop the sign-in, and the real-data states it could reach are covered by the captures above.

`xcodebuild build-for-testing -workspace Tyche.xcworkspace -scheme Tyche -destination 'generic/platform=iOS Simulator' -disableAutomaticPackageResolution CODE_SIGNING_ALLOWED=NO`: TEST BUILD SUCCEEDED. The new tests were not run (see Not verified).

### READMEs

`iOS/Bet/README.md` documents the grouped feed contract, paging rules, movement states, presentation, and the independent points summary. `iOS/Pool/README.md` documents the `trendGroup` score contract, the movement table, announcements, large-text reflow, and placeholders.

Not verified:

- Summary-only versus list-only retry, the empty state, and the error states in the running app. The simulators share the host's network, so failures could not be forced without affecting the host. No pool or gambler in production has an empty History or Timeline. These states are covered by `FinishedBetListViewModelPointsSummaryTests`, `BetTimelineListViewModelTests`, and `KickoffGroupPagingTests`; the ViewInspector cases among them run on CI only.
- Running the two new tests. The only iOS 27 simulators were the two signed-in devices, where `xcodebuild test` is not allowed, and the shut-down iPhone 18 Pro Max.
- Partial (`1 of 2 computed`) and pending movement, multi-page appends, and the invalid-cursor restart, against live data. Production holds only a finished tournament.
- VoiceOver and the accessibility hierarchy. Running Accessibility Inspector needs Simulator.app or GUI control, which was not available. Announcement order and focus stops remain covered by `PoolTests/GamblerScoreItemAccessibilityTests`, `PoolScoreItemAccessibilityTests`, `UITests/RankMovementAccessibilityTests`, and `BetTests/KickoffGroupPresentationTests`.
- iPad landscape and Split View widths. Neither `simctl` nor the simulator tool can rotate the device or resize the window without Simulator.app.
- The iPhone 18 Pro Max. It stayed shut down after the simulator service failures in the build and install step.

Accepted without the running-app error, empty, and retry states, VoiceOver, iPad landscape and Split View, the iPhone 18 Pro Max, and live partial/pending movement and paging by the user on 2026-10-13; live partial/pending movement and paging are covered by task 5.1 and 5.2.

## Android — task 4.1 exploration (2026-10-13)

Status: done. The first round stopped when the emulator crashed before History was scrolled to its end (see the end of this section); the end of History was checked after the user signed in again (below).

Exploration used the `android` CLI (`android layout --device=emulator-5554 --flat --no-idle`, `android screen capture`) with `adb -s emulator-5554 shell input`, not ARTEMIS. ARTEMIS's Gemini prepaid credits are depleted (its tasks failed with `402 Payment Required … RESOURCE_EXHAUSTED`), and the user decided on 2026-10-13 to explore with the android CLI instead.

Device: the Pixel_10_Pro_XL emulator (API 37.2, `emulator-5554`, 1344 × 2992 px, density 480), light theme, font scale 1.0, portrait. The app was built with `./gradlew :app:assembleProdDebug -Dorg.gradle.jvmargs="-Xmx4g -Dfile.encoding=UTF-8"` (Prod flavor, `https://tyche-api.felipearpa.com`), installed with `adb -s emulator-5554 install -r Android/app/build/outputs/apk/prod/debug/app-prod-debug.apk`, and signed in by the user. The data is production data: a finished 2026 World Cup pool of 13 gamblers, a one-member pool, and a three-member pool. Requests were counted from the app's Ktor logcat lines (`REQUEST`/`RESPONSE`), with tokens excluded.

`android layout` must be called with `--no-idle`: on the Timeline and History screens it otherwise waits indefinitely for an idle state and never returns. Compose test tags are not exported as resource IDs (`testTagsAsResourceId` is not set), so the on-device locators are content descriptions and text; the tags below are for Compose tests only.

### Verified screens, locators, transitions, and waits

| Screen | Reached by | Locators (content description or text) | Observed |
| --- | --- | --- | --- |
| My pools | App launch while signed in | Title text `My pools`; `Open menu`; `Create pool`; one clickable row per pool with description `<pool>, Rank <n>, <points> points, <members> members, <movement>`, for example `Prueba, Rank 3, 4 points, 3 members, Rank unchanged` | All three pools show the steady symbol (a short orange bar under the rank tile), announced `Rank unchanged`. The invite icon has no node of its own in the flat layout. |
| Leaderboard (Scores tab) | Tap a pool row on My pools; wait about 6 s for the rows | Top bar text `Scores`; tabs `Scores`, `Bets`, `History` (text); rows `Rank <n>, <username>, <points> points, <movement>`; the signed-in row adds `You` after the username. Compose tag `gamblerScoreRow:<gamblerId>` | 13 rows, all `Rank unchanged`. The signed-in row (`Rank 8, felipearpa, You, 589 points, Rank unchanged`) has no click action; every other row is clickable. The top bar's pool-switch icon (top right) is clickable but has no content description, which predates this change. |
| Another gambler's Timeline | Tap a non-current leaderboard row (C.A.Vélez); wait about 6 s | Top bar `Back`, text `Timeline`, `Home`; identity text `C.A.Vélez`; summary `C.A.Vélez earned 573 points in this pool`; group header `<Weekday>, <Mon> <d>, <h:mm a>. <n> match(es). <movement>`; rows `<date>, <time>. Final score: <home> <n>, <away> <n>. Bet: <a> to <b>. <n> points` or `No bet placed` | Headers are not actions; each row is clickable. Two-match group-stage kickoffs read, for example, `Saturday, Jun 27, 9:00 PM. 2 matches. Rank unchanged`, with the visible line `9:00 PM · 2 matches`. Movement seen: `Rank unchanged`, `Up 3 places`, `Up 1 place`, `Down 1 place`, and `Movement unavailable` on the opening match (Thursday, Jun 11, 2:00 PM). The newest groups are `Rank unchanged`, matching the leaderboard. |
| Match screen | Tap a Timeline or History row | Top bar `Back`, text `Score`, `Home`; heading `Final score: <home> <n>, <away> <n>. <date> · <time>`; rows `<username>, Bet: <home> <n>, <away> <n>, <n> points` | Each child row opens its own match (`GET …/gamblers/{id}/matches/{matchId}` and `…/matches/{matchId}/bets?sort=pool-score-desc`). In-app `Back` returns to the Timeline at the same scroll position. |
| History tab | Tap tab text `History`; wait about 6 s | Summary `589 points earned in this pool` (visible `+589 pts earned`); the same header and row descriptions as Timeline, with `Your bet: <a> to <b>` | The newest group (Sunday, Jul 19, España – Argentina) is `Rank unchanged`, as the signed-in leaderboard row is; Saturday, Jul 18 is `Down 1 place`. Two-match groups (for example Thursday, Jun 25, 6:00 PM) show the rail, the internal divider, and `2 matches`. |

Waits used: about 6 s after each navigation for the first page, with a fresh `android layout --no-idle` read before acting. Scrolling uses `adb shell input swipe 300 2300 300 600 <ms>`, started away from the left edge: swipes at x = 40 px fall in the system back-gesture zone. One fast swipe (250 ms) registered as a tap while the emulator was lagging and opened a match.

Network observations:

- Timeline: one `bets/timeline` request returned the whole feed; scrolling from the newest group to the last one (Thursday, Jun 11, 2:00 PM) made no further feed request. A full finished tournament fits in one server page, so live paging cannot be observed with this data (task 5.2).
- History pull to refresh: two requests, the score total (`GET pools/{poolId}/gamblers/{gamblerId}`) and `bets/finished`, both 200.
- Summary-only retry on Timeline: the first total request timed out (`SocketTimeoutException`) while the emulator was stalled, and the summary showed `Couldn't load points.` with a `Retry` button while the groups stayed visible. Tapping `Retry` sent exactly one request, to the score endpoint (200), and no feed request; the summary then read `C.A.Vélez earned 573 points in this pool`. No capture was kept of the failed state.

Edge-to-edge, light theme, portrait, font scale 1.0:

- Leaderboard: the first row sits below the top bar; at the scroll limit, the last row (rank 13) ends above the tab bar (`leaderboard-light-end.png`). The top bar collapses on scroll and the status bar gets a tinted fill.
- Timeline: at the scroll limit, the last group's row ends above the gesture navigation bar (`timeline-light-end.png`).
- History: the first group sits below the summary, and the summary below the top bar (`history-light-top.png`). At the scroll limit the last group, the opening match (Thursday, Jun 11, 2:00 PM, `Movement unavailable`), ends at y = 2703 px, above the tab bar, whose labels start at y = 2847 px (`history-light-end.png`, `history-dark-end.png`; checked at 14:42 after the relaunch below). Scrolling the whole History made no further feed request.

Captures in [verification/android/live](verification/android/live) (real account data): `my-pools-light.png`, `leaderboard-light-top.png`, `leaderboard-light-end.png`, `timeline-light-top.png`, `timeline-light-two-match-group.png`, `timeline-light-end.png`, `history-light-top.png`, `history-light-end.png`.

### Emulator failures

- At 12:14 the emulator crashed during the first ARTEMIS attempt. The next boot restored its 08:17 quickboot snapshot, which discarded the user's sign-in and the 12:07 install. Two relaunches hung on the emulator's crash-report consent dialog until the dump was moved out of `/tmp/android-felipe/emu-crash-37.2.12.db/completed/`.
- ARTEMIS had installed its accessibility helper (`com.artemis.helper`) and enabled it as an accessibility service. It was uninstalled and the service setting deleted (`adb -s emulator-5554 shell settings delete secure enabled_accessibility_services`; `accessibility_enabled` set back to 0; `adb -s emulator-5554 uninstall com.artemis.helper`).
- From about 13:15, with the host's load average between 35 and 60 on 10 cores (other agents' Xcode builds and iOS simulators), System UI and then Fortuna repeatedly showed "isn't responding". System UI was restarted once through its dialog's `Close app`; every other dialog got `Wait`. At 13:37 the emulator crashed again (dump moved to the session scratchpad). The emulator was not relaunched: after the first crash the next boot restored the 08:17 snapshot without the sign-in, so this one is expected to lose it too, and the user asked to stop in that case.

The emulator was relaunched at 14:27 with the host's load average at 5, the current build was installed again with `install -r`, and the user signed in again. The remaining checks are in the task 4.6 section.

## Android — task 4.6 checks (2026-10-13)

Device: the Pixel_10_Pro_XL emulator (API 37.2, `emulator-5554`), relaunched at 14:27 through `mobile_diagnose(launch_avd="Pixel_10_Pro_XL")` with the host's load average at 5 (it stayed between 5 and 17 throughout). The Prod debug build was installed with `adb -s emulator-5554 install -r Android/app/build/outputs/apk/prod/debug/app-prod-debug.apk`, and the user signed in by hand. Every `adb` and Gradle command named the emulator (`-s emulator-5554`, `ANDROID_SERIAL=emulator-5554`). The checks below follow the locators and waits recorded in the task 4.1 section, using the `android` CLI and adb instead of ARTEMIS, as the user decided on 2026-10-13 (ARTEMIS's Gemini credits are depleted).

Starting values, recorded first: `font_scale` 1.0; night mode off; `wm size` 1344x2992; `wm density` 480; `user_rotation` 0; `accelerometer_rotation` 1; `animator_duration_scale` unset; `transition_animation_scale` and `window_animation_scale` 1.0; no accessibility service enabled.

Commands used and their restores:

| Change | Command | Restore |
| --- | --- | --- |
| Dark theme | `adb -s emulator-5554 shell cmd uimode night yes` | `… cmd uimode night no` |
| Font scale 2.0 | `adb -s emulator-5554 shell settings put system font_scale 2.0` | `… settings put system font_scale 1.0` |
| Landscape | `… settings put system accelerometer_rotation 0`; `… settings put system user_rotation 1` | `… settings put system user_rotation 0`; `… settings put system accelerometer_rotation 1` |
| 320 dp wide window | `adb -s emulator-5554 shell wm density 672` (1344 px / 4.2) | `… wm density reset` |
| 640 dp wide window | `adb -s emulator-5554 shell wm density 336` | `… wm density reset` |

Values read back at the end match the starting values. Scrolling used `adb shell input swipe` (600–800 ms) started away from the left back-gesture edge. A slow drag built from `input motionevent DOWN/MOVE/UP` avoids taps on a lagging device; it was used in portrait.

### Hand checks on the signed-in app

| Check | Result | Captures |
| --- | --- | --- |
| Light theme, font scale 1.0, portrait | Covered by the task 4.1 section, including the first and last content of the leaderboard, Timeline, and History. | 4.1 captures |
| Dark theme | History, the leaderboard, and Timeline render with the dark trend colors: steady yellow bar, red down arrow, green up arrow, gray `Unavailable` pill. The signed-in leaderboard row uses the dark green current-user fill. The last History group ends above the tab bar. Status-bar icons are light over both the canvas and the tinted bar of a scrolled list. One capture taken while the theme was still switching showed dark icons; a later capture of the same state showed light ones. | `history-dark-end.png`, `history-dark-top.png`, `leaderboard-dark-top.png`, `timeline-dark-top.png` |
| Font scale 2.0 (dark) | The leaderboard rows reflow: the rank tile, avatar, and score on the first line, the trend under the tile, and the username at full width. The last row (rank 13) ends at y = 2652 px, above the tab bar, whose label starts at y = 2805 px. Timeline and History rows stack the teams around the result; headers keep the date, time, and badge without clipping. My pools moves the name and counts below the rank tile and invite control. Nothing is truncated. | `leaderboard-dark-fs2-top.png`, `leaderboard-dark-fs2-end.png`, `timeline-dark-fs2-top.png`, `history-dark-fs2.png`, `my-pools-dark-fs2.png` |
| Landscape (2992 × 1344 px) | My pools, the leaderboard, and History lay out at full width, with content inset from the display cutout on the leading edge. At the scroll limit the leaderboard's last row ends above the tab bar. As on the iPad, rows have no readable-width limit, so team names and the trend badge sit far apart; this change did not introduce one. | `my-pools-light-landscape.png`, `leaderboard-light-landscape-top.png`, `leaderboard-light-landscape-end.png`, `history-light-landscape.png` |
| 320 dp wide window | History keeps three columns for these team names, with the header badge on the date line. | `history-light-320dp.png` |
| 640 dp wide window | History and the leaderboard keep their phone layout at the wider width; all 13 leaderboard rows fit above the tab bar. | `history-light-640dp.png`, `leaderboard-light-640dp.png` |
| Pull to refresh | My pools: one request, `GET gamblers/{gamblerId}/pools`, 200. Leaderboard: one request, `GET pools/{poolId}/gamblers`, 200. History (task 4.1): the total and `bets/finished`, both 200. | — |
| Edge-to-edge scroll limits | First content below the top bar and last content above the tab bar or navigation bar on My pools, the leaderboard, History, and Timeline, in light and dark, at font scale 1.0 and 2.0, and in landscape. | as above |

### UI tests added

Both use Compose semantics locators (content descriptions, headings, test tags), wait for the first page with `waitUntil`, and need no coordinate fallback. They run on a signed-out app with in-memory `PagingData`, like the existing module tests.

- `bet` `finished.HistoryGroupedListTest` (new):
  - `aTwoMatchHeaderIsOneHeadingWithItsCountAndMovementAndEachRowOpensItsOwnMatch` checks one heading `…. 2 matches. Rank unchanged` with no click action, and each child row opening its own match, as seen live on History and Timeline.
  - `theLastGroupEndsAboveTheBottomPaddingAtTheScrollLimit` scrolls `FinishedBetList` to its end with a bottom content padding like the tab shell's and requires the last row to end within the section's bottom spacing above the padded end.
- `pool` `gamblerscore.GamblerScoreListTest.theLastGamblerEndsAboveTheBottomPaddingAtTheScrollLimit` (added): the same scroll-limit check for the leaderboard, using the `gamblerScoreRow:<gamblerId>` tag.

### Connected tests, class by class

Command for each class, from `Android/`:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :<module>:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=<fully.qualified.Class> \
  -Dorg.gradle.jvmargs="-Xmx4g -Dfile.encoding=UTF-8"
# app: :app:connectedProdDebugAndroidTest with the same arguments; it uninstalls Fortuna afterwards
```

| Module | Class | Result |
| --- | --- | --- |
| bet | `finished.HistoryGroupedListTest` (new) | 2 passed |
| bet | `finished.HistoryPresentationTest` | 20 passed |
| bet | `finished.HistoryListRetryTest` | 2 passed |
| bet | `finished.HistoryPointsRetryTest` | 6 passed |
| bet | `timeline.BetTimelineListTest` | 5 passed |
| bet | `BetPlaceholderAccessibilityTest` | 3 passed |
| pool | `gamblerscore.GamblerScoreListTest` (one test added) | 4 passed |
| pool | `gamblerscore.GamblerScorePlaceholderTest` | 6 passed |
| pool | `gamblerscore.GamblerScoreItemRankPreviewTest` | 3 passed |
| pool | `poolscore.PoolScoreItemAccessibilityTest` | 11 passed |
| pool | `poolscore.PoolScoreListAccessibilityTest` | 3 passed |
| pool | `poolscore.PoolScoreListInsetsTest` | 3 passed |
| pool | `poolscore.PoolScoreTemplatesRetryTest` | 1 passed |
| ui | `ProgressIndicatorTest` | 6 passed |
| ui | `theme.LoadingPlaceholderPulseMotionTest` | 2 passed |
| ui | `lazy.RefreshableLazyPagingColumnCompanionRefreshTest` | 3 passed |
| ui | `lazy.RefreshableLazyPagingColumnInsetsTest` | 7 passed |
| ui | `lazy.LazyPagingColumnErrorRetryTest` | 3 passed |
| app | `PoolHomeTabSwitchAppBarTest` | 1 failed, twice |

`PoolHomeTabSwitchAppBarTest` failed in its screen capture: first `Failed waiting for PixelCopy!`, then `ComposeTimeoutException` in `WindowCapture.forceRedraw`. It reads the bar color with `captureToImage`, which this software-rendered AVD does not complete reliably. The same PixelCopy failure appeared earlier in `GamblerScorePlaceholderTest` on this AVD. This change does not touch `PoolHomeView` or the test, so the failure is classified as an environment failure, not caused by this change. The known unrelated suites (`PushDrawerAccessibilityTest`, `PoolNameKeyboardTest`, `PoolJoinSafeBoundsTest`) were not run.

### Unit tests

`./gradlew testDebugUnitTest --rerun :app:testProdDebugUnitTest --rerun -Dorg.gradle.jvmargs="-Xmx4g -Dfile.encoding=UTF-8"`: 716 tests, 0 failures, 0 errors.

### Documentation

`Android/bet/README.md` and `Android/pool/README.md` are new, modelled on the iOS package READMEs. They describe the grouped feed contract, paging rules, movement states, presentation, accessibility, and the independent points summary for Bet, and the `trendGroup` score contract, movement table, announcements, large-text reflow, and placeholders for Pool. Each ends with the commands to reproduce the checks.

### End state

The app connected test uninstalled Fortuna, which drops the user's sign-in on this emulator. No test APK remains installed. The ARTEMIS accessibility helper is not installed. The emulator was stopped by killing its process (`kill -9 <qemu pid>`), not with `adb emu kill`, so no quickboot snapshot was saved: `snapshots/default_boot/ram.bin` still dates from 08:17. No crash dump or AVD lock file was left.

Not verified:

- API 28 and API 36. Only the API 37.2 image is available; the user accepted this.
- TalkBack speaking on a device. Announcements were read from the accessibility tree (`android layout`) and checked by the Compose tests above.
- Live partial (`1 of 2 computed`) and pending movement, multi-page appends, and the invalid-cursor restart. Production holds only a finished tournament, and the whole History and Timeline arrive in one page; task 5.1 and 5.2 cover these.
- Reduced motion in the running app. It was covered with `animator_duration_scale 0` in the 4.3–4.5 round and was not repeated here.
- `PoolHomeTabSwitchAppBarTest`, which cannot capture the window on this AVD.

Accepted with exploration through the `android` CLI and adb instead of ARTEMIS (Gemini credits depleted) by the user on 2026-10-13.
Accepted without API 28 and 36, TalkBack speech, live reduced motion, `PoolHomeTabSwitchAppBarTest` (PixelCopy capture fails on the software-rendered emulator), and live partial/pending movement and paging by the user on 2026-10-13; live partial/pending movement and paging are covered by tasks 5.1 and 5.2.

## Backend — task 5.2 paging against DynamoDB (2026-10-13)

Environment: account 567776770335, `us-east-2`, run from the development Mac with the default AWS CLI profile. Only the two temporary tables below were created; nothing was deployed and no production table or queue was read or written.

### Commands

```bash
cd Amazon.Lambda/operations
AWS_REGION=us-east-2 ./pool-table.sh create-copy Pool-verify-20261013 PoolLayout-verify-20261013
# every Pool index and the layout index ACTIVE before the run (describe-table)
cd ../Felipearpa.Tyche.Verification
POOL_TABLE_NAME=Pool-verify-20261013 POOL_LAYOUT_TABLE_NAME=PoolLayout-verify-20261013 AWS_REGION=us-east-2 \
  dotnet run -- pagination ../../openspec/changes/group-kickoff-rank-movement/verification/backend/results.json
aws dynamodb delete-table --region us-east-2 --table-name Pool-verify-20261013
aws dynamodb delete-table --region us-east-2 --table-name PoolLayout-verify-20261013
aws dynamodb wait table-not-exists --region us-east-2 --table-name Pool-verify-20261013
aws dynamodb wait table-not-exists --region us-east-2 --table-name PoolLayout-verify-20261013
aws dynamodb list-tables --region us-east-2
```

Result of the recorded run: exit code 0, 59 of 59 checks passed ([verification/backend/results.json](verification/backend/results.json)). Fixture pools `01M4VH7QDY5XJ9DHDQNFG0WQTD` (shared-codec fixture) and `01M4VH8F0KF0MK421QJ5GNCSSV` (grouped fixture).

### What the harness does for 5.2

The `pagination` mode in `Felipearpa.Tyche.Verification/Pagination.fs` now also seeds a grouped fixture and runs four checks on it. They call the production application services the endpoints use, `GetFinishedPoolGamblerBets` (History) and `GetGamblerBetsTimeline` (Timeline), over `PoolGamblerBetDynamoDbRepository`, `GroupedBetFeedRequestBuilder`, and `KickoffGroupReader`, unchanged. The request builders have no page-size setting, so storage pages are forced small by the harness's existing client, which gives every query without its own `Limit` a limit of two items; the production requests are unchanged. Every token goes through URL encoding into `?next=` and back, as a client sends it. Each traversal is polled with a three-minute deadline until the indexes converge; all converged on the first attempt.

Grouped fixture: one gambler, one pool, one layout, five kickoff groups at 10:00–14:00 UTC on one calendar day, 11 bets and 11 layout matches. Computed bets carry `computedDateTime`, `computedMatchDateTime`, and `beforePosition`/`position` snapshots, so each group's movement is 8 → 8 − (computed children).

| Group | Matches | With a result at seeding |
|---|---|---|
| K0 | 1 | 1 |
| K1 | 2 | 1 (the second gets its result during the run) |
| K2 | 4 | 4 |
| K3 | 2 | 2 |
| K4 | 2 | 0 |

### Checks and results

Pages below are written `phase[group computed/total ×rows]`; `first` is the request without a token, `token`, `live`, and `finished` the requests with a returned token.

| Check | What it asserted | Result |
|---|---|---|
| (a) History: storage page ends inside a kickoff group | The full traversal returns K3, K2, K1, K0 once each, newest kickoff first, with the layout's counts, computed rows only, rows in match-ID order, and the expected movement. Replaying the token of the page that returned K2 against the storage query (`GetFinishedBetsByKickoffAsync`) returns only part of K2 with a continuation; the History page itself returns all 4 rows; no later page contains K2. | Pass. Pages `first[K3 2/2 ×2] \| token[K2 4/4 ×4] \| token[K1 1/2 ×1, K0 1/1 ×1] \| token[]`. The storage page behind page 2 held 2 of K2's 4 children and continued; K2 came back with 4 rows, counts 4/4, movement 8 → 4. |
| (b) Timeline: partial group in the unsettled phase | Unsettled groups K1, K4 oldest first with every row, then settled K3, K2, K0 newest first, each once. K1 appears once, on a page served from the unsettled phase, with both rows (one computed, one pending) and counts 1/2; the finished phase skips K1's computed row. Both phases issue tokens, so each spans several pages. | Pass. Pages `first[] \| live[K1 1/2 ×2] \| live[] \| live[] \| live[] \| live[K4 0/2 ×2, K3 2/2 ×2] \| finished[K2 4/4 ×4] \| finished[K0 1/1 ×1] \| finished[]`. Empty unsettled pages kept their `live:` token; the page where the unsettled phase ended carried the first settled group. |
| (b) Timeline: group after its last result in the finished phase | The harness then writes K1's last result as settlement leaves it: the layout match's score and `resultVersion`, and the bet's computed attributes and snapshots (`PutItem` on the temporary tables; no settlement pipeline runs in this mode). A fresh traversal returns K4 in the unsettled phase, then K3, K2, K1, K0; K1 appears once, among the settled groups, with both rows and counts 2/2. History then shows K1 with both rows. | Pass. Timeline `first[] \| live[] \| live[] \| live[] \| live[K4 0/2 ×2] \| live[K3 2/2 ×2] \| finished[K2 4/4 ×4] \| finished[K1 2/2 ×2] \| finished[K0 1/1 ×1]`; History `first[K3 2/2 ×2] \| token[K2 4/4 ×4] \| token[K1 2/2 ×2] \| token[K0 1/1 ×1]`. |
| (c) Old `computedDateTime` token rejected, first page reloads | A token issued from the old `GetFinishedPoolGamblerBets-index` (a real `LastEvaluatedKey` of a two-item query, encoded by the production `DynamoDbKeySerializer`) is sent as a History append and as a Timeline `finished:` append. Both fail with `InvalidContinuationException (MissingKeyAttribute computedMatchDateTime)` while the request is built, before any query runs; the hosts map that exception to 400 `INVALID_CURSOR`. One History request without a token then returns the first page, K3, with a token that fits the finished-by-kickoff start-key contract and reads K2. A Timeline request without a token succeeds as well. | Pass. Old token keys `computedDateTime:S, pk:S, sk:S`. Timeline's first page held no groups and a `live:` token, because its first storage page holds only computed details. |

The run's other 55 checks (the shared-codec operations, Timeline phases on the original fixture, ranked match bets, settlement discovery, username propagation) passed as well.

### Harness changes

- `Pagination.fs`: the grouped fixture and the four checks above, run after settlement discovery and before username propagation.
- `Pagination.fs`, repeated-traversal check of the shared-codec operations: two earlier runs on the same day failed it for different operations each time (unsettled bets by kickoff, pool leaderboard, gamblers by layout, finished bets by kickoff). A diagnostic run showed the cause: when the last items exactly fill a two-item storage page, DynamoDB sometimes returns a `LastEvaluatedKey` with them and sometimes does not, so one traversal ended on that page and the other with an extra empty page. Items, boundaries, and every other token were identical. The check now treats those two endings as the same traversal and reports the first differing page when it fails.
- The open-layouts check needs fresh tables: its index is shared by every layout in the table, so a second run on the same tables finds the first run's layouts. The recorded run used newly created tables.

### Client half of 5.2

Not run on devices for this record. The client behavior is covered by these existing unit tests:

- iOS `Bet` `KickoffGroupPagingTests`: `historyAppendsWholeGroupsInServerOrderWithTheirServerCountsAndMovement`, `aTimelineGroupThatSettlesBetweenPagesAppearsOnceAndItsSettledCopyAfterARefresh`, `aPageLeftEmptyByARepeatedGroupContinuesToTheNextPage`, `aRejectedContinuationReloadsTheFirstPageOnceWhileTheGroupsStayVisible`, `aSecondRejectionInARowShowsTheRetryInsteadOfRestartingAgain`, `timelineRestartsARejectedContinuationTheSameWay`, `anOrdinaryAppendFailureRetriesTheSameContinuation`.
- iOS `DataBet` `KickoffGroupFeedTests`: a partial group keeps the server's counts, `INVALID_CURSOR` maps to `cursorRejected`, and the repository passes a rejected continuation through.
- Android `bet` `kickoffgroup.KickoffGroupFeedTest`: groups follow in server order with the server's counts; a group that settles while paging is shown once; a page whose groups were all shown still continues; a partial unsettled group becomes finished after a refresh; a rejected continuation restarts from the first page once; a second rejection offers a restart and stops requesting; an ordinary failed append retries the same continuation.
- Android `data:bet` `infrastructure.KickoffGroupFeedRequestTest`: decoding keeps the server's counts and rows; a rejected cursor on either feed asks for a restart.
- Backend: `Felipearpa.Tyche.Pool/test/Application/GroupedBetFeedTest.fs`, and `InvalidCursorTest.fs` in the Lambda and HttpApi tests for the endpoint-level 400 `INVALID_CURSOR` mapping.

### Cleanup

Both temporary tables were deleted and `wait table-not-exists` returned for each. `list-tables` in `us-east-2` afterwards shows only `Account`, `MatchScoreIngestion`, `Pool`, and `PoolLayout`. The same two table names had been created once before in this session for the two failing runs and the diagnostic run, then deleted and created again for the recorded run.

Not verified here:

- The endpoint-level 400 `INVALID_CURSOR` body against DynamoDB; it is covered by the unit tests above.
- Both apps on devices against these pages; covered by the client unit tests above.
- A group's last result arriving in the middle of one Timeline traversal. The spec gives no snapshot guarantee for data that changes between requests; the check traverses before and after the result.

Accepted without the client half of task 5.2 on devices (each group once with full counts, old-token rejection and first-page recovery), covered by the iOS and Android unit tests listed above, by the user on 2026-10-13.

## Backend — task 5.1 settlement sequence on AWS (2026-10-13)

Scope: backend only, as the user decided. The app half (both apps showing pending, then `+n, 1 of 2`, then `2 of 2` on History, Timeline, the leaderboard, and My pools) is not verified on devices; the client tests listed in the task 5.2 section cover how the apps render these server responses.

Environment: account 567776770335, `us-east-2`, run from the development Mac with the default AWS CLI profile. Nothing was deployed; no production table or queue was read or written. The harness's refusal of production names is unchanged.

### Resources and commands

| Resource | Settings |
|---|---|
| `Pool-verify-20261013`, `PoolLayout-verify-20261013` | `pool-table.sh create-copy` (streams `NEW_AND_OLD_IMAGES`, all indexes `ACTIVE` before the run) |
| `PoolCommands-verify-20261013.fifo` | FIFO, visibility 300 s, retention 1 day, redrive to the DLQ after 30 receives |
| `PoolCommands-verify-20261013-dlq.fifo` | FIFO, retention 1 day |
| `PoolMaintenance-verify-20261013` | Standard, visibility 120 s, retention 1 day, redrive to the DLQ after 5 receives |
| `PoolMaintenance-verify-20261013-dlq` | Standard, retention 1 day |

The visibility timeouts are shorter than production's (1800 s and 720 s) so that a failed delivery in the harness is retried within the run; no delivery failed.

```bash
cd Amazon.Lambda/operations
AWS_REGION=us-east-2 ./pool-table.sh create-copy Pool-verify-20261013 PoolLayout-verify-20261013
aws sqs create-queue --region us-east-2 --queue-name PoolCommands-verify-20261013-dlq.fifo --attributes FifoQueue=true,MessageRetentionPeriod=86400
aws sqs create-queue --region us-east-2 --queue-name PoolCommands-verify-20261013.fifo --attributes file://cmd.json
#   cmd.json: FifoQueue=true, VisibilityTimeout=300, MessageRetentionPeriod=86400,
#             RedrivePolicy={deadLetterTargetArn: <dlq arn>, maxReceiveCount: 30}
aws sqs create-queue --region us-east-2 --queue-name PoolMaintenance-verify-20261013-dlq --attributes MessageRetentionPeriod=86400
aws sqs create-queue --region us-east-2 --queue-name PoolMaintenance-verify-20261013 --attributes file://mnt.json
#   mnt.json: VisibilityTimeout=120, MessageRetentionPeriod=86400,
#             RedrivePolicy={deadLetterTargetArn: <dlq arn>, maxReceiveCount: 5}
cd ../Felipearpa.Tyche.Verification
Q=https://sqs.us-east-2.amazonaws.com/567776770335
POOL_TABLE_NAME=Pool-verify-20261013 POOL_LAYOUT_TABLE_NAME=PoolLayout-verify-20261013 \
POOL_COMMAND_QUEUE_URL=$Q/PoolCommands-verify-20261013.fifo POOL_COMMAND_DLQ_URL=$Q/PoolCommands-verify-20261013-dlq.fifo \
POOL_MAINTENANCE_QUEUE_URL=$Q/PoolMaintenance-verify-20261013 AWS_REGION=us-east-2 \
  dotnet run -- kickoff-groups ../../openspec/changes/group-kickoff-rank-movement/verification/backend/task-5.1-results.json
aws sqs delete-queue --region us-east-2 --queue-url $Q/<each of the four queues>
aws dynamodb delete-table --region us-east-2 --table-name Pool-verify-20261013
aws dynamodb delete-table --region us-east-2 --table-name PoolLayout-verify-20261013
aws dynamodb wait table-not-exists --region us-east-2 --table-name <each table>
aws dynamodb list-tables --region us-east-2
aws sqs list-queues --region us-east-2 --queue-name-prefix Pool
```

Result: two runs on the same resources, each under fresh IDs, both exit 0 with 37 of 37 checks passed. The recorded run (pool `01M4VJ6REPBZFYE0RZ2MZSSBDC`) is in [verification/backend/task-5.1-results.json](verification/backend/task-5.1-results.json), with its check lines and the `KickoffCopiesMoved` metric lines in [task-5.1-checks.txt](verification/backend/task-5.1-checks.txt). Every read matched its expectation within 1 s of polling. No stream batch needed a retry.

### How the harness runs it

The new `kickoff-groups` mode (`Felipearpa.Tyche.Verification/KickoffGroupSequence.fs`) uses the existing `Pipeline`, which runs the production stream handlers (`PoolLayoutEvent`, `PoolEvent`, `MembershipEvent`) on genuine stream records and the production queue handlers (`PoolOperationEvent`, `PoolMaintenanceEvent`) on the real queues, until the pipeline is idle after each step.

- Fixture: gamblers A and T (A sorts first by ID), one pool, a layout with M_old at K1 (6 days ago, 18:00 UTC), M_a and M_b at K2 (5 days ago, 20:00), and M_late at K3 (4 days ago, 20:00). The layout matches are seeded directly into the layout table.
- Pool: A creates it with `PoolDynamoDbRepository.CreatePoolAsync` and T joins with `JoinPoolAsync`, the writes the endpoints make. The stream handlers then materialized both bet sheets and ran the join standings and restorations.
- Predictions: written to the materialized details with the same `homeTeamBet`/`awayTeamBet` update as `BetAsync`, without its lock condition. The kickoffs are in the past so Timeline reveals the rows with the real clock; `BetAsync` would refuse them. This is the one harness write in the flow.
- Results: `PoolLayoutDynamoDbRepository.SetMatchFinalScoreAsync`, the result write, then the PoolLayout stream → `DispatchMatchResult` → FIFO → `ProcessPoolCommand`.
- Postponement: an `UpdateItem` of M_late's `matchDateTime` to K3 + 1 day, as in the operator procedure ("Reschedule a match"), then the PoolLayout stream → `PropagateKickoffChange`.
- Reads, after each step, through the services the endpoints use: `GetPoolGamblerScoresByPool` (leaderboard) and `GetPoolGamblerScoresByGambler` (My pools for A and T), both with `PoolStandings`' `trendGroup`; `GetFinishedPoolGamblerBets` (History of A and T); `GetGamblerBetsTimeline` for T, the Timeline A opens (the endpoint's membership check is not part of this read); the root's `trendKickoffDateTime` (`GetRootAsync`); and each match's `beforePosition`/`position` snapshot on both details, read strongly. Index reads are polled for up to 60 s.

Scoring: winner 10, each exact team score 4, same margin 2 (`BetEvaluator`). Bets and results (A / T): M_old 1–0 / 0–2, result 0–2 (0 / 20); M_a 3–0 / 2–1, result 2–1 (10 / 20); M_b 1–1 / 2–0, result 1–1 (20 / 0); M_late 1–0 / 0–0, no result.

### Checks and results

Rows are `gambler points, position, beforePosition, trendGroup`; groups are `kickoff computed/total [rows] movement`.

| Step | Assertion | Result |
|---|---|---|
| Setup | Pool created and joined | Pass |
| 0, no result | Leaderboard A 0 pts position 1, T 0 pts position 2, no `beforePosition`, no `trendGroup`; My pools rows for A and T without `trendGroup`; root has no trend kickoff; A's History is empty; T's Timeline is K1 0/1 [M_old], K2 0/2 [M_a, M_b], K3 0/1 [M_late], all without movement | Pass |
| 1, M_a 2–1 | Leaderboard and My pools: T 20, 1, 2, K2 1/2; A 10, 2, 1, K2 1/2. Root trend kickoff K2. A's History K2 1/2 [M_a] 1→2; T's History K2 1/2 [M_a] 2→1. T's Timeline K1 0/1, then K2 1/2 [M_a, M_b] 2→1 in the unsettled phase, then K3 0/1. M_a snapshots A 1→2, T 2→1 | Pass |
| 2, M_b 1–1 | A 30, 1, 1, K2 2/2; T 20, 2, 2, K2 2/2 (same baseline as step 1). Root K2. A's History K2 2/2 [M_a, M_b] 1→1; T's 2→2. T's Timeline K1 0/1, K3 0/1 (unsettled), then K2 2/2 [M_a, M_b] 2→2 once, in the finished phase. M_b snapshots A 2→1, T 1→2 | Pass |
| 3, M_old 0–2 (late, older K1) | T 40, 1, 2, K2 2/2; A 30, 2, 1, K2 2/2. Root trend kickoff stays K2, so the older result does not change the trend group; the movement it causes shows against the K2 baseline. A's History K2 2/2 1→1, then K1 1/1 [M_old] 1→2; T's K2 2→2, then K1 2→1. T's Timeline K3 0/1 (unsettled), then K2 2/2 2→2 and K1 1/1 2→1. M_old snapshots A 1→2, T 2→1; M_a and M_b snapshots unchanged | Pass |
| 4, M_late to K3 + 1 day | Both M_late details hold the new kickoff (strong reads). Leaderboard and My pools rows and the root trend kickoff (K2) are unchanged from step 3. T's Timeline has K3+1d 0/1 [M_late] in the unsettled phase, then K2 and K1, and no group at K3. A's History is unchanged | Pass |
| 4, second delivery | The same postponement record, read again from the PoolLayout stream from its start, was delivered to a new `PoolLayoutEvent` handler: no batch failure, no Pool stream record in the following 7 s, and both M_late details unchanged. `KickoffCopiesMoved` was 2 for the first delivery and 0 for the second | Pass |

### Cleanup

The four queues were deleted (`delete-queue` returned for each), and both tables were deleted with `wait table-not-exists` returning for each. Afterwards `list-tables` shows only `Account`, `MatchScoreIngestion`, `Pool`, and `PoolLayout`, and `list-queues --queue-name-prefix Pool` shows only the production `PoolMaintenanceQueue` and `PoolMaintenanceQueue-dlq`.

Not verified:

- Both apps on devices for this sequence (user decision; client tests cover rendering).
- Predictions through `BetAsync`: the lock rule rejects bets on past kickoffs, and past kickoffs are what let Timeline reveal the rows with the real clock.
- The endpoint layer (HTTP routing, authentication, the Timeline membership check, JSON shape of `trendGroup` and group items); this run reads the services the endpoints call.
- Ingestion rescheduling (`MatchScoreIngestion`), which remains an operator step.

Accepted without the 5.1 sequence on devices in both apps, the HTTP layer (routing, authentication, JSON shape; covered by endpoint unit tests), bets placed through `BetAsync`, and rescheduling score ingestion for a postponed match (an operator step) by the user on 2026-10-13.

## Final review — task 5.3 (2026-10-13)

Review only: no product code, device, simulator, or AWS resource was touched. Screenshots were inspected as images (downscaled contact sheets, plus crops at full resolution), not by file name. Trend colors in captures were sampled from the PNG pixels with Pillow.

### `openspec validate`

```text
$ openspec --version
1.13.2
$ openspec validate group-kickoff-rank-movement
Change 'group-kickoff-rank-movement' is valid
$ openspec validate group-kickoff-rank-movement --strict
Change 'group-kickoff-rank-movement' is valid
$ openspec validate group-kickoff-rank-movement --strict --json
{"items":[{"id":"group-kickoff-rank-movement","type":"change","valid":true,"issues":[]}], "summary":{"totals":{"items":1,"passed":1,"failed":0}}}   # abridged
```

All three exit 0.

### Backend test run

The CI command (`.github/workflows/dotnet-unit-tests.yml`), run from `Amazon.Lambda/` with `AWS_PROFILE` pointed at a profile that does not exist so that no test can reach AWS:

```bash
dotnet test Felipearpa.Tyche.slnx --nologo --verbosity minimal
```

Result: 644 tests, 0 failed, 0 skipped (Pool 299, AmazonLambda 124, Data.DynamoDb 104, Type 54, HttpApi 27, Account 19, PoolLayout 13, Core 2, Crypto 1, Function 1).

### Icon contract

| Check | Result |
| --- | --- |
| Canonical sources | `assets/icons/arrow_upward.svg`, `arrow_downward.svg`, `horizontal_rule.svg` exist; SHA-256 `a902b444…`, `480a48a9…`, `e909c03a…` match `assets/icons/README.md`. |
| iOS assets | `arrow_upward.imageset/arrow_upward.svg`, `arrow_downward.imageset/arrow_downward.svg`, `horizontal_rule.imageset/horizontal_rule.svg` have the same SHA-256 as the canonical files. Each `Contents.json` sets `template-rendering-intent: template` and `preserves-vector-representation: true`. The old `*_FILL0_wght400_GRAD0_opsz24.svg` files are deleted. |
| Android assets | `Android/ui/src/main/res/drawable/arrow_upward.xml`, `arrow_downward.xml`, `horizontal_rule.xml` carry each SVG's `d` path unchanged (compared as text), with a 960 × 960 viewport and `translateY="960"`. The rounded `ic_arrow_upward.xml`, `ic_arrow_downward.xml`, and `ic_horizontal_rule.xml` are deleted, and nothing references them. |
| Consumers | Only `TrendIndicator` uses them: `iOS/UI/Sources/UI/TrendIndicator.swift` (`.arrowUpward`, `.arrowDownward`, `.horizontalRule`) and `Android/ui/src/main/java/com/felipearpa/tyche/ui/TrendIndicator.kt` (`R.drawable.arrow_upward`, `arrow_downward`, `horizontal_rule`). |
| Navigation assets | `git diff main` over every `*.xcassets` and `res/drawable*` path changes only the three trend icons and the three trend color sets. The live captures keep the existing tab glyphs (Scores, Bets, History) and the pool-switch glyph. The reference's trophy, sports_score, history, and swap_horiz repairs were not adopted, as the specs require. |
| Material Symbols | All three are Material Symbols (Outlined, weight 400). Comparison with the upstream files was accepted as not done on 2026-10-12 (iOS 3.1–3.5 section). |
| Rendered glyphs | In the iOS and Android captures the arrows are the same thin outlined Material Symbols geometry, and the steady symbol is the same flat rule. The reference's heavier raster arrows are illustrative only (`reference/README.md`). |

The canonical SVGs, the new image-set SVGs, and the new drawables are untracked in git (`??`). They need to be part of the commit.

### Screenshot review against `reference/history-grouped-icons.png`

Reviewed: every PNG in `verification/ios` (27), `verification/ios/live` (28), `verification/android` (84), and `verification/android/live` (24).

| Aspect | iOS | Android |
| --- | --- | --- |
| Header: date on line 1, time and pluralized count on line 2, trailing trend pill | Matches the reference (`history-groups-*`, `se-history-*`, `ipad-history-*`). A one-match group omits the count on both platforms. Different times on one date are separate groups (iPad History: Saturday 11 Jul 8:00 PM and 4:00 PM). | Matches (`history-sizes-1-2-*`, `history-size-*`, `live/history-*`). |
| Rail and internal dividers | A subdued leading rail per group and thin dividers between children. | Same. |
| Child row | Flags and names flanking a centered, largest result with a restrained separator; footer with ownership, prediction, and the points pill (green `+n pts`, neutral `0 pts`, neutral `— pts` when unavailable). | Same. |
| `Your bet` and `Bet` | History says `Your bet`; Timeline says `Bet` and `No bet` (`timeline-pending-partial-*`, `se-timeline-*`, `ipad-timeline-*`). | Same (`timeline-pending-partial-*`, `live/timeline-*`). |
| Partial, pending, unavailable versus steady | Partial: arrow and number in the pill with `1 of 2 computed` / `2 of 4 computed` beneath. Pending: a `Pending` text pill, with `Points pending` children. Unavailable: an `Unavailable` text pill. Steady: the amber rule. None of them reuse the steady rule. | Same. On the leaderboard and My pools, the rail shows `Pending` / `Unavailable` text or the qualifier under the arrow, on both platforms. |
| Large text | Leaderboard at AX3/AX5 reflows (rank, avatar, score, then the wrapped username). History and Timeline stack teams and move the pill under the metadata. Nothing clipped in the live AX-XL captures on iPhone SE and iPad. | Font scale 2 and 3 renders and live font scale 2: leaderboard and My pools reflow, History stacks teams, nothing clipped. |
| Light and dark | Live captures use the new light tokens: sampled steady `#8C5E00` and down `#B71C1C` in `se-history-light-top.png`. Dark values are unchanged. | `live/history-light-top.png` steady sampled `#8C5E00`. The dark renders and live captures use the dark tokens. |
| Placeholders | `history-placeholder-*`: production header and rows in pulse fill, no text. The leaderboard and My pools placeholder rows use the production geometry. | `group-placeholder-*`, plus the placeholder rows in the leaderboard and My pools renders. |
| Safe areas | First content below the bar, last group above the floating tab bar (`se-history-*-end`, `se-my-pools-dark-axxl-end`, `ipad-leaderboard-dark-axxl-end`). | Last content above the tab bar or navigation bar (`live/*-end.png`, landscape). |

Findings:

1. The only History capture at AX5 on a 320-pt width, `ios/history-partial-ax5-dark-320.png`, truncates the ownership label to `Your…` on the first row and hyphenates team names mid-word (`Inglater-ra`, `Argenti-na`). It was rendered in the 3.1–3.5 round with the earlier three-column row. The current `HistoryBetItem` stacks the teams (see `timeline-pending-partial-ax3-dark.png` and the live AX-XL captures, where `Your bet` is whole), but no capture shows the current row at AX5 and 320 pt. `bet-history` requires labels to stay readable without clipping at large accessibility text, so this is unverified for the largest size. See the open questions.
2. On the leaderboard and My pools, the rank rail is wider in rows that show a qualifier or a `Pending` / `Unavailable` label. As a result, the avatar and name columns sit further right in those rows than in the others (`followup-leaderboard-light.png`, `followup-my-pools-light.png`, Android `leaderboard-*-fs1-320dp.png`, `my-pools-light-fs1-320dp.png`). Both platforms behave the same way. This is a side effect of sizing the rail to the label's widest word to avoid mid-word breaks. `pool-leaderboard` asks for "stable areas for a rank rail". Low severity.
3. At font scale 3 on a 320-dp width, Android leaderboard usernames made of one token break inside the token (`AstroDelBal|on`, `MagoDeLaC|ancha`, `giraldocano|517@gmail|.com`). The 4.3–4.5 record already notes this for both platforms. The leaderboard spec does not forbid it; the no-mid-word rule applies to My pools. Low severity.
4. The earlier iOS light captures (`history-groups-light`, `history-partial-unavailable-light`, `group-sizes-1-to-4-light`, `timeline-pending-partial-light`, `history-placeholder-light`, `leaderboard-trend-states-light`, `my-pools-trend-states-light`) still show the old light tokens: the steady rule samples `#FBC02D`. The follow-up section already says so, and the `followup-*` and live captures show the current values. They are not evidence for light contrast.
5. Record correction: the Android 4.3–4.5 section says that at font scale 2 the History badge moves below the date and time "trailing-aligned". In `history-down-partial-light-fs2-320dp.png` the badge sits on the leading edge under the time, as on iOS. At 600 dp it stays on the date line with the qualifier below it.

Cross-platform differences that are not defects: date order follows the device locale (iOS simulator `Sunday, 11 Oct`, Android `Sunday, Oct 11`); the light badge fill differs slightly (`surfaceVariant` on Android, `secondarySystemFill` on iOS); at font scale 2 Android stacks History teams even at 600 dp, as iOS does on the iPad at accessibility sizes, because both switch on text size rather than width; My pools rows on Android have no dividers and no filled invite circle, which predates this change.

Accessibility contracts are not visible in screenshots. The announcement order, single focus stops, and inert placeholders are covered by the tests named in each section. Spoken output on a device was accepted as not verified on both platforms.

### Task reconciliation

Legend: **Covered** means every verify clause has recorded evidence. **Accepted** means a clause is unverified and the cited acceptance line covers it. **Missing** means a clause has neither evidence nor an acceptance line.

| Task | Where verified | Clauses | Status |
| --- | --- | --- | --- |
| 1.1 | `Pool/test/Domain/PositionPolicyTest.fs` (`two settlements of one group keep the baseline taken before the group` asserts 8 → 5 → 6 keeping 8; `a newer kickoff resets the baseline…`; `a late result or correction of an older group keeps the baseline`; `a resumed operation does not reset a membership it already wrote`); `Pool/test/Settlement/KickoffGroupSettlementTest.fs` (resumed after deadline yields, decision taken at admission); task 5.1 AWS run | All four | Covered |
| 1.2 | `Pool/test/Application/KickoffGroupReaderTest.fs` (equivalent offsets and other instants of the day; a result counted before settlement; layout read once per call) | All three | Covered |
| 1.3 | `AmazonLambda/test/Function/TrendGroupScoreTest.fs` (leaderboard keeps positions; My pools carries the same trend group and none for a never-settled pool; gambler score) | Identical values, absence, unchanged `position`/`beforePosition` | Covered |
| 1.4 | `KickoffGroupSettlementTest.fs` (`a computed detail records its kickoff once and keeps it on corrections`, `the finished-by-kickoff index holds only computed details…`); `docs/database-model.md` rows for both indexes and `computedMatchDateTime`; `operations/pool-table.sh` still defines `GetFinishedPoolGamblerBets-index` unchanged; deployed to TycheApi on 2026-10-13 | Index excludes pending; existing index untouched | Covered. The new GSI is created by `operations/pool-table.sh ensure-kickoff-index`; the SAM template only grants IAM on it, because the Pool table is not defined in the template. |
| 2.1 | `Pool/test/Application/GroupedBetFeedTest.fs` (group cut by a storage page; partial group with layout counts; legacy child, null movement; computation-time token rejected); `InvalidCursorTest.fs` (Lambda and HttpApi) for 400 `INVALID_CURSOR`; task 5.2 AWS check (a), (c) | All four | Covered |
| 2.2 | `GroupedBetFeedTest.fs` (partial group once in the unsettled phase; several unsettled storage pages; phase change after the last result; hidden predictions with counts revealing nothing); task 5.2 AWS check (b), with empty `live:` pages | All four | Covered |
| 2.3 | `Pool/test/Settlement/KickoffChangeAndBackfillTest.fs` (postponed pending match regroups in both feeds; delivered twice writes nothing; stale index entry); `AmazonLambda/test/Event/PoolLayoutKickoffEventTest.fs`; task 5.1 step 4 and second delivery on AWS | Both | Covered |
| 2.4 | `KickoffChangeAndBackfillTest.fs` (mid-tournament pool with the latest receipt, with a rerun writing nothing; pools before receipts; no computed details; waits for an idle pool); operator entry in `operations/enqueue-maintenance.sh` and `operations/README.md`. Production backfill on 2026-10-13: 0 computed details missing `computedMatchDateTime`, all 58 roots with a trend kickoff; `RecursiveLoop: Allow` confirmed on `OnPoolMaintenanceJob` and `OnUpdatePositions` (template lines 949 and 997) | Rerun with no writes; mid-tournament values | Covered |
| 2.5 | `docs/pool-settlement.md` sections Kickoff groups, Baseline rule, Grouped History and Timeline, Kickoff changes (with the `MatchScoreIngestion` operator note), Backfill; the backend run above (644 passed, including `PoolOperationTest`, `DispatchAndStandingsTest`, `ContinuationTraversalTest`, `SharedCodecPaginationTest`) | Settlement, position, restoration, and pagination suites pass | Covered |
| 3.1 | iOS 3.1–3.5 table: `CoreTests/RankMovementTests`, `DataBetTests/KickoffGroupFeedTests`, `DataPoolTests/PoolGamblerScoreResponseTests` | Each state, absent `trendGroup`, null `movement` | Covered |
| 3.2 | iOS 3.1–3.5 table; icon check above | One source per glyph, Material Symbols, no raster, no navigation-asset change | Covered. Upstream comparison accepted ("Accepted without comparing the committed trend SVGs against upstream Material Symbols by the user on 2026-10-12"). |
| 3.3 | `BetTests/KickoffGroupPresentationTests`; captures `group-sizes-1-to-4-*`, `history-partial-unavailable-*`, `timeline-pending-partial-*`; reference comparison above | Previews for one to four, partial, pending, `Your bet`/`Bet`: covered. Independent child activation: `KickoffGroupSection` makes each child its own `Button`, but no iOS test or live observation records a child opening its own match | Partly missing (child activation on iOS) |
| 3.4 | `BetTests/KickoffGroupPagingTests`, `BetTimelineListViewModelTests` (3.1–3.5 table and repeated-groups section) | Append order, partial-to-finished, stale not merging, bounded retry | Covered |
| 3.5 | `GamblerScoreItemAccessibilityTests`, `PoolScoreItemAccessibilityTests`, `UITests/RankMovementAccessibilityTests`, `KickoffGroupPresentationTests`, `GamblerScoreItemLargeTextTests`, `UITests/TrendColorTests`; AX3/AX5 captures | Announcement order, direct child focus, inert placeholders, large text | Covered by unit tests; VoiceOver accepted ("Accepted without VoiceOver on a running app… on 2026-10-12", moved to 3.6, then accepted on 2026-10-13). The ViewInspector suites listed as "CI only" (`PoolTests/PoolTests`, `GamblerScoreItemRankPreviewTests`, `PoolPlaceholderSurfacesTests`, `GamblerScoreItemPlaceholderBehaviorTests`, `HistoryPointsRetryTests`, the two `listRetryRequestsOnlyTheRowsOnce…` tests, `UITests/LazyPagingVStackErrorTests`) have no recorded CI run and no acceptance line: **missing** |
| 3.6 | iOS 3.6 live section (iPhone SE, iPad Pro 13-inch, light/dark, AX-XL, pull to refresh, safe areas); `iOS/Bet/README.md`, `iOS/Pool/README.md` | Retry, empty, and error states in the app, iPad landscape and Split View: accepted ("Accepted without the running-app error, empty, and retry states, VoiceOver, iPad landscape and Split View, the iPhone 18 Pro Max, and live partial/pending movement and paging by the user on 2026-10-13"). Native tests with explicit waits: the two added `firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal` tests only built (`TEST BUILD SUCCEEDED`) and were never run, and the acceptance line does not cover that | Partly missing (the two new iOS tests not run) |
| 4.1 | Android 4.1 section (screens, locators, waits, network observations) | Exploration done with the `android` CLI and adb instead of ARTEMIS: accepted ("Accepted with exploration through the `android` CLI and adb instead of ARTEMIS… on 2026-10-13"). `adb devices -l` output is not recorded; one emulator, `emulator-5554`, is named throughout | Covered with acceptance |
| 4.2 | Android 4.2–4.5 table: `RankMovementTest`, `KickoffGroupFeedRequestTest` (two instants reading 01:30 in New York stay two groups), `PoolGamblerScoreResponseTest`, `PoolGamblerScoreModelTest` | DST labels, absent `trendGroup`, null `movement` | Covered |
| 4.3 | Drawable path comparison; 84 renders with the reference comparison; `HistoryPresentationTest` (20), `HistoryGroupedListTest.aTwoMatchHeaderIsOneHeadingWithItsCountAndMovementAndEachRowOpensItsOwnMatch`; live child activation (4.1 Match screen row); nav assets unchanged (above) | Glyphs, hierarchy, owner wording, child activation, no shell change | Covered |
| 4.4 | `bet` `KickoffGroupFeedTest` (distinct keys for DST labels, a repeated group shown once, partial-to-finished, one restart then retry, ordinary append retry) | All five | Covered |
| 4.5 | `TrendColorContrastTest`; instrumented `PoolScoreItemAccessibilityTest`, `PoolScoreListAccessibilityTest`, `GamblerScoreListTest`, `GamblerScorePlaceholderTest` (avatar request suppressed), `BetPlaceholderAccessibilityTest`, `HistoryPresentationTest`, `ProgressIndicatorTest`; reduced motion with `animator_duration_scale 0`; renders at font scale 2 and 3 | No decorative stops, no filler exposure or remote loading, large text, reduced motion | Covered; TalkBack speech accepted ("Accepted without TalkBack speech on a device, the real app screens, and Android 14 non-linear font scaling… on 2026-10-13", and again in 4.6) |
| 4.6 | Android 4.6 section: dark, font scale 2, landscape, 320 and 640 dp, pull to refresh, edge-to-edge limits; connected tests class by class; 716 unit tests; `Android/bet/README.md`, `Android/pool/README.md` | API 28 and 36, TalkBack, live reduced motion, `PoolHomeTabSwitchAppBarTest`: accepted ("Accepted without API 28 and 36, TalkBack speech, live reduced motion, `PoolHomeTabSwitchAppBarTest`… on 2026-10-13"). ARTEMIS-verified UI tests: replaced by `android` CLI exploration, accepted (same line as 4.1) | Covered with acceptance |
| 5.1 | Backend 5.1 section: 37/37 on AWS (`task-5.1-results.json`) | Backend half covered. Both apps on devices accepted ("Accepted without the 5.1 sequence on devices in both apps… on 2026-10-13") | Covered with acceptance |
| 5.2 | Backend 5.2 section: 59/59 on DynamoDB (`results.json`); client unit tests listed there, all present | Backend half covered. Client half on devices accepted ("Accepted without the client half of task 5.2 on devices… on 2026-10-13") | Covered with acceptance |
| 5.3 | This section | Validate: valid, strict. Screenshots reviewed. Reconciliation done | Covered, with the open questions below |

Every test file and class cited in this record exists: the backend files above; the iOS suites `RankMovementTests`, `KickoffGroupFeedTests`, `PoolGamblerScoreResponseTests`, `KickoffGroupPresentationTests`, `KickoffGroupPagingTests` (all seven cited methods), `BetTimelineListViewModelTests`, `FinishedBetListViewModelPointsSummaryTests`, `GamblerScoreItemAccessibilityTests`, `PoolScoreItemAccessibilityTests`, `RankMovementAccessibilityTests`, `TrendColorTests`, `GamblerScoreItemLargeTextTests`, `AppendPagingVStackTests`, and `TrendIndicatorTests`; and all 26 Android classes named in the 4.x sections, including `HistoryGroupedListTest` with its two methods and `GamblerScoreListTest.theLastGamblerEndsAboveTheBottomPaddingAtTheScrollLimit`.

### Open questions

1. iOS ViewInspector suites (3.5 and the follow-ups): run them on CI, or on a throwaway iOS 18.1 simulator, or accept them as not verified.
2. The two iOS 3.6 tests `FinishedBetListViewModelPointsSummaryTests.firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal` and `BetTimelineListViewModelTests.firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal`: run them on a non-signed-in simulator or CI, or accept them as not verified.
3. iOS 3.3 independent child activation: record a live tap or a test, or accept the per-row `Button` in `KickoffGroupSection` as sufficient.
4. History at AX5 on a 320-pt width with the current `HistoryBetItem`: render or capture it to confirm the `Your…` truncation in `history-partial-ax5-dark-320.png` is gone, or accept the AX-XL live captures as sufficient.
5. Rank rail width varying per row on the leaderboard and My pools (finding 2): accept it as the trade-off for avoiding mid-word breaks, or ask for a fixed rail width.

## iOS — final checks for 5.3 (2026-10-13)

Toolchain: Xcode 27.0 (`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`) on the iPhone 18 Pro Max simulator (iOS 27.0, `D8F12B93-3319-437A-8CE2-59C620CC8A29`), which has no signed-in app. No `Package.resolved` changed.

### Check 1: History at AX5 on a 320-pt width

Rendered with `ImageRenderer` at 2× from the production `KickoffGroupSection` and the preview data (a temporary capture test, since deleted), at `accessibility5`, 320 pt wide: the partial four-match History group (Inglaterra – Argentina, España – Argentina, `Your bet`), the steady three-match group (Portugal – República Democrática del Congo, `No bet`), and the partial Timeline group with `Bet` and a pending row. Each group was rendered separately and stacked, because a single image of all three exceeds the PNG encoder's limit.

First render, current code: **failed.** `Your bet` truncated to `Your…` on the first row; team names broke mid-word beside the flag (`Inglater-ra`, `Argenti-na`, `Colom-bia`) although each fits the full width; and the `Points pending` pill, which never wraps, widened the Timeline group past 320 pt, clipping the header and the pill at both edges.

Fix:

- `HistoryBetItem`: a stacked team line keeps the flag beside the name while the name's widest word fits there; otherwise the flag goes above and the name takes the full width. At ordinary sizes the three-column matchup is used only when the widest word of either name fits a column; otherwise the teams stack around the result, as Android does (4.3). The ownership label and the prediction sit on one line when they fit; otherwise the prediction moves below and the label wraps between words.
- `HistoryPointsPill`: one line when it fits; otherwise the text wraps inside the pill instead of overflowing. The pill has layout priority, so in the match screen's participant rows the neighboring text still gives way first, as before.

After the fix: `final-history-ax5-320-{light,dark}.png` in [verification/ios](verification/ios). `Your bet` and `Bet` are whole; Inglaterra, Argentina, and Colombia are whole, with the flag above; `Points pending` wraps inside its pill; nothing overlaps or clips. `Democrática` still breaks (`Democráti-ca`): the single word is wider than the whole row at AX5, and `bet-history` asks only that names stay distinguishable without overlap and that text not shrink; it has no no-mid-word rule (that rule is in `pool-score-list`, for My pools).

Test: `BetTests/HistoryBetItemLargeTextTests` (`UIHostingController.sizeThatFits`, no ViewInspector): a pending row, for both owners, and every History preview row fit a 270-pt row (320 pt less gutters and rail) at AX5; `historyNameWords` splits on whitespace. With the previous pill the pending-row cases fail.

Bet tests: `-only-testing:BetTests` with `-skip-testing:BetTests/HistoryPointsRetryTests` and the two `listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth()` cases skipped: passed, 102 tests.

### Check 2: independent child activation in the running app

Not done. The iPhone SE was booted, but the host load average rose to about 72 and the run was stopped before the app was launched. The SE was shut down without the app being opened; appearance (`light`) and text size (`large`) were not changed. Task 3.3's child-activation clause remains open.

### Test run by the coordinator

`FinishedBetListViewModelPointsSummaryTests.firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal` and `BetTimelineListViewModelTests.firstPageRetryRequestsOnlyTheRowsAndKeepsTheTotal` were run by the coordinator on 2026-10-13 on the iPhone 18 Pro Max (iOS 27.0, Xcode 27), with `-only-testing` on those two suites and the two ViewInspector `listRetryRequestsOnlyTheRowsOnceAndPullToRefreshStillReloadsBoth()` cases skipped: 25 tests, all passed.

Not verified:

- Check 2 (see above).
- The changed row layout in the running app; the installed app predates this fix and was not reinstalled.

Accepted a mid-word break in a single word wider than the whole History row at AX5 on a 320-pt width ("Democrática") by the user on 2026-10-13; preventing it would require shrinking text, which the bet-history spec forbids.

### Live checks on the iPhone SE (2026-10-13)

The `Tyche` scheme (production) was built with Xcode 27 for the iPhone SE (3rd generation), iOS 27.0, `4D1E3B5D-B498-4B1C-82D9-965242CF0E88`, 375 × 667 pt. It was installed over the existing app with `xcrun simctl install` and launched. The app opened signed in, on My pools; no sign-in, sign-out, or reinstall happened. Taps went through the Claude iOS simulator tool and captures through `xcrun simctl io <udid> screenshot`. The device started and ended in light appearance at the `large` text size and was shut down afterwards. The host load average rose to between 70 and 145 during the boot, from disk and indexing activity rather than CPU; the app stayed responsive.

The data is the production 2026 World Cup pool; production holds only a finished tournament.

| Check | Result |
| --- | --- |
| Independent child activation, History (task 3.3) | **Passed.** Group Thursday 25 Jun, 3:00 PM, 2 matches. The first row opened the match screen for Curazao 0–2 Costa de Marfil (Thursday, 25 Jun · 3:00 PM). The in-app back button returned to History at the same scroll position. The second row opened Ecuador 2–1 Alemania (same kickoff). Each row is its own control; the header and rail open nothing. |
| Independent child activation, another gambler's Timeline | **Passed.** Opened Val Cardo from the leaderboard. Group Saturday 27 Jun, 9:00 PM, 2 matches: the first row opened Argelia 3–3 Austria, and after the in-app back button the second row opened Jordania 1–3 Argentina. |
| History and Timeline at `accessibility-extra-extra-extra-large` (AX5) with the new layout | **Passed.** Headers stack date, time with count, and the badge. Rows stack the teams around the result; on this 375-pt width names such as Sudáfrica, Argentina, Suecia, and Países Bajos fit beside the flag and wrap between words. `Your bet 0 – 5` and `Bet 1 – 3` are whole on one line. Nothing is clipped; the points pill scrolls under the floating tab bar like any content. |
| Default text size | Ordinary rows are unchanged: three columns with the result centered. `República Democrática del Congo` (Saturday 27 Jun and Wednesday 1 Jul) stays in three columns and wraps between words, because its widest word fits its column at 375 pt. `Estados Unidos`, `Costa de Marfil`, `Corea del Sur`, and `Arabia Saudita` wrap between words in their columns. |

Captures in [verification/ios/live](verification/ios/live): `final-row-activation-history-{group,first,second}.png`, `final-row-activation-timeline-{group,first,second}.png`, `final-history-ax5.png`, `final-history-ax5-2.png`, `final-timeline-ax5.png`, `final-timeline-ax5-2.png`, `final-timeline-ax5-3.png`, `final-history-default-top.png`, `final-history-default-long-name.png`, `final-timeline-default-top.png`.

The first History tap in this session opened Estados Unidos – Australia instead of the intended row because the list was still scrolling from a fling when the tap landed; the row under the finger at that moment was Estados Unidos – Australia, which the screen after returning confirmed. Later taps were made after the list settled.

Not verified: the flag-above-name fallback and the wrapped points pill in the running app. Neither occurs at 375 pt with this data; both are shown in the 320-pt renders above.

Accepted without a recorded run of the iOS ViewInspector suites (they crash on local iOS 27 runtimes; they run on CI when the change is committed), and with the wider rank rail on rows that carry a qualifier or a Pending/Unavailable label (chosen to avoid mid-word breaks), by the user on 2026-10-13.
