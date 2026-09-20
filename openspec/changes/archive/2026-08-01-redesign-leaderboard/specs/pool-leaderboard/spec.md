## MODIFIED Requirements

### Requirement: Leaderboard interaction and paging states
The redesigned leaderboard SHALL preserve pull-to-refresh, paged loading, inline retry, and navigation from another gambler's row to that gambler's bet timeline. The signed-in gambler's own row SHALL not expose a navigation action. Initial and append-loading placeholders SHALL render the same production `GamblerScoreItem` used for loaded scores, populated with `poolGamblerScorePlaceholderModel()` and styled with the existing shared platform shimmer treatment. Implementations SHALL NOT maintain a separate skeleton-only row layout. Placeholder rendering SHALL NOT request remote avatar content, expose a navigation action, or expose placeholder model values to assistive technology.

#### Scenario: Initial request is pending
- **GIVEN** no leaderboard page has loaded yet
- **WHEN** the first request is in progress
- **THEN** each loading row renders the production `GamblerScoreItem` populated with `poolGamblerScorePlaceholderModel()`
- **AND** the existing shared platform shimmer treatment is applied to its placeholder presentation
- **AND** no separately maintained skeleton row layout is rendered

#### Scenario: Append request is pending
- **GIVEN** leaderboard rows are already visible
- **WHEN** the next page request is in progress
- **THEN** the appended placeholder renders the same production `GamblerScoreItem`, placeholder model, and shimmer treatment used during initial loading
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
