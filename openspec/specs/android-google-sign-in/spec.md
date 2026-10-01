# android-google-sign-in Specification

## Purpose
Make the existing Android Google-button sign-in flow continue correctly through external account setup and finish with consistent progress, cancellation, failure, and app-authentication results.

## Requirements

### Requirement: External Google screens preserve the original sign-in attempt

The Android client SHALL preserve the current Google-button sign-in attempt while Google-owned account selection, account addition, consent, or verification temporarily backgrounds Fortuna. After Google returns a valid credential, the client SHALL continue the existing app-authentication flow without requiring another Google-button tap solely because an external Google screen was opened.

The client SHALL retain the existing user-initiated Google entry point and provider-owned presentation. This flow fix SHALL NOT introduce an automatic Google prompt or a new bottom-sheet entry point.

#### Scenario: First Google account is added

- **GIVEN** no Google account is registered on the device and the Fortuna process remains alive
- **WHEN** the gambler taps Google sign-in and successfully completes Google account setup and consent
- **THEN** Fortuna continues app authentication from the original request
- **AND** temporary backgrounding does not produce a false error or require a second tap

#### Scenario: Existing account is selected

- **GIVEN** one or multiple Google accounts are available on the device
- **WHEN** the gambler selects an account through the existing Google-button flow
- **THEN** Fortuna continues authentication using the credential returned for that account

#### Scenario: Another account or verification is required

- **GIVEN** the gambler uses the Google flow to add another account or complete required consent, password entry, or two-step verification
- **WHEN** Google returns a successful credential result
- **THEN** Fortuna continues the original attempt without a second Google-button tap

### Requirement: One active attempt controls progress and navigation

The client SHALL mark Google sign-in busy before requesting credentials and SHALL allow only one active attempt through the existing app-authentication result. Repeated actions SHALL NOT cancel and restart the in-flight request. A successful attempt SHALL cause at most one navigation transition; abandoned or superseded results SHALL NOT update a departed screen or navigate from it.

Temporary backgrounding or configuration recreation SHALL NOT start a duplicate request. Every completed, canceled, or failed attempt SHALL leave an appropriate terminal state rather than indefinite loading.

#### Scenario: Repeated taps occur before Google returns

- **GIVEN** an attempt is waiting for Google credentials
- **WHEN** additional Google-button actions occur
- **THEN** only the original request remains active
- **AND** no competing authentication action starts

#### Scenario: App returns from the background

- **GIVEN** an active request has not been canceled by the gambler or terminated by the provider
- **WHEN** Fortuna returns to the foreground
- **THEN** it continues observing the existing request without launching another one

#### Scenario: Activity is recreated

- **GIVEN** a sign-in request is active
- **WHEN** a configuration change recreates the activity
- **THEN** Fortuna does not launch duplicate Google UI
- **AND** it handles one retained result or reports an actual interruption with deliberate retry available

#### Scenario: User leaves the sign-in screen

- **GIVEN** an attempt is active
- **WHEN** the gambler deliberately leaves the sign-in flow and an old result subsequently arrives
- **THEN** the old result does not update that departed screen or trigger authentication navigation

### Requirement: Cancellation and acquisition failures are handled distinctly

Explicit Google cancellation SHALL return the sign-in screen to idle without an error or automatic retry. Actual task cancellation SHALL remain cancellation rather than becoming a generic sign-in failure. Missing credentials SHALL NOT be classified as malformed credentials.

Provider interruption, unavailable services, configuration failure, malformed credential responses, and network failure SHALL end busy state and offer appropriate existing error feedback and a deliberate next action. Failures SHALL NOT crash the flow or automatically reopen Google UI.

#### Scenario: User cancels or declines consent

- **GIVEN** the Google account or authentication screen is open
- **WHEN** the gambler cancels, presses Back, or declines consent
- **THEN** Fortuna returns to idle without an error
- **AND** another Google request requires a new user action

#### Scenario: Task is canceled

- **GIVEN** the sign-in task is canceled because its owner is destroyed or its flow is deliberately abandoned
- **WHEN** cancellation reaches credential acquisition or the affected authentication chain
- **THEN** the client does not convert it to a generic sign-in error
- **AND** cleanup does not overwrite a newer attempt's state

#### Scenario: No credential is returned

- **GIVEN** the explicit Google request ends without an available credential
- **WHEN** Fortuna handles that result
- **THEN** it ends busy state and presents recoverable missing-credential feedback
- **AND** it does not report a malformed credential or automatically repeat the request

#### Scenario: Provider or network fails

- **GIVEN** an explicit Google attempt is active
- **WHEN** credential acquisition is interrupted or fails because of network, unavailable Google services, or provider configuration
- **THEN** Fortuna releases busy state and presents appropriate existing failure feedback
- **AND** a deliberate retry or an existing email sign-in method remains available

#### Scenario: Request construction or credential parsing fails

- **GIVEN** Fortuna cannot construct the request or receives an unsupported or malformed credential
- **WHEN** the failure is handled
- **THEN** the flow reports an appropriate terminal failure without crashing
- **AND** it does not authenticate using that invalid result

### Requirement: Success follows the existing app-authentication result

A returned Google credential SHALL enter the existing app-authentication and account-installation pipeline once. Fortuna SHALL report success and navigate only when that pipeline returns a successful app account. Adding a Google account to Android alone SHALL NOT be treated as app sign-in success.

An app-authentication failure, including a returned or thrown failure, SHALL release busy state and allow a fresh deliberate sign-in attempt through the existing entry point. This requirement SHALL NOT introduce a separate account-provisioning retry or change backend or persistence behavior.

#### Scenario: Existing authentication pipeline succeeds

- **GIVEN** Google returned a valid credential for the current attempt
- **WHEN** the existing app-authentication pipeline returns a successfully installed app account
- **THEN** Fortuna handles success once and navigates once

#### Scenario: App authentication fails after Google succeeds

- **GIVEN** Google returned a valid credential
- **WHEN** Firebase authentication, existing account linking, or account installation reports or throws a failure
- **THEN** Fortuna presents the applicable existing failure feedback and ends loading
- **AND** no success navigation occurs
- **AND** the gambler can start a new deliberate attempt

#### Scenario: A prior failure is retried

- **GIVEN** an earlier attempt ended in failure
- **WHEN** the gambler taps Google sign-in again
- **THEN** the old failure is cleared and one new attempt enters busy state

### Requirement: Process death permits a clean manual restart

If the Android process dies during incomplete sign-in, Fortuna SHALL use its existing startup/session behavior. An already completed app session SHALL restore normally; otherwise the sign-in screen SHALL allow a fresh deliberate Google-button attempt. The client SHALL NOT claim successful app sign-in from device-account presence or automatically replay the interrupted Google request.

#### Scenario: Process dies before app sign-in completes

- **GIVEN** the process terminates during Google sign-in and no completed app session exists
- **WHEN** Fortuna is launched again
- **THEN** its existing sign-in screen is available without a stale loading state
- **AND** the gambler can start a fresh attempt manually
- **AND** Google UI is not automatically reopened

#### Scenario: Completed session exists at restart

- **GIVEN** the existing app-authentication flow completed before process termination
- **WHEN** Fortuna restarts
- **THEN** the existing startup/session logic restores that session normally
