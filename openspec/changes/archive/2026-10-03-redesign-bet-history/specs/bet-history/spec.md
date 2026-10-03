# Spec Delta

## Purpose

Help Fortuna gamblers review completed matches, compare their predictions with final results, and understand points earned in the current pool through a consistent, accessible History screen on iOS and Android.

## ADDED Requirements

### Requirement: History follows the reference hierarchy

The signed-in gambler's History tab on iOS and Android SHALL present a localized History title in the same title style as the platform's other pool-home tabs, a prominent earned-points summary, and a chronological match list using the hierarchy in the change's reference image. Each match SHALL occupy a flat row with a thin separator, a date/time line, a central final-score area flanked by team identities, and a prediction/points footer. History SHALL preserve its existing newest-first order and SHALL repeat the date within each row instead of showing sticky date-group headings. The points summary SHALL scroll with list content rather than remain as a fixed overlay.

#### Scenario: Viewing completed matches
- **GIVEN** History contains completed matches on different dates
- **WHEN** the gambler opens History
- **THEN** the title and total-earned summary precede the newest match
- **AND** each row shows its own date and time, team identities, final result, prediction, and awarded points in that order

#### Scenario: Several matches share a date
- **GIVEN** multiple History matches occurred on the same date
- **WHEN** their rows are displayed or scrolled
- **THEN** each row includes the date and time and there is no pinned date-group heading
- **AND** their existing ordering is retained

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

The summary SHALL distinguish initial loading, a known numeric total, a successfully returned unavailable total, and request failure. Initial loading SHALL use the production summary populated with a placeholder model and its `isPlaceholder` parameter. A successful response with no total SHALL show a localized unavailable state instead of zero. Failure SHALL show a localized summary error and a retry action without hiding usable match rows. Summary retry SHALL request only the summary. If the match list is empty or fails, a successfully loaded summary SHALL remain visible.

#### Scenario: The total request fails while matches load successfully
- **GIVEN** History has usable rows and no usable total
- **WHEN** the total request fails
- **THEN** a summary error and retry action replace the summary placeholder
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

### Requirement: Match rows separate final results from predictions

Each row SHALL place a localized weekday/month/day date at the leading edge and the match's localized time at the trailing edge. Years SHALL be included when needed to distinguish a match outside the current calendar year. Existing device locale, time-zone, and clock-format behavior SHALL be preserved. The home team flag and name SHALL be on the left, the away team flag and name on the right, and the final home/away score SHALL be the largest text within the row, centered between them with a restrained separator. The footer SHALL show the localized equivalent of `Your bet` beside the home/away prediction and a trailing pill containing the awarded points. Positive awards SHALL use green text on a subdued green fill with a plus sign; confirmed zero awards SHALL use a neutral pill without a plus sign. Scores, predictions, awards, and team names SHALL come from real match data rather than the reference's sample values, and the redesign SHALL NOT change scoring rules.

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

History SHALL preserve pull-to-refresh, page append, row activation to the existing match destination, and initial/append error recovery. One pull-to-refresh gesture SHALL refresh both the match list and the authoritative points summary independently. Available rows and a previously known total SHALL remain visible during refresh. A failed total refresh SHALL retain the previous total with a localized refresh-failure indication and a retry action; it SHALL NOT silently present the old value as newly confirmed. A successful unavailable-total response SHALL replace the previous number with the unavailable state. Superseded requests SHALL NOT overwrite the result of a newer refresh. The total and list need not complete together, and one failure SHALL NOT block the other's successful result.

#### Scenario: Refreshing existing content
- **GIVEN** History shows rows and an earned-points total
- **WHEN** the gambler pulls to refresh
- **THEN** both data sources refresh while available content remains visible with refresh feedback
- **AND** successful updated content is presented without waiting for the other request to succeed

#### Scenario: A total refresh fails
- **GIVEN** a previous total remains available during refresh
- **WHEN** the new total request fails
- **THEN** the previous total stays visible with an identifiable refresh failure and retry action
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

### Requirement: History placeholders use production components

Initial and append-loading match placeholders and the initial summary placeholder SHALL populate their respective production components with stable placeholder models and select the shared `loading-placeholders` treatment through `isPlaceholder`. Callers SHALL NOT supply a separate effect modifier, and implementations SHALL NOT maintain separate skeleton-only layouts. Placeholder-specific behavior SHALL suppress remote loading for filler identities, navigation, and meaningful accessibility exposure without replacing the production layout. Filler values SHALL NOT appear as real user content. Existing rows SHALL remain real content during refresh or append, and motion preferences SHALL follow `loading-placeholders`.

#### Scenario: Initial loading at different text sizes
- **GIVEN** History's rows and summary are still loading
- **WHEN** the device text size or available width changes
- **THEN** their placeholder models use the same production layout and adaptive rules as loaded components
- **AND** filler content is neither announced nor actionable

#### Scenario: Appending rows with content already available
- **GIVEN** History contains loaded rows and a known total
- **WHEN** a subsequent page is loading
- **THEN** only the append area displays row placeholders
- **AND** neither the existing rows nor the total become placeholders

### Requirement: History preserves navigation scope and platform surfaces

The redesign SHALL preserve the avatar/drawer and change-pool actions, tab count, labels, ordering, and existing navigation destinations. It SHALL keep native iOS navigation and tab materials under `ios-liquid-glass`, Android's current tab shell, and established platform light/dark content canvases. It SHALL NOT copy the reference's blank icon blocks as assets or change existing icons. The new History layout and the ownership-specific `Your bet` wording SHALL NOT alter another gambler's timeline. Android scrolling and all loading, loaded, empty, and error states SHALL preserve the `android-edge-to-edge` contract, with equivalent safe-area reachability on iOS.

#### Scenario: Switching tabs and inspecting another gambler
- **GIVEN** the redesigned History tab is available
- **WHEN** the gambler uses Scores, Bets, the drawer, change-pool, or another gambler's timeline
- **THEN** their existing actions and presentation remain intact
- **AND** another gambler's prediction is not labeled as the signed-in gambler's bet

#### Scenario: Reaching the first and last content
- **GIVEN** a short window, system bars, or a bottom tab bar constrains the viewport
- **WHEN** History is scrolled to either end in a loaded, loading, empty, or error state
- **THEN** the first and last content and recovery actions can be fully revealed outside obstructing chrome
- **AND** the list retains its existing platform scrolling and inset behavior

### Requirement: History is localized and accessible

History SHALL support English and Spanish, light and dark appearance, large accessibility text, narrow phones, and wider or resized windows. Date, time, points, prediction labels, summary states, and retry actions SHALL be localized. Team names and scores SHALL remain distinguishable without overlap; layouts SHALL reflow and grow vertically when necessary rather than fixing row heights or shrinking accessible text. Each loaded row SHALL expose one coherent screen-reader announcement covering date/time, both teams, final result, the gambler's prediction, and awarded points, with its existing activation available. The total SHALL have a separate meaningful announcement identifying it as the current pool's earned points. Decorative flags, separators, and pill backgrounds SHALL NOT introduce extra stops. Spoken points SHALL use full localized singular/plural words, and color SHALL NOT be the sole means of conveying awards or unavailable data.

#### Scenario: Reading History with assistive technology
- **GIVEN** VoiceOver or TalkBack is enabled
- **WHEN** the gambler navigates the summary and a loaded match row
- **THEN** the total is identified as current-pool earned points and the row distinguishes the final result, prediction, and award in one logical announcement
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
