# Verification

## iOS — tasks 1.1–1.5 (2026-10-14)

### Environment

- iOS: iPhone 18 Pro Max simulator, iOS 27.0 (24A434), UDID `D8F12B93-3319-437A-8CE2-59C620CC8A29`, English, 440 × 956 pt. Built with Xcode 27.0 (27A266a) via `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`; the selected `xcode-select` Xcode 26.5 has no iOS 27 runtime.
- Installed Fortuna on the simulator: 2.0.0 (12), signed out. No sign-in was attempted.
- Android (1.1 exploration only): `Pixel_10_Pro_XL` AVD booted through ARTEMIS `mobile_diagnose(launch_avd:)`, serial `emulator-5554`, Android 17 / API 37, 1344 × 2992 px. `adb devices -l` listed no other device. Fortuna `com.felipearpa.fortuna` installed from `:app:assembleProdDebug` (Prod flavor) on the current branch, before any Android change.
- Below-31 Android: not available in this group; not verified.

### 1.1 Inventory and exploration

Production full-screen `LoadingContainerView` callers match the design table, on both platforms:

| Flow | iOS caller | Android caller |
| --- | --- | --- |
| Send email link | `Account/ByEmail/EmailSignInView` | `account/byemail/EmailSignInView` |
| Email/password sign-in | `Account/ByEmailAndPassword/EmailAndPasswordSignInView` (loading and loaded) | `account/byemailandpassword/EmailAndPasswordSignInView` |
| Email-link verification | `Account/ByEmail/EmailLinkSignInView` (no content) | `account/byemail/EmailLinkSignInView` |
| Google sign-in | `Tyche/UI/Home/HomeRouter` (`Color.clear` overlay) | `home/HomeNavViewProvider` |
| Pool creation | `Pool/Creator/PoolFromLayoutCreatorView` (mutating and mutated; each builds a new `StepperView` and step-one view model) | `pool/creator/PoolFromLayoutCreatorView` |
| Pool loading/joining | `Pool/Joiner/PoolJoinerView` (join over content; initial load with no content) | `pool/joiner/PoolJoinerView` (two calls) |
| Pool deletion | `Tyche/UI/PoolHome/PoolHomeRouter` | `poolhome/PoolHomeView` |

`BallSpinner` is also used inline (excluded from this change) by `UsernameEditor`, `ProfileView`, `PendingBetItemView`, and `ManageGamblerItem` on iOS, and by `UsernameEditor`, `PendingBetItemView`, and `ManageGamblerItem` on Android.

Every Android caller except `PoolHomeView` and the join state of `PoolJoinerView` passes empty content and draws the screen as a sibling.

iOS, Fortuna app (signed out):
- Welcome → **Email** (tap at 218, 828 pt) opens "Sign in" with an empty Email field and a disabled Sign in button: `ios-baseline-app-email-signin-idle-light.png`.
- Back (40, 84 pt) → **Email and password** (218, 884 pt) opens the form with Email, Password with a visibility toggle, a disabled Sign in button, and the recovery hint: `ios-baseline-app-email-password-signin-idle-light.png`.
- No form was submitted. Pool flows need a signed-in session and were not opened. No test pools were created.

iOS, current loader (before the change), held in a throwaway harness app described under 1.4: material, 15% black dimming, and radius-6 blur cover the form until it is barely legible in both themes. The navigation large title is blurred with the content. Captures: `ios-baseline-form-idle-{light,dark}.png`, `ios-baseline-form-loading-{light,dark}.png`, `ios-baseline-standalone-loading-{light,dark}.png`.

Android, Fortuna Prod debug on API 37 (signed out):
- ARTEMIS autonomous tasks did not run. A Pro run (`29fed63e-a1e4-4a10-b7f7-244bcb5fa5cb`) failed to launch the locked package and then timed out on the planner; a Flash run (`8ec2abda-9966-4318-bd73-179a1d600bf6`) timed out before its first step; a third was rejected by the daemon. `mobile_diagnose` reported `ready` with a valid key, but its logs show Gemini `429 Too Many Requests` (quota exceeded) and `503` responses.
- Exploration therefore used ARTEMIS `mobile_get_device_state` (hierarchy and screenshot) with `adb shell input` taps. The emulator showed a "System UI isn't responding" dialog once; **Wait** dismissed it.
- Welcome hierarchy: "See who’s guessed the most scores and challenge them now", "Continue with:", **Email** (normalized bounds [454,867][545,884], tapped at 672, 2618 px), **Email and password** ([334,919][665,936], tapped at 672, 2771 px). Capture: `android-api37-welcome.jpg`.
- **Email** opens "Sign in": a clickable EditText labelled Email ([24,351][1320,543] px), a disabled Sign in button ([24,567][1320,711] px), and a back control ([12,183][156,327] px) with an empty content description. Tapping the field opened the keyboard (`mInputShown=true`); an invalid address kept Sign in disabled. `adb shell input text` dropped and repeated characters ("loading-probe" arrived as "loadiloadi"), so future Android tests should enter text through Compose test APIs rather than `input text`. Capture: `android-api37-email-signin-keyboard.png`.
- The top-bar back returned to the welcome screen. `uiautomator dump` returned a stale tree after navigation; ARTEMIS hierarchy and `screencap` were reliable.
- **Email and password** shows Email, Password, a disabled Sign in button, and the recovery hint. Capture: `android-api37-email-password-signin.png`.
- None of the controls expose resource ids or test tags; text is the only dynamic locator observed. No form was submitted.

### 1.2 State-driven host

`LoadingContainerView(isLoading:content:)` always renders its content in the same position and toggles the effects on it. `LoadingContainerView(isLoading:)` is the no-content form. The material and black dimming layers are gone. The only background effect is `blur(radius: 6)` on the content. Without content, the host paints `Color(uiColor: .systemBackground)`; with content, it paints nothing and the screen keeps its container's background.

Rendered on the simulator through the harness, with the light/dark appearance switched by `simctl ui appearance`:
- `ios-host-form-loading-light.png`, `ios-host-form-loading-dark.png`: the form is softly blurred over its normal white or black background, with no gray, black, or material layer. The ball is sharp (full-resolution crop shows hard edges). The navigation bar's toolbar button stays sharp and usable. The large title is blurred with the content, as in the baseline.
- `ios-host-standalone-loading-light.png`, `ios-host-standalone-loading-dark.png`: the ball on plain white or pure black, with the navigation bar intact.

The `#Preview`s in `LoadingContainerView.swift` cover the same four cases. They were not opened in Xcode's canvas; the harness rendered the same host on the simulator instead.

The UI package, Account, Pool, and the Tyche app compile: the `Tyche` scheme built for the test run below with no errors.

### 1.3 Tyche ball

- Asset: `tyche_logo.imageset/TycheLogo.svg` has the same `0 0 500 499` viewBox as `logos/TycheLogoBall.svg`. Its single path is the canonical silhouette path followed by the canonical panels path, token for token (399 tokens), rendered as a template. Size stays 64 × 64 pt, aspect fit, `Color.accentColor` (#2E7D32 light, #4CAF50 dark in `AccentColor.colorset`). The harness copies that colorset.
- Rotation: `BallSpinner` now draws from `TimelineView(.animation)` with the angle `(t mod 2 s) / 2 s × 360°`. It is linear and never reverses, and it stops when the ball leaves the screen. `BallSpinnerRotationTests` checks 0, 0.5, 1, 1.5, 2, and 2.25 s → 0°, 90°, 180°, 270°, 0°, and 45°.
- Normal motion: consecutive captures of the ball differ (MD5 of the ball crop changes every 0.4–0.5 s).
- Reduce Motion, toggled live while the ball was visible with `defaults write com.apple.Accessibility ReduceMotionEnabled` and the `com.apple.accessibility.reduce.motion.status` notification: two captures 0.5 s apart were identical, with the ball upright (`ios-host-reduce-motion-static.png`). Turning it off again resumed rotation (three different consecutive crops). The status stayed in place throughout.
- No stock spinner or new asset was introduced. Inline `BallSpinner` users get the same Reduce Motion behavior.

### 1.4 Placement, isolation, status, restoration

Representative screen: a throwaway harness app built in the scratchpad (not committed). It has a `NavigationStack` with a toolbar Load/Stop button outside the host and a `ScrollView` form inside `LoadingContainerView` (Email and Name fields, a Submit button that counts, and 40 rows). Launch arguments hold loading or start it after a delay.

Checks driven by an XCUITest suite in the harness, with explicit `waitForExistence` waits and coordinates taken from observed frames:

| Check | Result |
| --- | --- |
| Tap on a covered row and on Submit's frame while loading | Submissions stayed 0 |
| Drag up and down over the covered list while loading | After loading ended, Row 12's frame was unchanged (24, 938.7, 54.3 × 20.3) |
| Typing while loading | The focused field resigned when loading began (keyboard count 0 afterward); taps on the field during loading did not focus it; manual typing during loading did not change the field |
| Retained values after loading ends | Email kept "abc"/"kb"/"rot" across loading, scrolling, and rotation; Submit then worked (count 1) |
| Accessibility tree while loading | The Email field, Submit, and rows were absent; the tree held only the navigation bar, the toolbar button, and one element labelled "Loading" (its `tyche_logo` image appears as a child in XCUITest's tree) |
| Toolbar outside the host | Remained hittable |
| Ball placement, portrait | Center (220, 519) pt: horizontally centered, vertically centered between the navigation bar and the bottom safe area |
| Keyboard visible when loading begins | The keyboard dismissed as the field lost focus; the ball stayed centered at (220, 545) pt in the full content area (`ios-host-ui-keyboard-before-loading.png`, `ios-host-ui-keyboard-loading.png`) |
| Rotation while loading | Landscape center (478, 249) pt in a 956 × 440 window; back to portrait (220, 545); loading and the field value continued (`ios-host-ui-landscape-loading.png`) |

The status uses the new `loading_status` string (en "Loading", es and es-ES "Cargando") with `extractionState: manual`. It takes VoiceOver focus when it appears through `@AccessibilityFocusState`.

Not verified:
- VoiceOver itself: the iOS simulator has no VoiceOver. The XCUITest accessibility tree is the evidence, and the focus move is unobserved.
- iPad window resizing: the run used iPhone rotation only.
- A hardware-keyboard key press while loading: the focused field resigns, so there is no first responder to receive one, but no key press was sent.
- Real-flow loading in the Fortuna app: holding loading requires submitting a form or signing in.

### 1.5 Coverage and contract

Host contract (doc comment on `LoadingContainerView`):
- The content stays mounted in one position.
- While loading, the content is blurred, disabled, behind a clear hit-testing barrier, and hidden from accessibility, with one localized status and the ball centered in the host's safe area.
- When loading ends, all of that is removed.
- With no content, the host paints the system background.

New file `iOS/UI/Tests/UITests/Loading/LoadingContainerViewTests.swift` (Swift Testing):
- `startingAndEndingLoadingKeepsTheSameScreen`: hosted in a `UIWindow`, toggling loading on, off, and on creates the screen's `@StateObject` once, with one appearance and no disappearance. Passes.
- `idleHostLeavesTheScreenInteractiveWithoutAStatus`: passes.
- `loadingBlocksTheCoveredControlAndExposesOneStatus`: the tap-is-refused assertion passes. The `accessibilityHidden` and status-label assertions fail on the iOS 27 runtime.
- `aHostWithoutContentShowsOnlyTheStatus`: fails on the iOS 27 runtime at the status-label lookup.
- `BallSpinnerRotationTests`: 6 of 6 cases pass.

A probe showed that ViewInspector 0.10.3 on iOS 27 returns no `accessibilityHidden()` value and finds no accessibility label even for `Button("A").accessibilityHidden(true)` and `Color.clear.accessibilityLabel(...)`. These are the same APIs `DrawerInteractionTests` uses, and that suite crashes locally with "Can't unsafeBitCast between types of different sizes". The two failing assertions are expected to pass on CI's iOS 26.6 runtime but have not run there.

Command:

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild test \
  -workspace iOS/Tyche.xcworkspace -scheme Tyche \
  -destination 'id=D8F12B93-3319-437A-8CE2-59C620CC8A29' \
  -skip-testing:TycheUITests \
  -only-testing:UITests/LoadingContainerViewTests -only-testing:UITests/BallSpinnerRotationTests \
  CODE_SIGNING_ALLOWED=NO
```

Result: build succeeded. 8 test cases: 6 passed and 2 failed with the ViewInspector issue above (3 issues).

Settings changed and restored: the simulator appearance ended in light, as it started; `ReduceMotionEnabled` was deleted to restore the default; the harness app and its UI-test runner were uninstalled. The Android emulator was left booted with Fortuna Prod debug installed on the welcome screen.

### Follow-up checks (same day)

#### Decisions recorded

- Losing focus when loading starts is intended. `.disabled` makes a focused field resign, so the software keyboard closes as loading begins and focus is not restored afterward.
- The two ViewInspector accessibility assertions stay as written and must pass on CI's iOS 26.6 runtime.

#### 1.1 Android exploration with the `android` CLI (1.0.16500706)

API 37 emulator (`emulator-5554`, Fortuna Prod debug, signed out), using `android layout --device emulator-5554 [--full] --pretty` and `android screen capture`:
- The first `android layout` call failed ("Error reading response from server") while the CLI installed its instrumentation server (`com.android.cli.interact.instrumentation`, still installed on the emulator). Later calls worked. The trees are saved as `android-api37-welcome-layout.json`, `android-api37-email-signin-layout.json`, and `android-api37-email-password-layout.json`.
- Locators are no better than with `uiautomator`: no node in Fortuna has a resource id, test tag, or content description. Actionable rows are found by their text child ("Email" at center 672, 2620 px; "Email and password" at center 672, 2776 px).
- Controls with no accessible name:
  - the Google button ([600,2392][744,2536]);
  - the top-bar back button ([12,183][156,327]);
  - the password visibility toggle ([1176,627][1320,771]).
- The CLI reports interactions and state but not "enabled". The disabled Sign in button shows only `CLICKABLE`; enabled controls also show `FOCUSABLE`.
- Typing: after the layout confirmed the Email field had `state: FOCUSED`, `adb shell input text "loading-probe"` produced exactly `loading-probe`. The earlier "loadiloadi" came from typing before focus and the keyboard had settled, not from `input text` itself. Sign in stayed disabled for this invalid address. Capture: `android-api37-email-signin-cli.png`.
- The top-bar back control returned to the welcome screen both times.

Samsung Galaxy S8+ (`ce0417143a6ab8130c`, SM-G955F, Android 9 / API 28, build PPR1.180610.011.G955FXXUCDZEG, 1080 × 2220 override on a 1440 × 2960 panel, density 420, window animation scale 1.0, unchanged):
- Fortuna 2.0.0 (12) was already installed (debuggable; its dex contains the Prod `urlBasePath`, not the local one) and is signed in on the Spanish "Mis pollas" pool list. It was not reinstalled. The welcome, Email, and Email-and-password screens cannot be reached without signing out, so they were not explored on this device.
- `android layout --device ce0417143a6ab8130c` hung at "Installing layout instrumentation server..." and was stopped. No CLI package was left on the phone. `android screen capture` worked.

Current Cloudy loader, below and above API 31: there is no way to hold Fortuna's sign-in loader without submitting, so a temporary instrumented harness was used. It was `Android/ui/src/androidTest/.../loading/LoadingBaselineCaptureHarness.kt`, run as `com.felipearpa.tyche.ui.test` through `am instrument`. It rendered `TycheTheme` with a Scaffold "Sign in" form in two arrangements:
- **overlay**: `LoadingContainerView {}` as a sibling over the form, as the sign-in, link-verification, pool-creation, pool-load, and Google callers do;
- **wrapped**: `LoadingContainerView { form }`, as `PoolHomeView` and the pool-join state do.

The file and the test package were removed from both devices after the captures, and `git status` shows no Android change.

| Capture | Observation |
| --- | --- |
| `android-api28-baseline-overlay-loading-{light,dark}.png` | The form is fully sharp under a flat gray (light) or green-gray (dark) veil; the ball is centered and sharp |
| `android-api28-baseline-wrapped-loading-{light,dark}.png` | The same veil; the form text is slightly softened (Cloudy's pre-31 path) |
| `android-api37-baseline-overlay-loading-{light,dark}.png` | Same as API 28 overlay: sharp form under the veil |
| `android-api37-baseline-wrapped-loading-{light,dark}.png` | A slight blur under the veil, close to API 28 |

On both API levels, the screens that use the overlay pattern today show no blur at all. The only background effect is the colored veil.

#### 1.2 Preview bodies

Xcode 27 has no command-line preview renderer, and the Xcode canvas was not opened. The four `#Preview` bodies were copied verbatim into the harness, including the private `LoadingPreviewForm`, and rendered on the iPhone 18 Pro Max iOS 27 simulator:
- `ios-preview-content-light.png`, `ios-preview-content-dark.png`: the grouped Form keeps its normal grouped background (light gray, black) and is softly blurred, with the large "Sign in" title blurred with it. The ball is sharp and centered, with no added layer.
- `ios-preview-no-content-light.png`, `ios-preview-no-content-dark.png`: the ball on plain white or black.

What remains: the previews were not opened in Xcode's canvas itself.

#### 1.4 Additional checks

- **iPad window resizing** (iPad Pro 13-inch (M5), iOS 27, `2CADE6B5-934E-42A0-99A1-1BD92D9C83E8`, windowed apps): an XCUITest dragged the window's resize corner while loading was active. The window went from 1032 × 1376 pt to 375 × 486 pt at (329, 388).
  - The status center moved from (516, 747) to (516.5, 684). That is the horizontal center of the new window, and vertically centered between its navigation bar (bottom at 504) and its bottom edge.
  - Loading continued through the resize, and the field kept "ipad".
  - Captures: `ios-ipad-loading-before-resize.png`, `ios-ipad-loading-after-resize.png`.
- **Hardware keys during loading** (`XCUIApplication.typeKey` on the iPhone simulator):
  - As a control, keys typed into the focused field while idle arrived ("ab").
  - During loading, the letters z, y, x, Tab, Space, Return, and Command-V reached nothing: the field kept "ab", Submit was not activated (Submissions: 0), and loading stayed active.
- **Autocorrection when loading starts**: with autocorrection enabled on the field, "Ab" became "An" as loading began, even when no key was pressed during loading. Resigning focus commits the pending autocorrection into the bound text. With autocorrection disabled, the value was unchanged.
  - In production, `EmailTextField` (send-link and email/password forms) and `ValidatableTextField` (pool name) do not disable autocorrection; `PasswordTextField` does.
  - The submitted value is captured before loading starts, but the text shown afterward (and reused on retry) could differ from what was submitted.
  - Not compared against the old branch-switching loader.
- **Accessibility substitute for VoiceOver**: `performAccessibilityAudit()` while loading, on the form and on the standalone screen:
  - Its only issues were on the system toolbar's "Stop" button (contrast; Dynamic Type partly unsupported), which is outside the host.
  - The hittable labelled elements were only the navigation title, the Stop button, "Loading", and its `tyche_logo` child.
  - Real VoiceOver was not observed; the simulator has none, and the status's focus move remains unobserved.
- **Real Fortuna flows**: not run. The iOS simulator is signed out, and the Android emulator is signed out. The Samsung is signed in with Prod Fortuna, but no pools were created there pending your confirmation.

#### 1.5 CI

Not run here; it needs a push. CI must pass these, on the `Tyche` scheme with iOS 26.6:
- `UITests/LoadingContainerViewTests/loadingBlocksTheCoveredControlAndExposesOneStatus()`
- `UITests/LoadingContainerViewTests/aHostWithoutContentShowsOnlyTheStatus()`
- `UITests/LoadingContainerViewTests/idleHostLeavesTheScreenInteractiveWithoutAStatus()`
- `UITests/LoadingContainerViewTests/startingAndEndingLoadingKeepsTheSameScreen()`
- `UITests/BallSpinnerRotationTests/angleGrowsLinearlyAndWrapsEveryTwoSeconds(seconds:degrees:)`

#### Device state after these checks

- iPhone simulator: light appearance, Reduce Motion default; the harness and its runner are uninstalled.
- iPad simulator: the harness is uninstalled and the simulator is shut down.
- Emulator: Fortuna Prod debug on the welcome screen. The `android` CLI's `com.android.cli.interact.instrumentation` and ARTEMIS's `com.artemis.helper` are installed there.
- Samsung: unchanged apart from running the temporary test package, which was installed and then uninstalled. The protected `.test` packages are untouched. Fortuna was left on its pool list.

### Real pool flows on Android 9 and further decisions (same day)

#### Decisions recorded

- You accepted that, when loading drops focus, iOS may commit a pending autocorrection into the email fields (`EmailTextField`) and the pool-name field (`ValidatableTextField`). The fields are left as they are.
- These gaps stay open by your decision: the Xcode preview canvas, real VoiceOver, the CI run, and the Samsung sign-in forms (which would need a sign-out).

#### Samsung (`ce0417143a6ab8130c`, Android 9 / API 28): real Fortuna pool flows

Setup: the existing signed-in Prod Fortuna 2.0.0 (12) was used. It was not reinstalled, its data was not cleared, and no one signed out. Animation scales stayed at 1.0 and were not changed.

Tools:
- `android layout` does not work on this phone. Its helper `com.android.cli.interact.instrumentation` (pulled from the emulator, targetSdk 24) was rejected: `adb install` failed with `INSTALL_FAILED_VERIFICATION_FAILURE`, and the phone shows a Google Play Protect dialog, "Se bloqueó la app no segura", for "Android CLI Instrumentation Server". The dialog was left on screen for you; nothing was tapped and no security setting was changed.
- Navigation therefore used `uiautomator dump` and `adb shell input`. Loaders were captured with `screenrecord`; frames were extracted with ffmpeg at 4 fps.

Throwaway pool:

| Field | Value |
| --- | --- |
| Name | "zz loading probe 2026-10-14" |
| Template | "Fortuna QA — kickoff groups" |
| Pool id | `01M4XP3FPT9N6KGYBX1362SWD4` |
| Created | 12:07 |
| Deleted | 12:08:56–57: `DELETE /pools/01M4XP3FPT9N6KGYBX1362SWD4` → 204, per Ktor logcat |
| Afterward | The pool list again shows only the three existing pools ("Copa Mundial de la FIFA 2026", "Copa Mundial de la FIFA prur", "Prueba"). No existing pool was opened or changed. |

| Flow | Observation | Evidence |
| --- | --- | --- |
| Pool creation (sibling pattern) | After tapping **Hecho**, the current loader showed for about 1.75 s. The form stays fully sharp under a flat light-gray veil, and the ball is centered and rotating. For the first frame the software keyboard was still visible under the veil, then it closed. The app then returned to the pool list with the new pool | `android-api28-fortuna-pool-create-loading-keyboard.png`, `android-api28-fortuna-pool-create-loading.png`, `android-api28-fortuna-pool-create.mp4` |
| Opening the pool from the list | No full-screen loader appears: the pool screen shows model-backed placeholder rows for about 1.5 s, which this change excludes. The full-screen initial pool-loading state belongs to the join flow | `android-api28-fortuna-pool-open-placeholders.png`, `android-api28-fortuna-pool-open.mp4` |
| Pool deletion (wrapped pattern) | After confirming **Eliminar** in "¿Eliminar polla?", the loader showed for about 1.75 s (the DELETE took 1.63 s). The pool screen sits under the same gray veil, with its content barely visible and softened. The app then returned to the pool list, which briefly showed placeholders | `android-api28-fortuna-pool-delete-loading.png`, `android-api28-fortuna-pool-delete.mp4` |

Not verified:
- Join flow, including its initial full-screen pool load: it needs an invite to a pool you own.
- Real flows on API 37 and iOS: those devices are signed out.

Accepted without opening the `#Preview`s in Xcode's canvas (1.2) by the user on 2026-10-14.
Accepted without real VoiceOver traversal and focus move (1.4) by the user on 2026-10-14.
Accepted without exploring the welcome and sign-in forms on the Samsung API 28 device (1.1) by the user on 2026-10-14.
Accepted without the pool join flow and its initial full-screen pool load, real flows on the API 37 emulator and iOS, and `android layout` on the Samsung API 28 device (1.1) by the user on 2026-10-14.
Accepted without observing the new loader in a real Fortuna flow on iOS (1.4) by the user on 2026-10-14.

Accepted without a CI run of the 1.5 tests (they passed locally on iOS 18.6, including both accessibility assertions) (1.5) by the user on 2026-10-14.

## iOS — tasks 2.1–2.4 (2026-10-14)

### Environment

- iOS 27: iPhone 18 Pro Max simulator, iOS 27.0 (24A434), `D8F12B93-3319-437A-8CE2-59C620CC8A29`, 440 × 956 pt.
- iOS 18.6 (pre-26): iPhone 16 Pro Max simulator, iOS 18.6 (22G86), `5F3DAAC7-6DB3-4CE2-B3EB-CCA538430588`, 440 × 956 pt. iPad Pro 13-inch (M4), iOS 18.6, `1F355430-9EE7-448E-8A6C-40C2F612B609`, 1032 × 1376 pt.
- Everything was built with Xcode 27.0 (`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`). It has both runtimes; Xcode 26.5 lists the same runtimes but was not needed.
- Fortuna was rebuilt from this branch (`Tyche` scheme, Debug, generic iOS Simulator) and installed on both iPhones as 2.0.0 (12). Both are signed out. Nothing was submitted, no password was typed, and Sign out was never touched.
- Android: `android` CLI 1.0.16500706 on `emulator-5554` (API 37) and the Samsung `ce0417143a6ab8130c` (API 28).

### 2.1 Sign-in flows

Changes:
- Send link (`EmailSignInView`): idle, sending, and failure share one `LoadingContainerView` around the form, so the typed address stays in the same field. The failure alert sits on that subtree. Only a sent link replaces the form.
- Email and password (`EmailAndPasswordSignInView`): one host around the form for every state. It stays loading after a successful sign-in until the caller routes away, as before. The success callback now fires from `onChange` of the signed-in account instead of `onAppear` of a separate loaded branch, so it fires once per sign-in.
- Email-link verification (`EmailLinkSignInView`): unchanged from group 1. It is the standalone host (normal system background) until the result, which has no prior content.
- Google (`HomeRouter`): the `Color.clear` overlay is gone. The host wraps the actual welcome screen (`HomeView`) at the root of the navigation stack. It is loading while the Google account is verified and while the signed-in account is handed off (`SocialSignInRow` already treats both as busy). `onSignIn` fires from `onChange` of the signed-in account. The provider prompt comes before the loading state and is never covered.

Evidence. Real submissions were replaced by a throwaway harness app (not committed). It renders the production `EmailSignInStatefulView`, `EmailAndPasswordSignInStatefulView`, and `PoolFromLayoutCreatorStatefulView` through `@testable import`. `HomeView` is a byte-identical copy of the app's file, wrapped as `HomeRouter` wraps it. The harness drives states from a launch-argument timeline. An XCUITest typed into the fields with the simulator keyboard and read values back. Callbacks were recorded to a file in the app container.

| Check | iOS 18.6 | iOS 27 |
| --- | --- | --- |
| Send link: address typed with keyboard up → loading → failure alert → idle → loading → sent | Field kept `loading-probe@example.com` after the failure; keyboard closed when loading began (1 → 0); Sign in enabled again after the failure; sent screen replaced the loader | Same |
| Email and password (email typed, password empty) → loading → failure → idle → loading → signed in | Email kept; loader stayed up after sign-in; `onAuthenticate` recorded once | Same |
| Welcome (Google host) → loading → signed in | Welcome blurred under a sharp ball; tap on the covered Email button recorded nothing; `onSignIn` recorded once; loader stayed up through the handoff | Same |
| Standalone loader (email-link verification / initial pool load) | Ball on plain white or black | Same |
| Real Fortuna, signed out: Email form opened and an address typed (not submitted), back | Value kept in the field; Sign in enabled; back returned to welcome | Same |
| Real Fortuna: Google button → system "“Tyche” Wants to Use accounts.google.com" prompt → Cancel | No loader appeared; Email and Google remained enabled; no error alert | Same |

Captures (`verification/`): `g2-<os>-<light|dark>-welcome-{idle,loading}.png`, `g2-<os>-<light|dark>-send-{idle-keyboard,loading,failure,retry-idle,sent}.png`, `g2-<os>-light-password-{loading,loaded}.png`, `g2-<os>-standalone-{light,dark}.png`, `g2-<os>-fortuna-{welcome,email-typed,google-prompt,after-google-cancel}.png`, where `<os>` is `ios186` or `ios27`.

Observations:
- The covered buttons render in their disabled style under the blur (for example, the welcome screen's green Email button turns gray). This follows from the host disabling the covered content.
- The navigation title above a loading form ("Sign in", "Pool from layout") stays sharp, because the host sits under the navigation bar.

### 2.2 Pool flows

Changes:
- Pool creation (`PoolFromLayoutCreatorView`): the four branches, each with its own `StepperView` and step-one view model, are now one `StepperView` inside one host. The host is loading while creating and until the created pool is opened. A failure leaves the stepper as it was, with the error alert on it. `onPoolCreated` fires from `onChange` of the created pool id. The screen padding moved inside the host, so the host fills the screen.
- Pool joining (`PoolJoinerView`): idle, joining, and joined share one host around the loaded pool. The initial pool load remains the standalone host. The join failure screen is unchanged.
- Pool deletion (`PoolHomeRouter`): the `if`/`else` that swapped pool home into a different branch during deletion is replaced by a host that always wraps pool home. Pool home keeps its identity, including tabs, navigation paths, and scroll positions, through a deletion and its failure.

Harness evidence, pool creator (production view with a fake list of 30 templates). The test picked "Harness template 2", typed " zz draft" after the prefilled name, then ran loading → failure → idle → loading → created:

| Check | iOS 18.6 | iOS 27 |
| --- | --- | --- |
| Step and draft | Step two and "Harness template 2 zz draft" kept through loading and the failure; step one never came back | Same |
| Keyboard | Closed when loading began (fields 1, keyboard 0) | Same |
| Created pool | `onPoolCreated created` recorded once; loader stayed up until then | Same |

Captures: `g2-<os>-<light|dark>-creator-{step-two-keyboard,loading,failure}.png`.

Not verified, pending your sign-in: the real create → delete round trip with a throwaway pool on iOS, the deletion loader over the real pool home, and scroll-position retention on real pool content. Both simulators are signed out. No pool was created or deleted, and none exists from this group. Join is not verified, by your decision.

### 2.3 Exploration, regression coverage, and inventory

Android exploration with the `android` CLI only:
- `emulator-5554` (API 37): `android run` with the existing Prod debug APK (unchanged Android tree; the CLI did a forced delta reinstall to restart the app) opened Fortuna on the signed-out welcome screen. `android layout` matched group 1's welcome tree: "See who’s guessed the most scores and challenge them now", "Continue with:", an unnamed clickable (Google) at (672, 2464), "Email" at (672, 2620), and "Email and password" at (672, 2776) px. Captures: `android-api37-welcome-group2.png` and `android-api37-welcome-layout-group2.json`.
- The CLI has no tap, typing, or app-launch command except `android run`, which installs. Its own interaction guide uses `adb shell input` for taps, and that was ruled out for this group. Exploration therefore stopped at the screen each device was already on.
- Samsung (`ce0417143a6ab8130c`, API 28): after Play Protect was turned off, `android layout` installed `com.android.cli.interact.instrumentation` itself ("Installing layout instrumentation server...") and returned the layout. The phone was on its One UI home screen. Opening Fortuna there needs `am start` or a tap (not allowed), or `android run` (a reinstall, not allowed). Fortuna was not opened, and nothing was changed on the phone.

New Swift Testing coverage. Each test hosts the production view in a `UIWindow` and drives its state through an `ObservableObject`:
- `AccountTests/SignInLoadingRetentionTests`
  - `sendLinkFormKeepsTheTypedAddressThroughLoadingFailureAndRetry`
  - `emailAndPasswordFormKeepsTheTypedValuesThroughLoadingFailureAndRetry`: the same `UITextField` instances hold the typed text after each state.
  - `aSuccessfulEmailAndPasswordSignInIsReportedOnce`: loading → loaded, a redraw, and loaded again report once.
- `PoolTests/PoolCreatorLoadingRetentionTests/theStepperAndItsDraftSurviveLoadingFailureRetryAndSuccess`: the creator resolves its template use case once, so it builds the stepper once. The name field is the same instance with the same text through mutating → failure → idle → mutating → mutated. `onPoolCreated` fires once after a further redraw.
  - The pool-name field binds through `@FocusState`. Edits made from code (`editingChanged`, or `insertText` while first responder) never reached its binding in a unit-test host on either runtime, so this test uses the preselected name as the draft. Typed-text retention on that screen comes from the harness run above.

To make these tests possible, `EmailSignInStatefulView`, `EmailAndPasswordSignInStatefulView`, and `PoolFromLayoutCreatorStatefulView` changed from `private` to internal.

Regression check: with the group-1 versions of the three callers temporarily restored (only `private` removed so the tests compile), on iOS 18.6:
- The two sign-in retention tests and the pool-creator test failed: values reset and the stepper was rebuilt (30 issues).
- `aSuccessfulEmailAndPasswordSignInIsReportedOnce` passed. The old `onAppear` also fired once; this test guards the new `onChange` path.
- The current files were restored afterward and checked identical to the saved copies.

Commands and results:

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild test \
  -workspace iOS/Tyche.xcworkspace -scheme Tyche \
  -destination 'id=5F3DAAC7-6DB3-4CE2-B3EB-CCA538430588' \
  -skip-testing:TycheUITests -only-testing:AccountTests -only-testing:PoolTests -only-testing:UITests \
  CODE_SIGNING_ALLOWED=NO
```

- iOS 18.6: **TEST SUCCEEDED**. AccountTests 25, PoolTests 42, and UITests 103 Swift Testing cases all passed, including all of `LoadingContainerViewTests` (with `loadingBlocksTheCoveredControlAndExposesOneStatus` and `aHostWithoutContentShowsOnlyTheStatus`), `DrawerInteractionTests`, and the ViewInspector-based Pool suites. iOS 18.6 has no unsafeBitCast crash, so the two accessibility assertions that failed on iOS 27 in group 1 pass locally here.
- iOS 27 (`D8F12B93-…`), `-only-testing:AccountTests/SignInLoadingRetentionTests -only-testing:PoolTests/PoolCreatorLoadingRetentionTests`: **TEST SUCCEEDED** (3 + 1). The full iOS 27 UITests and PoolTests suites were not rerun: group 1 recorded the ViewInspector limits on that runtime.
- xcodebuild rewrote `iOS/Account/Package.resolved` and `iOS/Pool/Package.resolved` again. Both were reverted each time.

Inventory: every production `LoadingContainerView` caller wraps its actual screen, except two that intentionally use the standalone host because no prior content exists: email-link verification (idle/loading) and the initial pool load in `PoolLoadStatefulView`. No caller passes `Color.clear` or `EmptyView` content as an overlay, and none builds a second copy of its screen for the loading state.

### 2.4 Presentation checks

| Check | iOS 18.6 | iOS 27 |
| --- | --- | --- |
| Content-backed loader, light and dark (welcome, send-link form, pool creator) | Content softly blurred over its normal background (welcome gradient, white or black form); ball sharp; no added layer | Same |
| Standalone loader, light and dark | Ball on plain white or black (`g2-ios186-standalone-*`) | Same (`g2-ios27-standalone-*`) |
| Normal motion | Ball crop changed between three captures 0.4 s apart | Same |
| Reduce Motion toggled live while loading | Three identical crops with the ball upright (`g2-*-motion-reduced-1.png`); turning it off resumed rotation | Same |
| Accessibility while loading (substitute for VoiceOver) | Hittable labelled elements: the navigation title "Sign in", "Loading", and its `tyche_logo` child. The audit reported only a contrast issue on the "Sign in" navigation title, which is outside the host | Same |
| Recovery after loading | After the failure, the field was reachable, held its value, and Sign in was enabled | Same |
| Keyboard visible when loading begins | Keyboard closed; ball centered at (220, 511) pt in the content area | Ball centered at about (220, 519) pt |
| Rotation while loading | Landscape center (478, 231.5) pt in 956 × 440; back to portrait (220, 511); loading and value continued | Landscape (478, 249); portrait (220, 519) |
| iPad (18.6, portrait, full screen) | Send-link flow passed; ball at (516, 712.5) in 1032 × 1376 | Window resizing verified in group 1 |
| Completion and removal | The sent screen replaced the loader immediately; the loader stayed through sign-in or pool handoffs until the result callback, which fired once | Same |

The ball's frames above are rotated bounding boxes, so their reported size varies between 64 and 90 pt; the centers are what was compared.

Placeholders, inline progress, and native navigation styling:
- No placeholder, inline-progress, or navigation code changed in this group.
- In the harness, navigation bars above a loading screen kept their normal title and appearance.
- Pool home's idle host (always present now) was not observed in the real app.

Not verified:
- Real backend submissions: send link, email/password sign-in, email-link verification, and completing Google sign-in. Excluded by your decision. Harness and unit tests stand in for them.
- Real VoiceOver: the simulators have none. The accessibility tree and audit stand in for it.
- iPad window resizing on iOS 18.6. The rotation test on the iPad failed before rotating: typing into the field raised "Neither element nor any descendant has keyboard focus". Rotation was verified on the iPhone 16 Pro Max instead.
- Pool flows in the real app on both iOS versions (create, delete, deletion loader over pool home, pool home's idle navigation styling, inline progress in Profile/Username editor): they need a signed-in simulator.
- Pool join: excluded by your decision.
- Android sign-in and pool screens beyond the emulator's welcome screen: the CLI cannot navigate without `adb shell input`.

### Device state afterward

- The harness apps were uninstalled from all three simulators.
- Appearance is back to light, and `ReduceMotionEnabled` is deleted (default).
- The iPad is shut down. Both iPhones are booted with this branch's Fortuna build installed and signed out.
- Emulator: Fortuna Prod debug on the welcome screen.
- Samsung: `com.android.cli.interact.instrumentation` is now installed (by `android layout`). Fortuna was not opened; the phone was on its home screen. No settings were changed.

Accepted without real backend submissions for email-link sending, email/password sign-in, email-link verification and Google sign-in (2.1) by the user on 2026-10-14.
Accepted without real VoiceOver traversal and recovery (2.1, 2.4) by the user on 2026-10-14.

### Follow-up after your decisions (same day)

#### Decisions recorded

- Android navigation may use `adb shell input` and `am start`. Observation stays on the `android` CLI.
- Accepted without a CI run for 1.5: the full iOS 18.6 run above stands in for it.
- Accepted without real VoiceOver for 2.1 and 2.4.
- Real backend sign-in submissions remain excluded.
- iPad rotation and resizing on iOS 18.6 were not accepted as unverified; they were retried (below).

#### 2.3 Android exploration (current Android loader, before groups 3–4)

**`emulator-5554`** (API 37, Fortuna Prod debug, signed out):
- Opened from the welcome screen: **Email** at (672, 2620) px; the top-bar back control at (84, 255) px.
- Email form, located by `android layout --no-idle`:
  - EditText labelled "Email" at [24,351][1320,543]; Sign in at [24,567][1320,711].
  - Tapping the field set `FOCUSED`. `input text "loading-probe@example.com"` produced exactly that text.
  - Sign in went from `CLICKABLE` to `CLICKABLE,FOCUSABLE` (enabled). It was not tapped.
- Back returned to the welcome screen.
- **Email and password** at (672, 2776) px opened a form with:
  - Email at [24,375][1320,567];
  - Password (`PASSWORD`) at [24,591][1320,783], with its unnamed visibility toggle at [1176,627][1320,771];
  - Sign in at [24,807][1320,951];
  - the recovery hint.
- Only the email was typed. The password stayed empty, and Sign in stayed disabled. Back returned to the welcome screen.
- Captures: `g2-android-api37-email-typed.png`, `g2-android-api37-email-password-typed.png`.

**Samsung `ce0417143a6ab8130c`** (API 28, signed in to Prod):
- **Helper.** With Play Protect off, `android layout` installed `com.android.cli.interact.instrumentation` itself and works. On text fields with a blinking cursor it needs `--no-idle`.
- **Launch.** `am start -n com.felipearpa.fortuna/com.felipearpa.tyche.MainActivity` brought the existing process forward. "Mis pollas" showed "Error inesperado", and **Reintentar** did not help.
  - Ktor logcat showed `401` with `error_description="the token has expired"` on `/accounts/me`, `/gamblers/…/pools`, and `/pool-layouts/open`. The process had been in the background with an expired ID token, and retrying did not refresh it.
  - `am force-stop` plus `am start` refreshed the token (FirebaseAuth "Notifying id token listeners") and loaded the list with `200`s.
  - This is unrelated to loading screens. It is reported as a separate issue below.
- **Throwaway pool:**

| Field | Value |
| --- | --- |
| Name | "zz loading probe android g2 2026-10-14" |
| Template | "Fortuna QA — kickoff groups" |
| Pool id | `01M4XZ1SE8PQVT6XKNMN4A2EXB` |
| Created | 14:43:26 |
| Deleted | 14:44:16–18: `DELETE /pools/01M4XZ1SE8PQVT6XKNMN4A2EXB?gamblerId=…` → `204` |
| Afterward | `android layout` of "Mis pollas" lists only "Copa Mundial de la FIFA 2026", "Copa Mundial de la FIFA prur", and "Prueba". No existing pool was opened or changed. |

- **Pool name field:**
  - The name field ([21,343][1059,511]) was prefilled with the template name and was cleared with key events. An empty name showed "El nombre de la polla debe tener entre 1 y 100 caracteres", and **Hecho** turned non-focusable (disabled).
  - **Hecho** is at (540, 617) px.
- **Creation loader:** the form stayed sharp under the flat gray veil, with the ball centered and the keyboard already closed (`g2-android-api28-pool-create-loading.png`). The app then returned to the pool list with the new pool. This matches group 1's baseline.
- **Deletion path:** open the pool; open the drawer with **Abrir menú** (75, 148); the pool card reads "Jugando ahora, zz loading probe android g2 2026-10-14"; tap **Eliminar polla** (459, 1196); confirm **Eliminar** (787, 1254) in "¿Eliminar polla?".
  - The loader covered the Puntajes screen under the veil, with its content softened (`g2-android-api28-pool-delete-loading.png`).
  - The list then reloaded with placeholder rows (`g2-android-api28-pool-list-after-delete-placeholders.png`).
- Fortuna was not reinstalled, its data was not cleared, and no one signed out. The drawer's "Cerrar sesión" (459, 2009) was not touched.

#### 2.4 iPad rotation and resizing on iOS 18.6 (retry)

On iPad Pro 13-inch (M4), iOS 18.6, `1F355430-9EE7-448E-8A6C-40C2F612B609`:
- The focus failure was fixed by tapping the field until it reported `hasKeyboardFocus`, then typing.
- The window was resized through Split View: the multitasking control → **Split View** → Contacts.
- The first attempt picked "App Library" instead of an app and left the harness parked at the edge. The simulator was rebooted and the harness reinstalled before the passing run.

| Step (send-link form loading, "ipad@example.com" typed) | Window (pt) | Ball center (pt) |
| --- | --- | --- |
| Portrait | 1032 × 1376 | (516, 712) |
| Landscape | 1376 × 1032 | (688, 540) |
| Split View with Contacts, landscape | 683 × 1032 | (341, 540) |
| Back to portrait, full screen | 1032 × 1376 | (515, 712) |

Loading stayed active through every change. Captures: `g2-ipad186-loading-{portrait,landscape,split,portrait-after}.png`. The harness was uninstalled afterward, and the iPad is shut down.

#### iOS pool flows

Not run. At 15:06, after a relaunch, both iPhone simulators showed Fortuna's welcome screen (signed out). No iOS pool was created.

### Real pool flows on iOS 18.6 and iOS 27 (same day, after your sign-in)

This supersedes "iOS pool flows: not run" above. You signed in on both iPhones. The flows were driven by XCUITest against the installed Fortuna build from this branch (`com.felipearpa.tyche`), from the throwaway harness's UI-test runner, which was uninstalled afterward.
- Before creating, each test checked that no "zz loading probe ios 2026-10-14" row existed.
- Before deleting, each test checked that the drawer named that pool.
- No existing pool was opened or changed. No password was entered, and Sign out was never tapped.

| | iOS 18.6 (`5F3DAAC7-…`) | iOS 27 (`D8F12B93-…`) |
| --- | --- | --- |
| Pool | "zz loading probe ios 2026-10-14", template "Fortuna QA — kickoff groups" | Same name and template, created after the 18.6 pool was gone |
| Pool id (from the app's `/pools/<id>` request URLs in the simulator log) | `01M4Y61FZ67NPM99MRNDG4RMJ3` | `01M4Y6B4Q60YJ58TE9M5JMRTTF` |
| Creation loader | Step-two form (name typed with the keyboard up) softly blurred under a sharp ball; the keyboard closed; the "My pools" back button and large title stayed sharp; then back to My pools with the new row ("Pending") | Same |
| Placeholders | My pools, pool-home Scores, and Bets showed their placeholder rows while loading, with no full-screen loader and no blur | Same (Scores) |
| Pool home when idle | Large title, navigation buttons, and the tab bar rendered normally with no blur; content was interactive | Same, with the iOS 27 floating tab bar |
| Inline progress | Saving a bet (1–0, Colombia–Ecuador, in the throwaway pool) showed the inline ball under the match row with no full-screen loader; the row then showed the saved score and Edit | Same |
| Scroll position under the delete loader | Not exercised: the pool's four matches fit the portrait screen, so the list did not scroll | In landscape, the Bets list was scrolled so Colombia was off screen (Ecuador at −11 pt, Brasil at 92 pt); the delete loader showed that same scrolled content blurred behind the ball |
| Delete loader | **Delete pool** → "Delete pool?" → **Delete**: the drawer closed while the whole pool home (title, list, tab bar) was blurred under a sharp ball; then My pools | Same, in landscape |
| Afterward | My pools lists only "Copa Mundial de la FIFA 2026", "Copa Mundial de la FIFA prur", and "Prueba" | Same, after a relaunch in portrait |

Captures:
- `g2-ios186-pool-{explore-0-placeholders,create-step-two,create-loading-1,create-after,home-opening-1,home-bets-opening-1,home-scores-idle,home-bets-idle,bet-saving-1,bet-saved,drawer,delete-loading-1,delete-loading-3,after-delete}.png`
- `g2-ios27-pool-{create-step-two,create-loading-1,create-after,home-opening-1,home-scores-idle,home-bets-idle,bet-saving-1,home-bets-scrolled,delete-loading-1,delete-loading-3,verify-gone}.png`
- The iOS 27 landscape captures are stored in device orientation (rotated).

Notes:
- The iOS 27 deletion test errored after the loader captures, while reading row positions during the navigation away. Deletion had already gone through: a relaunch confirmed the pool was gone, and the simulator was left in portrait.
- The deletion loader also blurs pool home's tab bar and navigation bar, because the host wraps the whole pool home as before. The pool creator's "My pools" back button stays usable during creation; this is unchanged, since the creator never hid it.
- The DELETE response status was not visible in the iOS log. Removal is confirmed by the refreshed list.
- Scroll position under the delete loader is verified on iOS 27 only. On iOS 18.6 the throwaway pool's content did not scroll.

Device state: both iPhones are signed in, on My pools, in portrait and light appearance. The harness and its runner are uninstalled.

Accepted without the pool join flow (2.2) by the user on 2026-10-14.
Accepted without scroll retention under the delete loader on iOS 18.6 (2.2) by the user on 2026-10-14.
Accepted that the native navigation bar stays sharp outside the loader on the pool creator and sign-in screens while pool deletion blurs pool home's own bars (2.2) by the user on 2026-10-14.

## Android — tasks 3.1–3.5 (2026-10-14)

### Environment

- `adb devices -l` listed two devices, both chosen by you for this group:
  - **No-blur:** Samsung Galaxy S8+ SM-G955F, `ce0417143a6ab8130c`, Android 9 / API 28, 1080 × 2220 px, density 420. Signed in to Prod. This branch's Prod debug build was installed over the existing Fortuna 2.0.0 (12) with `adb install -r`, which kept the data and the sign-in.
  - **Blur:** `emulator-5554`, Pixel_10_Pro_XL, API 37, 1344 × 2992 px, density 480. Fortuna Prod debug installed and signed out.
- **API 29–36, including the 30/31 boundary:** unavailable. You declined to create another AVD.
- Observation used only the `android` CLI (`android layout --flat [--no-idle]`, `android screen capture`). Navigation used `adb shell input` and `am start`.

### 3.1 Inventory and exploration

The production `LoadingContainerView` callers are the eight call sites in the group 1 table, unchanged. Every caller except `PoolHomeView` and the join state of `PoolJoinerView` still draws the screen as a sibling.

Exploration before the tests were written:
- The Samsung pool list, pool creator, drawer, and "¿Eliminar polla?" dialog have the same text locators and coordinates as in group 2. Exceptions:
  - **Eliminar polla** is now at (459, 1147).
  - The first pool row exposes one merged content description, for example "Copa Mundial de la FIFA 2026, Puesto 8, 589 puntos, 13 jugadores, Puesto sin cambios".
- The CLI reports the loading host's status as one node, `desc='Cargando'` (API 28) or `desc='Loading'` (API 37). This is what the tests locate.
- A throwaway instrumented harness was used to hold loading for device checks. It was not committed and has been deleted. It lived at `ui/src/androidTest/.../loading/G3DeviceCaptureHarness.kt` and ran as `com.felipearpa.tyche.ui.test` through `am instrument`. It drew an edge-to-edge `ComponentActivity` with `TycheTheme` (light or dark) and one of two screens:
  - inside `LoadingContainerView`, a Scaffold "Sign in" form: Email field, a counting "Submissions: N" button, and 60 rows;
  - a standalone host.
- **Locators:** text and content description only. In the committed tests, coordinates are used only for the covered button's frame, measured from its own semantics node before loading starts.

### 3.2 Native blur and the API 28 path

`LoadingContainerView(isLoading, modifier, content)` always composes `content` in the same slot. While loading:
- **Blur:** on API 31+ the content gets `androidx.compose.ui.draw.blur(6.dp, BlurredEdgeTreatment.Rectangle)`. Below API 31, an explicit `Build.VERSION.SDK_INT >= S` check returns the modifier unchanged. The radius is the central `LoadingBlurRadius`.
- **Background:** the host paints no background. A host without content, `LoadingContainerView(isLoading)`, shows the ball on the root `Surface` background.
- **Cloudy:** no longer used by the host. Its dependency, catalog entries, and `loadingBackground` theme colors remain for 4.3.
- **SDK levels:** minimum SDK stays 28 (`projectMinSdk=28`).

| Check | API 28 (Samsung) | API 37 (emulator) |
| --- | --- | --- |
| Harness form loading, light | Form fully sharp on white, no veil, ball sharp (`g3-android-api28-harness-light-loading.png`) | Form softly blurred on white, including the top bar inside the host; no veil; ball sharp (`g3-android-api37-harness-light-loading.png`) |
| Harness form loading, dark | Sharp on #121212 (`g3-android-api28-harness-dark-loading.png`) | Blurred on #121212 (`g3-android-api37-harness-dark-loading-3button.png`) |
| Standalone, light and dark | Ball on white or #121212 (`g3-android-api28-standalone-{light,dark}.png`) | Same (`g3-android-api37-standalone-{light,dark}.png`) |
| Ball color in standalone captures (pixel counts) | #2E7D32 light, #4CAF50 dark | Same |

Build: `./gradlew :app:assembleProdDebug :ui:assembleDebugAndroidTest` → success.

Real Fortuna on the Samsung (API 28):
- **Throwaway pool:** "zz loading probe g3 2026-10-14", template "Fortuna QA — kickoff groups", id `01M4Y87CWQ82215RZWHPJ8QTJ8`.
  - Created at 17:23:47.
  - Deleted at 17:24:59–17:25:00: `DELETE /pools/01M4Y87CWQ82215RZWHPJ8QTJ8?gamblerId=…` → `204` (Ktor logcat, no headers kept).
  - Afterward, `android layout` of "Mis pollas" lists only "Copa Mundial de la FIFA 2026", "Copa Mundial de la FIFA prur", and "Prueba".
- **Creation:** still the sibling pattern (`LoadingContainerView(isLoading = true) {}` over the form) until 4.2. The step-two form with the typed name stays sharp under the centered ball, with no gray veil and the keyboard closed (`g3-android-api28-fortuna-pool-create-loading-interim.png`). The old veil is gone from every sibling caller.
- **Deletion:** pool home is wrapped by the host. Puntajes, the tab bar, and the top bar stay sharp under the ball, with no veil (`g3-android-api28-fortuna-pool-delete-loading.png`). The app then returns to "Mis pollas".

### 3.3 Tyche ball and motion

`BallSpinner` is unchanged apart from its doc comment:
- the same `R.drawable.tyche_logo` (group 1 matched it to `logos/TycheLogoBall.svg`);
- 64 dp;
- `MaterialTheme.colorScheme.primary`;
- a linear `rememberInfiniteTransition` turn every 2000 ms.

Compose's `InfiniteTransition` (animation-core 1.11.1 source):
- Duration scaling: it divides play time by the live `MotionDurationScale`.
- Scale 0: it snaps to the end value (360°, upright) and suspends until the scale is nonzero again.
- Removal: the transition is disposed with the loader layer when loading ends.

| Check | API 28 | API 37 |
| --- | --- | --- |
| Normal motion while loading (MD5 of the ball crop, consecutive captures) | 3 different | 2 different |
| `animator_duration_scale 0` set live while loading | 3 identical crops, ball upright (`g3-android-api28-harness-dark-animations-off.png`) | 2 identical, upright (`g3-android-api37-harness-dark-animations-off.png`) |
| 0.5 set live | 3 different crops (it turns) | Not run |
| Restored live (1.0 on the Samsung, deleted on the emulator) | 3 different crops (rotation resumed) | 2 different |

The exact rate under scaling is covered by instrumented tests (3.5): 72° after 400 ms at 1x, 144° after 400 ms at 0.5x, and upright at 0.

Animation teardown: after loading ended, `android layout` no longer lists the status and the ball is gone from captures. The frame callbacks stopping was not measured on a device.

### 3.4 Placement, isolation, status

Host behavior:
- A transparent full-size layer consumes every pointer event in the Initial pass.
- **Focus:** while loading, focus cannot enter the content (`focusProperties { onEnter = cancelFocusChange() }` on a `focusGroup`). If the content holds focus when loading starts, the host clears it, which closes the keyboard.
- **Accessibility:** while loading, the content gets `clearAndSetSemantics {}`. The status node has the content description `R.string.loading_status` (en "Loading", es and es-ES "Cargando"), `ProgressBarRangeInfo.Indeterminate`, and a polite live region. It holds no changing value, so rotation does not re-announce.
- **Placement:** the ball is centered inside `windowInsetsPadding(WindowInsets.safeDrawing)`.
- **Back:** system Back is not intercepted.

Harness sequence on both devices: tap Email, type "abc" with the keyboard up, then wait for loading to start. While loading, type "xyz", tap Submissions, tap Email, type "q", swipe the list, and press Tab and Enter. Then wait for loading to end.

| Check | API 28 | API 37 |
| --- | --- | --- |
| Keyboard when loading begins | Closed (`mInputShown=false`) | Closed (`mImeShowing=false`). One capture caught the keyboard mid-hide with the ball above it (`g3-android-api37-harness-light-loading-keyboard-closing.png`) |
| Accessibility tree while loading (`android layout`) | Only `desc='Cargando'` at [456,995][624,1163] for the app (`g3-android-api28-harness-loading-layout.json`) | Only `desc='Loading'` at [576,1444][768,1636] (`g3-android-api37-harness-loading-layout.json`) |
| After loading ends | Field "abc" (unfocused), "Submissions: 0", Row 0 at the same bounds; a tap then made "Submissions: 1" | Field "abc" (unfocused), "Submissions: 0", Row 0 at the same bounds |
| Ball center vs. safe area | (540, 1079); safe area y 63–2094 → 1078.5 (three-button) | (672, 1540); safe area y 159–2920 → 1539.5 (gestures) |
| Other navigation mode | Full-screen gestures (`navigation_bar_gesture_while_hidden=1`): ball center y 1122, between the status bar (63) and the 39 px gesture-hint inset (`g3-android-api28-harness-light-loading-gestures.png`) | Three-button (overlay `navbar.threebutton`): status at [576,1408][768,1600], center 1504; safe area 159–2848 → 1503.5 |

Notes:
- **Covered controls look enabled on Android.** The Submissions button keeps its enabled green, unlike iOS, where covered controls turn gray. The Android host blocks input instead of disabling the content.
- **The top bar follows the host.** In the harness and in pool home, the top bar is inside the host, so it is blurred on API 37. In the sibling callers (sign-in, pool creation), the overlay covers the whole Box, top bar included, which blocks its back button. Group 4 decides the final structure per screen.

### 3.5 Tests and contract

New instrumented tests in `Android/ui/src/androidTest/java/com/felipearpa/tyche/ui/loading/`:
- `LoadingContainerViewTest` (8 tests), using `LoadingHostHarness`:
  - idle host interactive with no status;
  - covered button tap does nothing;
  - covered list swipe keeps its position;
  - a focused field loses focus when loading begins, and keys (X, Tab, Y, Enter) reach nothing;
  - while loading, the merged tree has no form, button, or row nodes and exactly one indeterminate "Loading" status;
  - toggling loading twice keeps the same screen (created once, never disposed), the typed value, and the scrolled list index, and Submit works afterward;
  - Tab cannot move focus into the screen while loading but can afterward;
  - a full-window host centers a 64 dp ball in the `safeDrawing` area.
- `BallSpinnerMotionTest`: normal (72° after 25 frames), fast at 0.5x (144°), and disabled (upright). Each compares pixels with still reference balls drawn beside the spinner.

The test-only unmerged tree still lists the Submit text under `clearAndSetSemantics`. The CLI, which reads the real accessibility tree, shows only the status. The accessibility assertions therefore use the merged tree.

Commands:

```sh
cd Android
./gradlew :app:assembleProdDebug :ui:assembleDebugAndroidTest
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.felipearpa.tyche.ui.loading
```

Results:
- First run: 9 of 11 passed. The safe-area test was rewritten from a partial-window variant, and the accessibility test was moved to the merged tree.
- Those two tests, rerun: 2 of 2 passed.
- Final full run after removing the harness: **BUILD SUCCESSFUL**, 11 of 11 passed on `emulator-5554`.
- `./gradlew :app:assembleProdDebug :ui:testDebugUnitTest :app:testProdDebugUnitTest` → success.

Not verified:
- **TalkBack.** Turning TalkBack on (adding it to `enabled_accessibility_services`) was refused by the session's permission system, so no TalkBack traversal, announcement, or recovery was observed. The `android layout` accessibility trees and the merged-tree tests stand in for it.
- **API 29–36, including the 30/31 boundary:** no device.
- **The rotation rate on devices.** Captures cannot time it; the frame-clock tests cover it.
- **A hardware keyboard on the devices.** Keys were sent through `adb input` and the instrumented tests.
- **Rotation or resizing while loading** on Android.

### Settings and device state

- **Samsung, before:** animation scales 1.0/1.0/1.0, three-button navigation (`navigation_bar_gesture_while_hidden=0`), accessibility services `com.artemis.helper/.ArtemisAccessibilityService`.
  - Changed during the checks: `animator_duration_scale` (0, then 0.5) and gestures (1).
  - Restored to the values above.
- **Samsung popup.** After the gesture switch, One UI left an invisible "Ventana emergente" window ("Deslice hacia arriba y mantenga pulsado para abrir la aplicación de asistencia…"). `android layout` now reports it instead of the app window. Home and relaunch did not clear it.
- **Samsung packages:** `com.felipearpa.tyche.ui.test` was installed and then uninstalled. The protected `.test` packages are untouched. Fortuna (this branch's build) is signed in on "Mis pollas".
- **Emulator, before:** gestural navigation, `animator_duration_scale` unset. Both restored (three-button overlay switched back to gestural; the scale deleted). The UI test package is uninstalled by Gradle after the run. Fortuna is unchanged and signed out.

### Interim state of the callers until group 4

The callers changed only to compile against `isLoading`:

| Caller | Change | Appearance until group 4 |
| --- | --- | --- |
| `EmailSignInView`, `EmailAndPasswordSignInView`, `PoolFromLayoutCreatorView`, `HomeNavViewProvider` | `LoadingContainerView(isLoading = true) {}` as a sibling overlay | Sharp screen under the ball on every API level, with no veil or blur. Taps are blocked across the whole screen, including the top bar. Covered content stays in the accessibility tree, and a focused field keeps focus |
| `EmailLinkSignInView`, `PoolJoinerView` initial load | `LoadingContainerView(isLoading = true)` (no content) | Ball on the root background (final form) |
| `PoolJoinerView` join, `PoolHomeView` deletion | `LoadingContainerView(isLoading = true) { … }` inside the existing branch switch | Full host behavior (blur on 31+), but the branch switch still recreates the subtree |

Accepted without API 29–36 devices and the API 30/31 boundary (3.1) by the user on 2026-10-14.
Accepted without timing the ball's rotation on the devices and without measuring that animation frames stop after loading ends (3.3) by the user on 2026-10-14.
Accepted without real TalkBack traversal, announcement and recovery (3.4) by the user on 2026-10-14.
Accepted without a keyboard held visible during loading and without rotation or window resizing while loading (3.4) by the user on 2026-10-14.
Accepted without a failing-first run of the new tests against the old Cloudy host (3.5) by the user on 2026-10-14.

## Android — tasks 4.1–4.4 (2026-10-14)

Each caller now passes its existing screen into `LoadingContainerView(isLoading = …) { … }`. The shared host is unchanged from group 3. View models, steppers, navigation, and the flags that drive loading are unchanged; no device walkthroughs were run (design, Migration Plan step 4).

### 4.1 and 4.2 Callers

| Caller | Before (interim) | Now |
| --- | --- | --- |
| `EmailSignInView` | `Box { Scaffold; if (isLoading) LoadingContainerView(isLoading = true) {} }` | `LoadingContainerView(isLoading = viewState.isLoading()) { Scaffold }` |
| `EmailAndPasswordSignInView` | Same sibling pattern, overlay while loading or loaded | `LoadingContainerView(isLoading = isOverlayVisible) { Scaffold }`, same flag |
| `EmailLinkSignInView` | `LoadingContainerView(isLoading = true)` while idle or loading | Unchanged: there is no prior content, so the ball shows on the root background |
| `HomeNavViewProvider` (`HomeScreen`, Google sign-in) | `Box { HomeView; if (isBusy) LoadingContainerView(isLoading = true) {} }` | `LoadingContainerView(isLoading = googleState.isBusy) { HomeView }` |
| `PoolFromLayoutCreatorView` | `Box { Scaffold; if (isOverlayVisible) LoadingContainerView(isLoading = true) {} }` | `LoadingContainerView(isLoading = isOverlayVisible) { Scaffold }` |
| `PoolJoinerView` initial load | `LoadingContainerView(isLoading = true)` | Unchanged (no prior content) |
| `PoolJoinerView` join | `Idle` and `Saving`/`Saved` were separate `when` branches, so starting the join recreated the screen | One branch for `Idle`, `Saving` and `Saved`: `LoadingContainerView(isLoading = joinPoolState != Idle) { PoolLoadContainer }`, with the join and abort callbacks emptied while joining, as before |
| `PoolHomeView` deletion | `if (isSaving) LoadingContainerView(isLoading = true) { scaffoldContent() } else scaffoldContent()` | `LoadingContainerView(isLoading = isSaving) { Scaffold }`, so starting deletion no longer recreates the scaffold |

Pre-existing and not changed: `PoolFromLayoutCreatorView` still renders `Stepper` from separate `when (state)` branches for `Idle`, `Mutating`/`Mutated` and `Failure`, so the stepper subtree is recreated when the mutation state changes. The draft (`createPoolModel`) and the step are held above the host and survive.

### 4.3 Cloudy and loading-background removal

Removed:
- `implementation(libs.cloudy)` from `Android/ui/build.gradle.kts`;
- the `cloudy` version and library entries from `Android/gradle/libs.versions.toml`;
- `ExtendedColorScheme.loadingBackground` and its light/dark values (`lightLoadingBackgroundColor` `#66BDBDBD`, `darkLoadingBackgroundColor` `#66A5D6A7`).

Search before removal (`grep -rniE "cloudy|loadingBackground|LoadingBackgroundColor"` over `Android/`, excluding `build`): only those declarations matched; no Kotlin source imported Cloudy or read `loadingBackground`. The same search after removal finds nothing in `Android/`; across the repository only this change's planning and verification files mention them.

Dependency resolution:
- `./gradlew :ui:dependencies --configuration debugRuntimeClasspath` → success, no Cloudy.
- `./gradlew :app:dependencies --configuration prodDebugRuntimeClasspath` → success, no `cloudy`. The only `com.github.skydoves` artifact left is `compose-effects:0.1.4`, a transitive dependency of `com.revenuecat.purchases:placeholder`.
- Minimum SDK: `projectMinSdk=28`; the merged `prodDebug` manifest declares `minSdkVersion="28"`.

### 4.4 Commands and results

```sh
cd Android
./gradlew :app:assembleProdDebug :account:testDebugUnitTest :pool:testDebugUnitTest \
  :ui:testDebugUnitTest :app:testProdDebugUnitTest :ui:assembleDebugAndroidTest
ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.felipearpa.tyche.ui.loading
```

- Build and unit tests: **BUILD SUCCESSFUL**. app 60, account 85, pool 5, ui 47 tests; 0 failures, 0 skipped.
- Instrumented loading-host tests on `emulator-5554` (Pixel_10_Pro_XL, API 37): the first invocation failed after 10 s with one run-level failure and no test executed; the cause was not captured. The rerun: **BUILD SUCCESSFUL**, 11 of 11 passed (`TestRunner: run finished: 11 tests, 0 failed`).
- Nothing was run on or installed to the Samsung. The emulator's test package is removed by Gradle after the run; Fortuna (`com.felipearpa.fortuna`) is untouched.

Not run in this group (by decision): per-flow device walkthroughs.

### Follow-up: pool creator single stepper

This supersedes the "Pre-existing and not changed" note under 4.1 and 4.2. `PoolFromLayoutCreatorView` now renders one `Stepper` for every mutation state inside its single `LoadingContainerView(isLoading = isOverlayVisible)`, matching iOS:
- Only `Idle` accepts edits. In `Mutating`, `Mutated` and `Failure`, step changes, save and model changes are ignored, as they were in the old branches.
- `Failure` still shows `ExceptionAlertDialog`, whose dismissal calls `reset()`.
- The success handoff is unchanged. It is `LaunchedEffect(state)` at the top of the composable, not tied to a branch, so it fires once per distinct `Mutated` state.
- View models, navigation, and the draft (`createPoolModel`) and step ownership are unchanged.

New instrumented test `Android/pool/src/androidTest/java/com/felipearpa/tyche/pool/creator/PoolCreatorLoadingRetentionTest.kt` (2 tests):
- An unsaved name typed on the name step ("Office pool", replacing the preselected "Champions League") survives Mutating → Failure → dialog Done (one `reset`) → Idle.
- `Mutated` with a pool id followed by three forced recompositions calls `onPoolCreated` exactly once.

Commands and results:
- `./gradlew :pool:testDebugUnitTest :app:assembleProdDebug :pool:assembleDebugAndroidTest` → BUILD SUCCESSFUL (pool unit tests 5/5).
- `ANDROID_SERIAL=emulator-5554 ./gradlew :pool:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.felipearpa.tyche.pool.creator.PoolCreatorLoadingRetentionTest` → 2/2 passed.
- Failing-first: with the three `when (state)` branches temporarily restored, the retention test failed (`expected:<[Office pool]> but was:<[Champions League]>`) and the handoff test passed, since that behavior did not change. After restoring the fix, the rerun passed 2/2.
- Fortuna on the emulator is untouched. Nothing ran on the Samsung.

## iOS and Android — tasks 5.1–5.2 (2026-10-14)

### Environment

- iOS: iPhone 16 Pro Max, iOS 18.6 (`5F3DAAC7-…`), and iPhone 18 Pro Max, iOS 27.0 (`D8F12B93-…`), both 440 × 956 pt at 3x (1320 × 2868 px). Built with Xcode 27.0 (`DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer`).
- Android blur: `emulator-5554`, Pixel_10_Pro_XL, API 37, 1344 × 2992 px, density 480 (3x, 448 dp wide).
- Android no-blur: Samsung SM-G955F `ce0417143a6ab8130c`, API 28, 1080 × 2220 px, density 420 (2.625x).
- Fortuna was not used, reinstalled, or signed in or out on any device. No Prod data was read, created, changed, or deleted.

### 5.1 Cross-platform comparison

Throwaway harnesses (not committed, removed afterward) drew the same screen on both platforms inside the production host, so the comparison uses matching content at the same 3x scale on iOS and the API 37 emulator:
- a "Sign in" headline, an outlined field showing "gambler@example.com", a green "Sign in" button, and 14 "Match N" rows tinted with the accent at 15%;
- three modes: the form idle (reference), the form loading (content-backed loader), and `LoadingContainerView(isLoading: true)` without content (standalone loader).

The iOS harness was a scratchpad Xcode app linking the `UI` package (`LoadingContainerView` and `BallSpinner` as shipped) and copying the app's `AccentColor`, launched with `-mode` and themed with `simctl ui appearance`. The Android harness was a temporary instrumented test, `ui/src/androidTest/.../ui/g5/G5CompareHarness.kt`, drawing `TycheTheme(darkTheme)` + `Surface` edge-to-edge and run through `am instrument` (`com.felipearpa.tyche.ui.test`).

Captures (`verification/`), where `<p>` is `ios186`, `ios27`, `android-api37`, or `android-api28`:
- `g5-<p>-{light,dark}-content-idle.png`
- `g5-<p>-{light,dark}-content-loading.png`
- `g5-<p>-{light,dark}-standalone.png`
- `g5-android-api37-{light,dark}-content-loading-6dp.png`: API 37 before calibration.

#### Blur strength

Method: along a pixel column through the rows (no text), the loading capture was fitted as `gain × Gaussian(idle, σ) + offset`, and the best σ in pixels is reported. All fits had an RMS error under 0.5 grey levels. iOS and the emulator are both 3x, so pixels compare directly; σ / 3 is in points or dp.

| Capture | Radius | Effective σ (px) | σ (pt/dp) |
| --- | --- | --- | --- |
| iOS 18.6 and iOS 27, light and dark | 6 pt | 17.75 | 5.9 |
| API 37 light / dark, before | 6 dp | 10.75 / 10.75 | 3.6 |
| API 37 light / dark, after | 10 dp | 18.0 / 17.25 | 5.9 |
| API 28 light and dark | none (no blur) | 0 (content pixels identical to idle) | — |

With 6 dp, Android was visibly weaker than iOS: the "Sign in" headline and the "Match N" labels could still be read through the blur (`g5-android-api37-*-content-loading-6dp.png`), while on iOS they could not. The measured ratio is what the renderers predict: SwiftUI's radius acts as the Gaussian sigma (6 pt → σ ≈ 18 px), while Android's blur converts a radius r into σ ≈ 0.577 r + 0.5 (18 px → σ ≈ 10.9 px).

Calibration, in the central constants only:

| Platform | Constant | Before | After | Reason |
| --- | --- | --- | --- | --- |
| iOS | `blurRadius` in `iOS/UI/Sources/UI/Loading/LoadingContainerView.swift` | 6 pt | 6 pt (unchanged) | Reference strength: covered text becomes illegible, but layout and colors stay recognizable |
| Android | `LoadingBlurRadius` in `Android/ui/src/main/java/com/felipearpa/tyche/ui/loading/LoadingContainerView.kt` | 6 dp | **10 dp** | Matches iOS's effective sigma (σ ≈ 17.8 px at 3x); 6 dp was about 60% of the iOS strength |

**Final values: iOS 6 pt, Android 10 dp (API 31+; no blur below API 31).** After the change, the API 37 loading captures look equivalent to the iOS ones in both themes. With `BlurredEdgeTreatment.Rectangle` the screen edges show no halo or clipping on either platform. Since both values are in points or dp, the match holds at other densities.

#### Ball geometry, color, and legibility

Measured on every content-loading and standalone capture:

| | iOS 18.6 / 27 | API 37 | API 28 |
| --- | --- | --- | --- |
| Dominant ball color, light / dark | #2E7D32 / #4CAF50 | #2E7D32 / #4CAF50 | #2E7D32 / #4CAF50 |
| Ball diameter | 187–188 px = 62.3–62.7 pt (64 pt frame; the silhouette fills 62.3/64 of the 500 × 499 viewBox) | 187–188 px = 62.3–62.7 dp | 163–164 px = 62.1–62.5 dp |
| Ball center | (660, 1475) of 1320 × 2868 (safe area, below the status bar) | (672, 1540) of 1344 × 2992 | (540, 1078) of 1080 × 2220 |
| Contrast of the ball against its adjacent background, light (minimum) | 4.38:1 over the blurred form, 5.13:1 standalone | 4.21:1 / 5.13:1 | 4.21:1 / 5.13:1 |
| Same, dark (minimum) | 6.68:1 over the blurred form, 7.56:1 standalone (black) | 5.49:1 / 6.74:1 (#121212) | 5.49:1 / 6.74:1 |

- A side-by-side crop of the iOS and Android balls shows the same silhouette and panels at different rotation angles. Android draws about 7% fewer fully saturated ball pixels; its edges are anti-aliased, while iOS edges are harder. This is renderer anti-aliasing, not a geometry difference.
- All contrast values are above the 3:1 needed for a meaningful graphic, including over the sharp tinted rows on API 28, so the ball stays legible without a card or scrim.
- **API 28 fallback:** comparing the idle and loading captures pixel by pixel, the only differing region is the ball's box ([457,996]–[622,1161] px). The form, its colors, and the background are unchanged; there is no blur, veil, or tint.
- **No added layer:** the standalone captures show only the ball on the theme background: iOS white or black (`systemBackground`); Android white or #121212 (`Surface`).

#### Cross-platform difference (not changed)

On iOS, the host's `.disabled` makes covered controls use their disabled style, so under the blur the green button and accent-tinted rows turn gray (`g5-ios*-*-content-loading.png`). On Android, the host blocks input without disabling the content, so covered controls keep their colors under the blur. Group 2 (2.1) and group 3 (3.4) recorded this; the user accepted it on 2026-10-14 (follow-up below).

#### Checks after calibration

- `./gradlew :ui:assembleDebugAndroidTest :app:assembleProdDebug :ui:testDebugUnitTest` → BUILD SUCCESSFUL.
- `ANDROID_SERIAL=emulator-5554 ./gradlew :ui:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.felipearpa.tyche.ui.loading` → BUILD SUCCESSFUL, 11 of 11 passed.
- iOS code did not change. Nothing iOS-related was rebuilt apart from the harness, and no `Package.resolved` changed.

### 5.2 Reconciliation

#### Call-site inventory

A repository search for production `LoadingContainerView(` calls (excluding tests and the host itself) finds the same eight sites per platform as in the design table, each wrapping its actual screen or intentionally using the standalone host:

| Flow | iOS | Android | Form |
| --- | --- | --- | --- |
| Send email link | `EmailSignInView` | `EmailSignInView` | Wraps the form |
| Email/password sign-in | `EmailAndPasswordSignInView` (loading or loaded) | `EmailAndPasswordSignInView` (`isOverlayVisible`) | Wraps the form |
| Email-link verification | `EmailLinkSignInView` | `EmailLinkSignInView` | Standalone (no prior content) |
| Google sign-in | `HomeRouter` | `HomeNavViewProvider` | Wraps the welcome screen |
| Pool creation | `PoolFromLayoutCreatorView` | `PoolFromLayoutCreatorView` | Wraps one stepper |
| Pool join | `PoolJoinerView` (joining or joined) | `PoolJoinerView` (`isJoining`) | Wraps the loaded pool |
| Initial pool load | `PoolJoinerView` | `PoolJoinerView` | Standalone (no prior content) |
| Pool deletion | `PoolHomeRouter` | `PoolHomeView` | Wraps pool home |

Inline `BallSpinner` users remain outside the host: iOS `UsernameEditor`, `ProfileView`, `PendingBetItemView`, `ManageGamblerItem`; Android `UsernameEditor`, `PendingBetItemView`, `ManageGamblerItem`. The working-tree diff touches no placeholder, inline-progress, refresh, pagination, splash, or navigation code.

#### Native APIs, dependencies, and minimum versions

- iOS: SwiftUI `View.blur(radius:)` is the only effect in `iOS/UI/Sources/UI/Loading/`. No material, opacity, or black layer remains.
- Android: `androidx.compose.ui.draw.blur` behind an explicit `Build.VERSION.SDK_INT >= S` check. No Kotlin, Gradle, or catalog file mentions Cloudy or `loadingBackground`.
- Minimum versions are unchanged: `projectMinSdk=28` (merged `prodDebug` manifest `minSdkVersion="28"`); every iOS package declares `.iOS(.v16)`, and the app target `IPHONEOS_DEPLOYMENT_TARGET = 16.0`.
- Preserved flow contracts:
  - result callbacks fire once: iOS `aSuccessfulEmailAndPasswordSignInIsReportedOnce` and `PoolCreatorLoadingRetentionTests`; Android `PoolCreatorLoadingRetentionTest`;
  - request triggers, view models, navigation, and error dialogs are unchanged per the group 2 and group 4 notes;
  - System Back is not intercepted on Android;
  - the iOS Google prompt is never covered (group 2, 2.1).

#### Requirement-by-requirement evidence

| Requirement / scenario | Evidence | Status |
| --- | --- | --- |
| **Existing loading screens share one presentation** — An existing blocking operation begins | Inventory above. iOS: harness flows and real pool create/delete on iOS 18.6 and 27 (2.1, 2.2, real pool flows); callback-once tests (2.3). Android: real API 28 create/delete with the shared host (3.2, interim sibling form); caller migration compiled and tested (4.1–4.4); single-handoff test (4.x follow-up) | Verified for the host and iOS pool flows. Accepted without real backend sign-in submissions (2.1), without the pool join flow (1.1, 2.2), and without Android per-flow walkthroughs after group 4 (design, Migration Plan step 4) |
| — A list or inline operation is loading | iOS real placeholders and inline bet saving (real pool flows, `g2-ios*-pool-*placeholders*`, `*-bet-saving-1`); Android placeholders after deletion on API 28 (`g3-android-api28-fortuna-pool-list-after-delete.png`, 2.3 `g2-android-api28-pool-list-after-delete-placeholders.png`); no placeholder or inline code changed | Verified on iOS; Android inline progress was not observed on a device after the change (code unchanged) |
| **Content blur is the only loading background effect** — Loading over a form on a blur-capable platform | 5.1 `g5-ios*-*-content-loading`, `g5-android-api37-*-content-loading`; 1.2 `ios-host-form-loading-*`; 3.2 `g3-android-api37-harness-*-loading*`; source review above | Verified |
| — Comparing themes and platforms | 5.1 blur measurements and calibration (final iOS 6 pt, Android 10 dp); ball contrast table | Verified. Accepted that covered controls show the disabled style (grey) on iOS while Android keeps their colors (follow-up below) |
| **Older Android omits blur without a visual substitute** — Loading on Android 9 through 11 | 5.1 API 28 pixel diff (only the ball differs) and `g5-android-api28-*`; 3.2 harness and real create/delete; 3.4 input isolation on API 28; minimum versions above | Verified on API 28. Accepted without API 29–36 and the 30/31 boundary (3.1) |
| **The shared Tyche ball remains the loading indicator** — Observing normal loading | 5.1 color, diameter, and center; asset match (1.3, 3.3); `BallSpinnerRotationTests` (1.3) and `BallSpinnerMotionTest` (3.5); normal-motion crops change (1.3, 2.4, 3.3) | Verified. Accepted without timing the rotation on devices (3.3) |
| **Loading preserves the existing screen and supports an empty background** — A submitted form returns a failure | iOS harness failure/retry and `SignInLoadingRetentionTests`, `PoolCreatorLoadingRetentionTests` (2.1–2.3); iOS delete loader over scrolled content (real pool flows, iOS 27); Android `LoadingContainerViewTest` toggling (3.5) and `PoolCreatorLoadingRetentionTest` failure path (4.x follow-up) | Verified via tests and harnesses. Accepted that iOS may commit a pending autocorrection when loading drops focus (1.4); accepted without scroll retention on iOS 18.6 (2.2); Android sign-in failure/retry not run on a device (group 4 decision) |
| — Loading starts without prior content | 5.1 `g5-*-standalone`; 2.1 `g2-ios*-standalone-*`; 3.2 `g3-android-*-standalone-*` | Verified (harness; the real email-link and initial pool-load screens were not reached) |
| **Loading isolates covered controls and announces its state** — Input reaches covered content | iOS XCUITest taps, drags, typing, hardware keys (1.4); Android harness taps, swipes, typing, Tab/Enter on API 28 and 37 (3.4) and instrumented tests (3.5) | Verified, including both blur states on Android |
| — Assistive technology encounters loading | iOS accessibility tree and audit (1.4, 2.4), `LoadingContainerViewTests` on iOS 18.6 (2.3); Android `android layout` trees with one status (3.4) and merged-tree tests (3.5) | Accepted without real VoiceOver (1.4, 2.1, 2.4) and real TalkBack (3.4) |
| — An external sign-in interface is active | iOS real Google prompt → Cancel, no loader or error (2.1); the iOS host is active only after the provider returns | Verified on iOS. Android Google provider UI not exercised; not verified |
| **Loading placement follows the available screen** — Keyboard or window bounds change | iOS keyboard, rotation, iPad resize and Split View (1.4, 2.4 and its retry); Android safe-area centering in both navigation modes (3.4) and the safe-area test (3.5); 5.1 ball centers | Verified on iOS. Accepted without a held keyboard and without rotation or resizing on Android (3.4). Accepted that the native navigation bar stays sharp outside the loader on the creator and sign-in screens while pool deletion blurs pool home's bars (2.2). Android production top-bar and edge-to-edge appearance after group 4 not observed on a device |
| **Motion preferences and loading completion are respected** — Motion is reduced during loading | iOS live Reduce Motion (1.3, 2.4); Android live `animator_duration_scale 0` (3.3) and the disabled-motion test (3.5) | Verified |
| — Android animation speed is scaled | Live 0.5 on API 28 (3.3); 0.5x rate test (3.5) | Verified |
| — A fast result arrives or the screen leaves | iOS sent screen replaces the loader immediately; handoffs keep the loader until the callback (2.4); Android status and ball gone after loading ends (3.3); the hosts have no minimum duration or delay (source) | Verified. Accepted without measuring that animation frames stop after loading ends (3.3) |

#### Strict validation

`openspec validate unify-native-loading-screens --strict` → `Change 'unify-native-loading-screens' is valid`.

#### Remaining runtime limitations (not verified)

- Previously accepted (see the earlier sections): the Xcode preview canvas; real VoiceOver and TalkBack; a CI run; real backend sign-in submissions; the pool join flow and its initial full-screen load; Android API 29–36 and the 30/31 boundary; rotation timing on devices and frame teardown; a held keyboard and rotation or resizing on Android; iOS 18.6 scroll retention under the delete loader; the Samsung sign-in forms; Android per-flow walkthroughs after group 4.
- Not covered by an earlier acceptance:
  - the Android Google provider UI with the loader;
  - Android inline progress, and production top-bar and edge-to-edge appearance, after the group 4 caller migration;
  - real email-link verification and initial pool-load standalone screens (harness only, on both platforms).

### Device state afterward

- iOS: the harness app is uninstalled from both iPhones, and both are back in light appearance, as they started. Fortuna was not touched.
- Emulator: Gradle removed the UI test package after the run. Fortuna is still installed and signed out. Animation scale stays unset and night mode off; neither was changed.
- Samsung: the UI test package `com.felipearpa.tyche.ui.test` was installed for the captures and then uninstalled. Fortuna and the protected `.test` packages were not touched. Settings were not changed (animator scale 1.0). The phone is on its home screen.
- The temporary `G5CompareHarness.kt` is deleted.

### Follow-up: covered-content colors on iOS (same day, closed)

An experiment removed `.disabled(isLoading)` from the iOS host so covered controls would keep their colors under the blur, with an explicit `resignFirstResponder` on the loading transition in its place. Colors were kept, and touches, the software keyboard and plain hardware keys were still blocked, but on iOS 18.6 and iOS 27 a hardware-keyboard Tab moved focus back into the covered field during loading and later keys were typed into it. The experiment was undone: the host is as it was for the 5.1 captures, `.disabled(isLoading)` included, and `g5-ios*-content-loading.png` (grey covered controls) are the final captures. The host test suite gained `loadingTakesTouchesAwayFromTheCoveredScreen`, a window hit test of the barrier across idle, loading and idle again.

Accepted that covered controls show the system disabled style (grey) under the blur on iOS, while Android keeps their colors, by the user on 2026-10-14.
