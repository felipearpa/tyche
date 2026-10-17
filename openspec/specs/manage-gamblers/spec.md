# manage-gamblers Specification

## Purpose
Let a pool owner see who is in their pool and remove gamblers from it on Android, with each removal confirmed once, shown in place while it runs, and recoverable in place when it fails.

## Requirements

### Requirement: Gamblers follows the shared screen hierarchy

The Android Manage gamblers destination SHALL use the visual hierarchy of the change's reference canvas (`reference/`): the existing Back action, a scrolling heading "Gamblers" with the pool's gambler count beneath it, and a flat list of gambler rows with the shared account avatar, username, email, and thin separators. It SHALL use the app's existing canvases, typography, avatar fallback rules, and spacing. The heading SHALL be part of the scrolling content, and the top bar SHALL carry no title and no Edit, Done, or other action. Reference example values, device chrome, and the "Current app" artboards SHALL NOT become production behavior. iOS Manage gamblers is unchanged by this capability.

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

### Requirement: Gambler count reflects the pool

The count line SHALL read the localized "N gamblers in this pool" (singular for one) from the pool's own gambler count, requested from the pool when the screen opens and again on each pull-to-refresh. It SHALL NOT be derived from the number of loaded pages. While the count is loading the line SHALL render as a loading placeholder; if the request fails the line SHALL be omitted rather than guessed, without blocking the list or showing a separate error. Each successful removal SHALL reduce the shown count by one until the next refresh replaces it; a failed removal SHALL leave it unchanged.

#### Scenario: Pool with more members than one page
- **GIVEN** a pool has 120 members and the first page holds 50
- **WHEN** the list first loads
- **THEN** the count line reads "120 gamblers in this pool"

#### Scenario: Removal succeeds
- **GIVEN** the count line reads "3 gamblers in this pool"
- **WHEN** a removal succeeds
- **THEN** the count line reads "2 gamblers in this pool"

#### Scenario: Count request fails
- **GIVEN** the members list loads but the pool request fails
- **WHEN** the screen renders
- **THEN** the heading appears without a count line and the list is usable

#### Scenario: Someone joins while the screen is open
- **GIVEN** the count line reads "3 gamblers in this pool" and a fourth gambler joins
- **WHEN** the owner pulls to refresh
- **THEN** the new row appears and the count line reads "4 gamblers in this pool"

### Requirement: The owner's row is the current-user row

The owner's row SHALL use the current-user container and text colors, the current-user avatar fallback colors, a "You" marker beside the username, and an "Owner" pill at the trailing edge. The owner's row SHALL appear in its natural position in the list; it SHALL NOT be pinned first. The owner's row SHALL offer no swipe, no remove action, and no TalkBack remove action.

#### Scenario: Owner's row in the list
- **GIVEN** the owner is the third member returned
- **WHEN** the list renders
- **THEN** the third row is highlighted as the current user with "You" and "Owner"
- **AND** swiping that row does nothing
- **AND** TalkBack exposes no Remove action on it

### Requirement: Swiping a gambler's row requests removal

A non-owner row SHALL be removable by one horizontal swipe from the trailing edge toward the leading edge. While the row is dragged, the revealed background SHALL be the error color with the person-remove glyph and the localized "Remove" label at the trailing edge, both in the theme's on-error color; the glyph SHALL grow once the swipe passes the confirmation threshold. The confirmation threshold SHALL be a fixed drag distance of 56 dp, independent of the row's width. Releasing past the threshold SHALL open the confirmation dialog and return the row to its resting position. There SHALL be no Edit mode, no per-row remove button, and no always-visible remove control. The row SHALL also expose a TalkBack custom action "Remove" that opens the same dialog. A row that is removing or being removed SHALL not be swipeable. Swiping the row from the leading edge SHALL do nothing.

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
- **GIVEN** TalkBack focus is on a non-owner row
- **WHEN** the owner activates the row's "Remove" custom action
- **THEN** the confirmation dialog opens for that gambler

### Requirement: Removal is confirmed once

The confirmation dialog SHALL keep the existing localized title "Remove @username from this pool?", the existing message, a Cancel action, and a destructive Remove action. Cancel or dismissing the dialog SHALL leave the row unchanged. Confirming SHALL start the removal and close the dialog; no further confirmation SHALL be asked for a retry of the same removal.

#### Scenario: Owner cancels
- **GIVEN** the confirmation dialog is open for Santiago
- **WHEN** the owner taps Cancel or dismisses the dialog
- **THEN** Santiago's row is unchanged and still removable

#### Scenario: Owner confirms
- **GIVEN** the confirmation dialog is open for Santiago
- **WHEN** the owner taps Remove
- **THEN** the dialog closes and Santiago's row enters the removing state

### Requirement: A removing row stays in place and does not block the list

While a removal is in flight the row SHALL stay in its position, dim to the shared removing opacity, and show the shared ball spinner in place of its avatar; its username and email SHALL remain. No overlay, dialog, or progress bar SHALL cover the screen, and other rows SHALL remain removable so several removals can run at once. The removing state SHALL survive scrolling the row off screen and back, and configuration changes, without restarting or losing the request. TalkBack SHALL describe the row as removing.

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

### Requirement: A successful removal leaves the list

When a removal succeeds the row SHALL animate out (collapse and fade) and the rows below SHALL close the gap. The removed gambler SHALL NOT reappear while the list is paged further, scrolled, or pulled to refresh, and SHALL NOT reappear after the row's state is released.

#### Scenario: Removal succeeds
- **GIVEN** Santiago's removal is in flight
- **WHEN** the server confirms it
- **THEN** Santiago's row collapses and disappears
- **AND** scrolling, paging, and pull-to-refresh do not bring Santiago back

### Requirement: A failed removal is shown in its row

When a removal fails the row SHALL return to full opacity with its avatar, keep its username, replace the email line with an error-colored error glyph and the localized "Couldn't remove", and show a "Retry" pill followed by a dismiss (✕) button at the trailing edge. There SHALL be no floating banner, snackbar, toast, or dialog for the failure, and the failure SHALL NOT dismiss on a timer. Each failed row SHALL show its own failure, so several failures are visible at once. The failure SHALL persist until the owner retries, dismisses it, swipes the row again, pulls to refresh, or leaves the screen. Dismissing SHALL return the row to its idle, removable state without starting another removal and without changing the gambler count. On failure the screen SHALL play the platform's error haptic and TalkBack SHALL announce "Couldn't remove @username" without moving focus. The Retry and dismiss controls SHALL each be at least 44 dp and their TalkBack labels SHALL name the gambler. An in-flight removal SHALL have no cancel control; its outcome is always shown as removed or failed.

#### Scenario: Removal fails
- **GIVEN** Santiago's removal is in flight
- **WHEN** the request fails
- **THEN** Santiago's row shows "Couldn't remove" and a Retry pill
- **AND** the error haptic plays and TalkBack announces "Couldn't remove @santiago"
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

While the first page loads the screen SHALL show eight placeholder rows and a placeholder count line rendered by the production row and heading components with `isPlaceholder` set and the shared loading-placeholder treatment, offering no swipe, action, or TalkBack content. Append loading SHALL use one placeholder row. When the list has fully loaded and contains only the owner, the screen SHALL show the owner's row followed by the centered localized empty state "No other gamblers yet" with its existing explanation and a group glyph. The empty state SHALL NOT appear while a page is loading or after a load error.

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

The person-remove glyph in the swipe background, the error and close glyphs in a failed row, and the group glyph in the empty state SHALL be rendered from assets derived from committed canonical vector sources under `assets/icons/`, using Material Symbols where one exists. The Android asset SHALL be derived from the canonical source the way existing entries are.

#### Scenario: Swipe background glyph
- **GIVEN** a row is being dragged
- **WHEN** the background is visible
- **THEN** its glyph is the asset derived from `assets/icons/person_remove.svg`
