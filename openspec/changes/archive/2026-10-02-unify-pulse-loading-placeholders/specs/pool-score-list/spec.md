## MODIFIED Requirements

### Requirement: Loading placeholder rows are not announced
Loading placeholder rows in the "My pools" list SHALL be hidden from the accessibility tree. Their filler pool name, filler rank, filler score, and filler member count SHALL NOT be spoken, and their placeholder invite control SHALL NOT be offered as an action. They SHALL render the production pool row populated with a placeholder model and the shared pulse loading-placeholder treatment defined by `loading-placeholders`, retaining the same row geometry as loaded content.

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

### Requirement: Pool list presentation changes remain bounded

The My pools list SHALL preserve its layout, spacing, typography, content colors, paging, pull-to-refresh, empty-state and error-state behavior, navigation destinations, and existing visible strings. The member count's grammatical number SHALL agree with its value, so a pool with exactly one member reads "1 member" rather than "1 members".

The ios-liquid-glass capability permits a bounded presentation exception: on iOS, the row invitation control SHALL use a standard non-glass appearance, and the create-pool toolbar control SHALL use the accent-filled circular control and unframed add glyph specified by ios-liquid-glass. Control-surface colors and glyph sizing can adapt to the native control, but row data placement, row layout and spacing, and the localized open and invite semantics SHALL remain unchanged. The corresponding Android create-pool action SHALL use the same canonical glyph in the accent-filled 48 dp Material filled icon button specified by ios-liquid-glass, while retaining its placement and interaction. On both platforms, accent-colored elements MAY adopt the shared accent values defined by ios-liquid-glass. The toolbar avatar, screen title, navigation copy, and bottom tab arrangement SHALL otherwise retain their existing presentation and behavior.

The loading-placeholders capability permits a further bounded exception on both platforms: loading placeholder fills, contrast, and animation SHALL adopt its shared pulse treatment, including its static presentation when motion is reduced or disabled. This exception SHALL NOT change production row layout, spacing, geometry, loaded-content colors, list canvas, invitation-control behavior, or accessibility and navigation semantics.

#### Scenario: Sighted user sees no difference
- **GIVEN** the My pools list before and after Liquid Glass adoption or adoption of the shared pulse loading-placeholder treatment
- **WHEN** a sighted user compares loaded rows, loading placeholders, the empty state, and the error state outside the explicitly permitted control-material, create-glyph, shared-accent, and loading-placeholder treatments
- **THEN** those areas remain visually unchanged
- **AND** pool content, row geometry, and visible strings are unchanged, including correct singular member counts
- **AND** tapping a row still opens the pool and tapping the invite control still starts the invite flow

#### Scenario: Pool with exactly one member
- **GIVEN** a pool the signed-in gambler has joined and nobody else has
- **WHEN** its row is rendered and announced
- **THEN** the visible member count and the announced member count both read in the singular

#### Scenario: Surrounding screen is untouched
- **GIVEN** the list is rendered inside its existing screen
- **WHEN** the permitted iOS toolbar treatment or shared create glyph is applied
- **THEN** the rest of the screen, including the title, avatar, navigation destinations, and bottom tab arrangement, retains its existing appearance, copy, and behavior
- **AND** no navigation element is added, removed, or relocated

#### Scenario: Android pool rows retain their appearance
- **GIVEN** the My pools list is displayed on Android
- **WHEN** the shared create-pool glyph or shared pulse loading-placeholder treatment is adopted
- **THEN** its rows, invitation controls, placeholders, and Material surfaces retain their existing appearance and behavior apart from the shared accent values and the explicitly permitted loading-placeholder fills, contrast, and animation
