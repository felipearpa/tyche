# Proposal

## Why

Fortuna's Android activity already enables edge-to-edge, but screen-level padding prevents lists from scrolling into system-bar areas and keyboard handling is inconsistent across text-entry flows. Completing the migration described by the edge-to-edge skill will keep controls reachable and system bars legible across navigation modes, keyboard states, and window sizes.

## What Changes

- Declare an explicit Android target SDK of 36, matching the existing compile SDK and satisfying the skill's minimum of 35.
- Keep the existing ComponentActivity edge-to-edge setup and configure keyboard resizing in the manifest, replacing the username editor's runtime adjustment workaround.
- Give each screen a single, explicit owner for system and keyboard insets, using Material 3 component handling where available.
- Pass scaffold insets through to scrolling list content instead of shrinking list parent containers; apply the same safe bounds to loaded, loading, empty, and error states.
- Keep focused fields and form actions reachable in sign-in, pool creation, username editing, and pending-bet entry.
- Ensure pool tab backgrounds extend behind navigation bars and system-bar icons remain legible in both themes.
- Verify non-list screens, drawers, sheets, and any full-screen app dialogs against the same safe-area contract.

## Capabilities

### New Capabilities

- `android-edge-to-edge`: Android window configuration, inset ownership, scrolling under system bars, keyboard-safe forms, and legible system chrome.

### Modified Capabilities

None. Existing profile and drawer behavior remains required. The pool-score-list specification's visual-preservation constraint applies to its accessibility pass; this separate migration changes only screen inset placement and scrolling bounds, preserving row design and accessibility semantics.

## Impact

- Android app build configuration and manifest; MainActivity and screen scaffolds in app, account, pool, and bet modules.
- Shared UI paging-list integration, pool-home tabs, username editor, and any affected custom safe-area containers.
- Android build and targeted UI verification, including existing username-editor and drawer regressions.
- No backend, persistence, API, or iOS changes. No icon redesign, navigation redesign, or new adaptive navigation framework.
