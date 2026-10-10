# Design

## Context

See `proposal.md` for the motivation and `reference/gambler-timeline-reference.png` for the visual reference.
Both Timeline implementations already consume `PoolGamblerBetModel`, which also backs personal History.
Timeline currently has its own row, date grouping, and a gambler-name heading outside the list; History has the desired full-width match presentation and earned-points summary.

History's reusable components currently assume the signed-in gambler in visible and spoken copy.
Timeline can include unsettled entries, while History normally consumes computed results.
The existing Timeline feed, paging sources, match destination, and pool/gambler score query provide the required data without changing a backend contract.
The shared Account avatar already owns image loading and fallback behavior.

## Goals / Non-Goals

**Goals:**

- Give Timeline and personal History one maintained match-row layout and one maintained points-summary presentation on each platform.
- Make ownership and computation state explicit presentation inputs rather than inferring them from the screen title or incidental score values.
- Keep the selected gambler's identity, summary state, and row requests scoped to the same pool/gambler pair.
- Preserve accessible native interaction while allowing the reference hierarchy to reflow for small windows, large text, and translated content.

**Non-Goals:**

- A new shared cross-platform UI package, broad renaming or relocation of the `Finished` presentation files, or a new generic network-state framework.
- Changes to feed eligibility, server order, scoring, persistence, APIs, match navigation, or background polling.
- Deriving a live-match clock or lifecycle status from `isComputed` or `isLive`.
- Replacing existing navigation icons, copying the reference's device chrome, or adding a new fill-to-viewport placeholder policy.

## Decisions

### 1. Extend History presentation with an explicit viewing context

Add a small presentation context to `HistoryBetItem`, the points-summary/header entry points, and their text helpers.
It defaults to personal History so existing callers retain their current visible text, semantics, and behavior.
Timeline passes a selected-gambler context explicitly, including the display name where the summary announcement needs to identify its owner.

The context governs all ownership-sensitive copy: prediction labels, row announcements, missing-prediction announcements, summary announcements, and summary loading/error wording.
Timeline uses neutral `Bet` / `Apuesta` wording and neutral missing-prediction text; personal History keeps `Your bet` and its existing second-person variants.
Do not limit the change to the footer: the existing Spanish missing-prediction announcement also addresses the viewer directly.
Keep singular/plural point forms and full localized point names for speech.

**Rationale:** Explicit context makes shared defaults safe and prevents a selected gambler's result from being announced as the viewer's result.
**Alternatives:** Duplicating Timeline rows would reproduce layout and accessibility maintenance; replacing personal wording globally would regress History; caller-composed announcement strings would spread row semantics across screens.

### 2. Represent unsettled entries in the same production row

Use `HistoryBetItem` for every Timeline item, with a computation-state branch inside its existing score and points regions.
A computed item retains History's result, prediction, and awarded-points presentation.
For a noncomputed item, show the available `matchScore` in the central score region or the shared missing-score treatment when absent, keep its prediction or neutral missing-prediction state, and show a neutral `Points pending` / `Puntos pendientes` pill.
Its announcement describes the available score and pending points without calling the score final or claiming that play is currently underway.

`isComputed` distinguishes pending from computed points; neither it nor a field named `isLive` establishes a verified match lifecycle.
Do not display a noncomputed points value as an award, synthesize zero points, or infer status from a populated score.
Retain the existing feed's eligibility, ordering, and pagination, including the unsettled entries before completed entries.

**Rationale:** The same layout can express truthful pending state without dropping entries that Timeline already shows.
**Alternatives:** Filtering to completed bets changes the feed; keeping the older Timeline row creates two layouts; adding live status requires a stronger data contract and is outside this change.

### 3. Compose identity and summary inside the list

The list starts with an identity row using shared `AccountAvatar` and the routed gambler display name, followed by the shared History points header.
Use the selected gambler's ID with the existing avatar infrastructure; retain shared fallback rules rather than hard-coding the initials, avatar color, or image seen in the reference.
The avatar is decorative beside the spoken display name, avoiding duplicate identity announcements.

Keep identity and summary within the same scrolling content as the matches.
Replace sticky date groups with each shared row's date and time, and use History's full-width spacing, score hierarchy, points pills, and separators.
Allow the identity name, summary label, and footer to wrap when necessary; keep each prediction score expression together.
Retain native navigation chrome, existing Back/Home actions and icons, and safe-area/system-inset handling.

**Rationale:** Reusing production styling preserves theme and accessibility behavior while matching the reference's hierarchy.
**Alternatives:** A pinned identity reduces space at large text sizes; screenshot-specific dimensions or colors bypass the established adaptive and theme behavior.

### 4. Fetch the authoritative total independently of paging

Inject the existing `GetPoolGamblerScoreUseCase` on iOS and `GetPoolGamblerScore` on Android into Timeline's view model using the routed pool and gambler IDs.
Reuse `HistoryPoints` and `HistoryPointsSummaryState`: a confirmed zero is zero, a successful response without a total is unavailable, and a request failure is an error.
Never sum loaded rows or use the signed-in gambler's score as a substitute.

Start initial total and row loads independently so either region can succeed while the other is loading or failed.
Pull to refresh requests both sources and retains confirmed content while requests are pending; a summary-only retry requests only the total, and row retry/append retains its existing paging scope.
Preserve confirmed points on refresh failure and expose the shared recoverable status beside them.
Keep the summary visible and usable for an empty or failed list; a summary failure does not disable loaded matches.

Scope view-model identity and asynchronous results to the current pool/gambler pair and the authenticated session's lifetime; discard that presentation state when its session ends.
Cancel obsolete requests where supported and use request identity/generation checks so an older completion cannot overwrite newer state after refresh or navigation to another gambler.
Follow existing History refresh-indicator behavior, avoiding duplicate summary progress during pull to refresh.

**Rationale:** The server total covers the whole pool and remains correct with partial paging, missing bets, and independent request outcomes.
**Alternatives:** Summing pages is incomplete; a combined all-or-nothing request hides usable content; passing a snapshot total through navigation can become stale and cannot support an isolated retry.

### 5. Preserve native accessibility and production placeholders

Keep each available match as one coherent accessibility announcement and one native activation target using the existing match handler.
Do not replace native button/link click semantics when consolidating child announcements.
The announcement conveys date/time, teams, available result, prediction or its absence, and awarded or pending points with the selected context.
Suppress redundant focus on decorative flags and avatar imagery; identify the gambler and points summary clearly in reading order.
Give existing Back and Home controls localized accessible names without changing their assets or actions.

Use the production identity/summary/row components with placeholder models for any model-backed loading region, selecting the shared `loading-placeholders` treatment through `isPlaceholder`.
Placeholder values remain masked and excluded from accessibility, and placeholder rows suppress navigation and remote loading.
Keep real, known identity and stale confirmed content visible rather than replacing them with filler during refresh.
Do not introduce separate skeleton layouts or require callers to provide placeholder-effect modifiers.

**Rationale:** Component-owned semantics and placeholders retain the same geometry and accessibility behavior in every consumer.
**Alternatives:** Independently maintained skeletons drift from production layout; separate semantics built by each list allow ownership and activation to diverge.

### 6. Keep changes local to existing presentation and wiring

| Area | Main files and responsibility |
| --- | --- |
| iOS Timeline | `iOS/Bet/Sources/Bet/Timeline/BetTimelineListView.swift`, `BetTimelineList.swift`, and `BetTimelineListViewModel.swift`: scrolling header, flat shared rows, score dependency, request state and refresh. Retire or delegate the superseded `BetTimelineItem.swift` presentation. |
| iOS shared History | `iOS/Bet/Sources/Bet/Finished/HistoryBetItem.swift`, `HistoryPointsSummary.swift`, `HistoryPointsSummaryState.swift`, and related text helpers: context-aware presentation with personal defaults and pending row semantics. |
| iOS integration | `iOS/Bet/Package.swift`, `iOS/Bet/Sources/Bet/Localizable/Localizable.xcstrings`, and the existing Timeline dependency construction: Account UI dependency, localized copy, and reuse of the DataPool score repository. |
| Android Timeline | `Android/bet/src/main/java/com/felipearpa/tyche/bet/timeline/BetTimelineListView.kt`, `BetTimelineList.kt`, `BetTimelineListViewModel.kt`, and provider wiring: same presentation and state responsibilities. Retire or delegate the superseded `BetTimeLineItem.kt` presentation. |
| Android shared History | `Android/bet/src/main/java/com/felipearpa/tyche/bet/finished/HistoryBetItem.kt`, `HistoryPointsSummary.kt`, `HistoryPointsSummaryState.kt`, and related text helpers: same safe shared API and pending semantics. |
| Shared paging error | `iOS/UI/Sources/UI/Paging/LazyPagingVStackError.swift` and `Android/ui/src/main/java/com/felipearpa/tyche/ui/lazy/LazyPagingColumnError.kt`, with the defaults in `LazyPagingVStack`, `RefreshableLazyPagingVStack`, and `RefreshableLazyPagingColumn`: a localized Retry action wired to each list's paging retry. |
| Android integration | `Android/bet/src/main/java/com/felipearpa/tyche/bet/di/BetViewModelModule.kt`, `Android/bet/build.gradle.kts`, and the bet module's supported localized resources: score injection, Account UI dependency, and copy. |

Keep the existing routes and row model compatible; add presentation inputs rather than expanding backend models for this visual change.
Shared avatar implementation, data queries, and scoring infrastructure remain existing dependencies.

### 7. Give list load failures an explicit retry and keep the summary retry secondary

The shared full-list load-error component (`LazyPagingVStackError` on iOS, `lazyPagingColumnError` on Android) gains a localized Retry button beneath its error text.
The component owns the button, so every list that renders the shared error state offers it; its label and style match the existing append-error retry (`LazyPagingVStackConcatenateError` / `lazyPagingConcatenateError`).
It retries the list's own paging load and leaves pull to refresh available. On History and Timeline it reloads only the rows; the points total keeps its own retry.
Screens that supply custom error content keep their existing presentation.

The points-summary retry remains for both an initial total failure and a failed refresh that retains the last total, and still requests only the total.
It is rendered as a secondary, icon-only button inline beside the error text instead of a filled primary button beneath it, with a localized accessible name (`Retry` / `Reintentar`) and a native minimum hit target.
Its icon follows the repository icon rule: a Material Symbols refresh glyph, with iOS and Android assets derived from one committed canonical vector source.

**Rationale:** The rows are the screen's primary content, so their recovery deserves a visible primary action; pull to refresh is undiscoverable and is a path-based gesture without a single-tap alternative. The total is secondary to the rows, so its recovery should not compete with the list's.
**Alternatives:** Pull to refresh as the only recovery hides recovery and lacks a tap alternative; a primary summary button competes with the list's recovery and weighs down the total; per-screen retry buttons drift from the shared component.

### 8. Recovering a failed list issues a bounded reload

During task 3.3, recovering a failed match bettors list on iOS (the new Retry or pull to refresh) started an unbounded burst of about 100–130 requests per second to the production API and avatar bucket as soon as rows returned; continuous re-rendering preceded the bursts. The loop predates this change, but the new Retry makes it one tap away.

Recovering a failed list SHALL issue one list reload; on History and Timeline, pull to refresh also issues one total reload. Returning rows may each trigger at most their normal avatar load, and re-rendering alone SHALL NOT start a reload.
Fix the root cause where it lives (the screen, shared paging, or avatar loading) rather than throttling or debouncing requests, cover it with a regression test, and check Android for the same pattern with the same steps.

**Rationale:** A loop against production costs money, can degrade the service for every user, and drains devices; a throttle would hide the defect instead of removing it.
**Alternatives:** Hiding Retry on the affected screen leaves pull to refresh able to trigger the loop; rate-limiting requests masks the cause and still issues excess requests.

### 9. Full-list states fill the space below in-list headers

The shared full-list error and empty states (`lazyPagingColumnError` / `lazyPagingColumnEmpty` through `ViewportFillingItem` on Android; `LazyPagingVStackError` / `LazyPagingVStackEmpty` on iOS) fill only the viewport height remaining below any content above them in the list, such as Timeline's and History's identity and points header, so they are centered in the visible empty area. Lists without a header keep their current full-viewport centering.
Wrapped error and empty description text is center-aligned on both platforms. The content still scrolls and stays reachable at large text sizes.

**Rationale:** A state sized to the full viewport below a header extends past the bottom edge, so its center lands roughly one header-height too low.
**Alternatives:** Removing the minimum height leaves the state top-aligned under the header; per-screen offsets duplicate layout logic and drift.

## Risks / Trade-offs

- [Recovering a failed list loops against production] → Fix the root cause found in Decision 8, add a regression test, and run failure checks with a request watchdog that terminates the app within seconds.
- [Shared changes alter personal History] → Default to the personal context and explicitly verify existing wording, computed-row presentation, summary states, and match activation on both platforms.
- [Pending data is mistaken for a live or final match] → Base only points treatment on computation state and use neutral score/pending announcements without lifecycle claims.
- [A total belongs to the wrong gambler or older refresh] → Scope state to pool/gambler identity and reject obsolete request completions; exercise rapid navigation and overlapping responses.
- [Large text or localization clips scores and actions] → Reuse History's flexible layout and validate long names, supported locales, narrow widths, maximum accessibility text sizes, and system insets.
- [A failed request blocks unrelated content] → Keep summary and paging state independent and verify initial, refresh, retry, empty, and append combinations.
- [Avatar dependency increases coupling] → Depend on the existing Account component only; do not duplicate its loading or fallback logic inside Bet.

## Migration Plan

1. Extend the shared presentation APIs and localized resources with personal defaults, then adapt Timeline state and dependency wiring on each platform.
2. Switch Timeline to the shared scrolling composition and remove obsolete grouping/row layout paths after checking remaining callers.
3. Build both apps and validate personal History together with Timeline for computed, pending, missing-score/prediction, unavailable-total, and independent failure states.
4. Before authoring any executable mobile tests, explore and verify the actual Android interactions with ARTEMIS. Inspect attached devices and ask the user to choose when more than one is available; use an iOS simulator/device for the corresponding iOS verification.
5. After exploration, add only meaningful coverage with verified locators, native activation, and explicit waits; manually check VoiceOver/TalkBack, text scaling, themes, locales, refresh, pagination, and navigation. No tests or application code are authored by this proposal.

There is no data migration or release-order dependency. Rollback restores the previous Timeline composition and wiring while keeping backend data intact; shared personal defaults allow either consumer to be reverted independently.
