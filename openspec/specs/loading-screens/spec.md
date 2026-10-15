# loading-screens Specification

## Purpose
Give Fortuna a consistent full-screen loading presentation across iOS and Android using the Tyche ball and softly blurred content, with a deliberate no-blur fallback on older Android versions.

## Requirements

### Requirement: Existing loading screens share one presentation

The existing full-screen loading states for sending an email sign-in link, signing in with email and password, verifying an email link, Google sign-in, creating a pool, loading or joining a pool, and deleting a pool SHALL use the shared loading-screen presentation. This presentation SHALL NOT replace model-backed placeholders, inline progress indicators, pull-to-refresh or pagination feedback, launch or splash screens, or operating-system/provider-owned interfaces. Existing request triggers, cancellation rules, success routing, and error recovery SHALL remain in force.

#### Scenario: An existing blocking operation begins
- **GIVEN** the gambler is in one of the affected authentication or pool flows
- **WHEN** its existing full-screen loading state becomes active
- **THEN** the shared Tyche-ball loading presentation appears for that state
- **AND** the presentation does not start another request or change the operation's outcome handling

#### Scenario: A list or inline operation is loading
- **GIVEN** a screen uses model-backed placeholders, inline progress, refresh, or pagination feedback
- **WHEN** that existing loading state appears
- **THEN** it retains its established presentation without being replaced by a full-screen ball or receiving a new blur treatment

### Requirement: Content blur is the only loading background effect

On supported iOS versions and Android 12/API 31 or later, loading over an existing screen SHALL softly blur the underlying app content while leaving the ball sharp. Equivalent screens SHALL use comparable perceived blur strength in light and dark appearance. The loading presentation SHALL NOT add a material layer, black dimming, a colored veil, a scrim, or a card behind the ball. Normal screen colors and backgrounds SHALL remain the source of the blurred appearance.

#### Scenario: Loading over a form on a blur-capable platform
- **GIVEN** the form is visible on iOS or Android API 31 or later
- **WHEN** its blocking operation begins
- **THEN** the actual form becomes softly blurred and the ball remains sharp above it
- **AND** no extra gray, green, black, or material layer is drawn over the screen

#### Scenario: Comparing themes and platforms
- **GIVEN** equivalent content is loading on iOS and Android API 31 or later
- **WHEN** both presentations are inspected in light and dark appearance
- **THEN** the background treatment has comparable visual strength and the ball remains distinguishable in both themes
- **AND** normal screen colors are not replaced by a loading-specific tint

### Requirement: Older Android omits blur without a visual substitute

On supported Android versions below 12/API 31, loading SHALL retain the underlying screen sharp and unblurred with the same ball, placement, theme colors, interaction restrictions, and lifecycle as the blur-capable presentation. It SHALL NOT simulate blur or substitute dimming, tint, material, or a spinner card. Minimum supported versions SHALL remain Android API 28 and iOS/iPadOS 16.

#### Scenario: Loading on Android 9 through 11
- **GIVEN** Fortuna runs on Android API 28, 29, or 30
- **WHEN** an affected operation enters loading
- **THEN** the normal screen stays sharp beneath the centered Tyche ball with no added background effect
- **AND** covered controls remain unavailable until the existing flow releases loading

### Requirement: The shared Tyche ball remains the loading indicator

Both platforms SHALL retain the existing Tyche ball, rendered in a 64-by-64-point/dp area with its aspect ratio preserved, using assets derived from the same committed canonical vector source. At normal animation speed it SHALL rotate through 360 degrees linearly every two seconds without reversal. Its color SHALL use the shared accent, #2E7D32 in light appearance and #4CAF50 in dark appearance. The presentation SHALL NOT replace it with a platform stock spinner, add a percentage without measured progress, or introduce another decorative indicator.

#### Scenario: Observing normal loading
- **GIVEN** normal motion settings and an active loading screen
- **WHEN** a complete rotation is observed on each platform
- **THEN** the same ball geometry and theme color rotate once in two seconds within the same logical size
- **AND** the ball stays sharp without changing layout or scale

### Requirement: Loading preserves the existing screen and supports an empty background

Starting or ending loading SHALL preserve the existing form values, selection, loaded content, current step, and scroll position unless the operation's existing result intentionally changes them. Loading presentation SHALL NOT rebuild a fresh form with initial values, substitute fake content, or trigger additional data requests solely to produce a background. When there is no previous screen content, the ball SHALL appear on the normal light or dark screen background without an additional background effect.

#### Scenario: A submitted form returns a failure
- **GIVEN** the gambler entered values and submitted an affected form
- **WHEN** loading appears and the operation then fails
- **THEN** the values and form step remain consistent before, during, and after loading
- **AND** the existing error and retry flow becomes available without a presentation-induced reset

#### Scenario: Loading starts without prior content
- **GIVEN** email-link verification or initial pool loading has no previous content to display
- **WHEN** its loading state is presented
- **THEN** the ball appears on the normal themed screen background
- **AND** no fabricated form, placeholder screen, material, dimming, or colored overlay is introduced

### Requirement: Loading isolates covered controls and announces its state

While the full-screen loader is active, covered app content SHALL reject touch activation, scrolling, keyboard input, and accessibility activation. Covered controls SHALL NOT remain in the assistive-technology traversal order. The loader SHALL expose one localized indeterminate loading status without presenting the ball as an action or repeatedly announcing each animation frame. When loading ends, the resulting screen SHALL recover its appropriate interaction and accessibility state. Existing permitted system Back, cancellation, and provider-owned interactions SHALL retain their flow-specific behavior.

#### Scenario: Input reaches covered content
- **GIVEN** loading covers an editable form or navigable pool screen
- **WHEN** the gambler taps, scrolls, types, or attempts to activate a covered control
- **THEN** the covered content does not edit, navigate, scroll, or submit another operation
- **AND** blur availability does not change this restriction

#### Scenario: Assistive technology encounters loading
- **GIVEN** VoiceOver or TalkBack is enabled
- **WHEN** loading begins and is traversed
- **THEN** a single localized loading status is available and covered controls cannot be reached or activated
- **AND** completing or failing the operation makes the resulting screen's appropriate content and actions available again

#### Scenario: An external sign-in interface is active
- **GIVEN** a sign-in provider presents its own account or consent interface
- **WHEN** the gambler completes or cancels that interface
- **THEN** the existing provider interaction and app result handling remain intact
- **AND** the app's loading presentation does not obscure or intercept the provider-owned interface

### Requirement: Loading placement follows the available screen

The loader SHALL fill the app screen area assigned to it rather than the bounds of a form or individual component. The ball SHALL be centered within that area's safe visible bounds, excluding system bars, cutouts, window caption controls, and any visible software keyboard. Rotation and window resizing SHALL update placement without resetting the pending operation or adding duplicate inset spacing. Existing native navigation styling and system-bar legibility SHALL be preserved.

#### Scenario: Keyboard or window bounds change
- **GIVEN** an affected loading screen is visible
- **WHEN** the keyboard opens or closes, the device rotates, or the app window resizes
- **THEN** the ball remains centered and fully visible within the resulting safe area
- **AND** the operation and retained screen state continue without duplicate padding

### Requirement: Motion preferences and loading completion are respected

iOS Reduce Motion and Android disabled animations SHALL display a stable, visible Tyche ball without rotation or an added replacement animation. Android SHALL honor nonzero system animation duration scaling. Changes to motion preferences SHALL take effect while the loader is visible. Loading and its animation work SHALL end when the existing flow completes its handoff, fails, is canceled, or leaves the screen; presentation SHALL NOT impose a minimum duration or wait for a rotation to finish.

#### Scenario: Motion is reduced during loading
- **GIVEN** the ball is rotating during an operation
- **WHEN** iOS Reduce Motion is enabled or Android animations are disabled
- **THEN** the ball stops rotating while the accessible loading status remains available
- **AND** the operation continues and can finish normally

#### Scenario: Android animation speed is scaled
- **GIVEN** Android has a nonzero animation duration scale other than one
- **WHEN** the ball is shown
- **THEN** its rotation duration follows that scale relative to the normal two-second cycle

#### Scenario: A fast result arrives or the screen leaves
- **GIVEN** loading has begun
- **WHEN** the existing flow finishes its handoff, fails, is canceled, or leaves the screen before a rotation completes
- **THEN** the loader and its animation work end with that transition without an artificial wait
- **AND** success navigation, error recovery, or cancellation occurs according to the existing flow
