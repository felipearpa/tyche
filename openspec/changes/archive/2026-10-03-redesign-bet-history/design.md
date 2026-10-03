# Design

## Context

See [proposal.md](proposal.md) for motivation and [specs/bet-history/spec.md](specs/bet-history/spec.md) for the behavior contract. The user confirmed both mobile platforms and an authoritative total for the current pool. The [reference image](reference/history-reference.png) is saved with this change for implementation and visual review.

Both apps currently render History through `FinishedBetListView` / `FinishedBetList`, grouped beneath pinned date headers. `FinishedBetItem` is shared with another gambler's timeline. History view models expose paged bets, currently 50 per page, but no aggregate score. Existing data use cases already load `PoolGamblerScore.score` by pool and gambler; the drawer uses these only during its initial load. Results, predictions, awards, and aggregate scores can be unavailable independently.

The iOS shell already supplies History's large navigation title and native tab bar. Android supplies a compact top app bar and a full-width tab row. Shared list components provide loading, empty/error bodies, append recovery, refresh, and inset behavior. Existing specifications govern production-component placeholders, iOS materials, Android edge-to-edge behavior, and navigation.

## Goals / Non-Goals

**Goals:**

- Keep the change local to History presentation and state while reusing the existing data APIs.
- Make total loading independent from paging and preserve usable data during partial failures.
- Match the reference's relative sizing, alignment, spacing, and semantic emphasis with accessible platform typography.

**Non-Goals:**

- Changing scoring rules, persisted data, backend contracts, other gamblers' timelines, or the Scores/Bets layouts.
- Replacing navigation chrome with a screenshot replica, introducing new icons, or recoloring the app-wide canvas.
- Adding filters, charts, time-period totals, automatic polling, or a global pool-summary state refactor.

## Decisions

### 1. Create a History-specific production row

Introduce `HistoryBetItem` beside the existing finished-row component on each platform and use it for both real History rows and History row placeholders. Keep `FinishedBetItem` as the timeline presentation. Reuse the current bet model, country flag assets, value formatting helpers where appropriate, and shared theme and placeholder primitives.

The row has three vertical regions:

```text
Sunday, Jul 19                         2:00 PM

   [flag]            0 – 0             [flag]
   España                            Argentina

Your bet  3 – 2                        [0 pts]
─────────────────────────────────────────────
```

Use balanced team columns around an intrinsic-width score area, consistent horizontal gutters, generous vertical spacing, and a content-width divider. The final result is visibly larger than team names and prediction text. The date/time line is secondary. At narrow widths or large text sizes, allow names to wrap and the metadata/footer to reflow vertically; do not lock heights or shrink accessibility text. On wider windows, rows span the full available content width like the Scores and Bets lists, keeping balanced team columns around the centered score; no fixed maximum column width is applied.

Remove sticky date groups only from History and retain existing row identity, order, and match callback. A loaded row remains one activation target; placeholders never activate it.

**Alternative:** Changing `FinishedBetItem` globally is fewer lines initially but would also redesign another gambler's timeline and make `Your bet` incorrect there. A context flag would retain that coupling. A separate History production component keeps ownership and future changes explicit without maintaining a separate loading layout.

### 2. Keep authoritative summary state in the existing History view model

Inject the existing pool-score use case into `FinishedBetListViewModel` on each platform. Request the score for that view model's pool/account identity on initial History load and on refresh. Keep summary state independent of pager state, with a usable-value snapshot and request/error status sufficient to represent refresh without erasing existing content.

| State | Presentation |
|---|---|
| Initial request without a value | Production summary with placeholder model and `isPlaceholder = true` |
| Successful positive value | Large green `+N pts` and secondary earned label |
| Successful zero | Neutral `0 pts` and secondary earned label |
| Successful unavailable value | Neutral localized unavailable presentation; clear a prior numeric value |
| Initial failure | Compact localized error and summary-only retry |
| Refresh with a previous value | Retain the value and expose refresh progress |
| Refresh failure with a previous value | Retain value with explicit refresh-failure feedback and summary-only retry |

The existing `ViewingState` / `LoadState` conventions can be reused, with a retained value where their basic loading enum would otherwise discard content. Tie requests to the view model's identity and cancel or reject superseded results. iOS published updates stay on the main actor; Android state follows the existing coroutine/view-model lifecycle. Append loading and page retry do not request or calculate the total. No list-empty inference overrides the server total.

On iOS, add `DataPool` as a package and target dependency in `iOS/Bet/Package.swift`, inject `GetPoolGamblerScoreUseCase`, and resolve it at the existing History composition in `iOS/Tyche/Tyche/UI/PoolHome/PoolHomeView.swift`. Its production registration already exists in `DataPool`'s `PoolAssembly`; update preview construction too.

On Android, add `:data:pool` to `Android/bet/build.gradle.kts`, inject `GetPoolGamblerScore`, and update `BetViewModelModule`. `PoolProvider` already registers the use case. Neither platform needs a dependency on the Pool presentation module, a new repository, or a backend change.

**Alternatives:** Summing pages produces incomplete totals. Sharing the drawer's initialization-only state adds routing/account-menu coupling and still needs a refresh redesign. A feature-owned request reuses the API while keeping this change bounded, at the cost of a separate aggregate request when History first opens.

### 3. Compose the summary within every list state and coordinate refresh

Place a production `HistoryPointsSummary` at the beginning of History's scrolling content. Use one header builder across loaded, initial-loading, empty, and initial-error bodies so state changes do not remove a known total or duplicate headers. Add only the minimal optional header/refresh hooks needed by the shared paging containers; defaults must preserve every other caller's behavior.

One pull-to-refresh event starts both the existing pager refresh and the summary reload. Requests complete independently, and visible refresh feedback covers outstanding work, including a summary request that outlasts the pager. Do not hold successful rows behind a failed summary or vice versa. Summary retry requests only the score; initial or append list retry retains its existing scope. Refresh preserves available rows and score until their replacement is known.

On Android, the existing `RefreshableLazyPagingColumn` refresh path can gain an optional companion refresh callback and, where needed, an external refreshing flag. On iOS, extend the existing refresh operation to await the independent summary load along with its pager work rather than attach a second competing refresh gesture. Confirm the actual container hooks during implementation and retain defaults for non-History lists.

**Alternative:** A summary outside the list would remain fixed and could crowd content at large text sizes. A second History-only paging implementation would duplicate retry/loading/inset behavior.

### 4. Preserve native shell ownership of titles and navigation

Keep iOS's existing large navigation title; the list starts with earned points and uses the platform's native title-collapse behavior. On Android, keep the existing compact app bar for History, the same as Scores and Bets, so all pool-home tabs share one title style; the reference's large title is not reproduced on Android. Keep the shell's current avatar and change-pool callbacks and inset policy. Render the History title once, not in both the bar and the list. The summary scrolls away with content. On Android, the pool-home tabs share one top app bar, so its scroll state is reset whenever the selected tab changes; a newly shown tab starts with the untinted bar instead of inheriting the previous tab's scrolled tint. Pull-to-refresh consumes overscroll at the top of lists, so Material's own reset at the top cannot be relied on.

Keep existing toolbar/tab icons and actions. The white blocks in the reference do not define replacement glyphs. Preserve native iOS materials, supported pre-iOS-26 behavior, Android's current tab shell, system-background black on dark iOS, and the established dark Android canvas. Positive points use theme-aware green foreground and subdued green fills, with contrast checked in both appearances; do not reuse a primary container whose fill is much stronger than the reference's pill.

**Alternative:** Copying the iOS tab capsule onto Android or painting a custom iOS glass bar would expand this into a navigation redesign and conflict with established platform contracts.

### 5. Centralize formatting and preserve the meaning of absent data

Use localized date/time formatting: weekday and abbreviated month/day in locale-appropriate order, a year when the match is outside the current year, and existing time-zone and 12/24-hour preferences. Preserve server team names and current home/away ordering.

Use localized visible strings for the reference's `Your bet`, `earned`, and points units, and semantic missing/error descriptions. Keep nil/null result, prediction, award, and total distinct from numeric zero. An absent prediction receives a no-prediction indication; missing score/award fields use neutral unavailable presentation. Never display a bare plus sign. Screen-reader values expand abbreviated units and use correct singular/plural forms.

Each real row combines date/time, team names, final score, prediction, and award into one meaningful announcement while preserving activation. The summary announces current-pool earned points separately; actionable retries remain discoverable. Decorative flags and visual containers add no stops. `isPlaceholder` suppresses filler announcements and navigation at both component and enclosing row levels.

**Alternative:** Copying the screenshot's literal English dates or treating missing values as zero would fail localization or misrepresent data.

## Risks / Trade-offs

- Separate list and total endpoints can complete at different times → render independently, retain available content, and identify failed refreshes. This change does not promise an atomic cross-endpoint snapshot.
- Summary requests may overlap or finish after a pool switch → scope state to pool/account and reject superseded responses.
- Shared paging hook changes could affect unrelated lists → use default-preserving optional hooks and verify existing callers along with History.
- Larger central scores can crowd names at accessibility sizes → wrap/reflow, permit vertical growth, and verify long names at narrow widths on both platforms.
- Full-width rows place the two teams far apart on iPad and wide windows → keep balanced team columns around a centered score so pairing remains clear; accepted in exchange for consistency with the other lists.
- Visual parity can be mistaken for identical platform chrome → use the saved reference for History content while retaining the explicit native-shell boundaries above.

## Migration Plan

Implement the mobile groups independently against the existing backend. No data migration, endpoint rollout, or feature flag is required. Validate both platforms against the saved reference and the spec scenarios before release. Rollback consists of reverting the History UI/state and its optional paging hooks; existing persisted data remains compatible.

Before authoring new test code, follow the repository's ARTEMIS exploration requirements: diagnose connected Android devices, obtain user selection if ambiguous, and verify the target interactions through ARTEMIS. Also verify iOS-specific layout and navigation on the simulator. Record device/simulator observations before encoding assertions and waits. Use focused tests for total-state transitions, refresh isolation, stale-response protection, and context changes; verify visual layout, accessibility, and inset behavior on devices rather than writing implementation-mirroring layout tests.
