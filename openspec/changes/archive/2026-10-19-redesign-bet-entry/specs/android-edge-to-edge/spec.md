# Spec Delta

## MODIFIED Requirements

### Requirement: Text entry remains usable with the keyboard open

All Android text-entry flows, including email sign-in, email-and-password sign-in, pool naming, username editing, and prediction score entry, SHALL keep the focused field visible above the software keyboard. Related validation and submission actions SHALL remain reachable, using scrolling when space is limited. Keyboard transitions SHALL neither pan the whole screen's top navigation out of view nor add duplicate bottom spacing. Closing the keyboard SHALL restore normal content bounds without losing the draft or resetting the gambler's selection.

#### Scenario: Completing a form in a short window
- **GIVEN** a text-entry flow is open in landscape, a short window, or with large accessibility text
- **WHEN** the gambler focuses each field and opens the keyboard
- **THEN** the focused field is visible above the keyboard
- **AND** validation and submission actions can be reached without dismissing the keyboard
- **AND** top navigation remains visible

#### Scenario: Editing a score near the list end
- **GIVEN** the Bets tab includes an open match card near the bottom of the list
- **WHEN** the gambler taps one of its score boxes
- **THEN** the whole card, including both score boxes and their steppers, is brought into the visible area above the keyboard
- **AND** when focus moves from the home box to the away box the card stays visible
- **AND** a failure banner's Retry control on that card is reachable without being covered by the keyboard or pool tabs

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
