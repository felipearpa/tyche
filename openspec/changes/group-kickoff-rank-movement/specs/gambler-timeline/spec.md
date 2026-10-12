## MODIFIED Requirements

### Requirement: Timeline follows the reference hierarchy

Timeline on iOS and Android SHALL present the grouped hierarchy in this change's corrected reference: the selected gambler's circular avatar and name, a prominent earned-points summary, then kickoff groups using History's shared scrolling date/time/count/trend header, vertical rail, and flat child rows separated by thin internal dividers. Identity, summary, and group headers SHALL scroll with the rows; headers SHALL NOT be sticky. Matches in the selected pool sharing the exact normalized scheduled kickoff instant SHALL share one logical group regardless of computation timing. Each child's visible date/time SHALL be provided by its group header rather than repeated within the row. Full match counts and partial computed counts SHALL come from the same authoritative pool-fixture group metadata used by History and standings, independently of visible child count. Existing visibility and eligibility rules SHALL restrict child rows and predictions, not redefine the group counts or computation phase. A group SHALL appear only when it has at least one eligible visible child; counts SHALL NOT reveal hidden predictions. A single-match count can be omitted consistently with History. Content SHALL use the established platform canvas and History's horizontal gutters and full available content width, with balanced team areas around the result at ordinary text sizes. The reference's sample identities, scores, points, colors, blank icon blocks, and broken prediction-line wrapping SHALL NOT replace real data, existing theme roles, navigation assets, or adaptive behavior.

#### Scenario: Opening a gambler's Timeline
- **GIVEN** another gambler is selected from an existing Timeline entry point
- **WHEN** Timeline loads
- **THEN** that gambler's avatar and name and their pool-points summary precede the match rows
- **AND** the screen retains its localized Timeline title and existing navigation destinations

#### Scenario: Scrolling matches from the same day
- **GIVEN** several rows share a match date
- **WHEN** the user scrolls Timeline
- **THEN** same-kickoff entries share one scrolling group header and different kickoff instants remain separate groups
- **AND** child rows do not repeat visual dates and no date heading is pinned
- **AND** identity and summary scroll away with the content

### Requirement: Timeline reuses History match and points presentation with correct ownership

Timeline SHALL reuse the production group-header, match-row, and points-summary presentation used by personal History on each platform, with focused context inputs for the selected gambler and settlement state. It SHALL NOT maintain a duplicate Timeline-only version of that presentation. Personal History SHALL retain its existing visible and spoken ownership wording and behavior. Timeline SHALL label the selected gambler's prediction with the localized equivalent of `Bet`, and its group-movement, prediction, missing-prediction, summary, and error announcements SHALL NOT describe another gambler's values as the signed-in user's. Date formatting SHALL follow History's localized weekday/month/day presentation, including a distinguishing year for matches outside the current calendar year and the existing device time-zone and clock-format behavior.

#### Scenario: Switching between personal History and Timeline
- **GIVEN** the signed-in user has a prediction and another gambler has a different prediction for the same match
- **WHEN** the user visits History and then that gambler's Timeline
- **THEN** History shows and announces the signed-in user's prediction with its existing ownership wording
- **AND** Timeline shows and announces the selected gambler's prediction using neutral Bet wording

#### Scenario: A selected gambler has no prediction
- **GIVEN** a Timeline row has no prediction
- **WHEN** the row is displayed or read aloud in any supported locale
- **THEN** it conveys that no bet was placed without addressing the signed-in user as the bettor

#### Scenario: Older match and clock preference
- **GIVEN** a match is outside the current calendar year and the device uses a 24-hour clock
- **WHEN** Timeline formats its group metadata and individual row accessibility context
- **THEN** the date includes the year and the time respects that clock preference

### Requirement: Unsettled entries preserve truthful status and existing feed behavior

Timeline SHALL retain existing server-provided eligibility and prediction visibility while replacing the per-match unsettled/settled split with a server-ordered split of whole kickoff groups. A displayed group with any member of its authoritative full group not yet computed SHALL be in the unsettled phase, including its already computed visible children; it SHALL enter the finished phase only when the full group is computed. Unsettled groups SHALL precede finished groups. Within the phases the client SHALL preserve the deterministic group and child ordering supplied by `backend-pagination`, without local filtering, sorting, or merging separate phase fragments. A group SHALL appear once per feed revision and SHALL NOT split into separate unsettled and finished sections. An unsettled entry SHALL use the shared row layout with its available match score, or neutral dashes when that score is absent, and its selected gambler's prediction in the footer. Its points area SHALL show the localized equivalent of `Points pending` in a neutral treatment instead of an earned numeric award. Assistive technology SHALL describe `Match score` or its unavailability and pending points. An unsettled/computation-pending state SHALL NOT be treated as evidence that a match has started, is currently live, or has a final result. A prediction SHALL NOT be substituted for an unavailable match score. This redesign SHALL NOT change prediction visibility rules, scoring, or feed eligibility, disclose ineligible predictions through group membership or counts, or introduce polling. Already computed children in an unsettled group SHALL retain their final-result and awarded-points presentation; group phase SHALL NOT force their points back to pending. Numeric group movement SHALL update progressively from the original group baseline using only computed contributions and SHALL include the authoritative computed/full count when partial.

#### Scenario: Match is included before kickoff
- **GIVEN** the existing feed includes an unsettled entry within the pre-kickoff lock window and no match score is available
- **WHEN** Timeline renders the entry
- **THEN** the central area shows neutral dashes and the footer retains the available prediction
- **AND** it shows Points pending without claiming the match is live or final

#### Scenario: Match score is available before points settle
- **GIVEN** an unsettled entry contains a match score of 1–0 and no settled award
- **WHEN** Timeline renders and announces the row
- **THEN** it shows 1–0 as the match score and Points pending
- **AND** it does not describe the score as final or display zero points as an award

#### Scenario: Pagination crosses into settled entries
- **GIVEN** the Timeline feed spans unsettled and finished group pages
- **WHEN** the next page loads
- **THEN** the app follows the grouped feed cursor and server order without client-side filtering or resorting
- **AND** a same-kickoff group is not duplicated across phases or split by its children's computation statuses
- **AND** the points summary is neither recomputed from rows nor requested again by the append

#### Scenario: Refresh returns a settled entry
- **GIVEN** an existing row shows pending points
- **WHEN** an ordinary refresh returns the entry as settled
- **THEN** the row adopts final-result and authoritative award presentation
- **AND** this transition requires no added polling or change to scoring rules

#### Scenario: Computed and uncomputed matches share a kickoff
- **GIVEN** one eligible visible group contains two computed matches and one uncomputed match
- **WHEN** Timeline renders the grouped feed
- **THEN** all three rows appear under one header in the unsettled phase with two of three computed and the progressive group movement
- **AND** the computed rows retain their final results and awards while only the uncomputed row shows Points pending

#### Scenario: The last group member computes
- **GIVEN** a mixed group is currently in the unsettled phase
- **WHEN** an ordinary refresh confirms every member of the authoritative full group is computed
- **THEN** the complete group moves into the finished phase as one unit in server order
- **AND** no child or group header remains duplicated in the unsettled phase

#### Scenario: Group has no computed members
- **GIVEN** a Timeline group contains eligible visible predictions but none of its members is computed
- **WHEN** the group is rendered
- **THEN** one unsettled group header retains the date/time and full match count and visibly and accessibly identifies pending movement without a directional arrow, numeric movement, or steady indicator
- **AND** every eligible visible child shows its available match score or neutral dashes, retains its prediction or missing-prediction presentation, and shows the neutral Points pending treatment
- **AND** a reported match score does not establish a final result or earned award, and the group does not borrow movement from an earlier group or become a loading placeholder

#### Scenario: The first group member computes
- **GIVEN** a two-match group shows pending movement and both visible children show Points pending
- **WHEN** an ordinary refresh returns a coherent publication with one computed member and available group ranks of eight before and six after that member
- **THEN** the same logical group header shows an upward movement of two places with one of two computed
- **AND** only the computed child switches to its final result and awarded points while the other retains its available score or dashes and Points pending
- **AND** the group remains in the unsettled phase without duplicated headers or children and without requiring polling

#### Scenario: A pending member is not eligible for display
- **GIVEN** a full group has two matches, one computed visible child, and one uncomputed child whose prediction is not yet visible
- **WHEN** Timeline renders the group
- **THEN** it shows only the permitted child, reports one of two computed, and keeps the group in the unsettled phase
- **AND** it uses the same partial movement and counts as History and standings without revealing the hidden prediction

### Requirement: Refresh and error recovery keep independent content usable

The identity, total, and list SHALL remain independently usable. One pull-to-refresh SHALL refresh rows and the total independently while retaining known content with refresh feedback. A total failure SHALL show a localized error with an inline, secondary, icon-only retry control that has a localized accessible name and requests only the total. A failed total refresh SHALL retain the last known total with an explicit update-failure indication; a successful unavailable-total response SHALL replace the prior number. Superseded requests SHALL NOT overwrite newer refresh state. Empty or failed rows SHALL retain any known identity and total; failed totals SHALL retain usable rows. An initial row-load failure SHALL show the shared list error with a localized Retry action that requests only the rows again, in addition to pull to refresh; append errors SHALL preserve their existing retry. Appending or retrying a page SHALL NOT reset or reload the summary. A list recovered through Retry or pull to refresh SHALL issue a bounded set of requests and SHALL NOT start repeated requests once content returns.

Timeline SHALL use the grouped feed directly and preserve one logical group header and authoritative metadata across transport page boundaries under `backend-pagination`. Appended children SHALL NOT cause movement or full counts to be recalculated from locally loaded rows. An explicit revision-invalidation response SHALL stop the invalid continuation and trigger a bounded recovery from the grouped feed's beginning, retaining usable prior content with update feedback until coherent replacement content or a retryable error is available. The client SHALL NOT combine group metadata, child rows, or phase placement from incompatible revisions; repeated invalidation SHALL expose retry recovery rather than loop. During historical recalculation, cached coherent group movement SHALL be explicitly identified as updating or made unavailable, never silently claimed to be current. Feed recovery SHALL not reload or reset the authoritative points summary unless the user has requested a whole-screen refresh.

#### Scenario: Summary request fails while rows succeed
- **GIVEN** Timeline has usable rows but its initial total request failed
- **WHEN** the user activates the summary retry
- **THEN** only the total is requested again
- **AND** the identity and rows remain visible and usable

#### Scenario: Rows are empty or fail
- **GIVEN** identity and the total are available
- **WHEN** the list returns empty or its request fails
- **THEN** identity and total remain above the existing empty or retry presentation
- **AND** an empty list does not imply a zero total
- **AND** the empty or error presentation, including its Retry, is centered in the viewport space below identity and the total, with its text centered

#### Scenario: Refresh partially fails
- **GIVEN** rows and a known total are already displayed
- **WHEN** refreshed rows succeed but the total update fails
- **THEN** the refreshed rows remain usable and the previous total remains visible
- **AND** localized text identifies the failed update and offers total-only retry

#### Scenario: Initial rows fail to load
- **GIVEN** identity and the total are available and the initial row request failed
- **WHEN** the user activates the list's Retry
- **THEN** only the rows are requested again
- **AND** identity and the total remain visible without the total being reloaded

#### Scenario: Recovering a failed list does not loop
- **GIVEN** the initial row request failed
- **WHEN** the user activates Retry or pulls to refresh and rows return
- **THEN** the rows are requested once and each returned row loads its avatar at most once
- **AND** no further requests start until the user scrolls, refreshes, or retries again

#### Scenario: Successful refresh reports an unavailable total
- **GIVEN** a numeric total is already displayed
- **WHEN** a refresh succeeds with no available total
- **THEN** the unavailable state replaces the previous number

#### Scenario: Refresh responses arrive out of order
- **GIVEN** a newer refresh supersedes an earlier request
- **WHEN** the earlier response arrives last
- **THEN** it cannot replace the newer state

#### Scenario: A group continues across transport pages
- **GIVEN** some children of a logical group are already loaded with authoritative full counts and movement
- **WHEN** its next children load through the server continuation
- **THEN** they extend the same section without another header or movement badge
- **AND** full counts and movement are not recomputed from loaded children

#### Scenario: Group completion changes the feed revision
- **GIVEN** a loaded mixed group becomes complete and changes phase in a newer published revision
- **WHEN** append receives explicit revision invalidation
- **THEN** the invalid continuation is replaced by a bounded grouped-feed restart
- **AND** the client never shows the old unsettled group together with a new finished duplicate
- **AND** identity, usable prior content with update feedback, and the independent total remain available during recovery

#### Scenario: Replay has not published replacement movement
- **GIVEN** a late result or correction changes the selected gambler's historical movement
- **WHEN** the server reports an affected group as updating
- **THEN** Timeline either identifies the last coherent group as updating or shows movement unavailable
- **AND** it does not invent a zero or fall back to the signed-in gambler's movement

### Requirement: Timeline loading uses production components

Initial and append-loading group headers and rows, the initial summary, and any model-backed identity placeholder SHALL populate their production components with stable placeholder models and select the shared `loading-placeholders` treatment through `isPlaceholder`. Callers SHALL NOT supply an effect modifier, and no separately maintained skeleton layout SHALL be introduced. Placeholder behavior SHALL suppress remote loading for filler identities, navigation, meaningful accessibility exposure, and activation through enclosing controls while retaining the production layout. Filler values SHALL NOT appear as real user content. Real identity data already supplied by navigation SHALL remain real content while rows or totals load. Refresh and append SHALL preserve existing real content. Motion preferences SHALL follow the shared loading-placeholder contract.

#### Scenario: Rows and total are initially loading
- **GIVEN** the selected gambler's identity is known and their rows and total are loading
- **WHEN** Timeline renders
- **THEN** the real identity remains visible and unavailable group headers, rows, and total use their production placeholder presentation
- **AND** filler values introduce no meaningful announcement or activation

#### Scenario: Appending with motion reduced
- **GIVEN** loaded rows and a total remain visible and system animations are reduced or disabled
- **WHEN** another page loads
- **THEN** only the append area shows the shared static placeholder treatment on the pending production group header or row
- **AND** existing rows, identity, and total retain their content and actions

### Requirement: Timeline accessibility and adaptation match established screens

Each loaded row SHALL expose one coherent screen-reader announcement in the order date/time, teams and match result or score, selected gambler's prediction, then awarded or pending points, with one accessible action to its existing match destination. Each group header SHALL have one coherent announcement identifying its date/time, authoritative full count, computed count when partial, and selected-gambler movement or its pending/unavailable/updating status. Group movement and count SHALL be announced once in that header rather than repeated in each child. Individual rows SHALL retain their own date/time context for direct focus without requiring the header to have been read. Identity SHALL be an accessible heading and the total a separate meaningful announcement. Decorative flags, avatar, group rails, separators, and pill backgrounds SHALL NOT add focus stops. Back, Home, and retry controls SHALL have localized accessible names, appropriate roles, and usable native hit targets. Screen-reader traversal SHALL follow the visual reading order. Numeric signs, text, and unavailable/pending labels SHALL distinguish states without relying solely on color. Spoken points, match counts, and movement place counts SHALL use full localized singular/plural words.

Timeline SHALL support existing English and Spanish locales, including the established Spanish (Spain) terminology where supported; light/dark appearance; large accessibility text; long names; narrow phones; and wider/resized windows. Names, score pairs, summary values, group metadata and partial-computation qualifiers, status text, and actions SHALL remain readable through wrapping, reflow, and vertical growth without fixed row heights, overlapping text, or shrinking accessibility text. Date, time, all new state labels, and announcements SHALL be localized. Existing system motion and contrast preferences SHALL continue to apply.

#### Scenario: Reading and activating a row
- **GIVEN** VoiceOver or TalkBack is enabled
- **WHEN** the user traverses Timeline and activates a loaded row
- **THEN** identity, total, each group header, and each row provide their meaningful announcements without decorative focus stops
- **AND** the group movement is announced once at the header while each row retains its own match date/time context
- **AND** the row opens the existing match destination once

#### Scenario: Navigating using assistive technology
- **GIVEN** Timeline contains loaded content or an error requiring retry
- **WHEN** the user focuses Back, Home, or retry
- **THEN** each control has an understandable localized name and action
- **AND** existing navigation destinations and recovery behavior remain available

#### Scenario: Large text and long names
- **GIVEN** a narrow or resized window, a long gambler/team name, and a large accessibility text size
- **WHEN** Timeline lays out its header and rows
- **THEN** all names, values, pending states, and actions remain readable without overlap
- **AND** score pairs remain intact while surrounding content reflows and scrolls

#### Scenario: Spanish and single-point awards
- **GIVEN** a supported Spanish locale and an award of one point
- **WHEN** the row and total are displayed and read aloud
- **THEN** new copy uses that locale's established terminology and singular point forms
- **AND** the selected gambler's values are not announced using second-person ownership

#### Scenario: Partial group remains accessible
- **GIVEN** a selected gambler's group has one computed match out of two and a positive movement
- **WHEN** its header is displayed at large text size or announced in a supported Spanish locale
- **THEN** the full count, one-of-two computation progress, and movement are readable and localized with correct grammatical number
- **AND** the movement is not presented as final or as belonging to the signed-in gambler

### Requirement: Timeline preserves navigation and safe content bounds

Timeline SHALL preserve its existing title, Back and Home destinations, entry points from Scores and match bettors, and match-row destination. It SHALL retain native iOS navigation/material behavior and Android's existing navigation shell and edge-to-edge contract. All loaded, loading, empty, and error content and recovery actions SHALL be fully reachable at scroll limits outside obstructing system bars or app chrome. Existing navigation icons SHALL be retained; repaired navigation glyphs in the corrected reference are illustrative and SHALL NOT become a shell redesign. Shared group trend assets SHALL follow the canonical-source and shared-indicator rules in `bet-history`; blank reference blocks SHALL NOT become app assets. Shared presentation changes SHALL preserve personal History, Scores, Bets, and match-screen behavior except for the grouped History, Timeline, and standings movement explicitly specified by this change.

#### Scenario: Opening Timeline from either existing entry point
- **GIVEN** Timeline is opened from Scores or a match's gambler list
- **WHEN** the user opens a row, goes back, or chooses Home
- **THEN** the existing routes receive the selected context and navigation destinations remain unchanged

#### Scenario: Reaching content in a constrained window
- **GIVEN** system bars and app chrome constrain a short or landscape window
- **WHEN** the user scrolls to either end in any loading or content state
- **THEN** the first and last content and recovery controls can be fully revealed and operated

## ADDED Requirements

### Requirement: Timeline movement belongs to the selected gambler and group

Timeline SHALL consume the required discriminated `groupMovement` object for each group for the selected gambler, selected pool, and exact scheduled-kickoff group under `kickoff-group-standings`. The displayed movement SHALL compare that gambler's start-of-group historical rank with the rank after the group's currently computed members and SHALL update progressively without resetting the baseline per match. A numeric zero SHALL mean confirmed no movement; pending, explicitly unavailable, and updating movement SHALL NOT be converted to zero. A known group with no computed members SHALL carry its explicit pending state; it SHALL not obtain movement from another group. Partial numeric movement SHALL carry a visible and spoken computed/full-count qualifier. History's shared trend badge SHALL render the numeric direction and magnitude. A lack of trustworthy historical evidence SHALL be represented by an explicit unavailable state. Missing or malformed `groupMovement` SHALL make the response invalid and recoverable through the list's localized retry behavior, retaining previously usable content rather than accepting it as successful unavailable movement. The client SHALL NOT infer movement from loaded points, sum locally visible per-match movement, use per-match snapshots as group baselines, or substitute the signed-in gambler's standing. Current standing and historical group boundaries SHALL remain separate because their comparison membership can differ. Context changes SHALL prevent previous-gambler, previous-pool, or previous-session group metadata and movement from reaching the new presentation.

#### Scenario: Selected gambler differs from the signed-in gambler
- **GIVEN** the selected gambler gained three places in a group while the signed-in gambler lost one place
- **WHEN** Timeline renders the group's movement
- **THEN** it displays and announces an upward movement of three places for the selected gambler
- **AND** it never uses the signed-in gambler's movement

#### Scenario: Another result arrives within the same kickoff
- **GIVEN** a partial group's published baseline rank is 8 and its rank after one computed member is 6
- **WHEN** an ordinary refresh returns a second computed member and a rank after the group of 5
- **THEN** the header shows an upward movement of three places from the same baseline
- **AND** the computation progress reflects the authoritative group counts rather than the number of loaded rows

#### Scenario: Historical movement is unavailable
- **GIVEN** a computed group lacks a trustworthy historical baseline and its required `groupMovement` explicitly reports unavailable movement
- **WHEN** Timeline renders the group
- **THEN** it shows movement unavailable and retains the selected gambler's usable rows and total
- **AND** it does not claim zero movement or use a per-match position difference

#### Scenario: Required group movement is missing
- **GIVEN** a grouped Timeline response omits `groupMovement` or contains an invalid movement discriminator
- **WHEN** the client receives the response
- **THEN** it treats the response as an invalid, retryable load while retaining the selected gambler's previously usable content
- **AND** it does not accept missing data as a successful unavailable state or try another endpoint mode

#### Scenario: Selected context changes during group loading
- **GIVEN** a group request is pending for one selected gambler and pool
- **WHEN** the selected gambler, pool, or authenticated session changes before it completes
- **THEN** only metadata and rows for the new presentation context can update the displayed groups

