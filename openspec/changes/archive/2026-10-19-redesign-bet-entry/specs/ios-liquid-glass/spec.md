# Spec Delta

## MODIFIED Requirements

### Requirement: Content-layer controls use standard materials

App-rendered Liquid Glass SHALL NOT be used as the background of list rows, match cards, rank tiles, account or pool summaries, previews, text fields, or inline actions embedded in those content areas. Email and password fields, pool-name fields, username fields, and prediction score boxes SHALL use standard theme-aware input styling. Prediction-card steppers, filter chips, and retry controls; pool-row invitation controls; paging-error retry controls within lists; and inline form submit or retry controls SHALL use standard non-glass control styling with their existing semantic emphasis. The material change SHALL preserve visible copy, validation, draft values, selection, focus, submission state, and action routing.

#### Scenario: Editing a prediction within a list
- **GIVEN** an open match card is visible
- **WHEN** the gambler taps a score box, types a score, or taps a stepper
- **THEN** the score boxes and steppers use standard non-glass styling
- **AND** focus movement, numeric input behavior, draft retention during unrelated paging updates, and automatic saving follow the `bet-entry` capability

#### Scenario: Retrying a failed prediction
- **GIVEN** a prediction submission has failed
- **WHEN** the failure banner is displayed
- **THEN** the banner and its Retry control are identifiable and operable without glass backgrounds
- **AND** Retry submits the same scores through the existing retry flow

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
