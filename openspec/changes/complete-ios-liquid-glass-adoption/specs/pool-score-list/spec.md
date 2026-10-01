# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Visible pool list design is unchanged apart from plural agreement`
- TO: `### Requirement: Pool list presentation changes remain bounded`

## MODIFIED Requirements

### Requirement: Pool list presentation changes remain bounded

The My pools list SHALL preserve its layout, spacing, typography, content colors, paging, pull-to-refresh, empty-state and error-state behavior, navigation destinations, and existing visible strings. The member count's grammatical number SHALL agree with its value, so a pool with exactly one member reads "1 member" rather than "1 members".

The ios-liquid-glass capability permits a bounded presentation exception: on iOS, the row invitation control SHALL use a standard non-glass appearance, and the create-pool toolbar control SHALL use the accent-filled circular control and unframed add glyph specified by ios-liquid-glass. Control-surface colors and glyph sizing can adapt to the native control, but row data placement, row layout and spacing, and the localized open and invite semantics SHALL remain unchanged. The corresponding Android create-pool action SHALL use the same canonical glyph in the accent-filled 48 dp Material filled icon button specified by ios-liquid-glass, while retaining its placement and interaction. On both platforms, accent-colored elements MAY adopt the shared accent values defined by ios-liquid-glass. The toolbar avatar, screen title, navigation copy, and bottom tab arrangement SHALL otherwise retain their existing presentation and behavior.

#### Scenario: Sighted user sees no difference
- **GIVEN** the My pools list before and after Liquid Glass adoption
- **WHEN** a sighted user compares loaded rows, loading placeholders, the empty state, and the error state outside the explicitly permitted control-material, create-glyph, and shared-accent treatments
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
- **WHEN** the shared create-pool glyph is adopted
- **THEN** its rows, invitation controls, placeholders, and Material surfaces retain their existing appearance and behavior apart from the shared accent values
