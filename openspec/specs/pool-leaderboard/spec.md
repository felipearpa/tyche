# pool-leaderboard Specification

## Purpose
Define the redesigned pool leaderboard list: reference-aligned rows with rank rail, movement, shared avatar and indicator components, signed-in gambler highlighting, native press feedback, and preserved paging/interaction behavior — all scoped strictly to the list within the existing pool screen.

## Requirements

### Requirement: List-only redesign boundary
The leaderboard redesign SHALL modify only the leaderboard list, its rows, separators, loading placeholders, and paging states. The containing screen SHALL preserve its existing title, subtitle/header content, top navigation bar, and bottom tab navigation without visual, structural, copy, or behavior changes.

#### Scenario: Redesigned list is shown in the existing screen
- **GIVEN** the current leaderboard screen contains its existing title, subtitle/header content, top navigation bar, and bottom tab navigation
- **WHEN** the redesigned leaderboard list is rendered
- **THEN** those surrounding elements remain unchanged
- **AND** only the list content at the existing list boundary uses the new design

#### Scenario: Reference contains out-of-scope screen chrome
- **GIVEN** a supplied reference includes a title, subtitle, back control, or navigation treatment that differs from the current app
- **WHEN** the reference is applied
- **THEN** those out-of-scope elements are ignored
- **AND** no title, subtitle/header, or navigation element is added, removed, moved, restyled, or relabeled

### Requirement: Consistent leaderboard canvas
The leaderboard list SHALL inherit its base background from its container, exactly as the app's other paged lists (the Bets and History tabs) do, and SHALL NOT paint its own canvas color or otherwise override the background it inherits. On iOS the inherited base background is the platform system background (pure black in dark appearance, white in light); on Android it is the Material container background supplied by the surrounding surface/scaffold (the app's dark `#121212`). Neutral (non-signed-in) rows and loading placeholders SHALL be transparent so they show the inherited canvas; only the signed-in gambler's row SHALL paint a background (its `currentUserContainer` highlight), and the rank tile SHALL keep its `surfaceVariant` fill. The canvas SHALL remain inside the existing list boundary and SHALL NOT change the containing title, subtitle/header, top navigation, or bottom navigation surfaces.

#### Scenario: Leaderboard matches the Bets and History tabs
- **GIVEN** the app is in either light or dark appearance on a given platform
- **WHEN** the leaderboard, the Bets tab, and the History tab are displayed
- **THEN** the leaderboard list uses the same base background as the Bets and History tabs
- **AND** the leaderboard is not rendered as a lighter or darker panel than those tabs

#### Scenario: Leaderboard list is shown in dark appearance
- **GIVEN** the app uses dark appearance
- **WHEN** the leaderboard list is displayed
- **THEN** its rows, inter-row spacing, loading placeholders, empty and error states, and remaining scrollable area use the inherited base background — the system background (pure black) on iOS and the Material container background (`#121212`) on Android
- **AND** the list does not paint a `surface` or other list-specific canvas color over it

#### Scenario: Leaderboard list is shown in light appearance
- **GIVEN** the app uses light appearance
- **WHEN** the leaderboard list is displayed
- **THEN** its complete canvas uses the inherited light base background (`#FFFFFF`)

#### Scenario: Neutral rows and placeholders inherit the canvas
- **GIVEN** the leaderboard is displayed in dark appearance
- **WHEN** a non-signed-in gambler row or a loading placeholder is rendered
- **THEN** it does not paint its own background fill and shows the inherited canvas (pure black on iOS, `#121212` on Android)
- **AND** the signed-in gambler row still paints its `currentUserContainer` highlight and the rank tile still uses `surfaceVariant`

#### Scenario: List canvas meets surrounding screen chrome
- **GIVEN** the leaderboard is rendered inside its existing pool screen
- **WHEN** the list shows its inherited base background
- **THEN** the background stops at the existing list boundary
- **AND** the title, subtitle/header, top navigation bar, bottom navigation, and safe-area treatment remain visually and behaviorally unchanged

### Requirement: Reference-aligned leaderboard row
Each leaderboard entry SHALL reserve stable areas for a rank rail, circular gambler avatar, gambler identity, and trailing score. The rank rail SHALL contain a rounded-square position tile and place movement immediately beneath the tile, and each entry SHALL end with a full-column separator. The tile SHALL show the current position. A partial movement SHALL include a small, localized computed/total qualifier associated with the movement, such as the equivalent of `1 of 2 computed`. Pending or unavailable movement SHALL use an explicit localized nonnumeric state rather than a steady rule or fabricated zero. Qualifiers and state text SHALL wrap or reflow within the row when needed without obscuring the rank, identity, or score, shrinking accessible text, or changing the surrounding screen shell.

#### Scenario: Complete score row
- **GIVEN** a gambler has a current position, a `beforePosition`, a `trendGroup`, username, avatar state, and score
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
- **GIVEN** the trend group has one of two matches computed and the row has a numeric movement
- **WHEN** the row is rendered in a narrow window or at a large text size
- **THEN** the movement retains its computed/total qualifier using wrapping or reflow within the row
- **AND** the current rank, full accessible identity, score, and existing row action remain available without changes to the title or navigation

### Requirement: Rank movement semantics
The leaderboard SHALL derive movement as `beforePosition` minus `position`, where `beforePosition` is the gambler's position before the pool's current trend group as defined by `kickoff-group-standings`, and SHALL read the group's kickoff and computed/total counts from the row's `trendGroup`. When `trendGroup` is absent the row SHALL present pending movement without a numeric value. When `trendGroup` is present and `beforePosition` or `position` is missing, the row SHALL present unavailable movement. A numeric movement with `computedMatches` less than `totalMatches` SHALL carry its computed/total qualifier. The leaderboard SHALL distinguish up, down, no movement, partial numeric movement, pending, and unavailable through text or symbols and semantic color where applicable. The row SHALL NOT derive movement from loaded History or Timeline pages.

#### Scenario: Gambler moved up
- **GIVEN** a row has `beforePosition` 6 and `position` 3 with a complete trend group
- **WHEN** movement is rendered
- **THEN** the rank rail shows an upward indicator with the absolute value 3 using the positive color

#### Scenario: Gambler moved down
- **GIVEN** a row has `beforePosition` 4 and `position` 6 with a complete trend group
- **WHEN** movement is rendered
- **THEN** the rank rail shows a downward indicator with the absolute value 2 using the negative color

#### Scenario: Gambler position is unchanged
- **GIVEN** `beforePosition` equals `position` with a complete trend group
- **WHEN** movement is rendered
- **THEN** the rank rail shows a neutral steady indicator using the warning/amber color

#### Scenario: Previous position is unavailable
- **GIVEN** `trendGroup` is present, `position` exists, and `beforePosition` is absent
- **WHEN** the row is rendered
- **THEN** movement is identified as unavailable without claiming a numeric value or steady movement and the current rank tile remains visible

#### Scenario: Multiple matches compute in one group
- **GIVEN** the trend group started with the gambler at rank 8, one settled match left the gambler at 6, and a second left the gambler at 5
- **WHEN** the leaderboard is refreshed after the second settlement
- **THEN** it shows a gain of three places from the group baseline rather than only the second match's one-place gain
- **AND** it qualifies that value as partial if the trend group still has matches without a result

#### Scenario: No kickoff group has settled
- **GIVEN** the rows carry no `trendGroup`
- **WHEN** the leaderboard renders movement
- **THEN** it communicates pending movement without a numeric value or steady-zero claim
- **AND** any available current rank and score remain visible

### Requirement: Shared indicator component reuse
The leaderboard SHALL render position through the existing platform position indicator (`PostionIndicator` on iOS and `PositionIndicator` on Android) and numeric movement through the existing platform `TrendIndicator`. Partial-computation qualifiers and nonnumeric state labels SHALL remain associated with movement without duplicating numeric trend-rendering logic. Implementations MAY extend those shared components with focused size, style, or semantic inputs required by the approved design, but SHALL NOT create leaderboard-specific replacements or duplicate their position or movement-rendering logic.

App-rendered trend icons introduced or changed by this capability SHALL prefer Material Symbols where semantically accurate; otherwise custom icons SHALL be allowed without a recorded justification. Corresponding iOS and Android trend assets SHALL derive from the same committed canonical vector source. Operating-system-rendered controls are excluded. Existing shared arrow-up, arrow-down, and steady-rule assets SHALL remain the movement symbols; reference navigation glyphs SHALL NOT change the surrounding shell.

#### Scenario: Leaderboard renders rank and movement
- **GIVEN** a leaderboard row has a current position and a movement difference
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

#### Scenario: Shared symbols on both platforms
- **GIVEN** a numeric movement is displayed on iOS and Android
- **WHEN** the direction symbol is rendered
- **THEN** the shared indicator uses the corresponding arrow or steady-rule asset derived from the same committed canonical vector source on both platforms
- **AND** pending and unavailable states do not use the steady symbol to imply a confirmed zero

### Requirement: Gambler avatar through shared avatar components
The leaderboard SHALL use the existing platform `AccountAvatar` photo-loading path for `avatars/<gamblerId>.jpg` and SHALL use a shared `InitialAvatar` primitive extracted from the existing `EmailAvatar` fallback rendering when the photo is absent or cannot be loaded. Existing `EmailAvatar` callers SHALL retain their current API and rendering, and the leaderboard SHALL NOT introduce a separate avatar-photo loading implementation.

#### Scenario: Gambler has an uploaded photo
- **GIVEN** the avatar object for a leaderboard gambler can be loaded
- **WHEN** the gambler's row is rendered
- **THEN** the photo is aspect-filled, clipped to the circular avatar frame, and accessible as part of the gambler identity rather than as a separate control

#### Scenario: Gambler has no uploaded photo
- **GIVEN** the avatar object returns not found
- **WHEN** the gambler's row is rendered
- **THEN** the avatar frame shows the uppercase first user-perceived letter or number of the username
- **AND** its generated palette and foreground contrast follow the same behavior as `EmailAvatar`

#### Scenario: Username is blank
- **GIVEN** no avatar photo can be loaded and the username has no visible character
- **WHEN** the fallback is rendered
- **THEN** the avatar frame shows the existing person fallback icon

#### Scenario: Avatar was replaced
- **GIVEN** a gambler's deterministic avatar object changed after another client cached the previous image
- **WHEN** that client displays or refreshes the leaderboard
- **THEN** the avatar loader follows the existing cache-revalidation contract and displays the current stored photo

#### Scenario: Existing email avatar caller
- **GIVEN** an existing surface renders `EmailAvatar` or uses the email-based `AccountAvatar` API
- **WHEN** the shared `InitialAvatar` primitive is introduced
- **THEN** that surface keeps the same initial derivation, generated colors, fallback icon, and photo-loading behavior without call-site changes

### Requirement: Signed-in gambler identification
The leaderboard SHALL identify the signed-in gambler with a localized "You" label and a dedicated non-Material `currentUser` semantic color family. The full row SHALL be highlighted, and its rank, fallback avatar, username, and score SHALL use the corresponding theme-aware current-user roles while rank movement retains its gain/drop/steady semantics.

#### Scenario: Signed-in gambler row
- **GIVEN** a score row belongs to the signed-in gambler
- **WHEN** the row is rendered
- **THEN** the full row uses the current-user container
- **AND** its rank tile uses the stronger nested current-user container treatment
- **AND** the rank, username, localized "You" label, and score use the current-user container foreground
- **AND** a fallback avatar uses the filled current-user color and its paired foreground
- **AND** the row remains identifiable as the current user without depending on color alone

#### Scenario: Signed-in gambler has an uploaded avatar
- **GIVEN** the signed-in gambler has an uploaded avatar photo
- **WHEN** the highlighted row is rendered
- **THEN** the photo is shown without a color tint while the row, rank, username, "You" label, and score retain the current-user treatment

#### Scenario: Another gambler row
- **GIVEN** a score row belongs to another gambler
- **WHEN** the row is rendered
- **THEN** it does not display the "You" label or the current-user row treatment

#### Scenario: Current-user colors adapt to theme
- **GIVEN** the system uses either light or dark appearance
- **WHEN** the signed-in gambler row is rendered
- **THEN** the platform selects the matching `currentUser`, `onCurrentUser`, `currentUserContainer`, and `onCurrentUserContainer` values
- **AND** every current-user foreground/background pair meets at least 4.5:1 contrast

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

### Requirement: Native actionable-row press feedback
Each actionable leaderboard row SHALL be one full-row semantic activation target and SHALL provide immediate press feedback using the platform's native interaction convention. Android SHALL retain the theme-aware Material ripple/indication supplied by its clickable primitive. iOS SHALL use a semantic button with a reusable shared pressed/highlight style rather than gesture-only activation or an imitation of the Android ripple. A cancelled press SHALL clear the feedback without activating the row, and a non-actionable row SHALL expose neither press feedback nor activation semantics.

#### Scenario: Press another gambler's row on Android
- **GIVEN** another gambler's row can open that gambler's bet timeline
- **WHEN** the user presses the row on Android
- **THEN** the full-row target shows the theme-aware Material ripple or indication immediately
- **AND** releasing a valid press activates the existing navigation action

#### Scenario: Press another gambler's row on iOS
- **GIVEN** another gambler's row can open that gambler's bet timeline
- **WHEN** the user presses and holds the row on iOS
- **THEN** the full-row semantic button shows the shared platform-appropriate pressed/highlight state immediately
- **AND** releasing a valid press activates the existing navigation action

#### Scenario: Cancel a row press
- **GIVEN** an actionable leaderboard row is showing press feedback
- **WHEN** the user drags away or the interaction is otherwise cancelled
- **THEN** the feedback clears
- **AND** no gambler-detail navigation occurs

#### Scenario: Press the signed-in gambler row
- **GIVEN** the row belongs to the signed-in gambler
- **WHEN** the user presses or focuses the row
- **THEN** the row shows no actionable press feedback
- **AND** it exposes no button or click action to assistive technology

#### Scenario: Reuse the interaction in a later list redesign
- **GIVEN** another iOS list needs the same actionable-row behavior
- **WHEN** that list adopts press feedback
- **THEN** it can compose the shared semantic row button style without copying leaderboard-specific interaction code

### Requirement: Leaderboard accessibility and localization
The leaderboard SHALL support light and dark themes, dynamic type or font scaling, localized visible copy, and equivalent VoiceOver/TalkBack meaning for current rank, identity, score, and movement. The existing single row announcement SHALL include a partial movement's computed/total count after its direction and place count, and SHALL explicitly communicate pending or unavailable movement when applicable. Movement qualifiers SHALL not become additional focus stops. Group states and counted match phrases SHALL be localized in every currently supported locale, including the established Spanish and Spanish (Spain) terminology where supported. Readable qualifiers SHALL wrap or reflow without overlap or clipped text.

#### Scenario: Screen reader reads a complete row
- **GIVEN** a signed-in gambler is currently rank 3 with 150 points and the trend group moved the gambler up one place with every match computed
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
- **THEN** the list-specific "You" marker, points, movement, partial computed/total counts, pending and unavailable states, errors, and retry action are shown and announced in Spanish
- **AND** the existing title, subtitle/header content, and navigation strings remain unchanged by this redesign

#### Scenario: Partial movement is announced truthfully
- **GIVEN** the trend group has a gain of three places with one of two matches computed
- **WHEN** a screen reader focuses the row
- **THEN** its single announcement conveys current rank, identity, points, the three-place gain, and one of two computed
- **AND** it does not repeat the qualifier in a separate control

### Requirement: Counted nouns agree with their counts
Every counted value the leaderboard announces, including points, places moved, total group match counts, and computed match counts, SHALL take the grammatical number its count requires, in every supported locale. A value of one SHALL be announced in the singular and any other value in that locale's plural form. Rank is exempt: it names a position, not a quantity.

This requirement exists because the leaderboard row and the "My pools" row share one set of localized strings. A change to their grammatical number on either surface changes both, so the invariant is stated here as well as in `pool-score-list`.

#### Scenario: Gambler moved exactly one place
- **GIVEN** the trend group movement is an improvement of one place
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

### Requirement: Movement follows the newest settled kickoff group

The leaderboard SHALL show the movement of the pool's trend group, which `kickoff-group-standings` defines as the newest kickoff group with a settled match. An older group's late result or correction SHALL NOT replace that group; its effect on positions appears inside the trend group's movement on the next load or refresh. A newer group whose matches have no result yet SHALL NOT replace the trend group. This capability SHALL not add polling, change leaderboard ordering, or use loaded History or Timeline pages to derive movement.

#### Scenario: A newer group has not settled
- **GIVEN** the trend group is today's 14:00 kickoff and an 18:00 group has no result yet
- **WHEN** the leaderboard is refreshed after 18:00
- **THEN** it still shows the 14:00 group's movement and counts
- **AND** after the first 18:00 match settles, a refresh shows the 18:00 group's movement with its progress

#### Scenario: Older result arrives last
- **GIVEN** the trend group is 18:00 and a 14:00 match result arrives afterwards
- **WHEN** the leaderboard is refreshed
- **THEN** it remains attached to the 18:00 group
- **AND** the recalculated positions are reflected in that group's movement
