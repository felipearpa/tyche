# Proposal

## Why

Another gambler's Timeline still uses the older grouped match presentation and lacks the identity and earned-points hierarchy now available in personal History. Reusing History's presentation will bring Timeline closer to the supplied reference while carrying forward the app's accessibility, adaptive layout, and loading behavior.

## What Changes

- Redesign Timeline on iOS and Android around the [supplied reference](reference/gambler-timeline-reference.png): shared account avatar and gambler name, prominent total points earned in the selected pool, and flat match rows with dates, scores, predictions, points pills, and separators.
- Reuse the existing platform `HistoryBetItem` and History points-summary presentation with explicit context for the selected gambler. Timeline uses neutral `Bet` wording; personal History retains `Your bet` in visible and spoken content.
- Move identity and points summary into the scrolling content and replace sticky date groups with dates within each row.
- Preserve the existing feed, including unsettled entries before completed entries, paging, refresh, retry, and match navigation. Unsettled entries use the shared row with truthful score and pending-points semantics; computation status does not establish that a match is currently in progress.
- Load the selected gambler's authoritative pool score independently of paged bets, preserving known content during refresh and supporting isolated summary retry.
- Add a localized Retry action to the shared full-list load-error component on both platforms, so every list using it offers an explicit retry alongside pull to refresh, and render the points-summary retry as a secondary inline icon button beside its error text.
- Fix a pre-existing request loop in which recovering a failed list (Retry or pull to refresh), found on the match bettors screen, repeatedly re-requests rows and avatars once content returns.
- Apply the existing accessibility practices to the header, rows, loading states, and navigation controls, including large text, coherent announcements, localized control names, and production-component placeholders selected through `isPlaceholder`.
- Preserve existing navigation icons and native platform chrome, supported themes and locales, avatar loading infrastructure, backend contracts, and scoring rules.

## Capabilities

### New Capabilities

- `gambler-timeline`: Reference-aligned selected-gambler identity, authoritative pool points, shared match rows, unsettled-state semantics, refresh/paging, and accessible Timeline behavior on both platforms.

### Modified Capabilities

- `bet-history`: Permit explicit Timeline reuse of History presentation while retaining personal History's ownership wording, scope, and existing behavior.

## Impact

- iOS: `Bet/Timeline`, shared presentation currently in `Bet/Finished`, Timeline routing/dependency wiring, localized text, and the Bet package's dependency on shared Account avatar UI.
- Android: `bet/timeline`, shared presentation currently in `bet/finished`, view-model dependency wiring, localized resources, and the bet module's dependency on shared Account avatar UI.
- Reuse `PoolGamblerBetModel`, `HistoryBetItem`, `HistoryPointsSummary` / `HistoryPointsHeader`, `HistoryPointsSummaryState`, `AccountAvatar`, and the existing pool/gambler score query on each platform.
- Shared paging UI: the full-list load-error components `iOS/UI/Sources/UI/Paging/LazyPagingVStackError.swift` and `Android/ui/src/main/java/com/felipearpa/tyche/ui/lazy/LazyPagingColumnError.kt` (with the defaults in `LazyPagingVStack`, `RefreshableLazyPagingVStack`, and `RefreshableLazyPagingColumn`), plus a canonical retry icon asset shared by both apps. Other screens rendering the shared error (such as the match bettors list, the pool-from-layout picker, and the leaderboard) gain the Retry action; screens supplying their own error content keep it.
- Request-loop fix: the match bettors screen (`MatchBetList` on both platforms) and, depending on the root cause, the shared paging or avatar loading it uses.
- No API, persistence, scoring, eligibility, ordering, or background polling changes. Personal History, Scores, Bets, and the existing match destination remain supported consumers of shared infrastructure.
