# Verification

## Android — tasks 1.1–1.2 (2026-10-01)

### Environment

- Branch `hotfix/android-google-sign-in`, uncommitted working tree.
- Emulator: new AVD `Pixel_7_API_36_no_account` (`pixel_7` profile, `system-images;android-36;google_apis_playstore;arm64-v8a`, Play Store enabled, host keyboard enabled), serial `emulator-5554`. `dumpsys account` reported `Accounts: 0`; `com.google.android.gms` and `com.android.vending` installed.
- The attached physical phone was not used; every Gradle and adb command set `ANDROID_SERIAL=emulator-5554` (or `none` for host-only builds).

### Changes covered

- `GoogleSignInViewModel` no longer observes `ProcessLifecycleOwner`; the attempt lives in the view-model scope and is canceled only when the owning screen is destroyed. A canceled attempt publishes nothing.
- New `GoogleSignInState` (Idle, InProgress, Authenticated, Completed, Failed). The attempt is busy from the first tap; repeated actions while busy are ignored; a new attempt replaces an earlier failure; an authenticated account is handed to navigation once and then marked Completed.
- The welcome screen (`HomeScreen`) shows the existing loading overlay and disables the Google, Email, and Email-and-password actions while the attempt is busy.
- `lifecycle-process` moved from `:account` implementation to `:app` androidTest only.

### Automated checks

| Command | Result |
| --- | --- |
| `./gradlew :account:testDebugUnitTest :app:testProdDebugUnitTest :session:testDebugUnitTest :app:assembleProdDebug :app:connectedProdDebugAndroidTest --continue` | BUILD SUCCESSFUL. Unit tests: account 65/65 (including 13 in `GoogleSignInViewModelTest`), app 60/60, session 23/23. Connected app tests 25/25 on `emulator-5554`, including 6 in `HomeGoogleSignInAttemptTest`. |
| `./gradlew :account:connectedDebugAndroidTest --continue` | BUILD SUCCESSFUL, 10/10 on `emulator-5554`. |
| Regression check: `HomeGoogleSignInAttemptTest#backgroundingWhileGoogleIsOpenKeepsTheOriginalRequestThroughAppAuthentication` with the old process-`onStop` cancellation temporarily restored | Failed as expected (`expected:<0> but was:<1>` canceled requests); the temporary change was then removed and the suite passed again. |

`HomeGoogleSignInAttemptTest` covers: background (process lifecycle reaches CREATED) and return keeps the original request and reaches `SignInWithGoogle.execute` once with the returned token, then one navigation; a busy attempt disables every sign-in action and ignores further taps; Google cancellation restores the actions without app authentication; activity recreation while Google is open starts no second request and delivers once; recreation after success does not navigate again; destroying the screen cancels the request and ignores a late token.

### Device check — zero-account reproduction

Setup: prodDebug (`com.felipearpa.fortuna`, production backend) installed on `emulator-5554` with `Accounts: 0`; logcat buffers enlarged to 16 MiB and cleared; Fortuna launched to the welcome screen (process 9213).

Gambler's action and observation (reported by the user): tapped the Google button once, added their existing Google account in Google's screens, saw no error dialog, and did not tap a second time.

Evidence (adb only; times are emulator local; account identifiers, tokens, and response bodies omitted):

| Check | Source | Result |
| --- | --- | --- |
| One credential request | system `CredentialManager` log; `wm_create_activity` events | One `executeGetCredential` from `com.felipearpa.fortuna` at 13:07:03.353 and one `onFinalResponseReceived` at 13:09:37.905 (`propagateCancellation false`). One `CredentialChooserActivity` and one `GoogleSignInActivity` were created; account addition ran through `AccountIntroActivity`, `PreAddAccountActivity`, `MinuteMaidActivity`, and `GoogleServicesActivity`. |
| Fortuna backgrounded, then resumed without process death or recreation | `events` buffer for activity record 57633934 | `wm_on_paused_called` 13:07:03.579, `wm_on_stop_called` 13:07:10.138, `wm_on_restart_called` 13:09:33.224, `wm_on_resume_called` 13:09:38.144, all in process 9213. Fortuna stayed stopped for about 2 min 23 s, far longer than the process lifecycle's background delay. One `am_proc_start` for Fortuna (12:52:31, pid 9213), no `am_proc_died` or `am_kill` for it, and no second `wm_on_create_called` for MainActivity. |
| The original request reached the existing app authentication | `Ktor Client` log, REQUEST/RESPONSE/FROM/METHOD lines only | `REQUEST: https://tyche-api.felipearpa.com/accounts` `METHOD: POST` at 13:09:40.819, `RESPONSE: 200` `FROM: …/accounts` at 13:09:44.136, exactly one such request. This account-link call runs only inside `SignInWithGoogle.execute`, after Firebase accepts the Google token. Then `GET …/gamblers/<gamblerId>/pools` and `GET …/pool-layouts/open` both returned 200. |
| Navigation to the pools list | `dumpsys activity activities`; screenshot viewed outside the repository | `topResumedActivity` is `com.felipearpa.fortuna/com.felipearpa.tyche.MainActivity`; the screen shows "My pools" with the gambler's pools. |
| No crash | `crash` buffer; `AndroidRuntime`/`FATAL` lines in the main buffer | Crash buffer empty; the only `AndroidRuntime` lines belong to the `monkey` launcher command at 12:52:31. |
| Device account added | `dumpsys account` | One Google account now registered. |

Outcome: from zero device accounts, a single Google-button tap completed Google account addition and Fortuna sign-in in the same attempt, with no false error and no second tap.

Not covered here (task 3.1): one or several existing accounts, choosing another account, cancel/Back at each Google stage, rotation and repeated taps against the real provider, provider and network failures, and process death followed by a manual retry.

## Android — tasks 2.1–2.2 (2026-10-01)

### Environment

- Branch `hotfix/android-google-sign-in`, uncommitted working tree on top of the tasks 1.1–1.2 changes.
- Host-only JVM tests and builds; every Gradle command set `ANDROID_SERIAL=none`. No device or emulator was used, and Fortuna on `emulator-5554` was not touched.

### Changes covered

- `CredentialManagerGoogleCredentialProvider` builds the request inside its failure boundary, so a web client ID that cannot be read or is empty comes back as a returned failure without asking Google. Coroutine `CancellationException` is rethrown before every other catch. Outcomes: the gambler canceling (`GetCredentialCancellationException`) stays the silent `Cancelled`; `NoCredentialException` is the new `GoogleSignInException.NoCredential` instead of `InvalidCredential`; `GetCredentialInterruptedException` (Play services' mapping for network errors and dropped connections) is `NetworkError`; an unexpected credential type or a `GoogleIdTokenParsingException` is `InvalidCredential`; unsupported, misconfigured, and unknown provider failures keep their original exception and show the generic error. Each call makes one request; nothing retries.
- New localized failure `GoogleSignInLocalizedException.NoCredential` with strings in `values`, `values-es`, and `values-es-rES` of `:account`.
- `handleFirebaseSignInWithGoogle` rethrows coroutine cancellation. The email helpers in the same file and the shared `KtorExceptionHandler` are unchanged.
- `SignInWithGoogle` keeps its pipeline (Firebase, account link, `CurrentAccountCoordinator.install`). Because the shared network handler returns cancellation as a failure, a returned `CancellationException` from the account link is rethrown when the caller is actually canceled and otherwise stays a returned failure. The class documents that installation failures are thrown.
- `GoogleSignInViewModel` turns thrown failures from the credential provider and from `SignInWithGoogle.execute` into `Failed`, so loading always ends. Cancellation of the attempt itself still publishes nothing; a cancellation exception that arrives while the attempt is active is treated as a failure, not as a silent end with the screen busy. The class comment documents the lifecycle and cancellation boundary.
- `:account` JVM tests now use `unitTests.isReturnDefaultValues = true`, because Credential Manager and Google ID types build `Bundle`s in their constructors.

### Automated checks

| Command | Result |
| --- | --- |
| `./gradlew :account:testDebugUnitTest :session:testDebugUnitTest :app:testProdDebugUnitTest :app:assembleProdDebug --continue --rerun-tasks --no-build-cache` | BUILD SUCCESSFUL. account 85/85 (adds 12 in `CredentialManagerGoogleCredentialProviderTest` and 8 in `GoogleSignInViewModelTest`, now 21), session 32/32 (adds 6 in `SignInWithGoogleTest` and 3 in `FirebaseSignInWithGoogleExceptionHandlerTest`), app 60/60. |
| `./gradlew :app:compileProdDebugAndroidTestKotlin :account:compileDebugAndroidTestKotlin` | BUILD SUCCESSFUL; `HomeGoogleSignInAttemptTest` still compiles against the unchanged `GoogleCredentialProvider` interface. |
| Regression check: provider cancellation rethrow and the view model's handling of a thrown `execute` temporarily removed | 4 failures as expected: provider cancellation is rethrown; installation throws; retry after a thrown failure; foreign cancellation during app authentication. Both changes were restored and the suites passed again. |

`CredentialManagerGoogleCredentialProviderTest` covers: the token from one `GetSignInWithGoogleOption` request with the configured client ID; silent cancellation; no credential distinct from a malformed one; interruption as a network failure; unsupported, misconfigured, and unknown provider failures returned unchanged; another credential type and an unparsable Google credential as invalid; an unreadable or empty client ID returned as a failure without a request; coroutine cancellation rethrown. Every failure case verifies exactly one request.

`GoogleSignInViewModelTest` additions cover: no credential shown as the missing-credential failure; a generic provider failure shown with the generic error; a thrown provider failure; Firebase and account-link failures returned by the use case; a thrown installation failure; a foreign cancellation during app authentication; and a retry after failure that authenticates the new token once and delivers the account. The existing tests still cover one use-case call with the returned token and success-only navigation.

`SignInWithGoogleTest` covers: Firebase and link success installs and returns the linked account with one call each; a Firebase failure stops before the link; a link failure installs nothing; a storage failure during installation is thrown; caller cancellation during the link is not returned as a failure; a returned cancellation while the caller is active stays a failure.

Email regression: no email-specific unit tests exist in `:account`, `:session`, or `:app`; the email helpers are unchanged and all three modules' suites pass.

### Not run

- Connected tests (`:app:connectedProdDebugAndroidTest`, `:account:connectedDebugAndroidTest`): not rerun, to keep Fortuna's signed-in install on `emulator-5554` for task 3.1. The view-model behavior they exercise is covered by the JVM tests above.
- On-device provider failures (no credential, interruption, network, misconfiguration): task 3.1.

Accepted without the connected-test rerun, on-device provider-failure checks, and email-specific unit tests by the user on 2026-10-01 (covered later by tasks 3.1 and 3.2). The new no-credential strings (en, es, es-ES) were approved as written by the user on 2026-10-01.

## Android — tasks 3.1–3.2 (2026-10-01)

### Environment

- Branch `hotfix/android-google-sign-in`, uncommitted working tree with the tasks 1.1–2.2 changes.
- Emulator `emulator-5554` only: AVD `Pixel_7_API_36_no_account` (API 36, Google Play image, en-US, software rendering). The device now has one Google account, the one added during the task 1.1 check. No Google account was added or removed.
- Fortuna `prodDebug` (`com.felipearpa.fortuna`) against the production backend, using the user's existing Fortuna account. No new backend data beyond the existing account-link call made by each sign-in.
- Every Google-flow, test, and adb command for task 3.1 and the other task 3.2 checks used `emulator-5554` (`ANDROID_SERIAL=none` for host-only Gradle). The physical phone was used only for the email sign-in regression, with the user's permission (see the end of this section).
- Evidence came from adb only: the system `CredentialManager` log (`executeGetCredential`, `onFinalResponseReceived`, `finishing session with propagateCancellation …`), the `events` buffer (activity lifecycle, process start and kill), `Ktor Client` REQUEST/RESPONSE/FROM/METHOD lines with ids replaced by `<id>`, uiautomator dumps (text, bounds, enabled), and screenshots kept outside the repository. Times are emulator local time.
- The emulator was intermittently overloaded (load average 20–70; Google's post-account restore service, `artd` compilation, and software rendering). That affected two observations, noted below.

### Automated checks (task 3.2)

| Command | Result |
| --- | --- |
| `ANDROID_SERIAL=none ./gradlew :account:testDebugUnitTest --rerun :session:testDebugUnitTest --rerun :app:testProdDebugUnitTest --rerun :app:assembleProdDebug --continue` | BUILD SUCCESSFUL. account 85/85, session 32/32, app 60/60, 0 skipped. `app-prod-debug.apk` built. |
| `ANDROID_SERIAL=emulator-5554 ./gradlew :account:connectedDebugAndroidTest :session:connectedDebugAndroidTest :app:connectedProdDebugAndroidTest --continue` | BUILD FAILED, 4 failures, none in sign-in code. `:account` passed 10/10, and `HomeGoogleSignInAttemptTest` passed 6/6 within `:app`. `:session`: `ExampleInstrumentedTest.useAppContext` failed (`expected:<com.felipearpa.[data.user].test> but was:<com.felipearpa.[tyche.session].test>`); this is the known stale template test. `:app` 22/25: `PoolHomePendingBetKeyboardTest.focusingAScoreNearTheEndRevealsItAboveTheKeyboardWithoutStackingTheTabBar` ("gap above the keyboard (266.0) must not include the tab bar (211.0)") and `UsernameEditorKeyboardTest.saveStaysBelowTheFieldGuidanceAndScrollsWithTheContent` / `withTheKeyboardOpenTheBackButtonStaysVisibleAndSaveCanBeScrolledAboveIt` failed. |
| Same two keyboard classes rerun alone on the working tree (`-Pandroid.testInstrumentationRunnerArguments.class=…PoolHomePendingBetKeyboardTest,…UsernameEditorKeyboardTest`) | Same 3 failures with the same messages. |
| Same two classes run from a clean `git archive HEAD` export of `Android/` (outside the repository) on the same emulator | Same 3 failures with the same messages, so they are not caused by this change. They passed on this AVD in the task 1.1 run, before the Google account and its restore and keyboard state existed; they depend on keyboard height and this change does not touch those screens. |

After the connected run, the working-tree `prodDebug` APK was reinstalled with `adb install -r`.

### Scenario matrix (task 3.1, plus regressions for task 3.2)

| Scenario | How | Outcome | Status |
| --- | --- | --- | --- |
| Zero accounts, account added, first sign-in | Task 1.1 device check above (13:07–13:09) | One request, one Google-button tap; Fortuna was stopped about 2 min 23 s in the same process; one `POST /accounts` 200; "My pools"; no error and no second tap. | Verified (task 1.1 evidence) |
| One existing account selected | Chooser "Choose an account … to continue to Fortuna" listed the single account and "Add another account"; selected the account (13:51:41, 13:55:09) | Fortuna showed the existing loading overlay with Google, Email, and Email-and-password disabled; one `POST /accounts` → 200, then `GET /gamblers/<id>/pools` and `GET /pool-layouts/open` 200; "My pools". | Verified |
| Multiple accounts on the device | Requires adding a second Google account (declined) | Not run | Unverified |
| Choose another account, or add another account and sign in with it | Requires adding a Google account (declined) | Not run | Unverified |
| Consent screen | Task 1.1 ran account addition through `GoogleServicesActivity` and backup opt-in; no separate app-consent screen appeared in later runs with the existing account | Only what task 1.1 recorded | Unverified beyond task 1.1 |
| Reauthentication or password / two-step verification | Google never asked for it; entering credentials is not permitted in this environment | Not run | Unverified (needs the user) |
| Repeated taps before Google returns | Three `input tap`s on the Google button in one shell call (13:44:32) | One `executeGetCredential`, one `CredentialChooserActivity`, one `GoogleSignInActivity`. | Verified |
| Cancel with Back on the account chooser | Back on the chooser (13:44:45) | `finishing session with propagateCancellation false`; welcome screen with every action enabled, no dialog; no new request in the following 5 s. | Verified |
| Cancel or Back at Google's add-account stage | Tapped "Add another account" (about 13:58) | Google's sheet stayed collapsed and unresponsive for about 16 min (no add-account activity was created). Back twice, a tap outside the sheet, and a tap on the account had no visible effect. Fortuna correctly stayed busy because the request was still open. A second attempt at this step was not permitted by the session's permission policy. | Unverified (provider UI stalled on this emulator) |
| Background and return via Recents | Chooser open → Home (Fortuna `onStop`) → 10 s → Recents → Fortuna card (13:50:15 request) | The same chooser returned; still one request; later completed sign-in (row "One existing account"). | Verified |
| Background and return via the launcher icon | Chooser open → Home → launcher icon (13:55:53 request) | The same chooser returned on top of Fortuna's task, with no new request. | Verified |
| Background and return under heavy load | First Home/launcher run (13:45:14 request, load average about 70) | Chooser was gone on return: `finishing session with propagateCancellation false` at 13:49:57. Fortuna resumed in the same process without recreation, idle, no error, no second request. The provider ended the request silently; the cause could not be isolated under that load, and the run above at normal load kept the request. | Observed; not reproduced |
| Rotation while Google is open | `accelerometer_rotation 0`, `user_rotation 1`, then `0`, with the chooser open | `MainActivity` destroyed and recreated twice in the same process (13:51:15, 13:51:24); the chooser stayed; still one `executeGetCredential`; selecting the account then completed sign-in once. Rotation settings restored to `accelerometer_rotation 1`, `user_rotation 0`. | Verified |
| Network failure during credential acquisition | `svc wifi disable`, `svc data disable` (ping offline), Google button, selected the account (13:52:58) | Google's token call failed (`Auth.Api.Credentials` Cronet `onError`); Fortuna showed the existing "Network error" dialog: "We couldn't reach the sign-in service", "Check your connection and try again", Done. After Done: idle, actions enabled, Google UI not reopened, no new request in 10 s. Network re-enabled (`wifi_on 1`, `mobile_data 1`). | Verified |
| Manual retry after the network is restored | Google button → account (13:54:59) | One request → one `POST /accounts` 200 → "My pools". | Verified |
| Network or backend failure after Google returns (Firebase, account link, installation) | Can't be timed reliably with adb, because the token fetch and the app call follow each other immediately | Covered by `GoogleSignInViewModelTest` and `SignInWithGoogleTest` (tasks 2.1–2.2) | Unverified on device |
| Provider unavailable, misconfigured, no credential, or malformed credential | Not producible on this emulator without changing Google services or the build | Covered by `CredentialManagerGoogleCredentialProviderTest` | Unverified on device |
| Process death while Google is open (system kill), then manual retry | Chooser open → Home → `am kill com.felipearpa.fortuna` (13:54:12, `kill background`) → Recents | The system ended the session (`propagateCancellation true`). Google's stale chooser stayed on top and Fortuna restarted beneath it (new process 13:54:22). Selecting the account closed the chooser: Fortuna showed the welcome screen with every action enabled, no loading, no `POST /accounts`, and no new request. A manual Google-button tap then signed in once (13:54:59 → `POST /accounts` 200 → "My pools"). | Verified |
| Process death by force-stop while Google is open | After the stalled add-account sheet: `am force-stop` (14:14:23) | Session ended (`propagateCancellation true`). Google's stalled activity remained on Fortuna's old task until it was removed with `am stack remove 208`. The launcher then opened Fortuna in a new task on the welcome screen, idle, with no automatic Google request. | Verified |
| Completed-session startup (task 3.2) | Signed in → `am force-stop` → launcher (13:24:47, new process) | "My pools" with the gambler's pools; `GET /accounts/<id>`, `GET /gamblers/<id>/pools`, `GET /pool-layouts/open` all 200; no Google activity created. | Verified |
| Sign-out and Google re-sign-in (task 3.2) | Menu → Log out (welcome screen, idle) → Google button → existing account | Re-sign-in was the "One existing account" row (and again after process death): one request, one `POST /accounts` 200, "My pools". | Verified |
| Existing email sign-in (task 3.2) | On the physical phone, not the emulator; see "Email sign-in regression on the phone" below | The user signed in with the email link and saw no error. The link opened `MainActivity`, then one `POST /accounts` 200 and the pools calls 200; "Mis pollas" was shown. | Verified (phone) |

### Environment observations not attributed to this change

- After the connected tests uninstalled Fortuna and it was reinstalled, the first launch briefly showed "My pools". The system then killed that process (`am_kill … start timeout` at 13:37:27) while Google's restore service was busy. The next launch hit an ANR (`Input dispatching timed out`, main thread waiting in `HardwareRenderer.setStopped`, load average about 48). After a force-stop, Fortuna opened signed out. This happened before any Google sign-in action and involves no code from this change.

### Scope confirmation (task 3.2)

`git diff HEAD --stat` lists 14 modified files, all under `Android/` (`account`, `app`, `session`); the untracked files are the new state class, tests, and this change folder. No backend, iOS, or icon files changed. The Google request is still `GetSignInWithGoogleOption` from the existing button; no `GetGoogleIdOption`, bottom-sheet entry point, or resume/start-triggered sign-in was added. The only new `LaunchedEffect` delivers an `Authenticated` account once. The UI changes reuse the existing loading overlay and error dialog. No persistence or recovery flow was added. On the device, Google UI never opened without a tap: not after cancel, failure, process death, restart, or completed-session startup.

### Email sign-in regression on the phone (task 3.2)

- Device: Samsung SM-G955F (`ce0417143a6ab8130c`), Android 9, locale es-CO. Production backend, the user's existing Fortuna account.
- Build: the working-tree `prodDebug` APK installed with `adb -s ce0417143a6ab8130c install -r -t …/app-prod-debug.apk` at 14:28:30 phone time (`-t` because the debug APK is test-only). The SHA-256 of the local APK and of the installed `base.apk` both equal `ce6943f7acb659254bed9bcfea71073abc5d80c4f5ee173934a7fab0005c1d65`. The first install date (2026-05-02), signing certificate, and data directory were unchanged, so app data was kept.
- Starting state: Fortuna was already signed out on the phone when the check started. Its stored-session files (`shared_prefs/account.xml` and the Firebase Auth store; names and modification times only were read) were last changed at 11:04 that day, before the 14:28 install. The cause is left to the user. Because of this, the user did not need to tap "Cerrar sesión".
- Gambler's action and observation (reported by the user): from the welcome screen, tapped "Correo electrónico", entered their email address, opened the email on the phone, tapped the link, and saw no error.
- Evidence (adb only; a redacted live capture plus the phone's log buffer, both filtered to drop authorization and token lines and with ids replaced by `<id>`; the capture file was deleted afterwards):

| Check | Source | Result |
| --- | --- | --- |
| The link opened Fortuna | `events` buffer | Gmail, then a Chrome custom tab for the link redirect, then `am_create_activity … com.felipearpa.fortuna/com.felipearpa.tyche.MainActivity, android.intent.action.VIEW, https://tyche-588ce.web.app/…` at 14:32:26, in Fortuna's existing process; no process restart. |
| Existing account link and data load | `Ktor Client` REQUEST/RESPONSE/FROM/METHOD lines | `POST https://tyche-api.felipearpa.com/accounts` at 14:32:29 → `RESPONSE: 200` at 14:32:31, exactly one; then `GET …/gamblers/<id>/pools` → 200 and `GET …/pool-layouts/open` → 200 at 14:32:38–39. |
| No crash or ANR | `crash` buffer; capture lines for `am_crash`, `am_anr`, `FATAL`, `E/AndroidRuntime` | Crash buffer empty; none in the capture. |
| No Google sign-in UI | Capture lines for Google sign-in and credential activities | None. |
| Pools list shown | `dumpsys activity activities`; uiautomator dump | `mResumedActivity` is Fortuna's `MainActivity`; the screen shows "Mis pollas" with the gambler's pools and "Crear polla". |

Outcome: the existing email-link sign-in still works with this change. The phone ends signed in through email. After the check nothing further was done on the phone.
