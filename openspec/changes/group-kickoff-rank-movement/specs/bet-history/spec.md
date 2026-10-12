## MODIFIED Requirements

### Requirement: History follows the reference hierarchy

The signed-in gambler's History tab on iOS and Android SHALL present a localized History title in the same title style as the platform's other pool-home tabs, a prominent earned-points summary, and a chronological kickoff-group list using the hierarchy in the corrected reference saved with this change. A group SHALL contain matches in the current pool sharing the exact normalized scheduled kickoff instant defined by `kickoff-group-standings`; grouping SHALL NOT use result-processing proximity, a calendar date alone, or formatted display time. Groups SHALL appear in descending scheduled-kickoff order, replacing the former per-match computation-time ordering, and children SHALL retain the deterministic order supplied by the grouped feed. History SHALL display only computed child rows and only groups with at least one such row. Each group SHALL have a scrolling, non-sticky header with the localized date on its first line, the localized time and pluralized match count on its second line, and one group movement badge at the trailing edge. A single-match count can be omitted consistently on both platforms. The header's match count SHALL be the authoritative full eligible group count even when only some matches are computed; partial groups SHALL additionally identify the computed count. A vertical rail SHALL visually connect the group's flat match rows, with thin internal dividers. Each child SHALL retain a central final-score area flanked by team identities and a prediction/points footer without repeating a visual date/time line. The points summary and group headers SHALL scroll with list content rather than remain fixed overlays.

#### Scenario: Viewing completed matches
- **GIVEN** History contains completed matches on different dates
- **WHEN** the gambler opens History
- **THEN** the title and total-earned summary precede the group with the newest scheduled kickoff
- **AND** each group shows its date, time, authoritative count, and movement before its match rows
- **AND** each child shows team identities, final result, prediction, and awarded points in that order

#### Scenario: Several matches share a date
- **GIVEN** multiple History matches occurred on the same date
- **WHEN** their rows are displayed or scrolled
- **THEN** matches with identical scheduled instants share one header, rail, and movement badge while different times on that date remain separate groups
- **AND** no date-group heading is pinned and visual dates are not repeated within child rows
- **AND** groups follow newest-kickoff-first order and each group retains its server-supplied child order

#### Scenario: Only part of a kickoff group has computed
- **GIVEN** a kickoff group has four eligible matches and two are computed
- **WHEN** History renders that group
- **THEN** it displays the two computed rows with their actual awards under one header identifying four matches and two computed
- **AND** it does not create uncomputed History rows or infer a two-match group from the visible children

#### Scenario: Older kickoff is computed after a newer kickoff
- **GIVEN** one group has a later scheduled kickoff but an earlier computation time than another group
- **WHEN** History renders the grouped feed
- **THEN** the later-kickoff group remains first regardless of processing order

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

History SHALL use the grouped feed directly and follow the server's group order and revision-aware continuation contract in `backend-pagination`. A logical group spanning transport pages SHALL retain one header, stable group identity, authoritative full counts, and one authoritative movement; appending children SHALL NOT produce duplicate sections or derive counts or movement from loaded pages. On an explicit revision-invalidation response the client SHALL discard the invalid continuation, retain usable previous content with refresh/update feedback, and recover from the beginning of the grouped feed with bounded requests. It SHALL NOT merge group metadata or child results from incompatible revisions. Repeated invalidation or restart failure SHALL expose the existing localized retry recovery instead of looping. These feed recoveries SHALL leave the independent earned-points summary contract intact. Where recalculation makes group movement stale, the app SHALL either show the last coherent group with an explicit updating indication or present movement as unavailable until a coherent revision is published; stale movement SHALL NOT be presented as confirmed current movement.

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

#### Scenario: A group continues at a transport page boundary
- **GIVEN** an already displayed group has further children reachable through the server's continuation
- **WHEN** those children load
- **THEN** they join the same logical section without repeating its header or movement badge
- **AND** the authoritative match count and movement do not change merely because more children became locally available

#### Scenario: Correction invalidates a continuation
- **GIVEN** History has loaded a revision that is superseded by a result correction
- **WHEN** append receives an explicit revision-invalidation response
- **THEN** the client stops using that continuation and requests a fresh grouped feed from its beginning
- **AND** usable previous content remains identifiable as updating until coherent replacement content or a retryable error is available
- **AND** incompatible revisions are never combined and repeated invalidation does not start an unbounded retry loop

#### Scenario: Historical movement is recalculating
- **GIVEN** a correction or schedule change affects an already visible group's movement
- **WHEN** the server reports that its replacement revision is not ready
- **THEN** the old coherent group is explicitly identified as updating or its movement is unavailable
- **AND** the group does not show stale movement as confirmed, substitute a steady zero, or recompute movement locally

### Requirement: History placeholders use production components

Initial and append-loading group-header and match placeholders and the initial summary placeholder SHALL populate their respective production components with stable placeholder models and select the shared `loading-placeholders` treatment through `isPlaceholder`. Callers SHALL NOT supply a separate effect modifier, and implementations SHALL NOT maintain separate skeleton-only layouts. Placeholder-specific behavior SHALL suppress remote loading for filler identities, navigation, and meaningful accessibility exposure without replacing the production layout. Filler values SHALL NOT appear as real user content. Existing rows SHALL remain real content during refresh or append, and motion preferences SHALL follow `loading-placeholders`.

#### Scenario: Initial loading at different text sizes
- **GIVEN** History's group headers, rows, and summary are still loading
- **WHEN** the device text size or available width changes
- **THEN** their placeholder models use the same production layout and adaptive rules as loaded components
- **AND** filler content is neither announced nor actionable

#### Scenario: Appending rows with content already available
- **GIVEN** History contains loaded rows and a known total
- **WHEN** a subsequent page is loading
- **THEN** only the append area displays the production group-header or row placeholders appropriate to the pending content
- **AND** neither the existing rows nor the total become placeholders

### Requirement: History preserves navigation scope and platform surfaces

The redesign SHALL preserve the avatar/drawer and change-pool actions, tab count, labels, ordering, and existing navigation destinations. It SHALL keep native iOS navigation and tab materials under `ios-liquid-glass`, Android's current tab shell, and established platform light/dark content canvases. The corrected reference's navigation glyphs SHALL be treated as illustrative; this change SHALL NOT replace existing navigation assets or copy blank icon blocks as app assets. New group movement SHALL use the shared trend assets specified below. History's group-header, match-row, and points-summary presentation SHALL support explicit reuse by the `gambler-timeline` capability while preserving personal History's layout, data ownership, settled-result semantics, and visible and spoken `Your bet` wording. Another gambler's Timeline SHALL use its own selected-gambler context and neutral ownership wording rather than inherit personal History's wording or values. Android scrolling and all loading, loaded, empty, and error states SHALL preserve the `android-edge-to-edge` contract, with equivalent safe-area reachability on iOS.

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

History SHALL support English and Spanish, light and dark appearance, large accessibility text, narrow phones, and wider or resized windows. Date, time, full match counts, computed counts, movement directions and place counts, movement pending/unavailable/updating states, points, prediction labels, summary states, and retry actions SHALL be localized in each currently supported English and Spanish locale, including Spanish (Spain) where supported. Team names and scores SHALL remain distinguishable without overlap; layouts SHALL reflow and grow vertically when necessary rather than fixing row heights or shrinking accessible text. Each group header SHALL expose one coherent screen-reader announcement covering date/time, full match count, computed count when partial, and group movement or its pending/unavailable/updating status once. Each loaded row SHALL retain one coherent screen-reader announcement covering its own date/time context, both teams, final result, the gambler's prediction, and awarded points, with its existing activation available. Direct focus on an individual match SHALL remain understandable without requiring the preceding header to have been read; group count and group movement SHALL NOT be repeated in every child announcement. The total SHALL have a separate meaningful announcement identifying it as the current pool's earned points. Decorative flags, rails, separators, and pill backgrounds SHALL NOT introduce extra stops. Spoken points, match counts, and places moved SHALL use full localized singular/plural words, and color SHALL NOT be the sole means of conveying awards, movement, or unavailable data.

#### Scenario: Reading History with assistive technology
- **GIVEN** VoiceOver or TalkBack is enabled
- **WHEN** the gambler navigates the summary, a group header, and a loaded match row
- **THEN** the total is identified as current-pool earned points, the header announces the date/time, full count, partial computed count when applicable, and movement once, and the row distinguishes its date/time context, final result, prediction, and award in one logical announcement
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

## ADDED Requirements

### Requirement: History displays authoritative kickoff-group movement

History SHALL use the required discriminated `groupMovement` object supplied for each group for the signed-in gambler and current pool under `kickoff-group-standings`. A numeric movement SHALL represent the rank before the exact scheduled-kickoff group minus the rank after its currently computed members, using the published chronological projection rather than the last processed match. The group's baseline SHALL remain the start-of-group baseline as further members compute. Positive, negative, and confirmed zero movements SHALL render upward, downward, and steady indicators respectively. Pending movement, explicitly unavailable historical movement, and updating movement SHALL remain distinguishable from a confirmed zero. A partially computed group's numeric movement SHALL be visibly and accessibly qualified by its authoritative computed/full count. A lack of trustworthy historical evidence SHALL be represented by an explicit unavailable state, not the absence of the required object. Missing or malformed `groupMovement` SHALL make the response invalid and recoverable through the list's localized retry behavior, retaining previously usable content rather than accepting the response as successful unavailable movement. Clients SHALL NOT sum loaded match trends or points, use per-match rank snapshots as a group baseline, or derive movement from the current leaderboard position. Current standing and historical group boundaries SHALL remain separate because their comparison membership can differ. Revisions caused by late results, corrections, or schedule changes SHALL replace affected historical group movement only through the coherent publication and paging contracts.

#### Scenario: Two computed matches contribute to one movement
- **GIVEN** a gambler's rank is 8 before a group and 5 after its two computed matches
- **WHEN** History displays the group
- **THEN** the header shows one upward movement of three places
- **AND** the two rows retain their individual scores, predictions, and points without separate movement badges

#### Scenario: A further member computes
- **GIVEN** a four-match group has two computed members and a movement of two places from its start-of-group rank
- **WHEN** an ordinary refresh returns three computed members with an authoritative group movement of four places
- **THEN** the header shows four places with three of four computed
- **AND** it does not show only the third match's incremental movement or wait for the fourth member to compute

#### Scenario: No trustworthy historical movement exists
- **GIVEN** a computed group has no reconstructable historical baseline and its required `groupMovement` explicitly reports unavailable movement
- **WHEN** History renders that group
- **THEN** it identifies movement as unavailable while retaining usable match rows
- **AND** it does not display a steady zero or substitute a per-match position difference

#### Scenario: Required group movement is missing
- **GIVEN** a grouped History response omits `groupMovement` or contains an invalid movement discriminator
- **WHEN** the client receives the response
- **THEN** it treats the response as an invalid, retryable load while retaining any previously usable content
- **AND** it does not accept missing data as a successful unavailable state or try another endpoint mode

#### Scenario: A completed group has no net rank movement
- **GIVEN** a published completed group has a numeric movement of zero
- **WHEN** History renders its header
- **THEN** it displays and announces steady movement
- **AND** that state remains distinct from pending, unavailable, and updating movement


### Requirement: Group movement uses shared canonical trend assets

History and Timeline SHALL reuse the existing platform `TrendIndicator` for numeric group movement, including arrow-up, arrow-down, and steady-rule semantics, with a focused group-badge treatment rather than duplicate direction-rendering logic. Any app-rendered icons introduced or changed by this capability SHALL prefer Material Symbols when a semantically accurate representation exists; otherwise custom icons SHALL be allowed without a recorded justification. iOS and Android assets SHALL derive from the same committed canonical vector source for each corresponding icon. This rule excludes controls rendered entirely by the operating system. The corrected reference SHALL guide hierarchy and recognizable symbols without prescribing replacement navigation glyphs, shell styling, or fabricated sample content.

#### Scenario: The same movement is displayed on both platforms
- **GIVEN** the same group has a positive, negative, or confirmed-zero movement
- **WHEN** iOS and Android render its badge
- **THEN** both use their existing shared trend presentation and derivatives of the same committed canonical vector source for the corresponding direction or steady rule
- **AND** no blank reference block becomes an app icon or navigation-shell redesign

