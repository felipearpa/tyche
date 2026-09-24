## Purpose

Provide fluid, accessible mobile navigation in Fortuna through a leading drawer that reveals account and pool actions while keeping the current screen visibly connected to its previous position.

## ADDED Requirements

### Requirement: Drawer presentation and content hierarchy

The pool list and pool home SHALL use a leading push drawer with an opaque, theme-aware background, a clear account identity area, aligned action rows, and a visually separated sign-out action. The pool home drawer SHALL present the current pool summary as a restrained group within the drawer, with its name, available position and score, and applicable pool actions. The open drawer SHALL leave a visible, tappable portion of the current screen at the trailing side.

#### Scenario: Opening the pool list drawer
- **GIVEN** an authenticated gambler is on the pool list
- **WHEN** they open the drawer
- **THEN** they can identify their account and access Profile and Sign out
- **AND** the current screen is pushed toward the trailing edge and remains partially visible

#### Scenario: Opening the pool home drawer
- **GIVEN** an authenticated gambler is viewing a pool
- **WHEN** they open the drawer
- **THEN** the drawer presents account identity, Profile, the current pool summary, Invite, and Sign out
- **AND** Gamblers with its available count and Delete pool are available only to the pool owner
- **AND** the pool summary uses an inset, subdued surface and does not dominate the navigation actions

### Requirement: Drawer content grows and fades as a coordinated group

With normal motion enabled, opening SHALL reveal the drawer content through a continuous transition from a subtly reduced scale and zero opacity to its natural scale and full opacity. Closing SHALL reverse those effects. Account identity, summary, actions, and footer SHALL participate as one content group. The drawer background SHALL remain opaque and unscaled, and the content SHALL retain its final layout geometry throughout the transition so text does not reflow as it grows.

#### Scenario: Opening with normal motion
- **GIVEN** the drawer is closed and system motion settings allow animation
- **WHEN** the gambler opens it
- **THEN** its content visibly grows and fades in together as the current screen moves aside
- **AND** labels retain their line wrapping and relative arrangement throughout the reveal
- **AND** the background behind the content remains stable

#### Scenario: Closing with normal motion
- **GIVEN** the drawer is fully open and system motion settings allow animation
- **WHEN** the gambler dismisses it
- **THEN** its content shrinks and fades out together while the current screen returns
- **AND** the content does not disappear abruptly before its closing transition finishes
- **AND** no drawer content remains visible after closing

### Requirement: Foreground screen transition is coordinated with the reveal

The current screen SHALL translate horizontally with drawer visibility and SHALL receive a progressively stronger dimming overlay while opening. Closing SHALL remove the overlay and restore the original screen position and edge appearance. The overlay SHALL follow the pushed screen's bounds, SHALL NOT dim the drawer, and SHALL NOT alter the underlying screen's theme or data. Any animated corner or edge treatment SHALL stay synchronized with the same reveal.

#### Scenario: Dimming during opening and closing
- **GIVEN** the current screen is visible in either light or dark appearance
- **WHEN** the drawer opens and then closes
- **THEN** the pushed screen progressively dims and then returns to its original colors
- **AND** the drawer remains at its intended brightness
- **AND** the translation, dimming, and content reveal begin and settle together without a separate delayed effect

### Requirement: Transitions are continuous and interruptible

In this requirement, a drawer host is the pool list or pool home, and a destination is a screen opened from a drawer host that is not itself a drawer host, such as Profile, a bet list, Manage gamblers, the username editor, or the pool creator. The pushed screen remains the drawer host's content displaced by the drawer.

A new open or close request SHALL continue from the currently displayed transition state. While a drawer host is the visible screen and its drawer is closed, a predominantly horizontal drag toward the logical trailing side that starts anywhere on the host SHALL open the drawer, following the drag continuously. While the drawer is open or transitioning, a predominantly horizontal drag that starts anywhere on the drawer, including on an action, or on the visible pushed screen SHALL move the reveal continuously. Release SHALL settle to an endpoint according to the platform's displacement and velocity behavior. A cancelled drag SHALL restore the previously settled endpoint. A gesture beginning during an animation SHALL take over without a visible jump. Once a drawer drag is recognized, the control where it started SHALL NOT activate; drawer and host controls activate only on a tap. Vertical scrolling, system-rendered bars, existing horizontal controls that handle their own drags, and system navigation gestures SHALL retain their intended behavior; where the platform reserves a screen edge for system Back, a drag starting there follows system Back instead of the drawer. Once a destination has been opened, including while the drawer is still closing after it was chosen, drawer drags SHALL NOT start, and the destination's platform back gesture and back button SHALL keep navigating back.

#### Scenario: Reversing an animation
- **GIVEN** the drawer is partly open during an opening transition
- **WHEN** a close request arrives
- **THEN** the content scale, opacity, screen position, and dimming reverse from their displayed values
- **AND** the drawer settles closed without jumping to the fully open state first

#### Scenario: Swiping the drawer open
- **GIVEN** the drawer is closed and the gambler is on the pool list or pool home
- **WHEN** they drag horizontally toward the logical trailing side from anywhere on the screen, including over a list row, and release
- **THEN** the reveal follows their movement continuously
- **AND** the drawer settles fully open or fully closed without leaving a partial interactive state
- **AND** the row under the finger does not activate

#### Scenario: Dragging the open drawer closed
- **GIVEN** the drawer is open
- **WHEN** the gambler drags horizontally toward the logical leading side from anywhere on the drawer or the visible pushed screen and releases
- **THEN** the reveal follows their movement continuously
- **AND** the drawer settles fully open or fully closed without leaving a partial interactive state

#### Scenario: Dragging from a drawer action
- **GIVEN** the drawer is open
- **WHEN** the gambler starts a horizontal drag on an action such as Profile
- **THEN** the drawer follows the drag and settles according to the release
- **AND** the action does not activate
- **AND** a tap on the same action without dragging still activates it once

#### Scenario: Cancelling a drag
- **GIVEN** a drawer drag has moved away from the previously settled endpoint
- **WHEN** the platform cancels that gesture
- **THEN** the drawer returns to that endpoint
- **AND** no underlying navigation action is triggered

#### Scenario: Scrolling without opening the drawer
- **GIVEN** the gambler is using a vertically scrollable area, a system-rendered bar such as the tab bar, or an existing horizontal control that handles its own drags
- **WHEN** they interact with that area
- **THEN** the original interaction works without accidentally opening or dismissing the drawer

#### Scenario: Swiping on a destination
- **GIVEN** the gambler has opened a destination such as Profile from the pool list or pool home
- **WHEN** they swipe horizontally toward the logical trailing side
- **THEN** the platform's back navigation behaves normally and the drawer does not open
- **AND** the native back button remains available

#### Scenario: Android edge swipe under gesture navigation
- **GIVEN** Android gesture navigation is active on the pool list or pool home
- **WHEN** the gambler swipes in from a screen edge
- **THEN** system Back handles the gesture, dismissing an open drawer before any route navigation
- **AND** the drawer does not start a drag from that edge

### Requirement: Dismissal isolates navigation and background interaction

While the drawer is open or transitioning, the pushed screen's controls SHALL NOT activate through the dismissal surface. Tapping the visible pushed screen SHALL dismiss the drawer. Android system Back SHALL dismiss the active drawer before navigating away from the current route; iOS SHALL support accessibility escape dismissal. Once the drawer is fully closed, normal screen interaction and navigation SHALL resume. Choosing a destination SHALL close the drawer and invoke the existing action once.

#### Scenario: Tapping the pushed screen
- **GIVEN** the drawer is open or returning to the closed position
- **WHEN** the gambler taps a location over an underlying screen control
- **THEN** the drawer closes or continues closing
- **AND** the underlying control does not activate

#### Scenario: Android Back dismisses before navigation
- **GIVEN** the drawer is open on Android
- **WHEN** the gambler invokes system Back
- **THEN** the drawer closes and the current route remains active
- **AND** a later Back action after closing follows the route's normal behavior

#### Scenario: iOS accessibility escape
- **GIVEN** the drawer is open on iOS
- **WHEN** the gambler invokes the accessibility escape action
- **THEN** the drawer closes and focus returns to its opener if that control still exists

#### Scenario: Selecting an existing destination
- **GIVEN** Profile or another permitted destination is available in the drawer
- **WHEN** the gambler activates it
- **THEN** the drawer closes and the existing destination opens once
- **AND** returning to the originating route does not unexpectedly reopen the drawer

### Requirement: Platform behavior and accessibility remain native

Each platform SHALL honor its system motion settings, text scaling, focus behavior, safe areas, and navigation conventions. iOS Reduce Motion SHALL suppress decorative scale and spatial animation and use an immediate change or brief non-spatial fade; Android SHALL honor system animation duration scaling, including immediate settling when animations are disabled. The drawer SHALL expose its actions to assistive technology only when open and settled; while modal, it SHALL hide the pushed screen's descendants from accessibility traversal and provide an accessible dismissal action. Closing SHALL restore focus to the opener when the route remains active. Pixel-identical typography, timings, curves, pressed feedback, and gesture thresholds across platforms are not required.

#### Scenario: VoiceOver or TalkBack navigation
- **GIVEN** a screen reader is active
- **WHEN** the drawer finishes opening
- **THEN** focus moves to the drawer's account or first actionable element
- **AND** actions have meaningful labels and button semantics
- **AND** focus cannot reach controls behind the drawer
- **AND** an accessible dismissal action is available

#### Scenario: Drawer is closed
- **GIVEN** the drawer has fully closed
- **WHEN** the gambler explores the screen by touch, screen reader, or keyboard
- **THEN** hidden drawer actions are neither actionable nor included in focus traversal
- **AND** the visible screen's normal controls are available

#### Scenario: Reduced or disabled motion
- **GIVEN** iOS Reduce Motion is enabled or Android system animations are disabled
- **WHEN** the drawer opens or closes
- **THEN** it reaches the requested state without decorative animated scaling
- **AND** iOS avoids animated spatial movement and Android disabled animations settle immediately
- **AND** focus and interaction reach the same usable endpoint as with normal motion

### Requirement: Drawer adapts to available space

Drawer sizing SHALL use the available application window, maintain a bounded readable width, and preserve the visible dismissal portion of the current screen. Content SHALL respect system insets and remain reachable through scrolling when height or text size requires it. Leading and trailing placement, movement, and gestures SHALL follow the active layout direction. Resizing during a transition SHALL leave the drawer in a valid state with correctly aligned interaction areas.

#### Scenario: Large text or short window
- **GIVEN** large accessibility text or a short application window leaves insufficient height for all drawer content
- **WHEN** the drawer opens
- **THEN** account and action text remains readable without overlapping controls
- **AND** all permitted actions, including Sign out, remain reachable by scrolling

#### Scenario: Wide window or resize
- **GIVEN** the app uses a wide window or its window changes size during a reveal
- **WHEN** the drawer is laid out in the new bounds
- **THEN** it uses a bounded menu width instead of filling most of a wide screen
- **AND** the visible dismissal area and interaction bounds remain aligned with the pushed screen

#### Scenario: Right-to-left layout
- **GIVEN** the active layout direction is right-to-left
- **WHEN** the drawer opens or is dragged
- **THEN** it reveals from the logical leading side and pushes the screen toward the logical trailing side
- **AND** the drawer's alignment and gesture directions follow that layout

### Requirement: Existing data and action states survive the redesign

Drawer presentation SHALL continue using the shared current-account and avatar sources. Opening, closing, or animating SHALL NOT reset loaded data or introduce drawer-specific account or avatar fetches. Pool ownership visibility, deletion confirmation and pending state, invitation, sign-out, and pool-summary error handling SHALL retain their current behavior. During model-backed summary loading, each platform SHALL populate the production summary component with a placeholder model and apply its existing shared shimmer treatment; placeholder values SHALL NOT be exposed as real user content. Placeholder behavior can suppress remote loading, navigation, and accessibility exposure, but SHALL NOT replace the production layout with a separate skeleton layout.

#### Scenario: Reopening with cached account information
- **GIVEN** the account and avatar are already available from their shared sources
- **WHEN** the drawer closes and reopens
- **THEN** the same account identity and available avatar remain visible
- **AND** the animation does not reset them to a loading state or cause an independent duplicate fetch

#### Scenario: Pool summary is loading or fails
- **GIVEN** pool-summary data is loading or its request fails
- **WHEN** the pool drawer is displayed
- **THEN** loading uses the production summary component populated with a placeholder model and the shared platform shimmer
- **AND** placeholder values are excluded from meaningful accessibility content and cannot trigger actions
- **AND** a failure presents the existing error behavior without disabling unrelated account or navigation actions

#### Scenario: Owner requests deletion
- **GIVEN** the signed-in gambler owns the current pool
- **WHEN** they activate Delete pool
- **THEN** the existing confirmation is required before deletion starts
- **AND** pending deletion still prevents duplicate deletion requests

### Requirement: Changed drawer icons share canonical sources

Any app-rendered icon introduced or changed by this redesign SHALL prefer Material Symbols when a semantically accurate symbol exists; otherwise a custom icon is allowed without a justification requirement. Both platforms SHALL use assets derived from the same committed canonical vector source for each affected icon. This requirement excludes controls rendered entirely by the operating system and does not require unrelated existing icons to be revised.

#### Scenario: Introducing or replacing a drawer icon
- **GIVEN** implementation introduces or replaces an app-rendered drawer icon
- **WHEN** its iOS and Android assets are prepared
- **THEN** both assets derive from the same committed canonical vector
- **AND** a semantically accurate Material Symbol is preferred when available
