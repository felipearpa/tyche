# Spec Delta

## MODIFIED Requirements

### Requirement: History preserves navigation scope and platform surfaces

The redesign SHALL preserve the avatar/drawer and change-pool actions, tab count, labels, ordering, and existing navigation destinations. It SHALL keep native iOS navigation and tab materials under `ios-liquid-glass`, Android's current tab shell, and established platform light/dark content canvases. It SHALL NOT copy the reference's blank icon blocks as assets or change existing icons. History's match-row and points-summary presentation SHALL support explicit reuse by the `gambler-timeline` capability while preserving personal History's layout, data ownership, settled-result semantics, and visible and spoken `Your bet` wording. Another gambler's Timeline SHALL use its own selected-gambler context and neutral ownership wording rather than inherit personal History's wording or values. Android scrolling and all loading, loaded, empty, and error states SHALL preserve the `android-edge-to-edge` contract, with equivalent safe-area reachability on iOS.

#### Scenario: Switching tabs and inspecting another gambler
- **GIVEN** the redesigned History tab is available
- **WHEN** the gambler uses Scores, Bets, the drawer, change-pool, or another gambler's Timeline
- **THEN** Scores, Bets, the drawer, and change-pool retain their existing actions and presentation
- **AND** Timeline follows the `gambler-timeline` presentation while preserving its existing navigation destinations
- **AND** another gambler's prediction is not labeled as the signed-in gambler's bet

#### Scenario: Reaching the first and last content
- **GIVEN** a short window, system bars, or a bottom tab bar constrains the viewport
- **WHEN** History is scrolled to either end in a loaded, loading, empty, or error state
- **THEN** the first and last content and recovery actions can be fully revealed outside obstructing chrome
- **AND** the list retains its existing platform scrolling and inset behavior

#### Scenario: Shared presentation is used by Timeline
- **GIVEN** Timeline uses the shared match-row and summary presentation with selected-gambler and unsettled-state context
- **WHEN** the signed-in gambler returns to personal History
- **THEN** History continues to show the signed-in gambler's total and predictions with its existing ownership wording
- **AND** its settled rows, summary states, accessibility, refresh, paging, and match actions retain their existing behavior

### Requirement: Summary loading and failure are independent of match loading

The summary SHALL distinguish initial loading, a known numeric total, a successfully returned unavailable total, and request failure. Initial loading SHALL use the production summary populated with a placeholder model and its `isPlaceholder` parameter. A successful response with no total SHALL show a localized unavailable state instead of zero. Failure SHALL show a localized summary error with an inline, secondary, icon-only retry control, which has a localized accessible name and a native minimum hit target, without hiding usable match rows. Summary retry SHALL request only the summary. If the match list is empty or fails, a successfully loaded summary SHALL remain visible.

#### Scenario: The total request fails while matches load successfully
- **GIVEN** History has usable rows and no usable total
- **WHEN** the total request fails
- **THEN** a summary error with its inline icon retry replaces the summary placeholder
- **AND** the loaded rows remain visible and usable
- **AND** retrying the summary does not reset or reload those rows

#### Scenario: A successful response contains no total
- **GIVEN** the total request completes successfully with an unavailable score
- **WHEN** the summary is rendered
- **THEN** it presents a localized unavailable state without an earned numeric value
- **AND** it does not show zero, an endless placeholder, or a stale previous numeric total

#### Scenario: Rows fail or are empty while the total is available
- **GIVEN** the authoritative total has loaded
- **WHEN** the match list returns no rows or its request fails
- **THEN** the available total remains above the existing empty or error presentation
- **AND** an empty list does not cause the total to be replaced by an inferred zero
- **AND** the empty or error presentation, including its Retry, is centered in the viewport space below the total, with its text centered

### Requirement: History refresh and paging preserve available content

History SHALL preserve pull-to-refresh, page append, row activation to the existing match destination, and append error recovery. An initial match-list failure SHALL show the shared list error with a localized Retry action that requests only the match list again, in addition to pull to refresh. One pull-to-refresh gesture SHALL refresh both the match list and the authoritative points summary independently. Available rows and a previously known total SHALL remain visible during refresh. A failed total refresh SHALL retain the previous total with a localized refresh-failure indication and the summary's inline icon retry; it SHALL NOT silently present the old value as newly confirmed. A successful unavailable-total response SHALL replace the previous number with the unavailable state. Superseded requests SHALL NOT overwrite the result of a newer refresh. The total and list need not complete together, and one failure SHALL NOT block the other's successful result. A list recovered through Retry or pull to refresh SHALL issue a bounded set of requests and SHALL NOT start repeated requests once content returns.

#### Scenario: Refreshing existing content
- **GIVEN** History shows rows and an earned-points total
- **WHEN** the gambler pulls to refresh
- **THEN** both data sources refresh while available content remains visible with refresh feedback
- **AND** successful updated content is presented without waiting for the other request to succeed

#### Scenario: A total refresh fails
- **GIVEN** a previous total remains available during refresh
- **WHEN** the new total request fails
- **THEN** the previous total stays visible with an identifiable refresh failure and its inline icon retry
- **AND** any successfully refreshed rows remain available

#### Scenario: Refresh requests complete out of order
- **GIVEN** a newer total refresh supersedes an older one
- **WHEN** the older request finishes last
- **THEN** its response does not replace the newer refresh's state

#### Scenario: Loading another page or opening a match
- **GIVEN** History already displays usable rows
- **WHEN** another page loads or fails, or the gambler activates a loaded row
- **THEN** existing rows remain usable, append failure retains its retry action, and row activation reaches the existing match destination once
- **AND** appending and retrying a page do not reset the earned-points summary

#### Scenario: The initial match list fails
- **GIVEN** the authoritative total has loaded and the initial match-list request failed
- **WHEN** the gambler activates the list's Retry
- **THEN** only the match list is requested again
- **AND** the total remains visible and is not reloaded

#### Scenario: Recovering a failed list does not loop
- **GIVEN** the initial match-list request failed
- **WHEN** the user activates Retry or pulls to refresh and rows return
- **THEN** the match list is requested once and each returned row loads its avatar at most once
- **AND** no further requests start until the user scrolls, refreshes, or retries again
