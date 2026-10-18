# Spec Delta

## MODIFIED Requirements

### Requirement: Gamblers follows the shared screen hierarchy

The Manage gamblers destination on iOS and Android SHALL use the visual hierarchy of the reference canvas (`reference/`): the existing Back action, a scrolling heading "Gamblers" with the pool's gambler count beneath it, and a flat list of gambler rows with the shared account avatar, username, email, and thin separators. It SHALL use the app's existing canvases, typography, avatar fallback rules, and spacing. The heading SHALL be part of the scrolling content, and the top bar SHALL carry no title and no Edit, Done, or other action. Reference example values, device chrome, and the "Current app" artboards SHALL NOT become production behavior.

#### Scenario: Owner opens Gamblers from the drawer
- **GIVEN** the signed-in gambler owns the pool and opens Gamblers from the pool drawer
- **WHEN** the members load
- **THEN** the heading "Gamblers" and the count line appear above the rows
- **AND** the top bar shows only Back
- **AND** Back returns to the pool home as before

#### Scenario: Large text or a short window
- **GIVEN** large text makes the heading and count consume most of the visible height
- **WHEN** the owner scrolls
- **THEN** the heading scrolls with the rows and every row remains reachable

### Requirement: The owner's row is the current-user row

The owner's row SHALL use the current-user container and text colors, the current-user avatar fallback colors, a "You" marker beside the username, and an "Owner" pill at the trailing edge. The owner's row SHALL appear in its natural position in the list; it SHALL NOT be pinned first. The owner's row SHALL offer no swipe, no remove action, and no screen-reader remove action.

#### Scenario: Owner's row in the list
- **GIVEN** the owner is the third member returned
- **WHEN** the list renders
- **THEN** the third row is highlighted as the current user with "You" and "Owner"
- **AND** swiping that row does nothing
- **AND** the screen reader exposes no Remove action on it

### Requirement: Swiping a gambler's row requests removal

A non-owner row SHALL be removable by one horizontal swipe from the trailing edge toward the leading edge. While the row is dragged, the revealed background SHALL be the error color with the person-remove glyph and the localized "Remove" label at the trailing edge, both in the theme's on-error color; the glyph SHALL grow once the swipe passes the confirmation threshold. The confirmation threshold SHALL be a fixed drag distance of 56 points (dp on Android), independent of the row's width. Releasing past the threshold SHALL open the confirmation dialog and return the row to its resting position. There SHALL be no Edit mode, no per-row remove button, and no always-visible remove control. The row SHALL also expose a screen-reader custom action "Remove" (VoiceOver on iOS, TalkBack on Android) that opens the same dialog. A row that is removing or being removed SHALL not be swipeable. Swiping the row from the leading edge SHALL do nothing.

#### Scenario: Swipe past the threshold
- **GIVEN** a non-owner row at rest
- **WHEN** the owner drags it toward the leading edge past the threshold and releases
- **THEN** the confirmation dialog opens for that gambler
- **AND** the row returns to its resting position behind the dialog

#### Scenario: Swipe released early
- **GIVEN** a non-owner row at rest
- **WHEN** the owner drags it part way and releases before the threshold
- **THEN** the row settles back and no dialog opens

#### Scenario: TalkBack remove action
- **GIVEN** TalkBack focus is on a non-owner row on Android
- **WHEN** the owner activates the row's "Remove" custom action
- **THEN** the confirmation dialog opens for that gambler

#### Scenario: VoiceOver remove action
- **GIVEN** VoiceOver focus is on a non-owner row on iOS
- **WHEN** the owner activates the row's "Remove" custom action from the actions rotor
- **THEN** the confirmation alert opens for that gambler

### Requirement: A removing row stays in place and does not block the list

While a removal is in flight the row SHALL stay in its position, dim to the shared removing opacity, and show the shared ball spinner in place of its avatar; its username and email SHALL remain. No overlay, dialog, or progress bar SHALL cover the screen, and other rows SHALL remain removable so several removals can run at once. The removing state SHALL survive scrolling the row off screen and back, and on Android configuration changes, without restarting or losing the request. The screen reader SHALL describe the row as removing.

#### Scenario: Two removals at once
- **GIVEN** Jaime's removal is in flight
- **WHEN** the owner swipes Santiago's row and confirms
- **THEN** both rows show the removing state independently

#### Scenario: Removing row scrolls off screen
- **GIVEN** a removal is in flight on a row near the top of a long list
- **WHEN** the owner scrolls to the bottom and back
- **THEN** the row still shows the removing state, or its outcome if the request finished meanwhile

#### Scenario: Rotation during removal
- **GIVEN** a removal is in flight
- **WHEN** the device rotates
- **THEN** the removal continues and its outcome is shown on the same row

### Requirement: A failed removal is shown in its row

When a removal fails the row SHALL return to full opacity with its avatar, keep its username, replace the email line with an error-colored error glyph and the localized "Couldn't remove", and show a "Retry" pill followed by a dismiss (✕) button at the trailing edge. There SHALL be no floating banner, snackbar, toast, or dialog for the failure, and the failure SHALL NOT dismiss on a timer. Each failed row SHALL show its own failure, so several failures are visible at once. The failure SHALL persist until the owner retries, dismisses it, swipes the row again, pulls to refresh, or leaves the screen. Dismissing SHALL return the row to its idle, removable state without starting another removal and without changing the gambler count. On failure the screen SHALL play the platform's error haptic and the screen reader SHALL announce "Couldn't remove @username" without moving focus. The Retry and dismiss controls SHALL each be at least 44 points (dp on Android) and their screen-reader labels SHALL name the gambler. An in-flight removal SHALL have no cancel control; its outcome is always shown as removed or failed.

#### Scenario: Removal fails
- **GIVEN** Santiago's removal is in flight
- **WHEN** the request fails
- **THEN** Santiago's row shows "Couldn't remove" and a Retry pill
- **AND** the error haptic plays and the screen reader announces "Couldn't remove @santiago"
- **AND** no other row changes

#### Scenario: Two failures
- **GIVEN** Jaime's and Santiago's removals both fail
- **WHEN** the list renders
- **THEN** each of the two rows shows its own "Couldn't remove" and Retry

#### Scenario: Retry from the row
- **GIVEN** Santiago's row shows the failure
- **WHEN** the owner taps Retry
- **THEN** the row enters the removing state without a confirmation dialog

#### Scenario: Dismiss a failure
- **GIVEN** Santiago's row shows the failure and the count line reads "3 gamblers in this pool"
- **WHEN** the owner taps the dismiss button
- **THEN** Santiago's row returns to its idle state with his email and is removable again
- **AND** the count line still reads "3 gamblers in this pool"
- **AND** no request is sent

#### Scenario: Swipe a failed row
- **GIVEN** Santiago's row shows the failure
- **WHEN** the owner swipes it past the threshold
- **THEN** the confirmation dialog opens and confirming restarts the removal

#### Scenario: Pull to refresh with a failed row
- **GIVEN** Santiago's row shows the failure
- **WHEN** the owner pulls to refresh
- **THEN** Santiago's row returns to its idle, removable state

### Requirement: Loading and empty states

While the first page loads the screen SHALL show eight placeholder rows and a placeholder count line rendered by the production row and heading components with `isPlaceholder` set and the shared loading-placeholder treatment, offering no swipe, action, or screen-reader content. Append loading SHALL use one placeholder row. When the list has fully loaded and contains only the owner, the screen SHALL show the owner's row followed by the centered localized empty state "No other gamblers yet" with its existing explanation and a group glyph. The empty state SHALL NOT appear while a page is loading or after a load error.

#### Scenario: First load
- **GIVEN** the screen opens
- **WHEN** no page has arrived
- **THEN** eight placeholder rows and a placeholder count line are shown with no interactive affordances

#### Scenario: Only the owner
- **GIVEN** the pool's only member is the owner
- **WHEN** the list finishes loading
- **THEN** the owner's row is followed by the "No other gamblers yet" state

#### Scenario: Load error
- **GIVEN** the first page fails to load
- **WHEN** the error is shown with its retry
- **THEN** the empty state is not shown

### Requirement: Shared icon sources

The person-remove glyph in the swipe background, the error and close glyphs in a failed row, and the group glyph in the empty state SHALL be rendered on both platforms from one committed canonical vector source per glyph, using Material Symbols where one exists. The canonical source is the Android vector drawable under `Android/ui/src/main/res/drawable/` (`person_remove.xml`, `error.xml`, `close.xml`, `group.xml`), whose path data is the Material Symbol's unchanged; the iOS image set SHALL carry that same path data in an SVG rendered as a template with vector data preserved, so the two platforms draw identical geometry.

#### Scenario: Swipe background glyph
- **GIVEN** a row is being dragged
- **WHEN** the background is visible
- **THEN** its glyph is `person_remove` on both platforms, with the iOS image set's path data equal to `Android/ui/src/main/res/drawable/person_remove.xml`
