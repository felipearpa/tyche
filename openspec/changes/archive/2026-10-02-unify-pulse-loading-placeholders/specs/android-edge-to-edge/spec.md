## MODIFIED Requirements

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
