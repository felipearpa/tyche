# Spec Delta

## Purpose

Give Fortuna a consistent iOS and iPadOS Liquid Glass presentation that distinguishes navigation and standalone actions from content, preserves existing interactions, and remains usable on supported older operating systems.

## ADDED Requirements

### Requirement: Liquid Glass adoption preserves operating-system support

Fortuna SHALL support iOS and iPadOS 16 and later. On version 26 and later, eligible navigation and standalone action controls SHALL use the platform Liquid Glass appearance. On supported versions below 26, the same controls SHALL offer equivalent actions, primary-versus-secondary emphasis, and enabled states using supported standard appearances. Glass availability SHALL NOT determine whether an action or destination is available.

#### Scenario: Opening the app on iOS 26 or later
- **GIVEN** Fortuna is running on iOS or iPadOS 26 or later
- **WHEN** the gambler opens an existing navigation destination or standalone sign-in or join action group
- **THEN** its eligible controls use native Liquid Glass styling
- **AND** the existing actions remain available according to their original state and authorization rules

#### Scenario: Opening the app on a supported older version
- **GIVEN** Fortuna is running on a supported iOS or iPadOS version below 26
- **WHEN** the gambler opens the same destination or action group
- **THEN** its controls render using supported standard appearances without requiring Liquid Glass
- **AND** navigation, validation, disabled states, and primary-action emphasis remain equivalent

### Requirement: Native navigation surfaces and toolbar exceptions

On iOS and iPadOS 26 and later, navigation bars, tab bars, and ordinary toolbar actions SHALL retain their native material and scroll-edge treatment without an additional app-drawn glass layer or opaque bar background. The create-pool toolbar action SHALL be a circular control filled with the shared accent, at least 44 points in diameter with a hit area covering the whole circle, with an unframed add glyph in the on-accent colour reaching at least 4.5:1 against the fill and a localized accessible name describing pool creation. As a bounded exception to the rule against app-drawn glass on bars, on iOS 26 and later this control MAY hide the toolbar's shared background and draw its own accent-tinted glass circle; it SHALL be the only toolbar control that does so, and it SHALL keep its toolbar placement. On Android the corresponding action SHALL be a Material filled icon button with a 48 dp accent-filled container, the same glyph in the on-accent colour, and the same localized accessible name. The toolbar avatar SHALL retain its circular account image or existing letter fallback without an additional shared glass surround, and SHALL continue to open the drawer.

#### Scenario: Creating a pool from the toolbar
- **GIVEN** the gambler is viewing My pools on iOS 26 or later
- **WHEN** the toolbar is rendered and the create-pool control is activated
- **THEN** a single accent-filled circular control of at least 44 points surrounds the add glyph, which reaches at least 4.5:1 against the fill in light and dark appearance, and assistive technology identifies the create-pool action
- **AND** the existing pool-creation flow opens once

#### Scenario: Opening the drawer through the avatar
- **GIVEN** the pool-list or pool-home toolbar displays the signed-in account avatar
- **WHEN** the gambler activates that control
- **THEN** the existing drawer opens
- **AND** the avatar retains its photo or letter fallback without a newly added glass surround

#### Scenario: Scrolling beneath native navigation
- **GIVEN** a pool-home tab contains scrollable content on iOS 26 or later
- **WHEN** the gambler scrolls toward the navigation or tab bar
- **THEN** the system material and scroll-edge treatment maintain the legibility of the controls
- **AND** the app does not add a second glass background over those controls

### Requirement: Content-layer controls use standard materials

App-rendered Liquid Glass SHALL NOT be used as the background of list rows, rank tiles, account or pool summaries, previews, text fields, or inline actions embedded in those content areas. Email and password fields, pool-name fields, username fields, and prediction score fields SHALL use standard theme-aware input styling. Prediction-row edit, save, cancel, and retry controls; pool-row invitation controls; paging-error retry controls within lists; and inline form submit or retry controls SHALL use standard non-glass control styling with their existing semantic emphasis. The material change SHALL preserve visible copy, validation, draft values, selection, focus, submission state, and action routing.

#### Scenario: Editing a prediction within a list
- **GIVEN** a prediction is editable
- **WHEN** the gambler enters edit mode, changes a score, and saves or cancels
- **THEN** its inputs and row actions use standard non-glass styling
- **AND** initial focus, numeric input behavior, draft retention during unrelated paging updates, and save or cancel behavior remain unchanged

#### Scenario: Retrying a failed prediction
- **GIVEN** a prediction submission has failed
- **WHEN** the failure actions are displayed
- **THEN** retry and cancel remain identifiable and operable without glass backgrounds
- **AND** retry submits the same pending prediction through the existing retry flow

#### Scenario: Editing and saving a username
- **GIVEN** the username editor is open with the keyboard visible
- **WHEN** the gambler edits or selects text and the form updates
- **THEN** the standard input keeps the existing draft, caret or selection, and validation behavior
- **AND** Save username remains in the same scrollable content flow below its guidance, including during saving and failure

#### Scenario: Inviting from a pool row
- **GIVEN** a loaded My pools row is visible
- **WHEN** its invite action is rendered or activated
- **THEN** it uses a standard non-glass control and retains its localized accessible action
- **AND** invitation opens without activating the row's open-pool action

### Requirement: Standalone glass action groups remain distinct and coordinated

The existing standalone sign-in choices, email-link continuation or recovery actions, and join-pool confirmation or recovery actions SHALL retain native glass primary and secondary controls on iOS and iPadOS 26 and later. Nearby custom glass controls within each action group SHALL have coordinated material rendering while remaining visually distinct and independently actionable at rest. Styling changes SHALL NOT introduce new decorative morphing transitions or change the order of actions. Controls supplied by an external sign-in provider SHALL retain that provider's required presentation.

#### Scenario: Choosing a sign-in method
- **GIVEN** the welcome screen displays the existing sign-in choices on iOS 26 or later
- **WHEN** the group is displayed and the gambler chooses an email sign-in method
- **THEN** adjacent glass actions remain separate, legible targets with their existing relative emphasis
- **AND** only the selected sign-in flow opens

#### Scenario: Confirming or cancelling a pool join
- **GIVEN** a join-pool screen presents adjacent confirmation and cancellation actions
- **WHEN** the gambler activates either action within its visible control area
- **THEN** the intended action is invoked once and the adjacent control is not activated
- **AND** loading or disabled state continues to prevent disallowed repeated submissions

### Requirement: Existing sheets and navigation structure remain native

Existing partial-height system sheets SHALL retain their native presentation background, available detents, and dismissal behavior without an added custom glass layer. This migration SHALL preserve the current navigation destinations, tab count, labels and ordering, tab-bar visibility behavior, and the opaque push drawer defined by navigation-drawer. It SHALL NOT introduce tab-bar minimization, a sidebar replacement, new sheet transitions, or relocation of inline form actions.

#### Scenario: Presenting an invitation sheet
- **GIVEN** the gambler requests an invitation from a pool row or the pool drawer
- **WHEN** the existing system share sheet opens
- **THEN** the existing medium and large detents and native background are retained
- **AND** dismissal returns to the same pool context

#### Scenario: Operating the drawer and tabs
- **GIVEN** the gambler is on My pools or pool home
- **WHEN** they open, drag, reverse, or close the drawer, or interact with a pool-home tab
- **THEN** the opaque drawer, displaced-screen layout, gesture ownership, and focus behavior follow the existing navigation-drawer contract
- **AND** tabs retain their current arrangement and do not gain scroll-triggered minimization

### Requirement: Materials preserve accessibility and interaction

Changed controls SHALL remain legible in light and dark appearances and with Reduce Transparency or Increase Contrast enabled, allowing native materials to respond to those preferences. App-owned transitions touched by this migration SHALL honor Reduce Motion. Controls SHALL retain localized labels, roles, enabled-state announcements, and focus order; decorative material containers SHALL NOT introduce additional accessibility stops. Large Dynamic Type, narrow iPhone widths, iPad window resizing, and the software keyboard SHALL NOT make actions unreachable or create overlapping hit areas. Changed standalone and toolbar touch controls SHALL provide at least a 44-by-44-point hit target.

#### Scenario: Using transparency and contrast preferences
- **GIVEN** Reduce Transparency or Increase Contrast is enabled in either appearance
- **WHEN** a changed control is displayed above light, dark, or mixed content
- **THEN** its label, boundary where needed, and enabled state remain distinguishable
- **AND** the app does not force the fully transparent glass appearance or remove the control's usable surface

#### Scenario: Navigating with VoiceOver
- **GIVEN** VoiceOver is enabled
- **WHEN** the gambler traverses changed controls and activates one
- **THEN** each action retains its localized name, role, and state without an extra stop for its material container
- **AND** activation reaches the same action as a touch

#### Scenario: Enlarging text or resizing the window
- **GIVEN** an accessibility text size, a narrow phone, or a resized iPad window
- **WHEN** changed actions and fields are laid out with or without the keyboard
- **THEN** their labels remain readable and actions remain reachable without overlapping hit areas
- **AND** the existing content scrolls where necessary

#### Scenario: Reducing motion
- **GIVEN** Reduce Motion is enabled
- **WHEN** an affected control changes state or the drawer is operated
- **THEN** app-owned decorative spatial motion is suppressed
- **AND** the same final action, focus, and enabled states remain available

#### Scenario: Retrying a failed page load
- **GIVEN** a list page failed to load
- **WHEN** the paging-error retry control is displayed within the list
- **THEN** it uses a standard non-glass prominent control with its existing label and placement
- **AND** activating it retries the page load once

### Requirement: Accent-filled controls keep legible labels

On iOS and Android, the shared accent colour value SHALL be #2E7D32 in light appearance and #4CAF50 in dark appearance, and the shared primary-container colour SHALL be #1B5E20 with light content in both appearances. Labels and glyphs on accent-filled and primary-container-filled controls SHALL reach at least 4.5:1 contrast against the rendered fill in both appearances, using a light label on the accent in light appearance and a dark label on the accent in dark appearance. On iOS the colour assets SHALL keep these values with Increase Contrast enabled; native control styles MAY further adjust the rendered fill for Increase Contrast provided label contrast is maintained. The iOS app icon and launch-screen logo, the Android launcher icon, and the Android splash logo SHALL use the light-appearance accent. Pool illustrations and store screenshots are not changed by this requirement.

#### Scenario: Primary action in light appearance
- **GIVEN** light appearance on iOS or Android
- **WHEN** an accent-filled primary action such as Join, Sign in, or Save is displayed
- **THEN** its fill is #2E7D32 and its label reaches at least 4.5:1 against the fill

#### Scenario: Primary action in dark appearance with Increase Contrast
- **GIVEN** dark appearance, and on iOS Increase Contrast enabled or disabled
- **WHEN** an accent-filled primary action is displayed
- **THEN** its fill derives from the #4CAF50 accent value and its dark label reaches at least 4.5:1 against the rendered fill

#### Scenario: Launching the app
- **GIVEN** the app icon, iOS launch screen, or Android splash screen is displayed
- **WHEN** the user launches Fortuna
- **THEN** the icon and splash logo use #2E7D32 where they previously used #4CAF50

### Requirement: Changed icons share canonical vector sources

For each app-rendered icon introduced or changed by this migration, the implementation SHALL prefer Material Symbols when a semantically accurate representation exists. Otherwise a custom icon SHALL be allowed without requiring or recording a justification. iOS and Android SHALL use assets derived from the same committed canonical vector source for each affected icon. Controls rendered entirely by the operating system are excluded. Unrelated existing icons SHALL NOT be changed solely to apply this requirement.

#### Scenario: Preparing the create-pool add glyph
- **GIVEN** the migration replaces the create-pool glyph with an unframed add icon
- **WHEN** the iOS and Android assets are prepared and used for the corresponding action
- **THEN** both assets derive from one committed canonical vector, preferring a semantically accurate Material Symbol
- **AND** each platform retains its native surrounding control presentation

### Requirement: Affected placeholders retain production presentation

For model-backed loading items affected by this migration, each affected platform SHALL populate its production item or row component with a placeholder model and apply the existing shared platform shimmer treatment. Placeholder-specific behavior can suppress remote loading, navigation, and accessibility exposure but SHALL NOT replace the production layout with a separately maintained skeleton. Placeholder values SHALL NOT appear as real user content or become available as meaningful accessibility content or actions.

#### Scenario: Loading My pools after its invite styling changes
- **GIVEN** the initial page or a subsequent page of My pools is loading
- **WHEN** its placeholders are displayed
- **THEN** they use the production pool row populated with a placeholder model and the existing shared shimmer
- **AND** filler account or pool values do not trigger remote loading, navigation, invitations, or meaningful accessibility exposure
