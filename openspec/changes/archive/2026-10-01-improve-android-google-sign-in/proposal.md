# Proposal

## Why

Fortuna's Android Google sign-in can show an error while Google account setup continues, then require a second tap to sign into the app. Fix the lifecycle and outcome handling of the existing Google-button flow so adding or selecting an account completes the original sign-in attempt.

## What Changes

- Preserve the active request while Google account setup, consent, or verification temporarily backgrounds Fortuna.
- Track the attempt from the first tap through the existing app-authentication result, preventing duplicate requests and repeated navigation.
- Treat explicit Google cancellation as a normal return to idle, propagate coroutine cancellation correctly, and distinguish missing credentials from malformed credentials.
- End loading and show appropriate existing failure feedback when credential acquisition or app authentication fails; a new attempt remains a deliberate user action.
- Verify zero, one, and multiple device accounts, reauthentication, cancellation, repeated taps, lifecycle transitions, provider/network failures, and a clean manual restart after process death.

## Capabilities

### New Capabilities

- `android-google-sign-in`: Correct lifecycle, progress, cancellation, and completion behavior for the existing Android Google-button sign-in flow.

### Modified Capabilities

None.

## Impact

- Android `account` Google credential provider, ViewModel, and existing sign-in UI bindings; limited changes in the affected `session` authentication chain only where needed for cancellation or failure propagation.
- Reuse the current Google-button request, Google-owned presentation, app error/progress components, and existing authentication/account-installation pipeline.
- This change does not add a bottom-sheet entry point, automatic sign-in prompt, recovery journal, resumable account-provisioning flow, storage redesign, backend changes, or logout redesign. iOS, icon assets, and loading-placeholder layouts are outside scope.
