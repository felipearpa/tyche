# Spec Delta

## Purpose

Let Fortuna users review another gambler's predictions, match results, and earned points in a selected pool through a Timeline that shares History's presentation and the app's accessibility practices on iOS and Android.

## ADDED Requirements

### Requirement: Timeline follows the reference hierarchy

Timeline on iOS and Android SHALL present the hierarchy in this change's supplied reference: the selected gambler's circular avatar and name, a prominent earned-points summary, then flat match rows separated by thin dividers. Identity and summary SHALL scroll with the rows. Each row SHALL contain its own date and time instead of a sticky date-group heading. Content SHALL use the established platform canvas and History's horizontal gutters and full available content width, with balanced team areas around the result at ordinary text sizes. The reference's sample identities, scores, points, colors, blank icon blocks, and broken prediction-line wrapping SHALL NOT replace real data, existing theme roles, navigation assets, or adaptive behavior.

#### Scenario: Opening a gambler's Timeline
- **GIVEN** another gambler is selected from an existing Timeline entry point
- **WHEN** Timeline loads
- **THEN** that gambler's avatar and name and their pool-points summary precede the match rows
- **AND** the screen retains its localized Timeline title and existing navigation destinations

#### Scenario: Scrolling matches from the same day
- **GIVEN** several rows share a match date
- **WHEN** the user scrolls Timeline
- **THEN** each row shows its own date and time without a pinned date heading
- **AND** identity and summary scroll away with the content

### Requirement: Timeline identifies the selected gambler through shared avatar behavior

Timeline SHALL show the selected gambler's name and use the existing shared account-avatar behavior, including uploaded photos, cached-image reuse, and the shared username-derived initial or blank-identity fallback. It SHALL NOT introduce independent avatar fetching or derive a new two-letter fallback from the reference. An absent or failed photo SHALL leave the gambler's name and remaining content usable. A long name SHALL wrap and remain available in full to assistive technology. The avatar SHALL be decorative within the identity presentation and SHALL NOT introduce a separate action or duplicate focus stop.

#### Scenario: Uploaded photo is available
- **GIVEN** the selected gambler has an available account photo
- **WHEN** their Timeline identity renders
- **THEN** it displays that photo through the shared avatar-loading behavior
- **AND** the full gambler name identifies the screen's subject

#### Scenario: Photo is missing or fails
- **GIVEN** the selected gambler's photo is unavailable or cannot be loaded
- **WHEN** the identity renders
- **THEN** it uses the existing shared initial or blank-identity fallback
- **AND** photo failure does not block points, rows, or navigation

### Requirement: Timeline reuses History match and points presentation with correct ownership

Timeline SHALL reuse the production match-row and points-summary presentation used by personal History on each platform, with focused context inputs for the selected gambler and settlement state. It SHALL NOT maintain a duplicate Timeline-only version of that presentation. Personal History SHALL retain its existing visible and spoken ownership wording and behavior. Timeline SHALL label the selected gambler's prediction with the localized equivalent of `Bet`, and its prediction, missing-prediction, summary, and error announcements SHALL NOT describe another gambler's values as the signed-in user's. Date formatting SHALL follow History's localized weekday/month/day presentation, including a distinguishing year for matches outside the current calendar year and the existing device time-zone and clock-format behavior.

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
- **WHEN** Timeline formats its row metadata
- **THEN** the date includes the year and the time respects that clock preference

### Requirement: Settled rows distinguish final result, prediction, and award

For this capability, a settled entry is one marked as having completed points computation by the existing data contract; an unsettled entry is one not marked computed. Match time or score availability SHALL NOT substitute for that computation status.

For a settled entry, Timeline SHALL show the home and away team identities around the largest text in the row, the final score. Its footer SHALL show the selected gambler's prediction and the authoritative points award. Positive awards SHALL use History's theme-aware green pill and signed value; a confirmed zero SHALL use a neutral zero value without a plus sign. Missing final results SHALL use neutral dashes, missing predictions SHALL use a localized no-bet indication, and missing awards SHALL use an unavailable indication. Missing values SHALL NOT become a zero score or zero-point award. The prediction score pair SHALL stay together as a readable unit when the footer reflows.

#### Scenario: Comparing a final result with a prediction
- **GIVEN** a settled match ended 1–2, the selected gambler predicted 2–1, and the awarded points are 2
- **WHEN** its row renders
- **THEN** the central result is 1–2, the footer shows Bet 2–1, and the pill shows the localized equivalent of +2 pts

#### Scenario: Zero and unavailable values are different
- **GIVEN** one settled entry has an explicit zero award and another has no available award or result
- **WHEN** both rows render and are announced
- **THEN** the first communicates zero points
- **AND** the second explicitly communicates unavailable points and result without inventing zeros

### Requirement: Unsettled entries preserve truthful status and existing feed behavior

Timeline SHALL retain the existing server-provided eligibility and ordering, including unsettled entries before settled entries and the existing page transition between them. An unsettled entry SHALL use the shared row layout with its available match score, or neutral dashes when that score is absent, and its selected gambler's prediction in the footer. Its points area SHALL show the localized equivalent of `Points pending` in a neutral treatment instead of an earned numeric award. Assistive technology SHALL describe `Match score` or its unavailability and pending points. An unsettled/computation-pending state SHALL NOT be treated as evidence that a match has started, is currently live, or has a final result. A prediction SHALL NOT be substituted for an unavailable match score. This redesign SHALL NOT change prediction visibility rules, scoring, feed filtering, or introduce polling.

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
- **GIVEN** the Timeline feed spans unsettled and settled pages
- **WHEN** the next page loads
- **THEN** the app follows the existing feed cursor and order without client-side filtering or resorting
- **AND** the points summary is neither recomputed from rows nor requested again by the append

#### Scenario: Refresh returns a settled entry
- **GIVEN** an existing row shows pending points
- **WHEN** an ordinary refresh returns the entry as settled
- **THEN** the row adopts final-result and authoritative award presentation
- **AND** this transition requires no added polling or change to scoring rules

### Requirement: Timeline total belongs to the selected gambler and pool

The summary SHALL show the authoritative total for the selected gambler in the selected pool, independently of how many rows have loaded. It SHALL load on first presentation and refresh, and SHALL NOT sum visible or loaded awards. A positive total SHALL use History's prominent signed green value and localized abbreviated point unit with the earned label. A confirmed zero SHALL use neutral zero without a plus sign. A successfully returned absent total SHALL show a localized unavailable state. Screen-reader output SHALL identify both the selected gambler and that the total represents points earned in this pool. Switching gambler, pool, or authenticated session SHALL prevent previous-context responses and values from being presented as belonging to the new context.

#### Scenario: Total exceeds the loaded page awards
- **GIVEN** the selected gambler's authoritative total is 120 and loaded entries account for 40 points
- **WHEN** Timeline loads or appends more entries
- **THEN** the summary displays the localized equivalent of +120 pts earned
- **AND** it does not use the signed-in user's total or the sum of loaded entries

#### Scenario: Total is zero or unavailable
- **GIVEN** the total request succeeds with either zero or no available score
- **WHEN** the summary renders
- **THEN** confirmed zero is displayed as zero points earned and an absent score as unavailable
- **AND** those states remain distinguishable from loading and failure

#### Scenario: Selected context changes during a request
- **GIVEN** a request is still pending for one gambler and pool
- **WHEN** another gambler, pool, or authenticated session becomes active
- **THEN** only responses for the current presentation context may update its summary and rows
- **AND** the previous total does not appear under the new gambler's identity

### Requirement: Refresh and error recovery keep independent content usable

The identity, total, and list SHALL remain independently usable. One pull-to-refresh SHALL refresh rows and the total independently while retaining known content with refresh feedback. A total failure SHALL show a localized error with an inline, secondary, icon-only retry control that has a localized accessible name and requests only the total. A failed total refresh SHALL retain the last known total with an explicit update-failure indication; a successful unavailable-total response SHALL replace the prior number. Superseded requests SHALL NOT overwrite newer refresh state. Empty or failed rows SHALL retain any known identity and total; failed totals SHALL retain usable rows. An initial row-load failure SHALL show the shared list error with a localized Retry action that requests only the rows again, in addition to pull to refresh; append errors SHALL preserve their existing retry. Appending or retrying a page SHALL NOT reset or reload the summary. A list recovered through Retry or pull to refresh SHALL issue a bounded set of requests and SHALL NOT start repeated requests once content returns.

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

### Requirement: Timeline loading uses production components

Initial and append-loading rows, the initial summary, and any model-backed identity placeholder SHALL populate their production components with stable placeholder models and select the shared `loading-placeholders` treatment through `isPlaceholder`. Callers SHALL NOT supply an effect modifier, and no separately maintained skeleton layout SHALL be introduced. Placeholder behavior SHALL suppress remote loading for filler identities, navigation, meaningful accessibility exposure, and activation through enclosing controls while retaining the production layout. Filler values SHALL NOT appear as real user content. Real identity data already supplied by navigation SHALL remain real content while rows or totals load. Refresh and append SHALL preserve existing real content. Motion preferences SHALL follow the shared loading-placeholder contract.

#### Scenario: Rows and total are initially loading
- **GIVEN** the selected gambler's identity is known and their rows and total are loading
- **WHEN** Timeline renders
- **THEN** the real identity remains visible and unavailable rows and total use their production placeholder presentation
- **AND** filler values introduce no meaningful announcement or activation

#### Scenario: Appending with motion reduced
- **GIVEN** loaded rows and a total remain visible and system animations are reduced or disabled
- **WHEN** another page loads
- **THEN** only the append area shows the shared static row-placeholder treatment
- **AND** existing rows, identity, and total retain their content and actions

### Requirement: Timeline accessibility and adaptation match established screens

Each loaded row SHALL expose one coherent screen-reader announcement in the order date/time, teams and match result or score, selected gambler's prediction, then awarded or pending points, with one accessible action to its existing match destination. Identity SHALL be an accessible heading and the total a separate meaningful announcement. Decorative flags, avatar, separators, and pill backgrounds SHALL NOT add focus stops. Back, Home, and retry controls SHALL have localized accessible names, appropriate roles, and usable native hit targets. Screen-reader traversal SHALL follow the visual reading order. Numeric signs, text, and unavailable/pending labels SHALL distinguish states without relying solely on color. Spoken points SHALL use full singular/plural words.

Timeline SHALL support existing English and Spanish locales, including the established Spanish (Spain) terminology where supported; light/dark appearance; large accessibility text; long names; narrow phones; and wider/resized windows. Names, score pairs, summary values, status text, and actions SHALL remain readable through wrapping, reflow, and vertical growth without fixed row heights, overlapping text, or shrinking accessibility text. Date, time, all new state labels, and announcements SHALL be localized. Existing system motion and contrast preferences SHALL continue to apply.

#### Scenario: Reading and activating a row
- **GIVEN** VoiceOver or TalkBack is enabled
- **WHEN** the user traverses Timeline and activates a loaded row
- **THEN** identity, total, and each row provide their meaningful announcements without decorative focus stops
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

### Requirement: Timeline preserves navigation and safe content bounds

Timeline SHALL preserve its existing title, Back and Home destinations, entry points from Scores and match bettors, and match-row destination. It SHALL retain native iOS navigation/material behavior and Android's existing navigation shell and edge-to-edge contract. All loaded, loading, empty, and error content and recovery actions SHALL be fully reachable at scroll limits outside obstructing system bars or app chrome. Existing navigation icons SHALL be retained; the reference's blank icon blocks SHALL NOT become app assets. Shared presentation changes SHALL preserve personal History, Scores, Bets, and match-screen behavior except for Timeline's specified presentation and accessibility improvements.

#### Scenario: Opening Timeline from either existing entry point
- **GIVEN** Timeline is opened from Scores or a match's gambler list
- **WHEN** the user opens a row, goes back, or chooses Home
- **THEN** the existing routes receive the selected context and navigation destinations remain unchanged

#### Scenario: Reaching content in a constrained window
- **GIVEN** system bars and app chrome constrain a short or landscape window
- **WHEN** the user scrolls to either end in any loading or content state
- **THEN** the first and last content and recovery controls can be fully revealed and operated
