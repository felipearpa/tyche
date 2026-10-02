## MODIFIED Requirements

### Requirement: Existing data and action states survive the redesign

Drawer presentation SHALL continue using the shared current-account and avatar sources. Opening, closing, or animating SHALL NOT reset loaded data or introduce drawer-specific account or avatar fetches. Pool ownership visibility, deletion confirmation and pending state, invitation, sign-out, and pool-summary error handling SHALL retain their current behavior. During model-backed summary loading, each platform SHALL populate the production summary component with a placeholder model and apply the shared pulse loading-placeholder treatment defined by `loading-placeholders`; placeholder values SHALL NOT be exposed as real user content. Placeholder behavior can suppress remote loading, navigation, and accessibility exposure, but SHALL NOT replace the production layout with a separate skeleton layout.

#### Scenario: Reopening with cached account information
- **GIVEN** the account and avatar are already available from their shared sources
- **WHEN** the drawer closes and reopens
- **THEN** the same account identity and available avatar remain visible
- **AND** the animation does not reset them to a loading state or cause an independent duplicate fetch

#### Scenario: Pool summary is loading or fails
- **GIVEN** pool-summary data is loading or its request fails
- **WHEN** the pool drawer is displayed
- **THEN** loading uses the production summary component populated with a placeholder model and the shared pulse loading-placeholder treatment
- **AND** placeholder values are excluded from meaningful accessibility content and cannot trigger actions
- **AND** a failure presents the existing error behavior without disabling unrelated account or navigation actions

#### Scenario: Owner requests deletion
- **GIVEN** the signed-in gambler owns the current pool
- **WHEN** they activate Delete pool
- **THEN** the existing confirmation is required before deletion starts
- **AND** pending deletion still prevents duplicate deletion requests
