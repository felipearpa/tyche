# pool-score-list Specification

## Purpose
Define the screen-reader contract for the "My pools" list — the board of pools a gambler has joined — so that each row is announced as one coherent pool standing with reachable open and invite actions, matching the announcement discipline the pool leaderboard already guarantees.

## Requirements

### Requirement: Single-element pool row announcement
Each pool row in the "My pools" list SHALL be exposed to the screen reader as a single accessibility element whose announcement conveys, in this order: the pool name, the gambler's rank in that pool, the gambler's points, the pool's member count, and the gambler's rank movement. The individual visual parts of the row (rank tile, trend indicator, pool name, points text, member-count text) SHALL NOT be reachable as separate screen-reader stops.

#### Scenario: Screen reader reads a complete pool row
- **GIVEN** a gambler is rank 4 with 8 points in a 101-member pool named "Neptune World Series 2023" and dropped one place
- **WHEN** VoiceOver or TalkBack focuses that row
- **THEN** one logical announcement identifies the pool name, rank 4, 8 points, 101 members, and movement down one place in that order

#### Scenario: Row parts are not announced separately
- **GIVEN** a pool row is displayed
- **WHEN** the screen-reader user swipes through the list
- **THEN** focus moves from one whole pool row to the next
- **AND** the rank tile, trend indicator, pool name, points, and member count are not individually focusable

#### Scenario: Abbreviated visible text is spoken in full
- **GIVEN** the row visibly abbreviates the score as "8 pts."
- **WHEN** the row is announced
- **THEN** the spoken points value uses the full localized phrasing ("8 points") rather than the abbreviation

#### Scenario: Member count is spoken from the visible string
- **GIVEN** the visible member count is already spelled out ("101 members")
- **WHEN** the row is announced
- **THEN** the announcement reuses that same localized string rather than a separate accessibility copy of it
- **AND** a second, duplicate member-count string SHALL NOT be introduced, so the visible and spoken counts cannot drift apart in any locale

#### Scenario: Counted nouns agree with their counts
- **GIVEN** any counted value in the announcement — points, members, or places moved
- **WHEN** the row is announced in any supported locale
- **THEN** each noun takes the grammatical number its count requires, so a value of one is announced in the singular and any other value in that locale's plural form
- **AND** rank is exempt, because it names a position rather than a quantity

### Requirement: Missing rank or score is announced explicitly
When a pool row has no rank or no score, the announcement SHALL state that the value is unavailable rather than omitting it silently. Rank movement SHALL be omitted from the announcement only when no previous rank exists to compare against.

#### Scenario: Pool the gambler has not yet scored in
- **GIVEN** a pool row has no rank and no score
- **WHEN** the row is announced
- **THEN** the announcement states that the rank is unavailable and that the points are unavailable
- **AND** it still identifies the pool by name and states the member count

#### Scenario: No previous rank to compare
- **GIVEN** a pool row has a current rank but no previous rank
- **WHEN** the row is announced
- **THEN** no movement phrase is announced
- **AND** the pool name, rank, points, and member count are still announced

#### Scenario: Rank is unchanged
- **GIVEN** a pool row's current rank equals its previous rank
- **WHEN** the row is announced
- **THEN** the announcement ends with the localized unchanged-rank phrase

### Requirement: Open-pool action is exposed
Each pool row SHALL be exposed to assistive technology as an activatable control with a button role, and activating it SHALL open that pool — the same destination the visible row tap reaches. The activation SHALL be reachable through the screen reader's standard activation gesture without requiring the user to locate a separate visual target.

#### Scenario: Screen-reader user opens a pool
- **GIVEN** a pool row has screen-reader focus
- **WHEN** the user performs the standard activation gesture
- **THEN** that pool opens, exactly as a sighted tap on the row does

#### Scenario: Row advertises that it is actionable
- **GIVEN** a pool row has screen-reader focus
- **WHEN** it is announced
- **THEN** the announcement identifies it as a button in addition to its pool content

### Requirement: Invite action is labeled and reachable
The invite control on each pool row SHALL carry a localized accessibility label naming the invite action, and SHALL be reachable by a screen-reader user from that row without leaving it. It SHALL NOT be announced as an unlabeled image or an unnamed button.

#### Scenario: Screen reader reaches the invite control
- **GIVEN** a pool row has screen-reader focus
- **WHEN** the user looks for the row's available actions
- **THEN** a localized invite action is offered
- **AND** activating it starts the same invite flow the visible control starts

#### Scenario: Invite control is never unnamed
- **GIVEN** the invite control is announced by any means
- **WHEN** the screen reader speaks it
- **THEN** it speaks the localized invite label rather than an image file name, a raw resource identifier, or a bare "button"

### Requirement: Loading placeholder rows are not announced
Loading placeholder rows in the "My pools" list SHALL be hidden from the accessibility tree. Their filler pool name, filler rank, filler score, and filler member count SHALL NOT be spoken, and their placeholder invite control SHALL NOT be offered as an action.

#### Scenario: List is loading its first page
- **GIVEN** the list shows shimmering placeholder rows while the first page loads
- **WHEN** a screen-reader user swipes through the list
- **THEN** no placeholder row receives focus and no filler text is spoken

#### Scenario: Next page is loading at the end of the list
- **GIVEN** the list appends a placeholder row while the next page loads
- **WHEN** the screen-reader user reaches the end of the loaded rows
- **THEN** the appended placeholder row is not announced as a pool

#### Scenario: Placeholder keeps its production layout
- **GIVEN** placeholder rows suppress their accessibility exposure
- **WHEN** they are rendered
- **THEN** they still use the production pool row layout with the shared shimmer treatment and the same row geometry as a loaded row

### Requirement: Rank movement is not conveyed by color alone
Rank movement in the "My pools" list SHALL be conveyed by a directional symbol and a spoken direction in addition to its semantic trend color, so that the direction is available without color perception and without sight.

#### Scenario: Movement is shown visually
- **GIVEN** a pool row has upward, downward, or unchanged movement
- **WHEN** the row is rendered
- **THEN** an upward arrow, downward arrow, or steady rule accompanies the trend color

#### Scenario: Movement is spoken
- **GIVEN** a pool row has movement
- **WHEN** the row is announced
- **THEN** the spoken direction and place count match the displayed symbol and value

### Requirement: Pool list accessibility scales and localizes
The "My pools" list SHALL remain usable at large dynamic type / font scaling settings, and every announced accessibility phrase it introduces SHALL be localized in each locale the app already supports — English, Spanish, and Spanish (Spain) — using each locale's existing register for the word "pool".

#### Scenario: Large text setting
- **GIVEN** the system text size is set to a large accessibility size
- **WHEN** the "My pools" list is displayed
- **THEN** the rank, trend, pool name, points, and member count remain legible and the invite control remains operable
- **AND** no value is truncated or clipped, and no word breaks mid-word
- **AND** the row announcement is unchanged

#### Scenario: Row cannot fit three columns
- **GIVEN** the text size is large enough that the rank rail, identity and invite control cannot share the row's width without squeezing the identity column
- **WHEN** the row is rendered
- **THEN** it reflows so each string gets the full width and wraps on word boundaries, rather than keeping three columns and breaking words
- **AND** every value stays present and whole

#### Scenario: Multi-digit rank at a large text size
- **GIVEN** a gambler's rank has more than one digit
- **WHEN** the row is rendered at the platform's largest text size
- **THEN** the complete rank is visible
- **AND** it is not clipped to its leading digits by the rank tile's bounds

#### Scenario: Spanish locale
- **GIVEN** the app locale is Spanish
- **WHEN** a pool row is announced
- **THEN** the rank, points, member count, movement, invite, and open phrases are spoken in Spanish
- **AND** the wording matches the locale's existing register ("polla" for Spanish, "quiniela" for Spanish (Spain))

### Requirement: Visible pool list design is unchanged apart from plural agreement
This accessibility pass SHALL NOT change the visible layout, spacing, colors, typography, paging, pull-to-refresh, empty state, error state, or navigation behavior of the "My pools" list. The single permitted visible change is the member count's grammatical number: it SHALL agree with its value, so a pool with exactly one member reads "1 member" rather than "1 members". Every other visible string SHALL be unchanged.

#### Scenario: Sighted user sees no difference
- **GIVEN** the "My pools" list before and after this change
- **WHEN** a sighted user compares the loaded list, the loading placeholders, the empty state, and the error state
- **THEN** they are visually identical apart from the member count of a one-member pool
- **AND** tapping a row still opens the pool and tapping the invite control still starts the invite flow

#### Scenario: Pool with exactly one member
- **GIVEN** a pool the signed-in gambler has joined and nobody else has
- **WHEN** its row is rendered and announced
- **THEN** the visible member count and the announced member count both read in the singular

#### Scenario: Surrounding screen is untouched
- **GIVEN** the list is rendered inside its existing screen
- **WHEN** the accessibility semantics are applied
- **THEN** the screen title, top navigation bar, and bottom tab navigation are unchanged in appearance, copy, and behavior
