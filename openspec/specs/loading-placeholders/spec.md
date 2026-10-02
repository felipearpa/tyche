# loading-placeholders Specification

## Purpose
Provide consistent, restrained loading placeholders across Fortuna's iOS and Android applications while preserving production component layouts, usable loading transitions, and accessibility.

## Requirements

### Requirement: Loading presentation reuses production components

Each affected platform SHALL populate the production item, row, or summary component with a placeholder model when model-backed content is unavailable. Initial and append placeholders SHALL use the same component layout rules, dimensions, typography, adaptive behavior, and inset policy as loaded content. Implementations SHALL NOT maintain a separate skeleton-only layout. Placeholder text lengths SHALL be stable across redraws. Placeholder models SHALL remain presentation data and SHALL NOT be persisted or published as genuine account, pool, bet, or score data.

#### Scenario: First content request is pending
- **GIVEN** an affected surface has no usable model and its initial request is pending
- **WHEN** loading content is displayed
- **THEN** its production component receives a stable placeholder model
- **AND** no separately maintained skeleton layout is rendered

#### Scenario: Text size or available width changes
- **GIVEN** a production row is showing placeholder content
- **WHEN** text scaling or available width changes
- **THEN** the same layout rules used by the loaded row adapt its content
- **AND** gaps, columns, and safe bounds are not replaced by a fixed skeleton geometry

#### Scenario: Placeholder content is redrawn
- **GIVEN** the loading state has not changed
- **WHEN** a component is redrawn or the pulse advances
- **THEN** its placeholder values and reserved content lengths remain stable
- **AND** filler values are not saved or exposed through real-data sources

### Requirement: Components expose explicit placeholder state

Affected production components on iOS and Android SHALL accept an explicit `isPlaceholder` parameter that defaults to `false`. This parameter SHALL determine whether placeholder presentation and placeholder-specific behavior apply. Callers SHALL NOT have to supply a visual-effect modifier to select this state, and components SHALL NOT infer it from modifier presence, filler identity, or a request being in progress. With the default value, existing loaded-content behavior SHALL remain unchanged.

#### Scenario: Existing loaded caller omits the parameter
- **GIVEN** a production component receives a real model
- **WHEN** its caller omits `isPlaceholder`
- **THEN** it displays normal content with its existing applicable actions and accessibility

#### Scenario: Caller requests a placeholder
- **GIVEN** a caller provides a placeholder model
- **WHEN** it sets `isPlaceholder` to `true`
- **THEN** the component selects the shared loading presentation and suppresses filler-specific actions and requests
- **AND** the caller supplies no pulse object, color, timing, or effect modifier

#### Scenario: A request runs while real content remains available
- **GIVEN** a component has a usable real model and `isPlaceholder` is `false`
- **WHEN** an unrelated or refresh request starts
- **THEN** request activity alone does not change the component into a placeholder

### Requirement: Placeholders use a shared non-spatial pulse

Model-backed placeholders SHALL use the shared platform loading-placeholder treatment expressed as a smooth, repeating change in brightness or opacity across each placeholder shape. At normal system animation speed, both platforms SHALL use a 1.8-second full cycle with equal fade directions, smooth ease-in-out motion, and no endpoint pause. The effect SHALL NOT sweep a highlight across content, change its scale or layout, use blur, or make placeholder shapes fully disappear. Each visual element SHALL receive the effect only once. Structural row backgrounds, inter-item gaps, separators, and surrounding screen chrome SHALL retain their established presentation rather than pulse with the content.

#### Scenario: Normal loading animation
- **GIVEN** normal motion is enabled and placeholder content is visible
- **WHEN** a full pulse cycle is observed
- **THEN** each placeholder shape smoothly changes intensity and returns over 1.8 seconds
- **AND** its position, size, and reserved layout remain unchanged without a moving highlight band

#### Scenario: A row contains several placeholder elements
- **GIVEN** a production row includes text, an avatar, and a score area
- **WHEN** it is rendered as a placeholder
- **THEN** those content areas remain distinguishable with their intended shapes and intervening spacing
- **AND** the complete row is not covered by one solid rectangle
- **AND** nesting does not apply multiple pulses to the same element

### Requirement: Placeholder contrast follows the active appearance

Placeholder fill and intensity SHALL come from shared appearance-aware presentation values on each platform. Equivalent elements SHALL have comparable perceived contrast and pulse amplitude across iOS and Android in light and dark appearance. Literal opacity values need not be identical when platform compositing differs. Fillers SHALL NOT become legible through translucent masks. Existing row canvases and structural container fills SHALL remain governed by their production components and feature specifications; this change SHALL NOT recolor loaded content or require a blanket recoloring of rank tiles.

#### Scenario: Light or dark appearance is selected
- **GIVEN** the same loading surface is available on iOS and Android
- **WHEN** each platform displays it in light appearance and then dark appearance
- **THEN** placeholder shapes remain visible at the dimmest pulse point and restrained at the brightest
- **AND** the two platforms present comparable intensity without changing their established screen canvases

#### Scenario: A placeholder overlays a colored container
- **GIVEN** the production component includes a rank-tile or avatar background
- **WHEN** a translucent placeholder treatment is applied
- **THEN** filler text, identity glyphs, and semantic values remain concealed
- **AND** compositing does not unintentionally create an extra tint, pulse, or background layer

### Requirement: Placeholder content is inert and excluded from meaningful accessibility

While `isPlaceholder` is `true`, filler content SHALL NOT initiate remote image or avatar requests, navigation, invitations, edits, or other data-dependent actions. Placeholder rows and their controls SHALL NOT expose filler values, meaningful data announcements, focusable actions, or activation semantics to assistive technology. This suppression SHALL apply to parent activation targets as well as nested controls. Normal surrounding scrolling, refresh, navigation, and genuine error-retry actions SHALL remain available. These restrictions SHALL NOT prevent the owning screen's legitimate content request.

#### Scenario: Placeholder row is tapped or traversed
- **GIVEN** a loading row contains filler identities and normally actionable controls
- **WHEN** the user taps it, activates it through a keyboard, or traverses it with VoiceOver or TalkBack
- **THEN** no data-dependent action occurs and no filler content or placeholder action is exposed
- **AND** genuine controls outside that placeholder remain usable

#### Scenario: Placeholder avatar enters the visible area
- **GIVEN** a production avatar area is rendering a placeholder identity
- **WHEN** the row becomes visible or is recomposed
- **THEN** no remote request uses that filler identity
- **AND** the owning screen's real loading request continues normally

#### Scenario: Real data replaces filler content
- **GIVEN** a placeholder row is visible
- **WHEN** its real model is available and `isPlaceholder` becomes `false`
- **THEN** normal authorized actions, accessibility, and image loading resume for that real model

### Requirement: Motion preferences and platform compatibility are preserved

The pulse treatment SHALL work on every currently supported platform version without a blur or newer-material prerequisite: iOS and iPadOS 16 and later, and Android API 28 and later. iOS Reduce Motion and Android disabled animations SHALL replace the pulse with a stable visible placeholder at the shared treatment's midpoint intensity. Android SHALL honor nonzero system animation duration scaling. Changes to motion preferences SHALL take effect while placeholders are visible. Supported transparency and contrast preferences SHALL retain distinguishable placeholder shapes and concealed filler content.

#### Scenario: Reduced motion is enabled during loading
- **GIVEN** a placeholder is pulsing
- **WHEN** iOS Reduce Motion is enabled or Android animations are disabled
- **THEN** the animation stops and the placeholder remains visibly static at midpoint intensity
- **AND** data arrival still replaces it normally

#### Scenario: Android animation speed is changed
- **GIVEN** Android system animation duration scale is nonzero
- **WHEN** a placeholder is displayed
- **THEN** its cycle follows that scale relative to the normal 1.8-second cycle

#### Scenario: Older supported system displays loading content
- **GIVEN** the app runs on Android below API 31 or on a supported iOS version below 26
- **WHEN** a loading surface is displayed
- **THEN** it presents the same pulse or motion-preference fallback without blur or glass dependencies

#### Scenario: Contrast or transparency preferences change
- **GIVEN** a supported appearance accessibility preference is enabled
- **WHEN** a placeholder is displayed in either appearance
- **THEN** its shapes remain distinguishable and no filler value becomes readable

### Requirement: Loading transitions preserve available content

Placeholders SHALL represent unavailable content only. An append request SHALL add placeholders at the existing append-loading location while keeping loaded rows visible. A refresh with usable content SHALL retain that content and the existing refresh indication. Available data SHALL be displayed without waiting for a pulse boundary or minimum animation duration. Success, empty results, and failure SHALL replace placeholders with their existing corresponding states; an error SHALL NOT leave an endless loading animation. Placeholder animation work SHALL end when its rendering subtree leaves the active presentation or stops representing loading content.

#### Scenario: A subsequent page is pending
- **GIVEN** a list already displays loaded rows
- **WHEN** another page is requested
- **THEN** only the append-loading area displays placeholders
- **AND** existing rows retain their real content and applicable actions

#### Scenario: Loaded content is refreshed
- **GIVEN** usable content is visible
- **WHEN** the user requests a refresh
- **THEN** that content remains visible with the existing refresh indication
- **AND** the screen does not replace every row with placeholders

#### Scenario: A fast response arrives mid-pulse
- **GIVEN** placeholder presentation has begun
- **WHEN** real data becomes available before the current cycle ends
- **THEN** content is revealed without an artificial wait for that cycle

#### Scenario: Loading finishes without content
- **GIVEN** a request has no usable result yet
- **WHEN** it completes with an empty result or failure
- **THEN** the corresponding existing empty or error presentation replaces its placeholders
- **AND** existing retry behavior remains available where applicable

#### Scenario: Loading presentation is removed
- **GIVEN** an active presentation contains animated placeholders
- **WHEN** it is removed or replaced by a non-loading state
- **THEN** its placeholder animation work stops
