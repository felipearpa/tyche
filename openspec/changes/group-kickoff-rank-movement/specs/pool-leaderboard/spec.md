## MODIFIED Requirements

### Requirement: Reference-aligned leaderboard row
Each leaderboard entry SHALL reserve stable areas for a rank rail, circular gambler avatar, gambler identity, and trailing score. The rank rail SHALL contain a rounded-square position tile and place movement immediately beneath the tile, and each entry SHALL end with a full-column separator. The tile SHALL show the current authoritative position independently of the historical group positions used for movement. A partial movement SHALL include a small, localized computed/full-count qualifier associated with the movement, such as the equivalent of `1 of 2 computed`. Pending, unavailable, or updating movement SHALL use an explicit localized nonnumeric state rather than a steady rule or fabricated zero. Qualifiers and state text SHALL wrap or reflow within the row when needed without obscuring the rank, identity, or score, shrinking accessible text, or changing the surrounding screen shell.

#### Scenario: Complete score row
- **GIVEN** a gambler has a current position, authoritative kickoff-group movement, username, avatar state, and score
- **WHEN** the row is rendered
- **THEN** it displays the rank tile and movement in the rank rail, a circular avatar and username in the identity area, and a bold trailing-aligned score

#### Scenario: Long username
- **GIVEN** the gambler's username cannot fit between the avatar and reserved score area
- **WHEN** the row is rendered
- **THEN** the visible username is limited to one line and ellipsized before the score
- **AND** the full username remains available to assistive technology

#### Scenario: Missing rank or score
- **GIVEN** a row has no current position or no score
- **WHEN** the row is rendered
- **THEN** an em dash appears in the corresponding reserved area and the remaining columns stay aligned

#### Scenario: Partial movement needs extra space
- **GIVEN** the newest eligible kickoff group has one of two matches computed and the row has a numeric movement
- **WHEN** the row is rendered in a narrow window or at a large text size
- **THEN** the movement retains its computed/full-count qualifier using wrapping or reflow within the row
- **AND** the current rank, full accessible identity, score, and existing row action remain available without changes to the title or navigation

### Requirement: Rank movement semantics
The leaderboard SHALL consume the required discriminated `groupMovement` object defined by `kickoff-group-standings`. Its selected-group states SHALL provide the group identity and distinguish complete numeric movement, partial numeric movement, explicitly unavailable historical movement, and updating movement. When no group has a computed member, the object SHALL report pending with reason `noComputedGroups`, without a selected group identity or numeric movement. Numeric movement SHALL be the published group's rank before the group minus its rank after the currently computed group members. The score contract SHALL retire the per-match `beforePosition` field; clients SHALL use group movement directly rather than derive a trend from that field or the current `position`. The selected group SHALL be the newest scheduled-kickoff group with at least one computed member; result-processing order SHALL NOT choose it. Further results from that group SHALL update movement progressively from the same group baseline. The leaderboard SHALL distinguish up, down, confirmed no movement, partial numeric movement, pending, explicitly unavailable movement, and updating movement through text or symbols and semantic color where applicable. A partial numeric movement SHALL always carry its authoritative computed/full count. An unreconstructable group baseline SHALL be explicitly identified as unavailable rather than rendered as a false zero. Missing or malformed `groupMovement` SHALL make the response invalid and recoverable through existing localized load/retry behavior, retaining previously usable content rather than treating missing data as successful unavailable movement. Current rank display SHALL remain independent of the selected group's historical rank boundaries because their comparison membership can differ.

#### Scenario: Gambler moved up
- **GIVEN** a published kickoff group moved a gambler from historical position 6 before the group to position 3 after its currently computed members
- **WHEN** movement is rendered
- **THEN** the rank rail shows an upward indicator with the absolute value 3 using the positive color

#### Scenario: Gambler moved down
- **GIVEN** a published kickoff group moved a gambler from historical position 4 before the group to position 6 after its currently computed members
- **WHEN** movement is rendered
- **THEN** the rank rail shows a downward indicator with the absolute value 2 using the negative color

#### Scenario: Gambler position is unchanged
- **GIVEN** the authoritative group movement is numeric zero because its historical before-group and after-group positions are equal
- **WHEN** movement is rendered
- **THEN** the rank rail shows a neutral steady indicator using the warning/amber color

#### Scenario: Previous position is unavailable
- **GIVEN** the current position exists but the required `groupMovement` explicitly reports an unavailable historical group baseline
- **WHEN** the row is rendered
- **THEN** movement is identified as unavailable without claiming a numeric value or steady movement and the current rank tile remains visible

#### Scenario: Multiple matches compute in one group
- **GIVEN** the newest group starts at rank 8, one computed match leaves the gambler at rank 6, and a second computed match leaves the group at rank 5
- **WHEN** the leaderboard receives the group's updated published movement
- **THEN** it shows a gain of three places from the group baseline rather than only the second match's one-place gain
- **AND** it qualifies that value as partial if more eligible group members remain uncomputed

#### Scenario: Required group movement is missing
- **GIVEN** a leaderboard response omits `groupMovement` or contains an invalid movement discriminator
- **WHEN** the client receives the response
- **THEN** it treats the response as an invalid, retryable load while retaining any previously usable content
- **AND** it does not accept missing data as a successful unavailable state, derive a last-match trend, or probe an older contract

#### Scenario: No kickoff group has computed members
- **GIVEN** the pool has no eligible group with a computed member
- **WHEN** the leaderboard renders its movement state
- **THEN** it consumes the required pending/`noComputedGroups` state with no selected group identity and communicates pending movement without a numeric value or steady-zero claim
- **AND** any available current rank and score remain independently visible

### Requirement: Shared indicator component reuse
The leaderboard SHALL render position through the existing platform position indicator (`PostionIndicator` on iOS and `PositionIndicator` on Android) and numeric group movement through the existing platform `TrendIndicator`. The row SHALL provide the authoritative group delta to that indicator; partial-computation qualifiers and nonnumeric state labels SHALL remain associated with movement without duplicating numeric trend-rendering logic. Implementations MAY extend those shared components with focused size, style, or semantic inputs required by the approved design, but SHALL NOT create leaderboard-specific replacements or duplicate their position or movement-rendering logic.

App-rendered trend icons introduced or changed by this capability SHALL prefer Material Symbols where semantically accurate; otherwise custom icons SHALL be allowed without a recorded justification. Corresponding iOS and Android trend assets SHALL derive from the same committed canonical vector source. Operating-system-rendered controls are excluded. Existing shared arrow-up, arrow-down, and steady-rule assets SHALL remain the group movement symbols; reference navigation glyphs SHALL NOT change the surrounding shell.

#### Scenario: Leaderboard renders rank and movement
- **GIVEN** a leaderboard row has a current position and an authoritative group movement difference
- **WHEN** its rank rail is rendered
- **THEN** the row composes the existing platform position indicator for the rank tile
- **AND** it composes the existing platform `TrendIndicator` for movement

#### Scenario: Reference styling needs additional configuration
- **GIVEN** the approved leaderboard design needs an indicator size or color treatment that the current shared API cannot express
- **WHEN** the indicator implementation is updated
- **THEN** the existing shared component gains a focused configuration input with defaults that preserve current behavior for existing consumers
- **AND** no leaderboard-specific position or trend component is introduced

#### Scenario: Movement state is selected
- **GIVEN** a leaderboard row provides a positive, negative, or zero movement difference
- **WHEN** movement is displayed
- **THEN** the shared `TrendIndicator` owns the direction, symbol, semantic color, and absolute-value formatting
- **AND** the leaderboard row does not reproduce that decision logic

#### Scenario: Shared group symbols on both platforms
- **GIVEN** a numeric group delta is displayed on iOS and Android
- **WHEN** the direction symbol is rendered
- **THEN** the shared indicator uses the corresponding arrow or steady-rule asset derived from the same committed canonical vector source on both platforms
- **AND** pending, unavailable, and updating states do not use the steady symbol to imply a confirmed zero

### Requirement: Leaderboard interaction and paging states
The redesigned leaderboard SHALL preserve pull-to-refresh, paged loading, inline retry, and navigation from another gambler's row to that gambler's bet timeline. The signed-in gambler's own row SHALL not expose a navigation action. Initial and append-loading placeholders SHALL render the same production `GamblerScoreItem` used for loaded scores, populated with `poolGamblerScorePlaceholderModel()` and selecting the shared pulse loading-placeholder treatment defined by `loading-placeholders` through the production component's `isPlaceholder` parameter rather than a caller-supplied effect modifier. Any model-backed movement qualifier or state placeholder SHALL use that same production row and placeholder-model approach. Implementations SHALL NOT maintain a separate skeleton-only row layout. Placeholder rendering SHALL NOT request remote avatar content, expose a navigation action, or expose placeholder model values to assistive technology.

#### Scenario: Initial request is pending
- **GIVEN** no leaderboard page has loaded yet
- **WHEN** the first request is in progress
- **THEN** each loading row renders the production `GamblerScoreItem` populated with `poolGamblerScorePlaceholderModel()`
- **AND** the shared pulse loading-placeholder treatment is applied to its placeholder presentation
- **AND** no separately maintained skeleton row layout is rendered

#### Scenario: Append request is pending
- **GIVEN** leaderboard rows are already visible
- **WHEN** the next page request is in progress
- **THEN** the appended placeholder renders the same production `GamblerScoreItem`, placeholder model, and shared pulse treatment used during initial loading
- **AND** the existing loaded rows remain visible and unchanged

#### Scenario: Placeholder row is inert
- **GIVEN** a loading placeholder row is visible
- **WHEN** the row is rendered or encountered by assistive technology
- **THEN** no remote avatar request is made using its placeholder identity
- **AND** the row exposes no navigation action
- **AND** its placeholder model values are not announced as gambler data

#### Scenario: Open another gambler
- **GIVEN** a row belongs to another gambler and gambler-detail navigation is available
- **WHEN** the user activates the row
- **THEN** Fortuna opens that gambler's bet timeline for the current pool

#### Scenario: Activate the signed-in gambler row
- **GIVEN** a row belongs to the signed-in gambler
- **WHEN** the user taps or focuses the row
- **THEN** no gambler-detail navigation occurs and assistive technology does not announce the row as a button

#### Scenario: Refresh leaderboard
- **GIVEN** leaderboard content is visible
- **WHEN** the user performs pull-to-refresh
- **THEN** the existing paging source refreshes and replaces the rows without changing the selected pool or tab

#### Scenario: Append request fails
- **GIVEN** existing leaderboard rows are visible and the next page request fails
- **WHEN** the failure is shown
- **THEN** the existing rows remain visible with an inline localized retry action

#### Scenario: Retry append request
- **GIVEN** an append failure is visible
- **WHEN** the user activates Retry
- **THEN** the failed page request is retried without reloading the entire pool screen

### Requirement: Leaderboard accessibility and localization
The leaderboard SHALL support light and dark themes, dynamic type or font scaling, localized visible copy, and equivalent VoiceOver/TalkBack meaning for current rank, identity, score, and authoritative kickoff-group movement. The existing single row announcement SHALL include a partial movement's computed/full count after its direction and place count, and SHALL explicitly communicate pending, unavailable, or updating movement when applicable. Movement qualifiers SHALL not become additional focus stops. Group states and counted match phrases SHALL be localized in every currently supported locale, including the established Spanish and Spanish (Spain) terminology where supported. Readable qualifiers SHALL wrap or reflow without overlap or clipped text.

#### Scenario: Screen reader reads a complete row
- **GIVEN** a signed-in gambler is currently rank 3 with 150 points and the newest eligible kickoff group has a complete movement up one place
- **WHEN** VoiceOver or TalkBack focuses the row
- **THEN** one logical announcement identifies rank 3, the username, the localized "You" marker, 150 points, and movement up one place in that order

#### Scenario: Trend is not conveyed by color alone
- **GIVEN** a row has rank movement
- **WHEN** it is rendered or announced
- **THEN** an arrow or steady symbol and a spoken direction accompany the semantic trend color

#### Scenario: Theme changes
- **GIVEN** the system switches between light and dark appearance
- **WHEN** the leaderboard is rendered
- **THEN** surface, text, tile, separator, and trend colors use the corresponding app theme tokens with readable contrast

#### Scenario: Spanish locale
- **GIVEN** the app locale is Spanish
- **WHEN** the leaderboard is displayed
- **THEN** the list-specific "You" marker, points, movement, partial computed/full counts, pending/unavailable/updating states, errors, and retry action are shown and announced in Spanish
- **AND** the existing title, subtitle/header content, and navigation strings remain unchanged by this redesign

#### Scenario: Partial movement is announced truthfully
- **GIVEN** the latest eligible group has a gain of three places with one of two members computed
- **WHEN** a screen reader focuses the row
- **THEN** its single announcement conveys current rank, identity, points, the three-place gain, and one of two computed
- **AND** it does not announce the partial movement as final or repeat it in a separate control

### Requirement: Counted nouns agree with their counts
Every counted value the leaderboard announces, including points, places moved, full group match counts, and computed match counts, SHALL take the grammatical number its count requires, in every supported locale. A value of one SHALL be announced in the singular and any other value in that locale's plural form. Rank is exempt: it names a position, not a quantity.

This requirement exists because the leaderboard row and the "My pools" row share one set of localized strings. A change to their grammatical number on either surface changes both, so the invariant is stated here as well as in `pool-score-list`.

#### Scenario: Gambler moved exactly one place
- **GIVEN** the authoritative selected group movement is an improvement of one place
- **WHEN** VoiceOver or TalkBack announces the row
- **THEN** the movement is announced in the singular ("Up 1 place"), not "Up 1 places"

#### Scenario: Gambler has exactly one point
- **GIVEN** a gambler's score is 1
- **WHEN** the row is announced
- **THEN** the points value is announced in the singular ("1 point"), not "1 points"

#### Scenario: Counts other than one
- **GIVEN** a counted value of zero, two, or more
- **WHEN** the row is announced in any supported locale
- **THEN** that locale's plural form is used

#### Scenario: Rank is not pluralized
- **GIVEN** a gambler is in rank 1
- **WHEN** the row is announced
- **THEN** the rank is announced as a position ("Rank 1") with no singular or plural inflection applied to it

#### Scenario: A locale is added later
- **GIVEN** a new locale is added to the leaderboard's string tables
- **WHEN** the counted strings are translated
- **THEN** each one provides every plural category that locale requires, rather than a single flat form

## ADDED Requirements

### Requirement: Latest group selection survives late results and recalculation

The leaderboard SHALL show the movement of the newest scheduled-kickoff group with at least one computed member under `kickoff-group-standings`, using the group identity, state, counts, and movement from a coherent published revision. An older group's late result or correction SHALL NOT replace that selected group merely because it was processed later. If replay changes the selected group's historical movement, a subsequent ordinary load or refresh SHALL display its recalculated movement when coherently published. During replay the last coherent group SHALL be explicitly marked updating or movement SHALL be unavailable; obsolete movement SHALL NOT silently remain confirmed current data. Current rank and score SHALL continue to represent the current standing independently of group historical boundaries. This capability SHALL not add polling, change leaderboard ordering, or use loaded History/Timeline pages to derive its group selection or movement.

#### Scenario: A newer group has no computed members
- **GIVEN** the leaderboard has a selected group with at least one computed member and a newer kickoff group has none
- **WHEN** an ordinary load or refresh returns that publication, including after the newer kickoff has passed
- **THEN** the leaderboard retains the selected computed group's movement while Timeline shows pending movement for the newer group
- **AND** a reported match score alone does not switch the leaderboard to the pending group
- **AND** after the newer group's first completed settlement is coherently published, an ordinary load or refresh selects its progressive movement and computation progress

#### Scenario: Older result arrives last
- **GIVEN** groups scheduled at 14:00 and 18:00 each have computed members and the 18:00 group is selected
- **WHEN** another result from the 14:00 group is processed after the 18:00 results
- **THEN** the leaderboard remains attached to the 18:00 group's published movement
- **AND** it does not switch to the older group because of processing recency

#### Scenario: Correction changes a later group's boundaries
- **GIVEN** a correction to an earlier group changes the latest eligible group's historical rank boundaries
- **WHEN** replay publishes a coherent replacement and the leaderboard is ordinarily loaded or refreshed
- **THEN** it shows the recalculated movement for that same latest eligible group
- **AND** it does not combine old group counts with the new movement or claim stale movement while replay is pending

#### Scenario: Current standing differs from historical group boundary
- **GIVEN** the current rank is 4 and the selected historical group moved the gambler from 8 to 5
- **WHEN** the leaderboard renders the row
- **THEN** the position tile shows current rank 4 while the movement shows an upward three places
- **AND** the client does not recalculate movement as 8 minus 4

