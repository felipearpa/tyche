# Tasks

Only Android implementation is in scope. Backend, iOS, presentation redesign, persistence redesign, and new recovery flows have no tasks in this change.

## 1. Android — preserve the existing sign-in attempt

- [x] 1.1 Remove unconditional process-background cancellation from Google sign-in while retaining cancellation for deliberate flow departure or owner destruction; add lifecycle tests proving background/return keeps the original request and does not launch another one. Verify the no-account reproduction reaches the existing authentication call after Google returns.
- [x] 1.2 Start busy state before credential acquisition, ignore repeated taps, clear stale failure on a new attempt, and deliver success/navigation once; add ViewModel/UI tests covering repeated actions, competing controls, configuration recreation, terminal state cleanup, and late results after leaving the screen.

## 2. Android — correct cancellation and failure handling

- [x] 2.1 Preserve coroutine cancellation in the credential provider and affected authentication exception boundaries, keep explicit Google cancellation silent, distinguish missing credentials from malformed responses, and cover request construction failures; verify focused tests for cancellation, no credentials, interruption, unsupported/misconfigured services, parsing failure, and network failure without automatic retry.
- [x] 2.2 Keep the existing authentication/account-installation pipeline and handle both returned and thrown failures so loading ends and existing feedback permits another manual attempt; verify tests for one use-case call with the returned token, success-only navigation, Firebase/account-linking/installation failures, and retry after failure. Document the lifecycle/cancellation boundary alongside the changed code.

## 3. Android — verify the complete existing flow

- [x] 3.1 Verify on supported Android devices/emulators: zero accounts with account addition, one/multiple accounts, another account, consent/reauthentication/verification, cancel/back, repeated taps, background/return, rotation, network/provider failures, and process death followed by manual retry; record environment and outcomes in the change's verification notes, explicitly marking unavailable cases. Confirm successful first-account setup completes Fortuna sign-in without a false error or second tap.
- [x] 3.2 Run affected Android tests and an app build, then check existing email sign-in, completed-session startup, and sign-out/re-sign-in as regressions; record exact commands and results and confirm that the fix adds no automatic prompt, presentation redesign, backend change, or new recovery flow.
