# Tasks

## 1. iOS — shared loading presentation

- [x] 1.1 Confirm the full-screen loading inventory and inspect representative iOS form and standalone states on a simulator/device; before authoring any executable tests, also explore the corresponding Android paths with the `android` CLI, using `adb devices -l` and the repository's device-selection rules when needed. Record verified interactions, baseline captures, and device/OS details in the change's verification notes.
- [x] 1.2 Evolve `LoadingContainerView` into a stable, state-driven host using only SwiftUI content blur, remove its material and black dimming layers, and supply the normal themed canvas when content is absent; verify the UI target compiles and light/dark previews show a sharp ball over the intended background with no added layer.
- [x] 1.3 Keep the canonical Tyche ball, existing size/colors, and normal two-second linear rotation, and honor live Reduce Motion changes with a static ball; verify geometry against `logos/TycheLogoBall.svg` and record normal/reduced-motion observations without introducing a replacement spinner.
- [x] 1.4 Implement safe-area/keyboard-aware placement, covered-content input and accessibility isolation, one localized loading status, and state restoration; verify touch, keyboard input, VoiceOver traversal, keyboard-visible placement, and a resized/rotated window on the representative screen.
- [x] 1.5 After the verified exploration in 1.1, add or update only necessary behavioral coverage for loading-state restoration, retained content identity, and interaction/accessibility isolation; use observed interactions and explicit waits, run the focused coverage and affected iOS build, and document the host contract and results with this group's evidence.

## 2. iOS — affected flow integration

- [x] 2.1 Migrate email-link sending, email/password sign-in, email-link verification, and the Google welcome-screen overlay to the shared host around actual screen content; verify submitted values, standalone themed backgrounds, existing success/error routing, and provider cancellation without repeated authentication or navigation callbacks.
- [x] 2.2 Migrate pool creation, initial pool loading/joining, and pool deletion; retain the pool creator's real draft and step instead of constructing a fresh background form, and verify retained values/scroll position through loading, failure, retry, and success handoff.
- [x] 2.3 Explore the affected paths on iOS and, before any new or changed executable tests, the corresponding Android paths with the `android` CLI; update necessary feature regression coverage for draft retention and single result handling, run the affected tests/build, and confirm no production loading caller still depends on an empty sibling overlay or duplicated screen subtree.
- [x] 2.4 Record light/dark captures for a content-backed loader and a standalone loader, normal/reduced motion, VoiceOver recovery, keyboard and window bounds, and operation completion/removal on a supported pre-26 iOS version and iOS 26+; verify placeholders, inline progress, and native navigation styling retain their existing behavior and document any unavailable version checks.

## 3. Android — native blur and compatibility fallback

- [x] 3.1 Confirm the Android call-site inventory and explore representative form/standalone loading paths with the `android` CLI before writing or modifying tests; check attached devices with `adb devices -l`, obtain device selection when required, and record observations on a below-31 device/emulator and a 31+ device/emulator, including the API 30/31 boundary when available.
- [x] 3.2 Replace Cloudy rendering with `androidx.compose.ui.draw.blur` in the state-driven loading host, using the actual production content and an explicit no-blur path below API 31; verify compilation and compare API 31+ blur with sharp content on API 28–30, with no gray/green overlay, scrim, card, or minimum-SDK change.
- [x] 3.3 Retain the canonical Tyche ball, size, colors, and normal two-second rotation; honor disabled animations with a static ball and nonzero system duration scaling, and verify live preference changes, normal motion, and animation teardown on the selected devices.
- [x] 3.4 Implement full-viewport safe placement, clear input isolation, covered-content accessibility exclusion, and one localized loading status; verify focused-field typing, taps, scrolls, TalkBack traversal, keyboard-visible bounds, and gesture/three-button navigation on both blur-capable and no-blur presentations.
- [x] 3.5 After `android` CLI exploration, add or update focused behavioral tests for retained state, input/accessibility isolation, and lifecycle restoration using verified dynamic locators and explicit waits; apply coordinate fallback only when supported and verified, run the relevant UI tests/build, and document this group's host contract and evidence.

## 4. Android — affected flow integration and dependency cleanup

- [x] 4.1 Change email-link sending, email/password sign-in, email-link verification, and `HomeNavViewProvider` to pass their actual screen content into the shared host instead of drawing it beside an empty loader, without other flow changes; verify the affected modules compile.
- [x] 4.2 Change pool creation, initial pool loading/joining, and pool deletion to pass their actual screen content into the shared host, without other flow changes; verify the affected modules compile.
- [x] 4.3 Remove unused Cloudy imports, the UI-module dependency and version-catalog entries, and obsolete loading-background theme values after checking remaining consumers; verify a repository search finds no obsolete consumers, dependency resolution succeeds, and affected modules plus the Android app build with API 28 support intact.
- [x] 4.4 Run the affected Android unit tests, the shared loading host's instrumented tests, and the app build; per-flow device walkthroughs are not required.

## 5. iOS and Android — integration comparison

- [x] 5.1 Capture a content-backed and a standalone loader in light and dark on iOS and on Android API 31+ and below 31, compare that they look right and consistent across platforms (ball geometry/color, comparable blur, sharp older-Android fallback, ball legibility), and calibrate the central blur radii only if needed; record the final values.
- [x] 5.2 Reconcile the call-site inventory and accumulated evidence against every `loading-screens` requirement, confirm native APIs and preserved flow contracts, and run strict OpenSpec validation; record any remaining runtime verification limitations without marking unsupported checks complete. Specification synchronization and archival occur separately after implementation.
