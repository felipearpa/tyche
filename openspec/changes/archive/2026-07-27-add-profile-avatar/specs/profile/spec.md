## ADDED Requirements

### Requirement: Profile screen entry point
The app SHALL provide a Profile screen on iOS and Android, reached from the navigation drawer. The drawer row that currently opens the username editor SHALL instead navigate to the Profile screen, and both Android drawer entry points SHALL use the same presentation (pushed screen).

#### Scenario: Opening the profile from the drawer
- **WHEN** the user taps the Profile row in the navigation drawer
- **THEN** the Profile screen is pushed, showing the avatar, a "Change Photo" action, and a Username row with the current username

#### Scenario: Username editor no longer opens directly from the drawer
- **WHEN** the user opens the navigation drawer
- **THEN** no drawer row opens the username editor modal directly; username editing is reachable only through the Profile screen

### Requirement: Avatar display with letter fallback
The Profile screen SHALL display the account's avatar photo as a circle with a camera badge. WHEN no avatar photo exists for the account, the screen SHALL display the existing letter avatar (`EmailAvatar`) as the placeholder.

#### Scenario: Account with an uploaded avatar
- **WHEN** the Profile screen loads and an avatar object exists for the account
- **THEN** the photo is shown as a circular avatar with the camera badge overlaid

#### Scenario: Account without an avatar
- **WHEN** the Profile screen loads and no avatar object exists for the account
- **THEN** the letter avatar is shown in the same circular frame with the camera badge overlaid

#### Scenario: Avatar refreshes immediately after upload
- **WHEN** the user completes an avatar upload and returns to the Profile screen
- **THEN** the new photo is displayed without requiring an app restart or remote re-fetch

### Requirement: Username editing moves behind the Profile screen
The Profile screen SHALL contain a Username row showing the current username. Tapping it SHALL open the existing username editor, whose save behavior (validation, `PATCH /accounts`, 100-grapheme limit, retry on failure) SHALL remain unchanged.

#### Scenario: Editing the username from the profile
- **WHEN** the user taps the Username row and saves a new valid username
- **THEN** the username is updated via the existing update flow and the Profile screen shows the new value on return

#### Scenario: Drawer header reflects the change
- **WHEN** the username is saved from the Profile screen
- **THEN** the drawer header shows the updated username, as it does today

### Requirement: Account avatar in navigation chrome
The drawer header and the toolbar avatar SHALL display the signed-in account's avatar photo when one exists, and the existing letter avatar (`EmailAvatar`) otherwise. Each platform SHALL render these surfaces through one shared component so they cannot drift apart.

#### Scenario: Signed-in gambler with an avatar photo
- **WHEN** the drawer header or a toolbar renders the signed-in gambler's avatar and an avatar object exists for the account
- **THEN** the photo is shown as a circular avatar in place of the letter avatar

#### Scenario: Signed-in gambler without an avatar photo
- **WHEN** the drawer header or a toolbar renders the signed-in gambler's avatar and no avatar object exists
- **THEN** the letter avatar is shown, exactly as it is today

#### Scenario: Navigation chrome reflects a new upload
- **WHEN** the gambler completes an avatar upload from the Profile screen
- **THEN** the drawer header and toolbar avatar show the new photo without requiring an app restart

### Requirement: Avatar freshness for the signed-in gambler
WHEN the stored avatar for the signed-in account has been replaced since a client last loaded it, that client SHALL display the current stored photo rather than a superseded cached copy.

#### Scenario: Photo replaced from another device
- **WHEN** the gambler uploads a new photo on one device, then opens the app on a second device that had already displayed the previous photo
- **THEN** the second device displays the new photo rather than its cached copy of the previous one

#### Scenario: Unchanged photo is not downloaded again
- **WHEN** a client re-displays an avatar whose stored object has not changed
- **THEN** the client reuses its cached image without transferring the image bytes again
