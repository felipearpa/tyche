# bet-history Specification

## Purpose
Help Fortuna gamblers review completed matches, compare their predictions with final results, and understand points earned in the current pool through a consistent, accessible History screen on iOS and Android.

## Requirements

### Requirement: History follows the reference hierarchy

The signed-in gambler's History tab on iOS and Android SHALL present a localized History title in the same title style as the platform's other pool-home tabs, a prominent earned-points summary, and a list of kickoff groups using the hierarchy in the corrected reference saved with this change. A group SHALL contain the matches of the current pool that share the exact scheduled kickoff instant defined by `kickoff-group-standings`; grouping SHALL use the server's kickoff instant and SHALL NOT use result-processing order, a calendar date alone, or a formatted display time. Groups SHALL appear in descending kickoff order, replacing the former per-match computation-time order, and children SHALL keep the order supplied by the grouped feed. History SHALL display only computed child rows and only groups with at least one such row. Each group SHALL have a scrolling, non-sticky header with the localized date on its first line, the localized time and pluralized match count on its second line, and one group movement badge at the trailing edge. A single-match count can be omitted consistently on both platforms. The header's match count SHALL be the group's total count from the server even when only some matches are computed; a partial group SHALL additionally identify the computed count. A vertical rail SHALL visually connect the group's flat match rows, with thin internal dividers. Each child SHALL retain a central final-score area flanked by team identities and a prediction/points footer without repeating a visual date/time line. The points summary and group headers SHALL scroll with list content rather than remain fixed overlays.

#### Scenario: Viewing completed matches
- **GIVEN** History contains completed matches on different dates
- **WHEN** the gambler opens History
- **THEN** the title and total-earned summary precede the group with the newest kickoff
- **AND** each group shows its date, time, count, and movement before its match rows
- **AND** each child shows team identities, final result, prediction, and awarded points in that order

#### Scenario: Several matches share a date
- **GIVEN** multiple History matches occurred on the same date
- **WHEN** their rows are displayed or scrolled
- **THEN** matches with identical kickoff instants share one header, rail, and movement badge while different times on that date remain separate groups
- **AND** no date-group heading is pinned and visual dates are not repeated within child rows

#### Scenario: Only part of a kickoff group has computed
- **GIVEN** a kickoff group has four matches and two are computed
- **WHEN** History renders that group
- **THEN** it displays the two computed rows with their awards under one header identifying four matches and two computed
- **AND** it does not create uncomputed History rows or infer a two-match group from the visible children

#### Scenario: Older kickoff is computed after a newer kickoff
- **GIVEN** one group has a later kickoff but an earlier computation time than another group
- **WHEN** History renders the grouped feed
- **THEN** the later-kickoff group remains first regardless of processing order

### Requirement: The earned-points summary represents the current pool

The summary SHALL show the authoritative total earned by the signed-in gambler in the current pool, independently of the number of History pages loaded. A positive total SHALL appear as a prominent green signed value with a localized abbreviated points unit and a smaller earned label. A confirmed zero SHALL appear as neutral `0 pts` with the earned label and without a plus sign. The total SHALL be requested when the History presentation is first loaded and when History is refreshed, and SHALL NOT be calculated from loaded or visible rows. Page append SHALL neither recompute nor reload the summary. Changing pool or account SHALL prevent the previous context's total from being shown as the new context's total.

#### Scenario: More points exist than are represented on the first page
- **GIVEN** the authoritative pool total is 120 and the first loaded page's rows award 40 points
- **WHEN** History is displayed and further pages are loaded
- **THEN** the summary displays `+120 pts` with the localized earned label
- **AND** appending pages neither changes that total by summing rows nor requests it again

#### Scenario: The authoritative total is zero
- **GIVEN** the current pool total has successfully loaded as zero
- **WHEN** the summary is displayed
- **THEN** it shows neutral `0 pts` with the localized earned label
- **AND** it is distinguishable from loading, failure, and unavailable data

#### Scenario: Switching pools while a request is pending
- **GIVEN** History is loading a total for one pool
- **WHEN** the gambler changes to another pool before the request completes
- **THEN** only a total belonging to the new pool and current account is eligible for display

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

### Requirement: Match rows separate final results from predictions

Each match SHALL obtain its visible date/time context from its kickoff-group header instead of repeating that metadata within the row. The header SHALL use a localized weekday/month/day date and localized time, including a year when needed to distinguish a match outside the current calendar year. Existing device locale, time-zone, and clock-format behavior SHALL be preserved. The home team flag and name SHALL be on the left, the away team flag and name on the right, and the final home/away score SHALL be the largest text within the row, centered between them with a restrained separator. The footer SHALL show the localized equivalent of `Your bet` beside the home/away prediction and a trailing pill containing the awarded points. Positive awards SHALL use green text on a subdued green fill with a plus sign; confirmed zero awards SHALL use a neutral pill without a plus sign. Scores, predictions, awards, and team names SHALL come from real match data rather than the reference's sample values, and the redesign SHALL NOT change scoring rules.

#### Scenario: Comparing a prediction with a result
- **GIVEN** a match ended 1–2, the gambler predicted 2–1, and the server awarded 2 points
- **WHEN** its History row is displayed
- **THEN** the prominent result is 1–2 between the two team identities
- **AND** the footer labels 2–1 as the gambler's prediction and displays a green `+2 pts` pill

#### Scenario: A prediction earned no points
- **GIVEN** the match's award is explicitly zero
- **WHEN** its row is rendered
- **THEN** the footer displays a neutral `0 pts` pill without a plus sign

#### Scenario: Missing prediction, result, or award
- **GIVEN** a finished match has an unavailable prediction, final result, or points award
- **WHEN** the row is rendered
- **THEN** a missing prediction has a localized no-prediction indication, an unavailable result uses neutral dashes, and an unavailable award uses a neutral unavailable indication
- **AND** missing values are never presented as a 0–0 score or a zero-point award
- **AND** assistive technology describes each unavailable value explicitly

#### Scenario: An older match or a different clock preference
- **GIVEN** a match is outside the current calendar year or the device uses a 24-hour clock
- **WHEN** its date and time are displayed
- **THEN** the date includes the distinguishing year and the time respects the device clock preference

### Requirement: History refresh and paging preserve available content

History SHALL preserve pull-to-refresh, page append, row activation to the existing match destination, and append error recovery. An initial match-list failure SHALL show the shared list error with a localized Retry action that requests only the match list again, in addition to pull to refresh. One pull-to-refresh gesture SHALL refresh both the match list and the authoritative points summary independently. Available rows and a previously known total SHALL remain visible during refresh. A failed total refresh SHALL retain the previous total with a localized refresh-failure indication and the summary's inline icon retry; it SHALL NOT silently present the old value as newly confirmed. A successful unavailable-total response SHALL replace the previous number with the unavailable state. Superseded requests SHALL NOT overwrite the result of a newer refresh. The total and list need not complete together, and one failure SHALL NOT block the other's successful result. A list recovered through Retry or pull to refresh SHALL issue a bounded set of requests and SHALL NOT start repeated requests once content returns.

History SHALL use the grouped feed directly and follow the server's group order and the continuation contract in `backend-pagination`. Each page SHALL contain whole groups; the client SHALL render groups in server order with their server-supplied counts and movement and SHALL NOT merge groups, recount matches, or derive movement from loaded rows. An invalid-cursor response on append SHALL discard the continuation, keep usable content visible with refresh feedback, and reload from the first page with a bounded set of requests; repeated failure SHALL expose the existing localized retry rather than loop.

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
- **GIVEN** History already displays usable groups
- **WHEN** another page loads or fails, or the gambler activates a loaded row
- **THEN** existing groups remain usable, append failure retains its retry action, and row activation reaches the existing match destination once
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

#### Scenario: Appending another page of groups
- **GIVEN** History displays its first page of groups
- **WHEN** the next page loads
- **THEN** its groups are appended after the existing ones in server order
- **AND** no existing header, count, or movement changes because of the append

#### Scenario: The continuation is rejected
- **GIVEN** History holds a continuation the server no longer accepts
- **WHEN** append receives the invalid-cursor response
- **THEN** the client reloads the grouped feed from its first page while usable content stays visible with refresh feedback
- **AND** repeated rejection exposes the localized retry rather than an unbounded retry loop

### Requirement: History placeholders use production components

Initial and append-loading group-header and match placeholders and the initial summary placeholder SHALL populate their respective production components with stable placeholder models and select the shared `loading-placeholders` treatment through `isPlaceholder`. Callers SHALL NOT supply a separate effect modifier, and implementations SHALL NOT maintain separate skeleton-only layouts. Placeholder-specific behavior SHALL suppress remote loading for filler identities, navigation, and meaningful accessibility exposure without replacing the production layout. Filler values SHALL NOT appear as real user content. Existing rows SHALL remain real content during refresh or append, and motion preferences SHALL follow `loading-placeholders`.

#### Scenario: Initial loading at different text sizes
- **GIVEN** History's group headers, rows, and summary are still loading
- **WHEN** the device text size or available width changes
- **THEN** their placeholder models use the same production layout and adaptive rules as loaded components
- **AND** filler content is neither announced nor actionable

#### Scenario: Appending rows with content already available
- **GIVEN** History contains loaded groups and a known total
- **WHEN** a subsequent page is loading
- **THEN** only the append area displays the production group-header or row placeholders
- **AND** neither the existing rows nor the total become placeholders

### Requirement: History preserves navigation scope and platform surfaces

The redesign SHALL preserve the avatar/drawer and change-pool actions, tab count, labels, ordering, and existing navigation destinations. It SHALL keep native iOS navigation and tab materials under `ios-liquid-glass`, Android's current tab shell, and established platform light/dark content canvases. The corrected reference's navigation glyphs SHALL be treated as illustrative; this change SHALL NOT replace existing navigation assets or copy blank icon blocks as app assets. Group movement SHALL use the shared trend assets specified below. History's group-header, match-row, and points-summary presentation SHALL support explicit reuse by the `gambler-timeline` capability while preserving personal History's layout, data ownership, settled-result semantics, and visible and spoken `Your bet` wording. Another gambler's Timeline SHALL use its own selected-gambler context and neutral ownership wording rather than inherit personal History's wording or values. Android scrolling and all loading, loaded, empty, and error states SHALL preserve the `android-edge-to-edge` contract, with equivalent safe-area reachability on iOS.

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
- **GIVEN** Timeline uses the shared group-header, match-row, and summary presentation with selected-gambler and unsettled-state context
- **WHEN** the signed-in gambler returns to personal History
- **THEN** History continues to show the signed-in gambler's total and predictions with its existing ownership wording
- **AND** its computed-only rows, summary states, accessibility, refresh, paging, and individual match actions follow the History contract

### Requirement: History is localized and accessible

History SHALL support English and Spanish, light and dark appearance, large accessibility text, narrow phones, and wider or resized windows. Date, time, total match counts, computed counts, movement directions and place counts, movement pending and unavailable states, points, prediction labels, summary states, and retry actions SHALL be localized in each currently supported English and Spanish locale, including Spanish (Spain) where supported. Team names and scores SHALL remain distinguishable without overlap; layouts SHALL reflow and grow vertically when necessary rather than fixing row heights or shrinking accessible text. Each group header SHALL expose one coherent screen-reader announcement covering date/time, total match count, computed count when partial, and group movement or its pending/unavailable status once. Each loaded row SHALL retain one coherent screen-reader announcement covering its own date/time context, both teams, final result, the gambler's prediction, and awarded points, with its existing activation available. Direct focus on an individual match SHALL remain understandable without requiring the preceding header to have been read; group count and group movement SHALL NOT be repeated in every child announcement. The total SHALL have a separate meaningful announcement identifying it as the current pool's earned points. Decorative flags, rails, separators, and pill backgrounds SHALL NOT introduce extra stops. Spoken points, match counts, and places moved SHALL use full localized singular/plural words, and color SHALL NOT be the sole means of conveying awards, movement, or unavailable data.

#### Scenario: Reading History with assistive technology
- **GIVEN** VoiceOver or TalkBack is enabled
- **WHEN** the gambler navigates the summary, a group header, and a loaded match row
- **THEN** the total is identified as current-pool earned points, the header announces the date/time, total count, partial computed count when applicable, and movement once, and the row distinguishes its date/time context, final result, prediction, and award in one logical announcement
- **AND** its match action is available without separate focus stops for decorative elements

#### Scenario: Large text and long team names
- **GIVEN** large accessibility text, long team names, or a narrow/resized window
- **WHEN** History is laid out
- **THEN** labels, values, and retry actions remain readable and reachable through wrapping, reflow, and scrolling without overlap

#### Scenario: Spanish and singular points
- **GIVEN** Spanish is selected and a displayed or announced value is one point
- **WHEN** History is displayed and read aloud
- **THEN** all new copy and formatting follow Spanish conventions and the spoken point unit uses its singular form

#### Scenario: Light appearance and zero points
- **GIVEN** light appearance or a user who cannot distinguish green from neutral colors
- **WHEN** a positive award, zero award, or unavailable award is displayed
- **THEN** readable text and numeric or unavailable indicators distinguish those states without relying only on color

#### Scenario: Directly focusing a child match
- **GIVEN** a screen-reader user focuses a match without first focusing its group header
- **WHEN** the row is announced
- **THEN** its own date/time, teams, result, prediction, and award remain available together with its match action
- **AND** the group's movement and count are not redundantly repeated for that child

#### Scenario: A partial group at a large text size
- **GIVEN** a narrow window, large accessibility text, and a group with a long date and a partial computation qualifier
- **WHEN** the group header renders
- **THEN** its metadata and movement wrap or reflow without clipping the badge, counts, or child content
- **AND** the group remains understandable without color perception

### Requirement: History displays kickoff-group movement

Each History group SHALL display the `movement` supplied by the grouped feed for the signed-in gambler under `kickoff-group-standings`: `beforePosition` minus `position`, where both come from the group's own stored snapshots. Positive, negative, and zero movements SHALL render upward, downward, and steady indicators respectively. A partial group SHALL qualify its movement with the server's computed/total count. A null `movement` SHALL render as unavailable, distinct from a steady zero. Clients SHALL NOT sum loaded match trends or points, derive movement from the leaderboard position, or recompute it from loaded rows. Movement of a group SHALL change only when a refreshed feed returns a different value.

#### Scenario: Two computed matches contribute to one movement
- **GIVEN** a group's `movement` is `beforePosition` 8 and `position` 5
- **WHEN** History displays the group
- **THEN** the header shows one upward movement of three places
- **AND** the two rows retain their individual scores, predictions, and points without separate movement badges

#### Scenario: A further member computes
- **GIVEN** a four-match group showed two computed members and a movement of two places
- **WHEN** a refresh returns three computed members and a movement of four places
- **THEN** the header shows four places with three of four computed
- **AND** it does not show only the third match's incremental movement

#### Scenario: Movement is unavailable
- **GIVEN** a group's `movement` is null
- **WHEN** History renders that group
- **THEN** it identifies movement as unavailable while retaining usable match rows
- **AND** it does not display a steady zero

#### Scenario: A completed group has no net rank movement
- **GIVEN** a group's `beforePosition` equals its `position`
- **WHEN** History renders its header
- **THEN** it displays and announces steady movement

### Requirement: Group movement uses shared canonical trend assets

History and Timeline SHALL reuse the existing platform `TrendIndicator` for numeric group movement, including arrow-up, arrow-down, and steady-rule semantics, with a focused group-badge treatment rather than duplicate direction-rendering logic. Any app-rendered icons introduced or changed by this capability SHALL prefer Material Symbols when a semantically accurate representation exists; otherwise custom icons SHALL be allowed without a recorded justification. iOS and Android assets SHALL derive from the same committed canonical vector source for each corresponding icon. This rule excludes controls rendered entirely by the operating system. The corrected reference SHALL guide hierarchy and recognizable symbols without prescribing replacement navigation glyphs, shell styling, or fabricated sample content.

#### Scenario: The same movement is displayed on both platforms
- **GIVEN** the same group has a positive, negative, or zero movement
- **WHEN** iOS and Android render its badge
- **THEN** both use their existing shared trend presentation and derivatives of the same committed canonical vector source for the corresponding direction or steady rule
- **AND** no blank reference block becomes an app icon or navigation-shell redesign
