# Tasks

All implementation work is Android-only; backend and iOS require no changes. Record device configurations and observed results in this change's verification.md as each group is completed.

## 1. Android — Activity configuration and username editor

- [x] 1.1 Set the app's explicit target SDK to 36, retain its minimum SDK and existing ComponentActivity edge-to-edge setup, and add manifest adjustResize; verify the merged manifest for local and prod variants and successful app compilation.
- [x] 1.2 Replace UsernameEditorScreen's runtime soft-input adjustment with consumed scaffold insets and the design's IME strategy, placing any imePadding before scrolling; verify both editor entry contexts keep the back control visible and Save reachable in a short window with the keyboard open.
- [x] 1.3 Update or add focused username-editor UI regression coverage for keyboard visibility, in-flow Save, draft/selection preservation, and save/failure back-button behavior; run the relevant tests and record results in verification.md.

## 2. Android — Standalone lists and shared padding propagation

- [x] 2.1 Forward scaffold PaddingValues through pool-score, bet-timeline, match-bet, Manage gamblers, and pool-template list APIs to scrolling content, consuming them at the list boundary; verify first/last items can be fully revealed and list viewports no longer end at outer system-inset margins.
- [x] 2.2 Apply the same inset policy to loaded, loading, empty, and error branches, preserving fixed headers, refresh indicators, pagination, and production-row placeholders with shared shimmer; verify each state and ensure placeholder values are inaccessible and non-actionable.
- [x] 2.3 Add or update focused UI coverage for list endpoints and recovery-action reachability, run the affected list tests, and document the list inset owners and observed state checks in verification.md.

## 3. Android — Pool-home tabs and editable bets

- [x] 3.1 Change the pool-home content slot to pass padding to each tab instead of applying it to an enclosing Box; forward it through gambler-score, pending-bet, and finished-bet lists and verify safe scroll endpoints on all tabs.
- [x] 3.2 Make pending-bet score entry respect keyboard bounds without stacking tab, navigation-bar, and IME heights; verify focusing a score near the end of the list reveals it and preserves access to required bet actions.
- [x] 3.3 Draw the bottom-tab surface through the navigation-bar area with controls inset inside that surface; verify continuous background and safe tab targets in both themes and navigation modes.
- [x] 3.4 Add or update focused pending-bet keyboard regression coverage, run it, and record tab/IME overlap and keyboard-dismissal results in verification.md.

## 4. Android — Remaining forms

- [x] 4.1 Apply the design's consumed-inset and IME strategy to email and email/password sign-in, adding scrolling where needed; verify every field, validation message, and sign-in action is reachable with the keyboard open in landscape and at large font scale.
- [x] 4.2 Apply the same policy to the pool-name creation step without disrupting template selection or step navigation; verify the field and creation action remain reachable and the draft survives keyboard transitions.
- [x] 4.3 Add or update targeted UI coverage for constrained-window sign-in and pool creation, run the affected tests, and record form inset ownership and results in verification.md.

## 5. Android — System bars and remaining surfaces

- [x] 5.1 Audit HomeView, ProfileView, avatar crop, email-link sign-in, and pool-join states for missing or duplicate safe bounds; correct affected containers and verify actions remain clear of bars, cutouts, and caption controls after rotation or resizing.
- [x] 5.2 Add theme-aware status-bar protection only where content is exposed beneath system indicators, retaining Material app-bar coverage elsewhere and ComponentActivity icon handling; verify legibility while scrolling in both themes without an extra opaque strip.
- [x] 5.3 Audit PushDrawer, app-owned sheets, dialogs, and any discovered FAB/adaptive scaffold; correct only affected inset ownership, configure any qualifying full-screen dialog for edge-to-edge, and verify safe controls plus existing drawer/system Back priority. Record surfaces that require no change.
- [x] 5.4 Run existing drawer and affected surface instrumentation tests, adding focused regression coverage where an inset defect was fixed; record cutout, gesture, contrast, and overlay checks in verification.md.

## 6. Android — Integration verification

- [x] 6.1 Run ./gradlew build from Android and the affected instrumentation suites; record commands, results, and any environment blockers without marking unavailable checks as passed.
- [x] 6.2 Exercise the spec scenarios on API 35 and 36 plus an available supported pre-35 device/emulator, covering gesture/three-button navigation, light/dark themes, portrait/landscape, cutouts, resized windows, and large text; record configurations and any unavailable coverage in verification.md.
- [x] 6.3 Smoke-test authentication, deep links, photo selection/crop, pool navigation, drawer dismissal, list refresh/pagination, and all text-entry flows after the target SDK change; verify existing data, navigation, and accessibility behavior and record results.
