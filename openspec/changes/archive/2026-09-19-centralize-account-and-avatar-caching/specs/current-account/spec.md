## ADDED Requirements

### Requirement: Persisted current account is the immediate client source of truth
Each authenticated client SHALL maintain one observable current-account value containing the account id, external account id, email, and username. The client SHALL persist that value independently from authentication credentials, SHALL publish the persisted snapshot before waiting for a remote refresh, and SHALL clear both the observable value and its persisted snapshot on logout.

The persisted format MUST accept the legacy account-bundle representation already installed on devices and migrate it without signing the gambler out or losing account fields.

#### Scenario: Launching with a persisted account
- **GIVEN** the gambler is authenticated and a current-account snapshot is persisted
- **WHEN** Fortuna starts
- **THEN** account consumers receive the persisted account before remote reconciliation completes
- **AND** authenticated navigation is not blocked on an account network request

#### Scenario: Migrating a legacy account bundle
- **GIVEN** a released version of Fortuna persisted an account using the legacy bundle format
- **WHEN** the new current-account store opens that value
- **THEN** it publishes the same account id, external account id, email, and username
- **AND** it migrates the value without requiring the gambler to sign in again

#### Scenario: Logging out
- **GIVEN** the current-account store contains a signed-in account
- **WHEN** logout completes
- **THEN** the observable current account becomes absent
- **AND** no persisted account snapshot remains available to a later app launch

### Requirement: Authenticated current-account retrieval
The backend SHALL expose `GET /accounts/me` as an authenticated endpoint that resolves the caller from the authenticated principal and returns the canonical account id, external account id, email, and username. The endpoint MUST NOT accept an account id that lets a caller select another account and MUST NOT expose another gambler's account record.

#### Scenario: Authenticated caller retrieves their account
- **GIVEN** an authenticated principal is linked to an Account record
- **WHEN** the client calls `GET /accounts/me`
- **THEN** the endpoint returns that caller's canonical account fields

#### Scenario: Unauthenticated caller requests the current account
- **GIVEN** the request has no valid authenticated principal
- **WHEN** it calls `GET /accounts/me`
- **THEN** the endpoint rejects the request as unauthorized
- **AND** no account data is returned

#### Scenario: Caller identity cannot select a different account
- **GIVEN** an authenticated principal is linked to one Account record
- **WHEN** it calls `GET /accounts/me`
- **THEN** the response is derived only from that principal
- **AND** request input cannot substitute another account id

### Requirement: Current account reconciles with the server without discarding offline state
The client SHALL refresh the current account after authenticated cold start and whenever an eligible foreground or Profile trigger finds that the last successful validation is outside the configured staleness interval. Concurrent refresh triggers SHALL be coalesced into one remote request.

On success, the client SHALL atomically persist and publish the canonical response and its validation time. On failure, it SHALL retain and continue publishing the existing snapshot, SHALL leave that snapshot eligible for a later retry, and SHALL NOT replace account content with loading, empty, or error placeholder values.

#### Scenario: A username changed on another device
- **GIVEN** the persisted snapshot contains an older username
- **AND** the canonical account contains a newer username
- **WHEN** current-account reconciliation succeeds
- **THEN** the newer canonical username is persisted
- **AND** every active account consumer receives the newer username

#### Scenario: Refresh fails while a snapshot exists
- **GIVEN** the current-account store is publishing a persisted snapshot
- **WHEN** a refresh fails because the server or network is unavailable
- **THEN** the store continues publishing the same snapshot
- **AND** a later eligible trigger can retry reconciliation

#### Scenario: Concurrent refresh triggers
- **GIVEN** the current account is stale
- **WHEN** cold-start, foreground, Profile, or consumer triggers overlap
- **THEN** the client performs one current-account request for that overlap
- **AND** all waiters observe the same result

#### Scenario: Current account is still fresh
- **GIVEN** the last successful account validation is inside the configured staleness interval
- **WHEN** a non-forced eligible refresh trigger occurs
- **THEN** the persisted observable account remains available
- **AND** the client does not start another current-account request

### Requirement: Account mutations and refresh results are ordered
Sign-in installation, current-account refresh, username mutation, and logout clearing SHALL pass through one application-scoped coordinator on each platform. The coordinator SHALL serialize state changes so an older refresh result cannot overwrite a newer successful mutation or restore an account after logout.

A successful username mutation SHALL persist and publish the submitted canonical username through the current-account store. A failed mutation SHALL leave the published account snapshot unchanged. Account-consuming views SHALL observe the shared value rather than requiring route-specific callbacks to mirror the username.

#### Scenario: Username save succeeds
- **GIVEN** the current-account store contains the signed-in gambler
- **WHEN** the username PATCH succeeds
- **THEN** the submitted canonical username is persisted and published once
- **AND** Profile, drawers, toolbars, and later username editors observe it from the shared store

#### Scenario: Username save fails
- **GIVEN** the current-account store contains the previous username
- **WHEN** the username PATCH fails
- **THEN** the current-account store retains the previous username
- **AND** no account consumer observes the failed value as saved account state

#### Scenario: Refresh began before a successful username save
- **GIVEN** a refresh for an older account snapshot began before a username mutation
- **WHEN** the username mutation succeeds before that refresh attempts to publish
- **THEN** the older refresh result does not replace the newer stored username

#### Scenario: Logout races with account work
- **GIVEN** refresh or mutation work is in flight
- **WHEN** logout clears the current account
- **THEN** a later result from that work does not restore the cleared account

