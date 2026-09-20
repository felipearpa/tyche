## MODIFIED Requirements

### Requirement: Username editing moves behind the Profile screen
The Profile screen SHALL contain a Username row showing the current username. Tapping it SHALL push a dedicated username editor that prefills the current username, focuses a single-line field, shows a live pool preview, exposes the current count against the 100-grapheme limit, and provides a "Save username" primary action.

On Android, the username editor SHALL show a back button in the leading navigation position of its top app bar. The button SHALL remain visible when the software keyboard is open and when the editor content is scrolled, and SHALL expose a localized accessible back-navigation label. While no save is in progress, it SHALL be enabled regardless of whether the draft is unchanged, changed, invalid, or associated with a failed save. Tapping it SHALL return directly to Profile, discard any unsaved draft, and leave the stored username unchanged without submitting a username update or retry.

During saving, the Android top-app-bar back button SHALL remain visible but disabled, and system back navigation SHALL remain blocked. A failed save SHALL enable back navigation again so the gambler can leave without retrying. iOS SHALL retain its existing system-rendered navigation back control and save-lifecycle behavior.

The app-rendered back icon SHOULD use Material Symbols when a semantically accurate representation exists; otherwise a custom icon MAY be used. Any app-rendered iOS and Android variants of this icon SHALL use assets derived from the same committed canonical vector source. Controls rendered entirely by the operating system are excluded from these icon-asset requirements.

The live preview SHALL be rendered by the same production gambler-score row component used by pool leaderboards (`GamblerScoreItem` on iOS and Android), not by a separately styled replica. The row SHALL use the signed-in account id and avatar, the current trimmed draft as its username, `isCurrentUser = true`, `position = 1`, `beforePosition = 2`, and the fixed illustrative score `18`. It MUST NOT fetch or persist pool data.

The production rank rail SHALL visibly display rank tile `1` and an upward movement of `1` for every draft and save-lifecycle state at supported viewport widths and text sizes. The preview SHALL produce both elements through `GamblerScoreItem` by setting `position = 1` and `beforePosition = 2`, not through preview-specific rank UI. When horizontal space is constrained, the username SHALL truncate before the rank rail is hidden or displaced. The row's accessibility description SHALL expose the localized equivalents of "Rank 1" and "Up 1 place".

On each new presentation of the Username screen, the editor SHALL populate the stored username before focusing the text field and SHALL place a collapsed insertion caret immediately after the final character. An empty initial username SHALL place the caret at position `0`. Initial focus and caret placement SHALL occur only once per screen presentation. Draft changes, validation, preview rendering, recomposition, and save-lifecycle transitions MUST NOT request focus again or overwrite a selection subsequently made by the gambler.

The editor SHALL preserve the existing validation, trimming, `PATCH /accounts`, 100-grapheme limit, retry, local account-storage update, and username-propagation behavior. It SHALL preserve the draft through saving and failure states.

The "Save username" action SHALL appear below the field's helper or validation message in the same scrollable content flow. It SHALL scroll with the editor and SHALL NOT be presented as a sticky footer, bottom app-bar action, safe-area overlay, or control fixed to the viewport. The action SHALL remain reachable by scrolling without being obscured by the software keyboard or system insets.

#### Scenario: Opening the username editor from Profile
- **GIVEN** the signed-in gambler is viewing the Profile screen
- **WHEN** the gambler taps the Username row
- **THEN** the dedicated Username screen is pushed with the stored username prefilled, the field focused, a collapsed caret after the final character, the matching grapheme count visible, and "Save username" disabled
- **AND** on Android, the top app bar shows an enabled back button

#### Scenario: Keeping Android back navigation visible and accessible
- **GIVEN** the Android username editor is open and no save is in progress
- **WHEN** the software keyboard is shown or the gambler scrolls the editor content
- **THEN** the top-app-bar back button remains visible and available
- **AND** TalkBack exposes its localized back-navigation label and button action

#### Scenario: Leaving the Android editor without changing the username
- **GIVEN** the Android username editor is open with the stored username unchanged
- **WHEN** the gambler taps the top-app-bar back button
- **THEN** the app returns directly to Profile without submitting a username update
- **AND** Profile continues to show the stored username

#### Scenario: Discarding an unsaved Android username draft
- **GIVEN** the gambler has changed the Android username draft, including to an empty or invalid value
- **AND** no save is in progress
- **WHEN** the gambler taps the top-app-bar back button
- **THEN** the app returns directly to Profile and discards the draft without submitting a username update or retry
- **AND** Profile continues to show the stored username
- **AND** reopening the editor prefills the stored username rather than the discarded draft

#### Scenario: Opening with an empty username
- **GIVEN** the signed-in gambler has an empty stored username
- **WHEN** the Username screen becomes interactive
- **THEN** the empty field is focused with a collapsed caret at position `0`

#### Scenario: Preserving the gambler's selection after initial focus
- **GIVEN** the Username screen has completed its initial focus and caret placement
- **AND** the gambler moves the caret or selects part of the username
- **WHEN** the draft, validation, preview, or save lifecycle causes the screen to update
- **THEN** the editor does not request focus again or move or replace the gambler's selection

#### Scenario: Previewing a changed username with the production row
- **GIVEN** the username editor is open
- **WHEN** the gambler changes the draft username
- **THEN** the existing `GamblerScoreItem` immediately displays the trimmed draft with the signed-in account avatar, the current-user marker, and the illustrative rank and score

#### Scenario: Always displaying the illustrative rank component
- **GIVEN** the username editor is open at a supported viewport width and text size
- **WHEN** the pool preview renders in any draft or save-lifecycle state
- **THEN** `GamblerScoreItem` receives `position = 1` and `beforePosition = 2`
- **AND** its leading rank tile displays `1` with an upward movement indicator of `1`
- **AND** a long username truncates without hiding or displacing the rank rail
- **AND** VoiceOver or TalkBack exposes the localized equivalents of "Rank 1" and "Up 1 place"

#### Scenario: Previewing an empty draft
- **GIVEN** the username editor is open
- **WHEN** the gambler removes every non-whitespace grapheme
- **THEN** the production gambler row displays the localized "Your username" placeholder, the field displays "Enter a username.", and "Save username" remains disabled

#### Scenario: Preview does not load pool data
- **GIVEN** the username editor is displaying its pool preview
- **WHEN** the draft or preview is rendered
- **THEN** no pool repository or remote request is invoked and the illustrative rank, movement, and score are not persisted

#### Scenario: Enforcing the username limit
- **GIVEN** the gambler is editing the username
- **WHEN** the draft reaches 100 graphemes
- **THEN** the counter displays `100/100`, the preview displays the same 100-grapheme value using production truncation behavior, and additional graphemes are not accepted

#### Scenario: Unchanged username cannot be saved
- **GIVEN** the trimmed draft equals the stored username
- **WHEN** the username editor evaluates its actions
- **THEN** "Save username" is disabled and no update request is sent

#### Scenario: Keeping the save action in the content flow
- **GIVEN** the keyboard, a small viewport, localized copy, or large accessibility text pushes content below the fold
- **WHEN** the gambler scrolls through the username editor
- **THEN** "Save username" moves with the content, remains reachable below the field guidance or validation message, and neither overlays nor obscures editor content

#### Scenario: Saving a changed username
- **GIVEN** the trimmed draft is non-empty and differs from the stored username
- **WHEN** the gambler taps "Save username"
- **THEN** the existing username update flow submits only the trimmed username, preserves the visible draft and preview, disables editing and back navigation, and exposes the accessible state "Saving username"
- **AND** on Android, the top-app-bar back button remains visible but disabled, and system back navigation cannot leave the editor

#### Scenario: Retrying a failed save
- **GIVEN** the username could not be saved
- **WHEN** the failure is displayed
- **THEN** the editor preserves the draft, shows an inline message that explains the available recovery, and offers "Try again" for the same pending username
- **AND** on Android, the top-app-bar back button and system back navigation are enabled again

#### Scenario: Leaving the Android editor after a failed save
- **GIVEN** the Android username editor displays a failed save and no retry is in progress
- **WHEN** the gambler taps the top-app-bar back button
- **THEN** the app returns directly to Profile and discards the unsaved draft without submitting another username update or retry
- **AND** Profile continues to show the stored username

#### Scenario: Completing the username update
- **GIVEN** a changed username has been submitted
- **WHEN** the existing update flow succeeds
- **THEN** the editor returns to Profile and the Profile screen and drawer header display the saved username

#### Scenario: Reading the live preview with assistive technology
- **GIVEN** VoiceOver or TalkBack is active
- **WHEN** the gambler navigates to the pool preview after editing the draft
- **THEN** the production row exposes its updated gambler description in preview context without forcing an announcement after every keystroke

#### Scenario: Editing the username from the profile
- **WHEN** the user taps the Username row and saves a new valid username
- **THEN** the username is updated via the existing update flow and the Profile screen shows the new value on return

#### Scenario: Drawer header reflects the change
- **WHEN** the username is saved from the Profile screen
- **THEN** the drawer header shows the updated username, as it does today
