# Verification

## iOS — tasks 1.1–1.5 (2026-10-01)

Toolchain: Xcode 26.5 (`/Applications/Xcode_26.5.app`). Simulators: "iPhone 16 Pro (iOS 18.1)" `A2F5EF6F-BA25-4035-9494-F6ACE0144047` (holds Felipe's signed-in session; production backend) and `tmp-drawer-iPhone17-26.5` `48AD28C3-2C4E-406C-95FD-46563B66EA63`. Captures are in `verification/ios/`.

### 1.1 Inventory of model-backed loading call sites (iOS)

Excluded from this change: full-screen loaders (`LoadingContainerView`, `BallSpinner` in `HomeRouter`, `PoolHomeRouter`, sign-in views, `PoolFromLayoutCreatorView`, `PoolJoinerView`, `UsernameEditor`, `ProfileView`, `PendingBetItemView`, `ManageGamblerItem`), the inline `ProgressView` in `EditableInlineText`, and real-account avatar fallbacks (`AccountAvatar` → `InitialAvatar` for loaded accounts).

| Surface | Production component | Placeholder model | Call sites (loading slots) | State before this change | Task |
| --- | --- | --- | --- | --- | --- |
| Leaderboard | `Pool/GamblerScore/GamblerScoreItem` (+ `PostionIndicator`, `TrendIndicator`, `AccountAvatar`) | `poolGamblerScorePlaceholderModel()` | `GamblerScoreList`: `GamblerScorePlaceholderList` (initial), `GamblerScorePlaceholderRow` (append) | `placeholderModifier: ShimmerModifier()`; flag inferred from modifier presence | 1.3 (done) |
| My pools | `Pool/PoolScore/PoolScoreItem` | `poolGamblerScorePlaceholderModel()` | `PoolScoreList`: `PoolScorePlaceholderList`, `PoolScorePlaceholderRow` | `placeholderModifier: ShimmerModifier()`; flag inferred from modifier | 2.1 |
| My pools empty state, popular templates | `Pool/Creator/PoolFromLayoutCreatorItem` | `poolLayoutFakeModel()` | `PoolScoreList.TemplatesContent` (refresh loading) | `.shimmer()` on the item | 2.1 |
| Pool templates (creator) | `PoolFromLayoutCreatorItem` | `poolLayoutFakeModel()` | `PoolFromLayoutCreatorList`: `PoolFromLayoutCreatorPlaceholderList` (initial), append slot, nil-`peek` fallback | `.shimmer()` | 2.1 |
| Drawer pool summary | `Tyche/UI/PoolHome/PoolHomeDrawerView` `PoolSummaryItem` (private) | `poolGamblerScorePlaceholderModel()` | `PoolSummary` `.idle`/`.loading` | already `isPlaceholder`, rendered by private `ConditionalShimmer` → `.shimmer()` | 2.1 |
| Pending bets | `Bet/Pending/PendingBetItem` | `poolGamblerBetPlaceholderModel()` + `partialPoolGamblerBetFakeModel()` | `PendingBetList`: `PendingBetPlaceholderList`, `PendingBetPlaceholderRow`, nil-`peek` fallback (`PendingBetPlaceholderItem`) | `.shimmer()` | 2.2 |
| Live / finished history (timeline) | `Bet/Timeline/BetTimelineItem` → `LiveBetItem` / `FinishedBetItem` | `poolGamblerBetPlaceholderModel()` | `BetTimelineList`: `BetTimelinePlaceholderList`, `BetTimelinePlaceholderRow`, nil-`peek` fallback | `.shimmer()` | 2.2 |
| Finished bets | `Bet/Finished/FinishedBetItem` | `poolGamblerBetPlaceholderModel()` | `FinishedBetList`: `FinishedPoolGamblerBetFakeList`, `FinishedPoolGamblerBetFakeItem`, nil-`peek` fallback | `.shimmer()` | 2.2 |
| Match details header | `Bet/Match/MatchHeader` | `poolGamblerBetPlaceholderModel(isLocked:isComputed:)` | `MatchBetListView` → `MatchHeaderPlaceholderItem` (in `MatchGamblerBetItem.swift`) | `.shimmer()` | 2.2 |
| Match gambler bets | `Bet/Match/MatchGamblerBetItem` | `poolGamblerBetPlaceholderModel()` | `MatchBetList`: `MatchBetPlaceholderList`, `MatchBetPlaceholderRow`, nil-`peek` fallback | `.shimmer()` | 2.2 |
| Manage gamblers | `Pool/ManageGamblers/ManageGamblerItem` (production) | none yet | `ManageGamblersList`: `ManageGamblerPlaceholderList` (8 rows with `.opacity(1.0 - index * 0.04)`), `ManageGamblerPlaceholderRow` (append) | separately maintained skeleton geometry (circle + two fixed bars) under `.shimmer()` | 2.3 |

Shared effect plumbing to remove in 2.5: `UI/Sources/UI/ShimmerModifier.swift` (`View.shimmer()`, `ShimmerModifier`), the `SwiftUI-Shimmer` 1.5.1 dependency in `UI/Package.swift`, `PoolScoreItem.placeholderModifier`, and the `.shimmer()` uses in previews (`MatchHeader`, `LiveBetItem`, `FinishedBetItem`, `PendingBetItem`). Android counterparts (for 3.1, not inventoried here): `ui/ShimmerModifier.kt` and the shimmer users in `app` (pool home, drawer), `bet` (match, finished, live, pending), and `pool` (creator, pool score, spotlight, manage gamblers, gambler score).

Coverage check: pools, leaderboard, bets/history, match details, pool templates, drawer summaries, and Manage gamblers each appear above.

### 1.2 Future-authoring guidance

`AGENTS.md` and `openspec/config.yaml` now direct future specs to the shared loading-placeholder treatment defined by `loading-placeholders`, selected through `isPlaceholder` rather than a caller-supplied effect modifier. The production-model, no-skeleton-layout, suppression (remote loading, navigation, accessibility), no-real-content, and no-retroactive-revision rules are unchanged; the icon rules are untouched; no archived artifact was edited. `config.yaml` parses (Ruby YAML), and `openspec validate unify-pulse-loading-placeholders --strict` reports the change valid.

### 1.3 Theme values and representative row

Theme: `UI/Sources/UI/LoadingPlaceholderPulse.swift` (`LoadingPlaceholderPulse`): 0.9 s legs, 1.8 s cycle, symmetric smoothstep ease-in-out, no pause, starts dim; neutral `fillColor` `.primary`.

Native redaction calibration (measured with `ImageRenderer`, identical on iOS 18.1 and 26.5): a redacted `Text` or `Image` is drawn as a block of its foreground style at 16% of that style's opacity (black on white → 214; white on black → 41). Redacted images fill their whole frame, and redaction tints with the foreground color (green text → green-tinted block). `Color` fills and `Shape` fills are not redacted. 16% is therefore the highest fill native redaction can produce, below the design preset's bright targets (20% light, 24% dark). Calibrated foreground opacities, keeping the preset's dim:bright ratios:

| Appearance | Dim | Bright | Static (Reduce Motion) | Composite fill |
| --- | --- | --- | --- | --- |
| Light | 0.60 | 1.00 | 0.80 | black 9.6% → 16% → 9.6%, static 12.8% |
| Dark | 0.667 | 1.00 | 0.833 | white 10.7% → 16% → 10.7%, static 13.3% |

Representative row: `GamblerScoreItem(poolGamblerScore:isCurrentUser:isPlaceholder: Bool = false)` replaces `placeholderModifier`. Nested primitives gained `isPlaceholder = false`: `PostionIndicator` and `TrendIndicator` drop their own text/semantic colors so redaction does not tint (rank-tile background stays `surfaceVariant`); `AccountAvatar(accountId:fallback:isPlaceholder:)` renders a neutral full-frame glyph that the row redacts and clips to a circle, and never starts a photo load. The row owns a private `PulsingPlaceholderContent` (native redaction + `TimelineView(.animation)`-driven foreground opacity); no public helper or caller-supplied modifier was added, and `ShimmerModifier` is no longer referenced by the leaderboard.

Geometry (light/dark preview equivalents rendered with `ImageRenderer`, loaded `poolGamblerScoreDummyModel()` above placeholder): `leaderboard-row-{light,dark}-{default,ax2}.png`. Loaded and placeholder rows are both 393 × 82 pt at the default and accessibility-2 text sizes; rank tile, avatar circle, username, score, and trend regions occupy the same positions, separated by their production gaps, with no full-row block. Observation: the redacted trend arrow becomes a full icon-box square beside a short digit bar; at accessibility-2 that square extends below the row, as the loaded arrow already does.

Build: `xcodebuild build -workspace iOS/Tyche.xcworkspace -scheme Tyche` succeeded for the 26.5 simulator, the 18.1 simulator, and `generic/platform=iOS` (signed `H7N4H27ULJ.com.felipearpa.tyche`).

### 1.4 Transitions on the iOS 18.1 simulator

Method: a temporary build that delayed only the leaderboard paging query by 40 s (backed up and `shasum`-verified before building; source restored immediately; a clean build was reinstalled afterwards and loads the leaderboard within 3 s). Opened "Copa Mundial de la FIFA 2026" from My pools. Brightness traces sample the first placeholder username bar at 20 fps from `simctl io recordVideo`.

- Pulse, light (`sim18-light-pulse-trace.txt`, `sim18-light-pulse-reduce-motion-on.mp4`): bar oscillates 212 ↔ 229 with minima at 5.2, 7.0, 9.0, 10.8, 12.6, 14.4, 16.2, 18.0, 19.8 s — a 1.8 s cycle, no pause, no moving band.
- Live Reduce Motion on (`defaults write com.apple.Accessibility ReduceMotionEnabled -bool true` plus the accessibility notification, app in foreground): within 0.2 s the bar settled at a constant 221 (static midpoint) until data arrived.
- Pulse, dark, starting with Reduce Motion on (`sim18-05-dark-reduce-motion-static.png`, `sim18-dark-pulse-trace.txt`, `sim18-dark-static-reduce-motion-off.mp4`): static 32; after turning Reduce Motion off live, pulsing 25 ↔ 39 on a 1.8 s cycle (`sim18-06-dark-pulse-after-live-toggle.png`).
- Inert placeholder: tapping a placeholder row during loading neither navigated nor showed a pressed state (`sim18-02-light-placeholder-after-tap.png`).
- No filler avatar requests: with CFNetwork diagnostics, between the pool tap (16:08:10) and the leaderboard response (16:08:47) the app made no request to `tyche-avatars`; at 16:08:48 the loaded rows made eight avatar requests (403 for gamblers without a photo). Only the two cold-start account-avatar revalidations (16:07:50) preceded the tap.
- Restoration: data replaced placeholders as soon as it arrived (light 49.4 s, dark 48.0 s into the recordings) with names, letter avatars, Felipe's photo, trend marks, and the signed-in highlight (`sim18-03-light-loaded.png`, `sim18-07-dark-loaded.png`); tapping a loaded row opened that gambler's timeline (`sim18-04-loaded-row-opens-timeline.png`).
- Parent activation target: placeholder rows are built by `GamblerScorePlaceholderRow`, which never wraps the row in the gambler-detail `Button`; the row also sets `.allowsHitTesting(false)` and `.accessibilityHidden(true)` for placeholders.

Settings restored: Reduce Motion key deleted (default off), appearance light, clean build installed. Not observed: Felipe's Work iPhone (`00008110-000E1859148A801E`) — `devicectl device install` failed because the phone was locked; the device build was not installed. VoiceOver traversal was not run on device or simulator.

### 1.5 ARTEMIS exploration, tests, and component contract

Blocked. `mobile_diagnose` returned `verdict: blocked` ("No Multimodal LLM credential … found"; `/Users/felipe/artemis/.env`). The `medium_phone` AVD booted as `emulator-5554`, but Fortuna on it shows the sign-in screen. Per the rule that executable tests follow ARTEMIS exploration, no new behavioral tests were written. Existing tests were changed only mechanically (`placeholderModifier: ShimmerModifier()` → `isPlaceholder: true` in `GamblerScorePlaceholderTests` and `GamblerScoreItemAccessibilityTests`) so the test targets compile; the empty `avatarAccountId` they assert is kept as defense in depth.

Existing tests run on the iOS 18.1 simulator (`xcodebuild test -scheme <Package>` from each package directory; totals from `xcresulttool`): `UI` 89/89 passed; `Pool` 29/29 passed, including the ViewInspector suites; `Account` 21/21 passed with `AvatarWireProbeTests` skipped. No `Package.resolved` changed.

Public component contract (iOS, as implemented):

- `GamblerScoreItem(poolGamblerScore:isCurrentUser:isPlaceholder: Bool = false)`. With `true`, the caller supplies a placeholder model and nothing else: the row redacts its content, pulses with `LoadingPlaceholderPulse` (static midpoint under Reduce Motion), ignores touches, exposes no accessibility element, and requests no avatar. With the default, loaded behavior is unchanged.
- `PostionIndicator(…, isPlaceholder: Bool = false)` and `TrendIndicator(difference:textStyle:isPlaceholder: Bool = false)`: with `true` they drop their own foreground colors so the enclosing placeholder's neutral fill applies; they do not redact or animate themselves.
- `AccountAvatar(accountId:fallback:isPlaceholder: Bool = false)`: with `true` it never loads a photo and draws no fallback identity.
- `LoadingPlaceholderPulse`: theme-owned timing, curve, and per-appearance endpoints; not a view helper.

Accepted without the Xcode Preview canvas check for 1.3 (ImageRenderer light/dark captures used instead) by the user on 2026-10-01.
Accepted without the Work iPhone run and VoiceOver traversal for 1.4 (verified on the iOS 18.1 simulator) by the user on 2026-10-01.

#### 1.5 update — ARTEMIS exploration and behavioral tests

Calibration: the user chose to keep the iOS native-redaction values above (light 9.6% → 16%, midpoint 12.8%; dark 10.7% → 16%, midpoint 13.3%); Android is matched to them in 5.1.

Android exploration (ARTEMIS Pro, `device_serial=emulator-5554`, `medium_phone` API 36, signed in, read-only). `mobile_diagnose(device_serial="emulator-5554")` returned `ready`. The first run (trace `e8bb3c87-…`) was stopped: My pools showed "Unexpected error" because the emulator could not resolve any host name (`ping tyche-api.felipearpa.com` → unknown host; IP pings worked). After `adb emu kill` and a relaunch through `mobile_diagnose(launch_avd="medium_phone")`, name resolution worked and the session was still signed in. Run `9ef0967f-3133-49f4-8687-21e4b8a166ef` passed its five checks; its report and the device recording are in `verification/android-artemis/`.

- Opening "Copa Mundial de la FIFA 2026" shows production-shaped leaderboard placeholders (rank tile, circular avatar, username bar, score block) for about 1 s with the current Accompanist shimmer, then the loaded rows (`emulator-leaderboard-loading.mp4`, about 9.0–10.0 s).
- Loaded rows: another gambler's row is a clickable, focusable container whose child announces "Rank 1, El mono, 676 points, Rank unchanged"; tapping it opened that gambler's Timeline, and the in-app back arrow returned to Scores. The signed-in row is not clickable or focusable, announces "Rank 8, felipearpa, You, 589 points, Rank unchanged", and tapping it did nothing.
- Placeholder-row hierarchy attributes were not captured: the placeholders disappeared before the agent's hierarchy dump. The agent's statement that they expose no interactive nodes is an inference, not an observation.

iOS exploration (iOS 18.1 simulator, clean build, production backend). The 1.4 delayed-build run covers the loading path (inert placeholder tap, no filler avatar requests, live Reduce Motion, restoration). A second run on the clean build (`rec3`) showed placeholders for about 2.1 s before the loaded rows appeared. Tapping "Val Cardo" opened that gambler's Timeline, the back gesture returned to Scores, and tapping the signed-in "felipearpa" row did not navigate.

Behavioral tests written after this exploration (Swift Testing; ViewInspector 0.10.3 where a view is inspected):

- `iOS/Pool/Tests/PoolTests/GamblerScoreItemPlaceholderBehaviorTests.swift`
  - Filler avatar request: a placeholder row's `AccountAvatar` receives `isPlaceholder`, has `loadsPhoto == false`, and draws no `InitialAvatar`.
  - Flag, not filler identity: a real-looking model rendered with `isPlaceholder: true` is still inert (no photo load, hit testing off, hidden from accessibility).
  - Actions: the row container has hit testing off; `GamblerScorePlaceholderRow` and `GamblerScorePlaceholderList` contain no `Button`.
  - Accessibility: the row container is `accessibilityHidden` and its label is empty.
  - Loaded restoration: the same row type with a real model allows hit testing, is not hidden, announces the username, and its avatar loads the real account id.
- `iOS/Account/Tests/AccountTests/AccountAvatarPlaceholderTests.swift`: a placeholder avatar never loads a photo, even with a real-looking account id; a real account loads by default; an empty id does not load.

Positive controls (temporary edits backed up, restored, and `shasum`-verified):

- With `.allowsHitTesting(true)` and `.accessibilityHidden(false)` forced in `GamblerScoreItem`, three of the five Pool tests failed with four issues.
- With the `isPlaceholder` check removed from `AccountAvatar.loadsPhoto`, the placeholder-avatar Account test failed.
- The empty placeholder `avatarAccountId` masks that second regression in the Pool tests, so the Account test is the guard for it.

Final runs on the restored sources (iOS 18.1 simulator, `xcodebuild test -scheme <Package>`, totals from `xcresulttool`): `UI` 89/89, `Pool` 34/34, `Account` 24/24 passed (`AvatarWireProbeTests` skipped). No `Package.resolved` changed. The emulator's temporary `/sdcard/artemis_lb.mp4` was removed after it was copied.

Accepted without the Android placeholder-row accessibility observation and iOS VoiceOver traversal for 1.5 by the user on 2026-10-01.

## iOS — tasks 2.1–2.5 (2026-10-01)

Toolchain and devices as in group 1: Xcode 26.5; behavioral checks on "iPhone 16 Pro (iOS 18.1)" `A2F5EF6F-…` (signed in, production backend, read-only). Captures are in `verification/ios/` with a `g2-` prefix: `g2-render-*` are `ImageRenderer` captures (loaded model above placeholder model, 393 pt wide, light/dark × default/accessibility-2 text), `g2-sim18-*` are simulator screenshots, recordings, and brightness traces. Theme values are unchanged from group 1 (light 9.6% → 16%, static 12.8%; dark 10.7% → 16%, static 13.3%) and are the final iOS values.

### Component contract (all surfaces)

Every migrated component takes `isPlaceholder: Bool = false`. With `true` the caller passes a placeholder model and nothing else; the component wraps its own content in its module's internal `PulsingPlaceholderContent` (native redaction, neutral `LoadingPlaceholderPulse` foreground, `TimelineView(.animation)`; static midpoint under Reduce Motion), drops its own foreground colors so masks are not tinted, and sets `.allowsHitTesting(false)` and `.accessibilityHidden(true)` on its root. Structural fills (rank tiles, template cards, the drawer's inset group, invite capsule) stay outside the pulse. No public helper or caller-supplied modifier exists: the pulse view is module-internal in `Pool` and `Bet`, and private in the drawer file in the app target (three small copies, each reading the shared theme).

| Surface | Component(s) | Placeholder model | Change |
| --- | --- | --- | --- |
| My pools (initial, append) | `PoolScoreItem` (+ `PostionIndicator`, `TrendIndicator` flags) | `poolGamblerScorePlaceholderModel()` | `placeholderModifier` → `isPlaceholder`; placeholder exposes no open/invite accessibility actions |
| My pools empty-state templates, creator templates (initial, append, nil-`peek`) | `PoolFromLayoutCreatorItem` | `poolLayoutFakeModel()` | `.shimmer()` → `isPlaceholder`; `CardView` folded into the item (same background and 12 pt corners) |
| Drawer pool summary | private `PoolSummaryItem` | `poolGamblerScorePlaceholderModel()` | `ConditionalShimmer` → private pulse; trophy and text colors apply to loaded content only |
| Pending bets | `PendingBetItem` | `poolGamblerBetPlaceholderModel()` + `partialPoolGamblerBetFakeModel()` | `.shimmer()` → `isPlaceholder` |
| History / gambler timeline | `BetTimelineItem` → `FinishedBetItem` / `LiveBetItem` | `poolGamblerBetPlaceholderModel()` | flag passes through `BetTimelineItem`; only the leaf item pulses |
| Finished bets | `FinishedBetItem` | `poolGamblerBetPlaceholderModel()` | `.shimmer()` → `isPlaceholder` |
| Match header | `MatchHeader` | `poolGamblerBetPlaceholderModel(isLocked: false, isComputed: false)` | `MatchHeaderPlaceholderItem` wrapper removed |
| Match gambler bets | `MatchGamblerBetItem` | `poolGamblerBetPlaceholderModel()` | `MatchBetPlaceholderItem` wrapper removed; nil-`peek` uses the same placeholder row |
| Manage gamblers (initial, append) | `ManageGamblerItem` | `poolMemberPlaceholderModel()` (id now fixed, was a fresh UUID) | fixed circle/bar skeleton and `.opacity(1.0 - index * 0.04)` removed; placeholder avatar is `AccountAvatar(isPlaceholder: true)`, never `EmailAvatar` |

Removed in 2.5 after checking every consumer (`grep` finds no `shimmer`, `ShimmerModifier`, or `placeholderModifier` in `iOS/`): `UI/Sources/UI/ShimmerModifier.swift`; the `SwiftUI-Shimmer` package and `Shimmer` product in `UI/Package.swift`; the `swiftui-shimmer` pin in `UI`, `Pool`, `Bet`, `Account`, and workspace `Package.resolved` (9 lines each, nothing else changed). The pbxproj never referenced the package.

### Layout and appearance (2.1–2.3)

`g2-render-*`: every placeholder keeps its loaded component's layout rules at default and accessibility-2 sizes, with separate masks for text, flags, avatar, score, and indicators, the production gaps between them, and no full-row block. At accessibility-2 the My pools placeholder reflows into the same stacked layout as the loaded row, and the match header's filler team names wrap to two lines on each side as the loaded names do. On the simulator at accessibility-large (`g2-sim18-mypools-loading-dark-ax2.png`, `…loaded-dark-ax2.png`) My pools placeholders use the same reflowed layout as loaded rows and the separators fall at the same y positions. Loaded rows look as before (`g2-sim18-mypools-loaded-light.png`, `…drawer-summary-loaded.png`, `…history-loaded.png`, `…match-loaded.png`, `…empty-templates-loaded.png`). Manage gamblers' eight loading rows now have equal intensity (`g2-sim18-manage-gamblers-loading.png`).

Observation: redacted images (flags, avatar glyph, arrows, invite glyph) render dimmer than redacted text (light 240 vs 230, dark 18 vs 27 at the dim phase). They stay visible against the canvas, and the invite glyph pulses over its capsule (176 ↔ 188 light).

### Loading transitions on the iOS 18.1 simulator (2.2, 2.4)

Method: a temporary build whose `LazyPagingCursorSource.load`, drawer score load, and match header load read launch environment variables to delay, fail, return empty, or split a page so the next page comes from a synthetic cursor (no extra backend calls). The three files were backed up, restored, and `shasum`-verified; `git diff` shows no change to `LazyPagingCursorSource.swift` or `PoolHomeDrawerViewModel.swift`.

- My pools initial (`g2-sim18-mypools-light-load.mp4`, `…-light-trace.txt`): the first title mask oscillates 212 ↔ 229 with minima every 1.8 s (5.55, 7.35, 9.15, 10.95, 12.75 s); dark (`…-dark-load.mp4`, `…-dark-trace.txt`) 25 ↔ 39, minima 5.2, 7.0, 8.8 … 16.0 s. Data replaced placeholders mid-fade (light at 226, dark at 31), so no cycle boundary is awaited.
- Append (`g2-sim18-mypools-append-loading.png`, `…append-failure.mp4`): one placeholder row below the loaded row; tapping it and its invite control did nothing; the append failure showed the inline error with Retry while the loaded row stayed.
- Refresh with content (`g2-sim18-mypools-refresh-keeps-rows.png`): pull to refresh keeps the rows under the system spinner; no placeholders.
- Empty result: My pools forced empty shows the existing empty state; its templates show three placeholder cards, then the real template (`g2-sim18-empty-templates-*`). The Bets tab of the finished World Cup pool replaced placeholders with "Nothing to show" (`g2-sim18-pending-empty.png`).
- Templates retention (code change in `PoolScoreList.TemplatesContent`): returning from the creator restarted the template refresh (8 s delay); the loaded template stayed on screen instead of turning back into placeholders (`g2-sim18-templates-kept-during-refresh.png`). Creator template placeholders ignored a tap (`g2-sim18-creator-templates-loading.png`).
- Drawer summary (`g2-sim18-drawer-summary-loading.png`, `…loaded.png`): production inset group with masks, then the loaded summary with its green trophy.
- History, timeline, match (`g2-sim18-history-*`, `…match-*`): placeholders use the production rows; tapping a loaded finished bet opened the match, tapping a loaded match row opened that gambler's timeline, and the back button returned.
- Match reload retention (code change in `MatchBetListViewModel.loadPoolGamblerBet`): returning from the timeline re-runs the header load; the loaded header and rows stayed on screen during the 8 s reload (`g2-sim18-match-header-kept-after-back.png`). A first load and a retry after failure still show the placeholder header.
- Failure and retry (`g2-sim18-manage-gamblers-failure.png`, `…after-retry.png`): the Manage gamblers failure replaced placeholders with the existing error; pull to refresh loaded the three members.
- Animation work ends (Tyche process CPU from `ps`, simulator software rendering): My pools loading 83–85%, 1.4% one second after data and then 0.0%; History loading 37–43%, 0.0% after switching to Scores mid-load; Manage gamblers failure 0.0%; Reduce Motion loading 0.0%.
- Motion, contrast, transparency (`g2-sim18-mypools-dark-reduce-motion-increase-contrast.png`): with Reduce Motion, Increase Contrast, and the Reduce Transparency key (`EnhancedBackgroundContrastEnabled`, effect not separately confirmed) on at launch, the dark mask held a constant 34 across five screenshots about 0.35 s apart, shapes stayed distinct, and no filler was readable.
- No filler image requests: none of the migrated surfaces loads remote images except Manage gamblers' placeholder avatar, which is `AccountAvatar(isPlaceholder: true)` (never loads; covered by tests). Flags are bundled assets.

Older supported OS: all simulator observations above are iOS 18.1. The app also builds for the iOS 26.5 simulator and `generic/platform=iOS`; it was not launched on the 26.5 simulator or the Work iPhone.

Incident: while checking that a Manage gamblers placeholder ignores a swipe, the data arrived before the gesture and the swipe hit Santiago Arcila's loaded row, which opened the existing removal confirmation. Cancel was tapped and the app terminated; no removal request was made, and the list still showed all three members afterwards (`g2-sim18-manage-gamblers-after-retry.png`). Placeholder swipe inertness rests on construction (no `SwipeToDismissBox` or button in the placeholder row) and the test below, not on that gesture.

### Pre-existing problems found (not changed)

- Append Retry is a no-op in `lazy-paging-swift` 0.0.3: `onRowAccess` calls `appendIfNeeded` then `prependIfNeeded`, and the latter overwrites `retryAction` with a prepend that cannot run. Logged on My pools: Retry tapped with `append=failure`, `retry()` returned in 4 ms without a load.
- Refresh storm: after switching Bets → History → Scores while those lists' first loads are pending, one paging source starts about 60 refreshes per second until loads complete. Reproduced identically on a `HEAD` build (shimmer) with the same staging, so it is not caused by the pulse.
- The drawer content stays in the hierarchy while closed, so its summary placeholder keeps pulsing (25–30% simulator CPU) until the summary loads.
- A refresh failure with loaded rows shows the full-screen error (library resolver), not the existing rows.

### Tests (2.4, 2.5)

Written after the exploration above (Swift Testing):

- `iOS/Pool/Tests/PoolTests/PoolPlaceholderSurfacesTests.swift` (8 tests, ViewInspector): placeholder My pools rows are inert and hidden even with a real model and never run open/invite; loaded rows run invite and announce the pool; placeholder and loaded template cards; the Manage gamblers loading list renders eight production items with no buttons and no per-row opacity; the placeholder member draws no `EmailAvatar`/`InitialAvatar`, never loads a photo, and is inert; a loaded member restores `EmailAvatar`.
- `iOS/Bet/Tests/BetTests/MatchBetListViewModelReloadTests.swift` (3 tests): first load shows loading; a reload keeps the loaded match while the request runs; a failed reload shows the existing failure and a retry shows loading again.
- `PoolScoreItemAccessibilityTests` changed mechanically (`placeholderModifier:` → `isPlaceholder: true`).

Positive controls (sources backed up, restored, `shasum`-verified): forcing hit testing on and accessibility visible in the three Pool components and re-adding row opacity failed 5 of the 8 Pool tests with 8 issues; always setting `.loading` in `loadPoolGamblerBet` failed the reload test.

Final runs on the restored sources (iOS 18.1 simulator, `xcodebuild test -scheme <Package> -parallel-testing-enabled NO`, totals from `xcresulttool`): `UI` 89/89, `Pool` 42/42, `Account` 24/24 (`AvatarWireProbeTests` skipped), `Bet` 7/7. Only the intended `swiftui-shimmer` pin removals appear in `Package.resolved`.

Settings restored on the simulator: appearance light, text size large, Increase Contrast off, Reduce Motion and `EnhancedBackgroundContrastEnabled` keys deleted; the final clean build is installed.

Not observed: VoiceOver traversal of the new placeholder rows; an on-screen run on iOS 26.5; the Work iPhone; pending-bet rows with real data (the available pools have no open matches); append placeholders on lists other than My pools.

Accepted without VoiceOver traversal for 2.1 by the user on 2026-10-01.
Accepted without real-data pending-bet rows and the bet-editing path for 2.2 by the user on 2026-10-01.
Accepted without an on-simulator large-text check of Manage gamblers (render-only) for 2.3 by the user on 2026-10-01.
Accepted without append placeholders on lists other than My pools for 2.4 by the user on 2026-10-01.
Accepted without an on-screen iOS 26.5 run, a Work iPhone run, VoiceOver, and confirmation that Reduce Transparency took effect for 2.5 by the user on 2026-10-01.
Accepted without stopping the drawer pool-summary pulse while the drawer is closed (it stops when the summary loads) by the user on 2026-10-01.

## Android — tasks 3.1–3.4 (2026-10-01)

Toolchain: AGP 9.2.1, Kotlin 2.3.21, Compose BOM 2026.05.00 (Compose UI 1.11.1), compile/target SDK 36, min SDK 28, all unchanged. On-device checks ran on the `medium_phone` AVD (API 36, `emulator-5554`, signed in, production backend, read-only); instrumented tests ran on the signed-out `Pixel_7_API_36_no_account` AVD. The Galaxy S8 was not used. Captures are in `verification/android/` with a `g3-` prefix.

### 3.1 Inventory, dependency, and mapping (Android)

`com.revenuecat.purchases:placeholder:1.0.2` is declared as `revenuecat-placeholder` in `gradle/libs.versions.toml` and added to `:ui` (theme values and `TrendIndicator`) and `:pool` (leaderboard row, `PositionIndicator`). It resolves from Maven Central with `placeholder-android:1.0.2`, which brings Compose Material 1.9.x (resolved to 1.11.1). Its manifest declares `minSdkVersion 23`, and the built prodDebug APK reports `minSdkVersion 28`. Accompanist Placeholder 0.36.0 stays in `:ui` for the remaining `Modifier.shimmer()` consumers until 4.5.

| Surface | Production component | Placeholder model | Loading slots | Effect before this change | Task |
| --- | --- | --- | --- | --- | --- |
| Leaderboard | `pool/gamblerscore/GamblerScoreItem` (+ `pool/PositionIndicator`, `ui/TrendIndicator`, `account/AccountAvatar`) | `poolGamblerScorePlaceholderModel()` | `GamblerScoreList`: `gamblerScorePlaceholderList` (initial), `gamblerScorePlaceholderItemRow` (append) | `placeholderModifier = Modifier.shimmer()` via the `GamblerScorePlaceholderItem` wrapper; flag inferred from modifier presence | 3.2 (done) |
| My pools | `pool/poolscore/PoolScoreItem` | `poolGamblerScorePlaceholderModel()` | `PoolScoreList` initial and append | `placeholderModifier: Modifier?`; flag inferred | 4.1 |
| My pools spotlight | `pool/poolscore/spotlight/PoolSpotlightItem` | — | `PoolScoreList` | `shimmer` | 4.1 |
| Pool templates | `pool/creator/PoolFromLayoutCreatorItem` | `poolLayoutFakeModel()` | creator list and My pools empty state | `shimmerModifier = Modifier.shimmer()` | 4.1 |
| Drawer pool summary | `app/poolhome/drawer/DrawerView` summary item, `app/poolhome/PoolHomeView` | `poolGamblerScorePlaceholderModel()` | drawer summary | `placeholderModifier: Modifier?`; flag inferred | 4.1 |
| Pending bets | `bet/pending/PendingBetItem` | bet placeholder model | `PendingBetList` | `PendingBetPlaceholderItem` + `shimmer` | 4.2 |
| Live / finished / history | `bet/live/LiveBetItem`, `bet/finished/FinishedBetItem`, `bet/timeline/BetTimelineList` | bet placeholder model | `FinishedBetList`, `BetTimelineList` | `LiveBetPlaceholderItem`, `FinishedBetPlaceholderItem` + `shimmerModifier` | 4.2 |
| Match header | `bet/match/MatchHeader` | bet placeholder model | `MatchBetListView` | `MatchHeaderPlaceholderItem` + `shimmerModifier` | 4.2 |
| Match gambler bets | `bet/match/MatchGamblerBetItem` | bet placeholder model | `MatchBetList` | `MatchGamblerBetPlaceholderItem` + `shimmerModifier` | 4.2 |
| Manage gamblers | `pool/managegamblers/ManageGamblersView` | none yet | loading list | separately maintained skeleton under `.shimmer()` | 4.3 |

Shared plumbing to remove in 4.5: `ui/ShimmerModifier.kt`, `ExtendedColorScheme.placeholder` (`#BDBDBD` / `#424242`), the Accompanist catalog entry and `:ui` dependency, and the legacy `placeholderModifier` parameters that `PositionIndicator` and `TrendIndicator` keep for `PoolScoreItem` and `DrawerView`.

Compilation: `./gradlew :ui:compileDebugKotlin :account:compileDebugKotlin :pool:compileDebugKotlin :pool:compileDebugAndroidTestKotlin :app:compileProdDebugKotlin` and `:app:assembleProdDebug` succeeded.

### 3.2 Theme values and representative row

Theme: `ui/theme/LoadingPlaceholderPulse.kt` (`LoadingPlaceholderPulse`: `color`, `highlight`, default `shape` `RoundedCornerShape(4.dp)`), provided by `TycheTheme` through `LocalLoadingPlaceholderPulse`. Components pass these values directly to RevenueCat's `Modifier.placeholder(color, shape, highlight)` on each content leaf. No public helper, caller-supplied modifier, or copied `Modifier.composed` wrapper exists. The modifier is applied only when `isPlaceholder` is true. Leaving it on loaded rows with `enabled = false` would make RevenueCat draw each loaded leaf in its own offscreen layer.

Representative row: `GamblerScoreItem(poolGamblerScore, isCurrentUser, modifier, isPlaceholder = false)` replaces `placeholderModifier`. The `GamblerScorePlaceholderItem` wrapper was removed, and both loading slots now call `GamblerScoreItem(poolGamblerScorePlaceholderModel(), isCurrentUser = false, isPlaceholder = true)`. Masks and shapes:

- rank digits: theme text shape inside the tile, while the tile keeps its `surfaceVariant` fill (`PositionIndicator(isPlaceholder)`)
- trend: theme shape, one mask element shared by arrow and digits (`TrendIndicator(isPlaceholder)`)
- avatar: `CircleShape` mask on the row's avatar slot; `AccountAvatar(isPlaceholder = true)` draws nothing and starts no request
- username, "You", score: theme text shape

Placeholder rows keep `clearAndSetSemantics` with no description and are never wrapped in the clickable gambler-detail container.

Leaf shapes (`g3-light-loading.png`, `g3-artemis-before_placeholder_tap.png`, `g3-dark-loading-animations-off.png`): each row shows a rounded tile with a small digit bar, a small trend square under it, a circle, a long name bar, and a score block, with production gaps and dividers and no full-row block.

Loaded appearance: the loaded leaderboard of this build and of a `HEAD` build (`git archive HEAD Android`, built in scratch) is pixel-identical below the app bar and above the bottom bar, in light and dark (0 differing pixels; `g3-{light,dark}-loaded.png` vs `g3-{light,dark}-loaded-head-build.png`).

### 3.3 Pulse, motion scaling, and appearance

Values: `infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse)`, no delay, starting dim. RevenueCat draws `color`, then the `Pulse` highlight at alpha = progress.

| Appearance | `color` (dim) | `Pulse` highlight | Static (`highlight = null`) |
| --- | --- | --- | --- |
| Light | black 9.6% | black 7.08% (composite 16%) | black 12.8% |
| Dark | white 10.7% | white 5.94% (composite 16%) | white 13.3% |

`TycheTheme` reads the composition's `MotionDurationScale`, which Compose backs with snapshot state following `Settings.Global.ANIMATOR_DURATION_SCALE`. At 0 the theme supplies the static midpoint with no highlight. At any nonzero scale Compose stretches the running pulse.

Method: the leaderboard paging query was temporarily delayed in `GamblerScoreListViewModel` (90 s, then 240 s). The file was backed up and `shasum`-verified on restore. A clean build (`--rerun-tasks`) was then installed, and its APK hash matches the device's `base.apk` (`0825ed3f…`). Brightness traces sample the first username mask (400 × 30 px) at 20 fps from `adb shell screenrecord`. Exact values come from `screencap` PNGs.

- Normal speed, light (`g3-pulse-light1.mp4`, `…-trace.txt`): 229 ↔ 212, minima at 1.02, 2.85, 4.60, 6.50, 8.33, 10.03 s and maxima every 1.75–1.85 s (mean 1.80 s). No pause, no moving band. The iOS 18.1 light trace was 212 ↔ 229. Exact pixels from a 14-frame `screencap` burst: 231 ↔ 215 over white, i.e. 9.4% → 15.7%.
- Normal speed, dark (`g3-pulse-dark1.mp4`): 40 ↔ 53 over a row background of 16 in the video (`#121212`), i.e. 10.0% → 15.5%, with a cycle of 1.72–1.90 s. The iOS dark trace was 25 ↔ 39 over black (9.8% → 15.3%).
- Scale 2 (`g3-pulse-scale2.mp4`): cycle 3.50–3.68 s. Scale 0.5 (`g3-pulse-scale05.mp4`): 0.85–0.95 s.
- Preference change while loading (`g3-pulse-liveoff.mp4`): with the scale set to 0 at about 4.0 s, the mask settled at a constant 219 from the next frame until the end of the recording. Light static exact pixels: 222 (12.9%, `g3-light-loading-animations-off.png`). The iOS light static trace was 221.
- Back on while loading (`g3-pulse-liveon.mp4`): with the setting deleted at about 3.0 s, pulsing resumed from dim with a 1.75–1.9 s cycle.
- Dark static (`g3-pulse-darkoff.mp4`, `g3-dark-loading-animations-off.png`): video 47 (13.0%); exact pixels 50 over 18 (13.5%).
- Fast response on the clean build (`g3-pulse-clean.mp4`): placeholders showed for about 0.5 s, and rows replaced them before the first cycle completed.
- No blur: the pulse is a solid translucent fill (`SolidColor` brush); edges stay sharp in every capture.

Settings changed and restored on `medium_phone`: `animator_duration_scale` (set to 2, 0.5, and 0, then deleted; reads `null`), and dark mode (`cmd uimode night yes`, then `no`).

### 3.4 Exploration, tests, and component contract

Device selection: `adb devices -l` listed the S8 and `emulator-5554`. `mobile_diagnose(device_serial="emulator-5554")` returned `ready`. During the first ARTEMIS run (`12afb82e-…`, stopped) My pools showed Retry because the emulator could not resolve host names (`ping tyche-api.felipearpa.com` → unknown host). The emulator was restarted (`adb emu kill`, then `mobile_diagnose(launch_avd="medium_phone")`); name resolution worked and the session was intact.

ARTEMIS Pro run `64f4d036-b3af-4cd8-b4ed-9712a7111a23` on the 240 s delayed build passed 3 of 3 checks (`g3-artemis-leaderboard-report.md`, `g3-artemis-{before,after}_placeholder_tap.png`):

- Placeholder rows (9 on screen) are `android.view.View` nodes with clickable, focusable, and long-clickable all false, empty text and content-desc, and no descendant carrying either.
- Tapping the first placeholder row did nothing: no ripple, navigation, or new screen.
- Real rows appeared about 4 min 49 s after the pool tap.
- Loaded "El mono" row: container clickable and focusable, child "Rank 1, El mono, 676 points, Rank unchanged". Tapping it opened that gambler's Timeline, and the in-app back arrow returned to Scores.
- Signed-in row: not clickable or focusable, announced as "Rank 8, felipearpa, You, 589 points, Rank unchanged". Tapping it did nothing.

Filler avatar requests on the device (`g3-app-tcp-connections-during-load.txt`): the app's TCP sockets (uid 10216, from `/proc/net/tcp{,6}`) were polled every second during a delayed load. From 19:06:04 until the rows arrived, no new connection opened, leaving only the API host, a Google host, and one `CLOSE_WAIT` socket to Amazon from the toolbar avatar at launch. At 19:06:54, when the rows loaded, four new S3 connections (3.5.x / 52.219.x) opened for the loaded avatars.

Tests written after this exploration (instrumented, `Pixel_7_API_36_no_account`):

- `pool/src/androidTest/.../RecordingImageLoaderRule.kt`: replaces Coil's singleton loader with one that records each request's data and answers with an error, so nothing reaches the network. Adds `androidTestImplementation(libs.coil.compose)` to `:pool`.
- `GamblerScorePlaceholderTest`, new tests:
  - A placeholder row built from a real-looking gambler (`real-gambler-1`, "ElGoleador") makes no avatar request within 2 s, shows no text or description, and has no click action.
  - The same row switched from `isPlaceholder = true` to `false` makes no request before the switch. After it, the row requests `…/avatars/real-gambler-1.jpg` (explicit `waitUntil`, 5 s) and announces "Rank 2, ElGoleador, 150 points, Up 1 place".
  - The existing tests were updated mechanically from the removed wrapper to `GamblerScoreItem(…, isPlaceholder = true)`.
- `ui/src/androidTest/.../theme/LoadingPlaceholderPulseMotionTest.kt` (drives `MotionDurationScale` as the effect context):
  - Turning animations off and on while composed switches between a `Pulse` with the dim fill, no highlight with the static fill, and back.
  - Dark appearance with animations off uses the dark static fill.

Positive controls: three temporary edits in one run:

- removing `AccountAvatar`'s `isPlaceholder` return failed both avatar tests (`…/avatars/real-gambler-1.jpg` was requested)
- ignoring the motion scale in `TycheTheme` failed both motion tests
- exposing the description on placeholder rows failed `placeholderRowIsInertForTalkBackAndActions`

These edits were reversed by hand, not from a checksum backup: the backup command failed because zsh did not split the file list. The `git diff` of the three files shows only the intended changes, and the suites below passed afterwards.

Runs on the restored sources:

| Command | Result |
| --- | --- |
| `:ui:testDebugUnitTest :account:testDebugUnitTest :pool:testDebugUnitTest :app:testProdDebugUnitTest` | 44, 85, 2, and 60 passed |
| `ANDROID_SERIAL=emulator-5554 :ui:connectedDebugAndroidTest` (motion test class) | 2/2 |
| `:pool:connectedDebugAndroidTest` (`GamblerScorePlaceholderTest`, `GamblerScoreListTest`, `GamblerScoreItemRankPreviewTest`) | 12/12 |
| Full `:ui`, `:pool`, `:account` connected suites | `ui` 74/74, `account` 10/10, `pool` 36/37 |

The single `:pool` failure is `PoolNameKeyboardTest.theNameFieldItsValidationAndDoneStayReachableAboveTheKeyboard[LANDSCAPE]` ("In landscape the form must overflow…"). It fails the same way on the `HEAD` build on this AVD, so this change did not cause it.

Public component contract (Android, as implemented):

- `GamblerScoreItem(poolGamblerScore, isCurrentUser, modifier, isPlaceholder: Boolean = false)`. With `true`, the caller supplies a placeholder model and nothing else. Each content leaf is masked with the theme pulse (static midpoint when animations are off), the row exposes no semantics, and the avatar makes no request. With the default, loaded behavior and pixels are unchanged.
- `PositionIndicator(…, isPlaceholder: Boolean = false)`: masks the digits only; the tile keeps its container color.
- `TrendIndicator(…, isPlaceholder: Boolean = false)`: masks arrow and digits with one shared mask element.
- `AccountAvatar(accountId, fallback, modifier, isPlaceholder: Boolean = false)`: with `true` it draws nothing and never requests a photo, whatever the id.
- `LoadingPlaceholderPulse` / `LocalLoadingPlaceholderPulse`: theme-owned fill, `Pulse` highlight, timing, and default shape. These are values, not a view helper.
- Until 4.1 and 4.5, `PositionIndicator` and `TrendIndicator` also keep their legacy `placeholderModifier` parameter for `PoolScoreItem` and `DrawerView`.

Not observed: TalkBack traversal itself (accessibility attributes came from the hierarchy and Compose semantics); append placeholders on the device (covered by `appendLoadingRendersTheSameSharedPlaceholderRow`); API 28 on a device (the S8 was excluded from this group); large text on the device.

Accepted without TalkBack traversal, an API 28 device run, and a large-text device check for 3.4 (deferred to 4.5) by the user on 2026-10-01.

## Android — tasks 4.1–4.5 (2026-10-02), device checks on medium_phone

Correction to 3.2 and 3.4: the notes "one mask element shared by arrow and digits" are inaccurate. RevenueCat creates a separate placeholder node for each element that applies the modifier, so the arrow and the digits each get their own mask. The comment in `ui/TrendIndicator.kt` was corrected (comment only); `:ui:compileDebugKotlin` passes.

Slice A (code, compilation, and JVM tests) was done earlier: JVM tests passed for `ui` (44), `account` (85), `pool` (2), `bet` (2), and `app` (60); a grep for Accompanist returns nothing; two retention fixes were made.

Device: `medium_phone` AVD (API 36, `emulator-5554`), signed in, production backend, read-only. The Galaxy S8 was not used. `mobile_diagnose(device_serial="emulator-5554")` returned `ready`. Captures are in `verification/android/` with a `g4-` prefix.

Method:

- ARTEMIS Pro run `d07c8b6b-32cd-45f3-9ec3-ada6c163db6c` explored the app read-only first and passed 7 of 7 checks (`g4-artemis-navigation-map.md`). It mapped My pools, the invite share sheet, pool tabs (Scores, Bets, History), the drawer, the match screen (Score), the gambler Timeline, and the template screen. It also found that the Bets tab is empty on this account, and that the Gamblers drawer entry appears only on pools the user owns ("Prueba").
- Loads were staged with a temporary OkHttp interceptor on the authenticated Ktor client (`session/di/AccountProvider.kt`). It read rules from `files/staging.txt`, which was written with `run-as` and could delay, fail, or empty a GET by path. It could also cut the first History page to two items with a fake cursor, so the append slot showed without scrolling. That fake cursor reached the server once, as a GET that returned 500; after that the next-page rule failed locally. The file was backed up and restored, and `shasum -c` reported OK. The staging file was removed from the app. A rebuilt clean APK has the same entry CRCs as the build made before staging (`unzip -v`), and its SHA-256 matches the device's `base.apk` (`9cc192d3…`).
- Pulse values come from 20 fps brightness traces of `screenrecord` (1080 × 2400; a 600 × 30 px region of the first name mask). Static values come from `screencap` bursts. Geometry was compared by finding the dividers in the PNGs.
- The emulator was overloaded after repeated installs: system_server reached about 150% CPU, the load average about 22, and Fortuna hit ANR or start-timeout kills while launching. Earlier ANR files on this AVD date from 2026-10-01. The emulator was restarted (`adb emu kill`, then `mobile_diagnose(launch_avd="medium_phone")`), and the app was compiled with `cmd package compile -m speed-profile`. Later runs were stable. No ANR stack was readable (`/data/anr` is system-only), so the stalls are attributed to the emulator, not shown to have another cause.

### 4.1 Pool surfaces

- My pools (`g4-mypools-loading-light.png`, `g4-mypools-loaded-light.png`): placeholder rows use the production row. The rank tile keeps its amber fill, and the digits, trend, name, points, members, and invite icon are masked separately. No filler glyph shows, and My pools rows have no avatar. The hierarchy has no nodes for placeholder rows; only Open menu and Create pool are clickable. Tapping a placeholder row and its invite mask did nothing: no navigation and no `intentresolver` activity. Loaded rows: the invite icon opened the system share sheet ("Sharing link"), which was dismissed with BACK without choosing a target (`g4-mypools-invite-sharesheet.png`), and tapping "Prueba" opened its Scores.
- Drawer pool summary (`g4-drawer-loading-light.png`, pool and pool-gambler GETs delayed): the summary card keeps its fill, and the badge, "Playing now", name, and rank/points are masked. The card exposes no node. Owner-only entries (Gamblers, Delete pool) are absent until the pool loads.
- Pool templates (`g4-templates-loading-light.png`, `g4-templates-loaded-light.png`): staging an empty My pools response reaches the empty state without changing data. "Popular templates" shows three placeholder cards with the card fill kept and the title, date, and chevron masked, and no nodes. Tapping one did nothing. The cards were then replaced by "Copa Mundial de la FIFA 2026 / Starting 6/11/26".

### 4.2 Bet surfaces

- Bets tab, pending (`g4-bets-pending-loading-light.png`): placeholder rows mask the flags, team names, bet fields, and date. Only the app bar and tab controls are clickable. Tapping a row and a bet field opened nothing, and the keyboard did not appear (`mInputShown=false`). After the response, the existing empty state "Nothing to show" appears (`g4-bets-pending-empty-light.png`). This account has no open matches, so loaded pending rows (and saving bets) could not be observed.
- History, finished bets (`g4-history-finished-loading-light.png`): masks cover the time, flags, names, scores, bet, and points, with production dividers.
- Match screen (`g4-match-loading-light.png`): the header masks the flags, team names, and date, and the gambler rows mask the name, bet, and points (these rows have no avatar). Only the back and home controls are clickable.
- Timeline (`g4-timeline-loading-light.png`): placeholder rows use the `BetTimeLineItem` placeholder model, which renders the live-bet layout (no points line). Tapping two placeholder rows during loading left the Timeline in place.
- Navigation from loaded rows: History row → Score; tapping a loaded gambler row ("Gracias guerreros", tapped 1 s after rows arrived) → that gambler's Timeline; the back arrow → Score, with the header and rows intact.
- Avatar host (`g4-app-tcp-connections-match-load.txt`, app uid 10216, polled each second from 08:43:13 to 08:45:02 across the match and timeline loads): no new S3 connection opened. Only the API host and one existing S3 socket appeared. None of the group 4 placeholder surfaces include an avatar except Manage gamblers. There, the placeholder circle is a mask and the loaded rows show letter avatars.
- Live rows: real data has no uncomputed bets, so no loaded `LiveBetItem` was seen. The timeline placeholder uses that layout.

### 4.3 Manage gamblers ("Prueba", owner)

- Loading (`g4-managegamblers-loading-light.png`): placeholder rows use `ManageGamblerItem` with a circle avatar mask, a name mask, and an email mask. There is no swipe box, no per-row fade, and no node. Rows, dividers, avatar circle, and text start match the loaded rows (`g4-managegamblers-loaded-light.png`): 150 px pitch, edge-to-edge dividers, avatar x 21–125, text x 146/147.
- Font scale 1.3 (`g4-managegamblers-{loading,loaded}-font13.png`): both have a 155 px pitch with dividers at the same y positions. Restored to 1.0.
- Failure (`g4-managegamblers-failure-light.png`): the existing "Unexpected error" text replaces the placeholders, and no animation remains. That initial error state has no Retry button (unchanged by this change), so the only retry is pull-to-refresh. That swipe on this screen was refused by the session's permission check, so retry here was not observed.
- No swipe, long-press, Edit, or remove control was used on this screen.

### 4.4 Loading transitions

- Fast reveal (`g4-fast-reveal.mp4`, `…-trace.txt`, 2.7 s delay): placeholders appeared at 4.60 s starting dim (229). Rows replaced them at 7.90 s, while the mask read 214 on its way back from the darkest point. A full cycle would have ended near 8.2 s at 229, so the reveal did not wait for a pulse boundary.
- Append (`g4-history-append-light.png`): with two loaded History rows and the next page pending, one placeholder item appears only after the last row; the loaded rows are unchanged. Tapping it changed nothing (title and loaded rows pixel-identical). On failure, the existing append error with Retry replaced it and kept the rows (`g4-history-append-after-light.png`). Retry brought the placeholder back at the end (`g4-history-append-retry-light.png`).
- Refresh (`g4-history-refresh-light.png`): pulling down History with the first page delayed kept the loaded rows and showed the refresh spinner, with no placeholders.
- Empty: My pools shows the template empty state, and pending bets show "Nothing to show".
- Failure and retry: My pools shows "Unexpected error" with Retry (`g4-mypools-failure.png`). Retry shows the refresh spinner over the error, then the rows. On the match screen, Retry after a failure shows placeholders and then content (`g4-match-retry-{loading,loaded}.png`).
- Match header after returning from a timeline (`g4-match-header-after-back-light.png`): with the reload delayed 45 s, the header and rows stayed loaded. When that reload fails instead, the whole screen is replaced by the error with Retry (`g4-match-reload-fail-6s.png`). That matches the view model (`onFailure` → `Failure`) and is listed as an open question.
- Templates did not revert on My pools refresh: the refresh re-requested only the pools, and the template card stayed (`g4-templates-refresh-light.png`).
- Navigating away mid-load (Manage gamblers, members delayed 120 s): on placeholders, `gfxinfo` counted 70 frames in about 3 s and the app used 26.9% CPU. After the back arrow, with the request still pending, it counted 0 frames and 0.0% CPU.

### 4.5 Pulse, motion, and insets on this device

- Light pulse (`g4-pulse-mypools-light.mp4`, trace): 212 ↔ 229, minima about 1.5, 3.3, 5.1, and 6.95 s (cycle ≈ 1.8 s). This matches group 3.
- Dark pulse (`g4-pulse-mypools-dark.mp4`, trace): 40 ↔ 53 over a background of 18, maxima about 1.1, 2.95, 4.7, and 6.5 s (≈ 1.8 s).
- Animations off (`animator_duration_scale 0`): light is a static 222 over 255 (`g4-mypools-loading-light-animoff.png`). Dark is a static 50 over 18, both when set before launch and when switched while loading (5 identical `screencap` samples, `g4-mypools-loading-dark-animoff.png`). These match group 3's static values. A static screen produced almost no `screenrecord` frames.
- Insets: loading and loaded rows share divider extents and pitch in My pools (x 21–1058, 183 px), the match screen (x 42–1037, 113 px, header unchanged), History (x 42–1037; a placeholder item is 339 px, the same as a loaded item without a date header), Timeline (x 84–995), and Manage gamblers (x 0–1079, 150 px).

Settings changed and restored on `medium_phone`: dark mode (`cmd uimode night yes`, then `no`), `animator_duration_scale` (set to 0, then deleted; reads `null`), and `font_scale` (1.3, then 1.0, the original value). The app was force-stopped and relaunched several times, and the emulator was restarted once. Fortuna was never uninstalled, cleared, or signed out; the clean build launched signed in with three pools.

Not observed on this device: loaded pending and live rows (no data); Manage gamblers retry (needs a swipe, which was refused); TalkBack traversal, API 28 and the S8, and the instrumented test runs (next slice).

Accepted for 4.4 by the user on 2026-10-02: a failed automatic match reload after returning from a timeline replaces the loaded match with the existing full-screen error and Retry (same as iOS); this is not treated as a user-requested refresh.
Accepted without observing Manage gamblers retry (pull-to-refresh) for 4.3 by the user on 2026-10-02.

## Android — tasks 4.2, 4.3, 4.5 (2026-10-02), tests, TalkBack and API 28

Devices: `medium_phone` (API 36, `emulator-5554`, signed in, production backend, read-only) and the signed-out `Pixel_7_API_36_no_account` AVD for connected tests. The Galaxy S8 was not used (see below). Captures are in `verification/android/` with a `g4c-` prefix.

Method:

- ARTEMIS Pro run `289230a4-9958-4178-b921-2e6125a39dea` explored the pool "Copa Mundial de la FIFA prur" read-only first and passed 2 of 2 checks. The pool's Bets tab has one pending bet (Colombia vs Portugal, 10/2/26, 8:00 PM) in a single list grouped by date. No live bet exists in this pool (Scores holds the ranking, History holds finished bets). The run opened Edit, focused the home field, closed the keyboard with BACK, and left with Cancel; nothing was saved.
- Loads were staged with a temporary OkHttp interceptor in `session/di/AccountProvider.kt`, as in slice B. It read `files/staging.txt` (written with `run-as`) and delayed GETs whose path matched a regex. As an extra guard, it failed every non-GET request locally, so no write could reach production; logcat showed no blocked request, so the app attempted no write. The file was backed up first. Afterwards, the staging file was removed from the app, the source was restored, and `shasum -a 256 -c` reported OK; `git status` shows no change under `Android/session`. A clean `:app:assembleProdDebug --rerun-tasks` APK contains no staging string and was installed with `adb install -r`. The device's `base.apk` SHA-256 equals the local APK (`875ba01b…`). It differs from slice B's `9cc192d3…` only in `classes*.dex` entries (a rebuild, not a source difference in the staged file), and the app launched signed in with three pools.

### 4.2 Loaded pending and live bets

- Loading (`g4c-bets-pending-loading-light.png`, from `g4c-pending-reveal.mp4`, pending and live GETs delayed 8 s): the placeholder rows are the production `PendingBetItem` with masked flags, team names, bet fields, and date, plus production dividers (x 42–1031/1037). Team rows sit 105 px apart, the same spacing as the loaded row (Colombia y 382 → Portugal y 487). The placeholder item has no date header and no status/Edit row, because the list composes those around the item only for real bets; so its pitch (292 px) is shorter than the loaded group. The hierarchy had no clickable or labelled node in the list, and the keyboard did not appear (`mInputShown=false`).
- Reveal (`g4c-pending-reveal-trace.txt`, 20 fps, crop over the first name mask): the pulse cycled with minima at about 2.2, 4.0, 5.8, 7.7, and 9.45 s (about 1.8 s). The last placeholder frame (10.66 s) read 243, partway down from the 248 peak at 10.3 s. The next frame (10.69 s, `g4c-pending-reveal-first-loaded-frame.png`) already shows the real row, so the reveal did not wait for a pulse boundary.
- Loaded row (`g4c-bets-pending-loaded-light.png`): the date header "10/2/26", Colombia and Portugal with flags, "10/2/26, 8:00 PM", the status icon, and Edit. Edit opened the score fields with Cancel and a disabled Save. Tapping the home field focused it (green border) and showed the keyboard (`mInputShown=true`, `g4c-bets-pending-loaded-edit-focus-light.png`). BACK closed the keyboard, and Cancel returned the row to view mode. Nothing was typed or saved.
- Live rows: this pool has no live bet, and the earlier slice found none either. No loaded `LiveBetItem` was observed.

### 4.5 TalkBack exclusion (deferred from 3.4)

- Baseline: `enabled_accessibility_services` = `com.artemis.helper/.ArtemisAccessibilityService`, `accessibility_enabled` = 1. TalkBack was enabled by appending `com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService`.
- Swipe navigation was not practical. TalkBack placed its initial focus on "Open menu" (`g4c-tb-initial-focus-open-menu.png`, a genuine control, not a placeholder). After that, injected `input swipe` gestures, Alt+arrow key combinations, and explore-by-touch taps did not move TalkBack focus. This check therefore fell back to accessibility-tree dumps (`uiautomator dump`) taken with TalkBack running, while placeholders were on screen (My pools list GET delayed 25 s; leaderboard and pending bets delayed 75 s).
- Placeholders (`g4c-tb-{mypools,scores,bets}-loading.{png,xml}`): My pools had 10 placeholder nodes, Scores had 16, and Bets had 6. All were empty `android.view.View` nodes with no text, no content description, `focusable=false`, and `clickable=false`, which TalkBack skips. The surrounding controls stayed reachable: Open menu, Create pool (My pools), the swap control, and the Scores, Bets, and History tabs were focusable.
- Loaded (`g4c-tb-mypools-loaded.xml`, `g4c-tb-scores-loaded.{png,xml}`, `g4c-tb-bets-loaded.{png,xml}`): the My pools rows were focusable buttons labelled "Copa Mundial de la FIFA 2026, Rank 8, 589 points, 13 members, Rank unchanged" and so on. The leaderboard row was labelled "Rank 1, felipearpa, You, 41 points, Rank unchanged". The pending row exposed "Colombia", "Portugal", "10/2/26, 8:00 PM", the "10/2/26" header, and a focusable Edit.
- Spoken output was not captured. The dumps show the nodes TalkBack reads, not what it said.
- TalkBack ended OFF: `enabled_accessibility_services` reads `com.artemis.helper/.ArtemisAccessibilityService` again, `accessibility_enabled` is 1, and `dumpsys accessibility` reports `touchExplorationEnabled=false`. This was confirmed again after the emulator restart.

### 4.5 Below API 31 (Galaxy S8, API 28)

Not done. `adb -s ce0417143a6ab8130c install -r` of the temporary staging build was refused by the session's permission check, so no build was installed on the S8 and no setting was changed there. Its Fortuna 2.0.0 (versionCode 12, debuggable) is untouched. Its read-only baseline was font_scale 1.0, animator, transition, and window scales 1.0, and no accessibility services. Pulse, blur, animations-off, and font-scale checks on API 28 are still open.

### Instrumented tests (`Pixel_7_API_36_no_account`, `ANDROID_SERIAL=emulator-5554`, AVD name checked before each run)

medium_phone was stopped with `adb emu kill`. The test AVD was started through `mobile_diagnose(launch_avd=…)`. The first combined Gradle run died with `OutOfMemoryError` in dex merging under the 2 GB daemon heap from `gradle.properties`. Each module was then run on its own with `-Dorg.gradle.jvmargs=-Xmx4096m` (command line only, no file change).

- `:ui:connectedDebugAndroidTest`: 74 tests, 73 passed. `PushDrawerAccessibilityTest.given_a_touch_opened_drawer_…_then_focus_moves_to_that_action` failed with `ComposeNotIdleException` (idling resource timed out). A rerun of the class passed all 15 tests, so the failure was flaky.
- `:pool:connectedDebugAndroidTest`: 37 tests, 36 passed. The only failure is the known pre-existing `PoolNameKeyboardTest…[LANDSCAPE]` ("In landscape the form must overflow…").
- `:bet:connectedDebugAndroidTest`: 8 tests, 5 passed, including the placeholder accessibility tests. The 3 `BetTextFieldTest` failures are pre-existing: the tests look for a `textField` tag that `BetTextField` never sets.
- `:account:connectedDebugAndroidTest`: 10 tests, 9 passed. `SignInKeyboardTest.passwordSignInKeepsTheDraftWhenTheKeyboardClosesAndReopens[LANDSCAPE]` failed with "Form bottom expected:<1017.0> but was:<849.0>". A rerun of the class passed all 8 tests. The sign-in form does not use the changed `AccountAvatar`, so this is a flaky landscape keyboard measurement, not this change.

Afterwards the test AVD was stopped, and medium_phone was relaunched through `mobile_diagnose(launch_avd="medium_phone")`. Fortuna launched there signed in with three pools, and `base.apk` still matches `875ba01b…`.

Settings changed and restored on `medium_phone`: `enabled_accessibility_services` (TalkBack added, then restored exactly to the recorded value). Font scale, animation scales, and dark mode were not changed and read 1.0, `null`, and "no". Fortuna was never uninstalled, cleared, or signed out. Nothing was saved, sent, created, edited, or deleted.

Not observed: a loaded live bet row (no data); TalkBack swipe traversal and spoken output (the tree-dump fallback was used); every API 28 check on the S8 (install refused).

Accepted without a loaded live-bet row observation for 4.2 (no live bets in the data) by the user on 2026-10-02.
Accepted the 4.5 TalkBack exclusion check by accessibility-tree dumps with TalkBack running, without a spoken swipe traversal, by the user on 2026-10-02.

## Android — task 4.5 (2026-10-02), API 28 on Galaxy S8

Device: Galaxy S8 (SM-G955F, `ce0417143a6ab8130c`, Android 9 / API 28), signed in, production backend, read-only. Captures are in `verification/android/` with a `g4s8-` prefix. This closes the "Below API 31" item recorded as not done above.

Method: the same temporary interceptor in `session/di/AccountProvider.kt` (backed up first, SHA-256 `533a1a8f…`). It read `files/staging.txt` (written with `run-as`), delayed GETs whose path matched `^/gamblers/[^/]+/pools$` (the My pools list), and failed every non-GET locally; logcat logged each delay and no blocked request. The staging build was installed with `adb install -r` and kept the session (Fortuna 2.0.0, versionCode 12, three pools). Screen recordings (`screenrecord --size 540x1110`) were split at 20 fps, and each trace is the mean luma over the first row's name mask (x 120–380, y 156–168 of 540×1110). The S8's encoder repeats frames, so the traces have short plateaus.

- Pulse (`g4s8-mypools.mp4`, `g4s8-mypools-trace.txt`, `g4s8-mypools-loading-light.png`, 9 s delay): minima at 8.45, 10.25, 12.05, and 13.85 s and maxima at 9.30, 11.15, 12.95, and 14.75 s, so the cycle is 1.8 s. The mask swings between 214 and 230 on a 253 background, so both endpoints are light grey. Every pixel in the mask has the same value in each frame, and its edges step from mask to background within 2 px (for example 214 → 235 → 253 vertically), so there is no blur on API 28.
- Masks: each row is the production `PoolScoreItem` with a mask on each leaf (pool name, points, member count, rank digit, rank-change indicator, invite icon). The rank badge keeps its yellow fill with the digit masked, and the production dividers are kept.
- Reveal (`g4s8-mypools-reveal.mp4`, `g4s8-mypools-reveal-trace.txt`, 4 s delay): the placeholder appeared at 7.85 s at 230, fell to 214 at 8.75 s, rose to 230 at 9.70 s, and fell to 214 at 10.55 s. The last placeholder frame (11.40 s, `g4s8-mypools-reveal-last-placeholder-frame.png`) read 224 while rising; the next frame (11.45 s, `g4s8-mypools-reveal-first-loaded-frame.png`) shows the real rows, so the reveal did not wait for a pulse boundary. A notification from an unrelated app covers the toolbar in the loaded frames.
- Animations off (`animator_duration_scale` 0, `g4s8-mypools-animoff.mp4`, `g4s8-mypools-animoff-trace.txt`, `g4s8-mypools-loading-animoff-light.png`, 12 s delay): the mask stayed at 221 from 7.80 s to the end of the 15 s recording, with no change, at the midpoint of the 214–230 range.
- Font scale 1.3 (`g4s8-mypools-loading-font130-light.png`, `g4s8-mypools-loaded-font130-light.png`): the placeholder name mask grows to two lines and the points and members masks sit below it; rows do not overlap or clip. Loaded, "Copa Mundial de la FIFA 2026" wraps to two lines and every row shows its name, points, members, rank, and invite icon without clipping.
- Dark mode was not checked: `dumpsys uimode` reports `mNightModeLocked=true` on this device, and `cmd uimode night yes` left night mode at "no" (`ui_night_mode` stayed 1).

Restore: the staging file was removed from the app, the source was restored from the backup, `shasum -a 256 -c` reported OK, and `git status` shows no change under `Android/session`. A clean `:app:assembleProdDebug --rerun-tasks` APK contains no staging string, its SHA-256 is `875ba01b…` (the same as the medium_phone build above), and after `adb install -r` the S8's `base.apk` SHA-256 matches it. Fortuna launched signed in with three pools (`g4s8-mypools-loaded-light.png`).

Settings on the S8: `animator_duration_scale` 1.0 → 0 → 1.0 and `font_scale` 1.0 → 1.3 → 1.0. Transition and window scales (1.0), accessibility services (none), and `ui_night_mode` (1) were read again at the end and are unchanged. Fortuna was never uninstalled, cleared, or signed out. Nothing was saved, sent, created, edited, or deleted.

Not observed on API 28: the leaderboard or bet lists (only My pools was staged), dark mode (locked on this device), and TalkBack.

Accepted without dark mode, the leaderboard and bet lists, and TalkBack on API 28 (Galaxy S8; only My pools staged, night mode locked) for 4.5 by the user on 2026-10-02.

## Android and iOS — tasks 5.1–5.2 (2026-10-02)

No new device runs. 5.1 reuses the group 1 and group 3 leaderboard recordings and screenshots; 5.2 checks the code, the guidance files, and the specs. New files are in `verification/` with a `g5-` prefix: `g5-leaderboard-row-phases.png` (the first leaderboard row at dim, midpoint, and bright phases, iOS and Android, light and dark, taken from the recordings) and `g5-phase-measurements.txt` (regions, method, and the numbers below). No source, theme value, or setting was changed, and no device or simulator was touched.

### 5.1 Cross-platform appearance and final endpoints

Method: the same element on both platforms, the first leaderboard row's username mask, sampled at 20 fps from `ios/sim18-light-pulse-reduce-motion-on.mp4` (5.0–20.3 s), `ios/sim18-dark-static-reduce-motion-off.mp4` (20.5–47.5 s), `android/g3-pulse-light1.mp4`, and `android/g3-pulse-dark1.mp4`. Each frame's mask is compared with the row background in the same frame, so both platforms go through the same video encoding. Dim and bright are the 2nd and 98th percentiles; midpoint is halfway between them. The fill is (background − mask) / background in light and (mask − background) / (255 − background) in dark. Perceived contrast is given as the CIELAB lightness difference (ΔL\*) and the WCAG contrast ratio against each platform's own background: white in light, black on iOS dark, and `#121212` on Android dark. Exact values come from simulator screenshots and Android `screencap` PNGs where those exist.

Final endpoints. The theme constants are renderer inputs: iOS scales the foreground opacity of native redaction, which itself draws at 16% of the foreground style; RevenueCat draws `color` and then the `Pulse` highlight at alpha = progress over it. The design table's 12/20/16% (light) and 16/24/20% (dark) are design targets, not renderer inputs. Per the calibration decision recorded in 1.5, 16% is the bright endpoint on both platforms because iOS native redaction cannot exceed it.

| Appearance | Platform | Theme constants (`LoadingPlaceholderPulse`) | Target composite fill (dim / static / bright) | Exact pixels (PNG) | Video, in-frame fill (dim / mid / bright) | ΔL\* vs own background (dim / mid / bright) | WCAG ratio (dim / mid / bright) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Light | iOS | `.primary` foreground opacity 0.6 / 0.8 / 1.0 | black 9.6% / 12.8% / 16% over white | dim phase 230 (9.8%, `ios/sim18-02-…`) | 228.7 / 221.1 / 213.5 over 253 → 9.6% / 12.6% / 15.6% | 8.5 / 11.2 / 13.9 | 1.24 / 1.33 / 1.44 |
| Light | Android | `color` black 0.096, `Pulse` highlight black 0.0708, static black 0.128 | black 9.6% / 12.8% / 16% over white | burst 231 ↔ 215 (9.4% → 15.7%), static 222 (12.9%) | 229 / 220.5 / 212 over 253 → 9.5% / 12.8% / 16.2% | 8.4 / 11.4 / 14.4 | 1.24 / 1.34 / 1.46 |
| Dark | iOS | `.primary` foreground opacity 2/3 / 5/6 / 1.0 | white 10.7% / 13.3% / 16% over black | static 34 (13.3%, `ios/sim18-05-…`), bright phase 41 (16.1%, `ios/sim18-06-…`) | 24.7 / 31.5 / 38.4 over 0 → 9.7% / 12.4% / 15.0% | 9.9 / 13.2 / 16.6 (exact) | 1.22 / 1.32 / 1.44 (exact) |
| Dark | Android | `color` white 0.107, `Pulse` highlight white 0.0594, static white 0.133 | white 10.7% / 13.3% / 16% over `#121212` | static 50 over 18 (13.5%, `android/g4-mypools-loading-dark-animoff.png`) | 40 / 46.5 / 53 over 16 → 10.0% / 12.8% / 15.5% | 12.3 / 15.3 / 18.0 (exact) | 1.33 / 1.46 / 1.60 (exact) |

The dark ΔL\* and WCAG columns use exact pixels, or the composite the theme produces where no exact frame exists (iOS dim 27, Android dim 43 and bright 56). In the dark videos the encoder lowers values by about 2 levels (iOS static 32 in the video, 34 in the screenshot; Android `#121212` reads 16 in the video, 18 in the PNG). Black clips at 0, so the iOS dark video understates the mask's contrast against its background.

Results:

- Light: the two platforms match. Fill, ΔL\*, and amplitude agree within half an L\* unit at every phase (amplitude ΔL\* 5.4 on iOS, 6.0 on Android).
- Dark: both platforms reach the same fill fraction of the distance from their background to white, so the treatment is matched as calibrated. Against its own background, Android's mask is about 2 L\* units more prominent at each phase (12.3–18.0 vs 9.9–16.6), because the same white fraction over `#121212` sits higher on the lightness curve than over black. Amplitude is comparable (ΔL\* 5.7 Android, 6.7 iOS). That is about one just-noticeable difference, and the platforms are never seen side by side, so no theme value was changed. Matching ΔL\* instead would put Android dark at about 8.6% → 14.7%, static 11.6%; this is left as a question.
- Both platforms: masks stay visible at the dimmest point (WCAG ≥ 1.22 against the background) and restrained at the brightest (≤ 1.60). No filler glyph shows through in any phase (`g5-leaderboard-row-phases.png`).
- Cycle at normal speed: Android 1.80 s in light and dark (successive extrema 1.75–1.85 s). iOS dark 1.80 s (minima 21.65 → 46.85 s over 14 cycles); iOS light 1.82 s between minima and 1.86 s between maxima. The iOS recordings are 15 fps, so individual extrema move by a frame. Both themes use 0.9 s legs, symmetric ease-in-out, and no pause. Earlier traces agree: iOS My pools minima every 1.8 s (group 2), Android My pools ≈ 1.8 s (4.5), and Galaxy S8 1.8 s (API 28).
- Production layout: each platform keeps its own production row in every phase (rank tile with its structural fill, circular avatar, username, score, trend), with production gaps and dividers and no full-row block. Mask lengths follow each platform's production text layout: the iOS username mask follows the redacted filler text, and the Android mask fills the name slot. The design accepts this difference ("pixel-identical native text geometry" is a non-goal). Geometry against loaded rows was verified in 1.3, 2.1–2.3, 3.2, 4.3, and 4.5.

Notes on earlier dark numbers (recorded here; earlier sections are unchanged):

- The 4.5 line "40 ↔ 53 over a background of 18" pairs video mask values with the exact PNG background. In that video (`android/g4-pulse-mypools-dark.mp4`, My pools pool-name mask) the background reads 16, the same as in group 3's leaderboard video. Normalized to 16, both give 10.0% → 15.5%, so slice B and group 3 agree. Normalizing to 18 would wrongly give 9.3% → 14.8%. The two measured different elements (pool name vs. username) and got identical values because both use the same theme fill.
- `android/g3-dark-loading-animations-off.png` reads 54 over 18 (15.2%) on every mask, not the static 50 that 3.3 attributes to it. It looks like a frame captured during a pulse near the bright phase, before the animations-off setting took effect: `g3-pulse-darkoff-trace.txt` pulses until about 1.95 s and then settles. The dark static value of 50 over 18 (13.5%) comes from `android/g4-mypools-loading-dark-animoff.png` (five identical samples in 4.5) and matches the theme's 13.3%.

### 5.2 Reconciliation, contract, and validation

Code checks (working tree on `feature/skeleton`):

- `grep -rn -i "shimmer\|placeholderModifier\|ShimmerModifier\|accompanist" iOS Android --include='*.swift' --include='*.kt' --include='*.kts' --include='*.toml'` (excluding `build/`) returns nothing. The same grep over every `Package.swift`, `Package.resolved`, and `project.pbxproj` returns nothing. `ShimmerModifier.swift`, `ShimmerModifier.kt`, and `MatchGamblerBetPlaceholderItem.kt` are deleted.
- `isPlaceholder` defaults to `false` on every public or module-level component it was added to. iOS: `GamblerScoreItem`, `PoolScoreItem`, `PoolFromLayoutCreatorItem`, `ManageGamblerItem`, `PositionIndicator`, `TrendIndicator`, `AccountAvatar`, `PendingBetItem`, `LiveBetItem`, `FinishedBetItem`, `BetTimelineItem`, `MatchHeader`, `MatchGamblerBetItem`. Android: the same components (`BetTimeLineItem` on Android) plus the private `PoolSummaryItem` in `DrawerView.kt` and the private `ManageGamblerRow`. The one exception is iOS's private `PoolSummaryItem` in `PoolHomeDrawerView.swift`, a file-private struct with a memberwise initializer and no default; it already had the flag before this change, and both of its call sites pass it explicitly (`true` for the placeholder, `false` for the loaded summary).
- No caller passes an effect modifier. On Android, `com.revenuecat.placeholder` is imported only by the theme, the motion test, and the components that render their own masks (`TrendIndicator`, `PositionIndicator`, `GamblerScoreItem`, `PoolScoreItem`, `PoolFromLayoutCreatorItem`, `ManageGamblerItem`, `DrawerView`, and the five bet items). No list or screen imports it, and the only `LocalLoadingPlaceholderPulse` readers are those components and `Theme.kt`. On iOS, `LoadingPlaceholderPulse` and the pulse views are referenced only by the components and the theme file.
- No new public helper exists. The new source files are `LoadingPlaceholderPulse.swift` and `LoadingPlaceholderPulse.kt` (theme values) and the iOS `PulsingPlaceholderContent` structs in `Pool` and `Bet`, which are module-internal with no `public` declaration. The drawer's `PulsingSummaryContent` is file-private. The `loadedForeground` and `trendForeground` view extensions are internal or private. On Android, `systemAnimationsEnabled()` is internal, and no `Modifier.composed` wrapper or new `Modifier` extension was added; the existing public extensions (`disabledGestures`, `scoreWidth`, `navigationEmailAvatar`) predate this change. RevenueCat Placeholder 1.0.2 is declared in the catalog and used by `:ui`, `:pool`, `:bet`, and `:app`.
- `PoolSpotlightItem` (listed in 3.1) had an unused `shimmerModifier` parameter, which was removed; its only caller is its own preview, so it has no loading call site.
- Guidance: `AGENTS.md` and `openspec/config.yaml` both refer to "the shared loading-placeholder treatment defined by the `loading-placeholders` capability, selected through the component's `isPlaceholder` parameter rather than a caller-supplied effect modifier". Both keep the production-model, no-skeleton-layout, suppression, no-real-content, and no-retroactive-revision rules. The icon rules are unchanged.
- Spec deltas: every `shimmer` mention in `openspec/specs/` is in one of the five modified capabilities. Each delta's requirement heading matches the main spec exactly, and each keeps the main spec's full scenario set (leaderboard 8/8, pool-score-list 3/3 and 4/4, drawer 3/3, liquid glass 1/1, edge-to-edge 2/2). `loading-placeholders` is new (no main spec yet).

Validation: `PATH=/Users/felipe/.nvm/versions/node/v24.11.1/bin:$PATH openspec validate unify-pulse-loading-placeholders --strict` → "Change 'unify-pulse-loading-placeholders' is valid" (exit 0).

Inventory check: every surface in the iOS (1.1) and Android (3.1) inventories has a migrated row in the group 2 and group 4 records: leaderboard, My pools, My pools templates, creator templates, drawer summary, pending, live/finished/history/timeline, match header, match gambler bets, and Manage gamblers. The spotlight item is covered by the note above.

Requirement and scenario reconciliation ("Accepted" refers to the user acceptance lines above; "Limitation" means not observed here and not covered by an acceptance line):

| Capability / requirement | Scenario | Evidence | Limitations and acceptances |
| --- | --- | --- | --- |
| loading-placeholders / Loading presentation reuses production components | First content request is pending | iOS 2.1–2.3 component table and `g2-render-*`; Android 3.2, 4.1–4.3; Manage gamblers skeletons removed on both (2.3, 4.3) | Pending-bet placeholders omit the date header and status row that the list adds around real bets (4.2, Android); recorded deviation |
| | Text size or available width changes | iOS accessibility-2 renders and the simulator at accessibility-large (2.1–2.3); Android font 1.3 on Manage gamblers (4.3) and My pools on the S8 (4.5) | Limitation: width changes (rotation, larger windows) were not exercised; layouts are shared by construction. Accepted: Manage gamblers large text on iOS is render-only (2.3) |
| | Placeholder content is redrawn | Fixed placeholder factories (iOS Manage gamblers id fixed, 2.3); masks keep constant extents across every trace; models are presentation-only (no repository or persistence change) | — |
| loading-placeholders / Components expose explicit placeholder state | Existing loaded caller omits the parameter | Defaults above; Android loaded leaderboard pixel-identical to `HEAD` (3.2); loaded rows unchanged on iOS (2.1–2.3) | — |
| | Caller requests a placeholder | Callers pass only a model and `isPlaceholder: true` (code checks above); tests in 1.5, 2.4, and 3.4 | — |
| | A request runs while real content remains available | Refresh keeps rows (iOS `g2-sim18-mypools-refresh-keeps-rows.png`, Android `g4-history-refresh-light.png`); templates and match header retained during reload (2.4, 4.4) | Accepted: a failed automatic match reload shows the full-screen error (4.4, both platforms). Android template refresh failure replaces templates (left as is) |
| loading-placeholders / Placeholders use a shared non-spatial pulse | Normal loading animation | 5.1 cycle; traces in 1.4, 2.4, 3.3, 4.5; no band, no blur, constant mask extents | — |
| | A row contains several placeholder elements | Per-leaf masks (3.2, 4.1–4.3; iOS 2.1); only leaf items pulse in `BetTimelineItem` (2.2); RevenueCat one node per leaf (4.1 correction) | — |
| loading-placeholders / Placeholder contrast follows the active appearance | Light or dark appearance is selected | 5.1 endpoint table | Dark: Android masks about 2 ΔL\* more prominent than iOS (question below). Not checked: dark mode on API 28 (S8 night mode locked; accepted for 4.5) |
| | A placeholder overlays a colored container | Rank tile keeps `surfaceVariant`/amber with only digits masked and no tint (1.3, 3.2, 4.1, S8); indicators drop semantic colors on iOS (1.3) | iOS observation: redacted images render slightly dimmer than redacted text (2.1) |
| loading-placeholders / Placeholder content is inert and excluded from meaningful accessibility | Placeholder row is tapped or traversed | Taps inert on both (1.4, 2.4, 3.4, 4.1–4.4); Android hierarchy and TalkBack tree dumps (3.4, 4.5); iOS tests (hidden, no hit testing, no `Button`) | Accepted: no VoiceOver traversal (1.4, 1.5, 2.1, 2.5); TalkBack checked by tree dumps without spoken output (4.5). Limitation: hardware-keyboard activation was not exercised on either platform; placeholders expose no focusable or clickable node, so there is nothing to activate |
| | Placeholder avatar enters the visible area | iOS CFNetwork log (1.4) and `AccountAvatar` tests; Android TCP polling (3.4, 4.2) and the `RecordingImageLoaderRule` tests | — |
| | Real data replaces filler content | iOS loaded restoration test and simulator (1.4, 1.5); Android `isPlaceholder` true → false test and on-device navigation (3.4, 4.2) | — |
| loading-placeholders / Motion preferences and platform compatibility | Reduced motion is enabled during loading | iOS live Reduce Motion (1.4); Android animations off live (3.3, 4.5) and on the S8 (4.5); static midpoints in 5.1 | — |
| | Android animation speed is changed | Scale 2 → 3.50–3.68 s and 0.5 → 0.85–0.95 s (3.3); motion test | — |
| | Older supported system displays loading content | iOS 18.1 simulator (all iOS runs); Android API 28 on the S8 (4.5), with no blur | Accepted: no on-screen iOS 26.5 or physical iPhone run (2.5); API 28 checked on My pools only, without dark mode or TalkBack (4.5) |
| | Contrast or transparency preferences change | iOS Increase Contrast with Reduce Transparency (2.4) | Accepted: Reduce Transparency's effect not confirmed (2.5). Limitation: no Android contrast preference (high-contrast text or system contrast level) was exercised |
| loading-placeholders / Loading transitions preserve available content | A subsequent page is pending | iOS My pools append (2.4); Android History append (4.4) and the leaderboard append test (3.4) | Accepted: iOS append only on My pools (2.4) |
| | Loaded content is refreshed | iOS My pools and Android History refresh (2.4, 4.4) | iOS library: a refresh failure with loaded rows shows the full-screen error (pre-existing) |
| | A fast response arrives mid-pulse | iOS (2.4), Android (3.3, 4.4, 4.2 pending reveal), S8 (4.5) | — |
| | Loading finishes without content | Empty states on both (2.4, 4.4); failure and Retry on My pools, the match screen, and Manage gamblers | Android History and Manage gamblers first-load errors have no Retry button (pre-existing); accepted: Manage gamblers pull-to-refresh retry not observed (4.3) |
| | Loading presentation is removed | iOS CPU drops to 0% after data or navigation (2.4); Android `gfxinfo` 0 frames after back (4.4) | Accepted: the iOS drawer summary keeps pulsing while the closed drawer stays composed, until the summary loads (group 2) |
| pool-leaderboard / Leaderboard interaction and paging states | Initial request is pending | 1.3–1.4, 3.2–3.4 | — |
| | Append request is pending | Android test `appendLoadingRendersTheSameSharedPlaceholderRow` (3.4); iOS append slot uses the same `GamblerScorePlaceholderRow` (1.4, code) | Accepted for iOS: append only observed on My pools (2.4). Limitation: no on-screen leaderboard append on Android; the instrumented test covers it |
| | Placeholder row is inert | 1.4, 1.5 tests, 3.4 (ARTEMIS, TCP, tests), 4.5 TalkBack dumps | As for inertness above |
| | Open another gambler | iOS 1.4–1.5; Android 3.4 | — |
| | Activate the signed-in gambler row | iOS 1.5; Android 3.4 (not clickable or focusable) | — |
| | Refresh leaderboard | Paging infrastructure unchanged; refresh retention seen on other lists (2.4, 4.4) | Limitation: pull-to-refresh on the leaderboard itself was not observed in this change |
| | Append request fails | Android History append failure keeps rows with Retry (4.4); iOS My pools (2.4) | Limitation: not observed on the leaderboard itself |
| | Retry append request | Android History Retry (4.4) | iOS Retry is a no-op in lazy-paging-swift 0.0.3 (pre-existing, out of scope) |
| pool-score-list / Loading placeholder rows are not announced | List is loading its first page | Android TalkBack dumps (4.5); iOS `PoolPlaceholderSurfacesTests` (2.4) | Accepted: no VoiceOver traversal |
| | Next page is loading at the end of the list | iOS append row inert (2.4) and hidden by construction (tests) | Limitation: Android My pools append was not observed on screen (it uses the same `PoolScoreItem(isPlaceholder = true)`) |
| | Placeholder keeps its production layout | iOS 2.1 and accessibility-large; Android 4.1 and 4.5 (183 px pitch, same divider extents); S8 | — |
| pool-score-list / Pool list presentation changes remain bounded | Sighted user sees no difference | Loaded My pools unchanged (iOS 2.1 captures, Android 4.1); open and invite work (4.1, 2.4 tests) | Limitation: Android loaded My pools was not pixel-compared with `HEAD` (the leaderboard was, 3.2) |
| | Pool with exactly one member | Not touched by this change | Limitation: not re-observed |
| | Surrounding screen is untouched | Only placeholder plumbing changed in `PoolHomeView.kt` (2 lines removed) and in the list files | — |
| | Android pool rows retain their appearance | 4.1, 4.5, S8 | — |
| navigation-drawer / Existing data and action states survive the redesign | Reopening with cached account information | Drawer account and avatar code not changed; `AccountAvatar` keeps its loaded path by default (tests in 1.5, 3.4) | Limitation: drawer reopen not re-observed in this change |
| | Pool summary is loading or fails | Loading: iOS `g2-sim18-drawer-summary-loading.png`, Android `g4-drawer-loading-light.png` (no accessibility node) | Limitation: a drawer summary failure was not observed on either platform. Android owner-only drawer entries are missing until the pool loads (pre-existing). Accepted: the iOS closed-drawer pulse |
| | Owner requests deletion | Not exercised (destructive, production backend) | Limitation: not re-observed; deletion code unchanged |
| ios-liquid-glass / Affected placeholders retain production presentation | Loading My pools after its invite styling changes | iOS 2.1 and 2.4 (initial and append, invite inert); Android 4.1 | — |
| android-edge-to-edge / All list states respect the same safe bounds | Loading completes | 4.5 insets (same divider extents and pitch on My pools, match, History, Timeline, and Manage gamblers); TalkBack dumps | — |
| | Empty or failed list | `g4-mypools-failure.png`, empty templates, pending "Nothing to show" (4.1, 4.4) | — |

Pre-existing problems found during this change and left unchanged (out of scope):

- lazy-paging-swift 0.0.3: append Retry is a no-op (group 2).
- iOS: about 60 refreshes per second after switching tabs while first loads are pending; this reproduces on `HEAD` (group 2).
- iOS library resolver: a refresh failure with loaded rows replaces them with the full-screen error.
- Android: History and Manage gamblers first-load errors have no Retry button (4.3, 4.4).
- Android: the drawer's owner-only entries are missing until the pool loads (4.1).
- Tests: `PoolNameKeyboardTest…[LANDSCAPE]` fails on `HEAD` too; `BetTextFieldTest` looks for a `textField` tag that `BetTextField` never sets; `PushDrawerAccessibilityTest` and `SignInKeyboardTest…[LANDSCAPE]` failed once each and passed on rerun (4.5).

Recorded deviations, kept as accepted limitations: the iOS drawer summary pulses while the closed drawer stays composed, until it loads; a failed automatic match reload replaces content with the full-screen error (both platforms); an Android template refresh failure replaces the templates; pending-bet placeholders lack the date header and status row the list adds around real bets; and every "Accepted without …" line above.

Not verified in group 5: no new device captures were made, because the existing recordings settle 5.1; the limitations marked in the table above remain open.

Accepted without device checks of width changes (rotation/wider windows), hardware-keyboard activation, Android contrast preferences, on-screen Android leaderboard pull-to-refresh/append/append failure, Android My pools on-screen append, the singular member count, and drawer reopen/summary failure/owner deletion for 5.2 by the user on 2026-10-02.

## Android and iOS — tasks 5.1–5.2 addendum (2026-10-02)

Following the user's decisions on the 5.1 and 5.2 questions, Android dark was recalibrated so its dim, static, and bright points match iOS dark in ΔL\* (CIELAB lightness) against each platform's own background: black on iOS, `#121212` on Android. Android light and every iOS value are unchanged. This supersedes the Android dark row of the 5.1 endpoint table above; that table is not rewritten.

Derivation: the iOS dark composites (white 10.67% / 13.33% / 16% over black → 27.2 / 34 / 40.8) give ΔL\* 9.9 / 13.2 / 16.5. The same ΔL\* over 18 needs 38.4 / 45.5 / 52.6, which is white at 8.6% / 11.6% / 14.6% over `#121212`. The `Pulse` highlight follows from 1 − (1 − 0.086)(1 − h) = 0.146.

New Android dark constants (`Android/ui/.../theme/LoadingPlaceholderPulse.kt`, `LoadingPlaceholderPulse.dark`): `dim` white 0.086 (was 0.107), `highlight` white 0.0656 (was 0.0594), `static` white 0.116 (was 0.133). Timing, shape, light values, and the API are unchanged; the KDoc now states the dark matching rule.

Device check (`medium_phone`, `emulator-5554`, API 36, signed in, production backend, read-only). The leaderboard paging query was delayed by 150 s in `GamblerScoreListViewModel.kt`. The file was backed up first, the backup was confirmed to exist, and the file was restored from it afterwards; `shasum -a 256 -c` reported OK and `git status` shows no change to the file. The staging prodDebug build was installed with `adb install -r`, dark mode was turned on, and "Copa Mundial de la FIFA 2026" was opened. Captures are in `verification/android/` with a `g5-` prefix. Exact values come from `screencap` PNGs of the first username mask (x 380–820, y 345–372) over the row background.

| Dark | iOS (black canvas) | Android before (`#121212`) | Android now (`#121212`) | Android target |
| --- | --- | --- | --- | --- |
| Dim | 27 → ΔL\* 9.9 | 43 → ΔL\* 12.3 | 38 (8.4%) → ΔL\* 9.7 (`g5-android-dark-recalibrated-dim.png`) | 38.4 → 9.9 |
| Static, animations off | 34 → ΔL\* 13.2 | 50 → ΔL\* 15.3 | 46 (11.8%) → ΔL\* 13.5 (`g5-android-dark-recalibrated-animoff.png`, 5 identical samples) | 45.5 → 13.2 |
| Bright | 41 → ΔL\* 16.5 | 56 → ΔL\* 18.0 | 53 (14.8%) → ΔL\* 16.7 (`g5-android-dark-recalibrated-bright.png`) | 52.6 → 16.5 |
| Amplitude (bright − dim) | ΔL\* 6.6 | ΔL\* 5.7 | ΔL\* 7.0 | ΔL\* 6.6 |

Dim and bright are the extremes of a 20-frame `screencap` burst taken while pulsing (range 38–53). Every phase now matches iOS within 0.3 ΔL\*. WCAG ratios still differ slightly (Android 1.24 / 1.38 / 1.53, iOS 1.22 / 1.32 / 1.44) because the backgrounds differ; lightness difference was the agreed measure.

Cycle (`g5-android-dark-recalibrated.mp4`, `g5-android-dark-recalibrated-trace.txt`, 20 fps): minima at 1.05, 2.80, 4.65, 6.40, and 8.20 s; maxima at 0.20, 1.95, 3.70, 5.55, 7.35, and 9.10 s. That is 1.78–1.79 s on average, with successive periods of 1.75–1.85 s (one frame is 0.05 s). In the video the mask runs 35 ↔ 51 over 16, the same 2–3 level encoding offset as earlier recordings. A first recording made while the host was overloaded (load average about 74 after the iOS build) captured only 2–3 frames per second and was discarded. No moving band and no blur.

Restore: dark mode off (`cmd uimode night no`); `animator_duration_scale` set to 0 for the static check and then deleted (reads `null`). The staging source was restored as above. A clean `:app:assembleProdDebug --rerun-tasks` build was installed with `adb install -r`; the device's `base.apk` SHA-256 equals the local APK (`0799807d…`). Fortuna launched signed in with three pools, and the leaderboard loaded within 6 s. The temporary `/sdcard` recordings were deleted. Fortuna was never uninstalled, cleared, or signed out, and nothing was written to production.

Tests: `./gradlew :ui:testDebugUnitTest` passed 44 of 44. No JVM or instrumented test hard-codes the dark constants; `LoadingPlaceholderPulseMotionTest` compares against `LoadingPlaceholderPulse.dark.static` symbolically. Instrumented tests were not rerun.

iOS: the file-private `PoolSummaryItem` in `iOS/Tyche/Tyche/UI/PoolHome/PoolHomeDrawerView.swift` now declares `var isPlaceholder: Bool = false`, so its memberwise initializer defaults to `false`. Both call sites still pass the flag explicitly. `xcodebuild build -workspace iOS/Tyche.xcworkspace -scheme Tyche` for the iOS 18.1 simulator (`A2F5EF6F-…`, Xcode 26.5) succeeded. `git status` shows no new `Package.resolved` change: the five modified `Package.resolved` files contain only the earlier `swiftui-shimmer` pin removals.
