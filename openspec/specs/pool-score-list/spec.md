# pool-score-list Specification

## Purpose
Define the screen-reader contract for the "My pools" list — the board of pools a gambler has joined — so that each row is announced as one coherent pool standing with reachable open and invite actions, matching the announcement discipline the pool leaderboard already guarantees.

## Requirements

### Requirement: Single-element pool row announcement
Each pool row in the "My pools" list SHALL be exposed to the screen reader as a single accessibility element whose announcement conveys, in this order: the pool name, the gambler's current rank in that pool, the gambler's points, the pool's member count, and the gambler's trend-group movement or its pending/unavailable state. Partial numeric movement SHALL be followed by its computed/total match count. The individual visual parts of the row (rank tile, trend indicator, movement qualifier, pool name, points text, member-count text) SHALL NOT be reachable as separate screen-reader stops.

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
- **GIVEN** any counted value in the announcement — points, members, places moved, or group match counts
- **WHEN** the row is announced in any supported locale
- **THEN** each noun takes the grammatical number its count requires, so a value of one is announced in the singular and any other value in that locale's plural form
- **AND** rank is exempt, because it names a position rather than a quantity

#### Scenario: Partial group movement is part of the row announcement
- **GIVEN** a pool row's trend group has a gain of three places with two of four matches computed
- **WHEN** the row receives screen-reader focus
- **THEN** the single announcement ends with the three-place gain and the localized two-of-four computed qualifier
- **AND** the qualifier is not separately focusable

### Requirement: Missing rank or score is announced explicitly
When a pool row has no rank or no score, the announcement SHALL state that the value is unavailable rather than omitting it silently. Movement SHALL be announced from `beforePosition`, `position`, and `trendGroup` as defined by `kickoff-group-standings`. A row without `trendGroup` SHALL be announced as pending movement; a row with `trendGroup` but without `beforePosition` or `position` SHALL be announced as movement unavailable rather than silently omitted or represented by zero.

#### Scenario: Pool the gambler has not yet scored in
- **GIVEN** a pool row has no rank and no score
- **WHEN** the row is announced
- **THEN** the announcement states that the rank is unavailable and that the points are unavailable
- **AND** it still identifies the pool by name and states the member count

#### Scenario: No previous rank to compare
- **GIVEN** a pool row has `trendGroup` and a current rank but no `beforePosition`
- **WHEN** the row is announced
- **THEN** movement is explicitly announced as unavailable without a numeric movement claim
- **AND** the pool name, rank, points, and member count are still announced

#### Scenario: Rank is unchanged
- **GIVEN** a pool row's `beforePosition` equals its `position` with a trend group
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
Loading placeholder rows in the "My pools" list SHALL be hidden from the accessibility tree. Their filler pool name, filler rank, filler score, filler member count, and filler movement state or computation counts SHALL NOT be spoken, and their placeholder invite control SHALL NOT be offered as an action. They SHALL render the production pool row populated with a placeholder model and select the shared pulse loading-placeholder treatment defined by `loading-placeholders` through `isPlaceholder`, retaining the same adaptive layout as loaded content. Callers SHALL NOT provide an effect modifier or maintain a separate skeleton-only layout. Any placeholder qualifier SHALL use the production qualifier layout. Placeholder-specific suppression of remote loading, navigation, and accessibility SHALL not replace the production layout or expose filler values as real user content.

#### Scenario: List is loading its first page
- **GIVEN** the list shows placeholder rows using the shared pulse treatment while the first page loads
- **WHEN** a screen-reader user swipes through the list
- **THEN** no placeholder row receives focus and no filler text is spoken

#### Scenario: Next page is loading at the end of the list
- **GIVEN** the list appends a placeholder row while the next page loads
- **WHEN** the screen-reader user reaches the end of the loaded rows
- **THEN** the appended placeholder row is not announced as a pool

#### Scenario: Placeholder keeps its production layout
- **GIVEN** placeholder rows suppress their accessibility exposure
- **WHEN** they are rendered
- **THEN** they still use the production pool row populated with a placeholder model, the shared pulse loading-placeholder treatment, and the same row geometry as a loaded row

### Requirement: Rank movement is not conveyed by color alone
Numeric movement in the "My pools" list SHALL be conveyed by the existing shared `TrendIndicator` directional symbol and a spoken direction in addition to its semantic trend color, so that the direction is available without color perception and without sight. Partial movement SHALL include a visible and spoken computed/total qualifier; pending or unavailable movement SHALL use explicit localized text rather than a steady symbol that implies confirmed zero. Trend icons introduced or changed by this capability SHALL prefer Material Symbols where semantically accurate; otherwise custom icons SHALL be allowed without a recorded justification. Corresponding iOS and Android assets SHALL derive from the same committed canonical vector source. Controls rendered entirely by the operating system are excluded.

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
- **THEN** the current rank, trend, partial-computation qualifier or movement-state label, pool name, points, and member count remain legible and the invite control remains operable
- **AND** no value is truncated or clipped, and no word breaks mid-word
- **AND** text scaling does not change the row announcement's meaning or make the movement qualifier a separate focus stop

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
- **THEN** the rank, points, member count, movement, computed/total match count, pending or unavailable state, invite, and open phrases are spoken in Spanish
- **AND** the wording matches the locale's existing register ("polla" for Spanish, "quiniela" for Spanish (Spain))

#### Scenario: A long partial-computation qualifier
- **GIVEN** a partial group's localized computed/total qualifier does not fit beneath the rank at the current text size
- **WHEN** the pool row is rendered
- **THEN** the qualifier wraps or the row reflows to keep all values and the invite control readable and reachable
- **AND** accessibility text is not shrunk and no surrounding navigation element is moved

### Requirement: Pool list presentation changes remain bounded

The My pools list SHALL preserve its layout, spacing, typography, content colors, paging, pull-to-refresh, empty-state and error-state behavior, navigation destinations, and existing visible strings except for the bounded treatments stated in this requirement. The member count's grammatical number SHALL agree with its value, so a pool with exactly one member reads "1 member" rather than "1 members".

The ios-liquid-glass capability permits a bounded presentation exception: on iOS, the row invitation control SHALL use a standard non-glass appearance, and the create-pool toolbar control SHALL use the accent-filled circular control and unframed add glyph specified by ios-liquid-glass. Control-surface colors and glyph sizing can adapt to the native control, but row data placement, row layout and spacing, and the localized open and invite semantics SHALL remain unchanged. The corresponding Android create-pool action SHALL use the same canonical glyph in the accent-filled 48 dp Material filled icon button specified by ios-liquid-glass, while retaining its placement and interaction. On both platforms, accent-colored elements MAY adopt the shared accent values defined by ios-liquid-glass. The toolbar avatar, screen title, navigation copy, and bottom tab arrangement SHALL otherwise retain their existing presentation and behavior.

The loading-placeholders capability permits a further bounded exception on both platforms: loading placeholder fills, contrast, and animation SHALL adopt its shared pulse treatment, including its static presentation when motion is reduced or disabled. This exception SHALL NOT change production row layout, spacing, geometry, loaded-content colors, list canvas, invitation-control behavior, or accessibility and navigation semantics.

The kickoff-group-standings capability permits a bounded presentation exception on both platforms: the existing trend SHALL use the trend group's movement and SHALL add a small localized computed/total qualifier when partial, or a localized pending or unavailable state when numeric movement is not available. That supporting text SHALL remain visually associated with the trend, wrapping or reflowing the row only as needed for readability and accessible text sizes. This exception SHALL NOT redesign the pool list, change current rank or points semantics, restyle its canvas or controls, change open/invite behavior, or alter the surrounding title, avatar, toolbar, or navigation. Production placeholders SHALL follow the resulting production row through their placeholder model and `isPlaceholder`.

#### Scenario: Sighted user sees no difference
- **GIVEN** the My pools list before and after Liquid Glass adoption, shared pulse loading-placeholder adoption, or trend-group adoption
- **WHEN** a sighted user compares loaded rows, loading placeholders, the empty state, and the error state outside the explicitly permitted control-material, create-glyph, shared-accent, loading-placeholder, and trend-group treatments
- **THEN** those areas remain visually unchanged
- **AND** pool content, row geometry, and visible strings outside those bounded treatments are unchanged, including correct singular member counts
- **AND** tapping a row still opens the pool and tapping the invite control still starts the invite flow

#### Scenario: Pool with exactly one member
- **GIVEN** a pool the signed-in gambler has joined and nobody else has
- **WHEN** its row is rendered and announced
- **THEN** the visible member count and the announced member count both read in the singular

#### Scenario: Surrounding screen is untouched
- **GIVEN** the list is rendered inside its existing screen
- **WHEN** the permitted iOS toolbar treatment, shared create glyph, or trend-group treatment is applied
- **THEN** the rest of the screen, including the title, avatar, navigation destinations, and bottom tab arrangement, retains its existing appearance, copy, and behavior
- **AND** no navigation element is added, removed, or relocated

#### Scenario: Android pool rows retain their appearance
- **GIVEN** the My pools list is displayed on Android
- **WHEN** the shared create-pool glyph, shared pulse loading-placeholder treatment, or trend-group treatment is adopted
- **THEN** its rows, invitation controls, placeholders, and Material surfaces retain their existing appearance and behavior apart from the shared accent values, explicitly permitted loading-placeholder fills, contrast, and animation, and bounded movement value/state/qualifier changes

### Requirement: My pools and the leaderboard share the trend group

Each My pools row SHALL derive movement as `beforePosition` minus `position` and read the group's kickoff and computed/total counts from its `trendGroup`, exactly as the pool leaderboard does under `kickoff-group-standings`, so both surfaces show the same movement for the same gambler and pool. An absent `trendGroup` SHALL be presented as pending; a present `trendGroup` with a missing `beforePosition` or `position` SHALL be presented as unavailable. Counts SHALL come from the response, not from loaded History or Timeline pages. An older group's late result or correction SHALL NOT change which group the trend represents; ordinary refresh SHALL show the updated value without added polling.

#### Scenario: My pools agrees with its leaderboard
- **GIVEN** the same gambler and pool have `beforePosition` 8, `position` 5, and a trend group with two of four matches computed
- **WHEN** My pools and the leaderboard display that pool
- **THEN** both show an upward three-place movement with the two-of-four qualifier
- **AND** both show the same current rank

#### Scenario: The last processed result belongs to an older kickoff
- **GIVEN** the trend group is a newer kickoff with settled matches
- **WHEN** an older group's late result or correction settles and My pools refreshes
- **THEN** its trend remains attached to the newer group
- **AND** its value reflects the recalculated positions

#### Scenario: No kickoff group has settled
- **GIVEN** a pool row without `trendGroup`
- **WHEN** My pools renders it
- **THEN** it communicates pending movement without a numeric value or steady-zero claim
- **AND** any available current rank and score remain visible
