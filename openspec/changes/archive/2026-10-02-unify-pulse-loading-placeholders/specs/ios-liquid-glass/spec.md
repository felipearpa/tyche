## MODIFIED Requirements

### Requirement: Affected placeholders retain production presentation

For model-backed loading items affected by this migration, each affected platform SHALL populate its production item or row component with a placeholder model and apply the shared pulse loading-placeholder treatment defined by `loading-placeholders`. Placeholder-specific behavior can suppress remote loading, navigation, and accessibility exposure but SHALL NOT replace the production layout with a separately maintained skeleton. Placeholder values SHALL NOT appear as real user content or become available as meaningful accessibility content or actions.

#### Scenario: Loading My pools after its invite styling changes
- **GIVEN** the initial page or a subsequent page of My pools is loading
- **WHEN** its placeholders are displayed
- **THEN** they use the production pool row populated with a placeholder model and the shared pulse loading-placeholder treatment
- **AND** filler account or pool values do not trigger remote loading, navigation, invitations, or meaningful accessibility exposure
