# android-edge-to-edge Specification

## Purpose

Make Fortuna's Android screens draw to the window edges while keeping content, text entry, navigation, and actions usable around system bars, display cutouts, and the software keyboard.

## Requirements

### Requirement: Android screens use the full window safely

All app-owned Android activity windows SHALL support edge-to-edge presentation. Screen backgrounds SHALL extend into system-bar areas while interactive controls and essential content remain outside system bars, display cutouts, and window caption controls. Safe bounds SHALL update with window size, orientation, and navigation mode, without applying the same inset twice. The Android application SHALL explicitly target SDK 36 and retain its existing minimum supported SDK.

#### Scenario: Opening a screen with system bars visible
- **GIVEN** the app runs with gesture or three-button navigation
- **WHEN** the gambler opens a home, sign-in, pool, bet, profile, or pool-join screen
- **THEN** the screen background reaches the window edges
- **AND** navigation controls and actions remain visible and tappable outside system UI

#### Scenario: Changing the available window
- **GIVEN** a screen is visible on a device with a display cutout or in a window with caption controls
- **WHEN** the window rotates or resizes
- **THEN** essential content and actions use the new safe bounds, including horizontal bounds
- **AND** no stale or duplicate system inset leaves an unnecessary blank band

### Requirement: Lists scroll within an edge-to-edge viewport

Android lists SHALL have a viewport extending into available system-bar regions, with safe spacing applied inside their scrolling content rather than shrinking their enclosing viewport. First and last items SHALL be fully reachable outside system UI and fixed app bars at their respective scroll limits. Scrolling SHALL allow content to pass behind system-bar protection or app chrome where those occupy the window edge. Paging, refresh, item actions, and accessibility semantics SHALL retain their existing behavior.

#### Scenario: Reaching the ends of a list
- **GIVEN** a pool, leaderboard, bet, gambler-management, or pool-template list has enough items to scroll
- **WHEN** the gambler scrolls to the beginning and then to the end
- **THEN** the first and last items and their actions can be fully revealed clear of system UI and fixed app chrome
- **AND** intermediate scrolling is not clipped by an outer system-inset margin

#### Scenario: Refreshing or loading another page
- **GIVEN** a list has edge-to-edge scrolling enabled
- **WHEN** the gambler refreshes it or reaches the next page
- **THEN** refresh and pagination retain their existing behavior
- **AND** their visible indicators and recovery actions remain clear of obscuring chrome

### Requirement: All list states respect the same safe bounds

Loaded, loading, empty, and error states SHALL respect the screen's inset policy. Model-backed loading placeholders affected by this migration SHALL populate the production item or row component with a placeholder model and apply the shared pulse loading-placeholder treatment defined by `loading-placeholders`. Placeholder behavior MAY suppress remote loading, navigation, and accessibility exposure but SHALL NOT replace the production layout with a separate skeleton-only layout or expose placeholder values as real content.

#### Scenario: Loading completes
- **GIVEN** an Android list displays loading placeholders
- **WHEN** data arrives
- **THEN** loaded rows use the same production layout and screen inset policy as the placeholder rows
- **AND** the transition introduces no duplicate system padding
- **AND** placeholder values were neither announced as real data nor actionable

#### Scenario: Empty or failed list
- **GIVEN** a list is empty or its request fails
- **WHEN** its empty or error state is shown
- **THEN** the message and any retry or creation action are visible or reachable by scrolling outside system UI

### Requirement: Text entry remains usable with the keyboard open

All Android text-entry flows, including email sign-in, email-and-password sign-in, pool naming, username editing, and pending-bet score entry, SHALL keep the focused field visible above the software keyboard. Related validation and submission actions SHALL remain reachable, using scrolling when space is limited. Keyboard transitions SHALL neither pan the whole screen's top navigation out of view nor add duplicate bottom spacing. Closing the keyboard SHALL restore normal content bounds without losing the draft or resetting the gambler's selection.

#### Scenario: Completing a form in a short window
- **GIVEN** a text-entry flow is open in landscape, a short window, or with large accessibility text
- **WHEN** the gambler focuses each field and opens the keyboard
- **THEN** the focused field is visible above the keyboard
- **AND** validation and submission actions can be reached without dismissing the keyboard
- **AND** top navigation remains visible

#### Scenario: Editing a score near the list end
- **GIVEN** pending bets include an editable score near the bottom of the list
- **WHEN** the gambler focuses that score field
- **THEN** the field is brought into the visible area above the keyboard
- **AND** the pending-bet action controls are reachable without being covered by the keyboard or pool tabs

#### Scenario: Returning from the keyboard
- **GIVEN** the gambler has entered a draft and moved the caret
- **WHEN** the keyboard closes and reopens without leaving the screen
- **THEN** the draft and selection are preserved
- **AND** the content returns to the appropriate safe bounds without an extra keyboard-sized gap

#### Scenario: Preserving the username editor contract
- **GIVEN** the username editor is open
- **WHEN** the keyboard appears or the gambler scrolls
- **THEN** its existing top-app-bar back control remains visible, enabled except while saving
- **AND** Save username stays below field guidance in the scrollable content flow and can be reached above the keyboard
- **AND** existing save, retry, discard, and focus behavior remains unchanged

### Requirement: System bars remain legible over app content

Status and navigation indicators SHALL remain legible in light and dark themes, including when list content scrolls beneath them. Where a bottom app bar or pool tab bar occupies the bottom edge, its background SHALL extend through the navigation-bar area while its controls remain safely inset. System-bar protection SHALL preserve contrast without introducing an unintended contrasting band over that background.

#### Scenario: Scrolling under the status bar
- **GIVEN** a list is displayed in either theme
- **WHEN** content scrolls beneath the status-bar area
- **THEN** status indicators remain distinguishable from that content through app-bar coverage or theme-aware protection

#### Scenario: Pool tabs with three-button navigation
- **GIVEN** pool home is displayed with three-button navigation
- **WHEN** the gambler switches among its tabs
- **THEN** the bottom bar background extends to the bottom window edge without an unintended system contrast band
- **AND** tab controls remain above the navigation buttons

### Requirement: Overlays and drawers preserve safe interaction

App-owned drawers, sheets, and any full-screen dialogs SHALL keep essential content and actions within current safe bounds while their backgrounds use the intended edge-to-edge extent. Full-screen dialogs SHALL follow the same system-bar and keyboard contract as activity screens. Drawer gestures, system Back priority, accessibility isolation, and dismissal behavior SHALL retain their existing contracts. Operating-system-owned pickers and dialogs are outside this requirement.

#### Scenario: Drawer in a constrained window
- **GIVEN** a pool drawer is open with large text or a short window
- **WHEN** the gambler scrolls its actions or performs system Back
- **THEN** actions remain reachable clear of system UI
- **AND** system Back dismisses the drawer before navigating away
- **AND** inset handling does not apply duplicate safe spacing to the pushed screen

#### Scenario: App-owned overlay reaches a window edge
- **GIVEN** an app-owned sheet or full-screen dialog is displayed
- **WHEN** the keyboard opens or the window changes size
- **THEN** essential controls remain visible or reachable within the current safe bounds
- **AND** a full-screen dialog background continues to reach the window edges
