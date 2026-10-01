# Design

## Context

See `proposal.md` for the problem and scope.

The existing credential provider already uses `GetSignInWithGoogleOption`, which supports adding a device account and reauthentication. `GoogleSignInViewModel` cancels its job on process `onStop`, while the provider's generic exception handler can turn coroutine cancellation into a failure. Google account setup can background Fortuna, so this is the leading explanation for the reported false error and second tap; verify that lifecycle sequence on a device.

Busy state currently begins after Google returns a token. Repeated taps during credential acquisition can therefore cancel and restart the request. The successful app path is already Google token -> Firebase authentication -> existing account linking -> current-account installation -> navigation.

## Goals / Non-Goals

**Goals:** Fix continuation of the existing button flow, keep one active request, distinguish cancellation from failure, release busy state reliably, and deliver one successful navigation after the existing authentication use case completes.

**Non-Goals:** New native-sheet presentation, automatic prompts, custom Google UI, new recovery screens or durable metadata, split/resumable provisioning, backend or storage changes, global authentication ownership, logout redesign, or changes to iOS and existing email flows.

## Decisions

### 1. Keep the existing request and presentation

Continue to launch `GetSignInWithGoogleOption` from the current Google button. Do not introduce `GetGoogleIdOption`, an automatic initial prompt, a custom sheet, or an authorized-account filtering fallback. Preserve existing Google assets and app-owned error/progress components.

Changing presentation does not address the interrupted request and is outside this flow fix.

### 2. Keep the request alive through external Google activities

Remove unconditional sign-in cancellation from `ProcessLifecycleOwner.onStop`. Temporary backgrounding during account addition, consent, verification, or ordinary app switching does not mean the user canceled authentication.

Keep work scoped to the existing sign-in ViewModel/screen owner. Deliberate departure or destruction of that owner still cancels its work. Configuration recreation that retains the owner must not issue a new request. Use an appropriate activity context for the current Credential Manager integration, and verify provider behavior across recreation; an actual interrupted request settles into deliberate retry instead of an automatic prompt loop.

Do not add an `onStart`/resume retry. Keeping the original request alive is the intended fix. If the provider itself terminates it, classify that result separately.

### 3. Make the whole existing attempt single-flight

Enter busy state before acquiring a Google credential. If an attempt is active, ignore additional Google-button actions rather than canceling and restarting it. Clear a prior failure when the user starts a new attempt, and use the existing screen controls to prevent competing authentication actions while busy.

Continue through the existing `SignInWithGoogle.execute` call after token acquisition. Publish completion and navigate once for the still-active screen only when that call succeeds. A request identity or equivalent ViewModel-local ownership check can prevent superseded results from updating the screen; this does not introduce an application-wide session coordinator.

On user cancellation, return to idle. On failure, publish a terminal failure with busy state released. On actual task cancellation, avoid user-facing failure and do not overwrite a newer state from an old cleanup callback. Leaving the screen must prevent a late UI update or navigation.

### 4. Classify failures without changing account provisioning

Rethrow coroutine `CancellationException` before generic catches in the credential provider and affected existing authentication chain. Handle option/client-id/request construction failures within the same failure boundary.

- `GetCredentialCancellationException`: silent idle, no automatic retry.
- `NoCredentialException`: a recoverable missing-credential outcome, not a malformed-token diagnosis.
- Provider interruption, unavailable/unsupported services, or configuration failure: existing localized error presentation and a deliberate retry or other existing sign-in method.
- Unexpected credential type/parsing failure: credential failure, with no app authentication using that result.
- Firebase, existing account-linking, or account-installation failure: show the existing appropriate failure, stop loading, and allow a new button attempt. Ensure thrown failures as well as `Result.failure` cannot leave the screen loading.

Do not add special backend-completion retries, change the account-linking contract, or alter persistence semantics. Normal backend completion failure may require a fresh Google-button attempt; the no-second-tap guarantee concerns a successful Google flow that previously got canceled by backgrounding.

### 5. Use existing startup behavior after process death

An in-memory credential request cannot be promised to survive process death. Use the existing startup/session logic: restore an already completed app session normally; otherwise leave the sign-in screen available for a fresh deliberate Google-button attempt. Do not persist the request, automatically replay Google UI, or infer app sign-in success merely because a Google account was added to the device.

## Risks / Trade-offs

- The suspected lifecycle sequence has not been reproduced in this investigation -> reproduce zero-account setup and verify the original request reaches the existing app-authentication call after return.
- Credential Manager behavior varies with device and Google services -> cover zero/one/multiple accounts and recreation using representative supported devices plus controllable provider tests.
- The Firebase SDK operation can outlive cancellation of its coroutine -> preserve coroutine cancellation and prevent stale screen updates/navigation; broader authentication/logout coordination is outside this change.
- Process death or an actual provider interruption can require a fresh user action -> expose a clean retry instead of promising automatic continuation.
- Shared exception helpers can affect email authentication -> limit edits to the affected flow and run relevant existing regression tests.

## Validation Strategy

Add focused tests with a fake credential provider and controllable authentication result for background/return, repeated taps, silent user cancellation, propagated coroutine cancellation, missing credentials, interruptions, construction/parsing failures, loading cleanup, app-authentication failure, and one navigation result. Check that the existing use case is called once with the returned token and that device-account creation alone never triggers success.

Verify on Android: no account -> add account -> Fortuna sign-in in the same attempt; one/multiple accounts; choose another account; consent and reauthentication/verification; cancel/back at each provider stage; repeat taps; background/return and rotation; network/provider failures; process death followed by manual retry; and existing sign-out/re-sign-in behavior as a regression check. Record unavailable device cases instead of claiming them tested.

## References

- [Google button flow and supported account scenarios](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation)
- [Credential Manager cancellation and error handling](https://developer.android.com/identity/sign-in/credential-manager-troubleshooting-guide)
